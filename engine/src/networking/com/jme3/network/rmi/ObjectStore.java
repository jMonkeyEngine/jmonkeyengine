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

import com.jme3.network.ClientStateListener.DisconnectInfo;
import com.jme3.network.*;
import com.jme3.network.serializing.Serializer;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ObjectStore {

    private static final Logger logger = Logger.getLogger(ObjectStore.class.getName());

    private static final class Invocation {

        Object retVal;
        boolean available = false;

        @Override
        public String toString(){
            return "Invocation[" + retVal + "]";
        }
    }

    private Client client;
    private Server server;
    
    private ClientEventHandler clientEventHandler = new ClientEventHandler();
    private ServerEventHandler serverEventHandler = new ServerEventHandler();

    // Local object ID counter
    private volatile short objectIdCounter = 0;
    
    // Local invocation ID counter
    private volatile short invocationIdCounter = 0;

    // Invocations waiting ..
    private IntMap<Invocation> pendingInvocations = new IntMap<Invocation>();
    
    // Objects I share with other people
    private IntMap<LocalObject> localObjects = new IntMap<LocalObject>();

    // Objects others share with me
    private HashMap<String, RemoteObject> remoteObjects = new HashMap<String, RemoteObject>();
    private IntMap<RemoteObject> remoteObjectsById = new IntMap<RemoteObject>();

    private final Object receiveObjectLock = new Object();

    public class ServerEventHandler implements MessageListener<HostedConnection>,
                                                      ConnectionListener {

        public void messageReceived(HostedConnection source, Message m) {
            onMessage(source, m);
        }

        public void connectionAdded(Server server, HostedConnection conn) {
            onConnection(conn);
        }

        public void connectionRemoved(Server server, HostedConnection conn) {
        }
        
    } 
    
    public class ClientEventHandler implements MessageListener,
                                                      ClientStateListener {

        public void messageReceived(Object source, Message m) {
            onMessage(null, m);
        }

        public void clientConnected(Client c) {
            onConnection(null);
        }

        public void clientDisconnected(Client c, DisconnectInfo info) {
        }
        
    }
    
    static {
        Serializer s = new RmiSerializer();
        Serializer.registerClass(RemoteObjectDefMessage.class, s);
        Serializer.registerClass(RemoteMethodCallMessage.class, s);
        Serializer.registerClass(RemoteMethodReturnMessage.class, s);
    }

    public ObjectStore(Client client) {
        this.client = client;
        client.addMessageListener(clientEventHandler, 
                RemoteObjectDefMessage.class,
                RemoteMethodCallMessage.class,
                RemoteMethodReturnMessage.class);
        client.addClientStateListener(clientEventHandler);
    }

    public ObjectStore(Server server) {
        this.server = server;
        server.addMessageListener(serverEventHandler, 
                RemoteObjectDefMessage.class,
                RemoteMethodCallMessage.class,
                RemoteMethodReturnMessage.class);
        server.addConnectionListener(serverEventHandler);
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
        //localObj.methods   = obj.getClass().getMethods();
        
        ArrayList<Method> methodList = new ArrayList<Method>();
        for (Method method : obj.getClass().getMethods()){
            if (method.getDeclaringClass() == obj.getClass()){
                methodList.add(method);
            }
        }
        localObj.methods = methodList.toArray(new Method[methodList.size()]);  
        
        // Put it in the store
        localObjects.put(localObj.objectId, localObj);

        // Inform the others of its existence
        RemoteObjectDefMessage defMsg = new RemoteObjectDefMessage();
        defMsg.objects = new ObjectDef[]{ makeObjectDef(localObj) };

        if (client != null) {
            client.send(defMsg);
            logger.log(Level.INFO, "Client: Sending {0}", defMsg);
        } else {
            server.broadcast(defMsg);
            logger.log(Level.INFO, "Server: Sending {0}", defMsg);
        }
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
            // Note: could cause threading issues if used from multiple threads
            pendingInvocations.put(call.invocationId, invoke);
        }

        if (server != null){
            remoteObj.client.send(call);
            logger.log(Level.INFO, "Server: Sending {0}", call);
        }else{
            client.send(call);
            logger.log(Level.INFO, "Client: Sending {0}", call);
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
            // Note: could cause threading issues if used from multiple threads
            pendingInvocations.remove(call.invocationId);
            return invoke.retVal;
        }else{
            return null;
        }
    }

    private void onMessage(HostedConnection source, Message message) {
        // Might want to do more strict validation of the data
        // in the message to prevent crashes

        if (message instanceof RemoteObjectDefMessage){
            RemoteObjectDefMessage defMsg = (RemoteObjectDefMessage) message;

            ObjectDef[] defs = defMsg.objects;
            for (ObjectDef def : defs){
                RemoteObject remoteObject = new RemoteObject(this, source);
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
            if (localObj == null)
                return;

            if (call.methodId < 0 || call.methodId >= localObj.methods.length)
                return;

            Object obj = localObj.theObject;
            Method method = localObj.methods[call.methodId];
            Object[] args = call.args;
            Object ret = null;
            try {
                ret = method.invoke(obj, args);
            } catch (IllegalAccessException ex){
                logger.log(Level.WARNING, "RMI: Error accessing method", ex);
            } catch (IllegalArgumentException ex){
                logger.log(Level.WARNING, "RMI: Invalid arguments", ex);
            } catch (InvocationTargetException ex){
                logger.log(Level.WARNING, "RMI: Invocation exception", ex);
            }

            if (method.getReturnType() != void.class){
                // send return value back
                RemoteMethodReturnMessage retMsg = new RemoteMethodReturnMessage();
                retMsg.invocationID = call.invocationId;
                retMsg.retVal = ret;
                if (server != null){
                    source.send(retMsg);
                    logger.log(Level.INFO, "Server: Sending {0}", retMsg);
                } else{
                    client.send(retMsg);
                    logger.log(Level.INFO, "Client: Sending {0}", retMsg);
                }
            }
        }else if (message instanceof RemoteMethodReturnMessage){
            RemoteMethodReturnMessage retMsg = (RemoteMethodReturnMessage) message;
            Invocation invoke = pendingInvocations.get(retMsg.invocationID);
            if (invoke == null){
                logger.log(Level.WARNING, "Cannot find invocation ID: {0}", retMsg.invocationID);
                return;
            }

            synchronized (invoke){
                invoke.retVal = retMsg.retVal;
                invoke.available = true;
                invoke.notifyAll();
            }
        }
    }

    private void onConnection(HostedConnection conn) {
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
            if (this.client != null){
                this.client.send(defMsg);
                logger.log(Level.INFO, "Client: Sending {0}", defMsg);
            } else{
                conn.send(defMsg);
                logger.log(Level.INFO, "Server: Sending {0}", defMsg);
            }
        }
    }

}
