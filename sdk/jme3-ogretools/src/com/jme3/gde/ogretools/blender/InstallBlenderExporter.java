/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.ogretools.blender;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class InstallBlenderExporter implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        InstallBlenderExporterPanel panel=new InstallBlenderExporterPanel(null, false);
        panel.setLocationRelativeTo(null);
        panel.setVisible(true);
    }
}
