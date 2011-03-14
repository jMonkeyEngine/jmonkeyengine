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
import com.jme3.network.message.Message;
import com.jme3.network.message.StreamDataMessage;
import com.jme3.network.message.StreamMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerStreamingService extends MessageAdapter {
    private static Logger log = Logger.getLogger(StreamingService.class.getName());

    protected ArrayList<Stream> streams;

    private short nextStreamID = Short.MIN_VALUE;

    public ServerStreamingService(Server server) {
        streams = new ArrayList<Stream>();
        server.addMessageListener(this, StreamMessage.class);
    }

    public void offerStream(Client client, StreamMessage msg, InputStream data) {
        short streamID = ++nextStreamID;
        msg.setStreamID(streamID);
        msg.setReliable(true);

        Stream stream = new Stream();
        stream.setData(data);
        stream.setMessage(msg);
        stream.setReceiver(client);
        streams.add(stream);

        try {
            client.send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startStream(Stream stream) {
        Client receiver = stream.getReceiver();
        try
        {
            InputStream data = stream.getData();

            byte[] buffer = new byte[1024];
            int length;

            StreamDataMessage msg = new StreamDataMessage(stream.getMessage().getStreamID());
            msg.setReliable(true);

            while ((length = data.read(buffer)) != -1) {
                byte[] newBuffer = new byte[length];

                for (int i = 0; i != length; ++i) {
                    newBuffer[i] = buffer[i];
                }
                msg.setData(newBuffer);
                receiver.send(msg);
            }
            data.close();

            receiver.send(stream.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            log.log(Level.WARNING, "[StreamSender][TCP] Could not send stream with message {0} to {1}. Reason: {2}.", new Object[]{stream, receiver, ex.getMessage()});
        }
    }

    public void messageReceived(Message message) {
        if (message instanceof StreamMessage && !(message instanceof StreamDataMessage)) {
            // A stream was accepted.
            StreamMessage streamMessage = (StreamMessage)message;
            Stream stream = getStream(streamMessage);

            if (stream == null) return;
            stream.setAccepted(true);
            startStream(stream);
        }
    }

    private Stream getStream(short id) {
        for (Stream stream : streams) {
            if (stream.getMessage().getStreamID() == id) return stream;
        }
        return null;
    }

    private Stream getStream(StreamMessage msg) {
        for (Stream stream : streams) {
            if (stream.getMessage().getStreamID() == msg.getStreamID()) return stream;
        }
        return null;
    }
}
