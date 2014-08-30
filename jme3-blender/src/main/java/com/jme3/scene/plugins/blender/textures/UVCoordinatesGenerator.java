/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.plugins.blender.textures.UVProjectionGenerator.UVProjectionType;
import com.jme3.util.BufferUtils;

/**
 * This class is used for UV coordinates generation.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class UVCoordinatesGenerator {
    private static final Logger LOGGER = Logger.getLogger(UVCoordinatesGenerator.class.getName());

    public static enum UVCoordinatesType {
        TEXCO_ORCO(1), TEXCO_REFL(2), TEXCO_NORM(4), TEXCO_GLOB(8), TEXCO_UV(16), TEXCO_OBJECT(32), TEXCO_LAVECTOR(64), TEXCO_VIEW(128),
        TEXCO_STICKY(256), TEXCO_OSA(512), TEXCO_WINDOW(1024), NEED_UV(2048), TEXCO_TANGENT(4096),
        TEXCO_PARTICLE_OR_STRAND(8192), //TEXCO_PARTICLE (since blender 2.6x) has also the value of: 8192 but is used for halo materials instead of normal materials
        TEXCO_STRESS(16384), TEXCO_SPEED(32768);

        public final int blenderValue;

        private UVCoordinatesType(int blenderValue) {
            this.blenderValue = blenderValue;
        }

        public static UVCoordinatesType valueOf(int blenderValue) {
            for (UVCoordinatesType coordinatesType : UVCoordinatesType.values()) {
                if (coordinatesType.blenderValue == blenderValue) {
                    return coordinatesType;
                }
            }
            return null;
        }
    }

    /**
     * Generates a UV coordinates for 2D texture.
     * 
     * @param mesh
     *            the mesh we generate UV's for
     * @param texco
     *            UV coordinates type
     * @param projection
     *            projection type
     * @param geometries
     *            the geometris the given mesh belongs to (required to compute
     *            bounding box)
     * @return UV coordinates for the given mesh
     */
    public static List<Vector2f> generateUVCoordinatesFor2DTexture(Mesh mesh, UVCoordinatesType texco, UVProjectionType projection, Geometry geometries) {
        List<Vector2f> result = new ArrayList<Vector2f>();
        BoundingBox bb = UVCoordinatesGenerator.getBoundingBox(geometries);
        float[] inputData = null;// positions, normals, reflection vectors, etc.

        switch (texco) {
            case TEXCO_ORCO:
                inputData = BufferUtils.getFloatArray(mesh.getFloatBuffer(VertexBuffer.Type.Position));
                break;
            case TEXCO_UV:// this should be used if not defined by user explicitly
                Vector2f[] data = new Vector2f[] { new Vector2f(0, 1), new Vector2f(0, 0), new Vector2f(1, 0) };
                for (int i = 0; i < mesh.getVertexCount(); ++i) {
                    result.add(data[i % 3]);
                }
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

        if (inputData != null) {// make projection calculations
            switch (projection) {
                case PROJECTION_FLAT:
                    inputData = UVProjectionGenerator.flatProjection(inputData, bb);
                    break;
                case PROJECTION_CUBE:
                    inputData = UVProjectionGenerator.cubeProjection(inputData, bb);
                    break;
                case PROJECTION_TUBE:
                    BoundingTube bt = UVCoordinatesGenerator.getBoundingTube(geometries);
                    inputData = UVProjectionGenerator.tubeProjection(inputData, bt);
                    break;
                case PROJECTION_SPHERE:
                    BoundingSphere bs = UVCoordinatesGenerator.getBoundingSphere(geometries);
                    inputData = UVProjectionGenerator.sphereProjection(inputData, bs);
                    break;
                default:
                    throw new IllegalStateException("Unknown projection type: " + projection);
            }
            for (int i = 0; i < inputData.length; i += 2) {
                result.add(new Vector2f(inputData[i], inputData[i + 1]));
            }
        }
        return result;
    }

    /**
     * Generates a UV coordinates for 3D texture.
     * 
     * @param mesh
     *            the mesh we generate UV's for
     * @param texco
     *            UV coordinates type
     * @param coordinatesSwappingIndexes
     *            coordinates swapping indexes
     * @param geometries
     *            the geometris the given mesh belongs to (required to compute
     *            bounding box)
     * @return UV coordinates for the given mesh
     */
    public static List<Vector3f> generateUVCoordinatesFor3DTexture(Mesh mesh, UVCoordinatesType texco, int[] coordinatesSwappingIndexes, Geometry... geometries) {
        List<Vector3f> result = new ArrayList<Vector3f>();
        BoundingBox bb = UVCoordinatesGenerator.getBoundingBox(geometries);
        float[] inputData = null;// positions, normals, reflection vectors, etc.

        switch (texco) {
            case TEXCO_ORCO:
                inputData = BufferUtils.getFloatArray(mesh.getFloatBuffer(VertexBuffer.Type.Position));
                break;
            case TEXCO_UV:
                Vector2f[] data = new Vector2f[] { new Vector2f(0, 1), new Vector2f(0, 0), new Vector2f(1, 0) };
                for (int i = 0; i < mesh.getVertexCount(); ++i) {
                    Vector2f uv = data[i % 3];
                    result.add(new Vector3f(uv.x, uv.y, 0));
                }
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
            Vector3f min = bb.getMin(null);
            float[] uvCoordsResults = new float[4];// used for coordinates swapping
            float[] ext = new float[] { bb.getXExtent() * 2, bb.getYExtent() * 2, bb.getZExtent() * 2 };
            for (int i = 0; i < ext.length; ++i) {
                if (ext[i] == 0) {
                    ext[i] = 1;
                }
            }
            // now transform the coordinates so that they are in the range of
            // <0; 1>
            for (int i = 0; i < inputData.length; i += 3) {
                uvCoordsResults[1] = (inputData[i] - min.x) / ext[0];
                uvCoordsResults[2] = (inputData[i + 1] - min.y) / ext[1];
                uvCoordsResults[3] = (inputData[i + 2] - min.z) / ext[2];
                result.add(new Vector3f(uvCoordsResults[coordinatesSwappingIndexes[0]], uvCoordsResults[coordinatesSwappingIndexes[1]], uvCoordsResults[coordinatesSwappingIndexes[2]]));
            }
        }
        return result;
    }

    /**
     * This method should be used to determine if the texture will ever be
     * computed. If the texture coordinates are not supported then the try of
     * flattening the texture might result in runtime exceptions occurence.
     * 
     * @param texco
     *            the texture coordinates type
     * @return <b>true</b> if the type is supported and false otherwise
     */
    public static boolean isTextureCoordinateTypeSupported(UVCoordinatesType texco) {
        switch (texco) {
            case TEXCO_ORCO:
            case TEXCO_UV:
            case TEXCO_NORM:
                return true;
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
                return false;
            default:
                throw new IllegalStateException("Unknown texture coordinates value: " + texco);
        }
    }

    /**
     * This method returns the bounding box of the given geometries.
     * 
     * @param geometries
     *            the list of geometries
     * @return bounding box of the given geometries
     */
    public static BoundingBox getBoundingBox(Geometry... geometries) {
        BoundingBox result = null;
        for (Geometry geometry : geometries) {
            geometry.updateModelBound();
            BoundingVolume bv = geometry.getModelBound();
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
        return result;
    }

    /**
     * This method returns the bounding sphere of the given geometries.
     * 
     * @param geometries
     *            the list of geometries
     * @return bounding sphere of the given geometries
     */
    /* package */static BoundingSphere getBoundingSphere(Geometry... geometries) {
        BoundingSphere result = null;
        for (Geometry geometry : geometries) {
            geometry.updateModelBound();
            BoundingVolume bv = geometry.getModelBound();
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
        return result;
    }

    /**
     * This method returns the bounding tube of the given geometries.
     * 
     * @param geometries
     *            the list of geometries
     * @return bounding tube of the given geometries
     */
    /* package */static BoundingTube getBoundingTube(Geometry... geometries) {
        BoundingTube result = null;
        for (Geometry geometry : geometries) {
            BoundingBox bb = UVCoordinatesGenerator.getBoundingBox(geometry);
            Vector3f max = bb.getMax(null);
            Vector3f min = bb.getMin(null);
            float radius = Math.max(max.x - min.x, max.y - min.y) * 0.5f;
            
            BoundingTube bt = new BoundingTube(radius, max.z - min.z, bb.getCenter());
            if (result == null) {
                result = bt;
            } else {
                result.merge(bt);
            }
        }
        return result;
    }

    /**
     * A very simple bounding tube. It holds only the basic data bout the
     * bounding tube and does not provide full functionality of a
     * BoundingVolume. Should be replaced with a bounding tube that extends the
     * BoundingVolume if it is ever created.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    /* package */static class BoundingTube {
        private float    radius;
        private float    height;
        private Vector3f center;

        /**
         * Constructor creates the tube with the given params.
         * 
         * @param radius
         *            the radius of the tube
         * @param height
         *            the height of the tube
         * @param center
         *            the center of the tube
         */
        public BoundingTube(float radius, float height, Vector3f center) {
            this.radius = radius;
            this.height = height;
            this.center = center;
        }

        /**
         * This method merges two bounding tubes.
         * 
         * @param boundingTube
         *            bounding tube to be merged woth the current one
         * @return new instance of bounding tube representing the tubes' merge
         */
        public BoundingTube merge(BoundingTube boundingTube) {
            // get tubes (tube1.radius >= tube2.radius)
            BoundingTube tube1, tube2;
            if (radius >= boundingTube.radius) {
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
            // d <= r1 - r2: tube2 is inside tube1 or touches tube1 from the
            // inside
            // d > r1 - r2: tube2 is outside or touches tube1 or crosses tube1
            float radius = d <= r1 - r2 ? tube1.radius : (d + r1 + r2) * 0.5f;
            return new BoundingTube(radius, height, center);
        }

        /**
         * @return the radius of the tube
         */
        public float getRadius() {
            return radius;
        }

        /**
         * @return the height of the tube
         */
        public float getHeight() {
            return height;
        }

        /**
         * @return the center of the tube
         */
        public Vector3f getCenter() {
            return center;
        }
    }
}
