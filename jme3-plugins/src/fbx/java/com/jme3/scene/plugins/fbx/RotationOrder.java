package com.jme3.scene.plugins.fbx;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

// TODO This class has some potential in it... Should investigate
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
			throw new IllegalArgumentException("Spheric rotation is unsupported in this importer");
		}
		/*float c1 = FastMath.cos( x / 2 );
		float c2 = FastMath.cos( y / 2 );
		float c3 = FastMath.cos( z / 2 );
		float s1 = FastMath.sin( x / 2 );
		float s2 = FastMath.sin( y / 2 );
		float s3 = FastMath.sin( z / 2 );
		
		float _x;
		float _y;
		float _z;
		float _w;
		
		switch(order) {
		case EULER_XYZ:
			_x = s1 * c2 * c3 + c1 * s2 * s3;
			_y = c1 * s2 * c3 - s1 * c2 * s3;
			_z = c1 * c2 * s3 + s1 * s2 * c3;
			_w = c1 * c2 * c3 - s1 * s2 * s3;
			return new Quaternion(_x, _y, _z, _w);
		case EULER_YXZ:
			_x = s1 * c2 * c3 + c1 * s2 * s3;
			_y = c1 * s2 * c3 - s1 * c2 * s3;
			_z = c1 * c2 * s3 - s1 * s2 * c3;
			_w = c1 * c2 * c3 + s1 * s2 * s3;
			return new Quaternion(_x, _y, _z, _w);
		case EULER_ZXY:
			_x = s1 * c2 * c3 - c1 * s2 * s3;
			_y = c1 * s2 * c3 + s1 * c2 * s3;
			_z = c1 * c2 * s3 + s1 * s2 * c3;
			_w = c1 * c2 * c3 - s1 * s2 * s3;
			return new Quaternion(_x, _y, _z, _w);
		case EULER_ZYX:
			_x = s1 * c2 * c3 - c1 * s2 * s3;
			_y = c1 * s2 * c3 + s1 * c2 * s3;
			_z = c1 * c2 * s3 - s1 * s2 * c3;
			_w = c1 * c2 * c3 + s1 * s2 * s3;
			return new Quaternion(_x, _y, _z, _w);
		case EULER_YZX:
			_x = s1 * c2 * c3 + c1 * s2 * s3;
			_y = c1 * s2 * c3 + s1 * c2 * s3;
			_z = c1 * c2 * s3 - s1 * s2 * c3;
			_w = c1 * c2 * c3 - s1 * s2 * s3;
			return new Quaternion(_x, _y, _z, _w);
		case EULER_XZY:
			_x = s1 * c2 * c3 - c1 * s2 * s3;
			_y = c1 * s2 * c3 - s1 * c2 * s3;
			_z = c1 * c2 * s3 + s1 * s2 * c3;
			_w = c1 * c2 * c3 + s1 * s2 * s3;
			return new Quaternion(_x, _y, _z, _w);
		}
		*/
		throw new AssertionError("Impossible");
	}
	
	private static Quaternion toQuat(float ax1v, Vector3f ax1, float ax2v, Vector3f ax2, float ax3v, Vector3f ax3) {
		Quaternion q1 = new Quaternion().fromAngleNormalAxis(ax1v, ax1);
		Quaternion q2 = new Quaternion().fromAngleNormalAxis(ax2v, ax2);
		Quaternion q3 = new Quaternion().fromAngleNormalAxis(ax3v, ax3);
		return q1.multLocal(q2).multLocal(q3);
	}
}
