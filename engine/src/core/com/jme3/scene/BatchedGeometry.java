/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package com.jme3.scene;

import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.UnsupportedCollisionException;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.util.TempVars;
import java.util.Queue;

/**
 *
 * @author Nehon
 */
public class BatchedGeometry extends Spatial {

    private GeometryBatch batch;
    protected int startIndex;
    protected int vertexCount;
    protected int triangleCount;
    protected Transform prevLocalTransform = new Transform();
    protected Matrix4f cachedOffsetMat = new Matrix4f();
    protected Matrix4f tmpMat = new Matrix4f();
    

    protected BatchedGeometry(GeometryBatch batch, Geometry geom) {
        this.batch = batch;
        vertexCount = geom.getVertexCount();
        triangleCount = geom.getTriangleCount();
        name = geom.getName();
    }

    /**
     * Should only be called from updateGeometricState().
     * In most cases should not be subclassed.
     */
    @Override
    protected void updateWorldTransforms() {
        if (batch == null) {
            worldTransform.set(localTransform);
            refreshFlags &= ~RF_TRANSFORM;
        } else {
            // check if transform for parent is updated
            assert ((batch.refreshFlags & RF_TRANSFORM) == 0);
            worldTransform.set(localTransform);
            worldTransform.combineWithParent(batch.worldTransform);          
            computeOffsetTransform();           
            batch.updateSubBatch(this);
            prevLocalTransform.set(localTransform);
            refreshFlags &= ~RF_TRANSFORM;
        }
    }

    @Override
    public Node getParent() {
        return batch.getParent();
    }
    
       
      /**
     * Recomputes the matrix returned by {@link Geometry#getWorldMatrix() }.
     * This will require a localized transform update for this geometry.
     */
    public void computeOffsetTransform() {     
     
        
        // Compute the cached world matrix
        cachedOffsetMat.loadIdentity();
        cachedOffsetMat.setRotationQuaternion(prevLocalTransform.getRotation());
        cachedOffsetMat.setTranslation(prevLocalTransform.getTranslation());

        TempVars vars = TempVars.get();
        Matrix4f scaleMat = vars.tempMat4;
        scaleMat.loadIdentity();
        scaleMat.scale(prevLocalTransform.getScale());
        cachedOffsetMat.multLocal(scaleMat);
        cachedOffsetMat.invertLocal();
       
        tmpMat.loadIdentity();
        tmpMat.setRotationQuaternion(localTransform.getRotation());
        tmpMat.setTranslation(localTransform.getTranslation());      
        scaleMat.loadIdentity();
        scaleMat.scale(localTransform.getScale());
        tmpMat.multLocal(scaleMat);
        
        tmpMat.mult(cachedOffsetMat,cachedOffsetMat);
        
        vars.release();
    }   
     

    @Override
    public void updateModelBound() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void setModelBound(BoundingVolume modelBound) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public int getVertexCount() {
        return vertexCount;
    }

    @Override
    public int getTriangleCount() {
        return triangleCount;
    }

    @Override
    public Spatial deepClone() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void depthFirstTraversal(SceneGraphVisitor visitor) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    protected void breadthFirstTraversal(SceneGraphVisitor visitor, Queue<Spatial> queue) {
        throw new UnsupportedOperationException("Not supported.");
    }

    public int collideWith(Collidable other, CollisionResults results) throws UnsupportedCollisionException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
