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
package com.jme3.network.kernel;

import com.jme3.network.Filter;
import java.nio.ByteBuffer;

/**
 *  Defines the basic byte[] passing messaging
 *  kernel.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public interface Kernel
{
    /**
     *  A marker envelope returned from read() that indicates that
     *  there are events pending.  This allows a single thread to
     *  more easily process the envelopes and endpoint events.
     */
    public static final Envelope EVENTS_PENDING = new Envelope( null, new byte[0], false );     

    /**
     *  Initializes the kernel and starts any internal processing.
     */
    public void initialize();
    
    /**
     *  Gracefully terminates the kernel and stops any internal 
     *  daemon processing.  This method will not return until all
     *  internal threads have been shut down.
     */
    public void terminate() throws InterruptedException;

    /**
     *  Dispatches the data to all endpoints managed by the
     *  kernel that match the specified endpoint filter.
     *  If 'copy' is true then the implementation will copy the byte buffer
     *  before delivering it to endpoints.  This allows the caller to reuse
     *  the data buffer.  Though it is important that the buffer not be changed
     *  by another thread while this call is running.
     *  Only the bytes from data.position() to data.remaining() are sent.  
     */ 
    public void broadcast( Filter<? super Endpoint> filter, ByteBuffer data, boolean reliable, 
                           boolean copy );
 
    /**
     *  Returns true if there are waiting envelopes.
     */   
    public boolean hasEnvelopes();
 
    /**
     *  Removes one envelope from the received messages queue or
     *  blocks until one is available.
     */   
    public Envelope read() throws InterruptedException;
    
    /**
     *  Removes and returns one endpoint event from the event queue or
     *  null if there are no endpoint events.     
     */
    public EndpointEvent nextEvent();     
}
