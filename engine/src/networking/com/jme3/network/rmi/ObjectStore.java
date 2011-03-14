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


import com.jme3.network.connection.Client;
import com.jme3.network.connection.Server;
import com.jme3.network.events.ConnectionListener;
import com.jme3.network.events.MessageListener;
import com.jme3.network.message.Message;
import com.jme3.network.serializing.Serializer;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

public class ObjectStore implements MessageListener, ConnectionListener {

    private static final class Invocation {
        Object retVal;
        boolean available = false;
    }

    private Client client;
    private Server server;

    // Local object ID counter
    private short objectIdCounter = 0;
    
    // Local invocation ID counter
    private short invocationIdCounter = 0;

    // Invocations waiting ..
    private IntMap<Invocation> pendingInvocations = new IntMap<Invocation>();
    
    // Objects I share with other people
    private IntMap<LocalObject> localObjects = new IntMap<LocalObject>();

    // Objects others share with me
    private HashMap<String, RemoteObject> remoteObjects = new HashMap<String, RemoteObject>();
    private IntMap<RemoteObject> remoteObjectsById = new IntMap<RemoteObject>();

    private final Object receiveObjectLock = new Object();

    static {
        Serializer s = new RmiSerializer();
        Serializer.registerClass(RemoteObjectDefMessage.class, s);
        Serializer.registerClass(RemoteMethodCallMessage.class, s);
        Serializer.registerClass(RemoteMethodReturnMessage.class, s);
    }

    public ObjectStore(Client client){
        this.client = client;
        client.addMessageListener(this, RemoteObjectDefMessage.class, 
                                        RemoteMethodCallMessage.class,
                                        RemoteMethodReturnMessage.class);
        client.addConnectionListener(this);
    }

    public ObjectStore(Server server){
        this.server = server;
        server.addMessageListener(this, RemoteObjectDefMessage.class, 
                                        RemoteMethodCallMessage.class,
                                        RemoteMethodReturnMessage.class);
        server.addConnectionListener(this);
    }

    private ObjectDef makeObjectDef(LocalObject localObj){
        ObjectDef def = new ObjectDef();
        def.objectName = localObj.objectName;
        def.objectId   = localObj.objectId;
        def.methods    = localObj.methods;
        return def;
    }

    public void exposeObject(String name, Object obj) throws IOException{
        // Create a local object
        LocalObject localObj = new LocalObject();
        localObj.objectName = name;
        localObj.objectId  = objectIdCounter++;
        localObj.theObject = obj;
        localObj.methods   = obj.getClass().getMethods();
        
        // Put it in the store
        localObjects.put(localObj.objectId, localObj);

        // Inform the others of its existence
        RemoteObjectDefMessage defMsg = new RemoteObjectDefMessage();
        defMsg.objects = new ObjectDef[]{ makeObjectDef(localObj) };

        if (client != null)
            client.send(defMsg);
        else
            server.broadcast(defMsg);
    }

    public <T> T getExposedObject(String name, Class<T> type, boolean waitFor) throws InterruptedException{
        RemoteObject ro = remoteObjects.get(name);
        if (ro == null){
            if (!waitFor)
                throw new RuntimeException("Cannot find remote object named: " + name);
            else{
                do {
                    synchronized (receiveObjectLock){
                        receiveObjectLock.wait();
                    }
                } while ( (ro = remoteObjects.get(name)) == null );
            }
        }
            

        Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ type }, ro);
        ro.loadMethods(type);
        return (T) proxy;
    }

    Object invokeRemoteMethod(RemoteObject remoteObj, Method method, Object[] args){
        Integer methodIdInt = remoteObj.methodMap.get(method);
        if (methodIdInt == null)
             throw new RuntimeException("Method not implemented by remote object owner: "+method);

        boolean needReturn = method.getReturnType() != void.class;
        short objectId = remoteObj.objectId;
        short methodId = methodIdInt.shortValue();
        RemoteMethodCallMessage call = new RemoteMethodCallMessage();
        call.methodId = methodId;
        call.objectId = objectId;
        call.args = args;

        Invocation invoke = null;
        if (needReturn){
            call.invocationId = invocationIdCounter++;
            invoke = new Invocation();
            pendingInvocations.put(call.invocationId, invoke);
        }

        try{
            if (server != null){
                remoteObj.client.send(call);
            }else{
                client.send(call);
            }
        } catch (IOException ex){
            ex.printStackTrace();
        }

        if (invoke != null){
            synchronized(invoke){
                while (!invoke.available){
                    try {
                        invoke.wait();
                    } catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                }
            }
            pendingInvocations.remove(call.invocationId);
            return invoke.retVal;
        }else{
            return null;
        }
    }

    public void messageReceived(Message message) {
        if (message instanceof RemoteObjectDefMessage){
            RemoteObjectDefMessage defMsg = (RemoteObjectDefMessage) message;

            ObjectDef[] defs = defMsg.objects;
            for (ObjectDef def : defs){
                RemoteObject remoteObject = new RemoteObject(this, message.getClient());
                remoteObject.objectId = (short)def.objectId;
                remoteObject.methodDefs = def.methodDefs;
                remoteObjects.put(def.objectName, remoteObject);
                remoteObjectsById.put(def.objectId, remoteObject);
            }
            
            synchronized (receiveObjectLock){
                receiveObjectLock.notifyAll();
            }
        }else if (message instanceof RemoteMethodCallMessage){
            RemoteMethodCallMessage call = (RemoteMethodCallMessage) message;
            LocalObject localObj = localObjects.get(call.objectId);

            Object obj = localObj.theObject;
            Method method = localObj.methods[call.methodId];
            Object[] args = call.args;
            Object ret;
            try {
                ret = method.invoke(obj, args);
            } catch (Exception ex){
                throw new RuntimeException(ex);
            }

            if (method.getReturnType() != void.class){
                // send return value back
                RemoteMethodReturnMessage retMsg = new RemoteMethodReturnMessage();
                retMsg.invocationID = invocationIdCounter++;
                retMsg.retVal = ret;
                try {
                    if (server != null){
                        call.getClient().send(retMsg);
                    } else{
                        client.send(retMsg);
                    }
                } catch (IOException ex){
                    ex.printStackTrace();
                }
            }
        }else if (message instanceof RemoteMethodReturnMessage){
            RemoteMethodReturnMessage retMsg = (RemoteMethodReturnMessage) message;
            Invocation invoke = pendingInvocations.get(retMsg.invocationID);
            if (invoke == null){
                throw new RuntimeException("Cannot find invocation ID: " + retMsg.invocationID);
            }

            synchronized (invoke){
                invoke.retVal = retMsg.retVal;
                invoke.available = true;
                invoke.notifyAll();
            }
        }
    }

    public void clientConnected(Client client) {
        if (localObjects.size() > 0){
            // send a object definition message
            ObjectDef[] defs = new ObjectDef[localObjects.size()];
            int i = 0;
            for (Entry<LocalObject> entry : localObjects){
                defs[i] = makeObjectDef(entry.getValue());
                i++;
            }

            RemoteObjectDefMessage defMsg = new RemoteObjectDefMessage();
            defMsg.objects = defs;
            try {
                if (this.client != null){
                    this.client.send(defMsg);
                } else{
                    client.send(defMsg);
                }
            } catch (IOException ex){
                ex.printStackTrace();
            }
        }
    }

    public void clientDisconnected(Client client) {

    }

    public void messageSent(Message message) {
    }

    public void objectReceived(Object object) {
    }

    public void objectSent(Object object) {
    }

}
