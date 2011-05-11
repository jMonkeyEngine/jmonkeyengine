/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.terrain.geomipmap;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.terrain.heightmap.HeightMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.lodcalc.LodCalculatorFactory;
import com.jme3.terrain.geomipmap.lodcalc.LodDistanceCalculatorFactory;
import com.jme3.terrain.heightmap.HeightMapGrid;

/**
 * @author Anthyon
 */
public class TerrainGrid extends TerrainQuad {

    private static Logger log = Logger.getLogger(TerrainGrid.class.getCanonicalName());
    private Vector3f currentCell;
    private int quarterSize;
    private int quadSize;
    private HeightMapGrid heightMapGrid;
    private Vector3f[] quadOrigins;
    private Vector3f[] quadIndex;
    private Map<String, TerrainGridListener> listeners = new HashMap<String, TerrainGridListener>();
    private Material material;

    public TerrainGrid(String name, int patchSize, int size, Vector3f stepScale, HeightMapGrid heightMapGrid, int totalSize,
            Vector2f offset, float offsetAmount, LodCalculatorFactory lodCalculatorFactory) {
        this.name = name;
        this.patchSize = patchSize;
        this.size = size;
        this.quarterSize = size >> 2;
        this.quadSize = (size + 1) >> 1;
        this.stepScale = stepScale;
        this.heightMapGrid = heightMapGrid;
        heightMapGrid.setSize(this.quadSize);
        this.totalSize = totalSize;
        this.offset = offset;
        this.offsetAmount = offsetAmount;
        this.lodCalculatorFactory = lodCalculatorFactory;
        if (lodCalculatorFactory == null) {
            lodCalculatorFactory = new LodDistanceCalculatorFactory();
        }
        this.quadOrigins = new Vector3f[]{new Vector3f(-this.quarterSize, 0, -this.quarterSize).mult(this.stepScale),
            new Vector3f(-this.quarterSize, 0, this.quarterSize).mult(this.stepScale),
            new Vector3f(this.quarterSize, 0, -this.quarterSize).mult(this.stepScale),
            new Vector3f(this.quarterSize, 0, this.quarterSize).mult(this.stepScale)};
        this.quadIndex = new Vector3f[]{new Vector3f(0, 0, 0), new Vector3f(0, 0, 1), new Vector3f(1, 0, 0), new Vector3f(1, 0, 1)};

        updateChildrens(Vector3f.ZERO);
    }

    public TerrainGrid(String name, int patchSize, int size, Vector3f scale, HeightMapGrid heightMapGrid,
            LodCalculatorFactory lodCalculatorFactory) {
        this(name, patchSize, size, scale, heightMapGrid, size, new Vector2f(), 0, lodCalculatorFactory);
    }

    public TerrainGrid(String name, int patchSize, int totalSize, HeightMapGrid heightMapGrid, LodCalculatorFactory lodCalculatorFactory) {
        this(name, patchSize, totalSize, Vector3f.UNIT_XYZ, heightMapGrid, lodCalculatorFactory);
    }

    public TerrainGrid(String name, int patchSize, int totalSize, HeightMapGrid heightMapGrid) {
        this(name, patchSize, totalSize, heightMapGrid, null);
    }

    public TerrainGrid() {
    }

    @Override
    public void update(List<Vector3f> locations) {
        // for now, only the first camera is handled.
        // to accept more, there are two ways:
        // 1: every camera has an associated grid, then the location is not enough to identify which camera location has changed
        // 2: grids are associated with locations, and no incremental update is done, we load new grids for new locations, and unload those that are not needed anymore
        Vector3f cam = locations.get(0);
        Vector3f camCell = this.getCell(cam);
        if (!camCell.equals(this.currentCell)) {
            this.updateChildrens(camCell);
            for (TerrainGridListener l : this.listeners.values()) {
                l.gridMoved(camCell);
            }
        }

        super.update(locations);
    }

    public Vector3f getCell(Vector3f location) {
        final Vector3f v = location.clone().divideLocal(this.getLocalScale().mult(this.quadSize)).add(0.5f, 0, 0.5f);
        return new Vector3f(FastMath.floor(v.x), FastMath.floor(v.y), FastMath.floor(v.z));
    }

    protected void removeQuad(int idx) {
        this.detachChild(this.getQuad(idx));
    }

    protected void moveQuad(int from, int to) {
        this.removeQuad(to);
        TerrainQuad fq = this.getQuad(from);
        fq.setQuadrant((short) to);
        fq.setLocalTranslation(this.quadOrigins[to - 1]);
    }

    protected TerrainQuad createQuadAt(Vector3f location, int quadrant) {
        final HeightMap heightMapAt = this.heightMapGrid.getHeightMapAt(location);
        TerrainQuad q = new TerrainQuad(this.getName() + "Quad" + location, this.patchSize, this.quadSize, heightMapAt == null ? null : heightMapAt.getHeightMap(), this.lodCalculatorFactory);
        q.setLocalTranslation(this.quadOrigins[quadrant - 1]);
        q.setMaterial(this.material);
        q.setQuadrant((short) quadrant);
        return q;
    }

    private void updateChildrens(Vector3f cam) {
        RigidBodyControl control = getControl(RigidBodyControl.class);
        PhysicsSpace space = null;
        if (control != null) {
            space = control.getPhysicsSpace();
            space.remove(this);
            this.removeControl(control);
        }
        int dx = (int) cam.x;
        int dz = (int) cam.z;
        if (this.currentCell != null) {
            dx -= (int) (this.currentCell.x);
            dz -= (int) (this.currentCell.z);
        }
        if (this.currentCell == null || FastMath.abs(dx) > 1 || FastMath.abs(dz) > 1 || (dx != 0 && dz != 0)) {
            if (this.currentCell != null) {
                // in case of teleport, otherwise the FastMath.abs(delta) should
                // never be greater than 1
                this.removeQuad(1);
                this.removeQuad(2);
                this.removeQuad(3);
                this.removeQuad(4);
            }
            this.attachChild(this.createQuadAt(cam.add(this.quadIndex[0]).mult(this.quadSize - 1), 1));
            this.attachChild(this.createQuadAt(cam.add(this.quadIndex[1]).mult(this.quadSize - 1), 2));
            this.attachChild(this.createQuadAt(cam.add(this.quadIndex[2]).mult(this.quadSize - 1), 3));
            this.attachChild(this.createQuadAt(cam.add(this.quadIndex[3]).mult(this.quadSize - 1), 4));
        } else if (dx == 0) {
            if (dz < 0) {
                // move north
                this.moveQuad(1, 2);
                this.moveQuad(3, 4);
                this.attachChild(this.createQuadAt(cam.add(this.quadIndex[0]).mult(this.quadSize - 1), 1));
                this.attachChild(this.createQuadAt(cam.add(this.quadIndex[2]).mult(this.quadSize - 1), 3));
            } else {
                // move south
                this.moveQuad(2, 1);
                this.moveQuad(4, 3);
                this.attachChild(this.createQuadAt(cam.add(this.quadIndex[1]).mult(this.quadSize - 1), 2));
                this.attachChild(this.createQuadAt(cam.add(this.quadIndex[3]).mult(this.quadSize - 1), 4));
            }
        } else if (dz == 0) {
            if (dx < 0) {
                // move west
                this.moveQuad(1, 3);
                this.moveQuad(2, 4);
                this.attachChild(this.createQuadAt(cam.add(this.quadIndex[0]).mult(this.quadSize - 1), 1));
                this.attachChild(this.createQuadAt(cam.add(this.quadIndex[1]).mult(this.quadSize - 1), 2));
            } else {
                // move east
                this.moveQuad(3, 1);
                this.moveQuad(4, 2);
                this.attachChild(this.createQuadAt(cam.add(this.quadIndex[2]).mult(this.quadSize - 1), 3));
                this.attachChild(this.createQuadAt(cam.add(this.quadIndex[3]).mult(this.quadSize - 1), 4));
            }
        } else {
            // rare situation to enter into a diagonally placed cell
            // could not get into this part while testing, as it is handled by moving first
            // in either horizontally or vertically than the other way
            // I handle it in the first IF
        }
        this.currentCell = cam;
        this.setLocalTranslation(cam.mult(2 * this.quadSize));

        if (control != null) {
            control = new RigidBodyControl(new HeightfieldCollisionShape(getHeightMap(), getLocalScale()), 0);
            this.addControl(control);
            space.add(this);
        }
    }

    public void addListener(String id, TerrainGridListener listener) {
        this.listeners.put(id, listener);
    }

    public Vector3f getCurrentCell() {
        return this.currentCell;
    }

    public void removeListener(String id) {
        this.listeners.remove(id);
    }

    @Override
    public void setMaterial(Material mat) {
        this.material = mat;
        super.setMaterial(mat);
    }

    public void setQuadSize(int quadSize) {
        this.quadSize = quadSize;
    }
}
