/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.terrain.geomipmap.grid;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainGridTileLoader;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author normenhansen
 */
public class AssetTileLoader implements TerrainGridTileLoader {

    private AssetManager manager;
    private String assetPath;
    private String name;
    private int size;
    private int patchSize;
    private int quadSize;

    public AssetTileLoader() {
    }

    public AssetTileLoader(AssetManager manager, String name, String assetPath) {
        this.manager = manager;
        this.name = name;
        this.assetPath = assetPath;
    }

    public TerrainQuad getTerrainQuadAt(Vector3f location) {
        String modelName = assetPath + "/" + name + "_" + Math.round(location.x) + "_" + Math.round(location.y) + "_" + Math.round(location.z) + ".j3o";
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Load terrain grid tile: {0}", modelName);
        TerrainQuad quad = null;
        try {
            quad = (TerrainQuad) manager.loadModel(modelName);
        } catch (Exception e) {
//            e.printStackTrace();
        }
        if (quad == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not load terrain grid tile: {0}", modelName);
            quad = createNewQuad(location);
        } else {
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Loaded terrain grid tile: {0}", modelName);
        }
        return quad;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public String getName() {
        return name;
    }

    public void setPatchSize(int patchSize) {
        this.patchSize = patchSize;
    }

    public void setQuadSize(int quadSize) {
        this.quadSize = quadSize;
    }

    private TerrainQuad createNewQuad(Vector3f location) {
        TerrainQuad q = new TerrainQuad("Quad" + location, patchSize, quadSize, null);
        return q;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule c = ex.getCapsule(this);
        c.write(assetPath, "assetPath", null);
        c.write(name, "name", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule c = im.getCapsule(this);
        manager = im.getAssetManager();
        assetPath = c.readString("assetPath", null);
        name = c.readString("name", null);
    }
}