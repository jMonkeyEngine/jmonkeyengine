package jme3test.blender.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.jme3.asset.BlenderKey;
import com.jme3.asset.ModelKey;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;

/**
 * This class holds the configuration for all the files.
 * It can be saved and loaded using jme mechanisms.
 * @author Marcin Roguski (Kaelthas)
 */
public class BlenderKeyConfiguration implements Savable {
	/**
	 * The key is a directory of blender_version_folder.
	 * The value is the map between the blender file name and its blender key.
	 */
    /*package*/ Map<String, Map<String, BlenderKey>> blenderKeys;
    /** List of selected animations for each object. An object can have one active animations.*/
    /*package*/ Map<String, List<String[]>> selectedAnimations;
    /** The last version of blender opened. */
    /*package*/ String lastVersionUsed;
    /** The last used blender key for each blender version. */
    /*package*/ Map<String, BlenderKey> lastUsedKey;
    /** Last used log level. */
    /*package*/ Level logLevel;
    /** This variable tells if the model or blender loader is used. */
    /*package*/ boolean useModelKey;
    
    /**
     * Constructor that creates new empty configuration for every blender file.
     * Also used for jme serialization.
     */
    public BlenderKeyConfiguration() {
    	blenderKeys = new HashMap<String, Map<String, BlenderKey>>();
    	selectedAnimations = new HashMap<String, List<String[]>>();
    	lastUsedKey = new HashMap<String, BlenderKey>();
    	logLevel = Level.INFO;
	}

    /**
     * This method returns the name of the last used asset folder.
     * @return the name of the last used asset folder
     */
    public String getLastVersionUsed() {
		return lastVersionUsed;
	}
    
    /**
     * This method returns the log level of jme app.
     * @return the log level of jme app
     */
    public Level getLogLevel() {
		return logLevel;
	}
    
    /**
     * This method returns the key that will be used during the test.
     * @return the key that will be used during the test
     */
    public ModelKey getKeyToUse() {
        return useModelKey ? new ModelKey(lastUsedKey.get(lastVersionUsed).getName()) : lastUsedKey.get(lastVersionUsed);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(blenderKeys.size(), "versions-count", 0);
        int i=0;
        for(Entry<String, Map<String, BlenderKey>> entry : blenderKeys.entrySet()) {
        	oc.write(entry.getKey(), "key" + i, null);
        	oc.writeStringSavableMap(entry.getValue(), "value" + i++, null);
        }
        oc.writeStringSavableMap(lastUsedKey, "last-key", null);
        if(selectedAnimations==null) {
        	oc.write(0, "selected-animations-count", 0);
        } else {
        	i = 0;
        	oc.write(selectedAnimations.size(), "selected-animations-count", 0);
        	for(Entry<String, List<String[]>> entry : selectedAnimations.entrySet()) {
        		oc.write(entry.getKey(), "animKey" + i, null);
        		oc.write(entry.getValue().toArray(new String[selectedAnimations.size()][]), "animVal" + i++, null);
        	}
        }
        oc.write(useModelKey, "use-model-key", false);
        oc.write(logLevel == null ? null : logLevel.getName(), "log-level", Level.INFO.getName());
        oc.write(lastVersionUsed, "versionUsed", null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        int versionsCount = ic.readInt("versions-count", 0);
        for(int i=0;i<versionsCount;++i) {
        	String versionName = ic.readString("key" + i, null);
        	Map<String, BlenderKey> versionBlenderFiles = (Map<String, BlenderKey>) ic.readStringSavableMap("value" + i, null);
        	blenderKeys.put(versionName, versionBlenderFiles);
        }
        
        int selectedAnimCount = ic.readInt("selected-animations-count", 0);
        if(selectedAnimCount > 0) {
        	for(int i=0;i<selectedAnimCount;++i) {
        		String blenderKeyName = ic.readString("animKey" + i, null);
        		String[][] selectedAnimations = ic.readStringArray2D("animVal" + i, null);
        		List<String[]> anims = Arrays.asList(selectedAnimations);
        		this.selectedAnimations.put(blenderKeyName, anims);
        	}
        }
        lastUsedKey = (Map<String, BlenderKey>) ic.readStringSavableMap("last-key", null);
        useModelKey = ic.readBoolean("use-model-key", false);
        String logLevelName = ic.readString("log-level", Level.INFO.getName());
        logLevel = logLevelName == null ? Level.INFO : Level.parse(logLevelName);
        lastVersionUsed = ic.readString("versionUsed", null);
    }
}