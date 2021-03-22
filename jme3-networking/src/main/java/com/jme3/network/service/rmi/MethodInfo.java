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

import com.jme3.network.serializing.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 *  Internal information about shared methods.  This is part of the data that
 *  is passed over the wire when an object is shared. 
 *
 *  @author    Paul Speed
 */
@Serializable
public final class MethodInfo {
    
    public static final MethodInfo NULL_INFO = new MethodInfo();
    
    private String representation;
    private short id;
    private CallType callType;
    private transient Method method;
    
    /** 
     *  For serialization only.
     */
    public MethodInfo() {
    }
    
    public MethodInfo( short id, Method m ) {
        this.id = id;
        this.method = m;
        this.representation = methodToString(m);
        this.callType = getCallType(m);
    }
 
    public Object invoke( Object target, Object... parms ) {
        try {
            return method.invoke(target, parms);
        } catch (IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            throw new RuntimeException("Error invoking:" + method + " on:" + target, e);
        }
    }
 
    public short getId() {
        return id;
    }
 
    public CallType getCallType() {
        return callType;
    }
 
    public boolean matches( Method m ) {
        return representation.equals(methodToString(m));
    }
 
    public static String methodToString( Method m ) {
        StringBuilder sb = new StringBuilder();
        for( Class t : m.getParameterTypes() ) {
            if( sb.length() > 0 ) 
                sb.append(", ");
            sb.append(t.getName());
        }
        return m.getReturnType().getName() + " " + m.getName() + "(" + sb + ")";
    }
 
    public static CallType getCallType( Method m ) {
        if( m.getReturnType() != Void.TYPE ) {
            return CallType.Synchronous;
        }
        if( m.getAnnotation(Asynchronous.class) == null ) {
            return CallType.Synchronous;
        }
        for (Annotation annotation : m.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (type.getName().equals("javax.jws.Oneway")) {
                return CallType.Asynchronous;
            }
        }
            
        Asynchronous async = m.getAnnotation(Asynchronous.class);             
        return async.reliable() ? CallType.Asynchronous : CallType.Unreliable;         
    } 

    @Override
    public int hashCode() {
        return representation.hashCode();
    }
    
    @Override
    public boolean equals( Object o ) {
        if( o == this ) {
            return true;
        }
        if( o == null || o.getClass() != getClass() ) {
            return false;            
        }
        MethodInfo other = (MethodInfo)o;
        return representation.equals(other.representation);
    }
    
    @Override
    public String toString() {
        return "MethodInfo[#" + getId() + ", callType=" + callType + ", " + representation + "]";
    }
}
