/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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

import com.jme3.network.Filter;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.kernel.Endpoint;
import com.jme3.network.kernel.EndpointEvent;
import com.jme3.network.kernel.Envelope;
import com.jme3.network.kernel.Kernel;
import com.jme3.network.message.ClientRegistrationMessage;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Wraps a single Kernel and forwards new messages
 *  to the supplied message dispatcher and new endpoint
 *  events to the connection dispatcher.  This is used
 *  by DefaultServer to manage its kernel objects.
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
public class KernelAdapter extends Thread
{
    static Logger log = Logger.getLogger(KernelAdapter.class.getName());
    
    private DefaultServer server; // this is unfortunate
    private Kernel kernel;
    private MessageListener<HostedConnection> messageDispatcher;
    private AtomicBoolean go = new AtomicBoolean(true);
     
    private MessageProtocol protocol;
    
    // Keeps track of the in-progress messages that are received
    // on reliable connections
    private Map<Endpoint, MessageBuffer> messageBuffers = new ConcurrentHashMap<>();
    
    // Marks the messages as reliable or not if they came
    // through this connector.
    private boolean reliable;
    
    public KernelAdapter( DefaultServer server, Kernel kernel, MessageProtocol protocol, MessageListener<HostedConnection> messageDispatcher,
                          boolean reliable )
    {
        super( String.valueOf(kernel) );
        this.server = server;
        this.kernel = kernel;
        this.protocol = protocol;
        this.messageDispatcher = messageDispatcher;
        this.reliable = reliable;
        setDaemon(true);
    }

    public Kernel getKernel()
    {
        return kernel;
    }

    public void initialize()
    {
        kernel.initialize();
    }
 
    public void broadcast( Filter<? super Endpoint> filter, ByteBuffer data, boolean reliable, 
                           boolean copy )
    {
        kernel.broadcast( filter, data, reliable, copy );
    }                           
 
    public void close() throws InterruptedException
    {
        go.set(false);
        
        // Kill the kernel
        kernel.terminate();
        
        join();
    }

    protected void reportError( Endpoint p, Object context, Exception e )
    {
        // Should really be queued up so the outer thread can
        // retrieve them.  For now we'll just log it.  FIXME
        log.log( Level.SEVERE, "Unhandled error, endpoint:" + p + ", context:" + context, e );

        if( p.isConnected() ) {
            // In lieu of other options, at least close the endpoint
            p.close();
        }
    }                                                      

    protected HostedConnection getConnection( Endpoint p )
    {
        return server.getConnection(p);
    }
 
    protected void connectionClosed( Endpoint p )
    {
        // Remove any message buffer we've been accumulating 
        // on behalf of this endpoing
        messageBuffers.remove(p);

        log.log( Level.FINE, "Buffers size:{0}", messageBuffers.size() );
    
        server.connectionClosed(p);
    }
 
    /**
     *  Note on threading for those writing their own server 
     *  or adapter implementations.  The rule that a single connection be 
     *  processed by only one thread at a time is more about ensuring that
     *  the messages are delivered in the order that they are received
     *  than for any user-code safety.  99% of the time the user code should
     *  be writing for multithreaded access anyway.
     *
     *  <p>The issue with the messages is that if an implementation is
     *  using a general thread pool then it would be possible for a 
     *  naive implementation to have one thread grab an Envelope from
     *  connection 1's and another grab the next Envelope.  Since an Envelope
     *  may contain several messages, delivering the second thread's messages
     *  before or during the first's would be really confusing and hard
     *  to code for in user code.</p>
     *
     *  <p>And that's why this note is here.  DefaultServer does a rudimentary
     *  per-connection locking but it couldn't possibly guard against
     *  out of order Envelope processing.</p>    
     */
    protected void dispatch( Endpoint p, Message m )
    {
        // Because this class is the only one with the information
        // to do it... we need to pull of the registration message
        // here.
        if( m instanceof ClientRegistrationMessage ) {
            server.registerClient( this, p, (ClientRegistrationMessage)m );
            return;           
        }                
 
        try {           
            HostedConnection source = getConnection(p);
            if( source == null ) {
                if( reliable ) {
                    // If it's a reliable connection then it's slightly more
                    // concerning but this can happen all the time for a UDP endpoint.
                    log.log( Level.WARNING, "Received message from unconnected endpoint:" + p + "  message:" + m );
                }                    
                return; 
            }
            messageDispatcher.messageReceived( source, m );
        } catch( Exception e ) {
            reportError(p, m, e);
        }
    }

    protected MessageBuffer getMessageBuffer( Endpoint p )
    {
        if( !reliable ) {
            // Since UDP comes in packets and they aren't split
            // up, there is no reason to buffer.  In fact, there would
            // be a down side because there is no way for us to reliably
            // clean these up later since we'd create another one for 
            // any random UDP packet that comes to the port.
            return protocol.createBuffer();
        } else {
            // See if we already have one
            MessageBuffer result = messageBuffers.get(p);
            if( result == null ) {
                result = protocol.createBuffer();
                messageBuffers.put(p, result);
            }
            return result;
        }
    }

    protected void createAndDispatch( Envelope env )
    {
        MessageBuffer protocol = getMessageBuffer(env.getSource()); 
    
        byte[] data = env.getData();
        ByteBuffer buffer = ByteBuffer.wrap(data);

        if( !protocol.addBytes(buffer) ) {
            // This can happen if there was only a partial message
            // received.  However, this should never happen for unreliable
            // connections.
            if( !reliable ) {
                // Log some additional information about the packet.
                int len = Math.min( 10, data.length );
                StringBuilder sb = new StringBuilder();
                for( int i = 0; i < len; i++ ) {
                    sb.append( "[" + Integer.toHexString(data[i]) + "]" ); 
                }
                log.log( Level.FINE, "First 10 bytes of incomplete nessage:" + sb );         
                throw new RuntimeException( "Envelope contained incomplete data:" + env );
            }                
        }            
        
        // Should be complete... and maybe we should check but we don't
        Message m = null;
        while( (m = protocol.pollMessage()) != null ) {
            m.setReliable(reliable);
            dispatch(env.getSource(), m);
        }
    } 

    protected void createAndDispatch( EndpointEvent event )
    {
        // Only need to tell the server about disconnects 
        if( event.getType() == EndpointEvent.Type.REMOVE ) {            
            connectionClosed( event.getEndpoint() );
        }            
    }
 
    protected void flushEvents()
    {
        EndpointEvent event;
        while( (event = kernel.nextEvent()) != null ) {
            try {
                createAndDispatch( event );
            } catch( Exception e ) {
                reportError(event.getEndpoint(), event, e);        
            }
        }
    }
 
    public void run()
    {
        while( go.get() ) {
        
            try {           
                // Check for pending events
                flushEvents();
 
                // Grab the next envelope
                Envelope e = kernel.read();
                if( e == Kernel.EVENTS_PENDING )
                    continue; // We'll catch it up above
 
                // Check for pending events that might have
                // come in while we were blocking.  This is usually
                // when the connection add events come through
                flushEvents();
            
                try {
                    createAndDispatch( e );
                } catch( Exception ex ) {
                    reportError(e.getSource(), e, ex);        
                }
                        
            } catch( InterruptedException ex ) {
                if( !go.get() )
                    return;
                throw new RuntimeException( "Unexpected interruption", ex );
            }
        }
    }
        
}


