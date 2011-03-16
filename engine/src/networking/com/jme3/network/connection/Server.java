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
import com.jme3.network.message.Message;
import com.jme3.network.service.ServiceManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class where your SpiderMonkey adventures start. The server class
 *  manages the TCP and UDP servers.
 *
 * Using the constructors where you either provide ports or the instances,
 *  they will bind automatically. If you do not want this to happen, use the
 *  no arg constructor, and then call bind later on.
 *
 * @author Lars Wesselius
 */
public class Server extends ServiceManager {
    protected Logger        log = Logger.getLogger(Server.class.getName());

    protected static int    serverIDCounter = 0;
    protected TCPConnection tcp = null;
    protected UDPConnection udp = null;

    protected String        label;
    protected int           serverID;
    protected boolean       isBound = false;

    protected ConnectionRunnable
                            thread;

    protected SocketAddress lastUDPAddress;
    protected SocketAddress lastTCPAddress;

    protected ClientManager clientManager = new ClientManager();

    protected MessageListener listener = new ServerMessageObserver();

    /**
     * Default constructor. Sets the label to
     * <code>Server#[serverID]</code>
     */
    public Server() {
        super(ServiceManager.SERVER);
        serverID = ++serverIDCounter;
        this.label = "Server#" + serverID;
    }

    /**
     * Constructor providing custom instances of the servers and its addresses.
     *
     * @param tcp The TCPConnection instance to manage.
     * @param udp The UDPConnection instance to manage.
     * @param tcpAddress The TCP address to bind to.
     * @param udpAddress The UDP address to bind to.
     * @throws IOException When a bind error has occurred.
     */
    public Server(TCPConnection tcp, UDPConnection udp, SocketAddress tcpAddress, SocketAddress udpAddress) throws IOException {
        this();

        this.tcp = tcp;
        tcp.bind(tcpAddress);
        lastTCPAddress = tcpAddress;

        this.udp = udp;
        udp.bind(udpAddress);
        lastUDPAddress = udpAddress;
        isBound = true;

        registerInternalListeners();
    }

    /**
     * Constructor for providing a TCP server instance. UDP will be disabled.
     *
     * @param tcp The TCPConnection instance.
     * @param tcpAddress The address to bind to.
     * @throws IOException When a binding error occurs.
     */
    public Server(TCPConnection tcp, SocketAddress tcpAddress) throws IOException {
        this();

        this.tcp = tcp;
        tcp.bind(tcpAddress);
        lastTCPAddress = tcpAddress;
        isBound = true;

        registerInternalListeners();
    }

    /**
     * Constructor for providing a UDP server instance. TCP will be disabled.
     *
     * @param udp The UDP server instance.
     * @param udpAddress The address to bind to.
     * @throws IOException When a binding error occurs.
     */
    public Server(UDPConnection udp, SocketAddress udpAddress) throws IOException {
        this();

        this.udp = udp;
        udp.bind(udpAddress);
        lastUDPAddress = udpAddress;
        isBound = true;

        registerInternalListeners();
    }

    /**
     * Simple constructor for providing TCP port and UDP port. Will bind using on
     *  all interfaces, on given ports.
     *
     * @param tcpPort The TCP port to use.
     * @param udpPort The UDP port to use.
     * @throws IOException When a binding error occurs.
     */
    public Server(int tcpPort, int udpPort) throws IOException {
        this();

        tcp = new TCPConnection(label);

        lastTCPAddress = new InetSocketAddress(tcpPort);
        tcp.bind(lastTCPAddress);

        lastUDPAddress = new InetSocketAddress(udpPort);
        udp = new UDPConnection(label);
        udp.bind(lastUDPAddress);
        isBound = true;

        registerInternalListeners();
    }

    private void registerInternalListeners() {
        if (tcp != null) {
            tcp.addMessageListener(DisconnectMessage.class, listener);
            tcp.addMessageListener(ClientRegistrationMessage.class, clientManager);
            tcp.addConnectionListener(clientManager);
        }
        if (udp != null) {
            udp.addMessageListener(DisconnectMessage.class, listener);
            udp.addMessageListener(ClientRegistrationMessage.class, clientManager);
            udp.addConnectionListener(clientManager);
        }
    }

    /**
     * Bind method for when the no arg constructor was used.
     *
     * @param tcpPort The TCP port to use. To turn off, use -1.
     * @param udpPort The UDP port to use. To turn off, use -1.
     * @throws IllegalArgumentException When an illegal argument was given.
     * @throws java.io.IOException When a binding error occurs.
     */
    public void bind(int tcpPort, int udpPort) throws IllegalArgumentException, IOException {
        if (tcpPort == -1 && udpPort == -1) throw new IllegalArgumentException("No point in binding when you want to turn both the connections off.");

        if (tcpPort != -1) {
            lastTCPAddress = new InetSocketAddress(tcpPort);
            tcp.bind(lastTCPAddress);
        }
        if (udpPort != -1) {
            lastUDPAddress = new InetSocketAddress(udpPort);
            udp.bind(lastUDPAddress);
        }
        registerInternalListeners();
        isBound = true;
    }

    /**
     * Broadcast a message.
     *
     * @param message The message to broadcast.
     * @throws IOException When a writing error occurs.
     */
    public void broadcast(Message message) throws IOException {
        if (!isBound) throw new IOException("Not bound yet. Use bind() first.");
        if (message.isReliable()) {
            if (tcp == null) throw new IOException("No TCP server.");
            tcp.sendObject(message);
        } else {
            if (udp == null) throw new IOException("No UDP server.");
            udp.sendObject(message);
        }
    }

    /**
     * Broadcast a message, except to the given client.
     *
     * @param except The client to refrain from sending the message to.
     * @param message The message to send.
     * @throws IOException When a writing error occurs.
     */
    public void broadcastExcept(Client except, Message message) throws IOException {
        if (!isBound) throw new IOException("Not bound yet. Use bind() first.");

        // We don't have to check for reliable or not here, since client.send does that.
        for (Client con : clientManager.getConnectors()) {
            if (con == except) continue;
            con.send(message);
        }
    }

    /**
     * Start this server.
     *
     * @throws IOException When an error occurs.
     */
    public void start() throws IOException {
        if (!isBound) {
            tcp.bind(lastTCPAddress);
            udp.bind(lastUDPAddress);
        }
        new Thread(thread = new ConnectionRunnable(tcp, udp)).start();
        log.log(Level.INFO, "[{0}][???] Started server.", label);
    }

    /**
     * Start this server with given sleep time. Higher sleep times may affect the system's response time
     *  negatively, whereas lower values may increase CPU load. Use only when you're certain.
     *
     * @param sleep The sleep time.
     * @throws IOException When an error occurs.
     */
    public void start(int sleep) throws IOException {
        if (!isBound) {
            tcp.bind(lastTCPAddress);
            udp.bind(lastUDPAddress);
        }
        new Thread(thread = new ConnectionRunnable(tcp, udp, sleep)).start();
        log.log(Level.INFO, "[{0}][???] Started server.", label);
    }

    /**
     * Stop this server. Note that it kicks all clients so that they can
     *  gracefully quit.
     *
     * @throws IOException When a writing error occurs.
     */
    public void stop() throws IOException {
        stop(new DisconnectMessage());
    }

    /**
     * Stops the server with custom message.
     *
     * @throws IOException When a writing error occurs.
     */
    public void stop(DisconnectMessage message) throws IOException {
        log.log(Level.INFO, "[{0}][???] Server is shutting down..", label);
        if (message.getReason() == null) {
            message.setReason("Server shut down.");
        }
        if (message.getType() == null) {
            message.setType(DisconnectMessage.KICK);
        }
        message.setReliable(true);

        broadcast(message);

        for (Client client : getConnectors()) {
            tcp.addToDisconnectionQueue(client);
        }

        tcp.selector.wakeup();
        log.log(Level.FINE, "[{0}][???] Sent disconnection messages to all clients.", label);

        thread.setKeepAlive(false);
        thread = null;
        log.log(Level.INFO, "[{0}][???] Server shut down.", label);
        isBound = false;
    }

    public boolean isRunning() {
        return thread != null && thread.isRunning();
    }

    public int getServerID() {
        return serverID;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String toString() {
        return label;
    }

    /////////////////////////////// Connection management //////////////////////////

    /**
     * Get all the connectors for the TCP connection.
     *
     * @return A unmodifiable list with the connectors.
     */
    public List<Client> getTCPConnectors() {
        if (tcp != null) return tcp.getConnectors();
        return null;
    }

    /**
     * Get all the connectors for the UDP connection.
     *
     * @return A unmodifiable list with the connectors.
     */
    public List<Client> getUDPConnectors() {
        if (udp != null) return udp.getConnectors();
        return null;
    }

    /**
     * Get the combined connectors, meaning TCP and UDP are combined into one client. You should
     *  generally use this for clients.
     *
     * @return A unmodifiable list with the connectors.
     */
    public List<Client> getConnectors() {
        return clientManager.getConnectors();
    }

    /**
     * Get a specific client based on the provided clientID.
     * @param clientID The clientID identifying the client requested.
     * @return The located client or null if the client was not on the list.
     */
    public Client getClientByID(int clientID) {
        Client c = clientManager.getClientByClientID(clientID);
        return c;
    }
    
    /////////////////////////////// Connector filters //////////////////////////////

    public void addConnectorFilter(ConnectorFilter filter) {
        if (tcp != null) tcp.addConnectorFilter(filter);
        if (udp != null) udp.addConnectorFilter(filter);
    }

    public void removeConnectorFilter(ConnectorFilter filter) {
        if (tcp != null) tcp.removeConnectorFilter(filter);
        if (udp != null) udp.removeConnectorFilter(filter);
    }

    /////////////////////////////// Listener related ///////////////////////////////

    public void addLocalConnectionListener(ConnectionListener listener) {
        if (tcp != null) tcp.addConnectionListener(listener);
        if (udp != null) udp.addConnectionListener(listener);
    }

    public void removeLocalConnectionListener(ConnectionListener listener) {
        if (tcp != null) tcp.removeConnectionListener(listener);
        if (udp != null) udp.removeConnectionListener(listener);
    }

    public void addConnectionListener(ConnectionListener listener) {
        clientManager.addConnectionListener(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        clientManager.removeConnectionListener(listener);
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

    protected class ServerMessageObserver implements MessageListener {
    
        public void messageReceived(Message message) {
            // Right now, this is definitely a DisconnectMessage.
            DisconnectMessage dcMessage = (DisconnectMessage)message;
            Client client = dcMessage.getClient();
    
            if (clientManager.isClientConnected(client)) {
                log.log(Level.INFO, "[{0}][???] Client {1} disconnected ({2}: {3}).", new Object[]{
                        label,
                        client,
                        dcMessage.getType(),
                        (dcMessage.getReason() != null) ? dcMessage.getReason() : "No description"
                });
            }
            dcMessage.getConnection().addToDisconnectionQueue(client);
        }
    
        public void messageSent(Message message) {
    
        }

        public void objectReceived(Object object) {
    
        }

        public void objectSent(Object object) {
    
        }
    }        
}
