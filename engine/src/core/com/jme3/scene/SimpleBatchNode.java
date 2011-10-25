/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.scene;

import com.jme3.math.Transform;

/**
 * 
 * SimpleBatchNode  comes with some restrictions, but can yield better performances.
 * Geometries to be batched has to be attached directly to the BatchNode
 * You can't attach a Node to a SimpleBatchNode
 * SimpleBatchNode is recommended when you have a large number of geometries using the same material that does not require a complex scene graph structure.
 * @see BatchNode
 * @author Nehon
 */
public class SimpleBatchNode extends BatchNode {

    public SimpleBatchNode() {
        super();
    }

    public SimpleBatchNode(String name) {
        super(name);
    }

    @Override
    public int attachChild(Spatial child) {

        if (!(child instanceof Geometry)) {
            throw new UnsupportedOperationException("BatchNode is BatchMode.Simple only support child of type Geometry, use BatchMode.Complex to use a complex structure");
        }

        return super.attachChild(child);
    }

    @Override
    protected void setTransformRefresh() {

        refreshFlags |= RF_TRANSFORM;
        setBoundRefresh();
        for (Batch batch : batches.values()) {
            batch.geometry.setTransformRefresh();
        }
    }
    
     protected Transform getTransforms(Geometry geom){
        return geom.getLocalTransform();
    }

    @Override
    public void batch() {
        doBatch();
    }
}
