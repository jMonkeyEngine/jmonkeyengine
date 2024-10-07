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
package com.jme3.network.kernel.udp;

import com.jme3.network.kernel.Connector;
import com.jme3.network.kernel.ConnectorException;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 *  A straight forward datagram socket-based UDP connector 
 *  implementation.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class UdpConnector implements Connector
{
    private DatagramSocket sock = new DatagramSocket();
    private SocketAddress remoteAddress;
    private byte[] buffer = new byte[65535];
    private AtomicBoolean connected = new AtomicBoolean(false);

    /**
     *  Creates a new UDP connection that send datagrams to the
     *  specified address and port.
     */
    public UdpConnector( InetAddress remote, int remotePort ) throws IOException
    {
        InetSocketAddress localSocketAddress = new InetSocketAddress(0);
        this.sock = new DatagramSocket( localSocketAddress );
        remoteAddress = new InetSocketAddress( remote, remotePort );
        
        // Setup to receive only from the remote address
        sock.connect( remoteAddress );
 
        connected.set(true);
    }
 
    protected void checkClosed()
    {
        if( sock == null )
            throw new ConnectorException( "Connection is closed:" + remoteAddress );
    }
     
    @Override
    public boolean isConnected()
    {
        if( sock == null )
            return false;
        return sock.isConnected();
    }

    @Override
    public void close()
    {
        checkClosed();
        DatagramSocket temp = sock;
        sock = null;
        connected.set(false);            
        temp.close();
    }     

    /**
     *  This always returns false since the simple DatagramSocket usage
     *  cannot be run in a non-blocking way.
     */
    @Override
    public boolean available()
    {
        // It would take a separate thread or an NIO Selector based implementation to get this
        // to work.  If a polling strategy is never employed by callers then it doesn't
        // seem worth it to implement all of that just for this method.
        checkClosed();
        return false;
    }     
    
    @Override
    public ByteBuffer read()
    {
        checkClosed();
        try {
            DatagramPacket packet = new DatagramPacket( buffer, buffer.length );
            sock.receive(packet);
            
            // Wrap it in a ByteBuffer for the caller
            return ByteBuffer.wrap( buffer, 0, packet.getLength() ); 
        } catch( IOException e ) {
            if( !connected.get() ) {
                // Nothing to see here... just move along
                return null;
            }        
            throw new ConnectorException( "Error reading from connection to:" + remoteAddress, e );    
        }                
    }
    
    @Override
    public void write( ByteBuffer data )
    {
        checkClosed();
        try {
            DatagramPacket p = new DatagramPacket( data.array(), data.position(), data.remaining(), 
                                                   remoteAddress );
            sock.send(p);
        } catch( IOException e ) {
            throw new ConnectorException( "Error writing to connection:" + remoteAddress, e );
        }
    }    
}

