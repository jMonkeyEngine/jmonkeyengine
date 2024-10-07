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

import com.jme3.network.Client;


/**
 *  Manages ClientServices on behalf of a network Client object.
 *
 *  @author    Paul Speed
 */
public class ClientServiceManager extends ServiceManager<ClientServiceManager> {
    
    private Client client;
 
    /**
     *  Creates a new ClientServiceManager for the specified network Client.
     */   
    public ClientServiceManager( Client client ) {
        this.client = client;
    }
 
    /**
     *  Returns the network Client associated with this ClientServiceManager.
     */   
    public Client getClient() {
        return client;
    }

    /**
     *  Returns 'this' and is what is passed to ClientService.initialize()
     *  and ClientService.terminate();
     */
    @Override
    protected final ClientServiceManager getParent() {
        return this;
    }
    
    /**
     *  Adds the specified ClientService and initializes it.  If the service manager
     *  has already been started then the service will also be started.
     */   
    public void addService( ClientService s ) {
        super.addService(s);
    }

    /**
     *  Adds the specified services and initializes them.  If the service manager
     *  has already been started then the services will also be started.
     *  This is a convenience method that delegates to addService(), thus each
     *  service will be initialized (and possibly started) in sequence rather
     *  than doing them all at the end.
     */   
    public void addServices( ClientService... services ) {
        for( ClientService s : services ) {
            super.addService(s);
        }
    }

    /**
     *  Removes the specified ClientService from this service manager, stopping
     *  and terminating it as required.  If this service manager is in a
     *  started state then the service will be stopped.  After removal,
     *  the service will be terminated.
     */
    public void removeService( ClientService s ) {
        super.removeService(s);
    }
}
