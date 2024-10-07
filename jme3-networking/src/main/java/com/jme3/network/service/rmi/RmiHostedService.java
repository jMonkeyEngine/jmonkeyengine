/*
 * Copyright (c) 2015-2021 jMonkeyEngine
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

package com.jme3.network.service.rmi;

import com.jme3.network.HostedConnection;
import com.jme3.network.MessageConnection;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.service.AbstractHostedService;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.rpc.RpcHostedService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  A service that can be added to the host to support a simple
 *  shared objects protocol.
 *
 *  <p>Objects are shared by adding them to the RmiRegistry with one of the
 *  share() methods.  Shared objects must have a separate interface and implementation.
 *  The interface is what the other end of the connection will use to interact
 *  with the object and that interface class must be available on both ends of
 *  the connection.  The implementing class need only be on the sharing end.</p>
 *
 *  <p>Shared objects can be accessed on the other end of the connection by
 *  using one of the RmiRegistry's getRemoteObject() methods.  These can be
 *  used to lookup an object by class if it is a shared singleton or by name
 *  if it was registered with a name.</p>
 *
 *  <p>On the hosting side, a special shardGlobal() method is provided that
 *  will register shared objects that will automatically be provided to every
 *  new joining client and they will all be calling the same server-side instance.
 *  Normally, shared objects themselves are connection specific and handled
 *  at the connection layer.  The shareGlobal() space is a way to have global
 *  resources passed directly though the need is relatively rare.</p>
 * 
 *  <p>Note: This RMI implementation is not as advanced as Java's regular
 *  RMI as it won't marshall shared references, ie: you can't pass
 *  a shared objects as an argument to another shared object's method.</p>
 *
 *  @author    Paul Speed
 */
public class RmiHostedService extends AbstractHostedService {

    private static final Logger log = Logger.getLogger(RpcHostedService.class.getName());
    
    public static final String ATTRIBUTE_NAME = "rmi";

    private RpcHostedService rpcService;
    private short rmiId;
    private byte defaultChannel;
    private boolean autoHost;
    private final Map<String, GlobalShare> globalShares = new ConcurrentHashMap<>();

    public RmiHostedService() {
        this((short)-1, (byte)MessageConnection.CHANNEL_DEFAULT_RELIABLE, true);
    }

    public RmiHostedService( byte defaultChannel ) {
        this((short)-1, defaultChannel, true);
    }

    public RmiHostedService( short rmiId, byte defaultChannel, boolean autoHost ) {
        this.rmiId = rmiId;
        this.defaultChannel = defaultChannel;
        this.autoHost = autoHost;
        
        Serializer.registerClasses(ClassInfo.class, MethodInfo.class);
    }

    /**
     *  Shares a server-wide object associated with the specified type.  All connections
     *  with RMI hosting started will have access to this shared object as soon as they 
     *  connect, and they will all share the same instance.  It is up to the shared object
     *  to handle any multithreading that might be required.
     */     
    public <T> void shareGlobal( T object, Class<? super T> type ) {
        shareGlobal(defaultChannel, type.getName(), object, type);
    }
    
    /**
     *  Shares a server-wide object associated with the specified name.  All connections
     *  with RMI hosting started will have access to this shared object as soon as they 
     *  connect, and they will all share the same instance.  It is up to the shared object
     *  to handle any multithreading that might be required.
     */     
    public <T> void shareGlobal( String name, T object, Class<? super T> type ) {
        shareGlobal(defaultChannel, name, object, type);
    }
    
    /**
     *  Shares a server-wide object associated with the specified name over the specified
     *  channel.  All connections with RMI hosting started will have access to this shared 
     *  object as soon as they connect, and they will all share the same instance.  It is up
     *  to the shared object to handle any multithreading that might be required.
     *  All network communication associated with the shared object will be done over
     *  the specified channel. 
     */     
    public <T> void shareGlobal( byte channel, String name, T object, Class<? super T> type ) {
        GlobalShare share = new GlobalShare(channel, object, type);
        GlobalShare existing = globalShares.put(name, share);
        if( existing != null ) {
            // Shouldn't need to do anything, actually.
        }
        
        // Go through all of the children
        for( HostedConnection conn : getServer().getConnections() ) {
            RmiRegistry child = getRmiRegistry(conn);
            if( child == null ) {
                continue;
            }
            child.share(channel, name, object, type);
        }
    } 

    /**
     *  Set to true if all new connections should automatically have RMI hosting started.
     *  Set to false if the game-specific connection setup will call startHostingOnConnection()
     *  after some connection setup is done (for example, logging in).  Note: generally
     *  is safe to autohost RMI as long as callers are careful about what they've added
     *  using shareGlobal().  One reasonable use-case is to shareGlobal() some kind of login
     *  service and nothing else.  All other shared objects would then be added as
     *  connection-specific objects during successful login processing.
     */
    public void setAutoHost( boolean b ) {
        this.autoHost = b;
    }
 
    /**
     *  Returns true if RMI hosting is automatically started for all new connections. 
     */
    public boolean getAutoHost() {
        return autoHost;
    }

    /**
     *  Returns the RMI registry for the specific HostedConnection.  Each connection
     *  has its own registry with its own connection-specific shared objects.
     */
    public RmiRegistry getRmiRegistry( HostedConnection hc ) {
        return hc.getAttribute(ATTRIBUTE_NAME);
    }

    /**
     *  Sets up RMI hosting services for the hosted connection allowing
     *  getRmiRegistry() to return a valid RmiRegistry object.
     *  This method is called automatically for all new connections if
     *  autohost is set to true.
     */
    @SuppressWarnings("unchecked")
    public void startHostingOnConnection( HostedConnection hc ) {
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "startHostingOnConnection:{0}", hc);
        }
        RmiRegistry rmi = new RmiRegistry(hc, rpcService.getRpcConnection(hc), 
                                          rmiId, defaultChannel); 
        hc.setAttribute(ATTRIBUTE_NAME, rmi);
        
        // Register any global shares
        for( Map.Entry<String, GlobalShare> e : globalShares.entrySet() ) {
            GlobalShare share = e.getValue();
            rmi.share(share.channel, e.getKey(), share.object, share.type); 
        }
    }

    /**
     *  Removes any RMI hosting services associated with the specified
     *  connection.  Calls to getRmiRegistry() will return null for
     *  this connection.  
     *  This method is called automatically for all leaving connections if
     *  autohost is set to true.
     */
    public void stopHostingOnConnection( HostedConnection hc ) {
        RmiRegistry rmi = hc.getAttribute(ATTRIBUTE_NAME);
        if( rmi == null ) {
            return;
        }
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "stopHostingOnConnection:{0}", hc);
        }
        hc.setAttribute(ATTRIBUTE_NAME, null);
        //rpc.close();
    }

    @Override
    protected void onInitialize( HostedServiceManager s ) {
        this.rpcService = getService(RpcHostedService.class);
        if( rpcService == null ) {
            throw new RuntimeException("RmiHostedService requires RpcHostedService");
        }
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
        if( autoHost ) {    
            stopHostingOnConnection(hc);
        }
    }
    
    private class GlobalShare {
        byte channel;
        Object object;
        Class type;
        
        public GlobalShare( byte channel, Object object, Class type ) {
            this.channel = channel;
            this.object = object;
            this.type = type;
        }
    }
}
