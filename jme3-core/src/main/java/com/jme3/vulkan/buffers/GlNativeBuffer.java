package com.jme3.vulkan.buffers;

import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.memory.MemorySize;

import java.lang.ref.WeakReference;

public class GlNativeBuffer extends NioBuffer implements Native<Integer> {

    private GLRenderer renderer;
    private GlVertexBuffer.Usage usage = GlVertexBuffer.Usage.Dynamic;
    private boolean updateNeeded = true;
    private WeakReference<GlNativeBuffer> weakRef;
    private int object = -1;

    public GlNativeBuffer(MemorySize size) {
        super(size);
    }

    public GlNativeBuffer(MemorySize size, int padding) {
        super(size, padding);
    }

    public GlNativeBuffer(MemorySize size, int padding, boolean clearMem) {
        super(size, padding, clearMem);
        ref.refresh();
    }

    @Override
    public Runnable createDestroyer() {
        Runnable sup = super.createDestroyer();
        return () -> {
            if (object >= 0) {
                renderer.deleteBuffer(object);
            }
            sup.run();
        };
    }

    @Override
    public Integer getNativeObject() {
        return object;
    }

    @Override
    public DisposableReference getReference() {
        return ref;
    }

    public void initialize(GLRenderer renderer, int id) {
        if (object < 0) {
            this.renderer = renderer;
            this.object = id;
            ref.refresh();
        }
    }

    public boolean isInitialized() {
        return object >= 0;
    }

    public void setUsage(GlVertexBuffer.Usage usage) {
        this.usage = usage;
    }

    public GlVertexBuffer.Usage getUsage() {
        return usage;
    }

    public void setUpdateNeeded() {
        updateNeeded = true;
    }

    public void clearUpdateNeeded() {
        updateNeeded = false;
    }

    public boolean isUpdateNeeded() {
        return updateNeeded;
    }

    public WeakReference<GlNativeBuffer> getWeakReference() {
        if (weakRef == null) {
            weakRef = new WeakReference<>(this);
        }
        return weakRef;
    }

}
