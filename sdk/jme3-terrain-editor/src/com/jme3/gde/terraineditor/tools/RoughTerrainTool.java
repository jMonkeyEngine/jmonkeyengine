/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.terraineditor.tools;

import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.terraineditor.ExtraToolParams;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.openide.loaders.DataObject;

/**
 * Roughens the terrain using a fractal noise routine.
 * @author Brent Owens
 */
public class RoughTerrainTool extends TerrainTool {
    
    private RoughExtraToolParams params;
    
    @Override
    public void actionPrimary(Vector3f point, int textureIndex, AbstractSceneExplorerNode rootNode, DataObject dataObject) {
        if (radius == 0 || weight == 0)
            return;
        RoughTerrainToolAction action = new RoughTerrainToolAction(point, radius, weight, (RoughExtraToolParams)params);
        action.doActionPerformed(rootNode, dataObject);
    }

    @Override
    public void actionSecondary(Vector3f point, int textureIndex, AbstractSceneExplorerNode rootNode, DataObject dataObject) {
        // do nothing
    }
    
    @Override
    public void addMarkerPrimary(Node parent) {
        super.addMarkerPrimary(parent);
        markerPrimary.getMaterial().setColor("Color", ColorRGBA.Yellow);
    }
    
    @Override
    public void setExtraParams(ExtraToolParams params) {
        this.params = (RoughExtraToolParams) params;
    }
    
    @Override
    public ExtraToolParams getExtraParams() {
        return params;
    }
    
    @Override
    public void extraParamsChanged(ExtraToolParams params) {
        this.params = (RoughExtraToolParams) params;
    }
}
