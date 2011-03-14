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

package com.jme3.network.connection;

import com.jme3.network.message.DiscoverHostMessage;
import com.jme3.network.message.Message;
import com.jme3.network.serializing.Serializer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.logging.Level;

/**
 * The <code>UDPConnection</code> handles all UDP traffic.
 *
 * @author Lars Wesselius
 */
public class UDPConnection extends Connection {
    protected DatagramChannel datagramChannel;

    protected ByteBuffer writeBuffer;
    protected ByteBuffer readBuffer;

    protected SocketAddress target = null;

    public UDPConnection(String label) {
        this.label = label;

        readBuffer =    ByteBuffer.allocateDirect(8192);
        writeBuffer =   ByteBuffer.allocateDirect(8192);
    }

    public void connect(SocketAddress address) throws IOException {
        datagramChannel = selector.provider().openDatagramChannel();
        datagramChannel.socket().bind(null);
        datagramChannel.socket().connect(address);
        datagramChannel.configureBlocking(false);

        datagramChannel.register(selector, SelectionKey.OP_READ);
        log.log(Level.INFO, "[{1}][UDP] Set target to {0}", new Object[]{address, label});
        target = address;
    }

    public void bind(SocketAddress address) throws IOException {
        datagramChannel = selector.provider().openDatagramChannel();
        datagramChannel.socket().bind(address);
        datagramChannel.configureBlocking(false);
        
        datagramChannel.register(selector, SelectionKey.OP_READ);

        log.log(Level.INFO, "[{1}][UDP] Bound to {0}", new Object[]{address, label});
    }

    public void connect(SelectableChannel channel) throws IOException {
        // UDP is connectionless.
    }

    public void accept(SelectableChannel channel) throws IOException {
        // UDP is connectionless.
    }

    public void read(SelectableChannel channel) throws IOException {
        DatagramChannel socketChannel = (DatagramChannel)channel;

        InetSocketAddress address = (InetSocketAddress)datagramChannel.receive(readBuffer);
        if (address == null){
            //System.out.println("Address is NULL!");
		 //TODO: Fix disconnection issue
            socketChannel.close();

            return;
        }

        String reason = shouldFilterConnector(address);
        if (reason != null) {
            log.log(Level.INFO, "[Server][UDP] Client with address {0} got filtered with reason: {1}", new Object[]{address, reason});
            socketChannel.close();
            return;
        }

        SelectionKey key = socketChannel.keyFor(selector);
        if ((key.attachment() == null || ((Client)key.attachment()).getDatagramReceiver() != address) && target == null) {
            Client client = new Client(true);
            client.setDatagramReceiver(address);
            client.setUDPConnection(this);
            client.setDatagramChannel(socketChannel);
            synchronized (connections){
                connections.add(client);
            }

            key.attach(client);
        }

        readBuffer.flip();


        Object object = Serializer.readClassAndObject(readBuffer);

        log.log(Level.FINE, "[{0}][UDP] Read full object: {1}", new Object[]{label, object});

        if (object instanceof Message) {
            Message message = (Message)object;

            if (message instanceof DiscoverHostMessage) {
                synchronized (connections){
                    connections.remove( (Client) key.attachment() );
                }
                log.log(Level.FINE, "[{0}][UDP] Responded to a discover host message by {1}.", new Object[]{label, address});
                send(address, message);
                return;
            }

            Object attachment = socketChannel.keyFor(selector).attachment();
            if (attachment instanceof Client) message.setClient((Client)attachment);
            message.setConnection(this);
            this.fireMessageReceived(message);
        } else {
            this.fireObjectReceived(object);
        }

        readBuffer.clear();
    }

    protected void send(SocketAddress dest, Object object) {
        try {
            Serializer.writeClassAndObject(writeBuffer, object);
            writeBuffer.flip();

            if (dest == null)
                throw new NullPointerException();

            int bytes = datagramChannel.send(writeBuffer, dest);

            if (object instanceof Message) {
                this.fireMessageSent((Message)object);
            } else {
                this.fireObjectSent(object);
            }

            log.log(Level.FINE, "[{0}][UDP] Wrote {1} bytes to {2}.", new Object[]{label, bytes, dest});
            writeBuffer.clear();
        } catch (ClosedChannelException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendObject(Object object) throws IOException {
        if (target == null) {
            // This is a UDP server.
            synchronized (connections){
                for (Client connector : connections) {
                    send(connector.getDatagramReceiver(), object);
                }
            }
        } else {
            send(target, object);
        }
    }

    public void sendObject(Client client, Object object) throws IOException {
        if (object instanceof Message) ((Message)object).setClient(client);
        send(client.getDatagramReceiver(), object);
    }

    public void cleanup() throws IOException {
        datagramChannel.close();

        if (target == null) {
            synchronized (connections){
                connections.clear();
            }
        }
    }

    public void write(SelectableChannel channel) throws IOException {
        // UDP is (almost) always ready for data, so send() will do.
    }
}
