package com.jme3.renderer;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class CameraTest {

    @Test
    public void testClone() {
        final Camera camera = new Camera(2, 2);
        assertEquals(camera.clone(), camera);
    }

    @Test
    public void testCopyFrom() {
        final Camera baseCamera = new Camera(1, 1);
        final Camera copyToCamera = new Camera(2, 2);

        baseCamera.setLocation(new Vector3f(1, 2, 3));
        baseCamera.setFrustum(1, 2, 3, 4, 5, 6);
        baseCamera.setLocation(new Vector3f(1, 5, 8));
        baseCamera.setRotation(new Quaternion(8, 9, 10, 11));
        baseCamera.setViewPort(1, 2, 3, 4);
        baseCamera.setFrustumPerspective(1.0f, 2.5f, 6.5f, 8.5f);

        copyToCamera.copyFrom(baseCamera);

        assertEquals(baseCamera, copyToCamera);
    }

    @Test
    public void testResize() {
        final Camera camera = new Camera(1, 1);

        assertEquals(1.0, camera.frustumNear, 0.0f);
        assertEquals(2.0, camera.frustumFar, 0.0f);
        assertEquals(-0.5, camera.frustumLeft, 0.0f);
        assertEquals(0.5, camera.frustumRight, 0.0f);
        assertEquals(0.5, camera.frustumTop, 0.0f);
        assertEquals(-0.5, camera.frustumBottom, 0.0f);

        assertEquals(camera.getWidth(), 1);
        assertEquals(camera.getHeight(), 1);

        camera.resize(2, 4, false);

        assertEquals(1.0, camera.frustumNear, 0.0f);
        assertEquals(2.0, camera.frustumFar, 0.0f);
        assertEquals(-0.5, camera.frustumLeft, 0.0f);
        assertEquals(0.5, camera.frustumRight, 0.0f);
        assertEquals(0.5, camera.frustumTop, 0.0f);
        assertEquals(-0.5, camera.frustumBottom, 0.0f);

        assertEquals(2, camera.getWidth());
        assertEquals(4, camera.getHeight());

        camera.resize(2, 4, true);

        assertEquals(1.0, camera.frustumNear, 0.0f);
        assertEquals(2.0, camera.frustumFar, 0.0f);
        assertEquals(-0.25, camera.frustumLeft, 0.0f);
        assertEquals(0.25, camera.frustumRight, 0.0f);
        assertEquals(0.5, camera.frustumTop, 0.0f);
        assertEquals(-0.5, camera.frustumBottom, 0.0f);

        assertEquals(2, camera.getWidth());
        assertEquals(4, camera.getHeight());
    }

    @Test
    public void testLookAt() {
        final Vector3f pos = new Vector3f(1, 2, 3);
        final Vector3f worldUpVector = new Vector3f(30, 40, 50);
        final Camera camera = new Camera(1, 1);

        camera.lookAt(pos, worldUpVector);

        assertEquals(new Quaternion(-0.31154725f, -0.04523793f, -0.5420604f, 0.77914214f), camera.getRotation());
    }

    @Test
    public void testLookAt2() {
        final Vector3f pos = new Vector3f(1, 2, 3);
        final Vector3f worldUpVector = new Vector3f(0, 0, 0);
        final Camera camera = new Camera(1, 1);

        camera.lookAt(pos, worldUpVector);

        assertEquals(new Quaternion(-0.2746567f, 0.15385644f, 0.04457065f, 0.94810617f), camera.getRotation());
    }

    @Test
    public void testLookAt3() {
        final Vector3f pos = new Vector3f(0, 0, 0);
        final Vector3f worldUpVector = new Vector3f(1, 0, 1);
        final Camera camera = new Camera(1, 1);

        camera.lookAt(pos, worldUpVector);

        assertEquals(new Quaternion(0.0f, 0.0f, 0.0f, 1.0f), camera.getRotation());
    }

    @Test
    public void testGetViewToProjectionZ() {
        assertEquals(1.4285715, new Camera(3, 3).getViewToProjectionZ(3.5f), 0.01f);
        assertEquals(1.5833333, new Camera(0, 0).getViewToProjectionZ(4.8f), 0.01f);
        assertEquals(Float.NEGATIVE_INFINITY, new Camera(0, 0).getViewToProjectionZ(0.0f), 0.0f);
        assertEquals(0.0f, new Camera(0, 0).getViewToProjectionZ(1.0f), 0.0f);
        assertEquals(4.0f, new Camera(0, 0).getViewToProjectionZ(-1.0f), 0.0f);
    }

    @Test
    public void testGetWorldCoordinates() {
        final Camera camera = new Camera(1, 1);

        final Vector3f coords1 = camera.getWorldCoordinates(new Vector2f(1.0f, 2.0f), 1.0f, new Vector3f());
        assertEquals(new Vector3f(-0.5f, 1.5f, 2.0f), coords1);

        final Vector3f coords2 = camera.getWorldCoordinates(new Vector2f(5.0f, 3.5f), 1.0f, null);
        assertEquals(new Vector3f(-4.5f, 3.0f, 2.0f), coords2);

        final Vector3f coords3 = camera.getWorldCoordinates(new Vector2f(5.0f, 3.5f), 5.2f);
        assertEquals(new Vector3f(-4.5f, 3.0f, 6.2f), coords3);
    }

    @Test
    public void testGetScreenCoordinates() {
        final Camera camera = new Camera(1, 1);

        final Vector3f coords1 = camera.getScreenCoordinates(new Vector3f(1.0f, 2.0f, 2.0f), new Vector3f());
        assertEquals(new Vector3f(-0.5f, 2.5f, 1.0f), coords1);

        final Vector3f coords2 = camera.getScreenCoordinates(new Vector3f(5.0f, 3.5f, 4.5f),null);
        assertEquals(new Vector3f(-4.5f, 4.0f, 3.5f), coords2);

        final Vector3f coords3 = camera.getScreenCoordinates(new Vector3f(5.0f, 3.5f, 4.5f));
        assertEquals(new Vector3f(-4.5f, 4.0f, 3.5f), coords3);
    }

    @Test
    public void testSetProjectionMatrix() {
        final Camera camera = new Camera(1, 1);

        final Matrix4f baseMatrix = new Matrix4f(
                2.0f,  0.0f,  0.0f,  -0.0f,
                0.0f,  2.0f,  0.0f,  -0.0f,
                0.0f,  0.0f,  -2.0f,  -3.0f,
                0.0f,  0.0f,  0.0f,  1.0f);

        final Matrix4f targetMatrix = new Matrix4f(
                1.0f, 3.2f, 4.6f, 1.2f,
                9.4f, 3.6f, 2.1f, 7.4f,
                9.5f, 8.2f, 5.2f, 7.4f,
                1.3f, 1.6f, 2.4f, 7.7f);

        assertEquals(baseMatrix, camera.getProjectionMatrix());

        camera.setProjectionMatrix(targetMatrix);
        assertEquals(targetMatrix, camera.getProjectionMatrix());

        camera.setProjectionMatrix(null);
        assertEquals(baseMatrix, camera.getProjectionMatrix());
    }

    @Test
    public void testContains() {
        final Camera camera = new Camera(1, 5);

        assertEquals(Camera.FrustumIntersect.Inside, camera.contains(null));

        final BoundingVolume boundsInt = new BoundingBox(new Vector3f(1.0f, 2.0f, 3.0f), 4.0f, 9.0f, 4.5f);
        assertEquals(Camera.FrustumIntersect.Intersects, camera.contains(boundsInt));

        final BoundingVolume boundsOut = new BoundingSphere(-50.0f, new Vector3f(20.0f, 10.0f, 3.0f));
        assertEquals(Camera.FrustumIntersect.Outside, camera.contains(boundsOut));
    }

    @Test
    public void testContainsGui() {
        final Camera camera = new Camera(1, 5);

        assertTrue(camera.containsGui(null));

        final BoundingVolume bounds1 = new BoundingBox(new Vector3f(1.0f, 2.0f, 3.0f), 4.0f, 9.0f, 4.5f);
        assertTrue(camera.containsGui(bounds1));

        final BoundingVolume bounds2 = new BoundingSphere(-50.0f, new Vector3f(20.0f, 10.0f, 3.0f));
        assertTrue(camera.containsGui(bounds2));

        final BoundingVolume bounds3 = new BoundingBox(new Vector3f(100.0f, 200.0f, 300.0f), 400.0f, 900.0f, 400.0f);
        final Camera camera2 = new Camera(500, 500);
        camera2.setViewPort(50f, 50f, 50f, 50f);
        assertFalse(camera2.containsGui(bounds3));
    }

    @Test
    public void testSetClipPlane() {
        final Camera camera = new Camera(1, 2);

        assertEquals(new Matrix4f(
                2.0f, 0.0f,  0.0f,  -0.0f,
                0.0f, 2.0f,  0.0f,  -0.0f,
                0.0f, 0.0f,  -2.0f,  -3.0f,
                0.0f,  0.0f,  0.0f,  1.0f), camera.getProjectionMatrix());

        camera.setClipPlane(new Plane(new Vector3f(1.0f, 2.0f, 4.0f), 5.0f), Plane.Side.None);
        assertEquals(new Matrix4f(
                2.0f, 0.0f,  0.0f,  -0.0f,
                0.0f, 2.0f,  0.0f,  -0.0f,
                0.06779661f, -0.13559322f,  0.27118644f,  6.118644f,
                0.0f,  0.0f,  0.0f,  1.0f), camera.getProjectionMatrix());

        camera.setClipPlane(new Plane(new Vector3f(5.0f, 8.0f, 3.0f), 3.0f), Plane.Side.Negative);
        assertEquals(new Matrix4f(
                2.0f, 0.0f,  0.0f,  -0.0f,
                0.0f, 2.0f,  0.0f,  -0.0f,
                0.06779661f, -0.13559322f,  0.27118644f,  6.118644f,
                0.0f,  0.0f,  0.0f,  1.0f), camera.getProjectionMatrix());

        camera.setClipPlane(new Plane(new Vector3f(3.0f, 1.0f, 6.0f), 2.0f), Plane.Side.Positive);
        assertEquals(new Matrix4f(
                2.0f, 0.0f,  0.0f,  -0.0f,
                0.0f, 2.0f,  0.0f,  -0.0f,
                0.26470587f, -0.08823529f,  0.52941173f,  7.117646f,
                0.0f,  0.0f,  0.0f,  1.0f), camera.getProjectionMatrix());

        final Plane plane = new Plane(new Vector3f(1.0f, 2.0f, 3.0f), new Vector3f(5.0f, 3.0f, 7.0f));
        camera.setClipPlane(plane);
        assertEquals(new Matrix4f(
                2.0f, 0.0f,  0.0f,  -0.0f,
                0.0f, 2.0f,  0.0f,  -0.0f,
                0.26470587f, -0.08823529f,  0.52941173f,  7.117646f,
                0.0f,  0.0f,  0.0f,  1.0f), camera.getProjectionMatrix());
    }

    @Test
    public void testNormalize() {
        final Camera camera = new Camera(1, 2);

        assertEquals(new Quaternion(0.0f, 0.0f, 0.0f, 1.0f), camera.rotation);

        camera.normalize();

        assertEquals(new Quaternion(0.0f, 0.0f, 0.0f, 1.0f), camera.rotation);

        final Quaternion rotation = new Quaternion(5.0f, 1.3f, 7.9f, 9.5f);

        camera.setRotation(rotation);
        assertEquals(rotation, camera.rotation);

        camera.normalize();
        assertEquals(new Quaternion(0.3733527f, 0.0970717f, 0.5898973f, 0.70937014f), camera.rotation);
    }

    @Test
    public void testLookAtDirection() {
        final Camera camera = new Camera(1, 2);

        assertEquals(new Quaternion(0.0f, 0.0f, 0.0f, 1.0f), camera.rotation);

        final Vector3f direction = new Vector3f(1.5f, 3.5f, 8.3f);
        final Vector3f up = new Vector3f(6.5f, 7.2f, 9.1f);

        camera.lookAtDirection(direction, up);
        assertEquals(new Quaternion(-0.21235512f, -0.021214653f, -0.47220254f, 0.85526603f), camera.rotation);
    }
}
