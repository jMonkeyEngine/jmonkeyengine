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
        for (Batch batch : batches.getArray()) {
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
