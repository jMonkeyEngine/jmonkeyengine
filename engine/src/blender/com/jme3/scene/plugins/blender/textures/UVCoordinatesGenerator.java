/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.scene.plugins.blender.textures;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class is used for UV coordinates generation.
 * @author Marcin Roguski (Kaelthas)
 */
public class UVCoordinatesGenerator {
	private static final Logger	LOGGER						= Logger.getLogger(UVCoordinatesGenerator.class.getName());

	// texture UV coordinates types
	public static final int		TEXCO_ORCO					= 1;
	public static final int		TEXCO_REFL					= 2;
	public static final int		TEXCO_NORM					= 4;
	public static final int		TEXCO_GLOB					= 8;
	public static final int		TEXCO_UV					= 16;
	public static final int		TEXCO_OBJECT				= 32;
	public static final int		TEXCO_LAVECTOR				= 64;
	public static final int		TEXCO_VIEW					= 128;
	public static final int		TEXCO_STICKY				= 256;
	public static final int		TEXCO_OSA					= 512;
	public static final int		TEXCO_WINDOW				= 1024;
	public static final int		NEED_UV						= 2048;
	public static final int		TEXCO_TANGENT				= 4096;
	// still stored in vertex->accum, 1 D
	public static final int		TEXCO_PARTICLE_OR_STRAND	= 8192;													// strand is used
	public static final int		TEXCO_STRESS				= 16384;
	public static final int		TEXCO_SPEED					= 32768;

	// 2D texture mapping (projection)
	public static final int		PROJECTION_FLAT				= 0;
	public static final int		PROJECTION_CUBE				= 1;
	public static final int		PROJECTION_TUBE				= 2;
	public static final int		PROJECTION_SPHERE			= 3;

	/**
	 * This method generates UV coordinates for the given mesh.
	 * IMPORTANT! This method assumes that all geometries represent one node.
	 * Each containing mesh with separate material.
	 * So all meshes have the same reference to vertex table which stores all their vertices.
	 * @param texco
	 *        texture coordinates type
	 * @param projection
	 *        the projection type for 2D textures
	 * @param textureDimension
	 *        the dimension of the texture (only 2D and 3D)
	 * @param coordinatesSwappingIndexes
	 *        an array that tells how UV-coordinates need to be swapped
	 * @param geometries
	 *        a list of geometries the UV coordinates will be applied to
	 * @return created UV-coordinates buffer
	 */
	public static VertexBuffer generateUVCoordinates(int texco, int projection, int textureDimension, int[] coordinatesSwappingIndexes, List<Geometry> geometries) {
		if (textureDimension != 2 && textureDimension != 3) {
			throw new IllegalStateException("Unsupported texture dimension: " + textureDimension);
		}

		VertexBuffer result = new VertexBuffer(VertexBuffer.Type.TexCoord);
		Mesh mesh = geometries.get(0).getMesh();
		BoundingBox bb = UVCoordinatesGenerator.getBoundingBox(geometries);
		float[] inputData = null;// positions, normals, reflection vectors, etc.

		switch (texco) {
			case TEXCO_ORCO:
				inputData = BufferUtils.getFloatArray(mesh.getFloatBuffer(VertexBuffer.Type.Position));
				break;
			case TEXCO_UV:
				FloatBuffer uvCoordinatesBuffer = BufferUtils.createFloatBuffer(mesh.getVertexCount() * textureDimension);
				Vector2f[] data = new Vector2f[] { new Vector2f(0, 1), new Vector2f(0, 0), new Vector2f(1, 0) };
				for (int i = 0; i < mesh.getVertexCount(); ++i) {
					Vector2f uv = data[i % 3];
					uvCoordinatesBuffer.put(uv.x);
					uvCoordinatesBuffer.put(uv.y);
					if(textureDimension == 3) {
						uvCoordinatesBuffer.put(0);
					}
				}
				result.setupData(Usage.Static, textureDimension, Format.Float, uvCoordinatesBuffer);
				break;
			case TEXCO_NORM:
				inputData = BufferUtils.getFloatArray(mesh.getFloatBuffer(VertexBuffer.Type.Normal));
				break;
			case TEXCO_REFL:
			case TEXCO_GLOB:
			case TEXCO_TANGENT:
			case TEXCO_STRESS:
			case TEXCO_LAVECTOR:
			case TEXCO_OBJECT:
			case TEXCO_OSA:
			case TEXCO_PARTICLE_OR_STRAND:
			case TEXCO_SPEED:
			case TEXCO_STICKY:
			case TEXCO_VIEW:
			case TEXCO_WINDOW:
				LOGGER.warning("Texture coordinates type not currently supported: " + texco);
				break;
			default:
				throw new IllegalStateException("Unknown texture coordinates value: " + texco);
		}

		if (inputData != null) {// make calculations
			if (textureDimension == 2) {
				switch (projection) {
					case PROJECTION_FLAT:
						inputData = UVProjectionGenerator.flatProjection(mesh, bb);
						break;
					case PROJECTION_CUBE:
						inputData = UVProjectionGenerator.cubeProjection(mesh, bb);
						break;
					case PROJECTION_TUBE:
						BoundingTube bt = UVCoordinatesGenerator.getBoundingTube(geometries);
						inputData = UVProjectionGenerator.tubeProjection(mesh, bt);
						break;
					case PROJECTION_SPHERE:
						BoundingSphere bs = UVCoordinatesGenerator.getBoundingSphere(geometries);
						inputData = UVProjectionGenerator.sphereProjection(mesh, bs);
						break;
					default:
						throw new IllegalStateException("Unknown projection type: " + projection);
				}
			} else {
				Vector3f min = bb.getMin(null);
				float[] uvCoordsResults = new float[4];//used for coordinates swapping
				float[] ext = new float[] { bb.getXExtent() * 2, bb.getYExtent() * 2, bb.getZExtent() * 2 };

				// now transform the coordinates so that they are in the range of <0; 1>
				for (int i = 0; i < inputData.length; i += 3) {
					uvCoordsResults[1] = (inputData[i] - min.x) / ext[0];
					uvCoordsResults[2] = (inputData[i + 1] - min.y) / ext[1];
					uvCoordsResults[3] = (inputData[i + 2] - min.z) / ext[2];
					
					
					inputData[i] = uvCoordsResults[coordinatesSwappingIndexes[0]];
					inputData[i + 1] = uvCoordsResults[coordinatesSwappingIndexes[1]];
					inputData[i + 2] = uvCoordsResults[coordinatesSwappingIndexes[2]];
				}
			}
			result.setupData(Usage.Static, textureDimension, Format.Float, BufferUtils.createFloatBuffer(inputData));
		}

		// each mesh will have the same coordinates
		for (Geometry geometry : geometries) {
			mesh = geometry.getMesh();
			mesh.clearBuffer(VertexBuffer.Type.TexCoord);// in case there are coordinates already set
			mesh.setBuffer(result);
		}
		
		return result;
	}

	/**
	 * This method returns the bounding box of the given geometries.
	 * @param geometries
	 *        the list of geometries
	 * @return bounding box of the given geometries
	 */
	/* package */static BoundingBox getBoundingBox(List<Geometry> geometries) {
		BoundingBox result = null;
		for (Geometry geometry : geometries) {
			BoundingBox bb = UVCoordinatesGenerator.getBoundingBox(geometry.getMesh());
			if (result == null) {
				result = bb;
			} else {
				result.merge(bb);
			}
		}
		return result;
	}

	/**
	 * This method returns the bounding box of the given mesh.
	 * @param mesh
	 *        the mesh
	 * @return bounding box of the given mesh
	 */
	/* package */static BoundingBox getBoundingBox(Mesh mesh) {
		mesh.updateBound();
		BoundingVolume bv = mesh.getBound();
		if (bv instanceof BoundingBox) {
			return (BoundingBox) bv;
		} else if (bv instanceof BoundingSphere) {
			BoundingSphere bs = (BoundingSphere) bv;
			float r = bs.getRadius();
			return new BoundingBox(bs.getCenter(), r, r, r);
		} else {
			throw new IllegalStateException("Unknown bounding volume type: " + bv.getClass().getName());
		}
	}

	/**
	 * This method returns the bounding sphere of the given geometries.
	 * @param geometries
	 *        the list of geometries
	 * @return bounding sphere of the given geometries
	 */
	/* package */static BoundingSphere getBoundingSphere(List<Geometry> geometries) {
		BoundingSphere result = null;
		for (Geometry geometry : geometries) {
			BoundingSphere bs = UVCoordinatesGenerator.getBoundingSphere(geometry.getMesh());
			if (result == null) {
				result = bs;
			} else {
				result.merge(bs);
			}
		}
		return result;
	}

	/**
	 * This method returns the bounding sphere of the given mesh.
	 * @param mesh
	 *        the mesh
	 * @return bounding sphere of the given mesh
	 */
	/* package */static BoundingSphere getBoundingSphere(Mesh mesh) {
		mesh.updateBound();
		BoundingVolume bv = mesh.getBound();
		if (bv instanceof BoundingBox) {
			BoundingBox bb = (BoundingBox) bv;
			float r = Math.max(bb.getXExtent(), bb.getYExtent());
			r = Math.max(r, bb.getZExtent());
			return new BoundingSphere(r, bb.getCenter());
		} else if (bv instanceof BoundingSphere) {
			return (BoundingSphere) bv;
		} else {
			throw new IllegalStateException("Unknown bounding volume type: " + bv.getClass().getName());
		}
	}

	/**
	 * This method returns the bounding tube of the given mesh.
	 * @param mesh
	 *        the mesh
	 * @return bounding tube of the given mesh
	 */
	/* package */static BoundingTube getBoundingTube(Mesh mesh) {
		Vector3f center = new Vector3f();
		float maxx = -Float.MAX_VALUE, minx = Float.MAX_VALUE;
		float maxy = -Float.MAX_VALUE, miny = Float.MAX_VALUE;
		float maxz = -Float.MAX_VALUE, minz = Float.MAX_VALUE;

		FloatBuffer positions = mesh.getFloatBuffer(VertexBuffer.Type.Position);
		int limit = positions.limit();
		for (int i = 0; i < limit; i += 3) {
			float x = positions.get(i);
			float y = positions.get(i + 1);
			float z = positions.get(i + 2);
			center.addLocal(x, y, z);
			maxx = x > maxx ? x : maxx;
			minx = x < minx ? x : minx;
			maxy = y > maxy ? y : maxy;
			miny = y < miny ? y : miny;
			maxz = z > maxz ? z : maxz;
			minz = z < minz ? z : minz;
		}
		center.divideLocal(limit / 3);

		float radius = Math.max(maxx - minx, maxy - miny) * 0.5f;
		return new BoundingTube(radius, maxz - minz, center);
	}
	
	/**
	 * This method returns the bounding tube of the given geometries.
	 * @param geometries
	 *        the list of geometries
	 * @return bounding tube of the given geometries
	 */
	/* package */static BoundingTube getBoundingTube(List<Geometry> geometries) {
		BoundingTube result = null;
		for (Geometry geometry : geometries) {
			BoundingTube bt = UVCoordinatesGenerator.getBoundingTube(geometry.getMesh());
			if (result == null) {
				result = bt;
			} else {
				result.merge(bt);
			}
		}
		return result;
	}

	/**
	 * A very simple bounding tube. Id holds only the basic data bout the bounding tube
	 * and does not provide full functionality of a BoundingVolume.
	 * Should be replaced with a bounding tube that extends the BoundingVolume if it is ever created.
	 * @author Marcin Roguski (Kaelthas)
	 */
	/* package */static class BoundingTube {
		private float		radius;
		private float		height;
		private Vector3f	center;

		/**
		 * Constructor creates the tube with the given params.
		 * @param radius
		 *        the radius of the tube
		 * @param height
		 *        the height of the tube
		 * @param center
		 *        the center of the tube
		 */
		public BoundingTube(float radius, float height, Vector3f center) {
			this.radius = radius;
			this.height = height;
			this.center = center;
		}

		/**
		 * This method merges two bounding tubes.
		 * @param boundingTube
		 *        bounding tube to be merged woth the current one
		 * @return new instance of bounding tube representing the tubes' merge
		 */
		public BoundingTube merge(BoundingTube boundingTube) {
			// get tubes (tube1.radius >= tube2.radius)
			BoundingTube tube1, tube2;
			if (this.radius >= boundingTube.radius) {
				tube1 = this;
				tube2 = boundingTube;
			} else {
				tube1 = boundingTube;
				tube2 = this;
			}
			float r1 = tube1.radius;
			float r2 = tube2.radius;

			float minZ = Math.min(tube1.center.z - tube1.height * 0.5f, tube2.center.z - tube2.height * 0.5f);
			float maxZ = Math.max(tube1.center.z + tube1.height * 0.5f, tube2.center.z + tube2.height * 0.5f);
			float height = maxZ - minZ;
			Vector3f distance = tube2.center.subtract(tube1.center);
			Vector3f center = tube1.center.add(distance.mult(0.5f));
			distance.z = 0;// projecting this vector on XY plane
			float d = distance.length();
			// d <= r1 - r2: tube2 is inside tube1 or touches tube1 from the inside
			// d > r1 - r2: tube2 is outside or touches tube1 or crosses tube1
			float radius = d <= r1 - r2 ? tube1.radius : (d + r1 + r2) * 0.5f;
			return new BoundingTube(radius, height, center);
		}

		/**
		 * This method returns the radius of the tube.
		 * @return the radius of the tube
		 */
		public float getRadius() {
			return radius;
		}

		/**
		 * This method returns the height of the tube.
		 * @return the height of the tube
		 */
		public float getHeight() {
			return height;
		}

		/**
		 * This method returns the center of the tube.
		 * @return the center of the tube
		 */
		public Vector3f getCenter() {
			return center;
		}
	}
}
