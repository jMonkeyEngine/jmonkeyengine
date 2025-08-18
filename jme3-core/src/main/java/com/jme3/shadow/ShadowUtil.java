/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.shadow;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.List;

/**
 * Includes various useful shadow mapping functions.
 *
 * See <ul> <li><a
 * href="http://appsrv.cse.cuhk.edu.hk/~fzhang/pssm_vrcia/">http://appsrv.cse.cuhk.edu.hk/~fzhang/pssm_vrcia/</a></li>
 * <li><a
 * href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a></li>
 * </ul> for more info.
 */
public class ShadowUtil {

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private ShadowUtil() {
    }

    /**
     * Updates a points arrays with the frustum corners of the provided camera.
     *
     * @param viewCam the viewing Camera (not null, unaffected)
     * @param points  storage for the corner coordinates (not null, length &ge;8, modified)
     */
    public static void updateFrustumPoints2(Camera viewCam, Vector3f[] points) {
        int w = viewCam.getWidth();
        int h = viewCam.getHeight();

        TempVars vars = TempVars.get();
        Vector2f tempVec2 = vars.vect2d;

        viewCam.getWorldCoordinates(tempVec2.set(0, 0), 0, points[0]);
        viewCam.getWorldCoordinates(tempVec2.set(0, h), 0, points[1]);
        viewCam.getWorldCoordinates(tempVec2.set(w, h), 0, points[2]);
        viewCam.getWorldCoordinates(tempVec2.set(w, 0), 0, points[3]);

        viewCam.getWorldCoordinates(tempVec2.set(0, 0), 1, points[4]);
        viewCam.getWorldCoordinates(tempVec2.set(0, h), 1, points[5]);
        viewCam.getWorldCoordinates(tempVec2.set(w, h), 1, points[6]);
        viewCam.getWorldCoordinates(tempVec2.set(w, 0), 1, points[7]);

        vars.release();
    }

    /**
     * Updates the provided array of {@code Vector3f} to contain the frustum corners
     * of the given camera, with optional overrides for near/far distances and a scale factor.
     * The array must have a length of at least 8.
     *
     * @param viewCam the viewing Camera (not null, unaffected)
     * @param near    distance to the near plane (in world units)
     * @param far     distance to the far plane (in world units)
     * @param scale   a factor to scale the frustum points around their center (1.0 for no scaling)
     * @param points  storage for the corner coordinates (not null, length &ge; 8, modified)
     */
    public static void updateFrustumPoints(Camera viewCam, float near, float far, float scale, Vector3f[] points) {

        TempVars vars = TempVars.get();

        Vector3f camPos = viewCam.getLocation();
        Vector3f camDir = viewCam.getDirection(vars.vect1);
        Vector3f camUp  = viewCam.getUp(vars.vect2);
        Vector3f camRight = vars.vect3.set(camDir).crossLocal(camUp).normalizeLocal();

        float depthHeightRatio = viewCam.getFrustumTop() / viewCam.getFrustumNear();
        float ftop   = viewCam.getFrustumTop();
        float fright = viewCam.getFrustumRight();
        float ratio  = fright / ftop;

        float nearHeight;
        float nearWidth;
        float farHeight;
        float farWidth;

        if (viewCam.isParallelProjection()) {
            nearHeight = ftop;
            nearWidth  = nearHeight * ratio;
            farHeight  = ftop;
            farWidth   = farHeight * ratio;
        } else {
            nearHeight = depthHeightRatio * near;
            nearWidth  = nearHeight * ratio;
            farHeight  = depthHeightRatio * far;
            farWidth   = farHeight * ratio;
        }

        Vector3f nearCenter = vars.vect4;
        Vector3f farCenter  = vars.vect5;

        // Calculate near and far center points
        nearCenter.set(camDir).multLocal(near).addLocal(camPos);
        farCenter.set(camDir).multLocal(far).addLocal(camPos);

        Vector3f nearUp     = vars.vect6.set(camUp).multLocal(nearHeight);
        Vector3f farUp      = vars.vect7.set(camUp).multLocal(farHeight);
        Vector3f nearRight  = vars.vect8.set(camRight).multLocal(nearWidth);
        Vector3f farRight   = vars.vect9.set(camRight).multLocal(farWidth);

        // Populate frustum points
        points[0].set(nearCenter).subtractLocal(nearUp).subtractLocal(nearRight);
        points[1].set(nearCenter).addLocal(nearUp).subtractLocal(nearRight);
        points[2].set(nearCenter).addLocal(nearUp).addLocal(nearRight);
        points[3].set(nearCenter).subtractLocal(nearUp).addLocal(nearRight);

        points[4].set(farCenter).subtractLocal(farUp).subtractLocal(farRight);
        points[5].set(farCenter).addLocal(farUp).subtractLocal(farRight);
        points[6].set(farCenter).addLocal(farUp).addLocal(farRight);
        points[7].set(farCenter).subtractLocal(farUp).addLocal(farRight);

        if (scale != 1.0f) {
            // find center of frustum
            Vector3f center = new Vector3f();
            for (int i = 0; i < 8; i++) {
                center.addLocal(points[i]);
            }
            center.divideLocal(8f);

            Vector3f tempVec = vars.vect10; // Reusing tempVec for calculations
            for (int i = 0; i < 8; i++) {
                tempVec.set(points[i]).subtractLocal(center);
                tempVec.multLocal(scale - 1.0f);
                points[i].addLocal(tempVec);
            }
        }

        vars.release();
    }

    /**
     * Computes the union bounding box of all geometries in the given list,
     * after transforming their world bounds by the provided {@link Transform}.
     *
     * @param list      a list of geometries (not null)
     * @param transform a coordinate transform to apply to each geometry's world bound (not null, unaffected)
     * @return a new {@link BoundingBox} representing the union of transformed bounds.
     */
    public static BoundingBox computeUnionBound(GeometryList list, Transform transform) {
        BoundingBox bbox = new BoundingBox();
        TempVars vars = TempVars.get();
        for (int i = 0; i < list.size(); i++) {
            BoundingVolume vol = list.get(i).getWorldBound();
            BoundingVolume newVol = vol.transform(transform, vars.bbox);
            //Nehon : prevent NaN and infinity values to screw the final bounding box
            if (!Float.isNaN(newVol.getCenter().x) && !Float.isInfinite(newVol.getCenter().x)) {
                bbox.mergeLocal(newVol);
            }
        }
        vars.release();
        return bbox;
    }

    /**
     * Computes the union bounding box of all geometries in the given list,
     * after transforming their world bounds by the provided {@link Matrix4f}.
     *
     * @param list a list of geometries (not null)
     * @param mat  a coordinate-transform matrix to apply to each geometry's world bound (not null, unaffected)
     * @return a new {@link BoundingBox} representing the union of transformed bounds.
     */
    public static BoundingBox computeUnionBound(GeometryList list, Matrix4f mat) {
        BoundingBox bbox = new BoundingBox();
        TempVars vars = TempVars.get();
        for (int i = 0; i < list.size(); i++) {
            BoundingVolume vol = list.get(i).getWorldBound();
            BoundingVolume store = vol.transform(mat, vars.bbox);
            //Nehon : prevent NaN and infinity values to screw the final bounding box
            if (!Float.isNaN(store.getCenter().x) && !Float.isInfinite(store.getCenter().x)) {
                bbox.mergeLocal(store);
            }
        }
        vars.release();
        return bbox;
    }

    /**
     * Computes the union bounding box of a list of {@link BoundingVolume}s.
     *
     * @param bv a list of bounding volumes (not null, elements can be null)
     * @return a new {@link BoundingBox} representing the union of the provided volumes.
     */
    public static BoundingBox computeUnionBound(List<BoundingVolume> bv) {
        BoundingBox bbox = new BoundingBox();
        for (BoundingVolume vol : bv) {
            bbox.mergeLocal(vol);
        }
        return bbox;
    }

    /**
     * Computes a {@link BoundingBox} that encloses an array of 3D points,
     * after transforming them by the provided {@link Transform}.
     *
     * @param pts       an array of location vectors (not null, unaffected)
     * @param transform a coordinate transform to apply to each point (not null, unaffected)
     * @return a new {@link BoundingBox} enclosing the transformed points.
     */
    public static BoundingBox computeBoundForPoints(Vector3f[] pts, Transform transform) {
        TempVars vars = TempVars.get();
        Vector3f min = vars.vect1.set(Vector3f.POSITIVE_INFINITY);
        Vector3f max = vars.vect2.set(Vector3f.NEGATIVE_INFINITY);
        Vector3f tempVec = vars.vect3;

        for (Vector3f pt : pts) {
            transform.transformVector(pt, tempVec);
            min.minLocal(tempVec);
            max.maxLocal(tempVec);
        }
        Vector3f center = vars.vect4.set(min).addLocal(max).multLocal(0.5f);
        Vector3f extent = vars.vect5.set(max).subtractLocal(min).multLocal(0.5f);

        BoundingBox bbox = new BoundingBox(center, extent.x, extent.y, extent.z);
        vars.release();
        return bbox;
    }

    /**
     * Computes a {@link BoundingBox} that encloses an array of 3D points,
     * after transforming them by the provided {@link Matrix4f}.
     * <p>
     * Note: An offset is added to the extent to help avoid banding artifacts
     * when frustums are aligned.
     *
     * @param pts an array of location vectors (not null, unaffected)
     * @param mat a coordinate-transform matrix (not null, unaffected)
     * @return a new {@link BoundingBox} enclosing the transformed points.
     */
    public static BoundingBox computeBoundForPoints(Vector3f[] pts, Matrix4f mat) {
        TempVars vars = TempVars.get();
        Vector3f min = vars.vect1.set(Vector3f.POSITIVE_INFINITY);
        Vector3f max = vars.vect2.set(Vector3f.NEGATIVE_INFINITY);
        Vector3f tempVec = vars.vect3;

        for (Vector3f pt : pts) {
            float w = mat.multProj(pt, tempVec);
            tempVec.x /= w;
            tempVec.y /= w;
            tempVec.z /= w; // Z component correction

            min.minLocal(tempVec);
            max.maxLocal(tempVec);
        }
        Vector3f center = vars.vect4.set(min).addLocal(max).multLocal(0.5f);
        Vector3f extent = vars.vect5.set(max).subtractLocal(min).multLocal(0.5f);

        //Nehon 08/18/2010 : Added an offset to the extent, to avoid banding artifacts when the frustums are aligned.
        BoundingBox bbox = new BoundingBox(center, extent.x + 2.0f, extent.y + 2.0f, extent.z + 2.5f);
        vars.release();
        return bbox;
    }

    /**
     * Updates the projection matrix of the shadow camera to properly contain
     * the given points (which typically represent the eye camera frustum corners).
     * This method is suitable for simple shadow camera setups where only the
     * frustum points determine the shadow camera's projection.
     *
     * @param shadowCam the shadow camera (not null, modified)
     * @param points    an array of location vectors representing the bounds to contain (not null, unaffected)
     */
    public static void updateShadowCamera(Camera shadowCam, Vector3f[] points) {
        boolean ortho = shadowCam.isParallelProjection();
        shadowCam.setProjectionMatrix(null);

        if (ortho) {
            shadowCam.setFrustum(-1, 1, -1, 1, 1, -1);
        } else {
            shadowCam.setFrustumPerspective(45, 1, 1, 150);
        }

        Matrix4f viewProjMatrix = shadowCam.getViewProjectionMatrix();
        Matrix4f projMatrix = shadowCam.getProjectionMatrix();

        BoundingBox splitBB = computeBoundForPoints(points, viewProjMatrix);

        TempVars vars = TempVars.get();

        Vector3f splitMin = splitBB.getMin(vars.vect1);
        Vector3f splitMax = splitBB.getMax(vars.vect2);

        // Create the crop matrix based on the contained bounding box.
        float scaleX, scaleY, scaleZ;
        float offsetX, offsetY, offsetZ;

        scaleX = 2.0f / (splitMax.x - splitMin.x);
        scaleY = 2.0f / (splitMax.y - splitMin.y);
        offsetX = -0.5f * (splitMax.x + splitMin.x) * scaleX;
        offsetY = -0.5f * (splitMax.y + splitMin.y) * scaleY;
        scaleZ = 1.0f / (splitMax.z - splitMin.z);
        offsetZ = -splitMin.z * scaleZ;

        Matrix4f cropMatrix = vars.tempMat4;
        cropMatrix.set(scaleX, 0f, 0f, offsetX,
                0f, scaleY, 0f, offsetY,
                0f, 0f, scaleZ, offsetZ,
                0f, 0f, 0f, 1f);

        Matrix4f result = vars.tempMat42;
        result.set(cropMatrix);
        result.multLocal(projMatrix);

        shadowCam.setProjectionMatrix(result);
        vars.release();
    }

    /**
     * OccludersExtractor is a helper class to collect splitOccluders from scene recursively.
     * It utilizes the scene hierarchy, instead of making the huge flat geometries list first.
     * Instead of adding all geometries from scene to the RenderQueue.shadowCast and checking
     * all of them one by one against camera frustum the whole Node is checked first
     * to hopefully avoid the check on its children.
     */
    public static class OccludersExtractor {
        // global variables set in order not to have recursive process method with too many parameters
        private final Matrix4f viewProjMatrix;
        private int  casterCount;
        private final BoundingBox splitBB, casterBB;
        private final GeometryList splitOccluders;
        private final TempVars vars;

        /**
         * Creates a new {@code OccludersExtractor}.
         *
         * @param viewProjMatrix   the view-projection matrix of the shadow camera (not null, unaffected)
         * @param splitBB          the bounding box of the viewer camera's frustum in shadow camera's view-projection space (not null, unaffected)
         * @param casterBB         a bounding box to merge found caster bounds into (not null, modified)
         * @param splitOccluders   a list to add found caster geometries to (may be null if only counting casters)
         * @param vars             a {@link TempVars} instance for temporary object pooling (not null, managed by caller)
         */
        public OccludersExtractor(Matrix4f viewProjMatrix, BoundingBox splitBB, BoundingBox casterBB,
                                  GeometryList splitOccluders, TempVars vars) {
            this.viewProjMatrix = viewProjMatrix;
            this.casterCount = 0;
            this.splitBB = splitBB;
            this.casterBB = casterBB;
            this.splitOccluders = splitOccluders;
            this.vars = vars;
        }

        /**
         * Recursively checks the provided scene graph for shadow casters (occluders)
         * and adds them to the internal list if they intersect the specified frustum.
         * The {@code casterCount} and {@code casterBB} will be updated during this process.
         *
         * @param scene the root of the scene to check (may be null)
         * @return this {@code OccludersExtractor} instance for chaining.
         */
        public OccludersExtractor addOccluders(Spatial scene) {
            if (scene != null) {
                process(scene);
            }
            return this;
        }

        /**
         * Returns the number of shadow casters found by this extractor.
         *
         * @return the current count of shadow casters.
         */
        public int getCasterCount() {
            return casterCount;
        }

        /**
         * Internal recursive method to process the scene graph and identify shadow casters.
         *
         * @param scene the current spatial to process (not null)
         */
        private void process(Spatial scene) {
            if (scene.getCullHint() == Spatial.CullHint.Always) {
                return;
            }

            RenderQueue.ShadowMode shadowMode = scene.getShadowMode();
            if (scene instanceof Geometry) {
                // convert bounding box to light's viewproj space
                Geometry occluder = (Geometry) scene;
                if (shadowMode != RenderQueue.ShadowMode.Off
                        && shadowMode != RenderQueue.ShadowMode.Receive
                        && !occluder.isGrouped()
                        && occluder.getWorldBound() != null) {

                    BoundingVolume bv = occluder.getWorldBound();
                    BoundingVolume occBox = bv.transform(viewProjMatrix, vars.bbox);

                    boolean intersects = splitBB.intersects(occBox);
                    if (!intersects && occBox instanceof BoundingBox) {
                        BoundingBox occBB = (BoundingBox) occBox;
                        float originalZExtent = occBB.getZExtent();

                        // Attempt to extend the occluder further into the frustum for better shadow coverage.
                        // This fixes issues where the caster itself is outside the view camera, but its shadow is visible.
                        //      The number is in world units
                        occBB.setZExtent(originalZExtent + 50);
                        occBB.setCenter(occBB.getCenter().addLocal(0, 0, 25));
                        if (splitBB.intersects(occBB)) {
                            // Prevent NaN and infinity values from screwing the final bounding box
                            if (!Float.isNaN(occBox.getCenter().x) && !Float.isInfinite(occBox.getCenter().x)) {
                                // Restore original bound before merging to prevent depth range extension
                                occBB.setZExtent(originalZExtent - 50);
                                occBB.setCenter(occBB.getCenter().subtractLocal(0, 0, 25));
                                casterBB.mergeLocal(occBox);
                                casterCount++;
                            }
                            if (splitOccluders != null) {
                                splitOccluders.add(occluder);
                            }
                        }
                    } else if (intersects) {
                        casterBB.mergeLocal(occBox);
                        casterCount++;
                        if (splitOccluders != null) {
                            splitOccluders.add(occluder);
                        }
                    }
                }
            } else if (scene instanceof Node && scene.getWorldBound() != null) {
                Node nodeOcc = (Node) scene;
                BoundingVolume bv = nodeOcc.getWorldBound();
                BoundingVolume occBox = bv.transform(viewProjMatrix, vars.bbox);

                boolean intersects = splitBB.intersects(occBox);
                if (!intersects && occBox instanceof BoundingBox) {
                    BoundingBox occBB = (BoundingBox) occBox;
                    float originalZExtent = occBB.getZExtent();

                    // Attempt to extend the occluder further into the frustum for better shadow coverage.
                    // This fixes issues where the caster itself is outside the view camera, but its shadow is visible.
                    //      The number is in world units
                    occBB.setZExtent(originalZExtent + 50);
                    occBB.setCenter(occBB.getCenter().addLocal(0, 0, 25));
                    intersects = splitBB.intersects(occBB);
                }

                if (intersects) {
                    for (Spatial child : ((Node) scene).getChildren()) {
                        process(child); // Recursively process children
                    }
                }
            }
        }
    }

    /**
     * Updates the shadow camera's projection matrix to encompass the viewer camera's
     * frustum corners and the identified shadow occluder and receiver objects.
     * This method calculates a tight bounding box in the shadow camera's view-projection
     * space and uses it to derive a suitable orthographic projection.
     *
     * @param viewPort        the current view port (not null)
     * @param receivers       a list of geometries acting as shadow receivers (not null, unaffected)
     * @param shadowCam       the shadow camera to be updated (not null, modified)
     * @param points          an array of 8 {@code Vector3f} representing the viewer camera's frustum corners in world space (not null, unaffected)
     * @param splitOccluders  a {@link GeometryList} to populate with geometries that act as shadow casters (may be empty, modified)
     * @param shadowMapSize   the size of each edge of the shadow map texture (in pixels), used for stabilization
     */
    public static void updateShadowCamera(ViewPort viewPort,
                                          GeometryList receivers,
                                          Camera shadowCam,
                                          Vector3f[] points,
                                          GeometryList splitOccluders,
                                          float shadowMapSize) {

        boolean ortho = shadowCam.isParallelProjection();

        shadowCam.setProjectionMatrix(null);

        if (ortho) {
            shadowCam.setFrustum(-shadowCam.getFrustumFar(), shadowCam.getFrustumFar(), -1, 1, 1, -1);
        }

        // create transform to rotate points to viewspace
        Matrix4f viewProjMatrix = shadowCam.getViewProjectionMatrix();

        BoundingBox splitBB = computeBoundForPoints(points, viewProjMatrix);

        TempVars vars = TempVars.get();

        BoundingBox casterBB = new BoundingBox();
        BoundingBox receiverBB = new BoundingBox();

        int receiverCount = 0;

        for (int i = 0; i < receivers.size(); i++) {
            // convert bounding box to light's viewproj space
            Geometry receiver = receivers.get(i);
            BoundingVolume bv = receiver.getWorldBound();
            BoundingVolume recvBox = bv.transform(viewProjMatrix, vars.bbox);

            if (splitBB.intersects(recvBox)) {
                //Nehon : prevent NaN and infinity values to screw the final bounding box
                if (!Float.isNaN(recvBox.getCenter().x) && !Float.isInfinite(recvBox.getCenter().x)) {
                    receiverBB.mergeLocal(recvBox);
                    receiverCount++;
                }
            }
        }

        // collect splitOccluders through scene recursive traverse
        OccludersExtractor occExt = new OccludersExtractor(viewProjMatrix, splitBB, casterBB, splitOccluders, vars);
        for (Spatial scene : viewPort.getScenes()) {
            occExt.addOccluders(scene);
        }
        int casterCount = occExt.getCasterCount();

        if (casterCount == 0) {
            vars.release();
            return;
        }

        //Nehon 08/18/2010 this is to avoid shadow bleeding when the ground is set to only receive shadows
        if (casterCount != receiverCount) {
            casterBB.setXExtent(casterBB.getXExtent() + 2.0f);
            casterBB.setYExtent(casterBB.getYExtent() + 2.0f);
            casterBB.setZExtent(casterBB.getZExtent() + 2.0f);
        }

        Vector3f casterMin = casterBB.getMin(vars.vect1);
        Vector3f casterMax = casterBB.getMax(vars.vect2);

        Vector3f receiverMin = receiverBB.getMin(vars.vect3);
        Vector3f receiverMax = receiverBB.getMax(vars.vect4);

        Vector3f splitMin = splitBB.getMin(vars.vect5);
        Vector3f splitMax = splitBB.getMax(vars.vect6);

        splitMin.z = 0;

//        if (!ortho) {
//            shadowCam.setFrustumPerspective(45, 1, 1, splitMax.z);
//        }

        Matrix4f projMatrix = shadowCam.getProjectionMatrix();

        Vector3f cropMin = vars.vect7;
        Vector3f cropMax = vars.vect8;

        // IMPORTANT: Special handling for Z values
        cropMin.x = max(max(casterMin.x, receiverMin.x), splitMin.x);
        cropMax.x = min(min(casterMax.x, receiverMax.x), splitMax.x);

        cropMin.y = max(max(casterMin.y, receiverMin.y), splitMin.y);
        cropMax.y = min(min(casterMax.y, receiverMax.y), splitMax.y);

        cropMin.z = min(casterMin.z, splitMin.z);
        cropMax.z = min(receiverMax.z, splitMax.z);

        // Create the crop matrix.
        float scaleX, scaleY, scaleZ;
        float offsetX, offsetY, offsetZ;

        float deltaCropX = cropMax.x - cropMin.x;
        float deltaCropY = cropMax.y - cropMin.y;
        scaleX = deltaCropX == 0 ? 0 : 2.0f / deltaCropX;
        scaleY = deltaCropY == 0 ? 0 : 2.0f / deltaCropY;

        // Shadow map stabilization approximation from ShaderX 7 (Practical Cascaded Shadow Maps adapted to PSSM)
        // Scale stabilization: Quantizes the scale to prevent shimmering artifacts during camera movement.
        float halfTextureSize = shadowMapSize * 0.5f;

        if (halfTextureSize != 0 && scaleX > 0 && scaleY > 0) {
            float scaleQuantizer = 0.1f;
            scaleX = 1.0f / FastMath.ceil(1.0f / scaleX * scaleQuantizer) * scaleQuantizer;
            scaleY = 1.0f / FastMath.ceil(1.0f / scaleY * scaleQuantizer) * scaleQuantizer;
        }

        offsetX = -0.5f * (cropMax.x + cropMin.x) * scaleX;
        offsetY = -0.5f * (cropMax.y + cropMin.y) * scaleY;

        // Shadow map stabilization approximation from ShaderX 7 (Practical Cascaded Shadow Maps adapted to PSSM)
        // Offset stabilization: Quantizes the offset to align pixel boundaries
        if (halfTextureSize != 0 && scaleX > 0 && scaleY > 0) {
            offsetX = FastMath.ceil(offsetX * halfTextureSize) / halfTextureSize;
            offsetY = FastMath.ceil(offsetY * halfTextureSize) / halfTextureSize;
        }

        float deltaCropZ = cropMax.z - cropMin.z;
        scaleZ = deltaCropZ == 0 ? 0 : 1.0f / deltaCropZ;
        offsetZ = -cropMin.z * scaleZ;

        Matrix4f cropMatrix = vars.tempMat4;
        cropMatrix.set(scaleX, 0f, 0f, offsetX,
                0f, scaleY, 0f, offsetY,
                0f, 0f, scaleZ, offsetZ,
                0f, 0f, 0f, 1f);

        Matrix4f result = vars.tempMat42;
        result.set(cropMatrix);
        result.multLocal(projMatrix);

        shadowCam.setProjectionMatrix(result);
        vars.release();
    }

    /**
     * Populates the {@code outputGeometryList} with geometries from the
     * {@code inputGeometryList} that are within the frustum of the given camera.
     * This method iterates through each geometry and checks its world bound
     * against the camera's frustum.
     *
     * @param inputGeometryList  The list containing all geometries to check against the camera frustum (not null, unaffected)
     * @param camera             The camera to check geometries against (not null, unaffected)
     * @param outputGeometryList The list to which geometries within the camera frustum will be added (not null, modified)
     */
    public static void getGeometriesInCamFrustum(GeometryList inputGeometryList,
                                                 Camera camera, GeometryList outputGeometryList) {
        int planeState = camera.getPlaneState();
        for (int i = 0; i < inputGeometryList.size(); i++) {
            Geometry g = inputGeometryList.get(i);
            camera.setPlaneState(0);
            if (camera.contains(g.getWorldBound()) != Camera.FrustumIntersect.Outside) {
                outputGeometryList.add(g);
            }
        }
        camera.setPlaneState(planeState);
    }

    /**
     * Populates the {@code outputGeometryList} with geometries from the
     * provided scene graph (starting from {@code rootScene}) that are
     * within the frustum of the given camera and match the specified shadow mode.
     * This method traverses the scene hierarchy recursively.
     *
     * @param rootScene          The root of the scene to traverse (may be null)
     * @param camera             The camera to check geometries against (not null, unaffected)
     * @param mode               The {@link RenderQueue.ShadowMode} to filter geometries by
     * @param outputGeometryList The list to which matching geometries within the camera frustum will be added (not null, modified)
     */
    public static void getGeometriesInCamFrustum(Spatial rootScene, Camera camera,
                                                 RenderQueue.ShadowMode mode, GeometryList outputGeometryList) {
        if (rootScene instanceof Node) {
            int planeState = camera.getPlaneState();
            addGeometriesInCamFrustumFromNode(camera, (Node) rootScene, mode, outputGeometryList);
            camera.setPlaneState(planeState);
        }
    }

    /**
     * Helper function to determine if a spatial's shadow mode matches the desired mode.
     * This is useful for distinguishing between shadow casters and receivers.
     *
     * @param shadowMode The actual {@link RenderQueue.ShadowMode} of a spatial.
     * @param desired    The desired {@link RenderQueue.ShadowMode} to check against.
     * @return true if the {@code shadowMode} allows for the {@code desired} shadow behavior, false otherwise.
     */
    private static boolean checkShadowMode(RenderQueue.ShadowMode shadowMode, RenderQueue.ShadowMode desired) {
        if (shadowMode == RenderQueue.ShadowMode.Off) {
            return false;
        }
        switch (desired) {
            case Cast:
                return shadowMode == RenderQueue.ShadowMode.Cast || shadowMode == RenderQueue.ShadowMode.CastAndReceive;
            case Receive:
                return shadowMode == RenderQueue.ShadowMode.Receive || shadowMode == RenderQueue.ShadowMode.CastAndReceive;
            case CastAndReceive:
                return true; // Any non-Off mode implies CastAndReceive if that's desired
            default:
                return false;
        }
    }

    /**
     * Recursive helper function to populate the {@code outputGeometryList} with geometries
     * from a given node and its children that are within the camera frustum and match the
     * specified shadow mode.
     *
     * @param camera             The camera to check against (not null, unaffected)
     * @param scene              The current node in the scene graph to traverse (not null)
     * @param mode               The {@link RenderQueue.ShadowMode} to filter geometries by.
     * @param outputGeometryList The list to add matching geometries to (not null, modified)
     */
    private static void addGeometriesInCamFrustumFromNode(Camera camera, Node scene,
                                                          RenderQueue.ShadowMode mode, GeometryList outputGeometryList) {
        if (scene.getCullHint() == Spatial.CullHint.Always) {
            return;
        }

        camera.setPlaneState(0);
        if (camera.contains(scene.getWorldBound()) != Camera.FrustumIntersect.Outside) {
            for (Spatial child : scene.getChildren()) {
                if (child instanceof Node)
                    addGeometriesInCamFrustumFromNode(camera, (Node) child, mode, outputGeometryList);
                else if (child instanceof Geometry && child.getCullHint() != Spatial.CullHint.Always) {
                    camera.setPlaneState(0);
                    if (checkShadowMode(child.getShadowMode(), mode) &&
                            !((Geometry) child).isGrouped() &&
                            camera.contains(child.getWorldBound()) != Camera.FrustumIntersect.Outside) {
                        outputGeometryList.add((Geometry) child);
                    }
                }
            }
        }
    }

    /**
     * Populates the {@code outputGeometryList} with geometries from the
     * {@code inputGeometryList} that are within the light's effective radius,
     * represented by an array of cameras (e.g., 6 cameras for a point light's cubemap faces).
     * A geometry is considered "in light radius" if its world bound intersects
     * the frustum of at least one of the provided cameras.
     *
     * @param inputGeometryList  The list containing all geometries to check (not null, unaffected)
     * @param cameras            An array of cameras representing the light's view frustums (not null, unaffected)
     * @param outputGeometryList The list to which geometries within the light's radius will be added (not null, modified)
     */
    public static void getGeometriesInLightRadius(GeometryList inputGeometryList,
                                                  Camera[] cameras, GeometryList outputGeometryList) {
        for (int i = 0; i < inputGeometryList.size(); i++) {
            Geometry g = inputGeometryList.get(i);
            boolean inFrustum = false;

            // Iterate through all light cameras, stop if found in any
            for (int j = 0; j < cameras.length && !inFrustum; j++) {
                Camera camera = cameras[j];
                int planeState = camera.getPlaneState();
                camera.setPlaneState(0);
                inFrustum = camera.contains(g.getWorldBound()) != Camera.FrustumIntersect.Outside;
                camera.setPlaneState(planeState);
            }
            if (inFrustum) {
                outputGeometryList.add(g);
            }
        }
    }

    /**
     * Populates the {@code outputGeometryList} with geometries from the children
     * of {@code rootScene} that are both in the frustum of the main viewport camera
     * ({@code vpCamera}) and within the view frustum of at least one camera
     * from the provided {@code cameras} array (representing a light's view space,
     * like for point or spot lights). The geometries are also filtered by their
     * {@link RenderQueue.ShadowMode}.
     *
     * @param rootScene          The root of the scene to traverse (may be null)
     * @param vpCamera           The main viewport camera (not null, unaffected)
     * @param cameras            An array of cameras representing the light's view frustums (not null, unaffected)
     * @param mode               The {@link RenderQueue.ShadowMode} to filter geometries by.
     * @param outputGeometryList The list to which matching geometries will be added (not null, modified)
     */
    public static void getLitGeometriesInViewPort(Spatial rootScene, Camera vpCamera, Camera[] cameras,
                                                  RenderQueue.ShadowMode mode, GeometryList outputGeometryList) {
        if (rootScene instanceof Node) {
            addGeometriesInCamFrustumAndViewPortFromNode(vpCamera, cameras, rootScene, mode, outputGeometryList);
        }
    }

    /**
     * Recursive helper function to collect geometries that are visible in both
     * the main viewport camera and at least one light view camera.
     *
     * @param vpCamera           The main viewport camera (not null, unaffected)
     * @param cameras            An array of cameras representing the light's view frustums (not null, unaffected)
     * @param scene              The current spatial (Node or Geometry) to process (not null)
     * @param mode               The {@link RenderQueue.ShadowMode} to filter geometries by.
     * @param outputGeometryList The list to add matching geometries to (not null, modified)
     */
    private static void addGeometriesInCamFrustumAndViewPortFromNode(Camera vpCamera, Camera[] cameras,
                                                                     Spatial scene, RenderQueue.ShadowMode mode, GeometryList outputGeometryList) {
        if (scene.getCullHint() == Spatial.CullHint.Always) {
            return;
        }

        boolean inFrustum = false;
        for (int j = 0; j < cameras.length && !inFrustum; j++) {
            Camera camera = cameras[j];
            int planeState = camera.getPlaneState();
            camera.setPlaneState(0);
            inFrustum = camera.contains(scene.getWorldBound()) != Camera.FrustumIntersect.Outside && scene.checkCulling(vpCamera);
            camera.setPlaneState(planeState);
        }
        if (inFrustum) {
            if (scene instanceof Node) {
                Node node = (Node) scene;
                for (Spatial child : node.getChildren()) {
                    addGeometriesInCamFrustumAndViewPortFromNode(vpCamera, cameras, child, mode, outputGeometryList);
                }
            } else if (scene instanceof Geometry) {
                if (checkShadowMode(scene.getShadowMode(), mode) && !((Geometry) scene).isGrouped()) {
                    outputGeometryList.add((Geometry) scene);
                }
            }
        }
    }

}
