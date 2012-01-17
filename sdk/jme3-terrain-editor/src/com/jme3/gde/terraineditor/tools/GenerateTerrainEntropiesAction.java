/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.terraineditor.tools;

import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeTerrainQuad;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AbstractToolAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.ToolAction;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainQuad;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;

/**
 *
 * @author Brent Owens
 */
@org.openide.util.lookup.ServiceProvider(service = ToolAction.class)
public class GenerateTerrainEntropiesAction extends AbstractToolAction {

    public GenerateTerrainEntropiesAction() {
        name = "Generate Entropies";
    }

    @Override
    protected Object doApplyTool(AbstractSceneExplorerNode rootNode) {
        Node terrain = rootNode.getLookup().lookup(Node.class);
        ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Generating Entropies");
        progressHandle.start();
        try {
            if (terrain instanceof TerrainQuad) { // it should be terrain anyways
                ((TerrainQuad) terrain).generateEntropy(null); //TODO hook up to progress monitor
            }
        } finally {
            progressHandle.finish();
        }
        return true;
    }

    @Override
    protected void doUndoTool(AbstractSceneExplorerNode rootNode, Object undoObject) {
        // no undo, not needed
    }

    public Class<?> getNodeClass() {
        return JmeTerrainQuad.class;
    }
}
