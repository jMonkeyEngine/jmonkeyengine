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
package com.jme3.terrain;

import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.lodcalc.LodCalculator;
import java.util.List;

/**
 * Terrain can be one or many meshes comprising of a, probably large, piece of land.
 * Terrain is Y-up in the grid axis, meaning gravity acts in the -Y direction.
 * Level of Detail (LOD) is supported and expected as terrains can get very large. LOD can
 * also be disabled if you so desire, however some terrain implementations can choose to ignore
 * useLOD(boolean).
 * Terrain implementations should extend Node, or at least Spatial.
 *
 * @author bowens
 */
public interface Terrain {

    /**
     * Get the real-world height of the terrain at the specified X-Z coorindate.
     * @param xz the X-Z world coordinate
     * @return the height at the given point
     */
    public float getHeight(Vector2f xz);
    
    /**
     * Get the normal vector for the surface of the terrain at the specified
     * X-Z coordinate. This normal vector can be a close approximation. It does not
     * take into account any normal maps on the material.
     * @param xz the X-Z world coordinate
     * @return the normal vector at the given point
     */
    public Vector3f getNormal(Vector2f xz);

    /**
     * Get the heightmap height at the specified X-Z coordinate. This does not
     * count scaling and snaps the XZ coordinate to the nearest (rounded) heightmap grid point.
     * @param xz world coordinate
     * @return the height, unscaled and uninterpolated
     */
    public float getHeightmapHeight(Vector2f xz);

    /**
     * Set the height at the specified X-Z coordinate.
     * To set the height of the terrain and see it, you will have
     * to unlock the terrain meshes by calling terrain.setLocked(false) before
     * you call setHeight().
     * @param xzCoordinate coordinate to set the height
     * @param height that will be set at the coordinate
     */
    public void setHeight(Vector2f xzCoordinate, float height);

    /**
     * Set the height at many points. The two lists must be the same size.
     * Each xz coordinate entry matches to a height entry, 1 for 1. So the 
     * first coordinate matches to the first height value, the last to the 
     * last etc.
     * @param xz a list of coordinates where the hight will be set
     * @param height the heights that match the xz coordinates
     */
    public void setHeight(List<Vector2f> xz, List<Float> height);

    /**
     * Raise/lower the height in one call (instead of getHeight then setHeight).
     * @param xzCoordinate world coordinate to adjust the terrain height
     * @param delta +- value to adjust the height by
     */
    public void adjustHeight(Vector2f xzCoordinate, float delta);

    /**
     * Raise/lower the height at many points. The two lists must be the same size.
     * Each xz coordinate entry matches to a height entry, 1 for 1. So the
     * first coordinate matches to the first height value, the last to the
     * last etc.
     * @param xz a list of coordinates where the hight will be adjusted
     * @param height +- value to adjust the height by, that matches the xz coordinates
     */
    public void adjustHeight(List<Vector2f> xz, List<Float> height);

    /**
     * Get the heightmap of the entire terrain.
     * This can return null if that terrain object does not store the height data.
     * Infinite or "paged" terrains will not be able to support this, so use with caution.
     */
    public float[] getHeightMap();
    
    /**
     * This is calculated by the specific LOD algorithm.
     * A value of one means that the terrain is showing full detail.
     * The higher the value, the more the terrain has been generalized
     * and the less detailed it will be.
     */
    public int getMaxLod();

    /**
     * Called by an LodControl.
     * Calculates the level of detail of the terrain and adjusts its geometry.
     * This is where the Terrain's LOD algorithm will change the detail of
     * the terrain based on how far away this position is from the particular
     * terrain patch.
     * @param location the Camera's location. A list of one camera location is normal 
     *  if you just have one camera in your scene.
     */
    public void update(List<Vector3f> location, LodCalculator lodCalculator);

    /**
     * Lock or unlock the meshes of this terrain.
     * Locked meshes are un-editable but have better performance.
     * This should call the underlying getMesh().setStatic()/setDynamic() methods.
     * @param locked or unlocked
     */
    public void setLocked(boolean locked);

    /**
     * Pre-calculate entropy values.
     * Some terrain systems support entropy calculations to determine LOD
     * changes. Often these entropy calculations are expensive and can be
     * cached ahead of time. Use this method to do that.
     */
    public void generateEntropy(ProgressMonitor monitor);

    /**
     * Returns the material that this terrain uses.
     * If it uses many materials, just return the one you think is best.
     * For TerrainQuads this is sufficient. For TerrainGrid you want to call
     * getMaterial(Vector3f) instead.
     */
    public Material getMaterial();
    
    /**
     * Returns the material that this terrain uses.
     * Terrain can have different materials in different locations.
     * In general, the TerrainQuad will only have one material. But 
     * TerrainGrid will have a different material per tile.
     * 
     * It could be possible to pass in null for the location, some Terrain
     * implementations might just have the one material and not care where
     * you are looking. So implementations must handle null being supplied.
     * 
     * @param worldLocation the location, in world coordinates, of where 
     * we are interested in the underlying texture.
     */
    public Material getMaterial(Vector3f worldLocation);

    /**
     * Used for painting to get the number of vertices along the edge of the
     * terrain.
     * This is an un-scaled size, and should represent the vertex count (ie. the
     * texture coord count) along an edge of a square terrain.
     * 
     * In the standard TerrainQuad default implementation, this will return
     * the "totalSize" of the terrain (512 or so).
     */
    public int getTerrainSize();

    /**
     * Get the scale of the texture coordinates. Normally if the texture is
     * laid on the terrain and not scaled so that the texture does not repeat,
     * then each texture coordinate (on a vertex) will be 1/(terrain size).
     * That is: the coverage between each consecutive texture coordinate will
     * be a percentage of the total terrain size.
     * So if the terrain is 512 vertexes wide, then each texture coord will cover
     * 1/512 (or 0.00195) percent of the texture.
     * This is used for converting between tri-planar texture scales and regular
     * texture scales.
     * 
     * not needed
     */
    //public float getTextureCoordinateScale();
    
    /**
     * 
     * 
     */
    public int getNumMajorSubdivisions();
}
