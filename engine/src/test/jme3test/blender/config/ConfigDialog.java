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
import javax.swing.SwingUtilities;
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
    private File configFile;								//the config file
    private Map<String, BlenderKeyConfiguration> configMap;	//the blender key configuration map
    private BlenderKeyConfiguration blenderKeyConfiguration;//the configuration for the files
    private ConfigExecutable configExecutable;		//this is called after clicking the 'OK' button

    /**
     * Constructor. Builds the whole window and stores its data.
     * @param testAssetsFolderName the path to test files folder
     */
    public ConfigDialog(String baseFolderName, ConfigExecutable configExecutable) {
        if (baseFolderName == null) {
            throw new IllegalArgumentException("No test asset folder given!");
        }
        if (configExecutable == null) {
            throw new IllegalArgumentException("No config executable given!");
        }
        this.baseFolderName = baseFolderName;
        this.configExecutable = configExecutable;
        this.configMap = new HashMap<String, ConfigDialog.BlenderKeyConfiguration>();

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
            configMap.put(folder.getName(), null);
        }
        this.initListeners();

        jComboBoxVersionSelection.setSelectedIndex(0);
    }

    /**
     * This method returns the selected blender key.
     * @return the selected blender key
     */
    public BlenderKey getSelectedBlenderKey() {
        return blenderKeyConfiguration.lastUsedKey;
    }

    /**
     * This method prepares the blender files' list.
     * @param testAssetsFolderName the path to test files folder
     * @return array of blender files
     */
    private File[] prepareFilesList(String testAssetsFolderName) {
        File testAssetsFolder = new File(testAssetsFolderName);

        //loading blender files
        File[] blenderFiles = testAssetsFolder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isFile() && file.canRead() && file.getName().endsWith(".blend");
            }
        });

        //loading the blender files configuration
        File[] files = testAssetsFolder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isFile() && file.canRead() && file.getName().endsWith(".conf");
            }
        });
        if (files == null || files.length == 0) {
            blenderKeyConfiguration = new BlenderKeyConfiguration(blenderFiles.length);

        } else {
            BinaryImporter jmeImporter = new BinaryImporter();
            String instructionToUser = files.length == 1
                    ? "No other config file to load! No configuration set!"
                    : "Please choose different config file!";
            do {
                if (files.length > 1) {
                    configFile = (File) JOptionPane.showInputDialog(null, "Choose the config file!", "Config file selection",
                            JOptionPane.INFORMATION_MESSAGE, null, files, files[0]);
                } else {
                    configFile = files[0];
                }
                if (configFile == null) {
                    JOptionPane.showMessageDialog(this, "No config file selected!\nEmpty configuration will be created!",
                            "No configuration selected", JOptionPane.INFORMATION_MESSAGE);
                    blenderKeyConfiguration = new BlenderKeyConfiguration(blenderFiles.length);
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
            } while (blenderKeyConfiguration == null && files.length > 1);
        }
        configFile = new File(testAssetsFolder, "test.conf");

        jCheckBoxUseModelKey.setSelected(blenderKeyConfiguration.useModelKey);

        //enlisting the files in the list
        DefaultListModel defaultListModel = (DefaultListModel) jListBlenderFiles.getModel();
        defaultListModel.removeAllElements();
        for (int i = 0; i < blenderFiles.length; ++i) {
            defaultListModel.addElement(new FileListItem(blenderFiles[i]));
        }
        return blenderFiles;
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
        if (configuration.lastUsedKey != null) {//reading animations
            DefaultTableModel animationsTableModel = (DefaultTableModel) jTableAnimations.getModel();
            if (configuration.lastUsedKey.getAnimations() != null) {
                configuration.lastUsedKey.getAnimations().clear();
            }
            int animCounter = 0;
            for (int i = 0; i < animationsTableModel.getRowCount(); ++i) {
                String objectName = (String) animationsTableModel.getValueAt(i, 0);
                String animName = (String) animationsTableModel.getValueAt(i, 1);
                Number startFrame = (Number) animationsTableModel.getValueAt(i, 2);
                Number stopFrame = (Number) animationsTableModel.getValueAt(i, 3);
                if (objectName != null && animName != null && startFrame.intValue() <= stopFrame.intValue()) {
                    configuration.lastUsedKey.addAnimation(objectName, animName, startFrame.intValue(), stopFrame.intValue());
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
            if (!jmeExporter.save(configuration, configFile)) {
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
                //save the previous congifuration
                if (blenderKeyConfiguration != null) {
                    ConfigDialog.this.storeConfig(blenderKeyConfiguration);
                    blenderKeyConfiguration = null;
                }

                //load new configuration
                File[] blenderFiles = ConfigDialog.this.prepareFilesList(baseFolderName + '/' + jComboBoxVersionSelection.getSelectedItem().toString());
                if (blenderKeyConfiguration.lastUsedKey != null) {
                    for (int i = 0; i < blenderFiles.length; ++i) {
                        if (blenderFiles[i].getPath().equalsIgnoreCase(blenderKeyConfiguration.lastUsedKey.getName())) {
                            jListBlenderFiles.setSelectedIndex(i);
                            break;
                        }
                    }
                }
                if (blenderKeyConfiguration.logLevel == null) {
                    blenderKeyConfiguration.logLevel = Level.INFO;
                }
                JRadioButtonLevel.setSelectedLevel(blenderKeyConfiguration.logLevel);
            }
        });
        //selection of the file changes the config on the right
        jListBlenderFiles.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent evt) {
                BlenderKeyConfiguration config = ConfigDialog.this.blenderKeyConfiguration;
                FileListItem selectedItem = (FileListItem) ConfigDialog.this.jListBlenderFiles.getSelectedValue();
                if (selectedItem != null) {
                    String fileName = selectedItem.getFile().getName();
                    config.lastUsedKey = config.blenderKeys.get(fileName);
                    if (config.lastUsedKey == null) {
                        config.lastUsedKey = new BlenderKey(selectedItem.getFile().getPath());
                        config.blenderKeys.put(fileName, config.lastUsedKey);
                    }
                    ConfigDialog.this.setBlenderKey(config.lastUsedKey);
                } else {
                    config.lastUsedKey = null;
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
                    try {
                        Field field = config.lastUsedKey.getClass().getDeclaredField(name);
                        field.setAccessible(true);
                        field.set(config.lastUsedKey, value);
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

        //button listeners
        jButtonOK.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                ConfigDialog.this.storeConfig(blenderKeyConfiguration);
                //running the test
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        configExecutable.execute(ConfigDialog.this.blenderKeyConfiguration.getKeyToUse(),
                                ConfigDialog.this.blenderKeyConfiguration.logLevel);
                    }
                });
                //disposing the config window
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

        private Map<String, BlenderKey> blenderKeys;
        private BlenderKey lastUsedKey;
        private Level logLevel;
        private boolean useModelKey;

        /**
         * Constructor for jme serialization.
         */
        public BlenderKeyConfiguration() {
        }

        /**
         * Constructor that creates new empty configuration for every blender file.
         * @param blenderFilesAmount the amount of blender files
         */
        public BlenderKeyConfiguration(int blenderFilesAmount) {
            blenderKeys = new HashMap<String, BlenderKey>(blenderFilesAmount);
        }

        /**
         * This method returns the key that will be used during the test.
         * @return the key that will be used during the test
         */
        public ModelKey getKeyToUse() {
            return useModelKey ? new ModelKey(lastUsedKey.getName()) : lastUsedKey;
        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule oc = ex.getCapsule(this);
            oc.writeStringSavableMap(blenderKeys, "keys", null);
            oc.write(lastUsedKey, "last-key", null);
            oc.write(useModelKey, "use-model-key", false);
            oc.write(logLevel == null ? null : logLevel.getName(), "log-level", Level.INFO.getName());
        }

        @Override
        @SuppressWarnings("unchecked")
        public void read(JmeImporter im) throws IOException {
            InputCapsule ic = im.getCapsule(this);
            blenderKeys = (Map<String, BlenderKey>) ic.readStringSavableMap("keys", null);
            lastUsedKey = (BlenderKey) ic.readSavable("last-key", null);
            useModelKey = ic.readBoolean("use-model-key", false);
            String logLevelName = ic.readString("log-level", Level.INFO.getName());
            logLevel = logLevelName == null ? Level.INFO : Level.parse(logLevelName);
        }
    }
}
