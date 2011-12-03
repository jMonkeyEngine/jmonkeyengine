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

package jme3test.network;

import com.jme3.network.*;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;
import java.io.IOException;

public class TestMessages {

    @Serializable
    public static class PingMessage extends AbstractMessage {
    }

    @Serializable
    public static class PongMessage extends AbstractMessage {
    }

    private static class ServerPingResponder implements MessageListener<HostedConnection> {
        public void messageReceived(HostedConnection source, com.jme3.network.Message message) {
            if (message instanceof PingMessage){
                System.out.println("Server: Received ping message!");
                source.send(new PongMessage());
            }
        }
    }

    private static class ClientPingResponder implements MessageListener<Client> {
        public void messageReceived(Client source, com.jme3.network.Message message) {
            if (message instanceof PongMessage){
                System.out.println("Client: Received pong message!");
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException{
        Serializer.registerClass(PingMessage.class);
        Serializer.registerClass(PongMessage.class);

        Server server = Network.createServer(5110);
        server.start();

        Client client = Network.connectToServer("localhost", 5110);
        client.start();

        server.addMessageListener(new ServerPingResponder(), PingMessage.class);
        client.addMessageListener(new ClientPingResponder(), PongMessage.class);

        System.out.println("Client: Sending ping message..");
        client.send(new PingMessage());
        
        Object obj = new Object();
        synchronized (obj){
            obj.wait();
        }
    }
}
