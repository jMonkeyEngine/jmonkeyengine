/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.network;

import com.jme3.network.base.DefaultClient;
import com.jme3.network.base.DefaultServer;
import com.jme3.network.base.TcpConnectorFactory;
import com.jme3.network.kernel.tcp.SelectorKernel;
import com.jme3.network.kernel.tcp.SocketConnector;
import com.jme3.network.kernel.udp.UdpConnector;
import com.jme3.network.kernel.udp.UdpKernel;
import java.io.IOException;
import java.net.InetAddress;

/**
 *  The main service provider for conveniently creating
 *  server and client instances.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class Network
{
    public static final String DEFAULT_GAME_NAME = "Unnamed jME3 Game";
    public static final int DEFAULT_VERSION = 42;

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Network() {
    }

    /**
     *  Creates a Server that will utilize both reliable and fast
     *  transports to communicate with clients.  The specified port
     *  will be used for both TCP and UDP communication.
     */
    public static Server createServer( int port ) throws IOException
    {   
        return createServer( DEFAULT_GAME_NAME, DEFAULT_VERSION, port, port );
    }

    /**
     *  Creates a Server that will utilize both reliable and fast
     *  transports to communicate with clients.  The specified port
     *  will be used for both TCP and UDP communication.
     */
    public static Server createServer( int tcpPort, int udpPort ) throws IOException
    {
        return createServer( DEFAULT_GAME_NAME, DEFAULT_VERSION, tcpPort, udpPort );
    }

    /**
     *  Creates a named and versioned Server that will utilize both reliable and fast
     *  transports to communicate with clients.  The specified port
     *  will be used for both TCP and UDP communication.
     *
     *  @param gameName This is the name that identifies the game.  Connecting clients
     *                  must use this name or be turned away.
     *  @param version  This is a game-specific version that helps detect when out-of-date
     *                  clients have connected to an incompatible server.
     *  @param tcpPort  The port upon which the TCP hosting will listen for new connections.
     *  @param udpPort  The port upon which the UDP hosting will listen for new 'fast' UDP 
     *                  messages.  Set to -1 if 'fast' traffic should go over TCP.  This will
     *                  completely disable UDP traffic for this server.
     */
    public static Server createServer( String gameName, int version, int tcpPort, int udpPort ) throws IOException
    {
        UdpKernel fast = udpPort == -1 ? null : new UdpKernel(udpPort);
        SelectorKernel reliable = new SelectorKernel(tcpPort);
 
        return new DefaultServer( gameName, version, reliable, fast );       
    }
    
    /**
     *  Creates a client that can be connected at a later time.
     */
    public static NetworkClient createClient()
    {
        return createClient( DEFAULT_GAME_NAME, DEFAULT_VERSION );
    }     

    /**
     *  Creates a client that can be connected at a later time.  The specified
     *  game name and version must match the server or the client will be turned
     *  away.
     */
    public static NetworkClient createClient( String gameName, int version )
    {
        return new NetworkClientImpl(gameName, version);
    }     
    
    /**
     *  Creates a Client that communicates with the specified host and port
     *  using both reliable and fast transports. 
     */   
    public static Client connectToServer( String host, int hostPort ) throws IOException
    {
        return connectToServer( DEFAULT_GAME_NAME, DEFAULT_VERSION, host, hostPort, hostPort );   
    }

    /**
     *  Creates a Client that communicates with the specified host and separate TCP and UDP ports
     *  using both reliable and fast transports.
     */   
    public static Client connectToServer( String host, int hostPort, int remoteUdpPort ) throws IOException
    {
        return connectToServer( DEFAULT_GAME_NAME, DEFAULT_VERSION, host, hostPort, remoteUdpPort );
    }

    /**
     *  Creates a Client that communicates with the specified host and port
     *  using both reliable and fast transports.  
     */   
    public static Client connectToServer( String gameName, int version, 
                                          String host, int hostPort ) throws IOException
    {
        return connectToServer( gameName, version, host, hostPort, hostPort );   
    }

    /**
     *  Creates a Client that communicates with the specified host and separate TCP and UDP ports
     *  using both reliable and fast transports.  
     *  
     *  @param gameName This is the name that identifies the game.  This must match
     *                  the target server's name or this client will be turned away.
     *  @param version  This is a game-specific version that helps detect when out-of-date
     *                  clients have connected to an incompatible server.  This must match
     *                  the server's version of this client will be turned away.
     *  @param hostPort  The remote TCP port on the server to which this client should
     *                  send reliable messages. 
     *  @param remoteUdpPort  The remote UDP port on the server to which this client should
     *                  send 'fast'/unreliable messages.   Set to -1 if 'fast' traffic should 
     *                  go over TCP.  This will completely disable UDP traffic for this
     *                  client.
     */   
    public static Client connectToServer( String gameName, int version, 
                                          String host, int hostPort, int remoteUdpPort ) throws IOException
    {
        InetAddress remoteAddress = InetAddress.getByName(host);   
        UdpConnector fast = remoteUdpPort == -1 ? null : new UdpConnector( remoteAddress, remoteUdpPort ); 
        SocketConnector reliable = new SocketConnector( remoteAddress, hostPort );        
       
        return new DefaultClient( gameName, version, reliable, fast, new TcpConnectorFactory(remoteAddress) );
    }
 
 
    protected static class NetworkClientImpl extends DefaultClient implements NetworkClient
    {
        public NetworkClientImpl(String gameName, int version)
        {
            super( gameName, version );
        }
        
        @Override
        public void connectToServer( String host, int port, int remoteUdpPort ) throws IOException
        {
            connectToServer( InetAddress.getByName(host), port, remoteUdpPort );
        }                                     
                                 
        @Override
        public void connectToServer( InetAddress address, int port, int remoteUdpPort ) throws IOException
        {
            UdpConnector fast = new UdpConnector( address, remoteUdpPort ); 
            SocketConnector reliable = new SocketConnector( address, port );        
            
            setPrimaryConnectors( reliable, fast, new TcpConnectorFactory(address) );
        }                                             
    }   
}
