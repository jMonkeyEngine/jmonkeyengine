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

import com.jme3.network.MessageConnection;
import com.jme3.network.service.AbstractClientService;
import com.jme3.network.service.ClientServiceManager;
import com.jme3.network.service.rpc.RpcClientService;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 *  @author    Paul Speed
 */
public class RmiClientService extends AbstractClientService {

    private RpcClientService rpc;
    private byte defaultChannel;
    private short rmiObjectId;
    private RmiRegistry rmi;
    private volatile boolean isStarted = false;
    
    private final List<ObjectInfo> pending = new ArrayList<ObjectInfo>();   
    
    public RmiClientService() {
        this((short)-1, (byte)MessageConnection.CHANNEL_DEFAULT_RELIABLE);
    }
    
    public RmiClientService( short rmiObjectId, byte defaultChannel ) {
        this.defaultChannel = defaultChannel;
        this.rmiObjectId = rmiObjectId;
    }

    public <T> void share( T object, Class<? super T> type ) {
        share(defaultChannel, object, type);       
    }
    
    public <T> void share( byte channel, T object, Class<? super T> type ) {
        share(channel, type.getName(), object, type);
    } 
    
    public <T> void share( String name, T object, Class<? super T> type ) {
        share(defaultChannel, name, object, type);
    }
    
    public <T> void share( byte channel, String name, T object, Class<? super T> type ) {
        if( !isStarted ) {
            synchronized(pending) {
                if( !isStarted ) {
                    pending.add(new ObjectInfo(channel, name, object, type));
                    return;
                }
            }   
        }
        
        // Else we can add it directly.
        rmi.share(channel, name, object, type);
    }

    public <T> T getRemoteObject( Class<T> type ) {
        return rmi.getRemoteObject(type);
    }
        
    public <T> T getRemoteObject( String name, Class<T> type ) {
        return rmi.getRemoteObject(name, type);
    }    

    @Override
    protected void onInitialize( ClientServiceManager s ) {
        rpc = getService(RpcClientService.class);
        if( rpc == null ) {
            throw new RuntimeException("RmiClientService requires RpcClientService");
        }
        
        // Register it now so that it is available when the
        // server starts to send us stuff.  Waiting until start()
        // is too late in this case.
        rmi = new RmiRegistry(rpc.getRpcConnection(), rmiObjectId, defaultChannel);        
    }
    
    @Override
    public void start() {
        super.start();
        
        // Register all of the classes that have been waiting.
        synchronized(pending) {
            for( ObjectInfo info : pending ) {
                rmi.share(info.channel, info.name, info.object, info.type);
            }
            pending.clear();
            isStarted = true;
        }
    }
    
    private class ObjectInfo {
        byte channel;
        String name;
        Object object;
        Class type;
        
        public ObjectInfo( byte channel, String name, Object object, Class type ) {
            this.channel = channel;
            this.name = name;
            this.object = object;
            this.type = type;
        }
        
        @Override
        public String toString() {
            return "ObjectInfo[" + channel + ", " + name + ", " + object + ", " + type + "]";
        }
    }   
}

