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
package com.jme3.network.kernel;

import java.nio.ByteBuffer;

/**
 *  An abstract endpoint in a Kernel that can be used for
 *  sending/receiving messages within the kernel space.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public interface Endpoint
{
    /**
     *  Returns an ID that is unique for this endpoint within its
     *  Kernel instance.
     */
    public long getId();

    /**
     *  Returns the transport specific remote address of this endpoint
     *  as a string.  This may or may not be unique per endpoint depending
     *  on the type of transport. 
     */
    public String getAddress();     

    /**
     *  Returns the kernel to which this endpoint belongs.
     */
    public Kernel getKernel();    

    /**
     *  Returns true if this endpoint is currently connected.
     */
    public boolean isConnected();

    /**
     *  Sends data to the other end of the connection represented
     *  by this endpoint.
     */
    public void send( ByteBuffer data );

    /**
     *  Closes this endpoint without flushing any of its
     *  currently enqueued outbound data.
     */
    public void close();
    
    /**
     *  Closes this endpoint, optionally flushing any queued
     *  data before closing.  As soon as this method is called,
     *  new send() calls will fail with an exception... even while
     *  close() is still flushing the earlier queued messages.
     */
    public void close(boolean flushData);
}
