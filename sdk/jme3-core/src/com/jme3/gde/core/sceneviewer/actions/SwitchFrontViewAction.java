package com.jme3.gde.core.sceneviewer.actions;

import com.jme3.gde.core.sceneviewer.SceneViewerTopComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "SceneComposer",
id = "com.jme3.gde.core.sceneviewer.actions.SwitchFrontViewAcction")
@ActionRegistration(displayName = "#CTL_SwitchFrontViewAcction")
@ActionReferences({
    @ActionReference(path = "Actions/jMonkeyPlatform"),
    @ActionReference(path = "Shortcuts", name = "NUMPAD1")
})
@Messages("CTL_SwitchFrontViewAcction=Switch to front view")
public final class SwitchFrontViewAction implements ActionListener {


    public void actionPerformed(ActionEvent e) {

        SceneViewerTopComponent svtc = SceneViewerTopComponent.findInstance();
        if (svtc.hasFocus()) {
            System.out.println("front view");
        }
    }


}
