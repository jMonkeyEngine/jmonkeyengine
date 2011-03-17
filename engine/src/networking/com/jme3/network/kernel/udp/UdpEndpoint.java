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

package com.jme3.network.kernel.udp;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

import com.jme3.network.kernel.*;


/**
 *  Endpoint implementation that encapsulates the
 *  UDP connection information for return messaging,
 *  identification of envelope sources, etc.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class UdpEndpoint implements Endpoint
{
    private long id;
    private SocketAddress address;
    private DatagramSocket socket;
    private UdpKernel kernel;

    public UdpEndpoint( UdpKernel kernel, long id, SocketAddress address, DatagramSocket socket )
    {
        this.id = id;
        this.address = address;
        this.socket = socket;
        this.kernel = kernel;
    }

    public Kernel getKernel()
    {
        return kernel;
    }

    protected SocketAddress getRemoteAddress()
    {
        return address;
    }

    public void close()
    {
        try {
            kernel.closeEndpoint(this);
        } catch( IOException e ) {
            throw new KernelException( "Error closing endpoint for socket:" + socket, e );
        }
    }

    public long getId()
    {
        return id;
    }

    public boolean isConnected()
    {
        return socket.isConnected();
    }

    public void send( ByteBuffer data )
    {
        try {
            DatagramPacket p = new DatagramPacket( data.array(), data.position(), 
                                                   data.remaining(), address );
            socket.send(p);
        } catch( IOException e ) {
            throw new KernelException( "Error sending datagram to:" + address, e );
        }
    }

    public String toString()
    {
        return "UdpEndpoint[" + id + ", " + socket + "]";
    }
}
