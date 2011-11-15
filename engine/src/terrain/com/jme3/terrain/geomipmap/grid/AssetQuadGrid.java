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
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.TerrainQuadGrid;
import java.io.IOException;

/**
 *
 * @author normenhansen
 */
public class AssetQuadGrid implements TerrainQuadGrid {

    private AssetManager manager;
    private String assetPath;
    private String name;
    private int size;
    private int patchSize;
    private int quadSize;

    public AssetQuadGrid() {
    }

    public AssetQuadGrid(AssetManager manager, String name, String assetPath, int size, int patchSize, int quadSize) {
        this.manager = manager;
        this.name = name;
        this.assetPath = assetPath;
        this.size = size;
        this.patchSize = patchSize;
        this.quadSize = quadSize;
    }

    public TerrainQuad getTerrainQuadAt(Vector3f location) {
        TerrainQuad quad = (TerrainQuad) manager.loadModel(assetPath + "/" + name + "-" + Math.round(location.x) + "/" + Math.round(location.y) + "/" + Math.round(location.z));
        if (quad == null) {
            createNewQuad(location);
        }
        return quad;
    }

    public void setSize(int size) {
        this.size = size;
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
        c.write(size, "size", 0);
        c.write(name, "name", null);
        c.write(patchSize, "patchSize", 0);
        c.write(quadSize, "quadSize", 0);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule c = im.getCapsule(this);
        manager = im.getAssetManager();
        assetPath = c.readString("assetPath", null);
        name = c.readString("name", null);
        size = c.readInt("size", 0);
        patchSize = c.readInt("patchSize", 0);
        quadSize = c.readInt("quadSize", 0);
    }
}