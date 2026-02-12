package com.jme3.renderer.camera;

import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.vulkan.util.ScenePropertyStack;
import com.jme3.vulkan.util.SceneStack;

import java.util.function.Consumer;

public class BaseCamera implements Camera {

    private final Vector3f location = new Vector3f();
    private final Quaternion rotation = new Quaternion();
    private float near = 1.0f;
    private float far = 2.0f;

    @Override
    public void setLocation(float x, float y, float z) {
        location.set(x, y, z);
    }

    @Override
    public void setRotation(Quaternion rotation) {
        this.rotation.set(rotation);
    }

    @Override
    public Vector3f getLocation() {
        return location;
    }

    @Override
    public Quaternion getRotation() {
        return rotation;
    }

    @Override
    public Matrix4f getViewMatrix() {
        return Matrix4f.IDENTITY;
    }

    @Override
    public Matrix4f getProjectionMatrix() {
        return Matrix4f.IDENTITY;
    }

    @Override
    public Matrix4f getViewProjectionMatrix() {
        return Matrix4f.IDENTITY;
    }

    @Override
    public Camera.FrustumIntersect contains(BoundingVolume volume) {
        return Camera.FrustumIntersect.Outside;
    }

    @Override
    public SceneStack<FrustumIntersect> createCullStack() {
        return new ScenePropertyStack<>(FrustumIntersect.Inside, FrustumIntersect.Inside, s -> FrustumIntersect.Inside);
    }

    @Override
    public void setNearDistance(float near) {
        this.near = near;
    }

    @Override
    public float getNearDistance() {
        return near;
    }

    @Override
    public void setFarDistance(float far) {
        this.far = far;
    }

    @Override
    public float getFarDistance() {
        return far;
    }

}
