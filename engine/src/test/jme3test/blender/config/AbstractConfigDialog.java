package jme3test.blender.config;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;

/**
 * A base class for defining the start configuration.
 * This separates the gui window definition from its logic.
 * It is made so to easier maintain the dialog window.
 * @author Marcin Roguski (Kaelthas)
 */
public abstract class AbstractConfigDialog extends JDialog {

    private static final long serialVersionUID = -3677493125861310310L;
    private static final Logger LOGGER = Logger.getLogger(AbstractConfigDialog.class.getName());
    protected JComboBox jComboBoxVersionSelection;
    protected JList jListBlenderFiles;
    protected JTable jTableProperties;
    protected JTable jTableAnimations;
    protected JButton jButtonAddAnimation;
    protected JButton jButtonRemoveAnimation;
    protected JCheckBox jCheckBoxUseModelKey;
    protected JButton jButtonOK;
    protected JButton jButtonCancel;

    /**
     * Cionstructor initializes the gui.
     */
    public AbstractConfigDialog() {
    	super((Frame)null, true);
        this.init();
    }

    /**
     * This method initializes the window.
     */
    private void init() {
        try {//setting the system Look And Feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (InstantiationException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (UnsupportedLookAndFeelException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setLocationByPlatform(true);

        this.add(this.prepareBlenderFilesAndLogLevelPanel(), BorderLayout.WEST);
        this.add(this.prepareFilePropertiesPanel(), BorderLayout.CENTER);
        this.add(this.prepareButtonsPanel(), BorderLayout.SOUTH);

        this.pack();
    }

    /**
     * This method prepares a swing panel containing the list of blender files
     * and log level chooser.
     * @return prepared swing panel
     */
    private JPanel prepareBlenderFilesAndLogLevelPanel() {
        JPanel jPanelBlenderFilesListAndLogLevel = new JPanel();
        jPanelBlenderFilesListAndLogLevel.setBorder(new TitledBorder("Blender test files"));
        jPanelBlenderFilesListAndLogLevel.setLayout(new BorderLayout());

        //blender version selection combo box
        jComboBoxVersionSelection = new JComboBox(new DefaultComboBoxModel());
        jComboBoxVersionSelection.setEditable(false);
        jPanelBlenderFilesListAndLogLevel.add(jComboBoxVersionSelection, BorderLayout.NORTH);

        //blender list files
        jListBlenderFiles = new JList(new DefaultListModel());
        jListBlenderFiles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroller = new JScrollPane(jListBlenderFiles);
        jPanelBlenderFilesListAndLogLevel.add(listScroller, BorderLayout.CENTER);

        //Log Level list
        Box box = Box.createVerticalBox();
        box.add(new Label("Log level:"));
        ButtonGroup buttonGroup = new ButtonGroup();

        Level[] levels = new Level[]{Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO,
            Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.ALL};
        for (Level level : levels) {
            JRadioButtonLevel jRadioButtonLevel = new JRadioButtonLevel(level);
            buttonGroup.add(jRadioButtonLevel);
            box.add(jRadioButtonLevel);
        }
        jPanelBlenderFilesListAndLogLevel.add(box, BorderLayout.SOUTH);

        return jPanelBlenderFilesListAndLogLevel;
    }

    /**
     * This method prepares a swing panel containing the file's animations.
     * @return prepared swing panel
     */
    protected JPanel prepareFilePropertiesPanel() {
        //properties table
        JPanel jPanelProperties = new JPanel();
        jPanelProperties.setBorder(new EmptyBorder(new Insets(0, 5, 0, 5)));
        jPanelProperties.setLayout(new BorderLayout());
        jPanelProperties.add(new JLabel("Properties"), BorderLayout.NORTH);

        jTableProperties = new JTable();
        jTableProperties.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        jTableProperties.setModel(new BlenderTableModel(new Object[]{"Name", "Value"}));
        jTableProperties.getColumnModel().getColumn(1).setCellEditor(new BlenderTableCellEditor());
        JScrollPane jScrollPaneProperties = new JScrollPane(jTableProperties);
        jTableProperties.setFillsViewportHeight(true);

        jPanelProperties.add(jScrollPaneProperties, BorderLayout.CENTER);

        //animations table
        JPanel jPanelAnimations = new JPanel();
        jPanelAnimations.setBorder(new EmptyBorder(new Insets(0, 5, 0, 5)));
        jPanelAnimations.setLayout(new BorderLayout());
        jPanelAnimations.add(new JLabel("Animations"), BorderLayout.NORTH);

        jTableAnimations = new AnimationsTable();
        JScrollPane jScrollPaneAnimations = new JScrollPane(jTableAnimations);
        jTableAnimations.setFillsViewportHeight(true);

        JPanel jPanelTableButtons = new JPanel();
        jPanelTableButtons.setLayout(new FlowLayout(FlowLayout.LEFT));
        jButtonAddAnimation = new JButton("Add animation");
        jButtonAddAnimation.setEnabled(false);
        jButtonRemoveAnimation = new JButton("Remove animation");
        jButtonRemoveAnimation.setEnabled(false);
        jPanelTableButtons.add(jButtonAddAnimation);
        jPanelTableButtons.add(jButtonRemoveAnimation);

        jPanelAnimations.add(jScrollPaneAnimations, BorderLayout.CENTER);
        jPanelAnimations.add(jPanelTableButtons, BorderLayout.SOUTH);

        //model key check-box
        jCheckBoxUseModelKey = new JCheckBox();
        jCheckBoxUseModelKey.setText("Use ModelKey to start the test");
        jCheckBoxUseModelKey.setToolTipText("All BlenderKey settings will remain here, but the application will be "
                + "started using a model key. So only the path to the file will be given!");

        //building the result panel
        JPanel jPanelResult = new JPanel();
        jPanelResult.setBorder(new TitledBorder("Loading properties"));
        jPanelResult.setLayout(new BorderLayout());

        jPanelResult.add(jPanelProperties, BorderLayout.WEST);
        jPanelResult.add(jPanelAnimations, BorderLayout.CENTER);
        jPanelResult.add(jCheckBoxUseModelKey, BorderLayout.SOUTH);
        return jPanelResult;
    }

    /**
     * This method prepares a swing panel containing the buttons.
     * @return prepared swing panel
     */
    protected JPanel prepareButtonsPanel() {
        JPanel jPanelButtons = new JPanel();
        jButtonOK = new JButton("OK");
        jButtonOK.setEnabled(false);
        jPanelButtons.add(jButtonOK);
        jButtonCancel = new JButton("Cancel");
        jPanelButtons.add(jButtonCancel);
        return jPanelButtons;
    }

    /**
     * This class was made only to make the selection of a level radio button easier.
     * @author Marcin Roguski
     */
    protected static class JRadioButtonLevel extends JRadioButton {

        private static final long serialVersionUID = 8874525909060993518L;
        private static Level selectedLevel;
        private static Map<Level, JRadioButtonLevel> radioButtons = new HashMap<Level, AbstractConfigDialog.JRadioButtonLevel>();
        private Level level;

        /**
         * Constructor. Creates the radio button.
         * Stores it inside the buttons map.
         * @param level the level of log info
         */
        public JRadioButtonLevel(Level level) {
            super(level.getName());
            this.level = level;
            radioButtons.put(level, this);
            this.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    JRadioButtonLevel.selectedLevel = JRadioButtonLevel.this.level;
                }
            });
        }

        /**
         * This method returns the currently selected info level.
         * @return currently selected info level
         */
        public static Level getSelectedLevel() {
            return selectedLevel;
        }

        /**
         * This method sets the current info level.
         * @param level the current info level
         */
        public static synchronized void setSelectedLevel(Level level) {
            radioButtons.get(level).setSelected(true);
            selectedLevel = level;
        }
    }

    /**
     * This class is an item that should be stored in the files' list.
     * @author Marcin Roguski (Kaelthas)
     */
    protected static class FileListItem {

        private File file;				//the file to be stored

        /**
         * Constructore. Stores the given file.
         * @param file the file to be stored
         */
        public FileListItem(File file) {
            this.file = file;
        }

        /**
         * This method returns the stored file.
         * @return the stored file
         */
        public File getFile() {
            return file;
        }

        @Override
        public String toString() {
            return file.getName();
        }
    }

    /**
     * The model for properties table.
     * It makes left column ineditable.
     * @author Marcin Roguski (Kaelthas)
     */
    protected static class BlenderTableModel extends DefaultTableModel {

        private static final long serialVersionUID = -4211206550875326553L;

        /**
         * Constructor only calls super-constuctor.
         * @param columnNames the names of table columns
         */
        public BlenderTableModel(Object[] columnNames) {
            super(columnNames, 0);
        }

        @Override
        public void addRow(Object[] rowData) {
            for (int i = 0; i < rowData.length; ++i) {
                if (rowData[i] == null) {
                    rowData[i] = "";
                }
            }
            super.addRow(rowData);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column > 0;
        }
    }

    /**
     * A cell editor tah improves data input to the table.
     * @author Marcin Roguski (Kaelthas)
     */
    protected static class BlenderTableCellEditor extends AbstractCellEditor implements TableCellEditor {

        private static final long serialVersionUID = -8601975203921608519L;
        private JCheckBox jCheckBox = new JCheckBox();
        private JTextField jTextField = new JTextField();
        private JComboBox jComboBox = new JComboBox();
        private Object lastValue;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.lastValue = value;
            if (value instanceof Boolean) {
                jCheckBox.setSelected(((Boolean) value).booleanValue());
                return jCheckBox;
            } else if (value instanceof String || value instanceof Number || value instanceof Character) {
                jTextField.setText(value.toString());
                return jTextField;
            } else if (value instanceof Enum<?>) {
                DefaultComboBoxModel defaultComboBoxModel = (DefaultComboBoxModel) jComboBox.getModel();
                defaultComboBoxModel.removeAllElements();
                for (Object object : value.getClass().getEnumConstants()) {
                    defaultComboBoxModel.addElement(object);
                }
                return jComboBox;
            } else {
                jTextField.setText(value == null ? "" : value.toString());
                return jTextField;
            }
        }

        @Override
        public Object getCellEditorValue() {
            if (lastValue instanceof Boolean) {
                return Boolean.valueOf(jCheckBox.isSelected());
            } else if (lastValue instanceof String) {
                return jTextField.getText();
            } else if (lastValue instanceof Character) {
                return Character.valueOf(jTextField.getText().charAt(0));
            } else if (lastValue instanceof Byte) {
                return Byte.valueOf(jTextField.getText());
            } else if (lastValue instanceof Short) {
                return Short.valueOf(jTextField.getText());
            } else if (lastValue instanceof Integer) {
                return Integer.valueOf(jTextField.getText());
            } else if (lastValue instanceof Long) {
                return Long.valueOf(jTextField.getText());
            } else if (lastValue instanceof Float) {
                return Float.valueOf(jTextField.getText());
            } else if (lastValue instanceof Double) {
                return Double.valueOf(jTextField.getText());
            } else if (lastValue instanceof Enum<?>) {
                return jComboBox.getSelectedItem();
            }
            //TODO: savable objects
            return null;
        }
    }
}
