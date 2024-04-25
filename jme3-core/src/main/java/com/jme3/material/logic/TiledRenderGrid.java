/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.material.logic;

import com.jme3.renderer.Camera;

/**
 *
 * @author codex
 */
public class TiledRenderGrid {
    
    private int divisions = 4;
    private int forcedTileSize = 64;
    private int tileSize = 0;
    private int gridWidth = 0;
    private int gridHeight = 0;

    public TiledRenderGrid() {}
    public TiledRenderGrid(int divisions) {
        this.divisions = divisions;
    }
    
    public void update(Camera cam) {
        tileSize = (forcedTileSize > 0 ? forcedTileSize : cam.getWidth()/divisions);
        int x1 = (int)(cam.getViewPortLeft()*cam.getWidth());
        int y1 = (int)(cam.getViewPortBottom()*cam.getHeight());
        int x2 = (int)(cam.getViewPortRight()*cam.getWidth());
        int y2 = (int)(cam.getViewPortTop()*cam.getHeight());
        gridWidth = (x2-x1)/tileSize;
        gridHeight = (y2-y1)/tileSize;
    }
    
    public void setNumDivisions(int divisions) {
        assert divisions > 0 : "Number of divisions must be greater than zero.";
        this.divisions = divisions;
    }
    public void setForcedTileSize(int forcedTileSize) {
        this.forcedTileSize = forcedTileSize;
    }
    public void copyFrom(TiledRenderGrid src) {
        setNumDivisions(src.divisions);
        setForcedTileSize(src.forcedTileSize);
        tileSize = src.tileSize;
        gridWidth = src.gridWidth;
        gridHeight = src.gridHeight;
    }
    
    public int getNumDivisions() {
        return divisions;
    }
    public int getForcedTileSize() {
        return forcedTileSize;
    }
    public int getTileSize() {
        return tileSize;
    }
    public int getGridWidth() {
        return gridWidth;
    }
    public int getGridHeight() {
        return gridHeight;
    }
    public int getNumTiles() {
        return gridWidth*gridHeight;
    }
    
    public boolean gridPropertiesDiffer(TiledRenderGrid grid) {
        return grid.tileSize != tileSize || gridDemensionsDiffer(grid);
    }
    public boolean gridDemensionsDiffer(TiledRenderGrid grid) {
        return grid.gridWidth != gridWidth || grid.gridHeight != gridHeight;
    }
    public boolean numTilesDiffer(TiledRenderGrid grid) {
        return grid.getNumTiles() != getNumTiles();
    }
    
    public void verifyUpdated() {
        if (needsUpdate()) {
            throw new IllegalStateException("Update is required before use.");
        }
    }
    public boolean needsUpdate() {
        return tileSize <= 0 || gridWidth <= 0 || gridHeight <= 0;
    }
    
}
