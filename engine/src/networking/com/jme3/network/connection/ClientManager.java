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
import com.jme3.network.events.MessageAdapter;
import com.jme3.network.message.ClientRegistrationMessage;
import com.jme3.network.message.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

/**
 * The ClientManager is an internal class that deals with client registrations and disconnects.
 *
 * @author Lars Wesselius
 */
public class ClientManager extends MessageAdapter implements ConnectionListener {
    protected Logger log = Logger.getLogger(ClientManager.class.getName());
    private ArrayList<Client> clients = new ArrayList<Client>();
    private Hashtable<Integer, Client> clientsByClientID = new Hashtable<Integer, Client>();
    
    private ArrayList<ClientRegistrationMessage> pendingMessages = new ArrayList<ClientRegistrationMessage>();

    private ArrayList<ConnectionListener> connectionListeners = new ArrayList<ConnectionListener>();

    private ClientRegistrationMessage findMessage(long playerId) {
        for (ClientRegistrationMessage message : pendingMessages) {
            if (message.getId() == playerId) {
                return message;
            }
        }
        return null;
    }

    public List<Client> getConnectors() {
        return Collections.unmodifiableList(clients);
    }

    public Client getClient(long playerId) {
        for (Client client : clients) {
            if (client.getPlayerID() == playerId) return client;
        }
        return null;
    }

    public Client getClientByClientID(int clientID) {
        return clientsByClientID.get(clientID);
    }

    public boolean isClientConnected(Client client) {
        return clients.contains(client);
    }


    @Override
    public void messageReceived(Message message) {
        ClientRegistrationMessage regMessage = (ClientRegistrationMessage)message;
        ClientRegistrationMessage existingMessage = findMessage(regMessage.getId());

        // Check if message exists, if not add this message to the pending queue.
        if (existingMessage == null) {
            pendingMessages.add(regMessage);
            return;
        }

        // We've got two messages of which we can construct a client.
        Client client = new Client(true);

        Connection conOne = regMessage.getConnection();
        Connection conTwo = existingMessage.getConnection();

        if (conOne instanceof TCPConnection) {
            fillInTCPInfo(client, regMessage);
        } else if (conOne instanceof UDPConnection) {
            fillInUDPInfo(client, regMessage);
        }

        if (conTwo instanceof TCPConnection) {
            fillInTCPInfo(client, existingMessage);
        } else if (conTwo instanceof UDPConnection) {
            fillInUDPInfo(client, existingMessage);
        }

        if (client.getUDPConnection() == null || client.getTCPConnection() == null) {
            // Something went wrong in this registration.
            log.severe("[ClientManager][???] Something went wrong in the client registration process.");
            return;
        }

        client.setPlayerID(regMessage.getId());

        // Set other clients to this playerID as well.
        regMessage.getClient().setPlayerID(regMessage.getId());
        existingMessage.getClient().setPlayerID(regMessage.getId());


        fireClientConnected(client);

        // Remove pending message.
        pendingMessages.remove(existingMessage);
        clients.add(client);
        clientsByClientID.put(client.getClientID(), client);
    }

    private void fillInUDPInfo(Client client, ClientRegistrationMessage msg) {
        client.setUDPConnection((UDPConnection)msg.getConnection());
        client.setDatagramReceiver(msg.getClient().getDatagramReceiver());
        client.setDatagramChannel(msg.getClient().getDatagramChannel());

        client.getDatagramChannel().keyFor(msg.getConnection().selector).attach(client);
    }

    private void fillInTCPInfo(Client client, ClientRegistrationMessage msg) {
        client.setSocketChannel(msg.getClient().getSocketChannel());
        client.setTCPConnection((TCPConnection)msg.getConnection());

        client.getSocketChannel().keyFor(msg.getConnection().selector).attach(client);
    }

    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    public void clientConnected(Client client) {
    }

    public void clientDisconnected(Client client) {
        if (clients.contains(client)) {
            clients.remove(client);
            fireClientDisconnected(client);
        }
    }

    public void fireClientConnected(Client client) {
        for (ConnectionListener listener : connectionListeners) {
            listener.clientConnected(client);
        }
    }

    public void fireClientDisconnected(Client client) {
        for (ConnectionListener listener : connectionListeners) {
            listener.clientDisconnected(client);
        }
    }
}
