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
import com.jme3.network.events.MessageAdapter;
import com.jme3.network.message.Message;
import com.jme3.network.message.StreamDataMessage;
import com.jme3.network.message.StreamMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientStreamingService extends MessageAdapter {
    private static Logger log = Logger.getLogger(StreamingService.class.getName());

    protected ArrayList<StreamListener>
                                streamListeners;

    protected ArrayList<Stream> streams;

    private Client client;


    public ClientStreamingService(Client client) {
        this.client = client;
        streams = new ArrayList<Stream>();
        streamListeners = new ArrayList<StreamListener>();
        client.addMessageListener(this, StreamDataMessage.class,
                                        StreamMessage.class);
    }

    // Client classes/methods //////////////////////////////////////////////////////////////

    public void addStreamListener(StreamListener listener) {
        streamListeners.add(listener);
    }

    public void removeStreamListener(StreamListener listener) {
        streamListeners.remove(listener);
    }

    public void messageReceived(Message message) {
        if (message instanceof StreamMessage && !(message instanceof StreamDataMessage)) {
            // A stream was offered.
            StreamMessage msg = (StreamMessage)message;
            Stream stream = getStream(msg.getStreamID());

            if (stream != null) {
                // This is a completion message.
                for (StreamListener listener : stream.getDataListeners()) {
                    listener.streamCompleted(msg);
                }
            } else {
                stream = new Stream();
                stream.setMessage(msg);
                boolean accept = fireStreamOffered(stream, msg);

                streams.add(stream);
                if (accept) {
                    try {
                        client.send(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (message instanceof StreamDataMessage) {
            StreamDataMessage dataMessage = (StreamDataMessage)message;
            Stream stream = getStream(dataMessage.getStreamID());
            if (stream == null) {
                log.log(Level.WARNING, "[StreamClient][TCP] We've received a data message even though we didn't register to the stream.");
                return;
            }

            for (StreamListener listener : stream.getDataListeners()) {
                listener.streamDataReceived(dataMessage);
            }
        }

    }

    private Stream getStream(short id) {
        for (Stream stream : streams) {
            if (stream.getMessage().getStreamID() == id) return stream;
        }
        return null;
    }

    private boolean fireStreamOffered(Stream stream, StreamMessage message) {
        boolean accept = false;
        for (StreamListener listener : streamListeners) {
            if (listener.streamOffered(message)) {
                accept = true;

                stream.addDataListener(listener);
            }
        }
        return accept;
    }
}
