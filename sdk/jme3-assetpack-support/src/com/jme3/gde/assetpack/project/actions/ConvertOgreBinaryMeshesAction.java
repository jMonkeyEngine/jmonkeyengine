/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.project.actions;

import com.jme3.gde.assetpack.project.AssetPackProject;
import com.jme3.gde.assetpack.project.wizards.ConvertOgreBinaryWizardPanel1;
import com.jme3.gde.ogretools.convert.OgreXMLConvert;
import com.jme3.gde.ogretools.convert.OgreXMLConvertOptions;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JComponent;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class ConvertOgreBinaryMeshesAction implements Action {

    private final Project context;
    private WizardDescriptor.Panel[] panels;

    public ConvertOgreBinaryMeshesAction(Project context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        final WizardDescriptor wizardDescriptor = new WizardDescriptor(getPanels());
        // {0} will be replaced by WizardDesriptor.Panel.getComponent().getName()
        wizardDescriptor.setTitleFormat(new MessageFormat("{0}"));
        wizardDescriptor.setTitle("Convert Ogre Binary Meshes");
        wizardDescriptor.putProperty("project", context);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(wizardDescriptor);
        dialog.setVisible(true);
        dialog.toFront();
        boolean cancelled = wizardDescriptor.getValue() != WizardDescriptor.FINISH_OPTION;
        final boolean deleteFiles = (Boolean) wizardDescriptor.getProperty("deleteoriginal");
        if (!cancelled) {
            new Thread(new Runnable() {

                public void run() {
                    scanDir(((AssetPackProject) context).getAssetsFolder().getPath(), deleteFiles);
                }
            }).start();
        }
    }

    public void scanDir(String dir2scan, boolean delete) {
        ProgressHandle handle = ProgressHandleFactory.createHandle("Convert Ogre Binary Files");
        handle.start();
        try {
            File zipDir = new File(dir2scan);
            String[] dirList = zipDir.list();
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    scanDir(filePath, delete);
                    continue;
                }
                FileObject fobj = FileUtil.toFileObject(f);
                if (fobj.getExt().equalsIgnoreCase("mesh")||fobj.getExt().equalsIgnoreCase("skeleton")) {
                    OgreXMLConvertOptions options = new OgreXMLConvertOptions(fobj.getPath());
                    options.setBinaryFile(true);
                    OgreXMLConvert conv = new OgreXMLConvert();
                    conv.doConvert(options, handle);
                    if (delete) {
                        fobj.delete();
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(ConvertOgreBinaryMeshesAction.class.getName()).log(Level.SEVERE, "Error scanning directory", e);
        } finally {
            handle.finish();
        }
    }

    /**
     * Initialize panels representing individual wizard's steps and sets
     * various properties for them influencing wizard appearance.
     */
    private WizardDescriptor.Panel[] getPanels() {
        if (panels == null) {
            panels = new WizardDescriptor.Panel[]{
                        new ConvertOgreBinaryWizardPanel1()
                    };
            String[] steps = new String[panels.length];
            for (int i = 0; i < panels.length; i++) {
                Component c = panels[i].getComponent();
                // Default step name to component name of panel. Mainly useful
                // for getting the name of the target chooser to appear in the
                // list of steps.
                steps[i] = c.getName();
                if (c instanceof JComponent) { // assume Swing components
                    JComponent jc = (JComponent) c;
                    // Sets step number of a component
                    // TODO if using org.openide.dialogs >= 7.8, can use WizardDescriptor.PROP_*:
                    jc.putClientProperty("WizardPanel_contentSelectedIndex", new Integer(i));
                    // Sets steps names for a panel
                    jc.putClientProperty("WizardPanel_contentData", steps);
                    // Turn on subtitle creation on each step
                    jc.putClientProperty("WizardPanel_autoWizardStyle", Boolean.TRUE);
                    // Show steps on the left side with the image on the background
                    jc.putClientProperty("WizardPanel_contentDisplayed", Boolean.TRUE);
                    // Turn on numbering of all steps
                    jc.putClientProperty("WizardPanel_contentNumbered", Boolean.TRUE);
                }
            }
        }
        return panels;
    }

    public Object getValue(String key) {
        if (key.equals(NAME)) {
            return "Convert Binary Ogre Meshes..";
        }
        return null;
    }

    public void putValue(String key, Object value) {
    }

    public void setEnabled(boolean b) {
    }

    public boolean isEnabled() {
        return true;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }
}
