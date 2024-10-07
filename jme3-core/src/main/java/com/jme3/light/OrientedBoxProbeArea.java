package com.jme3.light;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.export.*;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.util.TempVars;

import java.io.IOException;

public class OrientedBoxProbeArea implements ProbeArea {
    private Transform transform = new Transform();

    /**
     * @see LightProbe#getUniformMatrix()
     * for this Area type, the matrix is updated when the probe is transformed,
     * and its data is used for bound checks in the light culling process.
     */
    private final Matrix4f uniformMatrix = new Matrix4f();

    public OrientedBoxProbeArea() {
    }

    public OrientedBoxProbeArea(Transform transform) {
        this.transform.set(transform);
        updateMatrix();
    }

    @Override
    public boolean intersectsBox(BoundingBox box, TempVars vars) {

        Vector3f axis1 = getScaledAxis(0, vars.vect1);
        Vector3f axis2 = getScaledAxis(1, vars.vect2);
        Vector3f axis3 = getScaledAxis(2, vars.vect3);

        Vector3f tn = vars.vect4;
        Plane p = vars.plane;
        Vector3f c = box.getCenter();

        p.setNormal(0, 0, -1);
        p.setConstant(-(c.z + box.getZExtent()));
        if (!insidePlane(p, axis1, axis2, axis3, tn)) return false;

        p.setNormal(0, 0, 1);
        p.setConstant(c.z - box.getZExtent());
        if (!insidePlane(p, axis1, axis2, axis3, tn)) return false;


        p.setNormal(0, -1, 0);
        p.setConstant(-(c.y + box.getYExtent()));
        if (!insidePlane(p, axis1, axis2, axis3, tn)) return false;

        p.setNormal(0, 1, 0);
        p.setConstant(c.y - box.getYExtent());
        if (!insidePlane(p, axis1, axis2, axis3, tn)) return false;

        p.setNormal(-1, 0, 0);
        p.setConstant(-(c.x + box.getXExtent()));
        if (!insidePlane(p, axis1, axis2, axis3, tn)) return false;

        p.setNormal(1, 0, 0);
        p.setConstant(c.x - box.getXExtent());
        return insidePlane(p, axis1, axis2, axis3, tn);

    }

    @Override
    public float getRadius() {
        return Math.max(Math.max(transform.getScale().x, transform.getScale().y), transform.getScale().z);
    }

    @Override
    public void setRadius(float radius) {
        transform.setScale(radius, radius, radius);
    }

    @Override
    public boolean intersectsSphere(BoundingSphere sphere, TempVars vars) {
        Vector3f closestPoint = getClosestPoint(vars, sphere.getCenter());
        // check if the point intersects with the sphere bound
        return sphere.intersects(closestPoint);
    }

    @Override
    public boolean intersectsFrustum(Camera camera, TempVars vars) {
        // extract the scaled axis
        // this allows a small optimization.
        Vector3f axis1 = getScaledAxis(0, vars.vect1);
        Vector3f axis2 = getScaledAxis(1, vars.vect2);
        Vector3f axis3 = getScaledAxis(2, vars.vect3);

        Vector3f tn = vars.vect4;

        for (int i = 5; i >= 0; i--) {
            Plane p = camera.getWorldPlane(i);
            if (!insidePlane(p, axis1, axis2, axis3, tn)) return false;
        }
        return true;
    }

    private Vector3f getScaledAxis(int index, Vector3f store) {
        Matrix4f u = uniformMatrix;
        float x, y, z, s;
        switch (index) {
            case 0:
                x = u.m00;
                y = u.m10;
                z = u.m20;
                s = u.m30;
                break;
            case 1:
                x = u.m01;
                y = u.m11;
                z = u.m21;
                s = u.m31;
                break;
            case 2:
                x = u.m02;
                y = u.m12;
                z = u.m22;
                s = u.m32;
                break;
            default:
                throw new IllegalArgumentException("Invalid axis, not in range [0, 2]");
        }
        return store.set(x, y, z).multLocal(s);
    }

    private boolean insidePlane(Plane p, Vector3f axis1, Vector3f axis2, Vector3f axis3, Vector3f tn) {
        // transform the plane normal in the box local space.
        tn.set(axis1.dot(p.getNormal()), axis2.dot(p.getNormal()), axis3.dot(p.getNormal()));

        // distance check
        float radius = FastMath.abs(tn.x) +
                FastMath.abs(tn.y) +
                FastMath.abs(tn.z);

        float distance = p.pseudoDistance(transform.getTranslation());
        return distance >= -radius;
    }

    private Vector3f getClosestPoint(TempVars vars, Vector3f point) {
        // non normalized direction
        Vector3f dir = vars.vect2.set(point).subtractLocal(transform.getTranslation());
        // initialize the closest point with box center
        Vector3f closestPoint = vars.vect3.set(transform.getTranslation());

        //store extent in an array
        float[] r = vars.fWdU;
        r[0] = transform.getScale().x;
        r[1] = transform.getScale().y;
        r[2] = transform.getScale().z;

        // Compute the closest point to sphere's center.
        for (int i = 0; i < 3; i++) {
            // extract the axis from the 3x3 matrix
            Vector3f axis = getScaledAxis(i, vars.vect1);
            // normalize (here we just divide by the extent)
            axis.divideLocal(r[i]);
            // distance to the closest point on this axis.
            float d = FastMath.clamp(dir.dot(axis), -r[i], r[i]);
            closestPoint.addLocal(vars.vect4.set(axis).multLocal(d));
        }
        return closestPoint;
    }

    private void updateMatrix() {
        TempVars vars = TempVars.get();
        Matrix3f r = vars.tempMat3;
        Matrix4f u = uniformMatrix;
        transform.getRotation().toRotationMatrix(r);

        u.m00 = r.get(0,0);
        u.m10 = r.get(1,0);
        u.m20 = r.get(2,0);
        u.m01 = r.get(0,1);
        u.m11 = r.get(1,1);
        u.m21 = r.get(2,1);
        u.m02 = r.get(0,2);
        u.m12 = r.get(1,2);
        u.m22 = r.get(2,2);

        //scale
        u.m30 = transform.getScale().x;
        u.m31 = transform.getScale().y;
        u.m32 = transform.getScale().z;

        //position
        u.m03 = transform.getTranslation().x;
        u.m13 = transform.getTranslation().y;
        u.m23 = transform.getTranslation().z;

        vars.release();
    }

    @Override
    public Matrix4f getUniformMatrix() {
        return uniformMatrix;
    }

    public Vector3f getExtent() {
        return transform.getScale();
    }

    public void setExtent(Vector3f extent) {
        transform.setScale(extent);
        updateMatrix();
    }

    public Vector3f getCenter() {
        return transform.getTranslation();
    }

    @Override
    public void setCenter(Vector3f center) {
        transform.setTranslation(center);
        updateMatrix();
    }

    public Quaternion getRotation() {
        return transform.getRotation();
    }

    public void setRotation(Quaternion rotation) {
        transform.setRotation(rotation);
        updateMatrix();
    }

    @Override
    protected OrientedBoxProbeArea clone() throws CloneNotSupportedException {
        return new OrientedBoxProbeArea(transform);
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        OutputCapsule oc = e.getCapsule(this);
        oc.write(transform, "transform", new Transform());
    }

    @Override
    public void read(JmeImporter i) throws IOException {
        InputCapsule ic = i.getCapsule(this);
        transform = (Transform) ic.readSavable("transform", new Transform());
        updateMatrix();
    }
}
