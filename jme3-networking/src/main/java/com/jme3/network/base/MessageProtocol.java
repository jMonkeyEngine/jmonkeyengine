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
package com.jme3.network.base;

import com.jme3.network.Message;
import com.jme3.network.serializing.Serializer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 *  Consolidates the conversion of messages to/from byte buffers
 *  and provides a rolling message buffer.  ByteBuffers can be
 *  pushed in and messages will be extracted, accumulated, and 
 *  available for retrieval.  This is not thread safe and is meant
 *  to be used within a single message processing thread.
 *
 *  <p>The protocol is based on a simple length + data format
 *  where two bytes represent the (short) length of the data
 *  and the rest is the raw data for the Serializers class.</p>
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class MessageProtocol
{
    private final LinkedList<Message> messages = new LinkedList<Message>();
    private ByteBuffer current;
    private int size;
    private Byte carry;
 
    /**
     *  Converts a message to a ByteBuffer using the Serializer
     *  and the (short length) + data protocol.  If target is null
     *  then a 32k byte buffer will be created and filled.
     */
    public static ByteBuffer messageToBuffer( Message message, ByteBuffer target )
    {
        // Could let the caller pass their own in       
        ByteBuffer buffer = target == null ? ByteBuffer.allocate( 32767 + 2 ) : target;
        
        try {
            buffer.position( 2 );
            Serializer.writeClassAndObject( buffer, message );
            buffer.flip();
            short dataLength = (short)(buffer.remaining() - 2);
            buffer.putShort( dataLength );
            buffer.position( 0 );
            
            return buffer;
        } catch( IOException e ) {
            throw new RuntimeException( "Error serializing message", e );
        }
    }
 
    /**
     *  Retrieves and removes an extracted message from the accumulated buffer
     *  or returns null if there are no more messages.
     */
    public Message getMessage()
    {
        if( messages.isEmpty() ) {
            return null;
        }
        
        return messages.removeFirst();
    }     
   
    /**
     *  Adds the specified buffer, extracting the contained messages 
     *  and making them available to getMessage().  The left over
     *  data is buffered to be combined with future data.
     &
     *  @return The total number of queued messages after this call.       
     */
    public int addBuffer( ByteBuffer buffer )
    {
        // push the data from the buffer into as
        // many messages as we can
        while( buffer.remaining() > 0 ) {

            if( current == null ) {

                // If we have a left over carry then we need to
                // do manual processing to get the short value
                if( carry != null ) {
                    byte high = carry;
                    byte low = buffer.get();
                    
                    size = (high & 0xff) << 8 | (low & 0xff);
                    carry = null;
                }
                else if( buffer.remaining() < 2 ) {
                    // It's possible that the supplied buffer only has one
                    // byte in it... and in that case we will get an underflow
                    // when attempting to read the short below.
                    
                    // It has to be 1 or we'd never get here... but one
                    // isn't enough so we stash it away.
                    carry = buffer.get();
                    break;
                } else {
                    // We are not currently reading an object so
                    // grab the size.
                    // Note: this is somewhat limiting... int would
                    // be better.
                    size = buffer.getShort();
                }               
 
                // Allocate the buffer into which we'll feed the
                // data as we get it               
                current = ByteBuffer.allocate(size);
            } 

            if( current.remaining() <= buffer.remaining() ) {
                // We have at least one complete object so
                // copy what we can into current, create a message,
                // and then continue pulling from buffer.
                    
                // Artificially set the limit so we don't overflow
                int extra = buffer.remaining() - current.remaining();
                buffer.limit( buffer.position() + current.remaining() );
 
                // Now copy the data                   
                current.put( buffer );
                current.flip();
                    
                // Now set the limit back to a good value
                buffer.limit( buffer.position() + extra );
 
                createMessage( current );
 
                current = null;                    
            } else {
                
                // Not yet a complete object so just copy what we have
                current.put( buffer ); 
            }            
        }            
        
        return messages.size();        
    }
 
    /**
     *  Creates a message from the properly sized byte buffer
     *  and adds it to the messages queue.
     */   
    protected void createMessage( ByteBuffer buffer )
    {
        try {
            Object obj = Serializer.readClassAndObject( buffer );
            Message m = (Message)obj;
            messages.add(m);
        } catch( IOException e ) {
            throw new RuntimeException( "Error deserializing object, class ID:" + buffer.getShort(0), e );   
        }         
    }
}



