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
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.network.*;
import com.jme3.network.message.ClientRegistrationMessage; //hopefully temporary
import com.jme3.network.kernel.Connector;
import com.jme3.network.serializing.Serializer;

/**
 *  A default implementation of the Client interface that delegates
 *  its network connectivity to a kernel.Connector.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class DefaultClient implements Client
{
    static Logger log = Logger.getLogger(DefaultClient.class.getName());
    
    private int id = -1;
    private boolean isRunning = false;
    private String gameName;
    private int version;
    private Connector reliable;
    private Connector fast;
    private MessageListenerRegistry<Client> messageListeners = new MessageListenerRegistry<Client>();
    private List<ClientStateListener> stateListeners = new CopyOnWriteArrayList<ClientStateListener>();
    private Redispatch dispatcher = new Redispatch();
    private ConnectorAdapter reliableAdapter;    
    private ConnectorAdapter fastAdapter;    
    
    public DefaultClient( String gameName, int version )
    {
        this.gameName = gameName;
        this.version = version;
    }
    
    public DefaultClient( String gameName, int version, Connector reliable, Connector fast )
    {
        this( gameName, version );
        setConnectors( reliable, fast );
    }

    protected void setConnectors( Connector reliable, Connector fast )
    {
        if( reliable == null )
            throw new IllegalArgumentException( "The reliable connector cannot be null." );            
        if( isRunning )
            throw new IllegalStateException( "Client is already started." );
            
        this.reliable = reliable;
        this.fast = fast;
        if( reliable != null ) {
            reliableAdapter = new ConnectorAdapter(reliable, dispatcher);
        }
        if( fast != null ) {
            fastAdapter = new ConnectorAdapter(fast, dispatcher);
        }
    }  

    protected void checkRunning()
    {
        if( !isRunning )
            throw new IllegalStateException( "Client is not started." );
    }
 
    public void start()
    {
        if( isRunning )
            throw new IllegalStateException( "Client is already started." );
            
        // Start up the threads and stuff
        if( reliableAdapter != null ) {
            reliableAdapter.start();
        }
        if( fastAdapter != null ) {
            fastAdapter.start();
        }

        // Send our connection message with a generated ID until
        // we get one back from the server.  We'll hash time in
        // millis and time in nanos.
        // This is used to match the TCP and UDP endpoints up on the
        // other end since they may take different routes to get there.
        // Behind NAT, many game clients may be coming over the same
        // IP address from the server's perspective and they may have
        // their UDP ports mapped all over the place.
        //
        // Since currentTimeMillis() is absolute time and nano time
        // is roughtly related to system start time, adding these two
        // together should be plenty unique for our purposes.  It wouldn't
        // hurt to reconcile with IP on the server side, though.
        long tempId = System.currentTimeMillis() + System.nanoTime();

        // Set it true here so we can send some messages.
        isRunning = true;        
                
        ClientRegistrationMessage reg;
        if( reliable != null ) {
            reg = new ClientRegistrationMessage();
            reg.setId(tempId);
            reg.setReliable(true);
            send(reg);            
        }
        if( fast != null ) {
            // We create two different ones to prepare for someday
            // when there will probably be threaded sending.
            reg = new ClientRegistrationMessage();
            reg.setId(tempId);
            reg.setReliable(false);
            send(reg); 
        }        
    }

    public boolean isConnected()
    {
        return id != -1; // for now
    }     

    public int getId()
    {   
        return id;
    }     
 
    public String getGameName()
    {
        return gameName;
    }

    public int getVersion()
    {
        return version;
    }
   
    public void send( Message message )
    {
        checkRunning();
        
        // For now just send direclty.  We allocate our
        // own buffer each time because this method might
        // be called from multiple threads.  If writing
        // is queued into its own thread then that could
        // be shared.
        ByteBuffer buffer = MessageProtocol.messageToBuffer(message, null);
        if( message.isReliable() || fast == null ) {
            if( reliable == null )
                throw new RuntimeException( "No reliable connector configured" );
            reliable.write(buffer);
        } else {
            fast.write(buffer); 
        }
    }
 
    public void close()
    {
        checkRunning();
            
        // Send a close message
    
        // Tell the thread it's ok to die
        if( fastAdapter != null ) {
            fastAdapter.close();
        }
        if( reliableAdapter != null ) {
            reliableAdapter.close();
        }
        
        // Wait for the threads?
        
        fireDisconnected();
        
        isRunning = false;
    }         

    public void addClientStateListener( ClientStateListener listener )
    {
        stateListeners.add( listener );
    } 

    public void removeClientStateListener( ClientStateListener listener )
    {
        stateListeners.remove( listener );
    } 

    public void addMessageListener( MessageListener<? super Client> listener )
    {
        messageListeners.addMessageListener( listener );
    } 

    public void addMessageListener( MessageListener<? super Client> listener, Class... classes )
    {
        messageListeners.addMessageListener( listener, classes );
    } 

    public void removeMessageListener( MessageListener<? super Client> listener )
    {
        messageListeners.removeMessageListener( listener );
    } 

    public void removeMessageListener( MessageListener<? super Client> listener, Class... classes )
    {
        messageListeners.removeMessageListener( listener, classes );
    } 
 
    protected void fireConnected()
    {
        for( ClientStateListener l : stateListeners ) {
            l.clientConnected( this );
        }            
    }
    
    protected void fireDisconnected()
    {
        for( ClientStateListener l : stateListeners ) {
            l.clientDisconnected( this );
        }            
    }
 
    protected void dispatch( Message m )
    {
        // Pull off the connection management messages we're
        // interested in and then pass on the rest.
        if( m instanceof ClientRegistrationMessage ) {
            // Then we've gotten our real id
            this.id = (int)((ClientRegistrationMessage)m).getId();
            log.log( Level.INFO, "Connection established, id:{0}.", this.id );
            fireConnected();
            return;
        }
    
        messageListeners.messageReceived( this, m );
    }
 
    protected class Redispatch implements MessageListener
    {
        public void messageReceived( Object source, Message m )
        {
            dispatch( m );
        }
    }
}
