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

package com.jme3.network.service.serializer;

import com.jme3.network.HostedConnection;
import com.jme3.network.Server;
import com.jme3.network.message.SerializerRegistrationsMessage;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.service.AbstractHostedService;
import com.jme3.network.service.HostedServiceManager;


/**
 *
 *
 *  @author    Paul Speed
 */
public class ServerSerializerRegistrationsService extends AbstractHostedService {

    @Override
    protected void onInitialize( HostedServiceManager serviceManager ) {
        // Make sure our message type is registered
        Serializer.registerClass(SerializerRegistrationsMessage.class);
        Serializer.registerClass(SerializerRegistrationsMessage.Registration.class);
    }
    
    @Override
    public void start() {
        // Compile the registrations into a message we will
        // send to all connecting clients
        SerializerRegistrationsMessage.compile();
    }
    
    @Override
    public void connectionAdded(Server server, HostedConnection hc) {
        // Just in case
        super.connectionAdded(server, hc);
        
        // Send the client the registration information
        hc.send(SerializerRegistrationsMessage.INSTANCE);
    }
}
