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

package com.jme3.network.base;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jme3.network.*;
import com.jme3.network.kernel.Endpoint;
import com.jme3.network.kernel.EndpointEvent;
import com.jme3.network.kernel.Envelope;
import com.jme3.network.kernel.Kernel;
import com.jme3.network.message.ClientRegistrationMessage; //hopefully temporary
import com.jme3.network.serializing.Serializer;

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
    private DefaultServer server; // this is unfortunate
    private Kernel kernel;
    private MessageListener messageDispatcher;
    private AtomicBoolean go = new AtomicBoolean(true);
    
    // Marks the messages as reliable or not if they came
    // through this connector.
    private boolean reliable;
    
    public KernelAdapter( DefaultServer server, Kernel kernel, MessageListener messageDispatcher,
                          boolean reliable )
    {
        super( String.valueOf(kernel) );
        this.server = server;
        this.kernel = kernel;
        this.messageDispatcher = messageDispatcher;
        this.reliable = reliable;
        setDaemon(true);
    }
 
    public void close() throws InterruptedException
    {
        go.set(false);
        
        // Kill the kernel
        kernel.terminate();
    }

    protected HostedConnection getConnection( Endpoint p )
    {
        return server.getConnection(p);
    }
 
    protected void connectionClosed( Endpoint p )
    {
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
     *  <p>The issue with the messages is that if a an implementation is
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
            
        HostedConnection source = getConnection(p);
        messageDispatcher.messageReceived( source, m );
    }

    protected void createAndDispatch( Envelope env )
    {
        MessageProtocol protocol = new MessageProtocol();
    
        byte[] data = env.getData();
        ByteBuffer buffer = ByteBuffer.wrap(data);

        int count = protocol.addBuffer( buffer );
        if( count == 0 )
            throw new RuntimeException( "Envelope contained incomplete data:" + env );
        
        // Should be complete... and maybe we should check but we don't
        Message m = null;
        while( (m = protocol.getMessage()) != null ) {
            m.setReliable(reliable);
            dispatch( env.getSource(), m );
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
        while( (event = kernel.nextEvent()) != null )
            {
            createAndDispatch( event );
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
            
                createAndDispatch( e );
            } catch( InterruptedException ex ) {
                if( !go.get() )
                    return;
                throw new RuntimeException( "Unexpected interruption", ex );
            }
        }
    }
        
}


