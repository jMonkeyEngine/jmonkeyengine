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

package com.jme3.network.service;

import com.jme3.network.HostedConnection;
import com.jme3.network.Server;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  Convenient base class for HostedServices providing some default HostedService 
 *  interface implementations as well as a few convenience methods 
 *  such as getServiceManager() and getService(type).  This implementation
 *  enhances the default capabilities provided by AbstractHostedService by
 *  adding automatic connection management.
 * 
 *  <p>Subclasses must at least override the onInitialize(), startHostingOnConnection(), and
 *  stopHostingOnConnection() methods to handle service and connection initialization.</p>
 *
 *  <p>An autoHost flag controls whether startHostingOnConnection() is called
 *  automatically when new connections are detected.  If autoHost is false then it
 *  is up to the implementation or application to specifically start hosting at
 *  some point.</p>
 *
 *  @author    Paul Speed
 */
public abstract class AbstractHostedConnectionService extends AbstractHostedService { 
 
    private static final Logger log = Logger.getLogger(AbstractHostedConnectionService.class.getName());

    private boolean autoHost;
    
    /**
     *  Creates a new HostedService that will autohost connections
     *  when detected.
     */
    protected AbstractHostedConnectionService() {
        this(true);
    }

    /**
     *  Creates a new HostedService that will automatically host
     *  connections only if autoHost is true.
     */
    protected AbstractHostedConnectionService( boolean autoHost ) {
        this.autoHost = autoHost;
    }
 
    /**
     *  When set to true, all new connections will automatically have
     *  hosting services attached to them by calling startHostingOnConnection().
     *  If this is set to false then it is up to the application or other services 
     *  to eventually call startHostingOnConnection().
     *  
     *  <p>Reasons for doing this vary but usually would be because
     *  the client shouldn't be allowed to perform any service-related calls until
     *  it has provided more information... for example, logging in.</p>
     */
    public void setAutoHost( boolean b ) {
        this.autoHost = b;
    }
 
    /**
     *  Returns true if this service automatically attaches 
     *  hosting capabilities to new connections.
     */   
    public boolean getAutoHost() {
        return autoHost;
    }

 
    /**
     *  Performs implementation specific connection hosting setup.
     *  Generally this involves setting up some handlers or session
     *  attributes on the connection.  If autoHost is true then this
     *  method is called automatically during connectionAdded() 
     *  processing.
     */
    public abstract void startHostingOnConnection( HostedConnection hc );

    /**
     *  Performs implementation specific connection tear-down.
     *  This will be called automatically when the connectionRemoved()
     *  event occurs... whether the application has already called it
     *  or not.
     */
    public abstract void stopHostingOnConnection( HostedConnection hc );

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
     *  the server.  This method always calls stopHostingOnConnection(hc).
     *  Implementations should be aware that if they stopHostingOnConnection()
     *  early that they will get a second call when the connection goes away.
     */
    @Override
    public void connectionRemoved(Server server, HostedConnection hc) {
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "connectionRemoved({0}, {1})", new Object[]{server, hc});
        }    
        stopHostingOnConnection(hc);
    }   
}
