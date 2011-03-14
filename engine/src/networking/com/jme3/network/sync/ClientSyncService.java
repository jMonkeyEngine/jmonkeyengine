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

package com.jme3.network.sync;

import com.jme3.network.connection.Client;
import com.jme3.network.events.MessageAdapter;
import com.jme3.network.message.Message;
import com.jme3.network.service.Service;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import java.nio.ByteBuffer;

public class ClientSyncService extends MessageAdapter implements Service {

    private static final ByteBuffer BUFFER = ByteBuffer.wrap(new byte[10000]);

    private static class ClientEntityInfo {
        SyncEntity entity;
        
        EntitySyncInfo lastSyncInfo;
        EntitySyncInfo lastCreateInfo;
        
        long lastUpdate = 0;
        long lastExtrapolate = -1;
        long lastUpdateRate;
        long lastLatencyDelta = Long.MAX_VALUE;
    }

//    private final ArrayList<EntitySyncInfo> syncQueue =
//                    new ArrayList<EntitySyncInfo>();

    private final IntMap<ClientEntityInfo> entities =
                    new IntMap<ClientEntityInfo>();
    
    private EntityFactory factory;
    private SyncSerializer serializer = new SyncSerializer();
    
    private long lastSyncMsgTime = 0;
    private int lastHeartbeat;
    private MovingAverage averageLatency = new MovingAverage(20);

    public void update(float tpf2){
        long time = System.currentTimeMillis();
        synchronized (entities){
            for (Entry<ClientEntityInfo> entry : entities){
                ClientEntityInfo info = entry.getValue();

                if (info.lastSyncInfo != null){
                    if (!inLoopApplySyncInfo(entry.getKey(), info))
                        continue; // entity was deleted due to this command
                }

                long timeSinceUpdate = time - info.lastUpdate;
                if (timeSinceUpdate >= info.lastUpdateRate){
                    if (info.lastExtrapolate == -1){
                        info.entity.interpolate(1);
                        info.lastExtrapolate = info.lastUpdate + info.lastUpdateRate;
                    }

                    long timeSinceExtrapolate = time - info.lastExtrapolate;
                    info.lastExtrapolate = time;
                    float tpf = timeSinceExtrapolate / 1000f;
                    info.entity.extrapolate(tpf);
                }else{
                    float blendAmount = (float) timeSinceUpdate / (float)info.lastUpdateRate;
                    info.entity.interpolate(blendAmount);
                }
            }
        }
    }

    public ClientSyncService(Client client){
        client.addMessageListener(this /*, SyncMessage.class*/ );
    }

    public void setEntityFactory(EntityFactory factory){
        this.factory = factory;
    }

    public SyncEntity getEntity(int id){
        return entities.get(id).entity;
    }

    private void inLoopCreateEntity(int entityId, ClientEntityInfo clientInfo){
        EntitySyncInfo initInfo = clientInfo.lastSyncInfo;

        Class<? extends SyncEntity> clazz;
        try {
            clazz = (Class<? extends SyncEntity>) Class.forName(initInfo.className);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Cannot find entity class: " + initInfo.className, ex);
        }

        SyncEntity entity;
        if (factory != null){
            entity = factory.createEntity(clazz);
        }else{
            try {
                entity = clazz.newInstance();
            } catch (InstantiationException ex) {
                throw new RuntimeException("Entity class is missing empty constructor", ex);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }

        clientInfo.entity = entity;
        entity.onRemoteCreate();

        serializer.read(entity, ByteBuffer.wrap(initInfo.data), true);

        clientInfo.lastSyncInfo = null;
    }

    private void inLoopSyncEntity(int entityId, ClientEntityInfo entityInfo){
        serializer.read(entityInfo.entity, ByteBuffer.wrap(entityInfo.lastSyncInfo.data), false);
        entityInfo.entity.onRemoteUpdate( entityInfo.lastLatencyDelta / 1000f );

        // clear so its not called again
        entityInfo.lastSyncInfo = null;
        entityInfo.lastLatencyDelta = Long.MAX_VALUE;
    }

    private void inLoopDeleteEntity(int entityId, ClientEntityInfo clientInfo){
        SyncEntity entity = clientInfo.entity;
        entity.onRemoteDelete();
        entities.remove(entityId);
    }

    private boolean inLoopApplySyncInfo(int entityId, ClientEntityInfo clientInfo){
        switch (clientInfo.lastSyncInfo.type){
            case EntitySyncInfo.TYPE_NEW:
                inLoopCreateEntity(entityId, clientInfo);
                return true;
            case EntitySyncInfo.TYPE_SYNC:
                inLoopSyncEntity(entityId, clientInfo);
                return true;
            case EntitySyncInfo.TYPE_DELETE:
                inLoopDeleteEntity(entityId, clientInfo);
                return false;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void createEntity(EntitySyncInfo info){
        ClientEntityInfo entityInfo = new ClientEntityInfo();
        entityInfo.lastUpdate = System.currentTimeMillis();
        
        // forces inLoopCreateEntity to be called later
        entityInfo.lastSyncInfo = info;

        entities.put(info.id, entityInfo);
    }

    private void syncEntity(EntitySyncInfo info, int latencyDelta){
        ClientEntityInfo entityInfo = entities.get(info.id);
        if (entityInfo == null || entityInfo.entity == null)
            return; // didn't receive init yet.
        
        long time = System.currentTimeMillis();
        entityInfo.lastUpdateRate = time - entityInfo.lastUpdate;
        entityInfo.lastUpdate = time;
        entityInfo.lastExtrapolate = -1;

        // forces inLoopSyncEntity to be called later
        entityInfo.lastSyncInfo = info;
        entityInfo.lastLatencyDelta = latencyDelta;
    }

    void deleteEntity(EntitySyncInfo info){
        ClientEntityInfo clientInfo = entities.get(info.id);
        clientInfo.lastSyncInfo = info;
    }

    private void applySyncInfo(EntitySyncInfo info, int latencyDelta){
        switch (info.type) {
            case EntitySyncInfo.TYPE_NEW:
                createEntity(info);
                break;
            case EntitySyncInfo.TYPE_SYNC:
                syncEntity(info, latencyDelta);
                break;
            case EntitySyncInfo.TYPE_DELETE:
                deleteEntity(info);
                break;
        }
    }

    @Override
    public void messageReceived(Message msg) {
        if (!(msg instanceof SyncMessage)) {
            return;
        }

        int latencyDelta = 0;
        if (lastSyncMsgTime == 0) {
            // this is the first syncmessage
            lastSyncMsgTime = System.currentTimeMillis();
        } else {
            long time = System.currentTimeMillis();
            long delta = time - lastSyncMsgTime;
            averageLatency.add(delta);
            lastSyncMsgTime = time;
            latencyDelta = (int) (delta - averageLatency.getAverage());
        }

        SyncMessage sync = (SyncMessage) msg;

        boolean isOldMessage = false;
        int newHeartbeat = sync.heartbeat;
        if (lastHeartbeat > newHeartbeat){
            // check if at the end of heartbeat indices
            // within 1000 heartbeats
            if (lastHeartbeat > Integer.MAX_VALUE - 1000 && newHeartbeat < 1000){
                lastHeartbeat = newHeartbeat;
            }else{
                isOldMessage = true;
            }
        }else{
            lastHeartbeat = newHeartbeat;
        }

        for (EntitySyncInfo info : sync.infos) {
            if (info.type == EntitySyncInfo.TYPE_SYNC && isOldMessage)
                continue; // old sync message, ignore.
            
            applySyncInfo(info, latencyDelta);
        }
        
    }

}
