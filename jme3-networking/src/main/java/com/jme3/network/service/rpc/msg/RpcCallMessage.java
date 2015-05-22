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

 
/**
 *  Used internally to send RPC call information to
 *  the other end of a connection for execution.
 *
 *  @author    Paul Speed
 */
@Serializable
public class RpcCallMessage extends AbstractMessage {

    private long msgId;
    private byte channel;
    private short objId;
    private short procId;
    private Object[] args;

    public RpcCallMessage() {
    }
    
    public RpcCallMessage( long msgId, byte channel, short objId, short procId, Object... args ) {
        this.msgId = msgId;
        this.channel = channel;
        this.objId = objId;
        this.procId = procId;
        this.args = args;
    }
 
    public long getMessageId() {
        return msgId;
    }
    
    public byte getChannel() {
        return channel;
    }
 
    public boolean isAsync() {
        return msgId == -1;
    }
 
    public short getObjectId() {
        return objId;
    }
    
    public short getProcedureId() {
        return procId;
    }
    
    public Object[] getArguments() {
        return args;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[#" + msgId + ", channel=" + channel
                                          + (isAsync() ? ", async" : ", sync") 
                                          + ", objId=" + objId 
                                          + ", procId=" + procId 
                                          + ", args.length=" + (args == null ? 0 : args.length) 
                                          + "]";
    }
}
