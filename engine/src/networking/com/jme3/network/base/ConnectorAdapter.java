/*
 * Copyright (c) 2011 jMonkeyEngine
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.kernel.Connector;
import com.jme3.network.serializing.Serializer;

/**
 *  Wraps a single Connector and forwards new messages
 *  to the supplied message dispatcher.  This is used
 *  by DefaultClient to manage its connector objects.
 *  This is only responsible for message reading and provides
 *  no support for buffering writes.
 *
 *  <p>This adapter assumes a simple protocol where two
 *  bytes define a (short) object size with the object data
 *  to follow.  Note: this limits the size of serialized
 *  objects to 32676 bytes... even though, for example,
 *  datagram packets can hold twice that. :P</p>  
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class ConnectorAdapter extends Thread
{
    private Connector connector;
    private MessageListener dispatcher;
    private AtomicBoolean go = new AtomicBoolean(true);
    
    public ConnectorAdapter( Connector connector, MessageListener dispatcher )
    {
        super( String.valueOf(connector) );
        this.connector = connector;
        this.dispatcher = dispatcher;
        setDaemon(true);
    }
 
    public void close()
    {
        go.set(false);
        
        // Kill the connector
        connector.close();
    }
 
    protected void createAndDispatch( ByteBuffer buffer )
    {
        try {
            Object obj = Serializer.readClassAndObject( buffer );
            Message m = (Message)obj;
            dispatcher.messageReceived( null, m );                        
        } catch( IOException e ) {
            throw new RuntimeException( "Error deserializing object", e );   
        }         
    }
 
    public void run()
    {
        ByteBuffer current = null;
        int size = 0;
    
        while( go.get() ) {
            ByteBuffer buffer = connector.read();
            
            // push the data from the buffer into as
            // many messages as we can
            while( buffer.remaining() > 0 ) {
                
                if( current == null ) {
                    // We are not currently reading an object so
                    // grab the size.
                    // Note: this is somewhat limiting... int would
                    // be better.
                    size = buffer.getShort();
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
 
                    createAndDispatch( current );
 
                    current = null;                    
                } else {
                
                    // Not yet a complete object so just copy what we have
                    current.put( buffer ); 
                }            
            }            
        }
    }
        
}
