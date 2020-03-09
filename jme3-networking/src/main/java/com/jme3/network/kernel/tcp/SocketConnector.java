/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package com.jme3.network.kernel.tcp;

import com.jme3.network.kernel.Connector;
import com.jme3.network.kernel.ConnectorException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 *  A straight forward socket-based connector implementation that
 *  does not use any separate threading.  It relies completely on
 *  the buffering in the OS network layer.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class SocketConnector implements Connector
{
    private Socket sock;
    private InputStream in;
    private OutputStream out;
    private SocketAddress remoteAddress;
    private byte[] buffer = new byte[65535];
    private AtomicBoolean connected = new AtomicBoolean(false);

    public SocketConnector( InetAddress address, int port ) throws IOException
    {
        this.sock = new Socket(address, port);
        remoteAddress = sock.getRemoteSocketAddress(); // for info purposes 
        
        // Disable Nagle's buffering so data goes out when we
        // put it there.
        sock.setTcpNoDelay(true);
        
        in = sock.getInputStream();
        out = sock.getOutputStream();
        
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
        try {
            Socket temp = sock;
            sock = null;            
            connected.set(false);
            temp.close();
        } catch( IOException e ) {            
            throw new ConnectorException( "Error closing socket for:" + remoteAddress, e );
        }            
    }     

    @Override
    public boolean available()
    {
        checkClosed();
        try {
            return in.available() > 0;
        } catch( IOException e ) {
            throw new ConnectorException( "Error retrieving data availability for:" + remoteAddress, e );
        }       
    }     
    
    @Override
    public ByteBuffer read()
    {
        checkClosed();
        
        try {
            // Read what we can
            int count = in.read(buffer);
            if( count < 0 ) {
                // Socket is closed
                close();
                return null;
            }

            // Wrap it in a ByteBuffer for the caller
            return ByteBuffer.wrap( buffer, 0, count ); 
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
            out.write(data.array(), data.position(), data.remaining());
        } catch( IOException e ) {
            throw new ConnectorException( "Error writing to connection:" + remoteAddress, e );
        }
    }   
    
}
