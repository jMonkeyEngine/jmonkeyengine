/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.sceneexplorer.nodes.actions.impl;

import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AbstractToolAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.ToolAction;
import com.jme3.scene.Spatial;
import jme3tools.optimize.GeometryBatchFactory;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = ToolAction.class)
public class OptimizeGeometry extends AbstractToolAction {

    public OptimizeGeometry() {
        name = "Optimize Geometry";
    }

    @Override
    protected Object doApplyTool(AbstractSceneExplorerNode rootNode) {
        Spatial geom = rootNode.getLookup().lookup(Spatial.class);
        GeometryBatchFactory.optimize(geom);
        return null;
    }

    @Override
    protected void doUndoTool(AbstractSceneExplorerNode rootNode, Object undoObject) {
//        Spatial geom = rootNode.getLookup().lookup(Spatial.class);
//        geom.removeFromParent();
    }

    public Class<?> getNodeClass() {
        return JmeSpatial.class;
    }

}
