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
import com.jme3.network.kernel.ConnectorException;
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
    private MessageListener<Object> dispatcher;
    private AtomicBoolean go = new AtomicBoolean(true);
    
    // Marks the messages as reliable or not if they came
    // through this connector.
    private boolean reliable;
    
    public ConnectorAdapter( Connector connector, MessageListener<Object> dispatcher, boolean reliable )
    {
        super( String.valueOf(connector) );
        this.connector = connector;        
        this.dispatcher = dispatcher;
        this.reliable = reliable;
        setDaemon(true);
    }
 
    public void close()
    {
        go.set(false);
        
        // Kill the connector
        connector.close();
    }
 
    protected void dispatch( Message m )
    {
        dispatcher.messageReceived( null, m );                        
    }
 
    public void run()
    {
        MessageProtocol protocol = new MessageProtocol();
        
        while( go.get() ) {
            ByteBuffer buffer = connector.read();
            if( buffer == null ) {
                if( go.get() ) {
                    throw new ConnectorException( "Connector closed." ); 
                } else {
                    // Just dump out because a null buffer is expected
                    // from a closed/closing connector
                    break;
                }
            }
            
            protocol.addBuffer( buffer );
            
            Message m = null;
            while( (m = protocol.getMessage()) != null ) {
                m.setReliable( reliable );
                dispatch( m );
            }
        }
    }
        
}
