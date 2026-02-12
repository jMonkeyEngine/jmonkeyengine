package com.jme3.renderer.camera;

import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.vulkan.util.SceneStack;

import java.util.function.Consumer;

public abstract class ExtensionCamera implements Camera {

    protected Camera base;

    public ExtensionCamera() {
        this(new BaseCamera());
    }

    public ExtensionCamera(Camera base) {
        this.base = base;
    }

    @Override
    public void setLocation(float x, float y, float z) {
        base.setLocation(x, y, z);
    }

    @Override
    public Vector3f getLocation() {
        return base.getLocation();
    }

    @Override
    public void setRotation(Quaternion rotation) {
        base.setRotation(rotation);
    }

    @Override
    public Quaternion getRotation() {
        return base.getRotation();
    }

    @Override
    public Matrix4f getViewMatrix() {
        return base.getViewMatrix();
    }

    @Override
    public Matrix4f getProjectionMatrix() {
        return base.getProjectionMatrix();
    }

    @Override
    public Matrix4f getViewProjectionMatrix() {
        return base.getViewProjectionMatrix();
    }

    @Override
    public Camera.FrustumIntersect contains(BoundingVolume volume) {
        return base.contains(volume);
    }

    @Override
    public SceneStack<FrustumIntersect> createCullStack() {
        return base.createCullStack();
    }

    @Override
    public void setNearDistance(float near) {
        base.setNearDistance(near);
    }

    @Override
    public float getNearDistance() {
        return base.getNearDistance();
    }

    @Override
    public void setFarDistance(float far) {
        base.setFarDistance(far);
    }

    @Override
    public float getFarDistance() {
        return base.getFarDistance();
    }

}
