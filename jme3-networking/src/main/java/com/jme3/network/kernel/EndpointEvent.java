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
 *  Provides information about an added or
 *  removed connection.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class EndpointEvent
{
    public enum Type { ADD, REMOVE };

    private Kernel source;
    private Endpoint endpoint;
    private Type type;

    public EndpointEvent( Kernel source, Endpoint p, Type type )
    {
        this.source = source;
        this.endpoint = p;
        this.type = type;
    }
    
    public static EndpointEvent createAdd( Kernel source, Endpoint p )
    {
        return new EndpointEvent( source, p, Type.ADD );
    }

    public static EndpointEvent createRemove( Kernel source, Endpoint p )
    {
        return new EndpointEvent( source, p, Type.REMOVE );
    }
    
    public Kernel getSource()
    {
        return source;
    }
    
    public Endpoint getEndpoint()
    {
        return endpoint;
    }
    
    public Type getType()
    {
        return type;
    }
    
    @Override
    public String toString()
    {
        return "EndpointEvent[" + type + ", " + endpoint + "]";
    } 
}
