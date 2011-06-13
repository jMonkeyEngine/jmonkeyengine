/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.terrain.geomipmap;

import com.jme3.material.Material;
import com.jme3.math.Vector3f;

/**
 *
 * @author Anthyon
 */
public interface TerrainGridListener {

    public void gridMoved(Vector3f newCenter);

    public Material tileLoaded(Material material, Vector3f cell);

}
