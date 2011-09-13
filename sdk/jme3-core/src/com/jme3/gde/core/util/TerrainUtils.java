
package com.jme3.gde.core.util;

import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainLodControl;

/**
 *
 * @author Brent Owens
 */
public class TerrainUtils {
    
    /**
     * Re-attach the camera to the LOD control.
     * Called when the scene is opened and will only
     * update the control if there is already a terrain present in
     * the scene.
     */
    public static void enableLodControl(Camera camera, Node rootNode) {
        
        Terrain terrain = (Terrain) findTerrain(rootNode);
        if (terrain == null)
            return;
        
        TerrainLodControl control = ((Spatial)terrain).getControl(TerrainLodControl.class);
        if (control != null) {
            control.setCamera(camera);
        }
    }
    
    protected static Node findTerrain(Spatial root) {
       
        // is this the terrain?
        if (root instanceof Terrain && root instanceof Node) {
            return (Node)root;
        }

        if (root instanceof Node) {
            Node n = (Node) root;
            for (Spatial c : n.getChildren()) {
                if (c instanceof Node){
                    Node res = findTerrain(c);
                    if (res != null)
                        return res;
                }
            }
        }

        return null;
    }
    
}
