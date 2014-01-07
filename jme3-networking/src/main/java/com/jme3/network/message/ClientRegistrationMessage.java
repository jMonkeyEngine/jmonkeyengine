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
package com.jme3.network.message;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.*;
import com.jme3.network.serializing.serializers.StringSerializer;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *  Client registration is a message that contains a unique ID. This ID
 *  is simply the current time in milliseconds, providing multiple clients
 *  will not connect to the same server within one millisecond. This is used
 *  to couple the TCP and UDP connections together into one 'Client' on the
 *  server.
 *
 * @author Lars Wesselius, Paul Speed
 */
@Serializable()
public class ClientRegistrationMessage extends AbstractMessage {

    public static final short SERIALIZER_ID = -44;
    
    private long id;
    private String gameName;
    private int version;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
    
    public void setGameName( String name ) {
        this.gameName = name;
    }
 
    public String getGameName() {
        return gameName;
    }
    
    public void setVersion( int version ) {
        this.version = version;
    }
    
    public int getVersion() {
        return version;
    }
    
    public String toString() {
        return getClass().getName() + "[id=" + id + ", gameName=" + gameName + ", version=" + version + "]";
    }
 
    /**
     *  A message-specific serializer to avoid compatibility issues
     *  between versions.  This serializer is registered to the specific
     *  SERIALIZER_ID which is compatible with previous versions of the 
     *  SM serializer registrations... and now will be forever.
     */   
    public static class ClientRegistrationSerializer extends Serializer {
     
        public ClientRegistrationMessage readObject( ByteBuffer data, Class c ) throws IOException {
    
            // Read the null/non-null marker
            if (data.get() == 0x0)
                return null;
 
            ClientRegistrationMessage msg = new ClientRegistrationMessage();
            
            msg.gameName = StringSerializer.readString(data);
            msg.id = data.getLong();
            msg.version = data.getInt();
            
            return msg;
        }

        public void writeObject(ByteBuffer buffer, Object object) throws IOException {
    
            // Add the null/non-null marker
            buffer.put( (byte)(object != null ? 0x1 : 0x0) );
            if (object == null) {
                // Nothing left to do
                return;
            }
            
            ClientRegistrationMessage msg = (ClientRegistrationMessage)object;
            StringSerializer.writeString( msg.gameName, buffer );           

            buffer.putLong(msg.id);
            buffer.putInt(msg.version);                    
        }
    }
     
}
