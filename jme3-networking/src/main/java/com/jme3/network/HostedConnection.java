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
package com.jme3.network;

import java.util.Set;

/**
 *  This is the connection back to a client that is being
 *  hosted in a server instance.  
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public interface HostedConnection extends MessageConnection
{
    /**
     *  Returns the Server instance that is hosting this connection.
     */
    public Server getServer();     

    /**
     *  Returns the server-unique ID for this client.
     */
    public int getId();

    /**
     *  Returns the transport specific remote address of this connection
     *  as a string.  This may or may not be unique per connection depending
     *  on the type of transport.  It is provided for information and filtering
     *  purposes. 
     */
    public String getAddress();
   
    /**
     *  Closes and removes this connection from the server
     *  sending the optional reason to the remote client.
     */
    public void close( String reason );
    
    /**
     *  Sets a session attribute specific to this connection.  If the value
     *  is set to null then the attribute is removed.
     *
     *  @return The previous session value for this key or null
     *          if there was no previous value.
     */
    public Object setAttribute( String name, Object value );
    
    /**
     *  Retrieves a previously stored session attribute or
     *  null if no such attribute exists.
     */
    public <T> T getAttribute( String name );
    
    /**
     *  Returns a read-only set of attribute names currently stored
     *  for this client session.
     */
    public Set<String> attributeNames();     
}
