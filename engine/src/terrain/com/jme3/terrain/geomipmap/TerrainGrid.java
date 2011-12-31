/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.terrain.geomipmap;

import com.jme3.bounding.BoundingBox;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.UpdateControl;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.lodcalc.LodCalculator;
import com.jme3.terrain.heightmap.HeightMap;
import com.jme3.terrain.heightmap.HeightMapGrid;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TerrainGrid itself is an actual TerrainQuad. Its four children are the visible four tiles.
 * 
 * The grid is indexed by cells. Each cell has an integer XZ coordinate originating at 0,0.
 * TerrainGrid will piggyback on the TerrainLodControl so it can use the camera for its
 * updates as well. It does this in the overwritten update() method.
 * 
 * It uses an LRU (Least Recently Used) cache of 16 terrain tiles (full TerrainQuadTrees). The
 * center 4 are the ones that are visible. As the camera moves, it checks what camera cell it is in
 * and will attach the now visible tiles.
 * 
 * The 'quadIndex' variable is a 4x4 array that represents the tiles. The center 
 * four (index numbers: 5, 6, 9, 10) are what is visible. Each quadIndex value is an
 * offset vector. The vector contains whole numbers and represents how many tiles in offset
 * this location is from the center of the map. So for example the index 11 [Vector3f(2, 0, 1)]
 * is located 2*terrainSize in X axis and 1*terrainSize in Z axis.
 * 
 * As the camera moves, it tests what cameraCell it is in. Each camera cell covers four quad tiles
 * and is half way inside each one.
 * 
 * +-------+-------+
 * | 1     |     4 |    Four terrainQuads that make up the grid
 * |    *..|..*    |    with the cameraCell in the middle, covering
 * |----|--|--|----|    all four quads.
 * |    *..|..*    |
 * | 2     |     3 |
 * +-------+-------+
 * 
 * This results in the effect of when the camera gets half way across one of the sides of a quad to
 * an empty (non-loaded) area, it will trigger the system to load in the next tiles.
 * 
 * The tile loading is done on a background thread, and once the tile is loaded, then it is
 * attached to the qrid quad tree, back on the OGL thread. It will grab the terrain quad from
 * the LRU cache if it exists. If it does not exist, it will load in the new TerrainQuad tile.
 * 
 * The loading of new tiles triggers events for any TerrainGridListeners. The events are:
 *  -tile Attached
 *  -tile Detached
 *  -grid moved.
 * 
 * These allow physics to update, and other operation (often needed for loading the terrain) to occur
 * at the right time.
 * 
 * @author Anthyon
 */
public class TerrainGrid extends TerrainQuad {

    protected static final Logger log = Logger.getLogger(TerrainGrid.class.getCanonicalName());
    protected Vector3f currentCamCell = Vector3f.ZERO;
    protected int quarterSize; // half of quadSize
    protected int quadSize;
    protected HeightMapGrid heightMapGrid;
    private TerrainGridTileLoader gridTileLoader;
    protected Vector3f[] quadIndex;
    protected Set<TerrainGridListener> listeners = new HashSet<TerrainGridListener>();
    protected Material material;
    protected LRUCache<Vector3f, TerrainQuad> cache = new LRUCache<Vector3f, TerrainQuad>(16);
    private int cellsLoaded = 0;
    private int[] gridOffset;
    private boolean runOnce = false;

    protected class UpdateQuadCache implements Runnable {

        protected final Vector3f location;

        public UpdateQuadCache(Vector3f location) {
            this.location = location;
        }

        /**
         * This is executed if the camera has moved into a new CameraCell and will load in
         * the new TerrainQuad tiles to be children of this TerrainGrid parent.
         * It will first check the LRU cache to see if the terrain tile is already there,
         * if it is not there, it will load it in and then cache that tile.
         * The terrain tiles get added to the quad tree back on the OGL thread using the
         * attachQuadAt() method. It also resets any cached values in TerrainQuad (such as
         * neighbours).
         */
        public void run() {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    int quadIdx = i * 4 + j;
                    final Vector3f quadCell = location.add(quadIndex[quadIdx]);
                    TerrainQuad q = cache.get(quadCell);
                    if (q == null) {
                        if (heightMapGrid != null) {
                            // create the new Quad since it doesn't exist
                            HeightMap heightMapAt = heightMapGrid.getHeightMapAt(quadCell);
                            q = new TerrainQuad(getName() + "Quad" + quadCell, patchSize, quadSize, heightMapAt == null ? null : heightMapAt.getHeightMap());
                            q.setMaterial(material.clone());
                            log.log(Level.FINE, "Loaded TerrainQuad {0} from HeightMapGrid", q.getName());
                        } else if (gridTileLoader != null) {
                            q = gridTileLoader.getTerrainQuadAt(quadCell);
                            // only clone the material to the quad if it doesn't have a material of its own
                            if(q.getMaterial()==null) q.setMaterial(material.clone());
                            log.log(Level.FINE, "Loaded TerrainQuad {0} from TerrainQuadGrid", q.getName());
                        }
                    }
                    cache.put(quadCell, q);

                    if (isCenter(quadIdx)) {
                        // if it should be attached as a child right now, attach it
                        final int quadrant = getQuadrant(quadIdx);
                        final TerrainQuad newQuad = q;
                        // back on the OpenGL thread:
                        getControl(UpdateControl.class).enqueue(new Callable() {

                            public Object call() throws Exception {
                                attachQuadAt(newQuad, quadrant, quadCell);
                                //newQuad.resetCachedNeighbours();
                                return null;
                            }
                        });
                    }
                }
            }

        }
    }

    protected boolean isCenter(int quadIndex) {
        return quadIndex == 9 || quadIndex == 5 || quadIndex == 10 || quadIndex == 6;
    }

    protected int getQuadrant(int quadIndex) {
        if (quadIndex == 5) {
            return 1;
        } else if (quadIndex == 9) {
            return 2;
        } else if (quadIndex == 6) {
            return 3;
        } else if (quadIndex == 10) {
            return 4;
        }
        return 0; // error
    }

    public TerrainGrid(String name, int patchSize, int maxVisibleSize, Vector3f scale, TerrainGridTileLoader terrainQuadGrid,
            Vector2f offset, float offsetAmount) {
        this.name = name;
        this.patchSize = patchSize;
        this.size = maxVisibleSize;
        this.stepScale = scale;
        this.offset = offset;
        this.offsetAmount = offsetAmount;
        initData();
        this.gridTileLoader = terrainQuadGrid;
        terrainQuadGrid.setPatchSize(this.patchSize);
        terrainQuadGrid.setQuadSize(this.quadSize);
        addControl(new UpdateControl());
    }

    public TerrainGrid(String name, int patchSize, int maxVisibleSize, Vector3f scale, TerrainGridTileLoader terrainQuadGrid) {
        this(name, patchSize, maxVisibleSize, scale, terrainQuadGrid, new Vector2f(), 0);
    }

    public TerrainGrid(String name, int patchSize, int maxVisibleSize, TerrainGridTileLoader terrainQuadGrid) {
        this(name, patchSize, maxVisibleSize, Vector3f.UNIT_XYZ, terrainQuadGrid);
    }

    @Deprecated
    public TerrainGrid(String name, int patchSize, int maxVisibleSize, Vector3f scale, HeightMapGrid heightMapGrid,
            Vector2f offset, float offsetAmount) {
        this.name = name;
        this.patchSize = patchSize;
        this.size = maxVisibleSize;
        this.stepScale = scale;
        this.offset = offset;
        this.offsetAmount = offsetAmount;
        initData();
        this.heightMapGrid = heightMapGrid;
        heightMapGrid.setSize(this.quadSize);
        addControl(new UpdateControl());
    }

    @Deprecated
    public TerrainGrid(String name, int patchSize, int maxVisibleSize, Vector3f scale, HeightMapGrid heightMapGrid) {
        this(name, patchSize, maxVisibleSize, scale, heightMapGrid, new Vector2f(), 0);
    }

    @Deprecated
    public TerrainGrid(String name, int patchSize, int maxVisibleSize, HeightMapGrid heightMapGrid) {
        this(name, patchSize, maxVisibleSize, Vector3f.UNIT_XYZ, heightMapGrid);
    }

    public TerrainGrid() {
    }

    private void initData() {
        int maxVisibleSize = size;
        this.quarterSize = maxVisibleSize >> 2;
        this.quadSize = (maxVisibleSize + 1) >> 1;
        this.totalSize = maxVisibleSize;
        this.gridOffset = new int[]{0, 0};

        /*
         *        -z
         *         | 
         *        1|3 
         *  -x ----+---- x
         *        2|4
         *         |
         *         z
         */
        this.quadIndex = new Vector3f[]{
            new Vector3f(-1, 0, -1), new Vector3f(0, 0, -1), new Vector3f(1, 0, -1), new Vector3f(2, 0, -1),
            new Vector3f(-1, 0, 0), new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), new Vector3f(2, 0, 0),
            new Vector3f(-1, 0, 1), new Vector3f(0, 0, 1), new Vector3f(1, 0, 1), new Vector3f(2, 0, 1),
            new Vector3f(-1, 0, 2), new Vector3f(0, 0, 2), new Vector3f(1, 0, 2), new Vector3f(2, 0, 2)};

    }

    /**
     * @deprecated not needed to be called any more, handled automatically
     */
    public void initialize(Vector3f location) {
        if (this.material == null) {
            throw new RuntimeException("Material must be set prior to call of initialize");
        }
        Vector3f camCell = this.getCamCell(location);
        this.updateChildren(camCell);
        for (TerrainGridListener l : this.listeners) {
            l.gridMoved(camCell);
        }
    }

    @Override
    public void update(List<Vector3f> locations, LodCalculator lodCalculator) {
        // for now, only the first camera is handled.
        // to accept more, there are two ways:
        // 1: every camera has an associated grid, then the location is not enough to identify which camera location has changed
        // 2: grids are associated with locations, and no incremental update is done, we load new grids for new locations, and unload those that are not needed anymore
        Vector3f cam = locations.isEmpty() ? Vector3f.ZERO.clone() : locations.get(0);
        Vector3f camCell = this.getCamCell(cam); // get the grid index value of where the camera is (ie. 2,1)
        if (cellsLoaded > 1) {                  // Check if cells are updated before updating gridoffset.
            gridOffset[0] = Math.round(camCell.x * (size / 2));
            gridOffset[1] = Math.round(camCell.z * (size / 2));
            cellsLoaded = 0;
        }
        if (camCell.x != this.currentCamCell.x || camCell.z != currentCamCell.z || !runOnce) {
            // if the camera has moved into a new cell, load new terrain into the visible 4 center quads
            this.updateChildren(camCell);
            for (TerrainGridListener l : this.listeners) {
                l.gridMoved(camCell);
            }
        }
        runOnce = true;
        super.update(locations, lodCalculator);
    }

    public Vector3f getCamCell(Vector3f location) {
        Vector3f tile = getTileCell(location);
        Vector3f offsetHalf = new Vector3f(-0.5f, 0, -0.5f);
        Vector3f shifted = tile.subtract(offsetHalf);
        return new Vector3f(FastMath.floor(shifted.x), 0, FastMath.floor(shifted.z));
    }

    /**
     * Centered at 0,0.
     * Get the tile index location in integer form:
     * @param location world coordinate
     */
    public Vector3f getTileCell(Vector3f location) {
        Vector3f tileLoc = location.divide(this.getWorldScale().mult(this.quadSize));
        return tileLoc;
    }

    public TerrainGridTileLoader getGridTileLoader() {
        return gridTileLoader;
    }
    
    protected void removeQuad(int idx) {
        if (this.getQuad(idx) != null) {
            for (TerrainGridListener l : listeners) {
                l.tileDetached(getTileCell(this.getQuad(idx).getWorldTranslation()), this.getQuad(idx));
            }
            this.detachChild(this.getQuad(idx));
            cellsLoaded++; // For gridoffset calc., maybe the run() method is a better location for this.
        }
    }

    /**
     * Runs on the rendering thread
     */
    protected void attachQuadAt(TerrainQuad q, int quadrant, Vector3f quadCell) {
        this.removeQuad(quadrant);

        q.setQuadrant((short) quadrant);
        this.attachChild(q);

        Vector3f loc = quadCell.mult(this.quadSize - 1).subtract(quarterSize, 0, quarterSize);// quadrant location handled TerrainQuad automatically now
        q.setLocalTranslation(loc);

        for (TerrainGridListener l : listeners) {
            l.tileAttached(quadCell, q);
        }
        updateModelBound();
        
        for (Spatial s : getChildren()) {
            if (s instanceof TerrainQuad) {
                TerrainQuad tq = (TerrainQuad)s;
                tq.resetCachedNeighbours();
                tq.fixNormalEdges(new BoundingBox(tq.getWorldTranslation(), totalSize*2, Float.MAX_VALUE, totalSize*2));
            }
        }
    }

    @Deprecated
    /**
     * @Deprecated, use updateChildren
     */
    protected void updateChildrens(Vector3f camCell) {
        updateChildren(camCell);
    }
    
    /**
     * Called when the camera has moved into a new cell. We need to
     * update what quads are in the scene now.
     * 
     * Step 1: touch cache
     * LRU cache is used, so elements that need to remain
     * should be touched.
     *
     * Step 2: load new quads in background thread
     * if the camera has moved into a new cell, we load in new quads
     * @param camCell the cell the camera is in
     */
    protected void updateChildren(Vector3f camCell) {

        int dx = 0;
        int dy = 0;
        if (currentCamCell != null) {
            dx = (int) (camCell.x - currentCamCell.x);
            dy = (int) (camCell.z - currentCamCell.z);
        }

        int xMin = 0;
        int xMax = 4;
        int yMin = 0;
        int yMax = 4;
        if (dx == -1) { // camera moved to -X direction
            xMax = 3;
        } else if (dx == 1) { // camera moved to +X direction
            xMin = 1;
        }

        if (dy == -1) { // camera moved to -Y direction
            yMax = 3;
        } else if (dy == 1) { // camera moved to +Y direction
            yMin = 1;
        }

        // Touch the items in the cache that we are and will be interested in.
        // We activate cells in the direction we are moving. If we didn't move 
        // either way in one of the axes (say X or Y axis) then they are all touched.
        for (int i = yMin; i < yMax; i++) {
            for (int j = xMin; j < xMax; j++) {
                cache.get(camCell.add(quadIndex[i * 4 + j]));
            }
        }
        // ---------------------------------------------------
        // ---------------------------------------------------

        if (executor == null) {
            // use the same executor as the LODControl
            executor = createExecutorService();
        }

        executor.submit(new UpdateQuadCache(camCell));

        this.currentCamCell = camCell;
    }

    public void addListener(TerrainGridListener listener) {
        this.listeners.add(listener);
    }

    public Vector3f getCurrentCell() {
        return this.currentCamCell;
    }

    public void removeListener(TerrainGridListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void setMaterial(Material mat) {
        this.material = mat;
        super.setMaterial(mat);
    }

    public void setQuadSize(int quadSize) {
        this.quadSize = quadSize;
    }

    @Override
    public void adjustHeight(List<Vector2f> xz, List<Float> height) {
        Vector3f currentGridLocation = getCurrentCell().mult(getLocalScale()).multLocal(quadSize - 1);
        for (Vector2f vect : xz) {
            vect.x -= currentGridLocation.x;
            vect.y -= currentGridLocation.z;
        }
        super.adjustHeight(xz, height);
    }

    @Override
    protected float getHeightmapHeight(int x, int z) {
        return super.getHeightmapHeight(x - gridOffset[0], z - gridOffset[1]);
    }
    
    @Override
    public int getNumMajorSubdivisions() {
        return 2;
    }
    
    @Override
    public Material getMaterial(Vector3f worldLocation) {
        if (worldLocation == null)
            return null;
        Vector3f tileCell = getTileCell(worldLocation);
        Terrain terrain = cache.get(tileCell);
        if (terrain == null)
            return null; // terrain not loaded for that cell yet!
        return terrain.getMaterial(worldLocation);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule c = im.getCapsule(this);
        name = c.readString("name", null);
        size = c.readInt("size", 0);
        patchSize = c.readInt("patchSize", 0);
        stepScale = (Vector3f) c.readSavable("stepScale", null);
        offset = (Vector2f) c.readSavable("offset", null);
        offsetAmount = c.readFloat("offsetAmount", 0);
        gridTileLoader = (TerrainGridTileLoader) c.readSavable("terrainQuadGrid", null);
        material = (Material) c.readSavable("material", null);
        initData();
        if (gridTileLoader != null) {
            gridTileLoader.setPatchSize(this.patchSize);
            gridTileLoader.setQuadSize(this.quadSize);
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule c = ex.getCapsule(this);
        c.write(gridTileLoader, "terrainQuadGrid", null);
        c.write(size, "size", 0);
        c.write(patchSize, "patchSize", 0);
        c.write(stepScale, "stepScale", null);
        c.write(offset, "offset", null);
        c.write(offsetAmount, "offsetAmount", 0);
        c.write(material, "material", null);
    }
}
