/*
 * Copyright (c) 2011 jMonkeyEngine
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

import java.io.IOException;
import java.net.InetAddress;

import com.jme3.network.base.DefaultClient;
import com.jme3.network.base.DefaultServer;
import com.jme3.network.kernel.tcp.SelectorKernel;
import com.jme3.network.kernel.tcp.SocketConnector;
import com.jme3.network.kernel.udp.UdpConnector;
import com.jme3.network.kernel.udp.UdpKernel;

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
     *  @param gersion  This is a game-specific verison that helps detect when out-of-date
     *                  clients have connected to an incompatible server.
     *  @param tcpPort  The port upon which the TCP hosting will listen for new connections.
     *  @param udpPort  The port upon which the UDP hosting will listen for new 'fast' UDP 
     *                  messages.
     */
    public static Server createServer( String gameName, int version, int tcpPort, int udpPort ) throws IOException
    {
        UdpKernel fast = new UdpKernel(udpPort);
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
     *  using both reliable and fast transports.  The localUdpPort specifies the
     *  local port to use for listening for incoming 'fast' UDP messages from the
     *  server.  This port is different than the host port in case the client
     *  and server are run on the same machine.
     */   
    public static Client connectToServer( String host, int hostPort, int localUdpPort ) throws IOException
    {
        return connectToServer( DEFAULT_GAME_NAME, DEFAULT_VERSION, host, hostPort, hostPort, localUdpPort );   
    }

    /**
     *  Creates a Client that communicates with the specified host and port
     *  using both reliable and fast transports.  The localUdpPort specifies the
     *  local port to use for listening for incoming 'fast' UDP messages from the
     *  server.  This port is different than the host port in case the client
     *  and server are run on the same machine.
     */   
    public static Client connectToServer( String host, int hostPort, int remoteUdpPort, 
                                          int localUdpPort ) throws IOException
    {
        return connectToServer( DEFAULT_GAME_NAME, DEFAULT_VERSION, host, hostPort, remoteUdpPort,
                                localUdpPort );
    }

    /**
     *  Creates a Client that communicates with the specified host and port
     *  using both reliable and fast transports.  The localUdpPort specifies the
     *  local port to use for listening for incoming 'fast' UDP messages from the
     *  server.  This port is different than the host port in case the client
     *  and server are run on the same machine.
     */   
    public static Client connectToServer( String gameName, int version, 
                                          String host, int hostPort, int localUdpPort ) throws IOException
    {
        return connectToServer( host, hostPort, hostPort, localUdpPort );   
    }

    /**
     *  Creates a Client that communicates with the specified host and port
     *  using both reliable and fast transports.  The localUdpPort specifies the
     *  local port to use for listening for incoming 'fast' UDP messages from the
     *  server.  This port is different than the host port in case the client
     *  and server are run on the same machine.
     */   
    public static Client connectToServer( String gameName, int version, 
                                          String host, int hostPort, int remoteUdpPort, 
                                          int localUdpPort ) throws IOException
    {
        InetAddress remoteAddress = InetAddress.getByName(host);   
        UdpConnector fast = new UdpConnector( localUdpPort, remoteAddress, hostPort ); 
        SocketConnector reliable = new SocketConnector( remoteAddress, hostPort );        
       
        return new DefaultClient( gameName, version, reliable, fast );
    }
 
 
    protected static class NetworkClientImpl extends DefaultClient implements NetworkClient
    {
        public NetworkClientImpl(String gameName, int version)
        {
            super( gameName, version );
        }
        
        public void connectToServer( String host, int port, int remoteUdpPort, 
                                     int localUdpPort ) throws IOException
        {
            connectToServer( InetAddress.getByName(host), port, remoteUdpPort, localUdpPort );
        }                                     
                                 
        public void connectToServer( InetAddress address, int port, int remoteUdpPort, 
                                     int localUdpPort ) throws IOException
        {
            UdpConnector fast = new UdpConnector( localUdpPort, address, port ); 
            SocketConnector reliable = new SocketConnector( address, port );        
            
            setConnectors( reliable, fast );
        }                                             
    }   
}
