/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.project.actions;

import com.jme3.gde.assetpack.project.AssetPackProject;
import com.jme3.gde.assetpack.project.wizards.FileDescription;
import com.jme3.gde.assetpack.project.wizards.ImportWizardPanel1;
import com.jme3.gde.assetpack.project.wizards.ImportWizardPanel2;
import com.jme3.gde.core.assets.ProjectAssetManager;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import javax.swing.Action;
import javax.swing.JComponent;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileUtil;
import org.w3c.dom.Element;

@SuppressWarnings("unchecked")
public final class ImportAssetAction implements Action {

    private final AssetPackProject context;
    private WizardDescriptor.Panel[] panels;

    public ImportAssetAction(AssetPackProject context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        WizardDescriptor wizardDescriptor = new WizardDescriptor(getPanels());
        // {0} will be replaced by WizardDesriptor.Panel.getComponent().getName()
        wizardDescriptor.setTitleFormat(new MessageFormat("{0}"));
        wizardDescriptor.setTitle("Import Asset to AssetPack..");
        wizardDescriptor.putProperty("project", context);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(wizardDescriptor);
        dialog.setVisible(true);
        dialog.toFront();
        boolean cancelled = wizardDescriptor.getValue() != WizardDescriptor.FINISH_OPTION;
        if (!cancelled) {
            importAsset(wizardDescriptor);
        }
    }

    private void importAsset(WizardDescriptor desc) {
        ProjectAssetManager pm = context.getProjectAssetManager();
        Element asset = context.getConfiguration().createElement("asset");
        asset.setAttribute("name", (String) desc.getProperty("name"));
        asset.setAttribute("type", (String) desc.getProperty("type"));
//        asset.setAttribute("format", (String) desc.getProperty("format"));
        asset.setAttribute("tags", (String) desc.getProperty("tags"));
        asset.setAttribute("categories", (String) desc.getProperty("categories"));
        Element description = context.getConfiguration().createElement("description");
        description.setTextContent((String) desc.getProperty("description"));
        asset.appendChild(description);
        Element license = context.getConfiguration().createElement("license");
        license.setTextContent((String) desc.getProperty("license"));
        asset.appendChild(license);

        List<FileDescription> files = (List<FileDescription>) desc.getProperty("filelist");
        for (Iterator<FileDescription> it = files.iterator(); it.hasNext();) {
            FileDescription fileObject = it.next();
            Element file = context.getConfiguration().createElement("file");
            asset.appendChild(file);
            file.setAttribute("path", fileObject.getPath() + fileObject.getFile().getNameExt());
            file.setAttribute("type", fileObject.getType());
            if (fileObject.isMainFile()) {
                file.setAttribute("main", "true");
            }
            if(!"default".equals(fileObject.getMaterial())){
                file.setAttribute("material", fileObject.getMaterial());
            }
            String[] extraProps = fileObject.getExtraPropsNames();
            String[] extraValues = fileObject.getExtraPropsValues();
            for (int i = 0; i < extraProps.length; i++) {
                file.setAttribute(extraProps[i], extraValues[i]);
            }
            if (!fileObject.isExisting()) {
                File ffile = new File(context.getAssetsFolder().getPath() + "/" + fileObject.getPath());
                try {
                    ffile.mkdirs();
                    fileObject.getFile().copy(FileUtil.toFileObject(ffile), fileObject.getFile().getName(), fileObject.getFile().getExt());
                } catch (Exception e) {
                }
            }
        }
        context.getProjectAssets().appendChild(asset);
        context.saveSettings();
        context.getAssetPackFolder().refresh();
    }

    /**
     * Initialize panels representing individual wizard's steps and sets
     * various properties for them influencing wizard appearance.
     */
    private WizardDescriptor.Panel[] getPanels() {
        if (panels == null) {
            panels = new WizardDescriptor.Panel[]{
                        new ImportWizardPanel1(),
                        new ImportWizardPanel2()
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
            return "Add Asset..";
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
