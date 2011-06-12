/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.network.rmi;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Sent to a remote client to make a remote method invocation.
 *
 * @author Kirill Vainer
 */
@Serializable
public class RemoteMethodCallMessage extends AbstractMessage {

    public RemoteMethodCallMessage(){
        super(true);
    }

    /**
     * The object ID on which the call is being made.
     */
    public int objectId;

    /**
     * The method ID used for look-up in the LocalObject.methods array.
     */
    public short methodId;

    /**
     * Invocation ID is used to identify a particular call if the calling
     * client needs the return value of the called RMI method.
     * This is set to zero if the method does not return a value.
     */
    public short invocationId;

    /**
     * Arguments of the remote method invocation.
     */
    public Object[] args;

    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("RemoteMethodCallMessage[objectID=").append(objectId).append(", methodID=")
          .append(methodId);
        if (args != null && args.length > 0){
            sb.append(", args={");
            for (Object arg : args){
                sb.append(arg.toString()).append(", ");
            }
            sb.setLength(sb.length()-2);
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }
}
