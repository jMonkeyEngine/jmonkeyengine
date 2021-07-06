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


/**
 *  Convenient base class for HostedServices providing some default HostedService 
 *  interface implementations as well as a few convenience methods 
 *  such as getServiceManager() and getService(type).  Subclasses 
 *  must at least override the onInitialize() method to handle 
 *  service initialization.
 *
 *  @author    Paul Speed
 */
public abstract class AbstractHostedService extends AbstractService<HostedServiceManager> 
                                            implements HostedService { 
    
    protected AbstractHostedService() {
    }
 
    /**
     *  Returns the server for this hosted service or null if
     *  the service is not yet attached.
     */   
    protected Server getServer() {
        HostedServiceManager hsm = getServiceManager();
        return hsm == null ? null : hsm.getServer();
    }

    /**
     *  Default implementation does nothing.  Implementations can
     *  override this to perform custom new connection behavior.
     */
    @Override
    public void connectionAdded(Server server, HostedConnection hc) {
    }

    /**
     *  Default implementation does nothing.  Implementations can
     *  override this to perform custom leaving connection behavior.
     */
    @Override
    public void connectionRemoved(Server server, HostedConnection hc) {
    }
    
}
