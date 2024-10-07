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
package com.jme3.network.base;

import com.jme3.network.*;
import com.jme3.network.base.protocol.SerializerMessageProtocol;
import com.jme3.network.kernel.Endpoint;
import com.jme3.network.kernel.Kernel;
import com.jme3.network.message.ChannelInfoMessage;
import com.jme3.network.message.ClientRegistrationMessage;
import com.jme3.network.message.DisconnectMessage;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.serializer.ServerSerializerRegistrationsService;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  A default implementation of the Server interface that delegates
 *  its network connectivity to kernel.Kernel.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class DefaultServer implements Server
{
    private static final Logger log = Logger.getLogger(DefaultServer.class.getName());

    // First two channels are reserved for reliable and
    // unreliable
    private static final int CH_RELIABLE = 0;
    private static final int CH_UNRELIABLE = 1;
    private static final int CH_FIRST = 2;
    
    private boolean isRunning = false;
    private final AtomicInteger nextId = new AtomicInteger(0);
    private String gameName;
    private int version;
    private final KernelFactory kernelFactory = KernelFactory.DEFAULT;
    private KernelAdapter reliableAdapter;
    private KernelAdapter fastAdapter;
    private final List<KernelAdapter> channels = new ArrayList<>();
    private final List<Integer> alternatePorts = new ArrayList<>();
    private final Redispatch dispatcher = new Redispatch();
    private final Map<Integer,HostedConnection> connections = new ConcurrentHashMap<>();
    private final Map<Endpoint,HostedConnection> endpointConnections 
                            = new ConcurrentHashMap<>();
    
    // Keeps track of clients for whom we've only received the UDP
    // registration message
    private final Map<Long,Connection> connecting = new ConcurrentHashMap<>();
    
    private final MessageListenerRegistry<HostedConnection> messageListeners 
                            = new MessageListenerRegistry<>();                        
    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    
    private HostedServiceManager services;
    private MessageProtocol protocol = new SerializerMessageProtocol();
    
    public DefaultServer( String gameName, int version, Kernel reliable, Kernel fast )
    {
        if( reliable == null )
            throw new IllegalArgumentException( "Default server requires a reliable kernel instance." );
            
        this.gameName = gameName;
        this.version = version;
        this.services = new HostedServiceManager(this);        
        addStandardServices();
        
        reliableAdapter = new KernelAdapter(this, reliable, protocol, dispatcher, true);
        channels.add( reliableAdapter );
        if( fast != null ) {
            fastAdapter = new KernelAdapter(this, fast, protocol, dispatcher, false);
            channels.add( fastAdapter );
        }
    }   

    protected void addStandardServices() {
        log.fine("Adding standard services...");
        services.addService(new ServerSerializerRegistrationsService());
    }

    @Override
    public String getGameName()
    {
        return gameName;
    }

    @Override
    public int getVersion()
    {
        return version;
    }
    
    @Override
    public HostedServiceManager getServices() 
    {
        return services;
    }

    @Override
    public int addChannel( int port )
    {
        if( isRunning )
            throw new IllegalStateException( "Channels cannot be added once server is started." );
 
        // Note: it does bug me that channels aren't 100% universal and
        // set up externally, but it requires a more invasive set of changes
        // for "connection types" and some kind of registry of kernel and
        // connector factories.  This really would be the best approach and
        // would allow all kinds of channel customization maybe... but for
        // now, we hard-code the standard connections and treat the +2 extras
        // differently.
            
        // Check for consistency with the channels list
        if( channels.size() - CH_FIRST != alternatePorts.size() )
            throw new IllegalStateException( "Channel and port lists do not match." ); 
            
        try {                                
            int result = alternatePorts.size(); 
            alternatePorts.add(port);
            
            Kernel kernel = kernelFactory.createKernel(result, port); 
            channels.add( new KernelAdapter(this, kernel, protocol, dispatcher, true) );
            
            return result;
        } catch( IOException e ) {
            throw new RuntimeException( "Error adding channel for port:" + port, e );
        } 
    } 

    protected void checkChannel( int channel )
    {
        if( channel < MessageConnection.CHANNEL_DEFAULT_RELIABLE 
            || channel >= alternatePorts.size() ) {
            throw new IllegalArgumentException( "Channel is undefined:" + channel );
        }              
    }

    @Override
    public void start()
    {
        if( isRunning )
            throw new IllegalStateException( "Server is already started." );
            
        // Initialize the kernels
        for( KernelAdapter ka : channels ) {
            ka.initialize();
        }
 
        // Start em up
        for( KernelAdapter ka : channels ) {
            ka.start();
        }
        
        isRunning = true;
        
        // Start the services
        services.start();             
    }

    @Override
    public boolean isRunning()
    {
        return isRunning;
    }     
 
    @Override
    public void close() 
    {
        if( !isRunning )
            throw new IllegalStateException( "Server is not started." );
 
        // First stop the services since we are about to
        // kill the connections they are using
        services.stop();
 
        try {
            // Kill the adapters, they will kill the kernels
            for( KernelAdapter ka : channels ) {
                ka.close();
            }
            
            isRunning = false;
            
            // Now terminate all of the services.
            services.terminate();             
        } catch( InterruptedException e ) {
            throw new RuntimeException( "Interrupted while closing", e );
        }                               
    }
 
    @Override
    public void broadcast( Message message )
    {
        broadcast( null, message );
    }

    @Override
    public void broadcast( Filter<? super HostedConnection> filter, Message message )
    {
        if( log.isLoggable(Level.FINER) ) {
            log.log(Level.FINER, "broadcast({0}, {1})", new Object[]{filter, message});
        }
        
        if( connections.isEmpty() )
            return;
 
        ByteBuffer buffer = protocol.toByteBuffer(message, null);
 
        FilterAdapter adapter = filter == null ? null : new FilterAdapter(filter);
               
        if( message.isReliable() || fastAdapter == null ) {
            // Don't need to copy the data because message protocol is already
            // giving us a fresh buffer
            reliableAdapter.broadcast( adapter, buffer, true, false );
        } else {
            fastAdapter.broadcast( adapter, buffer, false, false );
        }               
    }

    @Override
    public void broadcast( int channel, Filter<? super HostedConnection> filter, Message message )
    {
        if( log.isLoggable(Level.FINER) ) {
            log.log(Level.FINER, "broadcast({0}, {1}. {2})", new Object[]{channel, filter, message});
        }
        
        if( connections.isEmpty() )
            return;

        checkChannel(channel);
        
        ByteBuffer buffer = protocol.toByteBuffer(message, null);
 
        FilterAdapter adapter = filter == null ? null : new FilterAdapter(filter);

        channels.get(channel+CH_FIRST).broadcast( adapter, buffer, true, false );               
    }

    @Override
    public HostedConnection getConnection( int id )
    {
        return connections.get(id);
    }     
 
    @Override
    public boolean hasConnections()
    {
        return !connections.isEmpty();
    }
 
    @Override
    public Collection<HostedConnection> getConnections()
    {
        return Collections.unmodifiableCollection(connections.values());
    } 
 
    @Override
    public void addConnectionListener( ConnectionListener listener )
    {
        connectionListeners.add(listener);   
    }
 
    @Override
    public void removeConnectionListener( ConnectionListener listener )
    {
        connectionListeners.remove(listener);   
    }     
    
    @Override
    public void addMessageListener( MessageListener<? super HostedConnection> listener )
    {
        messageListeners.addMessageListener( listener );
    } 

    @Override
    public void addMessageListener( MessageListener<? super HostedConnection> listener, Class... classes )
    {
        messageListeners.addMessageListener( listener, classes );
    } 

    @Override
    public void removeMessageListener( MessageListener<? super HostedConnection> listener )
    {
        messageListeners.removeMessageListener( listener );
    } 

    @Override
    public void removeMessageListener( MessageListener<? super HostedConnection> listener, Class... classes )
    {
        messageListeners.removeMessageListener( listener, classes );
    }
 
    protected void dispatch( HostedConnection source, Message m )
    {
        if( log.isLoggable(Level.FINER) ) {
            log.log(Level.FINER, "{0} received:{1}", new Object[]{source, m});
        }
        
        if( source == null ) {
            messageListeners.messageReceived( source, m );
        } else {
        
            // A semi-heavy handed way to make sure the listener
            // doesn't get called at the same time from two different
            // threads for the same hosted connection.
            synchronized( source ) {
                messageListeners.messageReceived( source, m );
            }
        }
    }

    protected void fireConnectionAdded( HostedConnection conn )
    {
        for( ConnectionListener l : connectionListeners ) {
            l.connectionAdded( this, conn );
        }
    }            

    protected void fireConnectionRemoved( HostedConnection conn )
    {
        for( ConnectionListener l : connectionListeners ) {
            l.connectionRemoved( this, conn );
        }
    }            

    protected int getChannel( KernelAdapter ka )
    {
        return channels.indexOf(ka);
    }

    protected void registerClient( KernelAdapter ka, Endpoint p, ClientRegistrationMessage m )
    {
        Connection addedConnection = null;

        // Generally this will only be called by one thread, but it's
        // so important that I won't take chances.
        synchronized( this ) {       
            // Grab the random ID that the client created when creating
            // its two registration messages
            long tempId = m.getId();
 
            // See if we already have one
            Connection c = connecting.remove(tempId);
            if( c == null ) {
                c = new Connection(channels.size());
                log.log( Level.FINE, "Registering client for endpoint, pass 1:{0}.", p );
            } else {
                log.log( Level.FINE, "Refining client registration for endpoint:{0}.", p );
            } 
          
            // Fill in what we now know
            int channel = getChannel(ka); 
            c.setChannel(channel, p);            
            log.log( Level.FINE, "Setting up channel:{0}", channel );
 
            // If it's channel 0, then this is the initial connection,
            // and we will send the connection information.
            if( channel == CH_RELIABLE ) {
                // Validate the name and version which is only sent
                // over the reliable connection at this point.
                if( !getGameName().equals(m.getGameName()) 
                    || getVersion() != m.getVersion() ) {
 
                    log.log( Level.FINE, "Kicking client due to name/version mismatch:{0}.", c );
            
                    // Need to kick them off... I may regret doing this from within
                    // the sync block but the alternative is more code
                    c.close( "Server client mismatch, server:" + getGameName() + " v" + getVersion()
                             + "  client:" + m.getGameName() + " v" + m.getVersion() );
                    return;                        
                }
                
                // Else send the extra channel information to the client
                if( !alternatePorts.isEmpty() ) {
                    ChannelInfoMessage cim = new ChannelInfoMessage( m.getId(), alternatePorts );
                    c.send(cim);
                }
            }

            if( c.isComplete() ) {             
                // Then we are fully connected
                if( connections.put( c.getId(), c ) == null ) {
                
                    for( Endpoint cp : c.channels ) {
                        if( cp == null )
                            continue;
                        endpointConnections.put( cp, c );
                    } 
 
                    addedConnection = c;               
                }
            } else {
                // Need to keep getting channels, so we'll keep it in
                // the map.
                connecting.put(tempId, c);
            } 
        }
 
        // Best to do this outside of the synch block to avoid
        // over synchronizing which is the path to deadlocks
        if( addedConnection != null ) {       
            log.log( Level.FINE, "Client registered:{0}.", addedConnection );
            
            // Send the ID back to the client letting it know it's
            // fully connected.
            m = new ClientRegistrationMessage();
            m.setId( addedConnection.getId() );
            m.setReliable(true);
            addedConnection.send(m);
            
            // Now we can notify the listeners about the
            // new connection.
            fireConnectionAdded( addedConnection );
            
            // Send a second registration message with an invalid ID
            // to let the connection know that it can start its services
            m = new ClientRegistrationMessage();
            m.setId(-1);
            m.setReliable(true);
            addedConnection.send(m);            
        }            
    }

    protected HostedConnection getConnection( Endpoint endpoint )
    {
        return endpointConnections.get(endpoint);       
    } 

    protected void removeConnecting( Endpoint p ) 
    {
        // No easy lookup for connecting Connections
        // from endpoint.
        for( Map.Entry<Long,Connection> e : connecting.entrySet() ) {
            if( e.getValue().hasEndpoint(p) ) {
                connecting.remove(e.getKey());
                return;
            } 
        }
    }

    protected void connectionClosed( Endpoint p )
    {
        if( p.isConnected() ) {
            log.log( Level.FINE, "Connection closed:{0}.", p );
        } else {
            log.log( Level.FINE, "Connection closed:{0}.", p );
        }
        
        // Try to find the endpoint in all ways that it might
        // exist.  Note: by this point the raw network channel is 
        // closed already.
    
        // Also note: this method will be called multiple times per
        // HostedConnection if it has multiple endpoints.
 
        Connection removed;
        synchronized( this ) {             
            // Just in case the endpoint was still connecting
            removeConnecting(p);

            // And the regular management
            removed = (Connection)endpointConnections.remove(p);
            if( removed != null ) {
                connections.remove( removed.getId() );                
            }
            
            log.log( Level.FINE, "Connections size:{0}", connections.size() );
            log.log( Level.FINE, "Endpoint mappings size:{0}", endpointConnections.size() );
        }
        
        // Better not to fire events while we hold a lock
        // so always do this outside the synch block.
        // Note: checking removed.closed just to avoid spurious log messages
        //       since in general we are called back for every endpoint closing.
        if( removed != null && !removed.closed ) {
        
            log.log( Level.FINE, "Client closed:{0}.", removed );
            
            removed.closeConnection();
        }
    }

    protected class Connection implements HostedConnection
    {
        private final int id;
        private boolean closed;
        private Endpoint[] channels;
        private int setChannelCount = 0; 
       
        private final Map<String,Object> sessionData = new ConcurrentHashMap<>();       
        
        public Connection( int channelCount )
        {
            id = nextId.getAndIncrement();
            channels = new Endpoint[channelCount];
        }
        
        boolean hasEndpoint( Endpoint p )
        {
            for( Endpoint e : channels ) {
                if( p == e ) {
                    return true;
                }
            }
            return false;
        }
 
        void setChannel( int channel, Endpoint p )
        {
            if( channels[channel] != null && channels[channel] != p ) {
                throw new RuntimeException( "Channel has already been set:" + channel 
                                            + " = " + channels[channel] + ", cannot be set to:" + p );
            }
            channels[channel] = p;
            if( p != null )
                setChannelCount++;
        }
        
        boolean isComplete()
        {
            return setChannelCount == channels.length;
        }
 
        @Override
        public Server getServer()
        {   
            return DefaultServer.this;
        }     
       
        @Override
        public int getId()
        {
            return id;
        }
 
        @Override
        public String getAddress()
        {            
            return channels[CH_RELIABLE] == null ? null : channels[CH_RELIABLE].getAddress();
        }
       
        @Override
        public void send( Message message )
        {
            if( log.isLoggable(Level.FINER) ) {
                log.log(Level.FINER, "send({0})", message);
            }
            ByteBuffer buffer = protocol.toByteBuffer(message, null);
            if( message.isReliable() || channels[CH_UNRELIABLE] == null ) {
                channels[CH_RELIABLE].send( buffer );
            } else {
                channels[CH_UNRELIABLE].send( buffer );
            }
        }

        @Override
        public void send( int channel, Message message )
        {
            if( log.isLoggable(Level.FINER) ) {
                log.log(Level.FINER, "send({0}, {1})", new Object[]{channel, message});
            }
            checkChannel(channel);
            ByteBuffer buffer = protocol.toByteBuffer(message, null);
            channels[channel+CH_FIRST].send(buffer);
        }
 
        protected void closeConnection()
        {
            if( closed ) 
                return;
            closed = true;
            
            // Make sure all endpoints are closed.  Note: reliable
            // should always already be closed through all paths that I
            // can conceive... but it doesn't hurt to be sure. 
            for( Endpoint p : channels ) {
                if( p == null || !p.isConnected() ) 
                    continue;
                p.close();
            }
        
            fireConnectionRemoved( this );
        }
        
        @Override
        public void close( String reason )
        {
            // Send a reason
            DisconnectMessage m = new DisconnectMessage();
            m.setType( DisconnectMessage.KICK );
            m.setReason( reason );
            m.setReliable( true );
            send( m );
            
            // Just close the reliable endpoint
            // fast.  Will be cleaned up as a side effect
            // when closeConnection() is called by the
            // connectionClosed() endpoint callback.
            if( channels[CH_RELIABLE] != null ) {
                // Close with flush to ensure our
                // message gets out.
                channels[CH_RELIABLE].close(true);
            }
        }
        
        @Override
        public Object setAttribute( String name, Object value )
        {
            if( value == null )
                return sessionData.remove(name);
            return sessionData.put(name, value);
        }
    
        @SuppressWarnings("unchecked")
        @Override
        public <T> T getAttribute( String name )
        {
            return (T)sessionData.get(name);
        }             

        @Override
        public Set<String> attributeNames()
        {
            return Collections.unmodifiableSet(sessionData.keySet());
        }           
        
        @Override
        public String toString()
        {
            return "Connection[ id=" + id + ", reliable=" + channels[CH_RELIABLE] 
                                     + ", fast=" + channels[CH_UNRELIABLE] + " ]"; 
        }  
    } 

    protected class Redispatch implements MessageListener<HostedConnection>
    {
        @Override
        public void messageReceived( HostedConnection source, Message m )
        {
            dispatch( source, m );
        }
    }                                          
     
    protected class FilterAdapter implements Filter<Endpoint>
    {
        private final Filter<? super HostedConnection> delegate;
        
        public FilterAdapter( Filter<? super HostedConnection> delegate )
        {
            this.delegate = delegate;
        }
        
        @Override
        public boolean apply( Endpoint input )
        {
            HostedConnection conn = getConnection( input );
            if( conn == null )
                return false;
            return delegate.apply(conn);
        } 
    }     
}
