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
 *  A single channel remote connection allowing the sending
 *  and receiving of data.  As opposed to the Kernel, this will
 *  only ever receive data from one Endpoint and so bypasses
 *  the envelope wrapping.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public interface Connector
{
    /**
     *  Returns true if this connector is currently connected.
     */
    public boolean isConnected();

    /**
     *  Closes the connection.  Any subsequent attempts to read
     *  or write will fail with an exception.
     */
    public void close();     

    /**
     *  Returns true if there is currently data available for
     *  reading.  Some connector implementations may not be able
     *  to answer this question accurately and will always return
     *  false.
     */
    public boolean available();     
    
    /**
     *  Reads a chunk of data from the connection, blocking if
     *  there is no data available.  The buffer may only be valid
     *  until the next read() call is made.  Callers should copy
     *  the data if they need it for longer than that.
     *
     *  @return The data read or null if there is no more data
     *          because the connection is closed.
     */
    public ByteBuffer read();
    
    /**
     *  Writes a chunk of data to the connection from data.position()
     *  to data.limit().
     */
    public void write( ByteBuffer data );
}
