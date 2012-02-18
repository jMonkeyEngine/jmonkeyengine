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
package com.jme3.scene;

import com.jme3.asset.AssetNotFoundException;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>Geometry</code> defines a leaf node of the scene graph. The leaf node
 * contains the geometric data for rendering objects. It manages all rendering
 * information such as a {@link Material} object to define how the surface
 * should be shaded and the {@link Mesh} data to contain the actual geometry.
 * 
 * @author Kirill Vainer
 */
public class Geometry extends Spatial {

    // Version #1: removed shared meshes. 
    // models loaded with shared mesh will be automatically fixed.
    public static final int SAVABLE_VERSION = 1;
    
    private static final Logger logger = Logger.getLogger(Geometry.class.getName());
    protected Mesh mesh;
    protected transient int lodLevel = 0;
    protected Material material;
    /**
     * When true, the geometry's transform will not be applied.
     */
    protected boolean ignoreTransform = false;
    protected transient Matrix4f cachedWorldMat = new Matrix4f();
    /**
     * used when geometry is batched
     */
    protected BatchNode batchNode = null;
    /**
     * the start index of this geom's mesh in the batchNode mesh
     */
    protected int startIndex;
    /**
     * the previous transforms of the geometry used to compute world transforms
     */
    protected Transform prevBatchTransforms = null;
    /**
     * the cached offset matrix used when the geometry is batched
     */
    protected Matrix4f cachedOffsetMat = null;

    /**
     * Serialization only. Do not use.
     */
    public Geometry() {
    }

    /**
     * Create a geometry node without any mesh data.
     * Both the mesh and the material are null, the geometry
     * cannot be rendered until those are set.
     * 
     * @param name The name of this geometry
     */
    public Geometry(String name) {
        super(name);
    }

    /**
     * Create a geometry node with mesh data.
     * The material of the geometry is null, it cannot
     * be rendered until it is set.
     * 
     * @param name The name of this geometry
     * @param mesh The mesh data for this geometry
     */
    public Geometry(String name, Mesh mesh) {
        this(name);
        if (mesh == null) {
            throw new NullPointerException();
        }

        this.mesh = mesh;
    }

    /**
     * @return If ignoreTransform mode is set.
     * 
     * @see Geometry#setIgnoreTransform(boolean) 
     */
    public boolean isIgnoreTransform() {
        return ignoreTransform;
    }

    /**
     * @param ignoreTransform If true, the geometry's transform will not be applied.
     */
    public void setIgnoreTransform(boolean ignoreTransform) {
        this.ignoreTransform = ignoreTransform;
    }

    /**
     * Sets the LOD level to use when rendering the mesh of this geometry.
     * Level 0 indicates that the default index buffer should be used,
     * levels [1, LodLevels + 1] represent the levels set on the mesh
     * with {@link Mesh#setLodLevels(com.jme3.scene.VertexBuffer[]) }.
     * 
     * @param lod The lod level to set
     */
    @Override
    public void setLodLevel(int lod) {
        if (mesh.getNumLodLevels() == 0) {
            throw new IllegalStateException("LOD levels are not set on this mesh");
        }

        if (lod < 0 || lod >= mesh.getNumLodLevels()) {
            throw new IllegalArgumentException("LOD level is out of range: " + lod);
        }

        lodLevel = lod;
    }

    /**
     * Returns the LOD level set with {@link #setLodLevel(int) }.
     * 
     * @return the LOD level set
     */
    public int getLodLevel() {
        return lodLevel;
    }

    /**
     * Returns this geometry's mesh vertex count.
     * 
     * @return this geometry's mesh vertex count.
     * 
     * @see Mesh#getVertexCount() 
     */
    public int getVertexCount() {
        return mesh.getVertexCount();
    }

    /**
     * Returns this geometry's mesh triangle count.
     * 
     * @return this geometry's mesh triangle count.
     * 
     * @see Mesh#getTriangleCount() 
     */
    public int getTriangleCount() {
        return mesh.getTriangleCount();
    }

    /**
     * Sets the mesh to use for this geometry when rendering.
     * 
     * @param mesh the mesh to use for this geometry
     * 
     * @throws IllegalArgumentException If mesh is null
     */
    public void setMesh(Mesh mesh) {
        if (mesh == null) {
            throw new IllegalArgumentException();
        }
        if (isBatched()) {
            throw new UnsupportedOperationException("Cannot set the mesh of a batched geometry");
        }

        this.mesh = mesh;
        setBoundRefresh();
    }

    /**
     * Returns the mseh to use for this geometry
     * 
     * @return the mseh to use for this geometry
     * 
     * @see #setMesh(com.jme3.scene.Mesh) 
     */
    public Mesh getMesh() {
        return mesh;
    }

    /**
     * Sets the material to use for this geometry.
     * 
     * @param material the material to use for this geometry
     */
    @Override
    public void setMaterial(Material material) {
        if (isBatched()) {
            throw new UnsupportedOperationException("Cannot set the material of a batched geometry, change the material of the parent BatchNode.");
        }
        this.material = material;
    }

    /**
     * Returns the material that is used for this geometry.
     * 
     * @return the material that is used for this geometry
     * 
     * @see #setMaterial(com.jme3.material.Material) 
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * @return The bounding volume of the mesh, in model space.
     */
    public BoundingVolume getModelBound() {
        return mesh.getBound();
    }

    /**
     * Updates the bounding volume of the mesh. Should be called when the
     * mesh has been modified.
     */
    public void updateModelBound() {
        mesh.updateBound();
        setBoundRefresh();
    }

    /**
     * <code>updateWorldBound</code> updates the bounding volume that contains
     * this geometry. The location of the geometry is based on the location of
     * all this node's parents.
     *
     * @see Spatial#updateWorldBound()
     */
    @Override
    protected void updateWorldBound() {
        super.updateWorldBound();
        if (mesh == null) {
            throw new NullPointerException("Geometry: " + getName() + " has null mesh");
        }

        if (mesh.getBound() != null) {
            if (ignoreTransform) {
                // we do not transform the model bound by the world transform,
                // just use the model bound as-is
                worldBound = mesh.getBound().clone(worldBound);
            } else {
                worldBound = mesh.getBound().transform(worldTransform, worldBound);
            }
        }
    }

    @Override
    protected void updateWorldTransforms() {

        super.updateWorldTransforms();
        computeWorldMatrix();

        if (isBatched()) {
            computeOffsetTransform();
            batchNode.updateSubBatch(this);
            prevBatchTransforms.set(batchNode.getTransforms(this));

        }
        // geometry requires lights to be sorted
        worldLights.sort(true);
    }

    /**
     * Batch this geometry, should only be called by the BatchNode.
     * @param node the batchNode
     * @param startIndex the starting index of this geometry in the batched mesh
     */
    protected void batch(BatchNode node, int startIndex) {
        this.batchNode = node;
        this.startIndex = startIndex;
        prevBatchTransforms = new Transform();
        cachedOffsetMat = new Matrix4f();
        setCullHint(CullHint.Always);
    }

    /**
     * unBatch this geometry. 
     */
    protected void unBatch() {
        this.startIndex = 0;
        prevBatchTransforms = null;
        cachedOffsetMat = null;
        //once the geometry is removed from the screnegraph the batchNode needs to be rebatched.
        this.batchNode.setNeedsFullRebatch(true);
        this.batchNode = null;
        setCullHint(CullHint.Dynamic);
    }

    @Override
    public boolean removeFromParent() {
        boolean removed = super.removeFromParent();
        //if the geometry is batched we also have to unbatch it
        if (isBatched()) {
            unBatch();
        }
        return removed;
    }

    /**
     * Recomputes the cached offset matrix used when the geometry is batched     * 
     */
    public void computeOffsetTransform() {
        TempVars vars = TempVars.get();
        Matrix4f tmpMat = vars.tempMat42;

        // Compute the cached world matrix
        cachedOffsetMat.loadIdentity();
        cachedOffsetMat.setRotationQuaternion(prevBatchTransforms.getRotation());
        cachedOffsetMat.setTranslation(prevBatchTransforms.getTranslation());


        Matrix4f scaleMat = vars.tempMat4;
        scaleMat.loadIdentity();
        scaleMat.scale(prevBatchTransforms.getScale());
        cachedOffsetMat.multLocal(scaleMat);
        cachedOffsetMat.invertLocal();

        tmpMat.loadIdentity();
        tmpMat.setRotationQuaternion(batchNode.getTransforms(this).getRotation());
        tmpMat.setTranslation(batchNode.getTransforms(this).getTranslation());
        scaleMat.loadIdentity();
        scaleMat.scale(batchNode.getTransforms(this).getScale());
        tmpMat.multLocal(scaleMat);

        tmpMat.mult(cachedOffsetMat, cachedOffsetMat);

        vars.release();
    }

    /**
     * Indicate that the transform of this spatial has changed and that
     * a refresh is required.
     */
    @Override
    protected void setTransformRefresh() {
        refreshFlags |= RF_TRANSFORM;
        setBoundRefresh();
    }

    /**
     * Recomputes the matrix returned by {@link Geometry#getWorldMatrix() }.
     * This will require a localized transform update for this geometry.
     */
    public void computeWorldMatrix() {
        // Force a local update of the geometry's transform
        checkDoTransformUpdate();

        // Compute the cached world matrix
        cachedWorldMat.loadIdentity();
        cachedWorldMat.setRotationQuaternion(worldTransform.getRotation());
        cachedWorldMat.setTranslation(worldTransform.getTranslation());

        TempVars vars = TempVars.get();
        Matrix4f scaleMat = vars.tempMat4;
        scaleMat.loadIdentity();
        scaleMat.scale(worldTransform.getScale());
        cachedWorldMat.multLocal(scaleMat);
        vars.release();
    }

    /**
     * A {@link Matrix4f matrix} that transforms the {@link Geometry#getMesh() mesh}
     * from model space to world space. This matrix is computed based on the
     * {@link Geometry#getWorldTransform() world transform} of this geometry.
     * In order to receive updated values, you must call {@link Geometry#computeWorldMatrix() }
     * before using this method.
     * 
     * @return Matrix to transform from local space to world space
     */
    public Matrix4f getWorldMatrix() {
        return cachedWorldMat;
    }

    /**
     * Sets the model bound to use for this geometry.
     * This alters the bound used on the mesh as well via
     * {@link Mesh#setBound(com.jme3.bounding.BoundingVolume) } and
     * forces the world bounding volume to be recomputed.
     * 
     * @param modelBound The model bound to set
     */
    @Override
    public void setModelBound(BoundingVolume modelBound) {
        this.worldBound = null;
        mesh.setBound(modelBound);
        setBoundRefresh();

        // NOTE: Calling updateModelBound() would cause the mesh
        // to recompute the bound based on the geometry thus making
        // this call useless!
        //updateModelBound();
    }

    public int collideWith(Collidable other, CollisionResults results) {
        // Force bound to update
        checkDoBoundUpdate();
        // Update transform, and compute cached world matrix
        computeWorldMatrix();

        assert (refreshFlags & (RF_BOUND | RF_TRANSFORM)) == 0;

        if (mesh != null) {
            // NOTE: BIHTree in mesh already checks collision with the
            // mesh's bound
            int prevSize = results.size();
            int added = mesh.collideWith(other, cachedWorldMat, worldBound, results);
            int newSize = results.size();
            for (int i = prevSize; i < newSize; i++) {
                results.getCollisionDirect(i).setGeometry(this);
            }
            return added;
        }
        return 0;
    }

    @Override
    public void depthFirstTraversal(SceneGraphVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void breadthFirstTraversal(SceneGraphVisitor visitor, Queue<Spatial> queue) {
    }

    public boolean isBatched() {
        return batchNode != null;
    }

    /**
     * This version of clone is a shallow clone, in other words, the
     * same mesh is referenced as the original geometry.
     * Exception: if the mesh is marked as being a software
     * animated mesh, (bind pose is set) then the positions
     * and normals are deep copied.
     */
    @Override
    public Geometry clone(boolean cloneMaterial) {
        Geometry geomClone = (Geometry) super.clone(cloneMaterial);
        geomClone.cachedWorldMat = cachedWorldMat.clone();
        if (material != null) {
            if (cloneMaterial) {
                geomClone.material = material.clone();
            } else {
                geomClone.material = material;
            }
        }

        if (mesh != null && mesh.getBuffer(Type.BindPosePosition) != null) {
            geomClone.mesh = mesh.cloneForAnim();
        }

        return geomClone;
    }

    /**
     * This version of clone is a shallow clone, in other words, the
     * same mesh is referenced as the original geometry.
     * Exception: if the mesh is marked as being a software
     * animated mesh, (bind pose is set) then the positions
     * and normals are deep copied.
     */
    @Override
    public Geometry clone() {
        return clone(true);
    }

    /**
     * Creates a deep clone of the geometry,
     * this creates an identical copy of the mesh
     * with the vertexbuffer data duplicated.
     */
    @Override
    public Spatial deepClone() {
        Geometry geomClone = clone(true);
        geomClone.mesh = mesh.deepClone();
        return geomClone;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(mesh, "mesh", null);
        if (material != null) {
            oc.write(material.getAssetName(), "materialName", null);
        }
        oc.write(material, "material", null);
        oc.write(ignoreTransform, "ignoreTransform", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        mesh = (Mesh) ic.readSavable("mesh", null);

        material = null;
        String matName = ic.readString("materialName", null);
        if (matName != null) {
            // Material name is set,
            // Attempt to load material via J3M
            try {
                material = im.getAssetManager().loadMaterial(matName);
            } catch (AssetNotFoundException ex) {
                // Cannot find J3M file.
                logger.log(Level.FINE, "Cannot locate {0} for geometry {1}", new Object[]{matName, key});
            }
        }
        // If material is NULL, try to load it from the geometry
        if (material == null) {
            material = (Material) ic.readSavable("material", null);
        }
        ignoreTransform = ic.readBoolean("ignoreTransform", false);
        
        if (ic.getSavableVersion(Geometry.class) == 0){
            // Fix shared mesh (if set)
            Mesh sharedMesh = getUserData(UserData.JME_SHAREDMESH);
            if (sharedMesh != null){
                getMesh().extractVertexData(sharedMesh);
            }
        }
    }
}
