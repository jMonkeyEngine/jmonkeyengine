package com.jme3.renderer.camera;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;

import java.util.function.Consumer;

public class GuiCamera extends ExtensionCamera {

    private final float width, height;
    private final Matrix4f orthoMatrix = new Matrix4f();
    private final BoundingBox bound = new BoundingBox();
    private boolean orthoChanged = true;
    private boolean boundChanged = true;

    public GuiCamera(float width, float height) {
        super();
        this.width = width;
        this.height = height;
    }

    public GuiCamera(Camera base, float width, float height) {
        super(base);
        this.width = width;
        this.height = height;
    }

    @Override
    public Matrix4f getViewMatrix() {
        return Matrix4f.IDENTITY;
    }

    @Override
    public Matrix4f getProjectionMatrix() {
        updateOrtho();
        return orthoMatrix;
    }

    @Override
    public Matrix4f getViewProjectionMatrix() {
        updateOrtho();
        return orthoMatrix;
    }

    @Override
    public Camera.FrustumIntersect contains(BoundingVolume volume) {
        updateBound();
        return volume == null || bound.intersects(volume)
                ? Camera.FrustumIntersect.Intersects
                : Camera.FrustumIntersect.Outside;
    }

    @Override
    public void frustumCull(Spatial scene, Consumer<Spatial> onVisible) {
        for (Spatial.GraphIterator it = scene.iterator(); it.hasNext();) {
            Spatial child = it.next();
            switch (contains(child.getWorldBound())) {
                case Inside: for (Spatial s : child) {
                    onVisible.accept(s);
                } break;
                case Outside: it.skipChildren(); break;
                case Intersects: onVisible.accept(child); break;
            }
        }
    }

    public void updateOrtho() {
        if (!orthoChanged) return;
        orthoMatrix.loadIdentity();
        orthoMatrix.setTranslation(-1f, -1f, 0f);
        orthoMatrix.setScale(2f / width, 2f / height, 0f);
        orthoChanged = false;
    }

    public void updateBound() {
        if (!boundChanged) return;
        float xExtent = Math.max(0f, width / 2f);
        float yExtent = Math.max(0f, height / 2f);
        bound.setCenter(xExtent, yExtent, 0);
        bound.setXExtent(xExtent);
        bound.setYExtent(yExtent);
        bound.setZExtent(Float.MAX_VALUE);
        boundChanged = false;
    }

}
