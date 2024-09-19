/*
 * Copyright (c) 2024 jMonkeyEngine
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
package com.jme3.renderer;

import com.jme3.profile.AppProfiler;
import com.jme3.profile.VpStep;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class RenderUtils {
    
    /**
     * Renders ViewPort queue buckets according to {@link RenderManager#renderViewPortQueues(com.jme3.renderer.ViewPort, boolean)}.
     * <p>
     * <ol>
     *   <li>Opaque: normal settings.</li>
     *   <li>Sky: depth range set to {1, 1}.</li>
     *   <li>Transparent: normal settings.</li>
     *   <li>Gui: depth range set to {0, 0} with parallel projection.
     *   <li>Translucent: not rendered.</li>
     * </ol>
     * Profiling for each rendered bucket is performed.
     * 
     * @param rm render manager
     * @param vp viewport to render queues from
     * @param handler geometry handler (null to use {@link GeometryRenderHandler#DEFAULT})
     * @param flush true to flush rendered geometries from buckets
     */
    public static void renderViewPortQueues(RenderManager rm, ViewPort vp, GeometryRenderHandler handler, boolean flush) {
        
        /**
         * Copied and repurposed from RenderManager#renderViewPortQueues.
         */
        
        if (handler == null) {
            handler = GeometryRenderHandler.DEFAULT;
        }
        boolean depthRangeChanged = false;
        Renderer renderer = rm.getRenderer();
        AppProfiler prof = rm.getProfiler();
        RenderQueue rq = vp.getQueue();
        Camera cam = vp.getCamera();
        
        // render opaque
        if (prof != null) {
            prof.vpStep(VpStep.RenderBucket, vp, Bucket.Opaque);
        }
        renderGeometryList(rm, cam, rq.getList(Bucket.Opaque), handler, flush);
        
        // render sky
        if (!rq.isQueueEmpty(Bucket.Sky)) {
            if (prof != null) {
                prof.vpStep(VpStep.RenderBucket, vp, Bucket.Sky);
            }
            renderer.setDepthRange(1, 1);
            //rq.renderQueue(Bucket.Sky, this, cam, flush);
            renderGeometryList(rm, cam, rq.getList(Bucket.Sky), handler, flush);
            depthRangeChanged = true;
        }
        
        // render transparent
        if (!rq.isQueueEmpty(Bucket.Transparent)) {
            if (prof != null) {
                prof.vpStep(VpStep.RenderBucket, vp, Bucket.Transparent);
            }
            if (depthRangeChanged) {
                renderer.setDepthRange(0, 1);
                depthRangeChanged = false;
            }
            renderGeometryList(rm, cam, rq.getList(Bucket.Transparent), handler, flush);
        }
        
        // render gui
        if (!rq.isQueueEmpty(Bucket.Gui)) {
            if (prof != null) {
                prof.vpStep(VpStep.RenderBucket, vp, Bucket.Gui);
            }
            renderer.setDepthRange(0, 0);
            rm.setCamera(cam, true);
            //rq.renderQueue(Bucket.Gui, this, cam, flush);
            renderGeometryList(rm, cam, rq.getList(Bucket.Gui), handler, flush);
            rm.setCamera(cam, false);
            depthRangeChanged = true;
        }
        
        // reset
        if (depthRangeChanged) {
            renderer.setDepthRange(0, 1);
        }
        
    }
    
    /**
     * Renders the transparent queue according to {@link RenderManager#renderTranslucentQueue(com.jme3.renderer.ViewPort)}.
     * 
     * @param rm
     * @param vp
     * @param handler
     * @param flush 
     */
    public static void renderTransparentQueue(RenderManager rm, ViewPort vp, GeometryRenderHandler handler, boolean flush) {
        AppProfiler prof = rm.getProfiler();
        if (prof != null) {
            prof.vpStep(VpStep.RenderBucket, vp, Bucket.Translucent);
        }
        RenderQueue rq = vp.getQueue();
        if (!rq.isQueueEmpty(Bucket.Translucent)) {
            renderGeometryList(rm, vp.getCamera(), rq.getList(Bucket.Translucent), handler, flush);
        }
    }
    
    /**
     * Renders the geometry list sorted according to the given camera.
     * 
     * @param rm render manager
     * @param cam camera to sort geometries by
     * @param list list of geometries to render
     * @param handler geometry handler (null to use {@link GeometryRenderHandler#DEFAULT})
     * @param flush true to flush rendered geometries from list
     */
    public static void renderGeometryList(RenderManager rm, Camera cam, GeometryList list, GeometryRenderHandler handler, boolean flush) {
        if (handler == null) {
            handler = GeometryRenderHandler.DEFAULT;
        }
        list.setCamera(cam);
        list.sort();
        LinkedList<Geometry> saved = new LinkedList<>();
        for (Geometry g : list) {
            assert g != null;
            if (!handler.renderGeometry(rm, g) && flush) {
                saved.add(g);
            }
            g.queueDistance = Float.NEGATIVE_INFINITY;
        }
        if (flush) {
            list.clear();
            for (Geometry g : saved) {
                list.add(g);
            }
        }
    }
    
}
