/*
 * Copyright (c) 2015 jMonkeyEngine
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

package com.jme3.network.service.rpc;

import com.jme3.network.HostedConnection;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.util.SessionDataDelegator;
import com.jme3.network.service.AbstractHostedService;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.rpc.msg.RpcCallMessage;
import com.jme3.network.service.rpc.msg.RpcResponseMessage;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  RPC service that can be added to a network Server to
 *  add RPC send/receive capabilities.  For a particular
 *  HostedConnection, Remote procedure calls can be made to the 
 *  associated Client and responses retrieved.  Any remote procedure 
 *  calls that the Client performs for this connection will be 
 *  received by this service and delegated to the appropriate RpcHandlers.
 *
 *  Note: it can be dangerous for a server to perform synchronous
 *  RPC calls to a client but especially so if not done as part
 *  of the response to some other message.  ie: iterating over all
 *  or some HostedConnections to perform synchronous RPC calls
 *  will be slow and potentially block the server's threads in ways
 *  that can cause deadlocks or odd contention. 
 *
 *  @author    Paul Speed
 */
public class RpcHostedService extends AbstractHostedService {

    private static final String ATTRIBUTE_NAME = "rpcSession";

    static final Logger log = Logger.getLogger(RpcHostedService.class.getName());

    private boolean autoHost;
    private SessionDataDelegator delegator;

    /**
     *  Creates a new RPC host service that can be registered
     *  with the Network server and will automatically 'host'
     *  RPC services and each new network connection.
     */
    public RpcHostedService() {
        this(true);
    }
    
    /**
     *  Creates a new RPC host service that can be registered
     *  with the Network server and will optionally 'host'
     *  RPC services and each new network connection depending
     *  on the specified 'autoHost' flag.
     */
    public RpcHostedService( boolean autoHost ) {
        this.autoHost = autoHost;
        
        // This works for me... has to be different in
        // the general case
        Serializer.registerClasses(RpcCallMessage.class, RpcResponseMessage.class);
    }

    /**
     *  Used internally to setup the message delegator that will
     *  handle HostedConnection specific messages and forward them
     *  to that connection's RpcConnection.
     */
    @Override
    protected void onInitialize( HostedServiceManager serviceManager ) {
        Server server = serviceManager.getServer();
         
        // A general listener for forwarding the messages
        // to the client-specific handler
        this.delegator = new SessionDataDelegator(RpcConnection.class, 
                                                  ATTRIBUTE_NAME,
                                                  true);
        server.addMessageListener(delegator, delegator.getMessageTypes());

        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "Registered delegator for message types:{0}", Arrays.asList(delegator.getMessageTypes()));
        }
    }

    /**
     *  When set to true, all new connections will automatically have
     *  RPC hosting services attached to them, meaning they can send
     *  and receive RPC calls.  If this is set to false then it is up
     *  to other services to eventually call startHostingOnConnection().
     *  
     *  <p>Reasons for doing this vary but usually would be because
     *  the client shouldn't be allowed to perform any RPC calls until
     *  it has provided more information.  In general, this is unnecessary
     *  because the RpcHandler registries are not shared.  Each client
     *  gets their own and RPC calls will fail until the appropriate
     *  objects have been registtered.</p>
     */
    public void setAutoHost( boolean b ) {
        this.autoHost = b;
    }
 
    /**
     *  Returns true if this service automatically attaches RPC
     *  hosting capabilities to new connections.
     */   
    public boolean getAutoHost() {
        return autoHost;
    }

    /**
     *  Retrieves the RpcConnection for the specified HostedConnection
     *  if that HostedConnection has had RPC services started using
     *  startHostingOnConnection() (or via autohosting).  Returns null
     *  if the connection currently doesn't have RPC hosting services
     *  attached.
     */
    public RpcConnection getRpcConnection( HostedConnection hc ) {
        return hc.getAttribute(ATTRIBUTE_NAME);
    }

    /**
     *  Sets up RPC hosting services for the hosted connection allowing
     *  getRpcConnection() to return a valid RPC connection object.
     *  This method is called automatically for all new connections if
     *  autohost is set to true.
     */
    public void startHostingOnConnection( HostedConnection hc ) {
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "startHostingOnConnection:{0}", hc);
        }
        hc.setAttribute(ATTRIBUTE_NAME, new RpcConnection(hc));
    }

    /**
     *  Removes any RPC hosting services associated with the specified
     *  connection.  Calls to getRpcConnection() will return null for
     *  this connection.  The connection's RpcConnection is also closed,
     *  releasing any waiting synchronous calls with a "Connection closing"
     *  error.
     *  This method is called automatically for all leaving connections if
     *  autohost is set to true.
     */
    public void stopHostingOnConnection( HostedConnection hc ) {
        RpcConnection rpc = hc.getAttribute(ATTRIBUTE_NAME);
        if( rpc == null ) {
            return;
        }
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "stopHostingOnConnection:{0}", hc);
        }
        hc.setAttribute(ATTRIBUTE_NAME, null);
        rpc.close();
    }

    /**
     *  Used internally to remove the message delegator from the
     *  server.
     */
    @Override
    public void terminate(HostedServiceManager serviceManager) {
        Server server = serviceManager.getServer();
        server.removeMessageListener(delegator, delegator.getMessageTypes());
    }

    /**
     *  Called internally when a new connection is detected for
     *  the server.  If the current autoHost property is true then
     *  startHostingOnConnection(hc) is called. 
     */
    @Override
    public void connectionAdded(Server server, HostedConnection hc) {
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "connectionAdded({0}, {1})", new Object[]{server, hc});
        }    
        if( autoHost ) {
            startHostingOnConnection(hc);
        }
    }

    /**
     *  Called internally when an existing connection is leaving
     *  the server.  If the current autoHost property is true then
     *  stopHostingOnConnection(hc) is called. 
     */
    @Override
    public void connectionRemoved(Server server, HostedConnection hc) {
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "connectionRemoved({0}, {1})", new Object[]{server, hc});
        }    
        stopHostingOnConnection(hc);
    }

}

