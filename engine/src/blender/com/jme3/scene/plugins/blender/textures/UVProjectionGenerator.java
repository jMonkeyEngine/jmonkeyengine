package com.jme3.scene.plugins.blender.textures;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.math.FastMath;
import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.plugins.blender.textures.UVCoordinatesGenerator.BoundingTube;
import java.nio.FloatBuffer;

/**
 * This class helps with projection calculations.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class UVProjectionGenerator {
	/**
	 * Flat projection for 2D textures.
	 * 
	 * @param mesh
	 *            mesh that is to be projected
	 * @param bb
	 *            the bounding box for projecting
	 * @return UV coordinates after the projection
	 */
	public static float[] flatProjection(Mesh mesh, BoundingBox bb) {
		if (bb == null) {
			bb = UVCoordinatesGenerator.getBoundingBox(mesh);
		}
		Vector3f min = bb.getMin(null);
		float[] ext = new float[] { bb.getXExtent() * 2.0f, bb.getYExtent() * 2.0f };
		FloatBuffer positions = mesh.getFloatBuffer(VertexBuffer.Type.Position);
		float[] uvCoordinates = new float[positions.limit() / 3 * 2];
		for (int i = 0, j = 0; i < positions.limit(); i += 3, j += 2) {
			uvCoordinates[j] = (positions.get(i) - min.x) / ext[0];
			uvCoordinates[j + 1] = (positions.get(i + 1) - min.y) / ext[1];
			// skip the Z-coordinate
		}
		return uvCoordinates;
	}

	/**
	 * Cube projection for 2D textures.
	 * 
	 * @param mesh
	 *            mesh that is to be projected
	 * @param bb
	 *            the bounding box for projecting
	 * @return UV coordinates after the projection
	 */
	public static float[] cubeProjection(Mesh mesh, BoundingBox bb) {
		Triangle triangle = new Triangle();
		Vector3f x = new Vector3f(1, 0, 0);
		Vector3f y = new Vector3f(0, 1, 0);
		Vector3f z = new Vector3f(0, 0, 1);
		Vector3f min = bb.getMin(null);
		float[] ext = new float[] { bb.getXExtent() * 2.0f, bb.getYExtent() * 2.0f, bb.getZExtent() * 2.0f };

		float[] uvCoordinates = new float[mesh.getTriangleCount() * 6];// 6 == 3 * 2
		float borderAngle = (float) Math.sqrt(2.0f) / 2.0f;
		for (int i = 0, pointIndex = 0; i < mesh.getTriangleCount(); ++i) {
			mesh.getTriangle(i, triangle);
			Vector3f n = triangle.getNormal();
			float dotNX = Math.abs(n.dot(x));
			float dorNY = Math.abs(n.dot(y));
			float dotNZ = Math.abs(n.dot(z));
			if (dotNX > borderAngle) {
				if (dotNZ < borderAngle) {// discard X-coordinate
					uvCoordinates[pointIndex++] = (triangle.get1().y - min.y) / ext[1];
					uvCoordinates[pointIndex++] = (triangle.get1().z - min.z) / ext[2];
					uvCoordinates[pointIndex++] = (triangle.get2().y - min.y) / ext[1];
					uvCoordinates[pointIndex++] = (triangle.get2().z - min.z) / ext[2];
					uvCoordinates[pointIndex++] = (triangle.get3().y - min.y) / ext[1];
					uvCoordinates[pointIndex++] = (triangle.get3().z - min.z) / ext[2];
				} else {// discard Z-coordinate
					uvCoordinates[pointIndex++] = (triangle.get1().x - min.x) / ext[0];
					uvCoordinates[pointIndex++] = (triangle.get1().y - min.y) / ext[1];
					uvCoordinates[pointIndex++] = (triangle.get2().x - min.x) / ext[0];
					uvCoordinates[pointIndex++] = (triangle.get2().y - min.y) / ext[1];
					uvCoordinates[pointIndex++] = (triangle.get3().x - min.x) / ext[0];
					uvCoordinates[pointIndex++] = (triangle.get3().y - min.y) / ext[1];
				}
			} else {
				if (dorNY > borderAngle) {// discard Y-coordinate
					uvCoordinates[pointIndex++] = (triangle.get1().x - min.x) / ext[0];
					uvCoordinates[pointIndex++] = (triangle.get1().z - min.z) / ext[2];
					uvCoordinates[pointIndex++] = (triangle.get2().x - min.x) / ext[0];
					uvCoordinates[pointIndex++] = (triangle.get2().z - min.z) / ext[2];
					uvCoordinates[pointIndex++] = (triangle.get3().x - min.x) / ext[0];
					uvCoordinates[pointIndex++] = (triangle.get3().z - min.z) / ext[2];
				} else {// discard Z-coordinate
					uvCoordinates[pointIndex++] = (triangle.get1().x - min.x) / ext[0];
					uvCoordinates[pointIndex++] = (triangle.get1().y - min.y) / ext[1];
					uvCoordinates[pointIndex++] = (triangle.get2().x - min.x) / ext[0];
					uvCoordinates[pointIndex++] = (triangle.get2().y - min.y) / ext[1];
					uvCoordinates[pointIndex++] = (triangle.get3().x - min.x) / ext[0];
					uvCoordinates[pointIndex++] = (triangle.get3().y - min.y) / ext[1];
				}
			}
			triangle.setNormal(null);// clear the previous normal vector
		}
		return uvCoordinates;
	}

	/**
	 * Tube projection for 2D textures.
	 * 
	 * @param mesh
	 *            mesh that is to be projected
	 * @param bt
	 *            the bounding tube for projecting
	 * @return UV coordinates after the projection
	 */
	public static float[] tubeProjection(Mesh mesh, BoundingTube bt) {
		FloatBuffer positions = mesh.getFloatBuffer(VertexBuffer.Type.Position);
		float[] uvCoordinates = new float[positions.limit() / 3 * 2];
		Vector3f v = new Vector3f();
		float cx = bt.getCenter().x, cy = bt.getCenter().y;
		Vector3f uBase = new Vector3f(0, -1, 0);
		
		float vBase = bt.getCenter().z - bt.getHeight() * 0.5f;
		for (int i = 0, j = 0; i < positions.limit(); i += 3, j += 2) {
			// calculating U
			v.set(positions.get(i)-cx, positions.get(i + 1)-cy, 0);
			v.normalizeLocal();
			float angle = v.angleBetween(uBase);// result between [0; PI]
			if (v.x < 0) {// the angle should be greater than PI, we're on the other part of the image then
				angle = FastMath.TWO_PI - angle;
			}
			uvCoordinates[j] = angle / FastMath.TWO_PI;

			// calculating V
			float z = positions.get(i + 2);
			uvCoordinates[j + 1] = (z - vBase) / bt.getHeight();
		}
		
		//looking for splitted triangles
		Triangle triangle = new Triangle();
		for(int i=0;i<mesh.getTriangleCount();++i) {
			mesh.getTriangle(i, triangle);
			float sgn1 = Math.signum(triangle.get1().x-cx);
			float sgn2 = Math.signum(triangle.get2().x-cx);
			float sgn3 = Math.signum(triangle.get3().x-cx);
			float xSideFactor = sgn1 + sgn2 + sgn3;
			float ySideFactor = Math.signum(triangle.get1().y-cy)+
					   Math.signum(triangle.get2().y-cy)+
					   Math.signum(triangle.get3().y-cy);
			if((xSideFactor>-3 || xSideFactor<3) && ySideFactor<0) {//the triangle is on the splitting plane
				//indexOfUcoord = (indexOfTriangle*3 + indexOfTrianglesVertex)*2
				if(sgn1==1.0f) {
					uvCoordinates[i*3*2] += 1.0f;
				}
				if(sgn2==1.0f) {
					uvCoordinates[(i*3+1)*2] += 1.0f;
				}
				if(sgn3==1.0f) {
					uvCoordinates[(i*3+2)*2] += 1.0f;
				}
			}
		}
		return uvCoordinates;
	}

	/**
	 * Sphere projection for 2D textures.
	 * 
	 * @param mesh
	 *            mesh that is to be projected
	 * @param bb
	 *            the bounding box for projecting
	 * @return UV coordinates after the projection
	 */
	public static float[] sphereProjection(Mesh mesh, BoundingSphere bs) {
		FloatBuffer positions = mesh.getFloatBuffer(VertexBuffer.Type.Position);
		float[] uvCoordinates = new float[positions.limit() / 3 * 2];
		Vector3f v = new Vector3f();
		float cx = bs.getCenter().x, cy = bs.getCenter().y, cz = bs.getCenter().z;
		Vector3f uBase = new Vector3f(0, -1, 0);
		Vector3f vBase = new Vector3f(0, 0, -1);

		for (int i = 0, j = 0; i < positions.limit(); i += 3, j += 2) {
			// calculating U
			v.set(positions.get(i)-cx, positions.get(i + 1)-cy, 0);
			v.normalizeLocal();
			float angle = v.angleBetween(uBase);// result between [0; PI]
			if (v.x < 0) {// the angle should be greater than PI, we're on the other part of the image then
				angle = FastMath.TWO_PI - angle;
			}
			uvCoordinates[j] = angle / FastMath.TWO_PI;

			// calculating V
			v.set(positions.get(i)-cx, positions.get(i + 1)-cy, positions.get(i + 2)-cz);
			v.normalizeLocal();
			angle = v.angleBetween(vBase);// result between [0; PI]
			uvCoordinates[j+1] = angle / FastMath.PI;
		}
		
		//looking for splitted triangles
		Triangle triangle = new Triangle();
		for(int i=0;i<mesh.getTriangleCount();++i) {
			mesh.getTriangle(i, triangle);
			float sgn1 = Math.signum(triangle.get1().x-cx);
			float sgn2 = Math.signum(triangle.get2().x-cx);
			float sgn3 = Math.signum(triangle.get3().x-cx);
			float xSideFactor = sgn1 + sgn2 + sgn3;
			float ySideFactor = Math.signum(triangle.get1().y-cy)+
					   Math.signum(triangle.get2().y-cy)+
					   Math.signum(triangle.get3().y-cy);
			if((xSideFactor>-3 || xSideFactor<3) && ySideFactor<0) {//the triangle is on the splitting plane
				//indexOfUcoord = (indexOfTriangle*3 + indexOfTrianglesVertex)*2
				if(sgn1==1.0f) {
					uvCoordinates[i*3*2] += 1.0f;
				}
				if(sgn2==1.0f) {
					uvCoordinates[(i*3+1)*2] += 1.0f;
				}
				if(sgn3==1.0f) {
					uvCoordinates[(i*3+2)*2] += 1.0f;
				}
			}
		}
		return uvCoordinates;
	}
}
