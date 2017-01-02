package com.jme3.scene.plugins.fbx;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public enum RotationOrder {
	
	EULER_XYZ, EULER_XZY, EULER_YZX, EULER_YXZ, EULER_ZXY, EULER_ZYX, SPHERIC_XYZ;
	
	public static final RotationOrder[] values = values();
	
	private RotationOrder() {
	}
	
	public Quaternion rotate(Vector3f vec) {
		return fromEuler(vec.x * FastMath.DEG_TO_RAD, vec.y * FastMath.DEG_TO_RAD, vec.z * FastMath.DEG_TO_RAD, this);
	}
	
	public Quaternion rotate(float x, float y, float z) {
		return fromEuler(x * FastMath.DEG_TO_RAD, y * FastMath.DEG_TO_RAD, z * FastMath.DEG_TO_RAD, this);
	}
	
	public Matrix4f rotateToMatrix(Vector3f vec) {
		return fromEulerToMatrix(vec.x * FastMath.DEG_TO_RAD, vec.y * FastMath.DEG_TO_RAD, vec.z * FastMath.DEG_TO_RAD, this);
	}
	
	public Matrix4f rotateToMatrix(float x, float y, float z) {
		return fromEulerToMatrix(x * FastMath.DEG_TO_RAD, y * FastMath.DEG_TO_RAD, z * FastMath.DEG_TO_RAD, this);
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
			throw new IllegalArgumentException("Spheric rotation is unsupported in this importer");
		}
	}
	
	private static Matrix4f fromEulerToMatrix(float x, float y, float z, RotationOrder order) {
		Matrix4f c = new Matrix4f();
		switch(order) {
		case EULER_XYZ:
			return rotationZ(z, null).multLocal(rotationY(y, c)).multLocal(rotationX(x, c));
		case EULER_YXZ:
			return rotationZ(z, null).multLocal(rotationX(x, c)).multLocal(rotationY(y, c));
		case EULER_ZXY:
			return rotationY(y, null).multLocal(rotationX(x, c)).multLocal(rotationZ(z, c));
		case EULER_ZYX:
			return rotationX(x, null).multLocal(rotationY(y, c)).multLocal(rotationZ(z, c));
		case EULER_YZX:
			return rotationX(x, null).multLocal(rotationZ(z, c)).multLocal(rotationY(y, c));
		case EULER_XZY:
			return rotationY(y, null).multLocal(rotationZ(z, c)).multLocal(rotationX(x, c));
		case SPHERIC_XYZ:
		default:
			throw new IllegalArgumentException("Spheric rotation is unsupported in this importer");
		}
	}
	
	private static Quaternion toQuat(float ax1v, Vector3f ax1, float ax2v, Vector3f ax2, float ax3v, Vector3f ax3) {
		// TODO It has some potential for optimization
		Quaternion q1 = new Quaternion().fromAngleNormalAxis(ax1v, ax1);
		Quaternion q2 = new Quaternion().fromAngleNormalAxis(ax2v, ax2);
		Quaternion q3 = new Quaternion().fromAngleNormalAxis(ax3v, ax3);
		return q3.multLocal(q2).multLocal(q1);// FBX uses order opposite of what stated in RotationOrder's names... Because.
	}
	
	private static Matrix4f rotationX(float r, Matrix4f out) {
		if(out == null)
			out = new Matrix4f();
		else
			out.loadIdentity();
		out.m11 = out.m22 = FastMath.cos(r);
		out.m12 = -(out.m21 = FastMath.sin(r));
		return out;
	}
	
	private static Matrix4f rotationY(float r, Matrix4f out) {
		if(out == null)
			out = new Matrix4f();
		else
			out.loadIdentity();
		out.m00 = out.m22 = FastMath.cos(r);
		out.m20 = -(out.m02 = FastMath.sin(r));
		return out;
	}
	
	private static Matrix4f rotationZ(float r, Matrix4f out) {
		if(out == null)
			out = new Matrix4f();
		else
			out.loadIdentity();
		out.m00 = out.m11 = FastMath.cos(r);
		out.m01 = -(out.m10 = FastMath.sin(r));
		return out;
	}
}
