package com.jme3.light;

import com.jme3.bounding.*;
import com.jme3.export.*;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.util.TempVars;

import java.io.IOException;

public class SphereProbeArea implements ProbeArea {

    private Vector3f center = new Vector3f();
    private float radius = 1;
    final private Matrix4f uniformMatrix = new Matrix4f();

    public SphereProbeArea() {
    }

    public SphereProbeArea(Vector3f center, float radius) {
        this.center.set(center);
        this.radius = radius;
        updateMatrix();
    }

    public Vector3f getCenter() {
        return center;
    }

    @Override
    public void setCenter(Vector3f center) {
        this.center.set(center);
        updateMatrix();
    }

    @Override
    public float getRadius() {
        return radius;
    }

    @Override
    public void setRadius(float radius) {
        this.radius = radius;
        updateMatrix();
    }

    @Override
    public Matrix4f getUniformMatrix() {
        return uniformMatrix;
    }

    private void updateMatrix(){
        //position
        uniformMatrix.m03 = center.x;
        uniformMatrix.m13 = center.y;
        uniformMatrix.m23 = center.z;

    }

    @Override
    public boolean intersectsBox(BoundingBox box, TempVars vars) {
        return Intersection.intersect(box, center, radius);
    }

    @Override
    public boolean intersectsSphere(BoundingSphere sphere, TempVars vars) {
        return Intersection.intersect(sphere, center, radius);
    }

    @Override
    public boolean intersectsFrustum(Camera camera, TempVars vars) {
        return Intersection.intersect(camera, center, radius);
    }

    @Override
    public String toString() {
        return "SphereProbeArea{" +
                "center=" + center +
                ", radius=" + radius +
                '}';
    }

    @Override
    protected SphereProbeArea clone() throws CloneNotSupportedException {
        return new SphereProbeArea(center, radius);
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        OutputCapsule oc = e.getCapsule(this);
        oc.write(center, "center", new Vector3f());
        oc.write(radius, "radius", 1);
    }

    @Override
    public void read(JmeImporter i) throws IOException {
        InputCapsule ic = i.getCapsule(this);
        center = (Vector3f) ic.readSavable("center", new Vector3f());
        radius = ic.readFloat("radius", 1);
        updateMatrix();
    }

}
