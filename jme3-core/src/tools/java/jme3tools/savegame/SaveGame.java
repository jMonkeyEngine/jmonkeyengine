/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Tool for saving Savables as SaveGame entries in a system-dependent way.
 * @author normenhansen
 */
public class SaveGame {

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
            Logger.getLogger(SaveGame.class.getName()).log(Level.SEVERE, "Base Storage Folder Type is null, using External!");
            storageType = JmeSystem.StorageFolderType.External;
        }

        BinaryExporter ex = BinaryExporter.getInstance();
        OutputStream os = null;
        try {
            File baseFolder = JmeSystem.getStorageFolder(storageType);
            if (baseFolder == null) {
                Logger.getLogger(SaveGame.class.getName()).log(Level.SEVERE, "Error creating save file!");
                throw new IllegalStateException("SaveGame dataset cannot be created");
            }
            File daveFolder = new File(baseFolder.getAbsolutePath() + File.separator + gamePath.replace('/', File.separatorChar));
            if (!daveFolder.exists() && !daveFolder.mkdirs()) {
                Logger.getLogger(SaveGame.class.getName()).log(Level.SEVERE, "Error creating save file!");
                throw new IllegalStateException("SaveGame dataset cannot be created");
            }
            File saveFile = new File(daveFolder.getAbsolutePath() + File.separator + dataName);
            if (!saveFile.exists()) {
                if (!saveFile.createNewFile()) {
                    Logger.getLogger(SaveGame.class.getName()).log(Level.SEVERE, "Error creating save file!");
                    throw new IllegalStateException("SaveGame dataset cannot be created");
                }
            }
            os = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(saveFile)));
            ex.save(data, os);
            Logger.getLogger(SaveGame.class.getName()).log(Level.FINE, "Saving data to: {0}", saveFile.getAbsolutePath());
        } catch (IOException ex1) {
            Logger.getLogger(SaveGame.class.getName()).log(Level.SEVERE, "Error saving data: {0}", ex1);
            ex1.printStackTrace();
            throw new IllegalStateException("SaveGame dataset cannot be saved");
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(SaveGame.class.getName()).log(Level.SEVERE, "Error saving data: {0}", ex1);
                ex1.printStackTrace();
                throw new IllegalStateException("SaveGame dataset cannot be saved");
            }
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
     * @param manager Link to an AssetManager if required for loading the data (e.g. models with textures)
     * @param storageType The specific type of folder to use to save the data
     * @return The savable that was saved or null if none was found
     */
    public static Savable loadGame(String gamePath, String dataName, AssetManager manager, JmeSystem.StorageFolderType storageType) {
        if (storageType == null) {
            Logger.getLogger(SaveGame.class.getName()).log(Level.SEVERE, "Base Storage Folder Type is null, using External!");
            storageType = JmeSystem.StorageFolderType.External;
        }

        InputStream is = null;
        Savable sav = null;
        try {
            File baseFolder = JmeSystem.getStorageFolder(storageType);
            if (baseFolder == null) {
                Logger.getLogger(SaveGame.class.getName()).log(Level.SEVERE, "Error reading base storage folder!");
                return null;
            }
            File file = new File(baseFolder.getAbsolutePath() + File.separator + gamePath.replace('/', File.separatorChar) + File.separator + dataName);
            if(!file.exists()){
                return null;
            }
            is = new GZIPInputStream(new BufferedInputStream(new FileInputStream(file)));
            BinaryImporter imp = BinaryImporter.getInstance();
            if (manager != null) {
                imp.setAssetManager(manager);
            }
            sav = imp.load(is);
            Logger.getLogger(SaveGame.class.getName()).log(Level.FINE, "Loading data from: {0}", file.getAbsolutePath());
        } catch (IOException ex) {
            Logger.getLogger(SaveGame.class.getName()).log(Level.SEVERE, "Error loading data: {0}", ex);
            ex.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    Logger.getLogger(SaveGame.class.getName()).log(Level.SEVERE, "Error loading data: {0}", ex);
                    ex.printStackTrace();
                }
            }
        }
        return sav;
    }
}
