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

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;

/**
 * <code>RenderQueue</code> is used to queue up and sort 
 * {@link Geometry geometries} for rendering.
 * 
 * @author Kirill Vainer
 */
public class RenderQueue {

    private GeometryList opaqueList;
    private GeometryList guiList;
    private GeometryList transparentList;
    private GeometryList translucentList;
    private GeometryList skyList;
    private GeometryList shadowRecv;
    private GeometryList shadowCast;

    /**
     * Creates a new RenderQueue, the default {@link GeometryComparator comparators}
     * are used for all {@link GeometryList geometry lists}.
     */
    public RenderQueue() {
        this.opaqueList = new GeometryList(new OpaqueComparator());
        this.guiList = new GeometryList(new GuiComparator());
        this.transparentList = new GeometryList(new TransparentComparator());
        this.translucentList = new GeometryList(new TransparentComparator());
        this.skyList = new GeometryList(new NullComparator());
        this.shadowRecv = new GeometryList(new OpaqueComparator());
        this.shadowCast = new GeometryList(new OpaqueComparator());
    }

    /**
     * The render queue <code>Bucket</code> specifies the bucket
     * to which the spatial will be placed when rendered. 
     * <p>
     * The behavior of the rendering will differ depending on which 
     * bucket the spatial is placed. A spatial's queue bucket can be set
     * via {@link Spatial#setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket) }.
     */
    public enum Bucket {
        /**
         * The renderer will try to find the optimal order for rendering all 
         * objects using this mode.
         * You should use this mode for most normal objects, except transparent
         * ones, as it could give a nice performance boost to your application.
         */
        Opaque,
        
        /**
         * This is the mode you should use for object with
         * transparency in them. It will ensure the objects furthest away are
         * rendered first. That ensures when another transparent object is drawn on
         * top of previously drawn objects, you can see those (and the object drawn
         * using Opaque) through the transparent parts of the newly drawn
         * object. 
         */
        Transparent,
        
        /**
         * A special mode used for rendering really far away, flat objects - 
         * e.g. skies. In this mode, the depth is set to infinity so 
         * spatials in this bucket will appear behind everything, the downside
         * to this bucket is that 3D objects will not be rendered correctly
         * due to lack of depth testing.
         */
        Sky,
        
        /**
         * A special mode used for rendering transparent objects that
         * should not be effected by {@link SceneProcessor}. 
         * Generally this would contain translucent objects, and
         * also objects that do not write to the depth buffer such as
         * particle emitters.
         */
        Translucent,
        
        /**
         * This is a special mode, for drawing 2D object
         * without perspective (such as GUI or HUD parts).
         * The spatial's world coordinate system has the range
         * of [0, 0, -1] to [Width, Height, 1] where Width/Height is
         * the resolution of the screen rendered to. Any spatials
         * outside of that range are culled.
         */
        Gui,
        
        /**
         * A special mode, that will ensure that this spatial uses the same
         * mode as the parent Node does.
         */
        Inherit,
    }

    /**
     * <code>ShadowMode</code> is a marker used to specify how shadow
     * effects should treat the spatial.
     */
    public enum ShadowMode {
        /**
         * Disable both shadow casting and shadow receiving for this spatial.
         * Generally used for special effects like particle emitters.
         */
        Off,
        
        /**
         * Enable casting of shadows but not receiving them. 
         */
        Cast,
        
        /**
         * Enable receiving of shadows but not casting them.
         */
        Receive,
        
        /**
         * Enable both receiving and casting of shadows.
         */
        CastAndReceive,
        
        /**
         * Inherit the <code>ShadowMode</code> from the parent node.
         */
        Inherit
    }

    /**
     *  Sets a different geometry comparator for the specified bucket, one
     *  of Gui, Opaque, Sky, or Transparent.  The GeometryComparators are
     *  used to sort the accumulated list of geometries before actual rendering
     *  occurs.
     *
     *  <p>The most significant comparator is the one for the transparent
     *  bucket since there is no correct way to sort the transparent bucket
     *  that will handle all geometry all the time.  In certain cases, the
     *  application may know the best way to sort and now has the option of
     *  configuring a specific implementation.</p>
     *
     *  <p>The default comparators are:</p>
     *  <ul>
     *  <li>Bucket.Opaque: {@link com.jme3.renderer.queue.OpaqueComparator} which sorts
     *                     by material first and front to back within the same material.
     *  <li>Bucket.Transparent: {@link com.jme3.renderer.queue.TransparentComparator} which
     *                     sorts purely back to front by leading bounding edge with no material sort.
     *  <li>Bucket.Translucent: {@link com.jme3.renderer.queue.TransparentComparator} which
     *                     sorts purely back to front by leading bounding edge with no material sort. this bucket is rendered after post processors.
     *  <li>Bucket.Sky: {@link com.jme3.renderer.queue.NullComparator} which does no sorting
     *                     at all.
     *  <li>Bucket.Gui: {@link com.jme3.renderer.queue.GuiComparator} sorts geometries back to
     *                     front based on their Z values.
     */
    public void setGeometryComparator(Bucket bucket, GeometryComparator c) {
        switch (bucket) {
            case Gui:
                guiList = new GeometryList(c);
                break;
            case Opaque:
                opaqueList = new GeometryList(c);
                break;
            case Sky:
                skyList = new GeometryList(c);
                break;
            case Transparent:
                transparentList = new GeometryList(c);
                break;
            case Translucent:
                translucentList = new GeometryList(c);
                break;
            default:
                throw new UnsupportedOperationException("Unknown bucket type: " + bucket);
        }
    }

    /**
     * Adds a geometry to a shadow bucket.
     * Note that this operation is done automatically by the
     * {@link RenderManager}. {@link SceneProcessor}s that handle
     * shadow rendering should fetch the queue by using
     * {@link #getShadowQueueContent(com.jme3.renderer.queue.RenderQueue.ShadowMode) },
     * by default no action is taken on the shadow queues.
     * 
     * @param g The geometry to add
     * @param shadBucket The shadow bucket type, if it is
     * {@link ShadowMode#CastAndReceive}, it is added to both the cast
     * and the receive buckets.
     */
    public void addToShadowQueue(Geometry g, ShadowMode shadBucket) {
        switch (shadBucket) {
            case Inherit:
                break;
            case Off:
                break;
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
                throw new UnsupportedOperationException("Unrecognized shadow bucket type: " + shadBucket);
        }
    }

    /**
     * Adds a geometry to the given bucket.
     * The {@link RenderManager} automatically handles this task
     * when flattening the scene graph. The bucket to add
     * the geometry is determined by {@link Geometry#getQueueBucket() }.
     * 
     * @param g  The geometry to add
     * @param bucket The bucket to add to, usually 
     * {@link Geometry#getQueueBucket() }.
     */
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
            case Translucent:
                translucentList.add(g);
                break;
            default:
                throw new UnsupportedOperationException("Unknown bucket type: " + bucket);
        }
    }

    /**
     * 
     * @param shadBucket
     * @return 
     */
    public GeometryList getShadowQueueContent(ShadowMode shadBucket) {
        switch (shadBucket) {
            case Cast:
                return shadowCast;
            case Receive:
                return shadowRecv;
            default:
                throw new IllegalArgumentException("Only Cast or Receive are allowed");
        }
    }

    private void renderGeometryList(GeometryList list, RenderManager rm, Camera cam, boolean clear) {
        list.setCamera(cam); // select camera for sorting
        list.sort();
        for (int i = 0; i < list.size(); i++) {
            Geometry obj = list.get(i);
            assert obj != null;
            rm.renderGeometry(obj);
            obj.queueDistance = Float.NEGATIVE_INFINITY;
        }
        if (clear) {
            list.clear();
        }
    }

    public void renderShadowQueue(GeometryList list, RenderManager rm, Camera cam, boolean clear) {
        renderGeometryList(list, rm, cam, clear);
    }

    public void renderShadowQueue(ShadowMode shadBucket, RenderManager rm, Camera cam, boolean clear) {
        switch (shadBucket) {
            case Cast:
                renderGeometryList(shadowCast, rm, cam, clear);
                break;
            case Receive:
                renderGeometryList(shadowRecv, rm, cam, clear);
                break;
            default:
                throw new IllegalArgumentException("Unexpected shadow bucket: " + shadBucket);
        }
    }

    public boolean isQueueEmpty(Bucket bucket) {
        switch (bucket) {
            case Gui:
                return guiList.size() == 0;
            case Opaque:
                return opaqueList.size() == 0;
            case Sky:
                return skyList.size() == 0;
            case Transparent:
                return transparentList.size() == 0;
            case Translucent:
                return translucentList.size() == 0;
            default:
                throw new UnsupportedOperationException("Unsupported bucket type: " + bucket);
        }
    }

    public void renderQueue(Bucket bucket, RenderManager rm, Camera cam) {
        renderQueue(bucket, rm, cam, true);
    }

    public void renderQueue(Bucket bucket, RenderManager rm, Camera cam, boolean clear) {
        switch (bucket) {
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
            case Translucent:
                renderGeometryList(translucentList, rm, cam, clear);
                break;

            default:
                throw new UnsupportedOperationException("Unsupported bucket type: " + bucket);
        }
    }

    public void clear() {
        opaqueList.clear();
        guiList.clear();
        transparentList.clear();
        translucentList.clear();
        skyList.clear();
        shadowCast.clear();
        shadowRecv.clear();
    }
}
