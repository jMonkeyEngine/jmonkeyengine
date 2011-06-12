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

import com.jme3.network.HostedConnection;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Contains various meta-data about an RMI interface.
 *
 * @author Kirill Vainer
 */
public class RemoteObject implements InvocationHandler {

    /**
     * Object ID
     */
    short objectId;

    /**
     * Contains {@link MethodDef method definitions} for all exposed
     * RMI methods in the remote RMI interface.
     */
    MethodDef[] methodDefs;

    /**
     * Maps from methods locally retrieved from the RMI interface to
     * a method ID.
     */
    HashMap<Method, Integer> methodMap = new HashMap<Method, Integer>();

    /**
     * The {@link ObjectStore} which stores this RMI interface.
     */
    ObjectStore store;
    
    /**
     * The client who exposed the RMI interface, or null if the server
     * exposed it.
     */
    HostedConnection client;

    public RemoteObject(ObjectStore store, HostedConnection client){
        this.store = store;
        this.client = client;
    }

    private boolean methodEquals(MethodDef methodDef, Method method){
        Class<?>[] interfaceTypes = method.getParameterTypes();
        Class<?>[] defTypes       = methodDef.paramTypes;

        if (interfaceTypes.length == defTypes.length){
            for (int i = 0; i < interfaceTypes.length; i++){
                if (!defTypes[i].isAssignableFrom(interfaceTypes[i])){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Generates mappings from the given interface into the remote RMI
     * interface's implementation.
     *
     * @param interfaceClass The interface class to use.
     */
    public void loadMethods(Class<?> interfaceClass){
        HashMap<String, ArrayList<Method>> nameToMethods
                = new HashMap<String, ArrayList<Method>>();

        for (Method method : interfaceClass.getDeclaredMethods()){
            ArrayList<Method> list = nameToMethods.get(method.getName());
            if (list == null){
                list = new ArrayList<Method>();
                nameToMethods.put(method.getName(), list);
            }
            list.add(method);
        }

        mapping_search: for (int i = 0; i < methodDefs.length; i++){
            MethodDef methodDef = methodDefs[i];
            ArrayList<Method> methods = nameToMethods.get(methodDef.name);
            if (methods == null)
                continue;
            
            for (Method method : methods){
                if (methodEquals(methodDef, method)){
                    methodMap.put(method, i);
                    continue mapping_search;
                }
            }
        }
    }

    /**
     * Callback from InvocationHandler.
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return store.invokeRemoteMethod(this, method, args);
    }

}
