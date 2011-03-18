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

import com.jme3.network.events.ConnectionListener;
import com.jme3.network.events.MessageListener;
import com.jme3.network.message.ClientRegistrationMessage;
import com.jme3.network.message.DisconnectMessage;
import com.jme3.network.message.DiscoverHostMessage;
import com.jme3.network.message.Message;
import com.jme3.network.queue.MessageQueue;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.service.ServiceManager;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  @deprecated Use {@link com.jme3.network.Client} from {@link com.jme3.network.Network} instead. 
 */
@Deprecated
public class Client extends ServiceManager {
    protected Logger            log = Logger.getLogger(Client.class.getName());

    protected static int        clientIDCounter = 0;
    protected int               clientID;
    protected long              playerID = -1;
    protected String            label;

    protected boolean           isConnected;
    protected TCPConnection     tcp;
    protected UDPConnection     udp;

    protected ConnectionRunnable
                                thread;

    protected MessageQueue      messageQueue;

    // Client (connector) related.
    protected SocketChannel     tcpChannel;
    protected DatagramChannel   udpChannel;

    protected SocketAddress     udpTarget;

    protected boolean           isConnector;

    protected ClientObserver    listener = new ClientObserver();

    /**
     * Constructs this client.
     * @deprecated Call createClient() on {@link com.jme3.network.Network} instead.
     */
    @Deprecated
    public Client() {
        this(false);
    }

    /**
     * Construct this client, either as a server connector, or
     *  a real client. Internal method.
     *
     * @param connector Whether this client is a connector or not.
     */
    Client(boolean connector) {
        super(ServiceManager.CLIENT);
        clientID = ++clientIDCounter;
        this.label = "Client#" + clientID;

        isConnector = connector;
        if (connector)  {
            isConnected = true;
        } else {
            if (tcp == null) tcp = new TCPConnection(label);
            if (udp == null) udp = new UDPConnection(label);
        }

        messageQueue = new MessageQueue();
    }

    /**
     * Constructor providing custom instances of the clients and its addresses.
     *
     * @param tcp The TCPConnection instance to manage.
     * @param udp The UDPConnection instance to manage.
     * @param tcpAddress The TCP address to connect to.
     * @param udpAddress The UDP address to connect to.
     * @throws java.io.IOException When a connect error has occurred.
     */
    public Client(TCPConnection tcp, UDPConnection udp, SocketAddress tcpAddress, SocketAddress udpAddress) throws IOException {
        this();

        this.tcp = tcp;
        tcp.connect(tcpAddress);

        this.udp = udp;
        udp.connect(udpAddress);
        isConnected = true;

        registerInternalListeners();
    }

    /**
     * Constructor for providing a TCP client instance. UDP will be disabled.
     *
     * @param tcp The TCPConnection instance.
     * @param tcpAddress The address to connect to.
     * @throws IOException When a connection error occurs.
     */
    public Client(TCPConnection tcp, SocketAddress tcpAddress) throws IOException {
        this();

        this.tcp = tcp;
        tcp.connect(tcpAddress);
        isConnected = true;

        registerInternalListeners();
    }

    /**
     * Constructor for providing a UDP client instance. TCP will be disabled.
     *
     * @param udp The UDP client instance.
     * @param updAddress The address to connect to.
     * @throws IOException When a connection error occurs.
     */
    public Client(UDPConnection udp, SocketAddress updAddress) throws IOException {
        this();

        this.udp = udp;
        udp.connect(updAddress);
        isConnected = true;

        registerInternalListeners();
    }

    /**
     * Simple constructor for providing TCP port and UDP port. Will bind using on
     *  all interfaces, on given ports.
     *
     * @param ip The IP address where the server are located.
     * @param tcpPort The TCP port to use.
     * @param udpPort The UDP port to use.
     * @throws IOException When a connection error occurs.
     * @deprecated Call connectToServer() on {@link com.jme3.network.Network} instead.
     */
    @Deprecated
    public Client(String ip, int tcpPort, int udpPort) throws IOException {
        this();

        tcp = new TCPConnection(label);
        tcp.connect(new InetSocketAddress(ip, tcpPort));

        udp = new UDPConnection(label);
        udp.connect(new InetSocketAddress(ip, udpPort));
        isConnected = true;

        registerInternalListeners();
    }

    /**
     * Connect method for when the no arg constructor was used.
     *
     * @param ip The IP address to connect to.
     * @param tcpPort The TCP port to use. To turn off, use -1.
     * @param udpPort The UDP port to use. To turn off, use -1.
     * @throws IllegalArgumentException When an illegal argument was given.
     * @throws java.io.IOException When a connection error occurs.
     */
    public void connect(String ip, int tcpPort, int udpPort) throws IllegalArgumentException, IOException {
        if (tcpPort == -1 && udpPort == -1) throw new IllegalArgumentException("No point in connect when you want to turn both the connections off.");

        if (tcpPort != -1) {
            tcp.connect(new InetSocketAddress(ip, tcpPort));
        }
        if (udpPort != -1) {
            udp.connect(new InetSocketAddress(ip, udpPort));
        }
        registerInternalListeners();
        isConnected = true;
    }

    private void registerInternalListeners() {
        if (tcp != null) {
            tcp.addConnectionListener(listener);
            tcp.socketChannel.keyFor(tcp.selector).attach(this);
        }
        addMessageListener(listener, DisconnectMessage.class);
    }

    /**
     * Send a message. Whether it's over TCP or UDP is determined by the message flag.
     *
     * @param message The message to send.
     * @throws IOException When a writing error occurs.
     */
    public void send(Message message) throws IOException {
        if (!isConnected) throw new IOException("Not connected yet. Use connect() first.");

        try {
            if (message.isReliable()) {
                messageQueue.add(message);
                if (!isConnector) {
                    tcp.socketChannel.keyFor(tcp.selector).interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                } else {
                    tcpChannel.keyFor(tcp.selector).interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                }
            } else {
                udp.sendObject(message);
            }
        } catch (CancelledKeyException e) {
            // Client was disconnected.
        }
    }

    /**
     * Disconnect from the server.
     *
     * @param type See DisconnectMessage for the available types.
     * @throws IOException When a disconnection error occurs.
     */
    public void disconnect(String type) throws IOException {
        if (isConnector) return;
        // Send a disconnect message to the server.
        DisconnectMessage msg = new DisconnectMessage();
        msg.setType(type);
        tcp.sendObject(msg);
        udp.sendObject(msg);

        // We can disconnect now.
        thread.setKeepAlive(false);

        // GC it.
        thread = null;

        log.log(Level.INFO, "[{0}][???] Disconnected.", label);
        isConnected = false;
    }

    /**
     * Disconnect from the server.
     *
     * @param msg The custom DisconnectMessage to use.
     * @throws IOException When a disconnection error occurs.
     */
    public void disconnect(DisconnectMessage msg) throws IOException {
        if (isConnector) return;
        // Send a disconnect message to the server.
        tcp.sendObject(msg);
        udp.sendObject(msg);

        // We can disconnect now.
        thread.setKeepAlive(false);

        // GC it.
        thread = null;

        log.log(Level.INFO, "[{0}][???] Disconnected.", label);
        isConnected = false;
    }

    /**
     * Disconnect from the server with the default disconnection type:
     *  USER_REQUESTED.
     *
     * @throws IOException When a disconnection error occurs.
     */
    public void disconnect() throws IOException {
        disconnect(DisconnectMessage.USER_REQUESTED);
    }

    /**
     * Kick this client from the server, with given kick reason.
     *
     * @param reason The reason this client was kicked.
     * @throws IOException When a writing error occurs.
     */
    public void kick(String reason) throws IOException {
        if (!isConnector) return;
        DisconnectMessage message = new DisconnectMessage();
        message.setType(DisconnectMessage.KICK);
        message.setReason(reason);
        message.setReliable(true);
        send(message);

        tcp.addToDisconnectionQueue(this);

        log.log(Level.INFO, "[Server#?][???] {0} got kicked with reason: {1}.", new Object[]{this, reason});
    }

    /**
     * Kick this client from the server, with given kick reason.
     *
     * @param message The custom disconnect message.
     * @throws IOException When a writing error occurs.
     */
    public void kick(DisconnectMessage message) throws IOException {
        if (!isConnector) return;
        message.setReliable(true);
        send(message);

        tcp.addToDisconnectionQueue(this);

        log.log(Level.INFO, "[Server#?][???] {0} got kicked with reason: {1}.", new Object[]{this, message.getReason()});
    }

    private void disconnectInternal(DisconnectMessage message) throws IOException {
        DisconnectMessage dcMessage = (DisconnectMessage)message;
        String type = dcMessage.getType();
        String reason = dcMessage.getReason();

        log.log(Level.INFO, "[{0}][???] We got disconnected from the server ({1}: {2}).", new Object[]{
                label,
                type,
                reason
        });

        // We can disconnect now.
        thread.setKeepAlive(false);

        // GC it.
        thread = null;

        isConnected = false;
    }

    public List<InetAddress> discoverHosts(int port, int timeout) throws IOException {
        ArrayList<InetAddress> addresses = new ArrayList<InetAddress>();

        DatagramSocket socket = new DatagramSocket();
        ByteBuffer buffer = ByteBuffer.allocate(4);

        Serializer.writeClass(buffer, DiscoverHostMessage.class);

        buffer.flip();
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);

        for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            for (InetAddress address : Collections.list(iface.getInetAddresses())) {
                if (address instanceof Inet6Address || address.isLoopbackAddress()) continue;
                byte[] ip = address.getAddress();
                ip[3] = -1;
                socket.send(new DatagramPacket(data, data.length, InetAddress.getByAddress(ip), port));
                ip[2] = -1;
                socket.send(new DatagramPacket(data, data.length, InetAddress.getByAddress(ip), port));
            }
        }
        log.log(Level.FINE, "[{0}][UDP] Started discovery on port {1}.", new Object[]{label, port});

        long targetTime = System.currentTimeMillis() + timeout;

        DatagramPacket packet = new DatagramPacket(new byte[0], 0);
        socket.setSoTimeout(1000);
        while (System.currentTimeMillis() < targetTime) {
            try {
                socket.receive(packet);
                if (addresses.contains(packet.getAddress())) continue;
                addresses.add(packet.getAddress());
                log.log(Level.FINE, "[{0}][UDP] Discovered server on {1}.", new Object[]{label, packet.getAddress()});
            } catch (SocketTimeoutException ste) {
                // Nothing to be done here.
            }
        }

        return addresses;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    ///////////////

    // Server client related stuff.

    public void setSocketChannel(SocketChannel channel) {
        tcpChannel = channel;
    }

    public SocketChannel getSocketChannel() {
        return tcpChannel;
    }

    public void setDatagramChannel(DatagramChannel channel) {
        udpChannel = channel;
    }

    public DatagramChannel getDatagramChannel() {
        return udpChannel;
    }

    public void setDatagramReceiver(SocketAddress address) {
        udpTarget = address;
    }

    public SocketAddress getDatagramReceiver() {
        return udpTarget;
    }

    public void setTCPConnection(TCPConnection con) {
        tcp = con;
    }

    public void setUDPConnection(UDPConnection con) {
        udp = con;
    }

    public TCPConnection getTCPConnection() {
        return tcp;
    }

    public UDPConnection getUDPConnection() {
        return udp;
    }

    public MessageQueue getMessageQueue() {
        return messageQueue;
    }

    ///////////////

    /**
     * Start this client.
     */
    public void start()
    {
        new Thread(thread = new ConnectionRunnable(tcp, udp)).start();
    }

    /**
     * Start this client with given sleep time. Higher sleep times may affect the system's response time
     *  negatively, whereas lower values may increase CPU load. Use only when you're certain.
     *
     * @param sleep The sleep time.
     */
    public void start(int sleep) {
        new Thread(thread = new ConnectionRunnable(tcp, udp, sleep)).start();
    }
    
    public int getClientID() {
        return clientID;
    }

    public long getPlayerID() {
        return playerID;
    }

    public void setPlayerID(long id) {
        playerID = id;
    }

    public String toString() {
        return label;
    }

    public void addConnectionListener(ConnectionListener listener) {
        if (tcp != null) tcp.addConnectionListener(listener);
        if (udp != null) udp.addConnectionListener(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        if (tcp != null) tcp.removeConnectionListener(listener);
        if (udp != null) udp.removeConnectionListener(listener);
    }

    public void addMessageListener(MessageListener listener) {
        if (tcp != null) tcp.addMessageListener(listener);
        if (udp != null) udp.addMessageListener(listener);
    }

    public void addMessageListener(MessageListener listener, Class... classes) {
        for (Class c : classes) {
            if (tcp != null) tcp.addMessageListener(c, listener);
            if (udp != null) udp.addMessageListener(c, listener);
        }
    }

    public void removeMessageListener(MessageListener listener) {
        if (tcp != null) tcp.removeMessageListener(listener);
        if (udp != null) udp.removeMessageListener(listener);
    }

    public void removeMessageListener(MessageListener listener, Class... classes) {
        for (Class c : classes) {
            if (tcp != null) tcp.removeMessageListener(c, listener);
            if (udp != null) udp.removeMessageListener(c, listener);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Client || obj instanceof Integer)) {
            return false;
        } else if (obj instanceof Client){
            return ((Client)obj).getClientID() == getClientID();
        } else if (obj instanceof Integer) {
            return ((Integer)obj).intValue() == getClientID();
        } else {
            return false;
        }
    }
    
    protected class ClientObserver implements MessageListener, ConnectionListener {

        public void messageReceived(Message message) {
            try {
                disconnectInternal((DisconnectMessage)message);
            } catch (IOException e) {
                log.log(Level.WARNING, "[{0}][???] Could not disconnect.", label);
            }
        }
    
        public void messageSent(Message message) {
    
        }
    
        public void objectReceived(Object object) {
    
        }
    
        public void objectSent(Object object) {
    
        }
    
        public void clientConnected(Client client) {
            // We are a client. This means that we succeeded in connecting to the server.
            if (!isConnected) return;
            long time = System.currentTimeMillis();
            playerID = time;
            ClientRegistrationMessage message = new ClientRegistrationMessage();
            message.setId(time);
            try {
                message.setReliable(false);
                send(message);
                message.setReliable(true);
                send(message);
            } catch (Exception e) {
                e.printStackTrace();
                log.log(Level.SEVERE, "[{0}][???] Could not sent client registration message. Disconnecting.", label);
                try {
                    disconnect(DisconnectMessage.ERROR);
                } catch (IOException ie) {}
            }
        }

        public void clientDisconnected(Client client) {
            if (thread != null) {
                // We can disconnect now.
                thread.setKeepAlive(false);
    
                // GC it.
                thread = null;
            }
        }
    }
    
}
