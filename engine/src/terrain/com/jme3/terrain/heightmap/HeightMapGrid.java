/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.terrain.heightmap;

import com.jme3.math.Vector3f;

/**
 *
 * @author Anthyon
 */
@Deprecated
/**
 * @Deprecated in favor of TerrainGridTileLoader
 */
public interface HeightMapGrid {

    public HeightMap getHeightMapAt(Vector3f location);

    public void setSize(int size);

}
