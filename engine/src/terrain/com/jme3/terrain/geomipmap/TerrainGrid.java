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
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
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

    private static final Logger log = Logger.getLogger(TerrainGrid.class.getCanonicalName());
    private Vector3f currentCell;
    private int quarterSize;
    private int quadSize;
    private HeightMapGrid heightMapGrid;
    private Vector3f[] quadOrigins;
    private Vector3f[] quadIndex;
    private Map<String, TerrainGridListener> listeners = new HashMap<String, TerrainGridListener>();
    private Material material;
    private LRUCache<Vector3f, TerrainQuad> cache = new LRUCache<Vector3f, TerrainQuad>(16);

    private class UpdateQuadCache implements Runnable {

        private final Vector3f location;
        private final boolean centerOnly;

        public UpdateQuadCache(Vector3f location) {
            this.location = location;
            this.centerOnly = false;
        }

        public UpdateQuadCache(Vector3f location, boolean centerOnly) {
            this.location = location;
            this.centerOnly = centerOnly;
        }

        public void run() {
            for (int i = centerOnly ? 1 : 0; i < (centerOnly ? 3 : 4); i++) {
                for (int j = centerOnly ? 1 : 0; j < (centerOnly ? 3 : 4); j++) {
                    Vector3f temp = location.add(quadIndex[i * 4 + j]);
                    if (cache.get(temp) == null) {
                        HeightMap heightMapAt = heightMapGrid.getHeightMapAt(temp);
                        TerrainQuad q = new TerrainQuad(getName() + "Quad" + temp, patchSize, quadSize, heightMapAt == null ? null : heightMapAt.getHeightMap(), lodCalculatorFactory);
                        Material mat = material.clone();
                        for (TerrainGridListener l : listeners.values()) {
                            mat = l.tileLoaded(mat, temp);
                        }
                        q.setMaterial(mat);
                        cache.put(temp, q);
                    }
                }
            }

        }
    }

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
        this.quadIndex = new Vector3f[]{
            new Vector3f(-1, 0, -1), new Vector3f(-1, 0, 0), new Vector3f(-1, 0, 1), new Vector3f(-1, 0, 2),
            new Vector3f(0, 0, -1), new Vector3f(0, 0, 0), new Vector3f(0, 0, 1), new Vector3f(0, 0, 2),
            new Vector3f(1, 0, -1), new Vector3f(1, 0, 0), new Vector3f(1, 0, 1), new Vector3f(1, 0, 2),
            new Vector3f(2, 0, -1), new Vector3f(2, 0, 0), new Vector3f(2, 0, 1), new Vector3f(2, 0, 2)};
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

    public void initialize(Vector3f location) {
        if (this.material == null) {
            throw new RuntimeException("Material must be set prior to call of initialize");
        }
        Vector3f camCell = this.getCell(location);
        this.updateChildrens(camCell);
        for (TerrainGridListener l : this.listeners.values()) {
            l.gridMoved(camCell);
        }
    }

    @Override
    public void update(List<Vector3f> locations) {
        // for now, only the first camera is handled.
        // to accept more, there are two ways:
        // 1: every camera has an associated grid, then the location is not enough to identify which camera location has changed
        // 2: grids are associated with locations, and no incremental update is done, we load new grids for new locations, and unload those that are not needed anymore
        Vector3f cam = locations.get(0);
        Vector3f camCell = this.getCell(cam);
        if (camCell.x != this.currentCell.x || camCell.z != currentCell.z) {
            this.updateChildrens(camCell);
            for (TerrainGridListener l : this.listeners.values()) {
                l.gridMoved(camCell);
            }
        }

        super.update(locations);
    }

    public Vector3f getCell(Vector3f location) {
        final Vector3f v = location.clone().divideLocal(this.getLocalScale().mult(this.quadSize)).add(0.5f, 0, 0.5f);
        return new Vector3f(FastMath.floor(v.x), 0, FastMath.floor(v.z));
    }

    protected void removeQuad(int idx) {
        if (this.getQuad(idx) != null) {
            this.detachChild(this.getQuad(idx));
        }
    }

    protected void moveQuad(int from, int to) {
        this.removeQuad(to);
        TerrainQuad fq = this.getQuad(from);
        fq.setQuadrant((short) to);
        fq.setLocalTranslation(this.quadOrigins[to - 1]);
    }

    protected void attachQuadAt(TerrainQuad q, int quadrant) {
        //q.setMaterial(this.material);
        q.setLocalTranslation(quadOrigins[quadrant - 1]);
        q.setQuadrant((short) quadrant);
        this.attachChild(q);
    }

    private void updateChildrens(Vector3f cam) {
        TerrainQuad q1 = cache.get(cam.add(quadIndex[5]));
        TerrainQuad q2 = cache.get(cam.add(quadIndex[6]));
        TerrainQuad q3 = cache.get(cam.add(quadIndex[9]));
        TerrainQuad q4 = cache.get(cam.add(quadIndex[10]));

        int dx = 0;
        int dy = 0;
        if (currentCell != null) {
            dx = (int) (cam.x - currentCell.x);
            dy = (int) (cam.z - currentCell.z);
        }

        int kxm = 0;
        int kxM = 4;
        int kym = 0;
        int kyM = 4;
        if (dx == -1) {
            kxM = 3;
        } else if (dx == 1) {
            kxm = 1;
        }

        if (dy == -1) {
            kyM = 3;
        } else if (dy == 1) {
            kym = 1;
        }

        for (int i = kym; i < kyM; i++) {
            for (int j = kxm; j < kxM; j++) {
                cache.get(cam.add(quadIndex[i * 4 + j]));
            }
        }

        if (q1 == null || q2 == null || q3 == null || q4 == null) {
            try {
                executor.submit(new UpdateQuadCache(cam, true)).get();
                q1 = cache.get(cam.add(quadIndex[5]));
                q2 = cache.get(cam.add(quadIndex[6]));
                q3 = cache.get(cam.add(quadIndex[9]));
                q4 = cache.get(cam.add(quadIndex[10]));
            } catch (InterruptedException ex) {
                Logger.getLogger(TerrainGrid.class.getName()).log(Level.SEVERE, null, ex);
                return;
            } catch (ExecutionException ex) {
                Logger.getLogger(TerrainGrid.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }

        executor.execute(new UpdateQuadCache(cam));

        RigidBodyControl control = getControl(RigidBodyControl.class);
        PhysicsSpace space = null;
        if (control != null) {
            space = control.getPhysicsSpace();
            space.remove(this);
            this.removeControl(control);
        }

        this.removeQuad(1);
        this.removeQuad(2);
        this.removeQuad(3);
        this.removeQuad(4);

        attachQuadAt(q1, 1);
        attachQuadAt(q2, 2);
        attachQuadAt(q3, 3);
        attachQuadAt(q4, 4);

        this.currentCell = cam;
        this.setLocalTranslation(cam.mult(this.getLocalScale().mult(this.quadSize-1)));
        this.updateModelBound();

        if (control != null) {
            int collisionGroupsCollideWith = control.getCollideWithGroups();
            int collisionGroups = control.getCollisionGroup();
            control = new RigidBodyControl(new HeightfieldCollisionShape(getHeightMap(), getLocalScale()), 0);
            control.setCollideWithGroups(collisionGroupsCollideWith);
            control.setCollisionGroup(collisionGroups);
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
