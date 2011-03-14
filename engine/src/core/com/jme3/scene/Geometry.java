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

import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.util.Queue;

public class Geometry extends Spatial {

    /**
     * The mesh contained herein
     */
    protected Mesh mesh;

    protected transient int lodLevel = 0;

    protected Material material;

    /**
     * When true, the geometry's transform will not be applied.
     */
    protected boolean ignoreTransform = false;

    protected transient Matrix4f cachedWorldMat = new Matrix4f();

    /**
     * Do not use this constructor. Serialization purposes only.
     */
    public Geometry(){
    }

    /**
     * Create a geometry node without any mesh data.
     * @param name The name of this geometry
     */
    public Geometry(String name){
        super(name);
    }

    /**
     * Create a geometry node with mesh data.
     *
     * @param name The name of this geometry
     * @param mesh The mesh data for this geometry
     */
    public Geometry(String name, Mesh mesh){
        this(name);
        if (mesh == null)
            throw new NullPointerException();

        this.mesh = mesh;
    }

    /**
     * @return If ignoreTransform mode is set.
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

    @Override
    public void setLodLevel(int lod){
        if (mesh.getNumLodLevels() == 0)
            throw new IllegalStateException("LOD levels are not set on this mesh");

        if (lod < 0 || lod >= mesh.getNumLodLevels())
            throw new IllegalArgumentException("LOD level is out of range: "+lod);

        lodLevel = lod;
    }

    public int getLodLevel(){
        return lodLevel;
    }

    public int getVertexCount(){
        return mesh.getVertexCount();
    }

    public int getTriangleCount(){
        return mesh.getTriangleCount();
    }

    public void setMesh(Mesh mesh){
        if (mesh == null)
            throw new NullPointerException();

        this.mesh = mesh;
        setBoundRefresh();
    }

    public Mesh getMesh(){
        return mesh;
    }

    @Override
    public void setMaterial(Material material){
        this.material = material;
    }

    public Material getMaterial(){
        return material;
    }

    /**
     * @return The bounding volume of the mesh, in model space.
     */
    public BoundingVolume getModelBound(){
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
     * @see com.jme.scene.Spatial#updateWorldBound()
     */
    @Override
    protected void updateWorldBound() {
        super.updateWorldBound();
        if (mesh == null)
            throw new NullPointerException("Geometry: "+getName()+" has null mesh");

        if (mesh.getBound() != null) {
            if (ignoreTransform){
                // we do not transform the model bound by the world transform,
                // just use the model bound as-is
                worldBound = mesh.getBound().clone(worldBound);
            }else{
                worldBound = mesh.getBound().transform(worldTransform, worldBound);
            }
        }
    }

    @Override
    protected void updateWorldTransforms(){
        super.updateWorldTransforms();

        computeWorldMatrix();

        // geometry requires lights to be sorted
        worldLights.sort(true);
    }

    /**
     * Recomputes the matrix returned by {@link Geometry#getWorldMatrix() }.
     * This will require a localized transform update for this geometry.
     */
    public void computeWorldMatrix(){
        // Force a local update of the geometry's transform
        checkDoTransformUpdate();

        // Compute the cached world matrix
        cachedWorldMat.loadIdentity();
        cachedWorldMat.setRotationQuaternion(worldTransform.getRotation());
        cachedWorldMat.setTranslation(worldTransform.getTranslation());

        assert TempVars.get().lock();
        Matrix4f scaleMat = TempVars.get().tempMat4;
        scaleMat.loadIdentity();
        scaleMat.scale(worldTransform.getScale());
        cachedWorldMat.multLocal(scaleMat);
        assert TempVars.get().unlock();
    }

    /**
     * @return A {@link Matrix4f matrix} that transforms the {@link Geometry#getMesh() mesh}
     * from model space to world space. This matrix is computed based on the
     * {@link Geometry#getWorldTransform() world transform} of this geometry.
     * In order to receive updated values, you must call {@link Geometry#computeWorldMatrix() }
     * before using this method.
     */
    public Matrix4f getWorldMatrix(){
        return cachedWorldMat;
    }

    @Override
    public void setModelBound(BoundingVolume modelBound) {
        this.worldBound = null;
        mesh.setBound(modelBound);
        updateModelBound();
    }

    public int collideWith(Collidable other, CollisionResults results){
        // Force bound to update
        checkDoBoundUpdate();
        // Update transform, and compute cached world matrix
        computeWorldMatrix();
        
        assert (refreshFlags & (RF_BOUND | RF_TRANSFORM)) == 0;

        if (mesh != null){
            // NOTE: BIHTree in mesh already checks collision with the
            // mesh's bound
            int prevSize = results.size();
            int added = mesh.collideWith(other, cachedWorldMat, worldBound, results);
            int newSize = results.size();
            for (int i = prevSize; i < newSize; i++){
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

    /**
     * This version of clone is a shallow clone, in other words, the
     * same mesh is referenced as the original geometry.
     * Exception: if the mesh is marked as being a software
     * animated mesh, (bind pose is set) then the positions
     * and normals are deep copied.
     * @return
     */
    @Override
    public Geometry clone(boolean cloneMaterial){
        Geometry geomClone = (Geometry) super.clone(cloneMaterial);
        geomClone.cachedWorldMat = cachedWorldMat.clone();
        if (material != null){
            if (cloneMaterial)
                geomClone.material = material.clone();
            else
                geomClone.material = material;
        }
        
        if (mesh != null && mesh.getBuffer(Type.BindPosePosition) != null){
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
     * @return
     */
    public Geometry clone(){
        return clone(true);
    }

    /**
     * Creates a deep clone of the geometry,
     * this creates an identical copy of the mesh
     * with the vertexbuffer data duplicated.
     * @return
     */
    @Override
    public Spatial deepClone(){
        Geometry geomClone = clone(true);
        geomClone.mesh = mesh.deepClone();
        return geomClone;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(mesh, "mesh", null);
        oc.write(material, "material", null);
        oc.write(ignoreTransform, "ignoreTransform", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        mesh = (Mesh) ic.readSavable("mesh", null);
        material = (Material) ic.readSavable("material", null);
        ignoreTransform = ic.readBoolean("ignoreTransform", false);
    }

}
