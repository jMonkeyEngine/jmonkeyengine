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
 * @see <ul> <li><a
 * href="http://appsrv.cse.cuhk.edu.hk/~fzhang/pssm_vrcia/">http://appsrv.cse.cuhk.edu.hk/~fzhang/pssm_vrcia/</a></li>
 * <li><a
 * href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a></li>
 * </ul> for more info.
 */
public class ShadowUtil {

    /**
     * Updates a points arrays with the frustum corners of the provided camera.
     *
     * @param viewCam
     * @param points
     */
    public static void updateFrustumPoints2(Camera viewCam, Vector3f[] points) {
        int w = viewCam.getWidth();
        int h = viewCam.getHeight();

        points[0].set(viewCam.getWorldCoordinates(new Vector2f(0, 0), 0));
        points[1].set(viewCam.getWorldCoordinates(new Vector2f(0, h), 0));
        points[2].set(viewCam.getWorldCoordinates(new Vector2f(w, h), 0));
        points[3].set(viewCam.getWorldCoordinates(new Vector2f(w, 0), 0));

        points[4].set(viewCam.getWorldCoordinates(new Vector2f(0, 0), 1));
        points[5].set(viewCam.getWorldCoordinates(new Vector2f(0, h), 1));
        points[6].set(viewCam.getWorldCoordinates(new Vector2f(w, h), 1));
        points[7].set(viewCam.getWorldCoordinates(new Vector2f(w, 0), 1));
    }

    /**
     * Updates the points array to contain the frustum corners of the given
     * camera. The nearOverride and farOverride variables can be used to
     * override the camera's near/far values with own values.
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
        TempVars tempv = TempVars.get();
        for (int i = 0; i < list.size(); i++) {
            BoundingVolume vol = list.get(i).getWorldBound();
            BoundingVolume newVol = vol.transform(transform, tempv.bbox);
            //Nehon : prevent NaN and infinity values to screw the final bounding box
            if (!Float.isNaN(newVol.getCenter().x) && !Float.isInfinite(newVol.getCenter().x)) {
                bbox.mergeLocal(newVol);
            }
        }
        tempv.release();
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
        TempVars tempv = TempVars.get();
        for (int i = 0; i < list.size(); i++) {
            BoundingVolume vol = list.get(i).getWorldBound();
            BoundingVolume store = vol.transform(mat, tempv.bbox);
            //Nehon : prevent NaN and infinity values to screw the final bounding box
            if (!Float.isNaN(store.getCenter().x) && !Float.isInfinite(store.getCenter().x)) {
                bbox.mergeLocal(store);
            }
        }
        tempv.release();
        return bbox;
    }

    /**
     * Computes the bounds of multiple bounding volumes
     *
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
     *
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
        TempVars vars = TempVars.get();
        Vector3f temp = vars.vect1;

        for (int i = 0; i < pts.length; i++) {
            float w = mat.multProj(pts[i], temp);

            temp.x /= w;
            temp.y /= w;
            // Why was this commented out?
            temp.z /= w;

            min.minLocal(temp);
            max.maxLocal(temp);
        }
        vars.release();
        Vector3f center = min.add(max).multLocal(0.5f);
        Vector3f extent = max.subtract(min).multLocal(0.5f);
        //Nehon 08/18/2010 : Added an offset to the extend to avoid banding artifacts when the frustum are aligned
        return new BoundingBox(center, extent.x + 2.0f, extent.y + 2.0f, extent.z + 2.5f);
    }

    /**
     * Updates the shadow camera to properly contain the given points (which
     * contain the eye camera frustum corners)
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

        TempVars vars = TempVars.get();

        Vector3f splitMin = splitBB.getMin(vars.vect1);
        Vector3f splitMax = splitBB.getMax(vars.vect2);

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

        Matrix4f cropMatrix = vars.tempMat4;
        cropMatrix.set(scaleX, 0f, 0f, offsetX,
                0f, scaleY, 0f, offsetY,
                0f, 0f, scaleZ, offsetZ,
                0f, 0f, 0f, 1f);


        Matrix4f result = new Matrix4f();
        result.set(cropMatrix);
        result.multLocal(projMatrix);

        vars.release();
        shadowCam.setProjectionMatrix(result);
    }

    /**
     * OccludersExtractor is a helper class to collect splitOccluders from scene recursively.
     * It utilizes the scene hierarchy, instead of making the huge flat geometries list first.
     * Instead of adding all geometries from scene to the RenderQueue.shadowCast and checking
     * all of them one by one against camera frustum the whole Node is checked first
     * to hopefully avoid the check on its children.
     */
    public static class OccludersExtractor
    {
        // global variables set in order not to have recursive process method with too many parameters
        Matrix4f viewProjMatrix;
        public Integer casterCount;
        BoundingBox splitBB, casterBB;
        GeometryList splitOccluders;
        TempVars vars;
        
        public OccludersExtractor() {}
        
        // initialize the global OccludersExtractor variables
        public OccludersExtractor(Matrix4f vpm, int cc, BoundingBox sBB, BoundingBox cBB, GeometryList sOCC, TempVars v) {
            viewProjMatrix = vpm; 
            casterCount = cc;
            splitBB = sBB;
            casterBB = cBB;
            splitOccluders = sOCC;
            vars = v;
        }

        /**
         * Check the rootScene against camera frustum and if intersects process it recursively.
         * The global OccludersExtractor variables need to be initialized first.
         * Variables are updated and used in {@link ShadowUtil#updateShadowCamera} at last.
         */
        public int addOccluders(Spatial scene) {
            if ( scene != null ) process(scene);
            return casterCount;
        }
        
        private void process(Spatial scene) {
            if (scene.getCullHint() == Spatial.CullHint.Always) return;

            RenderQueue.ShadowMode shadowMode = scene.getShadowMode();
            if ( scene instanceof Geometry )
            {
                // convert bounding box to light's viewproj space
                Geometry occluder = (Geometry)scene;
                if (shadowMode != RenderQueue.ShadowMode.Off && shadowMode != RenderQueue.ShadowMode.Receive
                        && !occluder.isGrouped() && occluder.getWorldBound()!=null) {
                    BoundingVolume bv = occluder.getWorldBound();
                    BoundingVolume occBox = bv.transform(viewProjMatrix, vars.bbox);
          
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
                            //Nehon : prevent NaN and infinity values to screw the final bounding box
                            if (!Float.isNaN(occBox.getCenter().x) && !Float.isInfinite(occBox.getCenter().x)) {
                                // To prevent extending the depth range too much
                                // We return the bound to its former shape
                                // Before adding it
                                occBB.setZExtent(occBB.getZExtent() - 50);
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
            }
            else if ( scene instanceof Node && ((Node)scene).getWorldBound()!=null )
            {
                Node nodeOcc = (Node)scene;
                boolean intersects = false;
                // some 
                BoundingVolume bv = nodeOcc.getWorldBound();
                BoundingVolume occBox = bv.transform(viewProjMatrix, vars.bbox);
      
                intersects = splitBB.intersects(occBox);
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
                    intersects = splitBB.intersects(occBB);
                }
 
                if ( intersects ) {
                    for (Spatial child : ((Node)scene).getChildren()) {
                        process(child);
                    }
                }
            }
        }
    }
    
    /**
     * Updates the shadow camera to properly contain the given points (which
     * contain the eye camera frustum corners) and the shadow occluder objects
     * collected through the traverse of the scene hierarchy
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
        
        int casterCount = 0, receiverCount = 0;
        
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
        OccludersExtractor occExt = new OccludersExtractor(viewProjMatrix, casterCount, splitBB, casterBB, splitOccluders, vars);
        for (Spatial scene : viewPort.getScenes()) {
            occExt.addOccluders(scene);
        }
        casterCount = occExt.casterCount;
  
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

        scaleX = (2.0f) / (cropMax.x - cropMin.x);
        scaleY = (2.0f) / (cropMax.y - cropMin.y);

        //Shadow map stabilization approximation from shaderX 7
        //from Practical Cascaded Shadow maps adapted to PSSM
        //scale stabilization
        float halfTextureSize = shadowMapSize * 0.5f;

        if (halfTextureSize != 0 && scaleX >0 && scaleY>0) {
            float scaleQuantizer = 0.1f;            
            scaleX = 1.0f / FastMath.ceil(1.0f / scaleX * scaleQuantizer) * scaleQuantizer;
            scaleY = 1.0f / FastMath.ceil(1.0f / scaleY * scaleQuantizer) * scaleQuantizer;
        }

        offsetX = -0.5f * (cropMax.x + cropMin.x) * scaleX;
        offsetY = -0.5f * (cropMax.y + cropMin.y) * scaleY;


        //Shadow map stabilization approximation from shaderX 7
        //from Practical Cascaded Shadow maps adapted to PSSM
        //offset stabilization
        if (halfTextureSize != 0  && scaleX >0 && scaleY>0) {
            offsetX = FastMath.ceil(offsetX * halfTextureSize) / halfTextureSize;
            offsetY = FastMath.ceil(offsetY * halfTextureSize) / halfTextureSize;
        }

        scaleZ = 1.0f / (cropMax.z - cropMin.z);
        offsetZ = -cropMin.z * scaleZ;




        Matrix4f cropMatrix = vars.tempMat4;
        cropMatrix.set(scaleX, 0f, 0f, offsetX,
                0f, scaleY, 0f, offsetY,
                0f, 0f, scaleZ, offsetZ,
                0f, 0f, 0f, 1f);


        Matrix4f result = new Matrix4f();
        result.set(cropMatrix);
        result.multLocal(projMatrix);
        vars.release();

        shadowCam.setProjectionMatrix(result);
    }
    
    /**
     * Populates the outputGeometryList with the geometry of the
     * inputGeomtryList that are in the frustum of the given camera
     *
     * @param inputGeometryList The list containing all geometry to check
     * against the camera frustum
     * @param camera the camera to check geometries against
     * @param outputGeometryList the list of all geometries that are in the
     * camera frustum
     */
    public static void getGeometriesInCamFrustum(GeometryList inputGeometryList,
            Camera camera,
            GeometryList outputGeometryList) {
        for (int i = 0; i < inputGeometryList.size(); i++) {
            Geometry g = inputGeometryList.get(i);
            int planeState = camera.getPlaneState();
            camera.setPlaneState(0);
            if (camera.contains(g.getWorldBound()) != Camera.FrustumIntersect.Outside) {
                outputGeometryList.add(g);
            }
            camera.setPlaneState(planeState);
        }

    }

    /**
     * Populates the outputGeometryList with the rootScene children geometries
     * that are in the frustum of the given camera
     *
     * @param rootScene the rootNode of the scene to traverse
     * @param camera the camera to check geometries against
     * @param outputGeometryList the list of all geometries that are in the
     * camera frustum
     */    
    public static void getGeometriesInCamFrustum(Spatial rootScene, Camera camera, RenderQueue.ShadowMode mode, GeometryList outputGeometryList) {
        if (rootScene != null && rootScene instanceof Node) {
            int planeState = camera.getPlaneState();
            addGeometriesInCamFrustumFromNode(camera, (Node)rootScene, mode, outputGeometryList);
            camera.setPlaneState(planeState);
        }
    }
    
    /**
     * Helper function to distinguish between Occluders and Receivers
     * 
     * @param shadowMode the ShadowMode tested
     * @param desired the desired ShadowMode 
     * @return true if tested ShadowMode matches the desired one
     */
    static private boolean checkShadowMode(RenderQueue.ShadowMode shadowMode, RenderQueue.ShadowMode desired)
    {
        if (shadowMode != RenderQueue.ShadowMode.Off)
        {
            switch (desired) {
                case Cast : 
                    return shadowMode==RenderQueue.ShadowMode.Cast || shadowMode==RenderQueue.ShadowMode.CastAndReceive;
                case Receive: 
                    return shadowMode==RenderQueue.ShadowMode.Receive || shadowMode==RenderQueue.ShadowMode.CastAndReceive;
                case CastAndReceive:
                    return true;
            }
        }
        return false;
    }
    
    /**
     * Helper function used to recursively populate the outputGeometryList 
     * with geometry children of scene node
     * 
     * @param camera
     * @param scene
     * @param outputGeometryList 
     */
    private static void addGeometriesInCamFrustumFromNode(Camera camera, Node scene, RenderQueue.ShadowMode mode, GeometryList outputGeometryList) {
        if (scene.getCullHint() == Spatial.CullHint.Always) return;
        camera.setPlaneState(0);
        if (camera.contains(scene.getWorldBound()) != Camera.FrustumIntersect.Outside) {
            for (Spatial child: scene.getChildren()) {
                if (child instanceof Node) addGeometriesInCamFrustumFromNode(camera, (Node)child, mode, outputGeometryList);
                else if (child instanceof Geometry && child.getCullHint() != Spatial.CullHint.Always) {
                    camera.setPlaneState(0);
                    if (checkShadowMode(child.getShadowMode(), mode) &&
                            !((Geometry)child).isGrouped() &&
                            camera.contains(child.getWorldBound()) != Camera.FrustumIntersect.Outside) {
                      outputGeometryList.add((Geometry)child);
                    }
                }
            }
        }
    }
    
    /**
     * Populates the outputGeometryList with the geometry of the
     * inputGeomtryList that are in the radius of a light.
     * The array of camera must be an array of 6 cameras initialized so they represent the light viewspace of a pointlight
     *
     * @param inputGeometryList The list containing all geometry to check
     * against the camera frustum
     * @param cameras the camera array to check geometries against
     * @param outputGeometryList the list of all geometries that are in the
     * camera frustum
     */
    public static void getGeometriesInLightRadius(GeometryList inputGeometryList,
            Camera[] cameras,
            GeometryList outputGeometryList) {
        for (int i = 0; i < inputGeometryList.size(); i++) {
            Geometry g = inputGeometryList.get(i);
            boolean inFrustum = false;
            for (int j = 0; j < cameras.length && inFrustum == false; j++) {
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
     * Populates the outputGeometryList with the geometries of the children 
     * of OccludersExtractor.rootScene node that are both in the frustum of the given vpCamera and some camera inside cameras array.
     * The array of cameras must be initialized to represent the light viewspace of some light like pointLight or spotLight
     *
     * @param camera the viewPort camera 
     * @param cameras the camera array to check geometries against, representing the light viewspace
     * @param outputGeometryList the output list of all geometries that are in the camera frustum
     */
    public static void getLitGeometriesInViewPort(Spatial rootScene, Camera vpCamera, Camera[] cameras, RenderQueue.ShadowMode mode, GeometryList outputGeometryList) {
        if (rootScene != null && rootScene instanceof Node) {
            addGeometriesInCamFrustumAndViewPortFromNode(vpCamera, cameras, (Node)rootScene, mode, outputGeometryList);
        }
    }
    /**
     * Helper function to recursively collect the geometries for getLitGeometriesInViewPort function.
     * 
     * @param vpCamera the viewPort camera 
     * @param cameras the camera array to check geometries against, representing the light viewspace
     * @param scene the Node to traverse or geometry to possibly add
     * @param outputGeometryList the output list of all geometries that are in the camera frustum
     */
    private static void addGeometriesInCamFrustumAndViewPortFromNode(Camera vpCamera, Camera[] cameras, Spatial scene, RenderQueue.ShadowMode mode, GeometryList outputGeometryList) {
        if (scene.getCullHint() == Spatial.CullHint.Always) return;

        boolean inFrustum = false;
        for (int j = 0; j < cameras.length && inFrustum == false; j++) {
            Camera camera = cameras[j];
            int planeState = camera.getPlaneState();
            camera.setPlaneState(0);
            inFrustum = camera.contains(scene.getWorldBound()) != Camera.FrustumIntersect.Outside && scene.checkCulling(vpCamera);
            camera.setPlaneState(planeState);
        }
        if (inFrustum) {
            if (scene instanceof Node)
            {
                Node node = (Node)scene;
                for (Spatial child: node.getChildren()) {
                    addGeometriesInCamFrustumAndViewPortFromNode(vpCamera, cameras, child, mode, outputGeometryList);
                }
            }
            else if (scene instanceof Geometry) {
                if (checkShadowMode(scene.getShadowMode(), mode) && !((Geometry)scene).isGrouped() ) {
                    outputGeometryList.add((Geometry)scene);
                }
            }
        }
    }

}
