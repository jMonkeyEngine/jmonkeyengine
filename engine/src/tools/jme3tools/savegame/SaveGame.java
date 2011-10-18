/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3tools.savegame;

import com.jme3.asset.AssetManager;
import com.jme3.export.Savable;
import com.jme3.export.xml.XMLExporter;
import com.jme3.export.xml.XMLImporter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Tool for saving Savables as SaveGame entries in a system-dependent way.
 * @author normenhansen
 */
public class SaveGame {

    /**
     * Saves a savable in a system-dependent way.
     * @param gamePath A unique path for this game, e.g. com/mycompany/mygame
     * @param dataName A unique name for this savegame, e.g. "save_001"
     * @param data The Savable to save
     */
    public static void saveGame(String gamePath, String dataName, Savable data) {
        Preferences prefs = Preferences.userRoot().node(gamePath);
        XMLExporter ex = XMLExporter.getInstance();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ex.save(data, out);
        } catch (IOException ex1) {
            Logger.getLogger(SaveGame.class.getName()).log(Level.SEVERE, "Error saving data: {0}", ex1);
            ex1.printStackTrace();
        }
        try {
            prefs.put(dataName, out.toString("UTF-8"));
        } catch (UnsupportedEncodingException ex1) {
            Logger.getLogger(SaveGame.class.getName()).log(Level.SEVERE, "Error saving data: {0}", ex1);
            ex1.printStackTrace();
        }
    }

    /**
     * Loads a savable that has been saved on this system with saveGame() before.
     * @param gamePath A unique path for this game, e.g. com/mycompany/mygame
     * @param dataName A unique name for this savegame, e.g. "save_001"
     * @return The savable that was saved
     */
    public static Savable loadGame(String gamePath, String dataName) {
        return loadGame(gamePath, dataName, null);
    }

    /**
     * Loads a savable that has been saved on this system with saveGame() before.
     * @param gamePath A unique path for this game, e.g. com/mycompany/mygame
     * @param dataName A unique name for this savegame, e.g. "save_001"
     * @param assetManager Link to an AssetManager if required for loading the data (e.g. models with textures)
     * @return The savable that was saved
     */
    public static Savable loadGame(String gamePath, String dataName, AssetManager manager) {
        Preferences prefs = Preferences.userRoot().node(gamePath);
        String data = prefs.get(gamePath, dataName);
        InputStream is = null;
        Savable sav = null;
        try {
            is = new ByteArrayInputStream(data.getBytes("UTF-8"));
            XMLImporter imp = XMLImporter.getInstance();
            if (manager != null) {
                imp.setAssetManager(manager);
            }
            sav = imp.load(is);
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
