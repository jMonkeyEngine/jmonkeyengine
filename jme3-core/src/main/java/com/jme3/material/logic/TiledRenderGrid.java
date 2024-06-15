/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.material.logic;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.Camera;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class TiledRenderGrid implements Savable {
    
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

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(divisions, "divisions", 4);
        out.write(forcedTileSize, "forcedTileSize", 64);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        divisions = in.readInt("divisions", 4);
        forcedTileSize = in.readInt("forcedTileSize", 64);
    }
    
}
