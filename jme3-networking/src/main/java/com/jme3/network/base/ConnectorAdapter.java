/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package com.jme3.network.base;

import com.jme3.network.ErrorListener;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.kernel.Connector;
import com.jme3.network.kernel.ConnectorException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  Wraps a single Connector and forwards new messages
 *  to the supplied message dispatcher.  This is used
 *  by DefaultClient to manage its connector objects.
 *  This is only responsible for message reading and provides
 *  no support for buffering writes.
 *
 *  <p>This adapter assumes a simple protocol where two
 *  bytes define a (short) object size with the object data
 *  to follow.  Note: this limits the size of serialized
 *  objects to 32676 bytes... even though, for example,
 *  datagram packets can hold twice that. :P</p>  
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class ConnectorAdapter extends Thread
{
    private static final int OUTBOUND_BACKLOG = 16000;

    private Connector connector;
    private MessageListener<Object> dispatcher;
    private ErrorListener<Object> errorHandler;
    private AtomicBoolean go = new AtomicBoolean(true);
    private MessageProtocol protocol;

    private BlockingQueue<ByteBuffer> outbound;
     
    // Writes messages out on a background thread
    private WriterThread writer;
   
    // Marks the messages as reliable or not if they came
    // through this connector.
    private boolean reliable;
 
    public ConnectorAdapter( Connector connector, MessageProtocol protocol, 
                             MessageListener<Object> dispatcher, 
                             ErrorListener<Object> errorHandler, boolean reliable )
    {
        super( String.valueOf(connector) );
        this.connector = connector;
        this.protocol = protocol;        
        this.dispatcher = dispatcher;
        this.errorHandler = errorHandler;
        this.reliable = reliable;
        setDaemon(true);
 
        // The backlog makes sure that the outbound channel blocks once
        // a certain backlog level is reached.  It is set high so that it
        // is only reached in the worst cases... which are usually things like
        // raw throughput tests.  Technically, a saturated TCP channel could
        // back up quite a bit if the buffers are full and the socket has
        // stalled but 16,000 messages is still a big backlog.       
        outbound = new ArrayBlockingQueue<ByteBuffer>(OUTBOUND_BACKLOG); 
 
        // Note: this technically adds a potential deadlock case
        // with the above code where there wasn't one before.  For example,
        // if a TCP outbound queue fills to capacity and a client sends
        // in such a way that they block TCP message handling then if the HostedConnection
        // on the server is similarly blocked then the TCP network buffers may
        // all get full, and no outbound messages move, and we forever block
        // on the queue.
        // However, in practice this can't really happen... or at least it's
        // the sign of other really bad things.
        // First, currently the server-side outbound queues are all unbounded and
        // so won't ever block the handling of messages if the outbound channel is full.
        // Second, there would have to be a huge amount of data backlog for this
        // to ever occur anyway.
        // Third, it's a sign of a really poor architecture if 16,000 messages
        // can go out in a way that blocks reads. 
        
        writer = new WriterThread();
        writer.start();                                           
    }
 
    public void close()
    {
        go.set(false);

        // Kill the writer service
        writer.shutdown();
 
        if( connector.isConnected() )
            {       
            // Kill the connector
            connector.close();
            }
    }
 
    protected void dispatch( Message m )
    {
        dispatcher.messageReceived( null, m );                        
    }
 
    public void write( ByteBuffer data )
    {
        try {
            outbound.put( data );
        } catch( InterruptedException e ) {
            throw new RuntimeException( "Interrupted while waiting for queue to drain", e );
        }
    }
 
    protected void handleError( Exception e )
    {
        if( !go.get() )
            return;
        
        errorHandler.handleError( this, e );
    }
 
    @Override
    public void run()
    {
        MessageBuffer messageBuffer = protocol.createBuffer();
 
        try {                  
            while( go.get() ) {
                ByteBuffer buffer = connector.read();
                if( buffer == null ) {
                    if( go.get() ) {
                        throw new ConnectorException( "Connector closed." ); 
                    } else {
                        // Just dump out because a null buffer is expected
                        // from a closed/closing connector
                        break;
                    }
                }
                
                messageBuffer.addBytes(buffer);
                
                Message m = null;
                while( (m = messageBuffer.pollMessage()) != null ) {
                    m.setReliable( reliable );
                    dispatch( m );
                }
            }
        } catch( Exception e ) {
            handleError( e );
        }            
    }
 
    protected class WriterThread extends Thread
    {
        public WriterThread()
        {
            super( String.valueOf(connector) + "-writer" );
        }

        public void shutdown()
        {
            interrupt();
        }
 
        private void write( ByteBuffer data )
        {
            try {                                        
                connector.write(data);
            } catch( Exception e ) {
                handleError( e );
            }
        }
        
        @Override
        public void run()
        {
            while( go.get() ) {
                try {           
                    ByteBuffer data = outbound.take();
                    write(data);                                       
                } catch( InterruptedException e ) {
                    if( !go.get() )
                        return;
                    throw new RuntimeException( "Interrupted waiting for data", e );
                } 
            }
        }
    }
}
