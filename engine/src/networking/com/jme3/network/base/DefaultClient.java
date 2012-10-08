/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import com.jme3.network.*;
import com.jme3.network.ClientStateListener.DisconnectInfo;
import com.jme3.network.kernel.Connector;
import com.jme3.network.message.ChannelInfoMessage;
import com.jme3.network.message.ClientRegistrationMessage;
import com.jme3.network.message.DisconnectMessage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    
    // First two channels are reserved for reliable and
    // unreliable.  Note: channels are endpoint specific so these
    // constants and the handling need not have anything to do with
    // the same constants in DefaultServer... which is why they are
    // separate.
    private static final int CH_RELIABLE = 0;
    private static final int CH_UNRELIABLE = 1;
    private static final int CH_FIRST = 2;
        
    private ThreadLocal<ByteBuffer> dataBuffer = new ThreadLocal<ByteBuffer>();
    
    private int id = -1;
    private boolean isRunning = false;
    private CountDownLatch connecting = new CountDownLatch(1);
    private String gameName;
    private int version;
    private MessageListenerRegistry<Client> messageListeners = new MessageListenerRegistry<Client>();
    private List<ClientStateListener> stateListeners = new CopyOnWriteArrayList<ClientStateListener>();
    private List<ErrorListener<? super Client>> errorListeners = new CopyOnWriteArrayList<ErrorListener<? super Client>>();
    private Redispatch dispatcher = new Redispatch();
    private List<ConnectorAdapter> channels = new ArrayList<ConnectorAdapter>();    
 
    private ConnectorFactory connectorFactory;
    
    public DefaultClient( String gameName, int version )
    {
        this.gameName = gameName;
        this.version = version;
    }
    
    public DefaultClient( String gameName, int version, Connector reliable, Connector fast,
                          ConnectorFactory connectorFactory )
    {
        this( gameName, version );
        setPrimaryConnectors( reliable, fast, connectorFactory );
    }

    protected void setPrimaryConnectors( Connector reliable, Connector fast, ConnectorFactory connectorFactory )
    {
        if( reliable == null )
            throw new IllegalArgumentException( "The reliable connector cannot be null." );            
        if( isRunning )
            throw new IllegalStateException( "Client is already started." );
        if( !channels.isEmpty() )
            throw new IllegalStateException( "Channels already exist." );
            
        this.connectorFactory = connectorFactory;
        channels.add(new ConnectorAdapter(reliable, dispatcher, dispatcher, true));
        if( fast != null ) {
            channels.add(new ConnectorAdapter(fast, dispatcher, dispatcher, false));
        } else {
            // Add the null adapter to keep the indexes right
            channels.add(null);
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
            
        // Start up the threads and stuff for the
        // connectors that we have
        for( ConnectorAdapter ca : channels ) {
            if( ca == null )
                continue;
            ca.start();
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
        reg = new ClientRegistrationMessage();
        reg.setId(tempId);
        reg.setGameName(getGameName());
        reg.setVersion(getVersion());
        reg.setReliable(true);
        send(CH_RELIABLE, reg, false);
        
        // Send registration messages to any other configured
        // connectors
        reg = new ClientRegistrationMessage();
        reg.setId(tempId);
        reg.setReliable(false);
        for( int ch = CH_UNRELIABLE; ch < channels.size(); ch++ ) {
            if( channels.get(ch) == null )
                continue;
            send(ch, reg, false);
        }
    }

    protected void waitForConnected()
    {
        if( isConnected() )
            return;
            
        try {
            connecting.await();
        } catch( InterruptedException e ) {
            throw new RuntimeException( "Interrupted waiting for connect", e );
        }
    }

    public boolean isConnected()
    {
        return id != -1 && isRunning; 
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
        if( message.isReliable() || channels.get(CH_UNRELIABLE) == null ) {
            send(CH_RELIABLE, message, true);
        } else {
            send(CH_UNRELIABLE, message, true);
        }
    }
 
    public void send( int channel, Message message )
    {
        if( channel < 0 || channel + CH_FIRST >= channels.size() )
            throw new IllegalArgumentException( "Channel is undefined:" + channel );
        send( channel + CH_FIRST, message, true );
    }
    
    protected void send( int channel, Message message, boolean waitForConnected )
    {
        checkRunning();
 
        if( waitForConnected ) {       
            // Make sure we aren't still connecting
            waitForConnected();
        }
        
        ByteBuffer buffer = dataBuffer.get();
        if( buffer == null ) {
            buffer = ByteBuffer.allocate( 65536 + 2 );
            dataBuffer.set(buffer);
        }
        buffer.clear();        
 
        // Convert the message to bytes
        buffer = MessageProtocol.messageToBuffer(message, buffer);
                
        // Since we share the buffer between invocations, we will need to 
        // copy this message's part out of it.  This is because we actually
        // do the send on a background thread.       
        byte[] temp = new byte[buffer.remaining()];
        System.arraycopy(buffer.array(), buffer.position(), temp, 0, buffer.remaining());
        buffer = ByteBuffer.wrap(temp);
        
        channels.get(channel).write(buffer);
    }
 
    public void close()
    {
        checkRunning();
 
        closeConnections( null );            
    }         

    protected void closeConnections( DisconnectInfo info )
    {
        if( !isRunning )
            return;

        // Send a close message
    
        // Tell the thread it's ok to die
        for( ConnectorAdapter ca : channels ) {
            if( ca == null )
                continue;
            ca.close();
        }
        
        // Wait for the threads?

        // Just in case we never fully connected
        connecting.countDown();
        
        fireDisconnected(info);
        
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

    public void addErrorListener( ErrorListener<? super Client> listener )
    {
        errorListeners.add( listener );
    } 

    public void removeErrorListener( ErrorListener<? super Client> listener )
    {
        errorListeners.remove( listener );
    } 
 
    protected void fireConnected()
    {
        for( ClientStateListener l : stateListeners ) {
            l.clientConnected( this );
        }            
    }
    
    protected void fireDisconnected( DisconnectInfo info )
    {
        for( ClientStateListener l : stateListeners ) {
            l.clientDisconnected( this, info );
        }            
    }
 
    /**
     *  Either calls the ErrorListener or closes the connection
     *  if there are no listeners.  
     */ 
    protected void handleError( Throwable t )
    {
        // If there are no listeners then close the connection with
        // a reason
        if( errorListeners.isEmpty() ) {
            log.log( Level.SEVERE, "Termining connection due to unhandled error", t );
            DisconnectInfo info = new DisconnectInfo();
            info.reason = "Connection Error";
            info.error = t;
            closeConnections(info);
            return;
        }
    
        for( ErrorListener l : errorListeners ) {
            l.handleError( this, t );
        } 
    }
 
    protected void configureChannels( long tempId, int[] ports ) {

        try {               
            for( int i = 0; i < ports.length; i++ ) {
                Connector c = connectorFactory.createConnector( i, ports[i] );
                ConnectorAdapter ca = new ConnectorAdapter(c, dispatcher, dispatcher, true);
                int ch = channels.size(); 
                channels.add( ca );
                
                // Need to send the connection its hook-up registration
                // and start it.
                ca.start(); 
                ClientRegistrationMessage reg;
                reg = new ClientRegistrationMessage();
                reg.setId(tempId);
                reg.setReliable(true);
                send( ch, reg, false );
            }
        } catch( IOException e ) {
            throw new RuntimeException( "Error configuring channels", e );
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
            connecting.countDown();
            fireConnected();
            return;
        } else if( m instanceof ChannelInfoMessage ) {
            // This is an interum step in the connection process and
            // now we need to add a bunch of connections
            configureChannels( ((ChannelInfoMessage)m).getId(), ((ChannelInfoMessage)m).getPorts() );
            return; 
        } else if( m instanceof DisconnectMessage ) {
            // Can't do too much else yet
            String reason = ((DisconnectMessage)m).getReason();
            log.log( Level.SEVERE, "Connection terminated, reason:{0}.", reason );
            DisconnectInfo info = new DisconnectInfo();
            info.reason = reason;
            closeConnections(info);
        }
    
        // Make sure client MessageListeners are called single-threaded
        // since it could receive messages from the TCP and UDP
        // thread simultaneously.
        synchronized( this ) {
            messageListeners.messageReceived( this, m );
        }
    }
 
    protected class Redispatch implements MessageListener<Object>, ErrorListener<Object>
    {
        public void messageReceived( Object source, Message m )
        {
            dispatch( m );
        }
        
        public void handleError( Object source, Throwable t )
        {
            // Only doing the DefaultClient.this to make the code
            // checker happy... it compiles fine without it but I
            // don't like red lines in my editor. :P
            DefaultClient.this.handleError( t );   
        }
    }    
}
