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

import com.jme3.math.FastMath;
import com.jme3.network.connection.Client;
import com.jme3.network.connection.Server;
import com.jme3.network.events.ConnectionAdapter;
import com.jme3.network.service.Service;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ServerSyncService extends ConnectionAdapter implements Service {

    private static final ByteBuffer BUFFER = ByteBuffer.wrap(new byte[10000]);

    private float updateRate = 0.1f;
    private float packetDropRate = 0;
    private long latency = 0;
    private HashMap<Long, SyncMessage> latencyQueue;

    private final Server server;
    private final SyncSerializer serializer = new SyncSerializer();
//    private final ArrayList<Client> connectedClients = new ArrayList<Client>();
    private final ArrayList<SyncEntity> npcs = new ArrayList<SyncEntity>();
    private final HashMap<SyncEntity, Integer> npcToId
            = new HashMap<SyncEntity, Integer>();

    private static int nextId = 0;

    private int heartbeat = 0;
    private float time = 0;

    public ServerSyncService(Server server){
        this.server = server;
        server.addConnectionListener(this);
    }

    public void setNetworkSimulationParams(float packetDropRate, long latency){
        if (latencyQueue == null)
            latencyQueue = new HashMap<Long, SyncMessage>();

        this.packetDropRate = packetDropRate;
        this.latency = latency;
    }

    private EntitySyncInfo generateInitInfo(SyncEntity entity, boolean newId){
        EntitySyncInfo info = new EntitySyncInfo();
        info.className = entity.getClass().getName();
        info.id = newId ? nextId ++ : npcToId.get(entity);
        info.type = EntitySyncInfo.TYPE_NEW;

        BUFFER.clear();
        serializer.write(entity, BUFFER, true);
        BUFFER.flip();
        info.data = new byte[BUFFER.limit()];
        BUFFER.get(info.data);
        return info;
    }

    private EntitySyncInfo generateSyncInfo(SyncEntity entity){
        EntitySyncInfo info = new EntitySyncInfo();
        info.className = null;
        info.id = npcToId.get(entity);
        info.type = EntitySyncInfo.TYPE_SYNC;

        BUFFER.clear();
        serializer.write(entity, BUFFER, false);
        BUFFER.flip();
        info.data = new byte[BUFFER.limit()];
        BUFFER.get(info.data);
        return info;
    }

    private EntitySyncInfo generateDeleteInfo(SyncEntity entity){
        EntitySyncInfo info = new EntitySyncInfo();
        info.className = null;
        info.id = npcToId.get(entity);
        info.type = EntitySyncInfo.TYPE_DELETE;
        return info;
    }

    public void addNpc(SyncEntity entity){
        EntitySyncInfo info = generateInitInfo(entity, true);
        SyncMessage syncMsg = new SyncMessage();
        syncMsg.setReliable(true);
        syncMsg.heartbeat = heartbeat;
        syncMsg.infos = new EntitySyncInfo[]{ info };

        try {
            server.broadcast(syncMsg);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        synchronized (npcs){
            npcs.add(entity);
            npcToId.put(entity, info.id);
        }
    }

    public void removeNpc(SyncEntity entity){
        EntitySyncInfo info = generateDeleteInfo(entity);

        SyncMessage syncMsg = new SyncMessage();
        syncMsg.setReliable(true);
        syncMsg.heartbeat = heartbeat;
        syncMsg.infos = new EntitySyncInfo[]{ info };

        try {
            server.broadcast(syncMsg);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        synchronized (npcs){
            npcs.remove(entity);
            npcToId.remove(entity);
        }
    }

    @Override
    public void clientConnected(Client client){
        System.out.println("Server: Client connected: " + client);
        SyncMessage msg = new SyncMessage();
        msg.setReliable(true); // sending INIT information, has to be reliable.

        msg.heartbeat = heartbeat;
        EntitySyncInfo[] infos = new EntitySyncInfo[npcs.size()];
        msg.infos = infos;
        synchronized (npcs){
            for (int i = 0; i < npcs.size(); i++){
                SyncEntity entity = npcs.get(i);
                EntitySyncInfo info = generateInitInfo(entity, false);
                infos[i] = info;
            }
            
            try {
                client.send(msg);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void clientDisconnected(Client client){
        System.out.println("Server: Client disconnected: " + client);
    }

    private void sendDelayedMessages(){
        ArrayList<Long> removeList = new ArrayList<Long>();
        for (Map.Entry<Long, SyncMessage> entry : latencyQueue.entrySet()){
            if (entry.getKey() > System.currentTimeMillis())
                continue;

            removeList.add(entry.getKey());
            if (packetDropRate > FastMath.nextRandomFloat())
                continue;

            for (Client client : server.getConnectors()){
                try {
                    client.send(entry.getValue());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        for (Long removeEntry : removeList)
            latencyQueue.remove(removeEntry);
    }

    public void update(float tpf){
        if (latencyQueue != null)
            sendDelayedMessages();

        if (npcs.size() == 0)
            return;

        time += tpf;
        if (time < updateRate){
            return;
        }else{
            time = 0;
        }

        SyncMessage msg = new SyncMessage();
        msg.setReliable(false); // Purely SYNC message, reliability not needed

        msg.heartbeat = heartbeat;
        synchronized (npcs){
            EntitySyncInfo[] infos = new EntitySyncInfo[npcs.size()];
            msg.infos = infos;
            for (int i = 0; i < npcs.size(); i++){
                SyncEntity entity = npcs.get(i);
                EntitySyncInfo info = generateSyncInfo(entity);
                entity.onLocalUpdate();
                infos[i] = info;
            }
        }

        if (latencyQueue != null){
            long latencyTime = (long) (latency + (FastMath.nextRandomFloat()-0.5f) * latency);
            long timeToSend = System.currentTimeMillis() + latencyTime;
            latencyQueue.put(timeToSend, msg);
        }else{
            for (Client client : server.getConnectors()){
                try {
                    client.send(msg); // unreliable
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        heartbeat++;
        if (heartbeat < 0){
            // overflow detected
            heartbeat = 0;
        }
    }

}
