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
package com.jme3.network;

import java.util.Collection;

import com.jme3.network.service.HostedServiceManager;

/**
 *  Represents a host that can send and receive messages to
 *  a set of remote client connections.
 *  
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public interface Server
{
    /**
     *  Returns the 'game name' for this server.  This should match the
     *  'game name' set on connecting clients or they will be turned away.
     */
    public String getGameName();
 
    /**
     *  Returns the game-specific version of this server used for detecting
     *  mismatched clients.
     */   
    public int getVersion();

    /**
     *  Returns the manager for hosted services.  Hosted services extend
     *  the functionality of the server.
     */
    public HostedServiceManager getServices();     

    /**
     *  Sends the specified message to all connected clients.
     */ 
    public void broadcast( Message message );

    /**
     *  Sends the specified message to all connected clients that match
     *  the filter.  If no filter is specified then this is the same as
     *  calling broadcast(message) and the message will be delivered to
     *  all connections.
     *  <p>Examples:</p>
     *  <pre>
     *    // Broadcast to connections: conn1, conn2, and conn3
     *    server.broadcast( Filters.in( conn1, conn2, conn3 ), message );
     *
     *    // Broadcast to all connections exception source
     *    server.broadcast( Filters.notEqualTo( source ), message );
     *  </pre>
     */ 
    public void broadcast( Filter<? super HostedConnection> filter, Message message );

    /**
     *  Sends the specified message over the specified alternate channel to all connected 
     *  clients that match the filter.  If no filter is specified then this is the same as
     *  calling broadcast(message) and the message will be delivered to
     *  all connections.
     *  <p>Examples:</p>
     *  <pre>
     *    // Broadcast to connections: conn1, conn2, and conn3
     *    server.broadcast( Filters.in( conn1, conn2, conn3 ), message );
     *
     *    // Broadcast to all connections exception source
     *    server.broadcast( Filters.notEqualTo( source ), message );
     *  </pre>
     */ 
    public void broadcast( int channel, Filter<? super HostedConnection> filter, Message message );

    /**
     *  Start the server so that it will begin accepting new connections
     *  and processing messages.
     */
    public void start();

    /**
     *  Adds an alternate channel to the server, using the specified port.  This is an 
     *  entirely separate connection where messages are sent and received in parallel 
     *  to the two primary default channels.  All channels must be added before the connection
     *  is started.
     *  Returns the ID of the created channel for use when specifying the channel in send or 
     *  broadcast calls.  The ID is returned entirely out of convenience since the IDs
     *  are predictably incremented.  The first channel is 0, second is 1, and so on.
     */
    public int addChannel( int port ); 

    /**
     *  Returns true if the server has been started.
     */
    public boolean isRunning();     
 
    /**
     *  Closes all client connections, stops and running processing threads, and
     *  closes the host connection.
     */   
    public void close();
 
    /**
     *  Retrieves a hosted connection by ID.
     */
    public HostedConnection getConnection( int id );     
 
    /**
     *  Retrieves a read-only collection of all currently connected connections.
     */
    public Collection<HostedConnection> getConnections(); 
 
    /**
     *  Returns true if the server has active connections at the time of this
     *  call.
     */
    public boolean hasConnections();
    
    /**
     *  Adds a listener that will be notified when new hosted connections
     *  arrive.
     */
    public void addConnectionListener( ConnectionListener listener );
 
    /**
     *  Removes a previously registered connection listener.
     */   
    public void removeConnectionListener( ConnectionListener listener );     
    
    /**
     *  Adds a listener that will be notified when any message or object
     *  is received from one of the clients.
     *
     *  <p>Note about MessageListener multithreading: on the server, message events may
     *  be delivered by more than one thread depending on the server
     *  implementation used.  Listener implementations should treat their
     *  shared data structures accordingly and set them up for multithreaded 
     *  access.  The only threading guarantee is that for a single
     *  HostedConnection, there will only ever be one thread at a time
     *  and the messages will always be delivered to that connection in the 
     *  order that they were delivered.  This is the only restriction placed
     *  upon server message dispatch pool implementations.</p>   
     */
    public void addMessageListener( MessageListener<? super HostedConnection> listener ); 

    /**
     *  Adds a listener that will be notified when messages of the specified
     *  types are received from one of the clients.
     */
    public void addMessageListener( MessageListener<? super HostedConnection> listener, Class... classes ); 

    /**
     *  Removes a previously registered wildcard listener.  This does
     *  not remove this listener from any type-specific registrations.
     */
    public void removeMessageListener( MessageListener<? super HostedConnection> listener ); 

    /**
     *  Removes a previously registered type-specific listener from
     *  the specified types.
     */
    public void removeMessageListener( MessageListener<? super HostedConnection> listener, Class... classes ); 
    
     
}

