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

package com.jme3.network.service.rmi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 *  Used internally to remotely invoke methods on RMI shared objects.
 *
 *  @author    Paul Speed
 */
public class RemoteObjectHandler implements InvocationHandler {

    private final RmiRegistry rmi;
    private final byte channel;
    private final short objectId;
    private final ClassInfo typeInfo;
    private final Map<Method, MethodInfo> methodIndex = new ConcurrentHashMap<>();

    public RemoteObjectHandler( RmiRegistry rmi, byte channel, short objectId, ClassInfo typeInfo ) {
        this.rmi = rmi;
        this.channel = channel;
        this.objectId = objectId;
        this.typeInfo = typeInfo;
    } 

    protected MethodInfo getMethodInfo( Method method ) {
        MethodInfo mi = methodIndex.get(method);
        if( mi == null ) {
            mi = typeInfo.getMethod(method);
            if( mi == null ) {
                mi = MethodInfo.NULL_INFO;
            }                      
            methodIndex.put(method, mi);
        }
        return mi == MethodInfo.NULL_INFO ? null : mi;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] os) throws Throwable {
        MethodInfo mi = getMethodInfo(method);
        if( mi == null ) {
            // Try to invoke locally
            return method.invoke(this, os);
        }
        return rmi.invokeRemote(channel, objectId, mi.getId(), mi.getCallType(), os);
    }
 
    @Override
    public String toString() {
        return "RemoteObject[#" + objectId + ", " + typeInfo.getName() + "]";
    }   
}
