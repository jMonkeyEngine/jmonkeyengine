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
import java.util.concurrent.atomic.AtomicLong;

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
    private boolean isRunning = false;
    private AtomicLong nextId = new AtomicLong(0);
    private Kernel reliable;
    private KernelAdapter reliableAdapter;
    private Kernel fast;
    private KernelAdapter fastAdapter;
    private Redispatch dispatcher = new Redispatch();
    private Map<Long,HostedConnection> connections = new ConcurrentHashMap<Long,HostedConnection>();
    private Map<Endpoint,HostedConnection> endpointConnections 
                            = new ConcurrentHashMap<Endpoint,HostedConnection>();
    
    // Keeps track of clients for whom we've only received the UDP
    // registration message
    private Map<Long,Connection> connecting = new ConcurrentHashMap<Long,Connection>();
    
    private MessageListenerRegistry<HostedConnection> messageListeners 
                            = new MessageListenerRegistry<HostedConnection>();                        
    private List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<ConnectionListener>();
    
    public DefaultServer( Kernel reliable, Kernel fast )
    {
        this.reliable = reliable;
        this.fast = fast;
        
        if( reliable != null ) {
            reliableAdapter = new KernelAdapter( this, reliable, dispatcher );
        }
        if( fast != null ) {
            fastAdapter = new KernelAdapter( this, fast, dispatcher );
        }
    }   

    public void start()
    {
        if( isRunning )
            throw new IllegalStateException( "Server is already started." );
            
        // Initialize the kernels
        if( reliable != null ) {
            reliable.initialize();
        }
        if( fast != null ) {
            fast.initialize();
        }
 
        // Start em up
        if( reliableAdapter != null ) {
            reliableAdapter.start();            
        }
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
            if( reliableAdapter != null ) {
                reliableAdapter.close();
                
            isRunning = false;            
            }
        } catch( InterruptedException e ) {
            throw new RuntimeException( "Interrupted while closing", e );
        }                               
    }
 
    public void broadcast( Message message )
    {
        broadcast( null, message );
    }

    public void broadcast( Object filter, Message message )
    {
        ByteBuffer buffer = MessageProtocol.messageToBuffer(message, null);
        
        // Ignore the filter for the moment
        if( message.isReliable() || fast == null ) {
            if( reliable == null )
                throw new RuntimeException( "No reliable kernel configured" );
            reliable.broadcast( filter, buffer, true );
        } else {
            fast.broadcast( filter, buffer, false );
        }               
    }

    public HostedConnection getConnection( long id )
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
        messageListeners.messageReceived( source, m );
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
            // Nnow we can notify the listeners about the
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
        
            // Make sure both endpoints are closed.  Note: reliable
            // should always already be closed through all paths that I
            // can conceive... but it doesn't hurt to be sure. 
            if( removed.reliable != null && removed.reliable.isConnected() ) {
                removed.reliable.close();
            }
            if( removed.fast != null && removed.fast.isConnected() ) {
                removed.fast.close();
            }
        
            fireConnectionRemoved( removed );
        }
    }

    protected class Connection implements HostedConnection
    {
        private long id;
        private Endpoint reliable;
        private Endpoint fast;       
        
        public Connection()
        {
            id = nextId.getAndIncrement();
        }
        
        public long getId()
        {
            return id;
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
        
        public void close( String reason )
        {
            // Send a reason
            DisconnectMessage m = new DisconnectMessage();
            m.setType( DisconnectMessage.KICK );
            m.setReason( reason );
            m.setReliable( true );
            send( m );
            
            // Note: without a way to flush the pending messages
            //       during close, the above message may never
            //       go out.
                   
            // Just close the reliable endpoint
            // fast will be cleaned up as a side-effect
            if( reliable != null ) {
                reliable.close();
            }
        }     
    } 

    protected class Redispatch implements MessageListener<HostedConnection>
    {
        public void messageReceived( HostedConnection source, Message m )
        {
            dispatch( source, m );
        }
    }                                          
     
}
