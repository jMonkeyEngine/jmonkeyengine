/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.network.kernel.tcp;

import com.jme3.network.Filter;
import com.jme3.network.kernel.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  A Kernel implementation based on NIO selectors.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class SelectorKernel extends AbstractKernel
{
    private static final Logger log = Logger.getLogger(SelectorKernel.class.getName());

    private InetSocketAddress address;
    private SelectorThread thread;

    private Map<Long,NioEndpoint> endpoints = new ConcurrentHashMap<>();

    public SelectorKernel( InetAddress host, int port )
    {
        this( new InetSocketAddress(host, port) );
    }

    public SelectorKernel( int port ) throws IOException
    {
        this( new InetSocketAddress(port) );
    }

    public SelectorKernel( InetSocketAddress address )
    {
        this.address = address;
    }

    protected SelectorThread createSelectorThread()
    {
        return new SelectorThread();
    }

    @Override
    public void initialize()
    {
        if( thread != null )
            throw new IllegalStateException( "Kernel already initialized." );

        thread = createSelectorThread();

        try {
            thread.connect();
            thread.start();
        } catch( IOException e ) {
            throw new KernelException( "Error hosting:" + address, e );
        }
    }

    @Override
    public void terminate() throws InterruptedException
    {
        if( thread == null )
            throw new IllegalStateException( "Kernel not initialized." );

        try {
            thread.close();
            thread = null;
            
            // Need to let any caller waiting for a read() wakeup 
            wakeupReader();       
        } catch( IOException e ) {
            throw new KernelException( "Error closing host connection:" + address, e );
        }
    }

    @Override
    public void broadcast( Filter<? super Endpoint> filter, ByteBuffer data, boolean reliable,
                           boolean copy )
    {
        if( !reliable )
            throw new UnsupportedOperationException( "Unreliable send not supported by this kernel." );

        if( copy ) {
            // Copy the data just once
            byte[] temp = new byte[data.remaining()];
            System.arraycopy(data.array(), data.position(), temp, 0, data.remaining());
            data = ByteBuffer.wrap(temp);
        }

        // Hand it to all of the endpoints that match our routing
        for( NioEndpoint p : endpoints.values() ) {
            // Does it match the filter?
            if( filter != null && !filter.apply(p) )
                continue;

            // Give it the data... but let each endpoint track their
            // own completion over the shared array of bytes by
            // duplicating it
            p.send( data.duplicate(), false, false );
        }

        // Wake up the selector so it can reinitialize its
        // state accordingly.
        wakeupSelector();
    }

    protected NioEndpoint addEndpoint( SocketChannel c )
    {
        // Note: we purposely do NOT put the key in the endpoint.
        //       SelectionKeys are dangerous outside the selector thread
        //       and this is safer.
        NioEndpoint p = new NioEndpoint( this, nextEndpointId(), c );

        endpoints.put( p.getId(), p );

        // Enqueue an endpoint event for the listeners
        addEvent( EndpointEvent.createAdd( this, p ) );

        return p;
    }

    protected void removeEndpoint( NioEndpoint p, SocketChannel c )
    {
        endpoints.remove( p.getId() );
        log.log( Level.FINE, "Endpoints size:{0}", endpoints.size() );

        // Enqueue an endpoint event for the listeners
        addEvent( EndpointEvent.createRemove( this, p ) );

        wakeupReader();
    }

    /**
     *  Called by the endpoints when they need to be closed.
     */
    protected void closeEndpoint( NioEndpoint p ) throws IOException
    {
        //log.log( Level.FINE, "Closing endpoint:{0}.", p );
            
        thread.cancel(p);
    }

    /**
     *  Used internally by the endpoints to wake up the selector
     *  when they have data to send.
     */
    protected void wakeupSelector()
    {
        thread.wakeupSelector();
    }

    protected void newData( NioEndpoint p, SocketChannel c, ByteBuffer shared, int size )
    {
        // Note: if ever desirable, it would be possible to accumulate
        //       data per source channel and only 'finalize' it when
        //       asked for more envelopes than were ready.  I just don't
        //       think it will be an issue in practice.  The busier the
        //       server, the more the buffers will fill before we get to them.
        //       And if the server isn't busy, who cares if we chop things up
        //       smaller... the network is still likely to deliver things in
        //       bulk anyway.

        // Must copy the shared data before we use it
        byte[] dataCopy = new byte[size];
        System.arraycopy(shared.array(), 0, dataCopy, 0, size);

        Envelope env = new Envelope( p, dataCopy, true );
        addEnvelope( env );
    }

    /**
     *  This class is purposely tucked neatly away because
     *  messing with the selector from other threads for any
     *  reason is very bad.  This is the safest architecture.
     */
    protected class SelectorThread extends Thread
    {
        private ServerSocketChannel serverChannel;
        private Selector selector;
        private AtomicBoolean go = new AtomicBoolean(true);
        private ByteBuffer working = ByteBuffer.allocate( 8192 );

        /**
         *  Because we want to keep the keys to ourselves, we'll do
         *  the endpoint -&gt; key mapping internally.
         */
        private Map<NioEndpoint,SelectionKey> endpointKeys = new ConcurrentHashMap<>();

        public SelectorThread()
        {
            setName( "Selector@" + address );
            setDaemon(true);
        }

        public void connect() throws IOException
        {
            // Create a new selector
            this.selector = SelectorProvider.provider().openSelector();

            // Create a new non-blocking server socket channel
            this.serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            // Bind the server socket to the specified address and port
            serverChannel.socket().bind(address);

            // Register the server socket channel, indicating an interest in
            // accepting new connections
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            log.log( Level.FINE, "Hosting TCP connection:{0}.", address );
        }

        public void close() throws IOException, InterruptedException
        {
            // Set the thread to stop
            go.set(false);

            // Make sure the channel is closed
            serverChannel.close();

            // Force the selector to stop blocking
            wakeupSelector();

            // And wait for it
            join();
        }

        protected void wakeupSelector()
        {
            selector.wakeup();
        }

        protected void setupSelectorOptions()
        {
            // For now, selection keys will either be in OP_READ
            // or OP_WRITE.  So while we are writing a buffer, we
            // will not be reading.  This is way simpler and less
            // error-prone. It can be changed when everything
            // else works, if we are looking to micro-optimize.

            // Setup options based on the current state of
            // the endpoints.  This could potentially be more
            // efficiently done as change requests... or simply
            // keeping a thread-safe set of endpoints with pending
            // writes.  For most cases, it shouldn't matter.
            for( Map.Entry<NioEndpoint,SelectionKey> e : endpointKeys.entrySet() ) {
                if( e.getKey().hasPending() ) {
                    e.getValue().interestOps(SelectionKey.OP_WRITE);
                }
            }
        }

        protected void accept( SelectionKey key ) throws IOException
        {
            // Would only get accepts on a server channel
            ServerSocketChannel serverChan = (ServerSocketChannel)key.channel();

            // Set up the connection to be non-blocking.
            SocketChannel remoteChan = serverChan.accept();
            remoteChan.configureBlocking(false);

            // And disable Nagle's buffering algorithm... we want
            // data to go when we put it there.
            Socket sock = remoteChan.socket();
            sock.setTcpNoDelay(true);

            // Let the selector know we're interested in reading
            // data from the channel
            SelectionKey endKey = remoteChan.register( selector, SelectionKey.OP_READ );

            // And now create a new endpoint
            NioEndpoint p = addEndpoint( remoteChan );
            endKey.attach(p);
            endpointKeys.put(p, endKey);
        }

        protected void cancel( NioEndpoint p ) throws IOException
        {
            SelectionKey key = endpointKeys.remove(p);
            if( key == null ) {
                //log.log( Level.FINE, "Endpoint already closed:{0}.", p );
                return;  // already closed it
            }                
            log.log( Level.FINE, "Endpoint keys size:{0}", endpointKeys.size() );

            log.log( Level.FINE, "Closing endpoint:{0}.", p );
            SocketChannel c = (SocketChannel)key.channel();

            // Note: key.cancel() is specifically thread safe.  One of
            //       the few things one can do with a key from another
            //       thread.
            key.cancel();
            c.close();
            removeEndpoint( p, c );
        }

        protected void cancel( SelectionKey key, SocketChannel c ) throws IOException
        {
            NioEndpoint p = (NioEndpoint)key.attachment();            
            log.log( Level.FINE, "Closing channel endpoint:{0}.", p );
            Object o = endpointKeys.remove(p);

            log.log( Level.FINE, "Endpoint keys size:{0}", endpointKeys.size() );

            key.cancel();
            c.close();
            removeEndpoint( p, c );
        }

        protected void read( SelectionKey key ) throws IOException
        {
            NioEndpoint p = (NioEndpoint)key.attachment();
            SocketChannel c = (SocketChannel)key.channel();
            working.clear();

            int size;
            try {
                size = c.read(working);
            } catch( IOException e ) {
                // The remove end forcibly closed the connection...
                // close out our end and cancel the key
                cancel( key, c );
                return;
            }

            if( size == -1 ) {
                // The remote end shut down cleanly...
                // close out our end and cancel the key
                cancel( key, c );
                return;
            }

            newData( p, c, working, size );
        }

        protected void write( SelectionKey key ) throws IOException
        {
            NioEndpoint p = (NioEndpoint)key.attachment();
            SocketChannel c = (SocketChannel)key.channel();

            // We will send what we can and move on.
            ByteBuffer current = p.peekPending();
            if( current == NioEndpoint.CLOSE_MARKER ) {
                // This connection wants to be closed now
                closeEndpoint(p);

                // Nothing more to do
                return;
            }

            c.write( current );

            // If we wrote all of that packet then we need to remove it
            if( current.remaining() == 0 ) {
                p.removePending();
            }

            // If we happened to empty the pending queue then let's read
            // again.
            if( !p.hasPending() ) {
                key.interestOps( SelectionKey.OP_READ );
            }
        }

        protected void select() throws IOException
        {
            selector.select();

            for( Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                SelectionKey key = i.next();
                i.remove();

                if( !key.isValid() )
                    {
                    // When does this happen?
                    log.log( Level.FINE, "Key is not valid:{0}.", key );
                    continue;
                    }

                try {
                    if( key.isAcceptable() )
                        accept(key);
                    else if( key.isWritable() )
                        write(key);
                    else if( key.isReadable() )
                        read(key);
                } catch( IOException e ) {
                    if( !go.get() )
                        return;  // error likely due to shutting down
                    reportError( e );
                    
                    // And at this level, errors likely mean the key is now
                    // dead, and it doesn't hurt to kick them anyway.  If we
                    // find IOExceptions that are not fatal, this can be
                    // readdressed
                    cancel( key, (SocketChannel)key.channel() );                    
                }                        
            }
        }

        @Override
        public void run()
        {
            log.log( Level.FINE, "Kernel started for connection:{0}.", address );

            // An atomic is safest and costs almost nothing
            while( go.get() ) {
                // Setup any queued option changes
                setupSelectorOptions();

                // Check for available keys and process them
                try {
                    select();                    
                } catch( ClosedSelectorException e ) {
                    if( !go.get() )
                        return;  // it's because we're shutting down
                    throw new KernelException( "Premature selector closing", e );
                } catch( CancelledKeyException e ) {
                    if( !go.get() )
                        return;  // it's because we're shutting down
                    throw new KernelException( "Invalid key state", e );
                } catch( IOException e ) {
                    if( !go.get() )
                        return;  // error likely due to shutting down
                    reportError( e );
                }
            }
        }
    }
}
