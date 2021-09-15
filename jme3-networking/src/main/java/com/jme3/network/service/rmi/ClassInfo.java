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

package com.jme3.network.service.rmi;

import com.jme3.network.serializing.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 *  Internal information about a shared class.  This is the information
 *  that is sent over the wire for shared types.
 *
 *  @author    Paul Speed
 */
@Serializable
public final class ClassInfo {
    private String name;
    private short typeId;
    private MethodInfo[] methods;
 
    /**
     *  For serialization only.
     */   
    public ClassInfo() {
    }
    
    public ClassInfo( short typeId, Class type ) {
        this.typeId = typeId;
        this.name = type.getName();
        this.methods = toMethodInfo(type, type.getMethods());
    }
 
    public String getName() {
        return name;
    }
 
    public Class getType() {
        try {
            return Class.forName(name);
        } catch( ClassNotFoundException e ) {
            throw new RuntimeException("Error finding class for:" + this, e);   
        }
    }
 
    public short getId() {
        return typeId;
    }
 
    public MethodInfo getMethod( short id ) {
        return methods[id];
    }
 
    public MethodInfo getMethod( Method m ) {        
        for( MethodInfo mi : methods ) {
            if( mi.matches(m) ) {
                return mi;
            }
        }
        return null; 
    }
    
    private MethodInfo[] toMethodInfo( Class type, Method[] methods ) {
        List<MethodInfo> result = new ArrayList<>();
        short methodId = 0;
        for( Method m : methods ) {
            // Simple... add all methods exposed through the interface
            result.add(new MethodInfo(methodId++, m));
        }
        return result.toArray(new MethodInfo[result.size()]);
    }
 
    public MethodInfo[] getMethods() {
        return methods;
    }
    
    @Override
    public String toString() {
        return "ClassInfo[" + name + "]";
    }
}
