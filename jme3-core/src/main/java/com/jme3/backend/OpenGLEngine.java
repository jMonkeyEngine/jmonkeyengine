package com.jme3.backend;

import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.camera.GuiCamera;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.GlNativeBuffer;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.render.GeometryBatch;
import com.jme3.vulkan.render.GlGeometryBatch;
import com.jme3.vulkan.render.RenderEngine;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.ScenePropertyStack;
import com.jme3.vulkan.util.SceneStack;

public class OpenGLEngine implements RenderEngine {

    private final GLRenderer renderer = new GLRenderer();
    private GlGeometryBatch opaque, sky, transparent, gui, translucent;
    private final SceneStack<RenderQueue.Bucket> bucket = new ScenePropertyStack<>(
            RenderQueue.Bucket.Opaque, RenderQueue.Bucket.Inherit,
            s -> RenderQueue.Bucket.valueOf(s.getLocalQueueBucket()));

    @Override
    public void render(ViewPort vp) {
        opaque.setCamera(vp.getCamera());
        sky.setCamera(vp.getCamera());
        transparent.setCamera(vp.getCamera());
        gui.setCamera(new GuiCamera(vp.getCamera(), vp.getViewWidth(), vp.getViewHeight()));
        translucent.setCamera(vp.getCamera());
        SceneStack<Camera.FrustumIntersect> cull = vp.getCamera().createCullStack();
        for (Spatial scene : vp.getScenes()) for (Spatial.GraphIterator it = scene.iterator(cull, bucket); it.hasNext();) {
            Spatial child = it.next();
            child.runControlRender(this, vp);
            if (cull.peek() != Camera.FrustumIntersect.Outside) {
                if (child instanceof Geometry) {
                    Geometry g = (Geometry) child;
                    if (vp.getGeometryFilter() == null || vp.getGeometryFilter().test(g)) {
                        GeometryBatch<?> batch = getBatchByQueueBucket(bucket.peek());
                        if (batch != null) {
                            batch.add((Geometry)child);
                        }
                    }
                }
            } else {
                it.skipChildren();
            }
        }
        opaque.render();
        sky.render(() -> renderer.setDepthRange(1f, 1f));
        transparent.render(() -> renderer.setDepthRange(0f, 1f));
        gui.render(() -> renderer.setDepthRange(0f, 0f));
        translucent.render(() -> renderer.setDepthRange(0f, 1f));
        opaque.clear();
        sky.clear();
        transparent.clear();
        gui.clear();
        translucent.clear();
    }

    private GeometryBatch<?> getBatchByQueueBucket(RenderQueue.Bucket bucket) {
        switch (bucket) {
            case Opaque: return opaque;
            case Sky: return sky;
            case Transparent: return transparent;
            case Gui: return gui;
            case Translucent: return translucent;
            default: return null;
        }
    }

    @Override
    public Material createMaterial() {
        return null;
    }

    @Override
    public Material createMaterial(String matdefName) {
        return null;
    }

    @Override
    public Mesh createMesh(int vertices, int instances) {
        return null;
    }

    @Override
    public MappableBuffer createBuffer(MemorySize size, Flag<BufferUsage> bufUsage, GlVertexBuffer.Usage dataUsage) {
        return new GlNativeBuffer(size);
    }

    @Override
    public void render() {

    }

    @Override
    public void addViewPort(ViewPort vp) {

    }

    @Override
    public boolean removeViewPort(ViewPort vp) {
        return false;
    }

}
