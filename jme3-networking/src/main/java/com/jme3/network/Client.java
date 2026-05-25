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
package com.jme3.network;

import com.jme3.network.service.ClientServiceManager;


/**
 *  Represents a remote connection to a server that can be used
 *  for sending and receiving messages.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public interface Client extends MessageConnection
{
    /**
     *  Starts the client allowing it to begin processing incoming
     *  messages and delivering them to listeners.
     */
    public void start();

    /**
     *  Returns true if this client is fully connected to the
     *  host.
     *
     *  @return true if the client is connected to the server
     */
    public boolean isConnected();     

    /**
     *  Returns true if this client has been started and is still
     *  running.
     *
     *  @return true if the client has been started and not yet closed
     */
    public boolean isStarted();

    /**
     *  Returns a unique ID for this client within the remote
     *  server or -1 if this client isn't fully connected to the
     *  server.
     *
     *  @return the server-assigned client id, or -1 if not connected
     */
    public int getId();     
 
    /**
     *  Returns the 'game name' for servers to which this client should be able
     *  to connect.  This should match the 'game name' set on the server or this
     *  client will be turned away.
     *
     *  @return the configured game name
     */
    public String getGameName();
 
    /**
     *  Returns the game-specific version of the server this client should
     *  be able to connect to.
     *
     *  @return the expected game protocol version
     */   
    public int getVersion();

    /**
     *  Returns the manager for client services.  Client services extend
     *  the functionality of the client.
     *
     *  @return the client service manager
     */
    public ClientServiceManager getServices();     
 
    /**
     *  Sends a message to the server.
     */   
    @Override
    public void send( Message message );
 
    /**
     *  Sends a message to the other end of the connection using
     *  the specified alternate channel.
     */   
    @Override
    public void send( int channel, Message message );
 
    /**
     *  Closes this connection to the server.
     */
    public void close();         

    /**
     *  Adds a listener that will be notified about connection
     *  state changes.
     *
     *  @param listener the listener to add
     */
    public void addClientStateListener( ClientStateListener listener ); 

    /**
     *  Removes a previously registered connection listener.
     *
     *  @param listener the listener to remove
     */
    public void removeClientStateListener( ClientStateListener listener ); 

    /**
     *  Adds a listener that will be notified when any message or object
     *  is received from the server.
     *
     *  @param listener the listener to add
     */
    public void addMessageListener( MessageListener<? super Client> listener ); 

    /**
     *  Adds a listener that will be notified when messages of the specified
     *  types are received.
     *
     *  @param listener the listener to add
     *  @param classes the message classes the listener should receive
     */
    public void addMessageListener( MessageListener<? super Client> listener, Class... classes ); 

    /**
     *  Removes a previously registered wildcard listener.  This does
     *  not remove this listener from any type-specific registrations.
     *
     *  @param listener the listener to remove
     */
    public void removeMessageListener( MessageListener<? super Client> listener ); 

    /**
     *  Removes a previously registered type-specific listener from
     *  the specified types.
     *
     *  @param listener the listener to remove
     *  @param classes the message classes to unregister
     */
    public void removeMessageListener( MessageListener<? super Client> listener, Class... classes ); 
    
    /**
     *  Adds a listener that will be notified when any connection errors
     *  occur.  If a client has no error listeners then the default behavior
     *  is to close the connection and provide an appropriate DisconnectInfo
     *  to any ClientStateListeners.  If the application adds its own error
     *  listeners then it must take care of closing the connection itself.
     *
     *  @param listener the listener to add
     */
    public void addErrorListener( ErrorListener<? super Client> listener ); 

    /**
     *  Removes a previously registered error listener.
     *
     *  @param listener the listener to remove
     */
    public void removeErrorListener( ErrorListener<? super Client> listener ); 
}

