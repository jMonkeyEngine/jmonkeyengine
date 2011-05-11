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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.network.*;
import com.jme3.network.kernel.*;
import com.jme3.network.message.ClientRegistrationMessage; //hopefully temporary
import com.jme3.network.message.DisconnectMessage; //hopefully temporary 
import com.jme3.network.serializing.Serializer;

/**
 *  A default implementation of the Server interface that delegates
 *  its network connectivity to kernel.Kernel.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class DefaultServer implements Server
{
    static Logger log = Logger.getLogger(DefaultServer.class.getName());
    
    private boolean isRunning = false;
    private AtomicInteger nextId = new AtomicInteger(0);
    private String gameName;
    private int version;
    private Kernel reliable;
    private KernelAdapter reliableAdapter;
    private Kernel fast;
    private KernelAdapter fastAdapter;
    private Redispatch dispatcher = new Redispatch();
    private Map<Integer,HostedConnection> connections = new ConcurrentHashMap<Integer,HostedConnection>();
    private Map<Endpoint,HostedConnection> endpointConnections 
                            = new ConcurrentHashMap<Endpoint,HostedConnection>();
    
    // Keeps track of clients for whom we've only received the UDP
    // registration message
    private Map<Long,Connection> connecting = new ConcurrentHashMap<Long,Connection>();
    
    private MessageListenerRegistry<HostedConnection> messageListeners 
                            = new MessageListenerRegistry<HostedConnection>();                        
    private List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<ConnectionListener>();
    
    public DefaultServer( String gameName, int version, Kernel reliable, Kernel fast )
    {
        if( reliable == null )
            throw new IllegalArgumentException( "Default server reqiures a reliable kernel instance." );
            
        this.gameName = gameName;
        this.version = version;
        this.reliable = reliable;
        this.fast = fast;
        
        reliableAdapter = new KernelAdapter( this, reliable, dispatcher, true );
        if( fast != null ) {
            fastAdapter = new KernelAdapter( this, fast, dispatcher, false );
        }
    }   

    public String getGameName()
    {
        return gameName;
    }

    public int getVersion()
    {
        return version;
    }

    public void start()
    {
        if( isRunning )
            throw new IllegalStateException( "Server is already started." );
            
        // Initialize the kernels
        reliable.initialize();
        if( fast != null ) {
            fast.initialize();
        }
 
        // Start em up
        reliableAdapter.start();            
        if( fastAdapter != null ) {
            fastAdapter.start();
        }
        
        isRunning = true;             
    }

    public boolean isRunning()
    {
        return isRunning;
    }     
 
    public void close() 
    {
        if( !isRunning )
            throw new IllegalStateException( "Server is not started." );
 
        try {
            // Kill the adpaters, they will kill the kernels
            if( fastAdapter != null ) {
                fastAdapter.close();
            }
            
            reliableAdapter.close();                           
            isRunning = false;            
        } catch( InterruptedException e ) {
            throw new RuntimeException( "Interrupted while closing", e );
        }                               
    }
 
    public void broadcast( Message message )
    {
        broadcast( null, message );
    }

    public void broadcast( Filter<? super HostedConnection> filter, Message message )
    {
        ByteBuffer buffer = MessageProtocol.messageToBuffer(message, null);
 
        FilterAdapter adapter = filter == null ? null : new FilterAdapter(filter);
               
        // Ignore the filter for the moment
        if( message.isReliable() || fast == null ) {
            // Don't need to copy the data because message protocol is already
            // giving us a fresh buffer
            reliable.broadcast( adapter, buffer, true, false );
        } else {
            fast.broadcast( adapter, buffer, false, false );
        }               
    }

    public HostedConnection getConnection( int id )
    {
        return connections.get(id);
    }     
 
    public Collection<HostedConnection> getConnections()
    {
        return Collections.unmodifiableCollection((Collection<HostedConnection>)connections.values());
    } 
 
    public void addConnectionListener( ConnectionListener listener )
    {
        connectionListeners.add(listener);   
    }
 
    public void removeConnectionListener( ConnectionListener listener )
    {
        connectionListeners.remove(listener);   
    }     
    
    public void addMessageListener( MessageListener<? super HostedConnection> listener )
    {
        messageListeners.addMessageListener( listener );
    } 

    public void addMessageListener( MessageListener<? super HostedConnection> listener, Class... classes )
    {
        messageListeners.addMessageListener( listener, classes );
    } 

    public void removeMessageListener( MessageListener<? super HostedConnection> listener )
    {
        messageListeners.removeMessageListener( listener );
    } 

    public void removeMessageListener( MessageListener<? super HostedConnection> listener, Class... classes )
    {
        messageListeners.removeMessageListener( listener, classes );
    }
 
    protected void dispatch( HostedConnection source, Message m )
    {
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

    protected void registerClient( KernelAdapter ka, Endpoint p, ClientRegistrationMessage m )
    {
        Connection addedConnection = null;
        Connection bootedConnection = null;

        // generally this will only be called by one thread but it's        
        // important enough I won't take chances
        synchronized( this ) {       
            // Grab the random ID that the client created when creating
            // its two registration messages
            long tempId = m.getId();
 
            // See if we already have one
            Connection c = connecting.remove(tempId);
            if( c == null ) {
                c = new Connection();
                log.log( Level.FINE, "Registering client for endpoint, pass 1:{0}.", p );
            } else {
                log.log( Level.FINE, "Refining client registration for endpoint:{0}.", p );
            } 
          
            // Fill in what we now know       
            if( ka == fastAdapter ) {
                c.fast = p;
                
                if( c.reliable == null ) {
                    // Tuck it away for later
                    connecting.put(tempId, c);
                }
                
            } else {
                // It must be the reliable one            
                c.reliable = p;
 
                // Validate the name and version which is only sent
                // over the reliable connection at this point.
                if( !getGameName().equals(m.getGameName()) 
                    || getVersion() != m.getVersion() ) {
 
                    log.log( Level.INFO, "Kicking client due to name/version mismatch:{0}.", c );
            
                    // Need to kick them off... I may regret doing this from within
                    // the sync block but the alternative is more code
                    c.close( "Server client mismatch, server:" + getGameName() + " v" + getVersion()
                             + "  client:" + m.getGameName() + " v" + m.getVersion() );
                    return;                        
                }                                   
                
                if( c.fast == null && fastAdapter != null ) {
                    // Still waiting for the fast connection to
                    // register
                    connecting.put(tempId, c);
                }
            }
    
            if( !connecting.containsKey(tempId) ) {
    
                // Then we are fully connected
                if( connections.put( c.getId(), c ) == null ) {
                
                    if( c.fast != null ) {
                        endpointConnections.put( c.fast, c );
                    }
                    endpointConnections.put( c.reliable, c ); 
 
                    addedConnection = c;               
                }
            }
        }
 
        // Best to do this outside of the synch block to avoid
        // over synchronizing which is the path to deadlocks
        if( addedConnection != null ) {       
            log.log( Level.INFO, "Client registered:{0}.", addedConnection );
            
            // Now we can notify the listeners about the
            // new connection.
            fireConnectionAdded( addedConnection );
                                                    
            // Send the ID back to the client letting it know it's
            // fully connected.
            m = new ClientRegistrationMessage();
            m.setId( addedConnection.getId() );
            m.setReliable(true);
            addedConnection.send(m);
        }            
    }

    protected HostedConnection getConnection( Endpoint endpoint )
    {
        return endpointConnections.get(endpoint);       
    } 

    protected void connectionClosed( Endpoint p )
    {
        log.log( Level.INFO, "Connection closed:{0}.", p );
        
        // Try to find the endpoint in all ways that it might
        // exist.  Note: by this point the channel is closed
        // already.
    
        // Also note: this method will be called twice per
        // HostedConnection if it has two endpoints.
 
        Connection removed = null;
        synchronized( this ) {             
            // Just in case the endpoint was still connecting
            connecting.values().remove(p);

            // And the regular management
            removed = (Connection)endpointConnections.remove(p);
            if( removed != null ) {
                connections.remove( removed.getId() );                
            }
        }
        
        // Better not to fire events while we hold a lock
        // so always do this outside the synch block.
        if( removed != null ) {
        
            log.log( Level.INFO, "Client closed:{0}.", removed );
            
            removed.closeConnection();
        }
    }

    protected class Connection implements HostedConnection
    {
        private int id;
        private Endpoint reliable;
        private Endpoint fast;
        private boolean closed;
        
        private Map<String,Object> sessionData = new ConcurrentHashMap<String,Object>();       
        
        public Connection()
        {
            id = nextId.getAndIncrement();
        }
 
        public Server getServer()
        {   
            return DefaultServer.this;
        }     
       
        public int getId()
        {
            return id;
        }
 
        public String getAddress()
        {            
            return reliable == null ? null : reliable.getAddress();
        }
       
        public void send( Message message )
        {
            ByteBuffer buffer = MessageProtocol.messageToBuffer(message, null);
            if( message.isReliable() || fast == null ) {
                reliable.send( buffer );
            } else {
                fast.send( buffer );
            }
        }
 
        protected void closeConnection()
        {
            if( closed ) 
                return;
            closed = true;
            
            // Make sure both endpoints are closed.  Note: reliable
            // should always already be closed through all paths that I
            // can conceive... but it doesn't hurt to be sure. 
            if( reliable != null && reliable.isConnected() ) {
                reliable.close();
            }
            if( fast != null && fast.isConnected() ) {
                fast.close();
            }
        
            fireConnectionRemoved( this );
        }
        
        public void close( String reason )
        {
            // Send a reason
            DisconnectMessage m = new DisconnectMessage();
            m.setType( DisconnectMessage.KICK );
            m.setReason( reason );
            m.setReliable( true );
            send( m );
            
            // Just close the reliable endpoint
            // fast will be cleaned up as a side-effect
            // when closeConnection() is called by the
            // connectionClosed() endpoint callback.
            if( reliable != null ) {
                // Close with flush so we make sure our
                // message gets out
                reliable.close(true);
            }
        }
        
        public Object setAttribute( String name, Object value )
        {
            if( value == null )
                return sessionData.remove(name);
            return sessionData.put(name, value);
        }
    
        @SuppressWarnings("unchecked")
        public <T> T getAttribute( String name )
        {
            return (T)sessionData.get(name);
        }             

        public Set<String> attributeNames()
        {
            return Collections.unmodifiableSet(sessionData.keySet());
        }           
        
        public String toString()
        {
            return "Connection[ id=" + id + ", reliable=" + reliable + ", fast=" + fast + " ]"; 
        }  
    } 

    protected class Redispatch implements MessageListener<HostedConnection>
    {
        public void messageReceived( HostedConnection source, Message m )
        {
            dispatch( source, m );
        }
    }                                          
     
    protected class FilterAdapter implements Filter<Endpoint>
    {
        private Filter<? super HostedConnection> delegate;
        
        public FilterAdapter( Filter<? super HostedConnection> delegate )
        {
            this.delegate = delegate;
        }
        
        public boolean apply( Endpoint input )
        {
            HostedConnection conn = getConnection( input );
            if( conn == null )
                return false;
            return delegate.apply(conn);
        } 
    }     
}
