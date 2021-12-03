package com.jme3.scene.plugins.fbx;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public enum RotationOrder {

    EULER_XYZ, EULER_XZY, EULER_YZX, EULER_YXZ, EULER_ZXY, EULER_ZYX, SPHERIC_XYZ;

    // Static values field for fast access by ordinal without Enum.values() overhead
    public static final RotationOrder[] values = values();

    private RotationOrder() {
    }

    public Quaternion rotate(Vector3f vec) {
        return fromEuler(vec.x * FastMath.DEG_TO_RAD, vec.y * FastMath.DEG_TO_RAD, vec.z * FastMath.DEG_TO_RAD, this);
    }

    public Quaternion rotate(float x, float y, float z) {
        return fromEuler(x * FastMath.DEG_TO_RAD, y * FastMath.DEG_TO_RAD, z * FastMath.DEG_TO_RAD, this);
    }

    private static Quaternion fromEuler(float x, float y, float z, RotationOrder order) {
        switch(order) {
        case EULER_XYZ:
            return toQuat(x, Vector3f.UNIT_X, y, Vector3f.UNIT_Y, z, Vector3f.UNIT_Z);
        case EULER_YXZ:
            return toQuat(y, Vector3f.UNIT_Y, x, Vector3f.UNIT_X, z, Vector3f.UNIT_Z);
        case EULER_ZXY:
            return toQuat(z, Vector3f.UNIT_Z, x, Vector3f.UNIT_X, y, Vector3f.UNIT_Y);
        case EULER_ZYX:
            return toQuat(z, Vector3f.UNIT_Z, y, Vector3f.UNIT_Y, x, Vector3f.UNIT_X);
        case EULER_YZX:
            return toQuat(y, Vector3f.UNIT_Y, z, Vector3f.UNIT_Z, x, Vector3f.UNIT_X);
        case EULER_XZY:
            return toQuat(x, Vector3f.UNIT_X, z, Vector3f.UNIT_Z, y, Vector3f.UNIT_Y);
        case SPHERIC_XYZ:
        default:
            throw new IllegalArgumentException("Spherical rotation is unsupported in this importer");
        }
    }

    private static Quaternion toQuat(float ax1v, Vector3f ax1, float ax2v, Vector3f ax2, float ax3v, Vector3f ax3) {
        // TODO It has some potential in optimization
        Quaternion q1 = new Quaternion().fromAngleNormalAxis(ax1v, ax1);
        Quaternion q2 = new Quaternion().fromAngleNormalAxis(ax2v, ax2);
        Quaternion q3 = new Quaternion().fromAngleNormalAxis(ax3v, ax3);
        return q1.multLocal(q2).multLocal(q3);
    }
}
