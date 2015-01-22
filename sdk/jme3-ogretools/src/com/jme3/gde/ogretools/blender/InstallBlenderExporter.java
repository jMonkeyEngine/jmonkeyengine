/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.ogretools.blender;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(id = "com.jme3.gde.ogretools.blender.InstallBlenderExporter", category = "JME3")
@ActionRegistration(iconBase = "com/jme3/gde/ogretools/blender/ogre-logo.png", displayName = "#CTL_InstallBlenderExporter", iconInMenu = true)
@ActionReference(path = "Menu/Tools/OgreXML", position = 1250)
public final class InstallBlenderExporter implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        InstallBlenderExporterPanel panel=new InstallBlenderExporterPanel(null, false);
        panel.setLocationRelativeTo(null);
        panel.setVisible(true);
    }
}
