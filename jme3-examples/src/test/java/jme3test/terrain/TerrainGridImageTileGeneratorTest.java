/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
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
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3test.terrain;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.grid.ImageTileLoader;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TerrainGridImageTileGenerator}.
 *
 * @author jMonkeyEngine
 */
class TerrainGridImageTileGeneratorTest {

    @Test
    void generatesTilesWithSharedBorderSamplesAndExpectedNames(@TempDir Path tempDirectory) throws IOException {
        BufferedImage source = new BufferedImage(5, 5, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                source.getRaster().setSample(x, y, 0, x + y * source.getWidth());
            }
        }

        Path outputDirectory = tempDirectory.resolve("tiles");
        int tileCount = TerrainGridImageTileGenerator.generateTiles(
                source, outputDirectory, "terrain", 3, -1, 4);

        assertEquals(4, tileCount);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                Path tilePath = outputDirectory.resolve("terrain_" + (column - 1) + "_" + (5 - row) + ".png");
                assertTrue(Files.isRegularFile(tilePath), tilePath.toString());

                BufferedImage tile = ImageIO.read(tilePath.toFile());
                assertEquals(3, tile.getWidth());
                assertEquals(3, tile.getHeight());
                for (int localY = 0; localY < 3; localY++) {
                    for (int localX = 0; localX < 3; localX++) {
                        int sourceX = column * 2 + localX;
                        int sourceY = row * 2 + localY;
                        assertEquals(source.getRaster().getSample(sourceX, sourceY, 0),
                                tile.getRaster().getSample(localX, localY, 0));
                    }
                }
            }
        }
    }

    @Test
    void rejectsMisalignedSourceBeforeCreatingOutputDirectory(@TempDir Path tempDirectory) {
        BufferedImage source = new BufferedImage(6, 5, BufferedImage.TYPE_BYTE_GRAY);
        Path outputDirectory = tempDirectory.resolve("tiles");

        assertThrows(IllegalArgumentException.class, () -> TerrainGridImageTileGenerator.generateTiles(
                source, outputDirectory, "terrain", 3, 0, 0));
        assertFalse(Files.exists(outputDirectory));
    }

    @Test
    void generatedFilesLoadAsImageTileLoaderHeightmaps(@TempDir Path tempDirectory) throws IOException {
        BufferedImage source = new BufferedImage(257, 257, BufferedImage.TYPE_BYTE_GRAY);
        Path outputDirectory = tempDirectory.resolve("tiles");
        Path sourcePath = tempDirectory.resolve("heightmap.png");
        assertTrue(ImageIO.write(source, "png", sourcePath.toFile()));
        TerrainGridImageTileGenerator.main(new String[]{sourcePath.toString(), outputDirectory.toString()});

        AssetManager assetManager = new DesktopAssetManager(true);
        assetManager.registerLocator(outputDirectory.toString(), FileLocator.class);
        ImageTileLoader tileLoader = new ImageTileLoader("terrain", "png", assetManager);
        tileLoader.setPatchSize(65);
        tileLoader.setQuadSize(129);

        TerrainQuad tile = tileLoader.getTerrainQuadAt(Vector3f.ZERO);

        assertEquals(129, tile.getTotalSize());
    }
}
