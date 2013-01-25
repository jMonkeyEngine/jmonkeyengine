package org.mycompany.wizard.panels;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import org.netbeans.installer.utils.ResourceUtils;
import org.netbeans.installer.utils.StringUtils;
import org.netbeans.installer.utils.SystemUtils;
import org.netbeans.installer.utils.helper.swing.NbiCheckBox;
import org.netbeans.installer.wizard.components.panels.ErrorMessagePanel;
import org.netbeans.installer.wizard.containers.SwingContainer;
import org.netbeans.installer.wizard.ui.SwingUi;
import org.netbeans.installer.wizard.ui.WizardUi;

/**
 *
 * @author Dmitry Lipin
 */
public class ShortcutCreationPanel extends ErrorMessagePanel {

    public ShortcutCreationPanel() {
        setProperty(TITLE_PROPERTY,
                DEFAULT_TITLE);
        setProperty(DESCRIPTION_PROPERTY,
                DEFAULT_DESCRIPTION);

    }

    @Override
    public WizardUi getWizardUi() {
        if (wizardUi == null) {
            wizardUi = new ShortcutCreationPanelUi(this);
        }

        return wizardUi;
    }

    @Override
    public void initialize() {
        super.initialize();
        if(getWizard().getProperty(CREATE_DESKTOP_SHORTCUT_PROPERTY) == null) {
            getWizard().setProperty(CREATE_DESKTOP_SHORTCUT_PROPERTY, "" + true);
        }
        if(getWizard().getProperty(CREATE_START_MENU_SHORTCUT_PROPERTY) == null) {
            getWizard().setProperty(CREATE_START_MENU_SHORTCUT_PROPERTY, "" + true);
        }
    }


    public static class ShortcutCreationPanelUi extends ErrorMessagePanelUi {

        protected ShortcutCreationPanel panel;

        public ShortcutCreationPanelUi(ShortcutCreationPanel panel) {
            super(panel);


            this.panel = panel;
        }

        public SwingUi getSwingUi(SwingContainer container) {
            if (swingUi == null) {
                swingUi = new ShortcutCreationPanelSwingUi(panel, container);
            }

            return super.getSwingUi(container);
        }
    }

    public static class ShortcutCreationPanelSwingUi extends ErrorMessagePanelSwingUi {

        protected ShortcutCreationPanel panel;
        private NbiCheckBox desktopShortcutComboBox;
        private NbiCheckBox startMenuShortcutComboBox;

        public ShortcutCreationPanelSwingUi(
                final ShortcutCreationPanel panel,
                final SwingContainer container) {
            super(panel, container);

            this.panel = panel;

            initComponents();
        }

        // protected ////////////////////////////////////////////////////////////////
        @Override
        protected void initialize() {
            desktopShortcutComboBox.setText(CREATE_DESKTOP_SHORTCUT_NAME);            
            desktopShortcutComboBox.setSelected(false);
            if(Boolean.parseBoolean(panel.getWizard().getProperty(CREATE_DESKTOP_SHORTCUT_PROPERTY))) {
                desktopShortcutComboBox.doClick();
            }

            startMenuShortcutComboBox.setText(
                    SystemUtils.isWindows() ? CREATE_START_MENU_SHORTCUT_NAME_WINDOWS :
                        (SystemUtils.isMacOS() ? CREATE_START_MENU_SHORTCUT_NAME_MAC :
                            CREATE_START_MENU_SHORTCUT_NAME_UNIX));
            startMenuShortcutComboBox.setSelected(false);
            if(Boolean.parseBoolean(panel.getWizard().getProperty(CREATE_START_MENU_SHORTCUT_PROPERTY))) {
                startMenuShortcutComboBox.doClick();
            }

            super.initialize();
        }

        @Override
        protected void saveInput() {
            super.saveInput();
            panel.getWizard().setProperty(
                    CREATE_DESKTOP_SHORTCUT_PROPERTY,
                    StringUtils.EMPTY_STRING + desktopShortcutComboBox.isSelected());
            
            panel.getWizard().setProperty(
                    CREATE_START_MENU_SHORTCUT_PROPERTY,
                    StringUtils.EMPTY_STRING + startMenuShortcutComboBox.isSelected());
        }

        @Override
        protected String validateInput() {
            String errorMessage = super.validateInput();
            return errorMessage;
        }

        // private //////////////////////////////////////////////////////////////////
        private void initComponents() {
            // selectedLocationField ////////////////////////////////////////////////
            desktopShortcutComboBox = new NbiCheckBox();
            startMenuShortcutComboBox = new NbiCheckBox();

            // this /////////////////////////////////////////////////////////////////
            add(desktopShortcutComboBox, new GridBagConstraints(
                    0, 2, // x, y
                    2, 1, // width, height
                    1.0, 0.0, // weight-x, weight-y
                    GridBagConstraints.LINE_START, // anchor
                    GridBagConstraints.HORIZONTAL, // fill
                    new Insets(15, 11, 0, 11), // padding
                    0, 0));                           // padx, pady - ???
            add(startMenuShortcutComboBox, new GridBagConstraints(
                    0, 3, // x, y
                    2, 1, // width, height
                    1.0, 0.0, // weight-x, weight-y
                    GridBagConstraints.LINE_START, // anchor
                    GridBagConstraints.HORIZONTAL, // fill
                    new Insets(7, 11, 0, 11), // padding
                    0, 0));                           // padx, pady - ???

        }
    }
    /////////////////////////////////////////////////////////////////////////////////
    // Constants
    public static final String DEFAULT_TITLE =
            ResourceUtils.getString("data","product.display.name") + ResourceUtils.getString(ShortcutCreationPanel.class,
            "P.title"); // NOI18N
    public static final String DEFAULT_DESCRIPTION =
            ResourceUtils.getString(ShortcutCreationPanel.class,
            "P.description"); // NOI18N
    public static final String CREATE_DESKTOP_SHORTCUT_NAME =
            ResourceUtils.getString(ShortcutCreationPanel.class,
            "P.create.desktop.shortcut"); // NOI18N
    public static final String CREATE_START_MENU_SHORTCUT_NAME_WINDOWS =
            ResourceUtils.getString(ShortcutCreationPanel.class,
            "P.create.start.menu.shortcut.windows"); // NOI18N
    public static final String CREATE_START_MENU_SHORTCUT_NAME_UNIX =
            ResourceUtils.getString(ShortcutCreationPanel.class,
            "P.create.start.menu.shortcut.unix"); // NOI18N
    public static final String CREATE_START_MENU_SHORTCUT_NAME_MAC =
            ResourceUtils.getString(ShortcutCreationPanel.class,
            "P.create.start.menu.shortcut.macosx"); // NOI18N
    public static final String CREATE_DESKTOP_SHORTCUT_PROPERTY =
            "create.desktop.shortcut";
    public static final String CREATE_START_MENU_SHORTCUT_PROPERTY =
            "create.start.menu.shortcut";
}
