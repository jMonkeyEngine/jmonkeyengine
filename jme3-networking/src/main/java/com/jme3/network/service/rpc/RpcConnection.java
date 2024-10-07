/*
 * Copyright (c) 2015-2021 jMonkeyEngine
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

package com.jme3.network.service.rpc;

import com.jme3.network.MessageConnection;
import com.jme3.network.service.rpc.msg.RpcCallMessage;
import com.jme3.network.service.rpc.msg.RpcResponseMessage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  Wraps a message connection to provide RPC call support.  This
 *  is used internally by the RpcClientService and RpcHostedService to manage
 *  network messaging.
 *
 *  @author    Paul Speed
 */
public class RpcConnection {

    private static final Logger log = Logger.getLogger(RpcConnection.class.getName());
 
    /**
     *  The underlying connection upon which RPC call messages are sent
     *  and RPC response messages are received.  It can be a Client or
     *  a HostedConnection depending on the mode of the RPC service.
     */
    private MessageConnection connection;
    
    /**
     *  The objectId index of RpcHandler objects that are used to perform the
     *  RPC calls for a particular object.
     */
    private Map<Short, RpcHandler> handlers = new ConcurrentHashMap<>();
    
    /**
     *  Provides unique messages IDs for outbound synchronous call
     *  messages.  These are then used in the responses index to
     *  locate the proper ResponseHolder objects.
     */
    private AtomicLong sequenceNumber = new AtomicLong();
    
    /**
     *  Tracks the ResponseHolder objects for sent message IDs.  When the
     *  response is received, the appropriate handler is found here and the
     *  response or error set, thus releasing the waiting caller.
     */ 
    private Map<Long, ResponseHolder> responses = new ConcurrentHashMap<>(); 
 
    /**
     *  Creates a new RpcConnection for the specified network connection.
     */   
    public RpcConnection( MessageConnection connection ) {
        this.connection = connection;
    }
 
    /**
     *  Clears any pending synchronous calls causing them to
     *  throw an exception with the message "Closing connection".
     */    
    public void close() {
        // Let any pending waits go free
        for( ResponseHolder holder : responses.values() ) {
            holder.release();
        }
    }
 
    /**
     *  Performs a remote procedure call with the specified arguments and waits
     *  for the response.  Both the outbound message and inbound response will
     *  be sent on the specified channel.
     */
    public Object callAndWait( byte channel, short objId, short procId, Object... args ) {
        
        RpcCallMessage msg = new RpcCallMessage(sequenceNumber.getAndIncrement(), 
                                                channel, objId, procId, args);
        
        // Need to register an object so we can wait for the response.
        // ...before we send it.  Just in case.
        ResponseHolder holder = new ResponseHolder(msg); 
        responses.put(msg.getMessageId(), holder);        
 
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "Sending:{0}  on channel:{1}", new Object[]{msg, channel});
        }
        
        // Prevent non-async messages from being sent as UDP
        // because there is a high probability that this would block
        // forever waiting for a response.  For async calls it's ok
        // so it doesn't do the check.
        if( channel >= 0 ) {        
            connection.send(channel, msg);
        } else {
            connection.send(msg);
        }
                
        return holder.getResponse();
    }

    /**
     *  Performs a remote procedure call with the specified arguments but does
     *  not wait for a response.  The outbound message is sent on the specified channel.
     *  There is no inbound response message. 
     */
    public void callAsync( byte channel, short objId, short procId, Object... args ) {
        
        RpcCallMessage msg = new RpcCallMessage(-1, channel, objId, procId, args);
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "Sending:{0}  on channel:{1}", new Object[]{msg, channel});
        }        
        connection.send(channel, msg);        
    }
    
    /** 
     *  Register a handler that can be called by the other end
     *  of the connection using the specified object ID.  Only one
     *  handler per object ID can be registered at any given time,
     *  though the same handler can be registered for multiple object
     *  IDs.
     */    
    public void registerHandler( short objId, RpcHandler handler ) {
        handlers.put(objId, handler);
    }
    
    /**
     *  Removes a previously registered handler for the specified
     *  object ID.  
     */
    public void removeHandler( short objId, RpcHandler handler ) {
        RpcHandler removing = handlers.get(objId);
        if( handler != removing ) {
            throw new IllegalArgumentException("Handler not registered for object ID:" 
                                                + objId + ", handler:" + handler );
        }
        handlers.remove(objId);
    }
 
    protected void send( byte channel, RpcResponseMessage msg ) {
        if( channel >= 0 ) {
            connection.send(channel, msg);
        } else {
            connection.send(msg);
        }
    }
 
    /**
     *  Called internally when an RpcCallMessage is received from 
     *  the remote connection.
     */ 
    public void handleMessage( RpcCallMessage msg ) {
    
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "handleMessage({0})", msg);
        }
        RpcHandler handler = handlers.get(msg.getObjectId());
        try {
            if( handler == null ) {
                throw new RuntimeException("Handler not found for objectID:" + msg.getObjectId());
            }
            Object result = handler.call(this, msg.getObjectId(), msg.getProcedureId(), msg.getArguments());
            if( !msg.isAsync() ) {
                send(msg.getChannel(), new RpcResponseMessage(msg.getMessageId(), result));
            }
        } catch( Exception e ) {
            if( !msg.isAsync() ) {
                send(msg.getChannel(), new RpcResponseMessage(msg.getMessageId(), e));
            } else {
                log.log(Level.SEVERE, "Error invoking async call for:" + msg, e);
            }
        }   
    }

    /**
     *  Called internally when an RpcResponseMessage is received from 
     *  the remote connection.
     */ 
    public void handleMessage( RpcResponseMessage msg ) {
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "handleMessage({0})", msg);
        }    
        ResponseHolder holder = responses.remove(msg.getMessageId());
        if( holder == null ) {
            return;
        }
        holder.setResponse(msg);       
    }
 
    /**
     *  Sort of like a Future, holds a locked reference to a response
     *  until the remote call has completed and returned a response.
     */   
    private class ResponseHolder {
        private Object response;
        private String error;
        private Throwable exception;
        private RpcCallMessage msg;
        boolean received = false;
 
        public ResponseHolder( RpcCallMessage msg ) {
            this.msg = msg;
        }
        
        public synchronized void setResponse( RpcResponseMessage msg ) {
            this.response = msg.getResult();
            this.error = msg.getError();
            this.exception = msg.getThrowable();
            this.received = true;
            notifyAll();
        }
        
        public synchronized Object getResponse() {
            try {
                while(!received) {
                    wait();                
                }
            } catch( InterruptedException e ) {
                throw new RuntimeException("Interrupted waiting for response to:" + msg, e);
            }
            if( error != null ) {
                throw new RuntimeException("Error calling remote procedure:" + msg + "\n" + error);
            }
            if( exception != null ) {
                throw new RuntimeException("Error calling remote procedure:" + msg, exception);
            } 
            return response;              
        }
        
        public synchronized void release() {
            if( received ) {
                return;
            }
            // Else signal an error for the callers
            this.error = "Closing connection";
            this.received = true;
        }
    }
}
