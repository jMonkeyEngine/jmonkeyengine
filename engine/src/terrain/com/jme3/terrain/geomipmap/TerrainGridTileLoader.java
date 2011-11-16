/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.terrain.geomipmap;

import com.jme3.export.Savable;
import com.jme3.math.Vector3f;

/**
 *
 * @author normenhansen
 */
public interface TerrainGridTileLoader extends Savable {

    public TerrainQuad getTerrainQuadAt(Vector3f location);

    public void setPatchSize(int patchSize);

    public void setQuadSize(int quadSize);
}
