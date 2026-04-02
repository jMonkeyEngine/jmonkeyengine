package com.jme3.vulkan.buffers;

import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableReference;

import java.lang.ref.WeakReference;
import java.util.logging.Logger;

public class GlBuffer extends NioBuffer implements Disposable {

    private static final Logger logger = Logger.getLogger(GlBuffer.class.getName());
    private static final int NULL_ID = -1;

    private GLRenderer renderer;
    private GlVertexBuffer.Usage usage = GlVertexBuffer.Usage.Dynamic;
    private WeakReference<GlBuffer> weakRef;
    private boolean updateNeeded = true;
    private int id = NULL_ID;

    public GlBuffer() {}

    public GlBuffer(long bytes) {
        super(bytes);
    }

    public GlBuffer(long bytes, boolean clearMem) {
        super(bytes, clearMem);
    }

    @Override
    public Runnable createDestroyer() {
        Runnable sup = super.createDestroyer();
        return () -> {
            if (id != NULL_ID) {
                renderer.deleteBuffer(id);
            }
            sup.run();
        };
    }

    @Override
    public DisposableReference getReference() {
        return ref;
    }

    public int getId() {
        return id;
    }

    public void initialize(GLRenderer renderer, int id) {
        if (this.id == NULL_ID) {
            this.renderer = renderer;
            this.id = id;
            ref.refresh();
        } else {
            logger.warning("Already initialized. Ignoring initialize call.");
        }
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

    public boolean isInitialized() {
        return id >= 0;
    }

    public WeakReference<GlBuffer> getWeakReference() {
        if (weakRef == null) {
            weakRef = new WeakReference<>(this);
        }
        return weakRef;
    }

}
