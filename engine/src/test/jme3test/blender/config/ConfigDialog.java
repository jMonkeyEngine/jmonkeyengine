/**
 * 
 */
package jme3test.blender.config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import com.jme3.asset.BlenderKey;
import com.jme3.asset.ModelKey;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;

/**
 * A class that shows a dialog box for blender testing configuration.
 * @author Marcin Roguski (Kaelthas)
 */
public class ConfigDialog extends AbstractConfigDialog {

    private static final long serialVersionUID = 2863364888664674247L;
    private static final Logger LOGGER = Logger.getLogger(ConfigDialog.class.getName());
    private String baseFolderName;
    private BlenderKeyConfiguration blenderKeyConfiguration;//the configuration for the files

    /**
     * Constructor. Builds the whole window and stores its data.
     * @param baseFolderName base folder for test assets
     */
    public ConfigDialog(String baseFolderName) {
        if (baseFolderName == null) {
            throw new IllegalArgumentException("No test asset folder given!");
        }
        this.baseFolderName = baseFolderName;

        //setting up version selection (as a folder list in a compo box)
        File baseFolder = new File(baseFolderName);
        if (!baseFolder.exists() || !baseFolder.isDirectory()) {
            throw new IllegalArgumentException("The given base folder path either does not exists or does not point to a directory!");
        }
        File[] folders = baseFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && file.getName().charAt(0) != '.';
            }
        });
        for (File folder : folders) {
            ((DefaultComboBoxModel) jComboBoxVersionSelection.getModel()).addElement(folder.getName());
        }
        this.loadConfiguration();
        this.applyConfiguration();
        
        this.initListeners();
        this.setVisible(true);
    }
    
    /**
     * This method returns the blender key configuration.
     * @return the blender key configuration
     */
    public BlenderKeyConfiguration getBlenderKeyConfiguration() {
		return blenderKeyConfiguration;
	}

    /**
     * This method loades the configuration.
     * It stores the data into swing gui elements and enlists the proper blender files.
     */
    private void loadConfiguration() {
    	File baseFolder = new File(baseFolderName);

        //loading the blender files configuration
        File[] configFiles = baseFolder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isFile() && file.canRead() && file.getName().endsWith(".conf");
            }
        });
        if (configFiles == null || configFiles.length == 0) {
            blenderKeyConfiguration = new BlenderKeyConfiguration();
        } else {
            BinaryImporter jmeImporter = new BinaryImporter();
            String instructionToUser = configFiles.length == 1
                    ? "No other config file to load! No configuration set!"
                    : "Please choose different config file!";
            do {
            	File configFile;
                if (configFiles.length > 1) {
                    configFile = (File) JOptionPane.showInputDialog(null, "Choose the config file!", "Config file selection",
                            JOptionPane.INFORMATION_MESSAGE, null, configFiles, configFiles[0]);
                } else {
                    configFile = configFiles[0];
                }
                if (configFile == null) {
                    JOptionPane.showMessageDialog(this, "No config file selected!\nEmpty configuration will be created!",
                            "No configuration selected", JOptionPane.INFORMATION_MESSAGE);
                    blenderKeyConfiguration = new BlenderKeyConfiguration();
                } else {
                    try {
                        Savable loadedData = jmeImporter.load(configFile);
                        if (loadedData instanceof BlenderKeyConfiguration) {
                            blenderKeyConfiguration = (BlenderKeyConfiguration) loadedData;
                        } else {
                            LOGGER.warning("Cannot load data drom the given file!");
                            JOptionPane.showMessageDialog(this, "The data stored in the config file is of invalid type!\n"
                                    + instructionToUser, "Config data error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(this, "Unable to load configuration! Reason: " + e.getLocalizedMessage(),
                                "Loading data error", JOptionPane.ERROR_MESSAGE);
                        LOGGER.severe("Unable to load configuration");
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "Unable to load configuration!",
                                "Loading data error", JOptionPane.ERROR_MESSAGE);
                        LOGGER.log(Level.SEVERE, "Unable to load configuration due to unpredicted error!", e);
                    }
                }
            } while (blenderKeyConfiguration == null && configFiles.length > 1);
        }
    }
    
    /**
     * This method applies the loaded configuration.
     */
    private void applyConfiguration() {
    	//applying configuration to gui components
    	jCheckBoxUseModelKey.setSelected(blenderKeyConfiguration.useModelKey);
        if(blenderKeyConfiguration.lastVersionUsed != null) {
        	jComboBoxVersionSelection.setSelectedItem(blenderKeyConfiguration.lastVersionUsed);
        } else {
        	jComboBoxVersionSelection.setSelectedIndex(0);
        	blenderKeyConfiguration.lastVersionUsed = (String)jComboBoxVersionSelection.getSelectedItem();
        }
        JRadioButtonLevel.setSelectedLevel(blenderKeyConfiguration.logLevel);
        
        //enlisting the files in the list
        this.reloadFilesList();
    }
    
    /**
     * This method prepares the blender files' list and selects the blender key that was last selected
     * for this gorup of assets.
     */
    private void reloadFilesList() {
    	File testAssetsFolder = new File(baseFolderName + '/' + blenderKeyConfiguration.lastVersionUsed);
        File[] blenderFiles = testAssetsFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && file.canRead() && file.getName().endsWith(".blend");
            }
        });
        BlenderKey lastUsedKey = blenderKeyConfiguration.lastUsedKey.get(blenderKeyConfiguration.lastVersionUsed);
        String lastFileUsed = null;
        if(lastUsedKey!=null) {
        	lastFileUsed = lastUsedKey.getName().substring(lastUsedKey.getName().lastIndexOf('/') + 1);
        }
        DefaultListModel defaultListModel = (DefaultListModel) jListBlenderFiles.getModel();
        defaultListModel.removeAllElements();
        for (int i = 0; i < blenderFiles.length; ++i) {
            defaultListModel.addElement(new FileListItem(blenderFiles[i]));
            if(lastFileUsed != null && lastFileUsed.equals(blenderFiles[i].getName())) {
            	jListBlenderFiles.setSelectedIndex(i);
            	this.setBlenderKey(lastUsedKey);
            }
        }
    }

    /**
     * This method fills the properties panel with blender key data.
     * @param blenderKey the belnder key data
     */
    private void setBlenderKey(BlenderKey blenderKey) {
        //setting properties
        BlenderTableModel propertiesModel = (BlenderTableModel) jTableProperties.getModel();
        int rowCount = propertiesModel.getRowCount();
        for (int i = 0; i < rowCount; ++i) {
            propertiesModel.removeRow(0);
        }
        Field[] fields = blenderKey.getClass().getDeclaredFields();
        for (Field field : fields) {

            field.setAccessible(true);
            if (!"animations".equalsIgnoreCase(field.getName())
                    && (field.getModifiers() & Modifier.STATIC) == 0) {
                try {
                    propertiesModel.addRow(new Object[]{field.getName(), field.get(blenderKey)});
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }

        //setting animations
        DefaultTableModel animationsModel = (DefaultTableModel) jTableAnimations.getModel();
        rowCount = animationsModel.getRowCount();
        for (int i = 0; i < rowCount; ++i) {
            animationsModel.removeRow(0);
        }
        Map<String, Map<String, int[]>> animations = blenderKey.getAnimations();
        if (animations != null) {
            for (Entry<String, Map<String, int[]>> animationEntry : animations.entrySet()) {
                for (Entry<String, int[]> animDataEntry : animationEntry.getValue().entrySet()) {
                    int[] frames = animDataEntry.getValue();
                    animationsModel.addRow(new Object[]{animationEntry.getKey(), animDataEntry.getKey(),
                                Integer.valueOf(frames[0]), Integer.valueOf(frames[1])});
                }
            }
        }

        this.jButtonOK.setEnabled(true);
        this.jButtonOK.requestFocusInWindow();
        this.jButtonAddAnimation.setEnabled(true);
    }

    /**
     * This method stores the current blender config.
     * @param configuration the blender config to store
     */
    private void storeConfig(BlenderKeyConfiguration configuration) {
    	BlenderKey blenderKey = configuration.lastUsedKey.get(configuration.lastVersionUsed);
        if (blenderKey != null) {//reading animations
            DefaultTableModel animationsTableModel = (DefaultTableModel) jTableAnimations.getModel();
            if (blenderKey.getAnimations() != null) {
            	blenderKey.getAnimations().clear();
            }
            int animCounter = 0;
            for (int i = 0; i < animationsTableModel.getRowCount(); ++i) {
                String objectName = (String) animationsTableModel.getValueAt(i, 0);
                String animName = (String) animationsTableModel.getValueAt(i, 1);
                Number startFrame = (Number) animationsTableModel.getValueAt(i, 2);
                Number stopFrame = (Number) animationsTableModel.getValueAt(i, 3);
                if (objectName != null && animName != null && startFrame.intValue() <= stopFrame.intValue()) {
                	blenderKey.addAnimation(objectName, animName, startFrame.intValue(), stopFrame.intValue());
                    ++animCounter;
                }
            }
            if (animCounter < animationsTableModel.getRowCount()) {
                JOptionPane.showMessageDialog(ConfigDialog.this, "Some animations had errors!\nThey had not been added!",
                        "Invalid animations definitions", JOptionPane.WARNING_MESSAGE);
            }
        }
        //getting the key type
        configuration.useModelKey = jCheckBoxUseModelKey.isSelected();
        configuration.logLevel = JRadioButtonLevel.getSelectedLevel();

        //storing the config
        JmeExporter jmeExporter = new BinaryExporter();
        try {
            if (!jmeExporter.save(configuration, new File(baseFolderName, "test.conf"))) {
                JOptionPane.showMessageDialog(ConfigDialog.this, "Unable to save the config data!", "Config save problem", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(ConfigDialog.this, "Error occured during config saving!\nReason: " + e.getLocalizedMessage(),
                    "Config save problem", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This method initiates components listeners.
     */
    private void initListeners() {
        //selection of blender version
        jComboBoxVersionSelection.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
            	blenderKeyConfiguration.lastVersionUsed = jComboBoxVersionSelection.getSelectedItem().toString();
                ConfigDialog.this.reloadFilesList();
            }
        });
        //selection of the file changes the config on the right
        jListBlenderFiles.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent evt) {
                BlenderKeyConfiguration config = ConfigDialog.this.blenderKeyConfiguration;
                FileListItem selectedItem = (FileListItem) ConfigDialog.this.jListBlenderFiles.getSelectedValue();
                String blenderVersion = config.lastVersionUsed;
                if (selectedItem != null) {
                    String blenderFileName = selectedItem.getFile().getName();
                    BlenderKey blenderKey = null;
                    Map<String, BlenderKey> blenderKeys = config.blenderKeys.get(blenderVersion);
                    if(blenderKeys != null) {
                    	blenderKey = blenderKeys.get(blenderFileName);
                    	if(blenderKey == null) {
                    		blenderKey = new BlenderKey(ConfigDialog.this.baseFolderName+'/' + blenderVersion + '/' + blenderFileName);
                    	}
                    } else {
                    	blenderKeys = new HashMap<String, BlenderKey>();
                    	blenderKey = new BlenderKey(ConfigDialog.this.baseFolderName+'/' + blenderVersion + '/' + blenderFileName);
                    	blenderKeys.put(blenderFileName, blenderKey);
                    	config.blenderKeys.put(blenderVersion, blenderKeys);
                    }
                    config.lastUsedKey.put(blenderVersion, blenderKey);
                    ConfigDialog.this.setBlenderKey(config.lastUsedKey.get(blenderVersion));
                } else {
                	config.lastUsedKey.put(blenderVersion, null);
                }
            }
        });
        jTableProperties.getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent evt) {
                if (evt.getType() == TableModelEvent.UPDATE) {
                    BlenderKeyConfiguration config = ConfigDialog.this.blenderKeyConfiguration;
                    int row = evt.getFirstRow();
                    String name = (String) jTableProperties.getModel().getValueAt(row, 0);
                    Object value = jTableProperties.getModel().getValueAt(row, 1);
                    BlenderKey blenderKey = config.lastUsedKey.get(config.lastVersionUsed);
                    try {
                        Field field = blenderKey.getClass().getDeclaredField(name);
                        field.setAccessible(true);
                        field.set(blenderKey, value);
                    } catch (IllegalArgumentException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    } catch (SecurityException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    } catch (IllegalAccessException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    } catch (NoSuchFieldException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        });
        jTableAnimations.getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent evt) {
                if (evt.getType() == TableModelEvent.INSERT) {
                    jButtonRemoveAnimation.setEnabled(true);
                } else if (evt.getType() == TableModelEvent.DELETE && jTableAnimations.getModel().getRowCount() == 0) {
                    jButtonRemoveAnimation.setEnabled(false);
                }
            }
        });
        jButtonAddAnimation.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                ((DefaultTableModel) jTableAnimations.getModel()).addRow(new Object[]{"", "", Integer.valueOf(-1), Integer.valueOf(-1)});
            }
        });
        jButtonRemoveAnimation.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                int row = jTableAnimations.getSelectedRow();
                if (row >= 0) {
                    ((DefaultTableModel) jTableAnimations.getModel()).removeRow(row);
                }
            }
        });
        jButtonOK.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                ConfigDialog.this.storeConfig(blenderKeyConfiguration);
                ConfigDialog.this.dispose();
            }
        });
        jButtonCancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                ConfigDialog.this.dispose();
            }
        });
    }

    /**
     * This class holds the configuration for all the files.
     * It can be saved and loaded using jme mechanisms.
     * @author Marcin Roguski (Kaelthas)
     */
    public static class BlenderKeyConfiguration implements Savable {
    	/**
    	 * The key is a directory of blender_version_folder.
    	 * The value is the map between the blender file name and its blender key.
    	 */
        private Map<String, Map<String, BlenderKey>> blenderKeys;
        /** The last version of blender opened. */
        private String lastVersionUsed;
        /** The last used blender key for each blender version. */
        private Map<String, BlenderKey> lastUsedKey;
        /** Last used log level. */
        private Level logLevel;
        /** This variable tells if the model or blender loader is used. */
        private boolean useModelKey;
        
        /**
         * Constructor that creates new empty configuration for every blender file.
         * Also used for jme serialization.
         */
        public BlenderKeyConfiguration() {
        	blenderKeys = new HashMap<String, Map<String, BlenderKey>>();
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
            lastUsedKey = (Map<String, BlenderKey>) ic.readStringSavableMap("last-key", null);
            useModelKey = ic.readBoolean("use-model-key", false);
            String logLevelName = ic.readString("log-level", Level.INFO.getName());
            logLevel = logLevelName == null ? Level.INFO : Level.parse(logLevelName);
            lastVersionUsed = ic.readString("versionUsed", null);
        }
    }
}
