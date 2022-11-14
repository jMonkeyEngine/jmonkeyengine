/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
package com.jme3.scene.instancing;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Camera.FrustumIntersect;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiFunction;

public class InstancedGeometry extends Geometry {

    private static final int INSTANCE_SIZE = 16;

    private static BiFunction<Camera, Geometry, Boolean> instanceCullingFunction = new DefaultInstanceCullingFunction();

    private VertexBuffer[] globalInstanceData;
    private VertexBuffer transformInstanceData;
    private Geometry[] geometries = new Geometry[1];
    // Keep track of both transformInstanceData and globalInstanceData
    // that is used by renderer.
    private VertexBuffer[] allInstanceData;

    private int firstUnusedIndex = 0;
    private int numVisibleInstances = 0;
    private Camera cam;

    public InstancedGeometry() {
        super();
        setIgnoreTransform(true);
        setBatchHint(BatchHint.Never);
        setMaxNumInstances(1);
    }

    /**
     * Creates instanced geometry with the specified mode and name.
     *
     * @param name The name of the spatial.
     *
     * @see Spatial#Spatial(java.lang.String)
     */
    public InstancedGeometry(String name) {
        super(name);
        setIgnoreTransform(true);
        setBatchHint(BatchHint.Never);
        setMaxNumInstances(1);
    }

    /**
     * Set the function used for culling instances from being rendered.
     * Default is {@link DefaultInstanceCullingFunction}.
     */
    public static void setInstanceCullingFunction(BiFunction<Camera, Geometry, Boolean> instanceCullingFunction) {
        InstancedGeometry.instanceCullingFunction = instanceCullingFunction;
    }

    /**
     * @return The instance culling function or null if there isn't any.
     * Default is {@link DefaultInstanceCullingFunction}.
     */
    public static BiFunction<Camera, Geometry, Boolean> getInstanceCullingFunction() {
        return instanceCullingFunction;
    }

    /**
     * Global user specified per-instance data.
     *
     * By default set to <code>null</code>, specify an array of VertexBuffers
     * via {@link #setGlobalUserInstanceData(com.jme3.scene.VertexBuffer[]) }.
     *
     * @return global user specified per-instance data.
     * @see #setGlobalUserInstanceData(com.jme3.scene.VertexBuffer[])
     */
    public VertexBuffer[] getGlobalUserInstanceData() {
        return globalInstanceData;
    }

    /**
     * Specify global user per-instance data.
     *
     * By default set to <code>null</code>, specify an array of VertexBuffers
     * that contain per-instance vertex attributes.
     *
     * @param globalInstanceData global user per-instance data.
     *
     * @throws IllegalArgumentException If one of the VertexBuffers is not
     * {@link VertexBuffer#setInstanced(boolean) instanced}.
     */
    public void setGlobalUserInstanceData(VertexBuffer[] globalInstanceData) {
        this.globalInstanceData = globalInstanceData;
        updateAllInstanceData();
    }

    /**
     * Specify camera specific user per-instance data.
     *
     * @param transformInstanceData The transforms for each instance.
     */
    public void setTransformUserInstanceData(VertexBuffer transformInstanceData) {
        this.transformInstanceData = transformInstanceData;
        updateAllInstanceData();
    }

    /**
     * Return user per-instance transform data.
     *
     * @return The per-instance transform data.
     *
     * @see #setTransformUserInstanceData(com.jme3.scene.VertexBuffer)
     */
    public VertexBuffer getTransformUserInstanceData() {
        return transformInstanceData;
    }

    private void updateInstance(Matrix4f worldMatrix, float[] store,
                                int offset, Matrix3f tempMat3,
                                Quaternion tempQuat) {
        worldMatrix.toRotationMatrix(tempMat3);
        tempMat3.invertLocal();

        // NOTE: No need to take the transpose in order to encode
        // into quaternion, the multiplication in the shader is vec * quat
        // apparently...
        tempQuat.fromRotationMatrix(tempMat3);

        // Column-major encoding. The "W" field in each of the encoded
        // vectors represents the quaternion.
        store[offset + 0] = worldMatrix.m00;
        store[offset + 1] = worldMatrix.m10;
        store[offset + 2] = worldMatrix.m20;
        store[offset + 3] = tempQuat.getX();
        store[offset + 4] = worldMatrix.m01;
        store[offset + 5] = worldMatrix.m11;
        store[offset + 6] = worldMatrix.m21;
        store[offset + 7] = tempQuat.getY();
        store[offset + 8] = worldMatrix.m02;
        store[offset + 9] = worldMatrix.m12;
        store[offset + 10] = worldMatrix.m22;
        store[offset + 11] = tempQuat.getZ();
        store[offset + 12] = worldMatrix.m03;
        store[offset + 13] = worldMatrix.m13;
        store[offset + 14] = worldMatrix.m23;
        store[offset + 15] = tempQuat.getW();
    }

    /**
     * Set the maximum amount of instances that can be rendered by this
     * instanced geometry when mode is set to auto.
     *
     * This re-allocates internal structures and therefore should be called
     * only when necessary.
     *
     * @param maxNumInstances The maximum number of instances that can be
     * rendered.
     *
     * @throws IllegalStateException If mode is set to manual.
     * @throws IllegalArgumentException If maxNumInstances is zero or negative
     */
    public final void setMaxNumInstances(int maxNumInstances) {
        if (maxNumInstances < 1) {
            throw new IllegalArgumentException("maxNumInstances must be 1 or higher");
        }

        Geometry[] originalGeometries = geometries;
        this.geometries = new Geometry[maxNumInstances];

        if (originalGeometries != null) {
            System.arraycopy(originalGeometries, 0, geometries, 0, originalGeometries.length);
        }

        // Resize instance data.
        if (transformInstanceData != null) {
            BufferUtils.destroyDirectBuffer(transformInstanceData.getData());
            transformInstanceData.updateData(BufferUtils.createFloatBuffer(geometries.length * INSTANCE_SIZE));
        } else if (transformInstanceData == null) {
            transformInstanceData = new VertexBuffer(Type.InstanceData);
            transformInstanceData.setInstanced(true);
            transformInstanceData.setupData(Usage.Stream,
                    INSTANCE_SIZE,
                    Format.Float,
                    BufferUtils.createFloatBuffer(geometries.length * INSTANCE_SIZE));
            updateAllInstanceData();
        }
    }

    public int getMaxNumInstances() {
        return geometries.length;
    }

    /**
     * @return The number of instances are visible by camera.
     */
    public int getNumVisibleInstances() {
        return numVisibleInstances;
    }

    /**
     * @return The number of instances are in this {@link InstancedGeometry}
     */
    public int getNumInstances() {
        int count = 0;
        for (int i = 0; i < geometries.length; i++) {
            if (geometries[i] != null) {
                count++;
            }
        }
        return count;
    }

    public boolean isEmpty() {
        return getNumInstances() == 0;
    }

    private void swap(int idx1, int idx2) {
        Geometry g = geometries[idx1];
        geometries[idx1] = geometries[idx2];
        geometries[idx2] = g;

        if (geometries[idx1] != null) {
            InstancedNode.setGeometryStartIndex2(geometries[idx1], idx1);
        }
        if (geometries[idx2] != null) {
            InstancedNode.setGeometryStartIndex2(geometries[idx2], idx2);
        }
    }

    public void updateInstances() {
        FloatBuffer fb = (FloatBuffer) transformInstanceData.getData();
        fb.limit(fb.capacity());
        fb.position(0);

        int numCulledGeometries = 0;
        TempVars vars = TempVars.get();
        {
            float[] temp = vars.matrixWrite;

            for (int i = 0; i < firstUnusedIndex; i++) {
                Geometry geom = geometries[i];

                if (geom == null) {
                    geom = geometries[firstUnusedIndex - 1];

                    if (geom == null) {
                        throw new AssertionError();
                    }

                    swap(i, firstUnusedIndex - 1);

                    while (geometries[firstUnusedIndex -1] == null) {
                        firstUnusedIndex--;
                    }
                }

                if (cam != null && instanceCullingFunction != null) {
                    boolean culled = instanceCullingFunction.apply(cam, geom);
                    if (culled) {
                        numCulledGeometries++;
                        continue;
                    }
                }

                Matrix4f worldMatrix = geom.getWorldMatrix();
                updateInstance(worldMatrix, temp, 0, vars.tempMat3, vars.quat1);
                fb.put(temp);
            }
        }
        vars.release();

        fb.flip();

        numVisibleInstances = firstUnusedIndex - numCulledGeometries;
        if (fb.limit() / INSTANCE_SIZE != numVisibleInstances) {
            throw new AssertionError();
        }

        transformInstanceData.updateData(fb);
    }

    public void deleteInstance(Geometry geom) {
        int idx = InstancedNode.getGeometryStartIndex2(geom);
        InstancedNode.setGeometryStartIndex2(geom, -1);

        geometries[idx] = null;

        if (idx == firstUnusedIndex - 1) {
            // Deleting the last element.
            // Move index back.
            firstUnusedIndex--;
            while (geometries[firstUnusedIndex] == null) {
                firstUnusedIndex--;
                if (firstUnusedIndex < 0) {
                    break;
                }
            }
            firstUnusedIndex++;
        } else {
            // Deleting element in the middle
        }
        setBoundRefresh();
    }

    public void addInstance(Geometry geometry) {
        if (geometry == null) {
            throw new IllegalArgumentException("geometry cannot be null");
        }

        // Take an index from the end.
        if (firstUnusedIndex + 1 >= geometries.length) {
            // No more room.
            setMaxNumInstances(getMaxNumInstances() * 2);
        }

        int freeIndex = firstUnusedIndex;
        firstUnusedIndex++;

        geometries[freeIndex] = geometry;
        InstancedNode.setGeometryStartIndex2(geometry, freeIndex);
        setBoundRefresh();
    }

    @Override
    protected void updateWorldBound() {
        refreshFlags &= ~RF_BOUND;
        BoundingVolume resultBound = null;

        for (int i = 0; i < firstUnusedIndex; i++) {
            Geometry geom = geometries[i];

            if (geom != null) {
                if (resultBound != null) {
                    // merge current world bound with child world bound
                    resultBound.mergeLocal(geom.getWorldBound());
                } else {
                    // set world bound to first non-null child world bound
                    if (geom.getWorldBound() != null) {
                        resultBound = geom.getWorldBound().clone(this.worldBound);
                    }
                }
            }
        }

        if (resultBound == null) {
            resultBound = new BoundingBox(getWorldTranslation(), 0f, 0f, 0f);
        }
        this.worldBound = resultBound;
    }

    public Geometry[] getGeometries() {
        return geometries;
    }

    public VertexBuffer[] getAllInstanceData() {
        return allInstanceData;
    }

    private void updateAllInstanceData() {
        ArrayList<VertexBuffer> allData = new ArrayList<>();
        if (transformInstanceData != null) {
            allData.add(transformInstanceData);
        }
        if (globalInstanceData != null) {
            allData.addAll(Arrays.asList(globalInstanceData));
        }
        allInstanceData = allData.toArray(new VertexBuffer[allData.size()]);
    }

    @Override
    public boolean checkCulling(Camera cam) {
        this.cam = cam;
        return super.checkCulling(cam);
    }

    @Override
    public int collideWith(Collidable other, CollisionResults results) {
        return 0; // Ignore collision
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        this.globalInstanceData = cloner.clone(globalInstanceData);
        this.transformInstanceData = cloner.clone(transformInstanceData);
        this.allInstanceData = cloner.clone(allInstanceData);
        this.geometries = cloner.clone(geometries);
    }

    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule capsule = exporter.getCapsule(this);
        //capsule.write(currentNumInstances, "cur_num_instances", 1);
        capsule.write(geometries, "geometries", null);
    }

    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);
        //currentNumInstances = capsule.readInt("cur_num_instances", 1);

        Savable[] geometrySavables = capsule.readSavableArray("geometries", null);
        geometries = new Geometry[geometrySavables.length];
        for (int i = 0; i < geometrySavables.length; i++) {
            geometries[i] = (Geometry) geometrySavables[i];
        }

        updateAllInstanceData();
    }

    /**
     *  Destroy internal buffers.
     */
    protected void cleanup() {
        BufferUtils.destroyDirectBuffer(transformInstanceData.getData());
        transformInstanceData = null;
        allInstanceData = null;
        geometries = null;
    }

    /**
     * By default, it checks if geometry is in camera frustum and culls it
     * if it is outside camera view.
     */
    public static class DefaultInstanceCullingFunction implements BiFunction<Camera, Geometry, Boolean> {

        @Override
        public Boolean apply(Camera cam, Geometry geom) {
            BoundingVolume bv = geom.getWorldBound();
            int save = cam.getPlaneState();
            cam.setPlaneState(0);
            FrustumIntersect intersect = cam.contains(bv);
            cam.setPlaneState(save);

            return intersect == FrustumIntersect.Outside;
        }
    }
}
