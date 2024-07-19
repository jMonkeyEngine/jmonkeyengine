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
package com.jme3.renderer.framegraph.passes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.DepthRange;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.GeometryQueue;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.queue.GeometryComparator;
import com.jme3.renderer.queue.GuiComparator;
import com.jme3.renderer.queue.NullComparator;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.TransparentComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Enqueues geometries into different {@link GeometryQueue}s based on world
 * render bucket value.
 * <p>
 * Outputs vary based on what GeometryQueues are added. If default queues are
 * added (via {@link #SceneEnqueuePass(boolean, boolean)}), then the outputs
 * include: "Opaque", "Sky", "Transparent", "Gui", and "Translucent". All outputs
 * are GeometryQueues.
 * <p>
 * A geometry is placed in queues according to the userdata found at
 * {@link #QUEUE} (expected as String) according to ancestor inheritance, or the
 * value returned by {@link Geometry#getQueueBucket()} (converted to String).
 * Userdata value (if found) trumps queue bucket value.
 * 
 * @author codex
 */
public class SceneEnqueuePass extends RenderPass {
    
    /**
     * Userdata key for denoting the queue the spatial should be sorted into.
     */
    public static final String QUEUE = "SceneEnqueuePass.RenderQueue";
    
    /**
     * Userdata value for inheriting the queue of the spatial's parent.
     */
    public static final String INHERIT = RenderQueue.Bucket.Inherit.name();
    
    public static final String
            OPAQUE = "Opaque",
            SKY = "Sky",
            TRANSPARENT = "Transparent",
            GUI = "Gui",
            TRANSLUCENT = "Translucent";
    
    private boolean runControlRender = true;
    private final HashMap<String, Queue> buckets = new HashMap<>();
    private String defaultBucket = OPAQUE;

    /**
     * Initialize an instance with default settings.
     * <p>
     * Default queues are not added.
     */
    public SceneEnqueuePass() {}
    /**
     * 
     * @param runControlRender true to have this pass run {@link com.jme3.scene.control.Control} renders
     * @param useDefaultBuckets true to have default queues registered
     */
    public SceneEnqueuePass(boolean runControlRender, boolean useDefaultBuckets) {
        this.runControlRender = runControlRender;
        if (useDefaultBuckets) {
            add(OPAQUE, new OpaqueComparator());
            add(SKY, null, DepthRange.REAR, true);
            add(TRANSPARENT, new TransparentComparator());
            add(GUI, new GuiComparator(), DepthRange.FRONT, false);
            add(TRANSLUCENT, new TransparentComparator());
        }
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        for (Queue b : buckets.values()) {
            b.geometry = addOutput(b.name);
            b.lights = addOutput(b.name+"Lights");
        }
    }
    @Override
    protected void prepare(FGRenderContext context) {
        for (Queue b : buckets.values()) {
            declare(null, b.geometry);
            declare(null, b.lights);
        }
    }
    @Override
    protected void execute(FGRenderContext context) {
        ViewPort vp = context.getViewPort();
        List<Spatial> scenes = vp.getScenes();
        for (int i = scenes.size()-1; i >= 0; i--) {
            vp.getCamera().setPlaneState(0);
            queueSubScene(context, scenes.get(i), null);
        }
        System.out.println("set queue resources");
        for (Queue b : buckets.values()) {
            resources.setPrimitive(b.geometry, b.queue);
            resources.setPrimitive(b.lights, b.lightList);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {
        for (Queue b : buckets.values()) {
            b.queue.clear();
            b.lightList.clear();
        }
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(runControlRender, "runControlRender", true);
        ArrayList<Queue> list = new ArrayList<>();
        list.addAll(buckets.values());
        out.writeSavableArrayList(list, "buckets", new ArrayList<>());
        out.write(defaultBucket, "defaultBucket", OPAQUE);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        runControlRender = in.readBoolean("runControlRender", true);
        ArrayList<Savable> list = in.readSavableArrayList("buckets", new ArrayList<>());
        for (Savable s : list) {
            Queue b = (Queue)s;
            buckets.put(b.name, b);
        }
        defaultBucket = in.readString("defaultBucket", OPAQUE);
    }
    
    private void queueSubScene(FGRenderContext context, Spatial spatial, String parentBucket) {
        // check culling
        Camera cam = context.getViewPort().getCamera();
        if (!spatial.checkCulling(cam)) {
            return;
        }
        // render controls
        if (runControlRender) {
            spatial.runControlRender(context.getRenderManager(), context.getViewPort());
        }
        // get target bucket
        String value = getSpatialBucket(spatial, parentBucket);
        Queue bucket = (value != null ? buckets.get(value) : null);
        // accumulate lights
        if (bucket != null) for (Light l : spatial.getLocalLightList()) {
            bucket.lightList.add(l);
        }
        if (spatial instanceof Node) {
            int camState = cam.getPlaneState();
            for (Spatial s : ((Node)spatial).getChildren()) {
                // restore cam state before queueing children
                cam.setPlaneState(camState);
                queueSubScene(context, s, value);
            }
        } else if (bucket != null && spatial instanceof Geometry) {
            // add to the render queue
            Geometry g = (Geometry)spatial;
            if (g.getMaterial() == null) {
                throw new IllegalStateException("No material is set for Geometry: " + g.getName());
            }
            bucket.queue.add(g);
        }
    }
    private String getSpatialBucket(Spatial spatial, String parentValue) {
        String value = spatial.getUserData(QUEUE);
        if (value == null) {
            value = spatial.getLocalQueueBucket().name();
        }
        if (value.equals(INHERIT)) {
            if (parentValue != null) {
                value = parentValue;
            } else if (spatial.getParent() != null) {
                value = getSpatialBucket(spatial.getParent(), null);
            } else {
                value = defaultBucket;
            }
        }
        return value;
    }
    
    /**
     * Adds a queue with the name and comparator.
     * <p>
     * If a bucket already exists under the name, it will be replaced.
     * 
     * @param name name of the queue corresponding to the output name
     * @param comparator sorts geometries within the queue
     * @return this instance
     * @throws IllegalStateException if called while assigned to a framegraph
     */
    public final SceneEnqueuePass add(String name, GeometryComparator comparator) {
        return add(name, comparator, DepthRange.IDENTITY, true);
    }
    /**
     * Adds a queue with the name, comparator, depth range, and perspective mode.
     * 
     * @param name name of the queue corresponding to the output name.
     * @param comparator sorts geometries within the queue
     * @param depth range in which geometries in the bucket will be rendered within
     * @param perspective true to render geometries in the bucket in perspective mode (versus orthogonal)
     * @return this instance
     * @throws IllegalStateException if called while assigned to a framegraph
     */
    public final SceneEnqueuePass add(String name, GeometryComparator comparator, DepthRange depth, boolean perspective) {
        if (isAssigned()) {
            throw new IllegalStateException("Cannot add buckets while assigned to a framegraph.");
        }
        buckets.put(name, new Queue(name, comparator, depth, perspective));
        return this;
    }
    
    /**
     * Sets this pass to render controls when traversing the scene.
     * <p>
     * default=true
     * 
     * @param runControlRender 
     */
    public void setRunControlRender(boolean runControlRender) {
        this.runControlRender = runControlRender;
    }
    /**
     * Sets the default bucket geometries are added to if their
     * hierarchy only calls for {@link #INHERIT}.
     * <p>
     * default={@link #OPAQUE}
     * 
     * @param defaultBucket 
     */
    public void setDefaultBucket(String defaultBucket) {
        this.defaultBucket = defaultBucket;
    }

    /**
     * 
     * @return 
     */
    public boolean isRunControlRender() {
        return runControlRender;
    }
    /**
     * 
     * @return 
     */
    public String getDefaultBucket() {
        return defaultBucket;
    }
    
    private static class Queue implements Savable {
        
        public static final NullComparator NULL_COMPARATOR = new NullComparator();
        
        public String name;
        public GeometryQueue queue;
        public final LightList lightList = new LightList(null);
        public ResourceTicket<GeometryQueue> geometry;
        public ResourceTicket<LightList> lights;
        
        public Queue() {}
        public Queue(String name, GeometryComparator comparator, DepthRange depth, boolean perspective) {
            if (comparator == null) {
                comparator = Queue.NULL_COMPARATOR;
            }
            this.name = name;
            this.queue = new GeometryQueue(comparator);
            this.queue.setDepth(depth);
            this.queue.setPerspective(perspective);
        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule out = ex.getCapsule(this);
            out.write(name, "name", "Opaque");
            out.write(queue.getComparator(), "comparator", NULL_COMPARATOR);
            out.write(queue.getDepth(), "depth", DepthRange.IDENTITY);
            out.write(queue.isPerspective(), "perspective", true);
        }
        @Override
        public void read(JmeImporter im) throws IOException {
            InputCapsule in = im.getCapsule(this);
            name = in.readString("name", "Opaque");
            queue = new GeometryQueue(in.readSavable("comparator", GeometryComparator.class, NULL_COMPARATOR));
            queue.setDepth(in.readSavable("depth", DepthRange.class, DepthRange.IDENTITY));
            queue.setPerspective(in.readBoolean("perspective", true));
        }
        
    }
    
}
