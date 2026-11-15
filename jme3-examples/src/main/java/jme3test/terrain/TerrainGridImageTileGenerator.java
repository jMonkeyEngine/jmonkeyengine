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

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import javax.imageio.ImageIO;

/**
 * Splits a large heightmap image into the square PNG tiles used by
 * {@link com.jme3.terrain.geomipmap.grid.ImageTileLoader}.
 *
 * <p>Adjacent tiles share their border samples. For example, a 257 by 257
 * source image split into 129 by 129 tiles produces four images. This is the
 * tile size used by {@code new TerrainGrid("terrain", 65, 257, loader)}.
 * The output files are named {@code <base>_<x>_<y>.png}, matching the default
 * naming convention of {@code ImageTileLoader}. Since image heightmaps are
 * flipped vertically when loaded, source rows at the top receive larger Y
 * coordinates so the tiles align along TerrainGrid's positive Z axis.
 *
 * <p>Run this class with the jME examples runtime classpath, a source PNG, and
 * an output directory:
 * <pre>
 * java -cp CLASSPATH jme3test.terrain.TerrainGridImageTileGenerator heightmap.png assets/Scenes/MyTerrain
 * </pre>
 * The optional arguments are tile size, file-name base, first X coordinate,
 * and first Y coordinate, in that order. The default tile size is 129 and the
 * default base name is {@code terrain}. Put the output directory under an
 * AssetManager search root, then load the tiles with, for example:
 * <pre>
 * assetManager.registerLocator("assets", FileLocator.class);
 * ImageTileLoader loader = new ImageTileLoader("Scenes/MyTerrain/terrain", "png", assetManager);
 * </pre>
 *
 * @author jMonkeyEngine
 */
public final class TerrainGridImageTileGenerator {

    private static final int DEFAULT_TILE_SIZE = 129;
    private static final String DEFAULT_BASE_NAME = "terrain";

    private TerrainGridImageTileGenerator() {
    }

    /**
     * Split a source image into square, overlapping tiles and write them as
     * PNG files.
     *
     * @param source the complete heightmap (not null)
     * @param outputDirectory directory for the generated files (not null)
     * @param baseName file-name prefix without a directory or extension (not null)
     * @param tileSize width and height of each output tile; one less must be a
     * power of two
     * @param firstX X coordinate assigned to the leftmost tile
     * @param firstY Y coordinate assigned to the bottom row of source tiles;
     * coordinates increase toward the top of the source image
     * @return the number of PNG files written
     * @throws IOException if a tile cannot be written
     * @throws IllegalArgumentException if dimensions do not align to tile
     * boundaries or an argument is invalid
     */
    static int generateTiles(BufferedImage source, Path outputDirectory,
            String baseName, int tileSize, int firstX, int firstY) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(baseName, "baseName");

        int tileSpan = tileSize - 1;
        if (tileSpan < 2 || (tileSpan & (tileSpan - 1)) != 0) {
            throw new IllegalArgumentException("tile size must be one greater than a power of two: " + tileSize);
        }
        if (baseName.isEmpty() || baseName.contains("/") || baseName.contains("\\")) {
            throw new IllegalArgumentException("base name must not contain a directory: " + baseName);
        }

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        if (sourceWidth < tileSize || sourceHeight < tileSize
                || (sourceWidth - 1) % tileSpan != 0
                || (sourceHeight - 1) % tileSpan != 0) {
            throw new IllegalArgumentException("source dimensions must be N * (tile size - 1) + 1; got "
                    + sourceWidth + "x" + sourceHeight + " for tile size " + tileSize);
        }

        int columns = (sourceWidth - 1) / tileSpan;
        int rows = (sourceHeight - 1) / tileSpan;
        try {
            Math.addExact(firstX, columns - 1);
            Math.addExact(firstY, rows - 1);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("tile coordinates exceed the integer range", exception);
        }

        Files.createDirectories(outputDirectory);
        int count = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int x = firstX + column;
                int y = firstY + rows - 1 - row;
                int sourceX = column * tileSpan;
                int sourceY = row * tileSpan;

                // A child raster preserves the input color model and raw pixel
                // samples, including grayscale height values.
                WritableRaster tileRaster = source.getRaster().createWritableChild(
                        sourceX, sourceY, tileSize, tileSize, 0, 0, null);
                BufferedImage tile = new BufferedImage(source.getColorModel(), tileRaster,
                        source.isAlphaPremultiplied(), null);
                File outputFile = outputDirectory.resolve(baseName + "_" + x + "_" + y + ".png").toFile();
                if (!ImageIO.write(tile, "png", outputFile)) {
                    throw new IOException("No PNG writer is available for " + outputFile);
                }
                count++;
            }
        }
        return count;
    }

    /**
     * Generate tiles from a PNG file. Optional arguments are tile size, base
     * name, first X coordinate, and first Y coordinate.
     *
     * @param args source PNG, output directory, then up to four optional values
     * @throws IOException if the source cannot be read or a tile cannot be written
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 6) {
            throw new IllegalArgumentException("Usage: TerrainGridImageTileGenerator <source.png> <output-directory> "
                    + "[tile-size=129] [base-name=terrain] [first-x=0] [first-y=0]");
        }

        Path sourcePath = Paths.get(args[0]);
        BufferedImage source = ImageIO.read(sourcePath.toFile());
        if (source == null) {
            throw new IOException("Unsupported or invalid image: " + sourcePath);
        }

        int tileSize = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_TILE_SIZE;
        String baseName = args.length > 3 ? args[3] : DEFAULT_BASE_NAME;
        int firstX = args.length > 4 ? Integer.parseInt(args[4]) : 0;
        int firstY = args.length > 5 ? Integer.parseInt(args[5]) : 0;
        Path outputDirectory = Paths.get(args[1]);

        int count = generateTiles(source, outputDirectory, baseName, tileSize, firstX, firstY);
        System.out.printf("Wrote %d %dx%d PNG tiles to %s%n", count, tileSize, tileSize, outputDirectory);
    }
}
