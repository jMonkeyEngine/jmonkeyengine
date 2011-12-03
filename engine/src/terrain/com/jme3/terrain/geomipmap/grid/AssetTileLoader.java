/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Load terrain grid tile: {0}", modelName);
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
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Loaded terrain grid tile: {0}", modelName);
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