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
package com.jme3.shadow;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.scene.Geometry;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.List;

/**
 * Includes various useful shadow mapping functions.
 *
 * @see
 * <ul>
 * <li><a href="http://appsrv.cse.cuhk.edu.hk/~fzhang/pssm_vrcia/">http://appsrv.cse.cuhk.edu.hk/~fzhang/pssm_vrcia/</a></li>
 * <li><a href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a></li>
 * </ul>
 * for more info.
 */
public class ShadowUtil {

    /**
     * Updates a points arrays with the frustum corners of the provided camera.
     * @param viewCam
     * @param points 
     */
    public static void updateFrustumPoints2(Camera viewCam, Vector3f[] points) {
        int w = viewCam.getWidth();
        int h = viewCam.getHeight();
        float n = viewCam.getFrustumNear();
        float f = viewCam.getFrustumFar();

        points[0].set(viewCam.getWorldCoordinates(new Vector2f(0, 0), n));
        points[1].set(viewCam.getWorldCoordinates(new Vector2f(0, h), n));
        points[2].set(viewCam.getWorldCoordinates(new Vector2f(w, h), n));
        points[3].set(viewCam.getWorldCoordinates(new Vector2f(w, 0), n));

        points[4].set(viewCam.getWorldCoordinates(new Vector2f(0, 0), f));
        points[5].set(viewCam.getWorldCoordinates(new Vector2f(0, h), f));
        points[6].set(viewCam.getWorldCoordinates(new Vector2f(w, h), f));
        points[7].set(viewCam.getWorldCoordinates(new Vector2f(w, 0), f));
    }

    /**
     * Updates the points array to contain the frustum corners of the given
     * camera. The nearOverride and farOverride variables can be used
     * to override the camera's near/far values with own values.
     *
     * TODO: Reduce creation of new vectors
     *
     * @param viewCam
     * @param nearOverride
     * @param farOverride
     */
    public static void updateFrustumPoints(Camera viewCam,
            float nearOverride,
            float farOverride,
            float scale,
            Vector3f[] points) {

        Vector3f pos = viewCam.getLocation();
        Vector3f dir = viewCam.getDirection();
        Vector3f up = viewCam.getUp();

        float depthHeightRatio = viewCam.getFrustumTop() / viewCam.getFrustumNear();
        float near = nearOverride;
        float far = farOverride;
        float ftop = viewCam.getFrustumTop();
        float fright = viewCam.getFrustumRight();
        float ratio = fright / ftop;

        float near_height;
        float near_width;
        float far_height;
        float far_width;

        if (viewCam.isParallelProjection()) {
            near_height = ftop;
            near_width = near_height * ratio;
            far_height = ftop;
            far_width = far_height * ratio;
        } else {
            near_height = depthHeightRatio * near;
            near_width = near_height * ratio;
            far_height = depthHeightRatio * far;
            far_width = far_height * ratio;
        }

        Vector3f right = dir.cross(up).normalizeLocal();

        Vector3f temp = new Vector3f();
        temp.set(dir).multLocal(far).addLocal(pos);
        Vector3f farCenter = temp.clone();
        temp.set(dir).multLocal(near).addLocal(pos);
        Vector3f nearCenter = temp.clone();

        Vector3f nearUp = temp.set(up).multLocal(near_height).clone();
        Vector3f farUp = temp.set(up).multLocal(far_height).clone();
        Vector3f nearRight = temp.set(right).multLocal(near_width).clone();
        Vector3f farRight = temp.set(right).multLocal(far_width).clone();

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

            Vector3f cDir = new Vector3f();
            for (int i = 0; i < 8; i++) {
                cDir.set(points[i]).subtractLocal(center);
                cDir.multLocal(scale - 1.0f);
                points[i].addLocal(cDir);
            }
        }
    }

    /**
     * Compute bounds of a geomList
     * @param list
     * @param transform
     * @return 
     */
    public static BoundingBox computeUnionBound(GeometryList list, Transform transform) {
        BoundingBox bbox = new BoundingBox();
        for (int i = 0; i < list.size(); i++) {
            BoundingVolume vol = list.get(i).getWorldBound();
            BoundingVolume newVol = vol.transform(transform);
            //Nehon : prevent NaN and infinity values to screw the final bounding box
            if (!Float.isNaN(newVol.getCenter().x) && !Float.isInfinite(newVol.getCenter().x)) {
                bbox.mergeLocal(newVol);
            }
        }
        return bbox;
    }

    /**
     * Compute bounds of a geomList
     * @param list
     * @param mat
     * @return 
     */
    public static BoundingBox computeUnionBound(GeometryList list, Matrix4f mat) {
        BoundingBox bbox = new BoundingBox();
        BoundingVolume store = null;
        for (int i = 0; i < list.size(); i++) {
            BoundingVolume vol = list.get(i).getWorldBound();
            store = vol.clone().transform(mat, null);
            //Nehon : prevent NaN and infinity values to screw the final bounding box
            if (!Float.isNaN(store.getCenter().x) && !Float.isInfinite(store.getCenter().x)) {
                bbox.mergeLocal(store);
            }
        }
        return bbox;
    }

    /**
     * Computes the bounds of multiple bounding volumes
     * @param bv
     * @return 
     */
    public static BoundingBox computeUnionBound(List<BoundingVolume> bv) {
        BoundingBox bbox = new BoundingBox();
        for (int i = 0; i < bv.size(); i++) {
            BoundingVolume vol = bv.get(i);
            bbox.mergeLocal(vol);
        }
        return bbox;
    }

    /**
     * Compute bounds from an array of points
     * @param pts
     * @param transform
     * @return 
     */
    public static BoundingBox computeBoundForPoints(Vector3f[] pts, Transform transform) {
        Vector3f min = new Vector3f(Vector3f.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Vector3f.NEGATIVE_INFINITY);
        Vector3f temp = new Vector3f();
        for (int i = 0; i < pts.length; i++) {
            transform.transformVector(pts[i], temp);

            min.minLocal(temp);
            max.maxLocal(temp);
        }
        Vector3f center = min.add(max).multLocal(0.5f);
        Vector3f extent = max.subtract(min).multLocal(0.5f);
        return new BoundingBox(center, extent.x, extent.y, extent.z);
    }

    /**
     * Compute bounds from an array of points
     * @param pts
     * @param mat
     * @return 
     */
    public static BoundingBox computeBoundForPoints(Vector3f[] pts, Matrix4f mat) {
        Vector3f min = new Vector3f(Vector3f.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Vector3f.NEGATIVE_INFINITY);
        Vector3f temp = new Vector3f();

        for (int i = 0; i < pts.length; i++) {
            float w = mat.multProj(pts[i], temp);

            temp.x /= w;
            temp.y /= w;
            // Why was this commented out?
            temp.z /= w;

            min.minLocal(temp);
            max.maxLocal(temp);
        }

        Vector3f center = min.add(max).multLocal(0.5f);
        Vector3f extent = max.subtract(min).multLocal(0.5f);
        //Nehon 08/18/2010 : Added an offset to the extend to avoid banding artifacts when the frustum are aligned
        return new BoundingBox(center, extent.x + 2.0f, extent.y + 2.0f, extent.z + 2.5f);
    }

    /**
     * Updates the shadow camera to properly contain the given
     * points (which contain the eye camera frustum corners)
     *
     * @param shadowCam
     * @param points
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

        Vector3f splitMin = splitBB.getMin(null);
        Vector3f splitMax = splitBB.getMax(null);

//        splitMin.z = 0;

        // Create the crop matrix.
        float scaleX, scaleY, scaleZ;
        float offsetX, offsetY, offsetZ;

        scaleX = 2.0f / (splitMax.x - splitMin.x);
        scaleY = 2.0f / (splitMax.y - splitMin.y);
        offsetX = -0.5f * (splitMax.x + splitMin.x) * scaleX;
        offsetY = -0.5f * (splitMax.y + splitMin.y) * scaleY;
        scaleZ = 1.0f / (splitMax.z - splitMin.z);
        offsetZ = -splitMin.z * scaleZ;

        Matrix4f cropMatrix = new Matrix4f(scaleX, 0f, 0f, offsetX,
                0f, scaleY, 0f, offsetY,
                0f, 0f, scaleZ, offsetZ,
                0f, 0f, 0f, 1f);


        Matrix4f result = new Matrix4f();
        result.set(cropMatrix);
        result.multLocal(projMatrix);

        shadowCam.setProjectionMatrix(result);
    }

    /**
     * Updates the shadow camera to properly contain the given
     * points (which contain the eye camera frustum corners) and the
     * shadow occluder objects.
     *
     * @param occluders
     * @param receivers
     * @param shadowCam
     * @param points
     */
    public static void updateShadowCamera(GeometryList occluders,
            GeometryList receivers,
            Camera shadowCam,
            Vector3f[] points) {
        updateShadowCamera(occluders, receivers, shadowCam, points, null);
    }

    /**
     * Updates the shadow camera to properly contain the given
     * points (which contain the eye camera frustum corners) and the
     * shadow occluder objects.
     * 
     * @param occluders
     * @param shadowCam
     * @param points
     */
    public static void updateShadowCamera(GeometryList occluders,
            GeometryList receivers,
            Camera shadowCam,
            Vector3f[] points,
            GeometryList splitOccluders) {

        boolean ortho = shadowCam.isParallelProjection();

        shadowCam.setProjectionMatrix(null);

        if (ortho) {
            shadowCam.setFrustum(-1, 1, -1, 1, 1, -1);
        } else {
            shadowCam.setFrustumPerspective(45, 1, 1, 150);
        }

        // create transform to rotate points to viewspace        
        Matrix4f viewProjMatrix = shadowCam.getViewProjectionMatrix();

        BoundingBox splitBB = computeBoundForPoints(points, viewProjMatrix);

        ArrayList<BoundingVolume> visRecvList = new ArrayList<BoundingVolume>();
        for (int i = 0; i < receivers.size(); i++) {
            // convert bounding box to light's viewproj space
            Geometry receiver = receivers.get(i);
            BoundingVolume bv = receiver.getWorldBound();
            BoundingVolume recvBox = bv.transform(viewProjMatrix, null);

            if (splitBB.intersects(recvBox)) {
                visRecvList.add(recvBox);
            }
        }

        ArrayList<BoundingVolume> visOccList = new ArrayList<BoundingVolume>();
        for (int i = 0; i < occluders.size(); i++) {
            // convert bounding box to light's viewproj space
            Geometry occluder = occluders.get(i);
            BoundingVolume bv = occluder.getWorldBound();
            BoundingVolume occBox = bv.transform(viewProjMatrix, null);

            boolean intersects = splitBB.intersects(occBox);
            if (!intersects && occBox instanceof BoundingBox) {
                BoundingBox occBB = (BoundingBox) occBox;
                //Kirill 01/10/2011
                // Extend the occluder further into the frustum
                // This fixes shadow dissapearing issues when
                // the caster itself is not in the view camera
                // but its shadow is in the camera
                //      The number is in world units
                occBB.setZExtent(occBB.getZExtent() + 50);
                occBB.setCenter(occBB.getCenter().addLocal(0, 0, 25));
                if (splitBB.intersects(occBB)) {
                    // To prevent extending the depth range too much
                    // We return the bound to its former shape
                    // Before adding it
                    occBB.setZExtent(occBB.getZExtent() - 50);
                    occBB.setCenter(occBB.getCenter().subtractLocal(0, 0, 25));
                    visOccList.add(occBox);
                    if (splitOccluders != null) {
                        splitOccluders.add(occluder);
                    }
                }
            } else if (intersects) {
                visOccList.add(occBox);
                if (splitOccluders != null) {
                    splitOccluders.add(occluder);
                }
            }
        }

        BoundingBox casterBB = computeUnionBound(visOccList);
        BoundingBox receiverBB = computeUnionBound(visRecvList);

        //Nehon 08/18/2010 this is to avoid shadow bleeding when the ground is set to only receive shadows
        if (visOccList.size() != visRecvList.size()) {
            casterBB.setXExtent(casterBB.getXExtent() + 2.0f);
            casterBB.setYExtent(casterBB.getYExtent() + 2.0f);
            casterBB.setZExtent(casterBB.getZExtent() + 2.0f);
        }

        Vector3f casterMin = casterBB.getMin(null);
        Vector3f casterMax = casterBB.getMax(null);

        Vector3f receiverMin = receiverBB.getMin(null);
        Vector3f receiverMax = receiverBB.getMax(null);

        Vector3f splitMin = splitBB.getMin(null);
        Vector3f splitMax = splitBB.getMax(null);

        splitMin.z = 0;

        if (!ortho) {
            shadowCam.setFrustumPerspective(45, 1, 1, splitMax.z);
        }

        Matrix4f projMatrix = shadowCam.getProjectionMatrix();

        Vector3f cropMin = new Vector3f();
        Vector3f cropMax = new Vector3f();

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

        scaleX = (2.0f) / (cropMax.x - cropMin.x);
        scaleY = (2.0f) / (cropMax.y - cropMin.y);

        offsetX = -0.5f * (cropMax.x + cropMin.x) * scaleX;
        offsetY = -0.5f * (cropMax.y + cropMin.y) * scaleY;

        scaleZ = 1.0f / (cropMax.z - cropMin.z);
        offsetZ = -cropMin.z * scaleZ;



        Matrix4f cropMatrix = new Matrix4f(scaleX, 0f, 0f, offsetX,
                0f, scaleY, 0f, offsetY,
                0f, 0f, scaleZ, offsetZ,
                0f, 0f, 0f, 1f);


        Matrix4f result = new Matrix4f();
        result.set(cropMatrix);
        result.multLocal(projMatrix);

        shadowCam.setProjectionMatrix(result);

    }
}
