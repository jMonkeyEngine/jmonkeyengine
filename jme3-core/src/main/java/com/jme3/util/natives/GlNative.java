package com.jme3.util.natives;

import com.jme3.renderer.opengl.GLRenderer;

import java.lang.ref.WeakReference;

public abstract class GlNative <T> extends AbstractNative<T> implements Cloneable {

    protected GLRenderer renderer;
    protected boolean updateNeeded = true;
    private WeakReference<GlNative<T>> weakRef;

    public GlNative() {}

    public GlNative(T object) {
        this.object = object;
    }

    public abstract void resetObject();

    protected void setId(T id) {
        setId(null, id);
    }

    public void setId(GLRenderer renderer, T id) {
        object = id;
        if (ref != null) {
            ref.destroy();
        }
        this.renderer = renderer;
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
    public GlNative<T> clone() {
        try {
            GlNative<T> clone = (GlNative<T>) super.clone();
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
