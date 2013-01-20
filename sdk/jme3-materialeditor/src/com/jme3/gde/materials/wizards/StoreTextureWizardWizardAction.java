/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materials.wizards;

import com.jme3.gde.core.assets.ProjectAssetManager;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

// An example action demonstrating how the wizard could be called from within
// your code. You can move the code below wherever you need, or register an action:
// @ActionID(category="...", id="com.jme3.gde.materials.wizards.StoreTextureWizardWizardAction")
// @ActionRegistration(displayName="Open StoreTextureWizard Wizard")
// @ActionReference(path="Menu/Tools", position=...)
public final class StoreTextureWizardWizardAction implements ActionListener {

    private final ProjectAssetManager mgr;
    private byte[] data;
    private String name;

    public StoreTextureWizardWizardAction(ProjectAssetManager mgr, byte[] data, String name) {
        this.mgr = mgr;
        this.data = data;
        this.name = name;
    }

    public StoreTextureWizardWizardAction(ProjectAssetManager mgr, byte[] data) {
        this.mgr = mgr;
        this.data = data;
    }

    public StoreTextureWizardWizardAction(ProjectAssetManager mgr) {
        this.mgr = mgr;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (mgr == null) {
            return;
        }
        List<WizardDescriptor.Panel<WizardDescriptor>> panels = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>();
        panels.add(new StoreTextureWizardWizardPanel1());
        String[] steps = new String[panels.size()];
        for (int i = 0; i < panels.size(); i++) {
            Component c = panels.get(i).getComponent();
            // Default step name to component name of panel.
            steps[i] = c.getName();
            if (c instanceof JComponent) { // assume Swing components
                JComponent jc = (JComponent) c;
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
                jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
            }
        }
        WizardDescriptor wiz = new WizardDescriptor(new WizardDescriptor.ArrayIterator<WizardDescriptor>(panels));
        if (name != null) {
            wiz.putProperty("path", name);
        } else {
            wiz.putProperty("path", "Textures/MyTexture.png");
        }
        // {0} will be replaced by WizardDesriptor.Panel.getComponent().getName()
        wiz.setTitleFormat(new MessageFormat("{0}"));
        wiz.setTitle("Save texture as..");
        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION) {
            String path = (String) wiz.getProperties().get("path");
            if (path != null) {
                name = path;
                OutputStream out = null;
                try {
                    FileObject file = mgr.getAssetFolder().getFileObject(path);
                    if (file != null) {
                        NotifyDescriptor.Confirmation mesg = new NotifyDescriptor.Confirmation("File exists, overwrite?",
                                "File Exists",
                                NotifyDescriptor.YES_NO_OPTION);
                        DialogDisplayer.getDefault().notify(mesg);
                        if (mesg.getValue() != NotifyDescriptor.Confirmation.YES_OPTION) {
                            return;
                        }
                    }
                    file = FileUtil.createData(mgr.getAssetFolder(), path);
                    out = new BufferedOutputStream(file.getOutputStream());
                    out.write(data);
                    file.getParent().refresh();
                } catch (IOException ex) {
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Failed to create data!\n" + ex));
                    Exceptions.printStackTrace(ex);
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
            }
        }
    }

    public String getName() {
        return name;
    }
}
