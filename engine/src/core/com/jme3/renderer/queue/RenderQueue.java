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

package com.jme3.renderer.queue;

import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;

public class RenderQueue {

    private GeometryList opaqueList;
    private GeometryList guiList;
    private GeometryList transparentList;
    private GeometryList skyList;
    private GeometryList shadowRecv;
    private GeometryList shadowCast;

    public RenderQueue(){
        this.opaqueList =  new GeometryList(new OpaqueComparator());
        this.guiList = new GeometryList(new GuiComparator());
        this.transparentList = new GeometryList(new TransparentComparator());
        this.skyList = new GeometryList(new NullComparator());
        this.shadowRecv = new GeometryList(new OpaqueComparator());
        this.shadowCast = new GeometryList(new OpaqueComparator());
    }

    public enum Bucket {
        Gui,
        Opaque,
        Sky,
        Transparent,
        Inherit,
    }

    public enum ShadowMode {
        Off,
        Cast,
        Receive,
        CastAndReceive,
        Inherit
    }

    public void addToShadowQueue(Geometry g, ShadowMode shadBucket){
        switch (shadBucket){
            case Inherit: break;
            case Off: break;
            case Cast:
                shadowCast.add(g);
                break;
            case Receive:
                shadowRecv.add(g);
                break;
            case CastAndReceive:
                shadowCast.add(g);
                shadowRecv.add(g);
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized shadow bucket type: "+shadBucket);
        }
    }

    public void addToQueue(Geometry g, Bucket bucket) {
        switch (bucket) {
            case Gui:
                guiList.add(g);
                break;
            case Opaque:
                opaqueList.add(g);
                break;
            case Sky:
                skyList.add(g);
                break;
            case Transparent:
                transparentList.add(g);
                break;
            default:
                throw new UnsupportedOperationException("Unknown bucket type: "+bucket);
        }
    }

    public GeometryList getShadowQueueContent(ShadowMode shadBucket){
        switch (shadBucket){
            case Cast:
                return shadowCast;
            case Receive:
                return shadowRecv;
            default:
                return null;
        }
    }

    private void renderGeometryList(GeometryList list, RenderManager rm, Camera cam, boolean clear){
        list.setCamera(cam); // select camera for sorting
        list.sort();
        for (int i = 0; i < list.size(); i++){
            Spatial obj = list.get(i);
            assert obj != null;           
            //if(obj.checkCulling(cam)){
                if (obj instanceof Geometry){
                    Geometry g = (Geometry) obj;
                    rm.renderGeometry(g);
                    // make sure to reset queue distance
                }
            //}
            if (obj != null)
                obj.queueDistance = Float.NEGATIVE_INFINITY;
        }
        if (clear)
            list.clear();
    }

    public void renderShadowQueue(GeometryList list, RenderManager rm, Camera cam, boolean clear){
          renderGeometryList(list, rm, cam, clear);
    }

    public void renderShadowQueue(ShadowMode shadBucket, RenderManager rm, Camera cam, boolean clear){
        switch (shadBucket){
            case Cast:
                renderGeometryList(shadowCast, rm, cam, clear);
                break;
            case Receive:
                renderGeometryList(shadowRecv, rm, cam, clear);
                break;
        }
    }

    public boolean isQueueEmpty(Bucket bucket){
        switch (bucket){
            case Gui:
                return guiList.size() == 0;
            case Opaque:
                return opaqueList.size() == 0;
            case Sky:
                return skyList.size() == 0;
            case Transparent:
                return transparentList.size() == 0;
            default:
                throw new UnsupportedOperationException("Unsupported bucket type: "+bucket);
        }
    }

    public void renderQueue(Bucket bucket, RenderManager rm, Camera cam){
        renderQueue(bucket, rm, cam, true);
    }

    public void renderQueue(Bucket bucket, RenderManager rm, Camera cam, boolean clear){
        switch (bucket){
            case Gui:
                renderGeometryList(guiList, rm, cam, clear);
                break;
            case Opaque:
                renderGeometryList(opaqueList, rm, cam, clear);
                break;
            case Sky:
                renderGeometryList(skyList, rm, cam, clear);
                break;
            case Transparent:
                renderGeometryList(transparentList, rm, cam, clear);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported bucket type: "+bucket);
        }
    }

    public void clear(){
        opaqueList.clear();
        guiList.clear();
        transparentList.clear();
        skyList.clear();
        shadowCast.clear();
        shadowRecv.clear();
    }

}
