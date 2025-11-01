package com.jme3.util.natives;

import com.jme3.renderer.opengl.GLRenderer;

import java.lang.ref.WeakReference;

public abstract class GlNative extends AbstractNative<Integer> implements Cloneable {

    protected GLRenderer renderer;
    protected boolean updateNeeded = true;
    private WeakReference<GlNative> weakRef;

    public GlNative() {
        this(-1);
    }

    public GlNative(int object) {
        this.object = object;
    }

    public abstract void resetObject();

    protected void setId(int id) {
        setId(null, id);
    }

    public void setId(GLRenderer renderer, int id) {
        object = id;
        if (ref != null) {
            ref.destroy();
        }
        if (renderer != null) {
            this.renderer = renderer;
        }
        ref = Native.get().register(this);
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

    public void dispose() {
        if (ref != null) {
            ref.destroy();
            ref = null;
        }
    }

    @SuppressWarnings("unchecked")
    public <E> WeakReference<E> getWeakRef() {
        if (weakRef == null) {
            weakRef = new WeakReference<>(this);
        }
        return (WeakReference<E>) weakRef;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GlNative clone() {
        try {
            GlNative clone = (GlNative)super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            clone.updateNeeded = true;
            clone.renderer = renderer;
            clone.object = object;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

}
