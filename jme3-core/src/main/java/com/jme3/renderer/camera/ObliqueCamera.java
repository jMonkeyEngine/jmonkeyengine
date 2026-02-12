package com.jme3.renderer.camera;

import com.jme3.math.Matrix4f;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;

import java.util.function.Consumer;

/**
 * Camera extension which modifies the projection matrix to skew the near plane.
 * This technique is known as oblique near-plane clipping method introduced by
 * Eric Lengyel.
 */
public class ObliqueCamera extends ExtensionCamera {

    private Plane clipPlane;
    private Plane.Side side;
    private final Matrix4f formerProjMatrix = new Matrix4f();
    private final Matrix4f projOverride = new Matrix4f();

    public ObliqueCamera() {
        super();
    }

    public ObliqueCamera(Camera base) {
        super(base);
    }

    @Override
    public Matrix4f getProjectionMatrix() {
        updateProjection();
        return projOverride;
    }

    public void updateProjection() {
        Matrix4f currentProj = super.getProjectionMatrix();
        if (currentProj.equals(formerProjMatrix)) {
            return;
        }
        formerProjMatrix.set(currentProj);

        float sideFactor = 1;
        if (side == Plane.Side.Negative) {
            sideFactor = -1;
        }
        if (clipPlane.whichSide(getLocation()) == side) {
            return;
        }

        TempVars vars = TempVars.get();
        try {
            Matrix4f p = projOverride.set(formerProjMatrix);
            Matrix4f ivm = getViewMatrix();

            Vector3f point = clipPlane.getNormal().mult(clipPlane.getConstant(), vars.vect1);
            Vector3f pp = ivm.mult(point, vars.vect2);
            Vector3f pn = ivm.multNormal(clipPlane.getNormal(), vars.vect3);
            Vector4f clipPlaneV = vars.vect4f1.set(pn.x * sideFactor, pn.y * sideFactor, pn.z * sideFactor,
                    -(pp.dot(pn)) * sideFactor);

            Vector4f v = vars.vect4f2.set(0, 0, 0, 0);
            v.x = (Math.signum(clipPlaneV.x) + p.m02) / p.m00;
            v.y = (Math.signum(clipPlaneV.y) + p.m12) / p.m11;
            v.z = -1.0f;
            v.w = (1.0f + p.m22) / p.m23;

            float dot = clipPlaneV.dot(v);
            //clipPlaneV.x * v.x + clipPlaneV.y * v.y + clipPlaneV.z * v.z + clipPlaneV.w * v.w;
            Vector4f c = clipPlaneV.multLocal(2.0f / dot);

            p.m20 = c.x - p.m30;
            p.m21 = c.y - p.m31;
            p.m22 = c.z - p.m32;
            p.m23 = c.w - p.m33;
        } finally {
            vars.release();
        }
    }

}
