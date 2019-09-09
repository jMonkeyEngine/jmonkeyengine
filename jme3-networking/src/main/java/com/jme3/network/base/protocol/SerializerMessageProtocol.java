/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.network.base.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import com.jme3.network.Message;
import com.jme3.network.base.MessageBuffer;
import com.jme3.network.base.MessageProtocol;
import com.jme3.network.serializing.Serializer;

/**
 *  Implements a MessageProtocol providing message serializer/deserialization
 *  based on the built-in Serializer code. 
 *
 *  <p>The protocol is based on a simple length + data format
 *  where two bytes represent the (short) length of the data
 *  and the rest is the raw data for the Serializers class.</p>
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */ 
public class SerializerMessageProtocol implements MessageProtocol {
 
    public SerializerMessageProtocol() {
    }
 
    /**
     *  Converts a message to a ByteBuffer using the com.jme3.network.serializing.Serializer
     *  and the (short length) + data protocol.  If target is null
     *  then a 32k byte buffer will be created and filled.
     */
    public ByteBuffer toByteBuffer( Message message, ByteBuffer target ) {
    
        // Could let the caller pass their own in       
        ByteBuffer buffer = target == null ? ByteBuffer.allocate(32767 + 2) : target;
        
        try {
            buffer.position(2);
            Serializer.writeClassAndObject(buffer, message);
            buffer.flip();
            short dataLength = (short)(buffer.remaining() - 2);
            buffer.putShort(dataLength);
            buffer.position(0);
            
            return buffer;
        } catch( IOException e ) {
            throw new RuntimeException("Error serializing message", e);
        }
    }

    /**
     *  Creates and returns a message from the properly sized byte buffer
     *  using com.jme3.network.serializing.Serializer.
     */   
    public Message toMessage( ByteBuffer bytes ) {
        try {
            return (Message)Serializer.readClassAndObject(bytes);
        } catch( IOException e ) {
            throw new RuntimeException("Error deserializing object, class ID:" + bytes.getShort(0), e);   
        }         
    }
      
    public MessageBuffer createBuffer() {
        // Defaulting to LazyMessageBuffer
        return new LazyMessageBuffer(this);
    }
     
}



