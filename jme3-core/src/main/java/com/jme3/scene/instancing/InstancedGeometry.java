package com.jme3.scene.instancing;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.control.AbstractControl;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>InstancedGeometry</code> allows rendering many similar 
 * geometries efficiently through a feature called geometry 
 * instancing. 
 * 
 * <p>
 * All rendered geometries share material, mesh, and lod level 
 * but have different world transforms or possibly other parameters. 
 * The settings for all instances are inherited from this geometry's 
 * {@link #setMesh(com.jme3.scene.Mesh) mesh},
 * {@link #setMaterial(com.jme3.material.Material) material} and 
 * {@link #setLodLevel(int) lod level} and cannot be changed per-instance.
 * </p>
 * 
 * <p>
 * In order to receive any per-instance parameters, the material's shader
 * must be changed to retrieve per-instance data via 
 * {@link VertexBuffer#setInstanced(boolean) instanced vertex attributes} 
 * or uniform arrays indexed with the GLSL built-in uniform 
 * <code>gl_InstanceID</code>. At the very least, they should use the 
 * functions specified in <code>Instancing.glsllib</code> shader library
 * to transform vertex positions and normals instead of multiplying by the 
 * built-in matrix uniforms.
 * </p>
 * 
 * <p>
 * This class can operate in two modes, {@link InstancedGeometry.Mode#Auto}
 * and {@link InstancedGeometry.Mode#Manual}. See the respective enums
 * for more information</p>
 * 
 * <p>
 * Prior to usage, the maximum number of instances must be set via 
 * {@link #setMaxNumInstances(int) } and the current number of instances set
 * via {@link #setCurrentNumInstances(int) }. The user is then
 * expected to provide transforms for all instances up to the number
 * of current instances.
 * </p>
 * 
 * @author Kirill Vainer
 */
public class InstancedGeometry extends Geometry {
    
    /**
     * Indicates how the per-instance data is to be specified.
     */
    public static enum Mode {
        
        /**
         * The user must specify all per-instance transforms and 
         * parameters manually via
         * {@link InstancedGeometry#setGlobalUserInstanceData(com.jme3.scene.VertexBuffer[]) }
         * or 
         * {@link InstancedGeometry#setCameraUserInstanceData(com.jme3.renderer.Camera, com.jme3.scene.VertexBuffer) }.
         */
        Manual,
        
        /**
         * The user 
         * {@link InstancedGeometry#setInstanceTransform(int, com.jme3.math.Transform) provides world transforms}
         * and then uses the <code>Instancing.glsllib</code> transform functions in the 
         * shader to transform vertex attributes to the respective spaces.
         * Additional per-instance data can be specified via
         * {@link InstancedGeometry#setManualGlobalInstanceData(com.jme3.scene.VertexBuffer[]) }.
         * {@link #setManualCameraInstanceData(com.jme3.renderer.Camera, com.jme3.scene.VertexBuffer) }
         * cannot be used at this mode since it is computed automatically.
         */
        Auto
    }
    
    private static class InstancedGeometryControl extends AbstractControl {

        private InstancedGeometry geom;
        
        public InstancedGeometryControl() {
        }
        
        public InstancedGeometryControl(InstancedGeometry geom) {
            this.geom = geom;
        }
        
        @Override
        protected void controlUpdate(float tpf) {
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            geom.renderFromControl(vp.getCamera());
        }
    }
    
    private static final int INSTANCE_SIZE = 16;
    
    private InstancedGeometry.Mode mode;
    private InstancedGeometryControl control;
    private int currentNumInstances = 1;
    private Camera lastCamera = null;
    private Matrix4f[] worldMatrices = new Matrix4f[1];
    private VertexBuffer[] globalInstanceData;
    
    private final HashMap<Camera, VertexBuffer> instanceDataPerCam 
            = new HashMap<Camera, VertexBuffer>();
    
    // TODO: determine if perhaps its better to use TempVars here.
    
    private final Matrix4f tempMat4 = new Matrix4f();
    private final Matrix4f tempMat4_2 = new Matrix4f();
    private final Matrix3f tempMat3 = new Matrix3f();
    private final Quaternion tempQuat = new Quaternion();
    private final float[] tempFloatArray = new float[16];
    
    /**
     * Serialization only. Do not use.
     */
    public InstancedGeometry() {
        super();
        setIgnoreTransform(true);
    }
    
    /**
     * Creates instanced geometry with the specified mode and name.
     * 
     * @param mode The {@link Mode} at which the instanced geometry operates at.
     * @param name The name of the spatial. 
     * 
     * @see Mode
     * @see Spatial#Spatial(java.lang.String)
     */
    public InstancedGeometry(InstancedGeometry.Mode mode, String name) {
        super(name);
        this.mode = mode;
        setIgnoreTransform(true);
        if (mode == InstancedGeometry.Mode.Auto) {
            control = new InstancedGeometryControl(this);
            addControl(control);
        }
    }
    
    /**
     * The mode with which this instanced geometry was initialized
     * with. Cannot be changed after initialization.
     * 
     * @return instanced geometry mode.
     */
    public InstancedGeometry.Mode getMode() {
        return mode;
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
    }
    
    /**
     * Specify camera specific user per-instance data.
     * 
     * Only applies when operating in {@link Mode#Manual}. 
     * When operating in {@link Mode#Auto}, this data is computed automatically,
     * and using this method is not allowed.
     * 
     * @param camera The camera for which per-instance data is to be set.
     * @param cameraInstanceData The camera's per-instance data.
     * 
     * @throws IllegalArgumentException If camera is null.
     * @throws IllegalStateException If {@link #getMode() mode} is set to 
     * {@link Mode#Auto}.
     * 
     * @see Mode
     * @see #getCameraUserInstanceData(com.jme3.renderer.Camera)
     */
    public void setCameraUserInstanceData(Camera camera, VertexBuffer cameraInstanceData) {
        if (mode == Mode.Auto) {
            throw new IllegalStateException("Not allowed in auto mode");
        }
        if (camera == null) {
            throw new IllegalArgumentException("camera cannot be null");
        }
        instanceDataPerCam.put(camera, cameraInstanceData);
    }
    
    /**
     * Return camera specific user per-instance data.
     * 
     * Only applies when operating in {@link Mode#Manual}. 
     * When operating in {@link Mode#Auto}, this data is computed automatically,
     * and using this method is not allowed.
     * 
     * @param camera The camera to look up the per-instance data for.
     * @return The per-instance data, or <code>null</code> if none was specified
     * for the given camera.
     * 
     * @throws IllegalArgumentException If camera is null.
     * @throws IllegalStateException If {@link #getMode() mode} is set to 
     * {@link Mode#Auto}.
     * 
     * @see Mode
     * @see #setCameraUserInstanceData(com.jme3.renderer.Camera, com.jme3.scene.VertexBuffer) 
     */
    public VertexBuffer getCameraUserInstanceData(Camera camera) {
        if (mode == Mode.Auto) {
            throw new IllegalStateException("Not allowed in auto mode");
        }
        if (camera == null) {
            throw new IllegalArgumentException("camera cannot be null");
        }
        return instanceDataPerCam.get(camera);
    }
    
    /**
     * Return a read only map with the mappings between cameras and camera 
     * specific per-instance data. 
     * 
     * Only applies when operating in {@link Mode#Manual}. 
     * When operating in {@link Mode#Auto}, this data is computed automatically,
     * and using this method is not allowed.
     * 
     * @return read only map with the mappings between cameras and camera 
     * specific per-instance data. 
     * 
     * @throws IllegalStateException If {@link #getMode() mode} is set to 
     * {@link Mode#Auto}.
     * 
     * @see Mode
     * @see #setCameraUserInstanceData(com.jme3.renderer.Camera, com.jme3.scene.VertexBuffer) 
     */
    public Map<Camera, VertexBuffer> getAllCameraUserInstanceData() {
        if (mode == Mode.Auto) {
            throw new IllegalStateException("Not allowed in auto mode");
        }
        return Collections.unmodifiableMap(instanceDataPerCam);
    }
    
    private void updateInstance(Matrix4f viewMatrix, Matrix4f worldMatrix, float[] store, int offset) {
        viewMatrix.mult(worldMatrix, tempMat4);
        tempMat4.toRotationMatrix(tempMat3);
        tempMat3.invertLocal();
        
        // NOTE: No need to take the transpose in order to encode
        // into quaternion, the multiplication in the shader is vec * quat
        // apparently...
        tempQuat.fromRotationMatrix(tempMat3);
        
        // Column-major encoding. The "W" field in each of the encoded
        // vectors represents the quaternion.
        store[offset + 0] = tempMat4.m00;
        store[offset + 1] = tempMat4.m10;
        store[offset + 2] = tempMat4.m20;
        store[offset + 3] = tempQuat.getX();
        store[offset + 4] = tempMat4.m01;
        store[offset + 5] = tempMat4.m11;
        store[offset + 6] = tempMat4.m21;
        store[offset + 7] = tempQuat.getY();
        store[offset + 8] = tempMat4.m02;
        store[offset + 9] = tempMat4.m12;
        store[offset + 10] = tempMat4.m22;
        store[offset + 11] = tempQuat.getZ();
        store[offset + 12] = tempMat4.m03;
        store[offset + 13] = tempMat4.m13;
        store[offset + 14] = tempMat4.m23;
        store[offset + 15] = tempQuat.getW();
    }
    
    private void renderFromControl(Camera cam) {
        if (mode != Mode.Auto) {
            return;
        }
        
        // Get the instance data VBO for this camera.
        VertexBuffer instanceDataVB = instanceDataPerCam.get(cam);
        FloatBuffer instanceData;
        
        if (instanceDataVB == null) {
            // This is a new camera, create instance data VBO for it.
            instanceData = BufferUtils.createFloatBuffer(worldMatrices.length * INSTANCE_SIZE);
            instanceDataVB = new VertexBuffer(Type.InstanceData);
            instanceDataVB.setInstanced(true);
            instanceDataVB.setupData(Usage.Stream, INSTANCE_SIZE, Format.Float, instanceData);
            instanceDataPerCam.put(cam, instanceDataVB);
        } else {
            // Retrieve the current instance data buffer.
            instanceData = (FloatBuffer) instanceDataVB.getData();
        }
        
        Matrix4f viewMatrix = cam.getViewMatrix();
        
        instanceData.limit(instanceData.capacity());
        instanceData.position(0);
        
        assert currentNumInstances <= worldMatrices.length;
        
        for (int i = 0; i < currentNumInstances; i++) {
            Matrix4f worldMatrix = worldMatrices[i];
            if (worldMatrix == null) {
                worldMatrix = Matrix4f.IDENTITY;
            }
            updateInstance(viewMatrix, worldMatrix, tempFloatArray, 0);
            instanceData.put(tempFloatArray);
        }
        
        instanceData.flip();
        
        this.lastCamera = cam;
        instanceDataVB.updateData(instanceDataVB.getData());
    }
    
    /**
     * Set the current number of instances to be rendered.
     * 
     * @param currentNumInstances the current number of instances to be rendered.
     * 
     * @throws IllegalArgumentException If current number of instances is 
     * greater than the maximum number of instances.
     */
    public void setCurrentNumInstances(int currentNumInstances) {
        if (currentNumInstances > worldMatrices.length) {
            throw new IllegalArgumentException("currentNumInstances cannot be larger than maxNumInstances");
        }
        this.currentNumInstances = currentNumInstances;
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
    public void setMaxNumInstances(int maxNumInstances) {
        if (mode == Mode.Manual) {
            throw new IllegalStateException("Not allowed in manual mode");
        }
        if (maxNumInstances < 1) {
            throw new IllegalArgumentException("maxNumInstances must be 1 or higher");
        }
        
        this.worldMatrices = new Matrix4f[maxNumInstances];
        
        if (currentNumInstances > maxNumInstances) {
            currentNumInstances = maxNumInstances;
        }
        
        // Resize instance data for each of the cameras.
        for (VertexBuffer instanceDataVB : instanceDataPerCam.values()) {
            FloatBuffer instanceData = (FloatBuffer) instanceDataVB.getData();
            if (instanceData.capacity() / INSTANCE_SIZE != worldMatrices.length) {
                // Delete old data.
                BufferUtils.destroyDirectBuffer(instanceData);

                // Resize instance data for this camera.
                // Create new data with new length.
                instanceData = BufferUtils.createFloatBuffer(worldMatrices.length * INSTANCE_SIZE);
                instanceDataVB.updateData(instanceData);
            }
        }
    }
    
    public int getMaxNumInstances() {
        return worldMatrices.length;
    }

    public int getCurrentNumInstances() {
        return currentNumInstances;
    }
    
    public void setInstanceTransform(int instanceIndex, Matrix4f worldTransform) {
        if (mode == Mode.Manual) {
            throw new IllegalStateException("Not allowed in manual mode");
        }
        if (worldTransform == null) {
            throw new IllegalArgumentException("worldTransform cannot be null");
        }
        if (instanceIndex < 0) {
            throw new IllegalArgumentException("instanceIndex cannot be smaller than zero");
        }
        if (instanceIndex >= currentNumInstances) {
            throw new IllegalArgumentException("instanceIndex cannot be larger than currentNumInstances");
        }
        // TODO: Determine if need to make a copy of matrix or just doing this
        // is fine.
        worldMatrices[instanceIndex] = worldTransform;
    }
    
    public void setInstanceTransform(int instanceIndex, Transform worldTransform) {
        if (worldTransform == null) {
            throw new IllegalArgumentException("worldTransform cannot be null");
        }
        
        // Compute the world transform matrix.
        tempMat4.loadIdentity();
        tempMat4.setRotationQuaternion(worldTransform.getRotation());
        tempMat4.setTranslation(worldTransform.getTranslation());
        tempMat4_2.loadIdentity();
        tempMat4_2.scale(worldTransform.getScale());
        tempMat4.multLocal(tempMat4_2);
        
        setInstanceTransform(instanceIndex, tempMat4.clone());
    }
    
    public VertexBuffer[] getAllInstanceData() {
        VertexBuffer instanceDataForCam = instanceDataPerCam.get(lastCamera);
        ArrayList<VertexBuffer> allData = new ArrayList();

        if (instanceDataForCam != null) {
            allData.add(instanceDataForCam);
        }
        if (globalInstanceData != null) {
            allData.addAll(Arrays.asList(globalInstanceData));
        }

        return allData.toArray(new VertexBuffer[allData.size()]);
    }

    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule capsule = exporter.getCapsule(this);
        capsule.write(currentNumInstances, "cur_num_instances", 1);
        capsule.write(mode, "instancing_mode", InstancedGeometry.Mode.Auto);
        if (mode == Mode.Auto) {
            capsule.write(worldMatrices, "world_matrices", null);
        }
    }
    
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);
        currentNumInstances = capsule.readInt("cur_num_instances", 1);
        mode = capsule.readEnum("instancing_mode", InstancedGeometry.Mode.class, 
                                InstancedGeometry.Mode.Auto);
        
        if (mode == Mode.Auto) {
            Savable[] matrixSavables = capsule.readSavableArray("world_matrices", null);
            worldMatrices = new Matrix4f[matrixSavables.length];
            for (int i = 0; i < worldMatrices.length; i++) {
                worldMatrices[i] = (Matrix4f) matrixSavables[i];
            }

            control = getControl(InstancedGeometryControl.class);
            control.geom = this;
        }
    }
}
