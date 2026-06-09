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
package com.jme3.network.message;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.*;
import com.jme3.network.serializing.serializers.StringSerializer;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Represents a disconnect message.
 *
 * @author Lars Wesselius, Paul Speed
 */
@Serializable()
public class DisconnectMessage extends AbstractMessage {

    /**
     * Serializer id reserved for backwards-compatible disconnect messages.
     */
    public static final short SERIALIZER_ID = -42;

    /**
     * Disconnect type used when a client is kicked by the server.
     */
    public static final String KICK = "Kick";
    /**
     * Disconnect type used when the user requested closure.
     */
    public static final String USER_REQUESTED = "User requested";
    /**
     * Disconnect type used when an error closed the connection.
     */
    public static final String ERROR = "Error";
    /**
     * Disconnect type used when a message or client was filtered.
     */
    public static final String FILTERED = "Filtered";

    private String reason;
    private String type;

    /**
     * Creates an empty disconnect message for serialization.
     */
    public DisconnectMessage() {
    }

    /**
     * Returns the disconnect reason text.
     *
     * @return the disconnect reason, or null
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets the disconnect reason text.
     *
     * @param reason the disconnect reason
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Returns the disconnect category.
     *
     * @return the disconnect type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the disconnect category.
     *
     * @param type the disconnect type
     */
    public void setType(String type) {
        this.type = type;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "[reason=" + reason + ", type=" + type + "]";
    }
    
    /**
     *  A message-specific serializer to avoid compatibility issues
     *  between versions.  This serializer is registered to the specific
     *  SERIALIZER_ID which is compatible with previous versions of the 
     *  SM serializer registrations... and now will be forever.
     */   
    public static class DisconnectSerializer extends Serializer {
        /**
         * Creates the serializer used for disconnect messages.
         */
        public DisconnectSerializer() {
        }
     
        @Override
        public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
    
            // Read the null/non-null marker
            if (data.get() == 0x0)
                return null;
 
            DisconnectMessage msg = new DisconnectMessage();
            
            msg.reason = StringSerializer.readString(data);
            msg.type = StringSerializer.readString(data);
            
            return c.cast(msg);
        }

        @Override
        public void writeObject(ByteBuffer buffer, Object object) throws IOException {
    
            // Add the null/non-null marker
            buffer.put( (byte)(object != null ? 0x1 : 0x0) );
            if (object == null) {
                // Nothing left to do
                return;
            }
            
            DisconnectMessage msg = (DisconnectMessage)object;
            StringSerializer.writeString( msg.reason, buffer );           
            StringSerializer.writeString( msg.type, buffer );           
        }
    }     
}
