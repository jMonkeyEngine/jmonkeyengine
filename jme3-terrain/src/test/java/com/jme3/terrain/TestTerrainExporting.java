/*
 * Copyright (c) 2021 jMonkeyEngine
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

import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.FastMath;
import com.jme3.terrain.collision.BaseAWTTest;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test saving/loading terrain.
 *
 * @author Ali-RS
 */
public class TestTerrainExporting extends BaseAWTTest {

    /**
     * Test saving/loading a TerrainQuad.
     */
    @Test
    public void testTerrainExporting() {

        Texture heightMapImage = getAssetManager().loadTexture("Textures/Terrain/splat/mountains512.png");
        AbstractHeightMap map = new ImageBasedHeightMap(heightMapImage.getImage(), 0.25f);
        map.load();

        TerrainQuad terrain = new TerrainQuad("Terrain", 65, 513, map.getHeightMap());

        TerrainQuad saveAndLoad = BinaryExporter.saveAndLoad(getAssetManager(), terrain);

        Assert.assertEquals(513, saveAndLoad.getTotalSize());
        Assert.assertEquals(65, saveAndLoad.getPatchSize());
        Assert.assertArrayEquals(terrain.getHeightMap(), saveAndLoad.getHeightMap(), FastMath.ZERO_TOLERANCE);
    }

}
