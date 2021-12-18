/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

/**
 *  A Client whose network connection information can 
 *  be provided post-creation.  The actual connection stack
 *  will be setup the same as if Network.connectToServer
 *  had been called.  
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public interface NetworkClient extends Client
{
    /**
     *  Connects this client to the specified remote server and ports.
     */
    public void connectToServer( String host, int port, int remoteUdpPort ) throws IOException;
 
    /**
     *  Connects this client to the specified remote server and ports.
     *  
     *  @param address  The host's Internet address.
     *  @param port  The remote TCP port on the server to which this client should
     *                  send reliable messages. 
     *  @param remoteUdpPort  The remote UDP port on the server to which this client should
     *                  send 'fast'/unreliable messages.   Set to -1 if 'fast' traffic should 
     *                  go over TCP.  This will completely disable UDP traffic for this
     *                  client.
     */                               
    public void connectToServer( InetAddress address, int port, int remoteUdpPort ) throws IOException;
    
}
