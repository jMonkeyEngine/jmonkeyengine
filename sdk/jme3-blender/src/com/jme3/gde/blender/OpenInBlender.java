/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.blender;

import com.jme3.gde.core.scene.ApplicationLogHandler.LogLevel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
    category = "jMonkeyEngine",
id = "com.jme3.gde.blender.OpenBlender")
@ActionRegistration(
    iconBase = "com/jme3/gde/blender/blender.png",
displayName = "#CTL_OpenInBlender")
@ActionReferences({
    @ActionReference(path = "Toolbars/File", position = 335),
    @ActionReference(path = "Loaders/application/blender/Actions", position = 9)
})
@Messages("CTL_OpenInBlender=Open in Blender")
public final class OpenInBlender implements ActionListener {

    private static final Logger logger = Logger.getLogger(OpenInBlender.class.getName());
    private final BlenderDataObject context;

    public OpenInBlender(BlenderDataObject context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (!BlenderTool.openInBlender(context.getPrimaryFile())) {
            logger.log(LogLevel.INFO, "Could not open file in blender.");
        }
    }
}
