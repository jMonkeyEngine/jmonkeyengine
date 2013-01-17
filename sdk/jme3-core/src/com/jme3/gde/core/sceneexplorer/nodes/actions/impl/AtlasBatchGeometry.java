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

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AbstractToolAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.ToolAction;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jme3tools.optimize.TextureAtlas;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = ToolAction.class)
public class AtlasBatchGeometry extends AbstractToolAction {

    private class OldNew {

        public OldNew(Spatial newSpat, List<Spatial> oldChildren) {
            this.newSpat = newSpat;
            this.oldChildren = oldChildren;
        }
        Spatial newSpat;
        List<Spatial> oldChildren;
    }

    public AtlasBatchGeometry() {
        name = "Batch Geometry with TextureAtlas";
    }

    @Override
    protected Object doApplyTool(AbstractSceneExplorerNode rootNode) {
        Node parent = rootNode.getLookup().lookup(Node.class);
        AssetManager mgr = rootNode.getLookup().lookup(ProjectAssetManager.class);
        if (parent == null || mgr == null) {
            return null;
        }
        Geometry batch = TextureAtlas.makeAtlasBatch(parent, mgr, 4096);
        batch.setName(parent.getName() + " - batched");
        List<Spatial> currentChildren = new ArrayList<Spatial>();
        if (parent != null && batch != null) {
            currentChildren.addAll(parent.getChildren());
            parent.detachAllChildren();
            parent.attachChild(batch);
        }
        return new OldNew(batch, currentChildren);
    }

    @Override
    protected void doUndoTool(AbstractSceneExplorerNode rootNode, Object undoObject) {
        Node parent = rootNode.getLookup().lookup(Node.class);
        OldNew undo = (OldNew) undoObject;
        if (parent == null || undo == null) {
            return;
        }
        parent.detachChild(undo.newSpat);
        for (Iterator<Spatial> it = undo.oldChildren.iterator(); it.hasNext();) {
            Spatial spatial = it.next();
            parent.attachChild(spatial);
        }
    }

    public Class<?> getNodeClass() {
        return JmeNode.class;
    }
}
