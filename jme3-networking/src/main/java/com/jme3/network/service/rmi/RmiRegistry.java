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

import com.jme3.network.HostedConnection;
import com.jme3.network.MessageConnection;
import com.jme3.network.service.rpc.RpcConnection;
import com.jme3.network.service.rpc.RpcHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 *
 *  @author    Paul Speed
 */
public class RmiRegistry {

    static final Logger log = Logger.getLogger(RmiRegistry.class.getName());

    // RPC IDs for calling our remote endpoint
    private static final short NEW_CLASS = 0;
    private static final short ADD_OBJECT = 1;
    private static final short REMOVE_OBJECT = 2;
    
    private RpcConnection rpc;
    private short rmiId;
    private byte defaultChannel;
    private final RmiHandler rmiHandler = new RmiHandler();
    private final ClassInfoRegistry classCache = new ClassInfoRegistry();
    private final AtomicInteger nextObjectId = new AtomicInteger();
    
    private final ObjectIndex<SharedObject> local = new ObjectIndex<SharedObject>();
    private final ObjectIndex<Object> remote = new ObjectIndex<Object>();

    // Only used on the server to provide thread-local context for
    // local RMI calls.
    private HostedConnection context;
    
    public RmiRegistry( RpcConnection rpc, short rmiId, byte defaultChannel ) {
        this(null, rpc, rmiId, defaultChannel);
    }
    
    public RmiRegistry( HostedConnection context, RpcConnection rpc, short rmiId, byte defaultChannel ) {
        this.context = context;
        this.rpc = rpc;
        this.rmiId = rmiId;
        this.defaultChannel = defaultChannel;
        rpc.registerHandler(rmiId, rmiHandler);
    }
    
    /**
     *  Exposes the specified object to the other end of the connection as
     *  the specified interface type.  The object can be looked up by type
     *  on the other end.
     */
    public <T> void share( T object, Class<? super T> type ) {
        share(defaultChannel, object, type);       
    }
    
    /**
     *  Exposes, through a specific connection channel, the specified object 
     *  to the other end of the connection as the specified interface type.  
     *  The object can be looked up by type on the other end.
     *  The specified channel will be used for all network communication
     *  specific to this object. 
     */
    public <T> void share( byte channel, T object, Class<? super T> type ) {
        share(channel, type.getName(), object, type);
    } 
    
    /**
     *  Exposes the specified object to the other end of the connection as
     *  the specified interface type and associates it with the specified name.  
     *  The object can be looked up by the associated name on the other end of
     *  the connection.
     */
    public <T> void share( String name, T object, Class<? super T> type ) {
        share(defaultChannel, name, object, type);
    }
    
    /**
     *  Exposes, through a specific connection channel, the specified object to 
     *  the other end of the connection as the specified interface type and associates 
     *  it with the specified name.  
     *  The object can be looked up by the associated name on the other end of
     *  the connection.
     *  The specified channel will be used for all network communication
     *  specific to this object. 
     */
    public <T> void share( byte channel, String name, T object, Class<? super T> type ) {
        
        ClassInfo typeInfo = classCache.getClassInfo(type);
        
        local.lock.writeLock().lock();
        try {
            
            // First see if we've told the remote end about this class
            // before
            if( local.classes.put(typeInfo.getId(), typeInfo) == null ) {
                // It's new
                rpc.callAsync(defaultChannel, rmiId, NEW_CLASS, typeInfo);
                
                // Because type info IDs are global to the class cache,
                // we could in theory keep a global index that we broadcast
                // on first connection setup... we need only prepopulate
                // the index in that case.
            }
 
            // See if we already shared an object under that name           
            SharedObject existing = local.byName.remove(name);
            if( existing != null ) {
                local.byId.remove(existing.objectId);
                rpc.removeHandler(existing.objectId, rmiHandler);
            
                // Need to delete the old one from the remote end
                rpc.callAsync(defaultChannel, rmiId, REMOVE_OBJECT, existing.objectId);
                
                // We don't reuse the ID because it's kind of dangerous.
                // Churning through a new ID is our safety net for accidents.                
            }
            
            SharedObject newShare = new SharedObject(name, object, type, typeInfo);
            local.byName.put(name, newShare);
            local.byId.put(newShare.objectId, newShare);
            
            // Make sure we are setup to receive the remote method calls through
            // the RPC service
            rpc.registerHandler(newShare.objectId, rmiHandler);
            
            // Let the other end know
            rpc.callAsync(defaultChannel, rmiId, ADD_OBJECT, channel, newShare.objectId, name, typeInfo.getId()); 
 
            // We send the ADD_OBJECT to the other end before releasing the
            // lock to avoid a potential inconsistency if two threads try to
            // jam the same name at the same time.  Otherwise, if the timing were
            // right, the remove for one object could get there before its add.
        
        } finally {
            local.lock.writeLock().unlock();
        }   
    }
 
    /**
     *  Returns a local object that was previously registered with share() using
     *  just type registration.
     */
    public <T> T getLocalObject( Class<T> type ) {
        return getLocalObject(type.getName(), type);
    }
    
    /**
     *  Returns a local object that was previously registered with share() using
     *  name registration.
     */
    public <T> T getLocalObject( String name, Class<T> type ) {
        local.lock.readLock().lock();
        try {
            return type.cast(local.byName.get(name).object);
        } finally {
            local.lock.readLock().unlock();
        }
    }   
    
    /**
     *  Looks up a remote object by type and returns a local proxy to the remote object 
     *  that was shared on the other end of the network connection.  If this is called 
     *  from a client then it is accessing a shared object registered on the server.  
     *  If this is called from the server then it is accessing a shared object registered 
     *  on the client.
     */
    public <T> T getRemoteObject( Class<T> type ) {
        return getRemoteObject(type.getName(), type);
    }

    /**
     *  Looks up a remote object by name and returns a local proxy to the remote object 
     *  that was shared on the other end of the network connection.  If this is called 
     *  from a client then it is accessing a shared object registered on the server.  
     *  If this is called from the server then it is accessing a shared object registered 
     *  on the client.
     */
    public <T> T getRemoteObject( String name, Class<T> type ) {
        remote.lock.readLock().lock();
        try {
            return type.cast(remote.byName.get(name));
        } finally {
            remote.lock.readLock().unlock();
        }       
    }
    
    protected void addRemoteClass( ClassInfo info ) {
        if( remote.classes.put(info.getId(), info) != null ) {
            throw new RuntimeException("Error class already exists for ID:" + info.getId());
        }
    }
    
    protected void removeRemoteObject( short objectId ) {
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "removeRemoteObject({0})", objectId);
        }
        throw new UnsupportedOperationException("Removal not yet implemented.");
    }  
  
    protected void addRemoteObject( byte channel, short objectId, String name, ClassInfo typeInfo ) {
        if( log.isLoggable(Level.FINEST) ) {
            log.finest("addRemoveObject(" + objectId + ", " + name + ", " + typeInfo + ")");
        }
        remote.lock.writeLock().lock();
        try {
            Object existing = remote.byName.get(name);
            if( existing != null ) {
                throw new RuntimeException("Object already registered for:" + name);
            }
            
            RemoteObjectHandler remoteHandler = new RemoteObjectHandler(this, channel, objectId, typeInfo);
 
            Object remoteObject = Proxy.newProxyInstance(getClass().getClassLoader(),
                                                         new Class[] {typeInfo.getType()},
                                                         remoteHandler);
                                                                    
            remote.byName.put(name, remoteObject); 
            remote.byId.put(objectId, remoteObject);
        } finally {
            remote.lock.writeLock().unlock();
        } 
    } 

    protected Object invokeRemote( byte channel, short objectId, short procId, CallType callType, Object[] args ) {
        if( log.isLoggable(Level.FINEST) ) {
            log.finest("invokeRemote(" + channel + ", " + objectId + ", " + procId + ", " 
                                       + callType + ", " + (args == null ? "null" : Arrays.asList(args)) + ")");
        }
        switch( callType ) {
            case Asynchronous:
                log.finest("Sending reliable asynchronous.");            
                rpc.callAsync(channel, objectId, procId, args);
                return null;
            case Unreliable:
                log.finest("Sending unreliable asynchronous.");            
                rpc.callAsync((byte)MessageConnection.CHANNEL_DEFAULT_UNRELIABLE, objectId, procId, args);
                return null;
            default:
            case Synchronous:                                           
                log.finest("Sending synchronous.");            
                Object result = rpc.callAndWait(channel, objectId, procId, args);
                if( log.isLoggable(Level.FINEST) ) {
                    log.finest("->got:" + result);
                }
                return result;
        }
    }
    
    /**
     *  Handle remote object registry updates from the other end.
     */
    protected void rmiUpdate( short procId, Object[] args ) {
        if( log.isLoggable(Level.FINEST) ) {
            log.finest("rmiUpdate(" + procId + ", " + Arrays.asList(args) + ")");
        }
        switch( procId ) {
            case NEW_CLASS:
                addRemoteClass((ClassInfo)args[0]);
                break; 
            case REMOVE_OBJECT:
                removeRemoteObject((Short)args[0]);
                break;
            case ADD_OBJECT:
                ClassInfo info = remote.classes.get((Short)args[3]);
                addRemoteObject((Byte)args[0], (Short)args[1], (String)args[2], info);
                break;
        }
    }
 
    /**
     *  Handle the actual remote object method calls.
     */
    protected Object invokeLocal( short objectId, short procId, Object[] args ) {
        // Actually could use a regular concurrent map for this

        // Only lock the local registry during lookup and
        // not invocation.  It prevents a deadlock if the invoked method
        // tries to share an object.  It should be safe.
        SharedObject share = local.byId.get(objectId);         
        local.lock.readLock().lock();
        try {
            share = local.byId.get(objectId);
        } finally {
            local.lock.readLock().unlock();   
        }
                      
        try {
            RmiContext.setRmiConnection(context);
            return share.invoke(procId, args);
        } finally {
            RmiContext.setRmiConnection(null);
        }              
    }

    private class SharedObject {
        private final short objectId;
        private final String name;
        private final Object object;
        private final Class type;
        private final ClassInfo classInfo;
        
        public SharedObject( String name, Object object, Class type, ClassInfo classInfo ) {
            this.objectId = (short)nextObjectId.incrementAndGet();
            this.name = name;
            this.object = object;
            this.type = type;
            this.classInfo = classInfo; 
        }
        
        public Object invoke( short procId, Object[] args ) {
            if( log.isLoggable(Level.FINEST) ) {
                log.finest("SharedObject->invoking:" + classInfo.getMethod(procId) 
                           + " on:" + object 
                           + " with:" + (args == null ? "null" : Arrays.asList(args))); 
            }
            return classInfo.getMethod(procId).invoke(object, args);
        }
    }
       
    private class RmiHandler implements RpcHandler {
        @Override
        public Object call( RpcConnection conn, short objectId, short procId, Object... args ) {
            if( objectId == rmiId ) {
                rmiUpdate(procId, args);
                return null;
            } else {
                return invokeLocal(objectId, procId, args);
            }
        }
    }
    
    /**
     *  Keeps a coincident index between short ID, name, and related class info.
     *  There will be one of these to track our local objects and one to track
     *  the remote objects and a lock that can guard them.
     */
    private class ObjectIndex<T> {
        final Map<String, T> byName = new HashMap<String, T>();
        final Map<Short, T> byId = new HashMap<Short, T>(); 
        final Map<Short, ClassInfo> classes = new HashMap<Short, ClassInfo>();  
        final ReadWriteLock lock = new ReentrantReadWriteLock();
        
        public ObjectIndex() {
        }
    }    
    
}


