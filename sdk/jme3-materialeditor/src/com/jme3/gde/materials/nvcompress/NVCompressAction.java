/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materials.nvcompress;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Textures",
id = "com.jme3.gde.materials.nvcompress.NVCompressAction")
@ActionRegistration(displayName = "#CTL_NVCompress")
@ActionReferences({
    @ActionReference(path = "Menu/Tools/Textures", position = 100)
})
@Messages("CTL_NVCompress=NV Compress Tool")
public final class NVCompressAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        // TODO implement action body
        new NVCompress().start();
    }
}
