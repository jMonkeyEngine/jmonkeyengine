/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.terrain.heightmap;

/**
 *
 * @author Anthyon
 */
public interface Namer {

    /**
     * Gets a name for a heightmap tile given it's cell id
     * @param x
     * @param y
     * @return
     */
    public String getName(int x, int y);

}
