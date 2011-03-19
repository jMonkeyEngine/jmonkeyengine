/*
 * Copyright (c) 2011 jMonkeyEngine
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

package com.jme3.network.kernel.udp;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.network.Filter;
import com.jme3.network.kernel.*;

/**
 *  A Kernel implementation using UDP packets.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class UdpKernel extends AbstractKernel
{
    static Logger log = Logger.getLogger(UdpKernel.class.getName());

    private InetSocketAddress address;
    private HostThread thread;

    // The nature of UDP means that even through a firewall,
    // a user would have to have a unique address+port since UDP
    // can't really be NAT'ed.
    private Map<SocketAddress,UdpEndpoint> socketEndpoints = new ConcurrentHashMap<SocketAddress,UdpEndpoint>();

    public UdpKernel( InetAddress host, int port )
    {
        this( new InetSocketAddress(host, port) );
    }

    public UdpKernel( int port ) throws IOException
    {
        this( new InetSocketAddress(port) );
    }

    public UdpKernel( InetSocketAddress address )
    {
        this.address = address;
    }

    protected HostThread createHostThread()
    {
        return new HostThread();
    }

    public void initialize()
    {
        if( thread != null )
            throw new IllegalStateException( "Kernel already initialized." );

        thread = createHostThread();

        try {
            thread.connect();
            thread.start();
        } catch( IOException e ) {
            throw new KernelException( "Error hosting:" + address, e );
        }
    }

    public void terminate() throws InterruptedException
    {
        if( thread == null )
            throw new IllegalStateException( "Kernel not initialized." );

        try {
            thread.close();
            thread = null;
        } catch( IOException e ) {
            throw new KernelException( "Error closing host connection:" + address, e );
        }
    }

    /**
     *  Dispatches the data to all endpoints managed by the
     *  kernel.  'routing' is currently ignored.
     */
    public void broadcast( Filter<? super Endpoint> filter, ByteBuffer data, boolean reliable )
    {
        if( reliable )
            throw new UnsupportedOperationException( "Reliable send not supported by this kernel." );

        // Hand it to all of the endpoints that match our routing
        for( UdpEndpoint p : socketEndpoints.values() ) {
            // Does it match the filter?
            if( !filter.apply(p) )
                continue;

            // Send the data
            p.send( data );
        }
    }

    protected Endpoint getEndpoint( SocketAddress address, boolean create )
    {
        UdpEndpoint p = socketEndpoints.get(address);
        if( p == null && create ) {
            p = new UdpEndpoint( this, nextEndpointId(), address, thread.getSocket() );
            socketEndpoints.put( address, p );

            // Add an event for it.
            addEvent( EndpointEvent.createAdd( this, p ) );
        }
        return p;
    }

    /**
     *  Called by the endpoints when they need to be closed.
     */
    protected void closeEndpoint( UdpEndpoint p ) throws IOException
    {
        // Just book-keeping to do here.
        socketEndpoints.remove( p.getRemoteAddress() );

        addEvent( EndpointEvent.createRemove( this, p ) );
    }

    protected void newData( DatagramPacket packet )
    {
        // So the tricky part here is figuring out the endpoint and
        // whether it's new or not.  In these UDP schemes, firewalls have
        // to be ported back to a specific machine so we will consider
        // the address + port (ie: SocketAddress) the defacto unique
        // ID.
        Endpoint p = getEndpoint( packet.getSocketAddress(), true );

        // We'll copy the data to trim it.
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), 0, data, 0, data.length);

        Envelope env = new Envelope( p, data, false );
        addEnvelope( env );
    }

    protected class HostThread extends Thread
    {
        private DatagramSocket socket;
        private AtomicBoolean go = new AtomicBoolean(true);

        private byte[] buffer = new byte[65535]; // slightly bigger than needed.

        public HostThread()
        {
            setName( "UDP Host@" + address );
            setDaemon(true);
        }

        protected DatagramSocket getSocket()
        {
            return socket;
        }

        public void connect() throws IOException
        {
            socket = new DatagramSocket( address );
            log.log( Level.INFO, "Hosting UDP connection:{0}.", address );
        }

        public void close() throws IOException, InterruptedException
        {
            // Set the thread to stop
            go.set(false);

            // Make sure the channel is closed
            socket.close();

            // And wait for it
            join();
        }

        public void run()
        {
            log.log( Level.INFO, "Kernel started for connection:{0}.", address );

            // An atomic is safest and costs almost nothing
            while( go.get() ) {
                try {
                    // Could reuse the packet but I don't see the
                    // point and it may lead to subtle bugs if not properly
                    // reset.
                    DatagramPacket packet = new DatagramPacket( buffer, buffer.length );
                    socket.receive(packet);

                    newData( packet );
                } catch( IOException e ) {
                    if( !go.get() )
                        return;
                    reportError( e );
                }
            }
        }
    }
}
