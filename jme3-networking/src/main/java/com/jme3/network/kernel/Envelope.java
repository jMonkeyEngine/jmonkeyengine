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
package com.jme3.network.kernel;

/**
 *  Encapsulates a received piece of data.  This is used by the Kernel
 *  to track incoming chunks of data.  
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class Envelope
{
    private Endpoint source;  
    private byte[] data;
    private boolean reliable;
    
    /**
     *  Creates an incoming envelope holding the data from the specified
     *  source.  The 'reliable' flag further indicates on which mode of
     *  transport the data arrived.
     */
    public Envelope( Endpoint source, byte[] data, boolean reliable )
    {
        this.source = source;
        this.data = data;
        this.reliable = reliable;
    }
    
    public Endpoint getSource()
    {
        return source;
    }
    
    public byte[] getData()
    {
        return data;
    }
    
    public boolean isReliable()
    {
        return reliable;
    }
    
    @Override
    public String toString()
    {
        return "Envelope[" + source + ", " + (reliable?"reliable":"unreliable") + ", " + data.length + "]";
    }
}
