/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package jme3tools.savegame;

import com.jme3.asset.AssetManager;
import com.jme3.export.Savable;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.system.JmeSystem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Tool for saving Savables as SaveGame entries in a system-dependent way.
 *
 * @author normenhansen
 */
public class SaveGame {

    /**
     * The logger instance for this class. Used to log messages, warnings, and errors.
     */
    private static final Logger logger = Logger.getLogger(SaveGame.class.getName());

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SaveGame() {
    }

    /**
     * Saves a savable in a system-dependent way.
     * @param gamePath A unique path for this game, e.g. com/mycompany/mygame
     * @param dataName A unique name for this SaveGame, e.g. "save_001"
     * @param data The Savable to save
     */
    public static void saveGame(String gamePath, String dataName, Savable data) {
        saveGame(gamePath, dataName, data, JmeSystem.StorageFolderType.External);
    }

    /**
     * Saves a savable in a system-dependent way.
     * @param gamePath A unique path for this game, e.g. com/mycompany/mygame
     * @param dataName A unique name for this SaveGame, e.g. "save_001"
     * @param data The Savable to save
     * @param storageType The specific type of folder to use to save the data
     */
    public static void saveGame(String gamePath, String dataName, Savable data, JmeSystem.StorageFolderType storageType) {
        if (storageType == null) {
            logger.log(Level.WARNING, "Storage folder type is null. Using {0} as default.", JmeSystem.StorageFolderType.External);
            storageType = JmeSystem.StorageFolderType.External;
        }

        try {
            File baseFolder = JmeSystem.getStorageFolder(storageType);
            if (baseFolder == null) {
                throw new IllegalStateException("Failed to get base storage folder for type: " + storageType);
            }

            Path saveFolderPath = Paths.get(baseFolder.getAbsolutePath(), gamePath.replace('/', File.separatorChar));
            Path saveFilePath = saveFolderPath.resolve(dataName);

            // Ensure directories exist
            Files.createDirectories(saveFolderPath);

            try (OutputStream os = Files.newOutputStream(saveFilePath);
                 GZIPOutputStream gzip = new GZIPOutputStream(new BufferedOutputStream(os))) {

                BinaryExporter exporter = BinaryExporter.getInstance();
                exporter.save(data, gzip);
                logger.log(Level.INFO, "Successfully saved data to: {0}", saveFilePath.toAbsolutePath());
            }
        } catch (IOException | IllegalStateException ex) {
            logger.log(Level.SEVERE, "Error saving data for gamePath: {0}, dataName: {1}. Exception: {2}",
                    new Object[]{gamePath, dataName, ex.getMessage()});
            throw new IllegalStateException("SaveGame dataset cannot be saved.", ex);
        }
    }

    /**
     * Loads a savable that has been saved on this system with saveGame() before.
     * @param gamePath A unique path for this game, e.g. com/mycompany/mygame
     * @param dataName A unique name for this SaveGame, e.g. "save_001"
     * @return The savable that was saved
     */
    public static Savable loadGame(String gamePath, String dataName) {
        return loadGame(gamePath, dataName, null, JmeSystem.StorageFolderType.External);
    }

    /**
     * Loads a savable that has been saved on this system with saveGame() before.
     * @param gamePath A unique path for this game, e.g. com/mycompany/mygame
     * @param dataName A unique name for this SaveGame, e.g. "save_001"
     * @param storageType The specific type of folder to use to save the data
     * @return The savable that was saved
     */
    public static Savable loadGame(String gamePath, String dataName, JmeSystem.StorageFolderType storageType) {
        return loadGame(gamePath, dataName, null, storageType);
    }

    /**
     * Loads a savable that has been saved on this system with saveGame() before.
     * @param gamePath A unique path for this game, e.g. com/mycompany/mygame
     * @param dataName A unique name for this SaveGame, e.g. "save_001"
     * @param manager Link to an AssetManager if required for loading the data (e.g. models with textures)
     * @return The savable that was saved or null if none was found
     */
    public static Savable loadGame(String gamePath, String dataName, AssetManager manager) {
        return loadGame(gamePath, dataName, manager, JmeSystem.StorageFolderType.External);
    }

    /**
     * Loads a savable that has been saved on this system with saveGame() before.
     * @param gamePath A unique path for this game, e.g. com/mycompany/mygame
     * @param dataName A unique name for this SaveGame, e.g. "save_001"
     * @param assetManager Link to an AssetManager if required for loading the data (e.g. models with textures)
     * @param storageType The specific type of folder to use to save the data
     * @return The savable that was saved or null if none was found
     */
    public static Savable loadGame(String gamePath, String dataName, AssetManager assetManager, JmeSystem.StorageFolderType storageType) {
        if (storageType == null) {
            logger.log(Level.WARNING, "Storage folder type is null. Using {0} as default.", JmeSystem.StorageFolderType.External);
            storageType = JmeSystem.StorageFolderType.External;
        }

        try {
            File baseFolder = JmeSystem.getStorageFolder(storageType);
            if (baseFolder == null) {
                logger.log(Level.SEVERE, "Error reading base storage folder for type: {0}", storageType);
                return null;
            }
            Path loadFilePath = Paths.get(baseFolder.getAbsolutePath(), gamePath.replace('/', File.separatorChar), dataName);

            // Check if the file exists before attempting to load
            if (!Files.exists(loadFilePath)) {
                logger.log(Level.INFO, "Save game file not found: {0}", loadFilePath.toAbsolutePath());
                return null;
            }

            try (InputStream is = Files.newInputStream(loadFilePath);
                 GZIPInputStream gzip = new GZIPInputStream(new BufferedInputStream(is))) {

                BinaryImporter importer = BinaryImporter.getInstance();
                if (assetManager != null) {
                    importer.setAssetManager(assetManager);
                }
                Savable savable = importer.load(gzip);
                logger.log(Level.INFO, "Successfully loaded data from: {0}", loadFilePath.toAbsolutePath());
                return savable;
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error loading data from gamePath: {0}, dataName: {1}. Exception: {2}",
                    new Object[]{gamePath, dataName, ex.getMessage()});
            return null;
        }
    }
}
