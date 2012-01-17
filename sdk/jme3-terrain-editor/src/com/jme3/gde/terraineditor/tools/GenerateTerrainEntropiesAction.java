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
import com.jme3.terrain.ProgressMonitor;
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
        final ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Generating Terrain Entropies");
        progressHandle.start();
        try {
            if (terrain instanceof TerrainQuad) { // it should be terrain anyways
                ((TerrainQuad) terrain).generateEntropy(new ProgressMonitor() {

                    private float progress = 0;
                    private float max = 0;

                    public void incrementProgress(float f) {
                        progress += f * 100.0f;
                        progressHandle.progress((int) progress);
                    }

                    public void setMonitorMax(float f) {
                        max = f * 100.0f;
                        progressHandle.switchToDeterminate((int) (f));
                    }

                    public float getMonitorMax() {
                        return max;
                    }

                    public void progressComplete() {
                        progressHandle.switchToIndeterminate();
                    }
                });
            }
        } finally {
            progressHandle.finish();
        }
        //return null, no undo entry created
        return null;
    }

    @Override
    protected void doUndoTool(AbstractSceneExplorerNode rootNode, Object undoObject) {
        // no undo, not needed
    }

    public Class<?> getNodeClass() {
        return JmeTerrainQuad.class;
    }
}
