/*
 * Copyright (c) 2015 jMonkeyEngine
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

package com.jme3.network.service.rpc.msg;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;
import java.io.PrintWriter;
import java.io.StringWriter;

 
/**
 *  Used internally to send an RPC call's response back to
 *  the caller.
 *
 *  @author    Paul Speed
 */
@Serializable
public class RpcResponseMessage extends AbstractMessage {

    private long msgId;
    private Object result;
    private String error;
    private Object exception; // if it was serializable

    public RpcResponseMessage() {
    }
    
    public RpcResponseMessage( long msgId, Object result ) {
        this.msgId = msgId;
        this.result = result;
    }

    public RpcResponseMessage( long msgId, Throwable t ) {
        this.msgId = msgId;
 
        // See if the exception is serializable
        if( isSerializable(t) ) {
            // Can send the exception itself
            this.exception = t;
        } else {
            // We'll compose all of the info into a string           
            StringWriter sOut = new StringWriter();
            PrintWriter out = new PrintWriter(sOut);
            t.printStackTrace(out);
            out.close();
            this.error = sOut.toString();
        }
    }
 
    public static boolean isSerializable( Throwable error ) {
        if( error == null ) {
            return false;
        }
        for( Throwable t = error; t != null; t = t.getCause() ) {
            if( Serializer.getExactSerializerRegistration(t.getClass()) == null ) {
                return false;
            }
        }
        return true; 
    }
 
    public long getMessageId() {
        return msgId;
    }
    
    public Object getResult() {
        return result;
    }
        
    public String getError() {
        return error;
    }
    
    public Throwable getThrowable() {
        return (Throwable)exception;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[#" + msgId + ", result=" + result
                                          + (error != null ? ", error=" + error : "")
                                          + (exception != null ? ", exception=" + exception : "")
                                          + "]";
    }
}
