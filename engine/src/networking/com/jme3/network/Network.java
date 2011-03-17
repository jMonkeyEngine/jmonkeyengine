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
    /**
     *  Creates a Server that will utilize both reliable and fast
     *  transports to communicate with clients.  The specified port
     *  will be used for both TCP and UDP communication.
     */
    public static Server createServer( int port ) throws IOException
    {   
        return createServer( port, port );
    }

    /**
     *  Creates a Server that will utilize both reliable and fast
     *  transports to communicate with clients.  The specified port
     *  will be used for both TCP and UDP communication.
     */
    public static Server createServer( int tcpPort, int udpPort ) throws IOException
    {
        //InetAddress local = InetAddress.getLocalHost();
        
        UdpKernel fast = new UdpKernel(udpPort);
        SelectorKernel reliable = new SelectorKernel(tcpPort);
 
        return new DefaultServer( reliable, fast );       
    }
    
    /**
     *  Creates a client that can be connected at a later time.
     */
    public static NetworkClient createClient()
    {
        return new NetworkClientImpl();
    }     
    
    /**
     *  Creates a Client that communicates with the specified host and port
     *  using both reliable and fast transports.  The localUdpPort specifies the
     *  local port to use for listening for incoming 'fast' UDP messages.
     */   
    public static Client connectToServer( String host, int hostPort, int localUdpPort ) throws IOException
    {
        return connectToServer( InetAddress.getByName(host), hostPort, hostPort, localUdpPort );   
    }

    /**
     *  Creates a Client that communicates with the specified host and port
     *  using both reliable and fast transports.  The localUdpPort specifies the
     *  local port to use for listening for incoming 'fast' UDP messages.
     */   
    public static Client connectToServer( String host, int hostPort, int remoteUdpPort, 
                                          int localUdpPort ) throws IOException
    {
        return connectToServer( InetAddress.getByName(host), hostPort, remoteUdpPort, localUdpPort );   
    }
 
    /**
     *  Creates a Client that communicates with the specified address and port
     *  using both reliable and fast transports.  The localUdpPort specifies the
     *  local port to use for listening for incoming 'fast' messages.
     */   
    public static Client connectToServer( InetAddress address, int port, int localUdpPort ) throws IOException
    {
        return connectToServer( address, port, port, localUdpPort );
    }
    
    /**
     *  Creates a Client that communicates with the specified address and port
     *  using both reliable and fast transports.  The localUdpPort specifies the
     *  local port to use for listening for incoming 'fast' messages.
     */   
    public static Client connectToServer( InetAddress address, int port, int remoteUdpPort, 
                                          int localUdpPort ) throws IOException
    {
        InetAddress local = InetAddress.getLocalHost();
        UdpConnector fast = new UdpConnector( local, localUdpPort, address, port ); 
        SocketConnector reliable = new SocketConnector( address, port );        
       
        return new DefaultClient( reliable, fast );
    }
 
    protected static class NetworkClientImpl extends DefaultClient implements NetworkClient
    {
        public NetworkClientImpl()
        {
        }
        
        public void connectToServer( String host, int port, int remoteUdpPort, 
                                     int localUdpPort ) throws IOException
        {
            connectToServer( InetAddress.getByName(host), port, remoteUdpPort, localUdpPort );
        }                                     
                                 
        public void connectToServer( InetAddress address, int port, int remoteUdpPort, 
                                     int localUdpPort ) throws IOException
        {
            InetAddress local = InetAddress.getLocalHost();
            UdpConnector fast = new UdpConnector( local, localUdpPort, address, port ); 
            SocketConnector reliable = new SocketConnector( address, port );        
            
            setConnectors( reliable, fast );
        }                                             
    }   
}
