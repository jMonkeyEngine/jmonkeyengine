/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.network.streaming;

import com.jme3.network.connection.Client;
import com.jme3.network.connection.Server;
import com.jme3.network.events.MessageAdapter;
import com.jme3.network.message.StreamMessage;
import com.jme3.network.service.Service;

import java.io.InputStream;

/**
 * Streaming service handles all kinds of streaming to clients. It can be instantiated by
 *  both the client and server, where server will work as sender, and client as receiver.
 *
 * @author Lars Wesselius
 */
public class StreamingService extends MessageAdapter implements Service {

    private ClientStreamingService clientService;
    private ServerStreamingService serverService;

    public StreamingService(Client client) {
        clientService = new ClientStreamingService(client);
    }

    public StreamingService(Server server) {
        serverService = new ServerStreamingService(server);
    }

    public void offerStream(Client client, StreamMessage msg, InputStream data) {
        if (serverService == null) return;
        serverService.offerStream(client, msg, data);
    }

    public void addStreamListener(StreamListener listener) {
        if (clientService == null) return;
        clientService.addStreamListener(listener);
    }

    public void removeStreamListener(StreamListener listener) {
        if (clientService == null) return;
        clientService.removeStreamListener(listener);
    }
}
