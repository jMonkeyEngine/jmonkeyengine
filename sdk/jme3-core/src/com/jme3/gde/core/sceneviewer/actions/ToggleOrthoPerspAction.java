package com.jme3.gde.core.sceneviewer.actions;

import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.SceneRequest;
import com.jme3.gde.core.sceneviewer.SceneViewerTopComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "SceneComposer",
id = "com.jme3.gde.core.sceneviewer.actions.ToggleOrthoPerspAction")
@ActionRegistration(displayName = "#CTL_ToggleOrthoPerspAction")
@ActionReferences({
    @ActionReference(path = "Actions/jMonkeyPlatform"),
    @ActionReference(path = "Shortcuts", name = "NUMPAD5")
})
@Messages("CTL_ToggleOrthoPerspAction=Toggle ortho / persp")
public final class ToggleOrthoPerspAction implements ActionListener {

    public ToggleOrthoPerspAction() {
    }

    public void actionPerformed(ActionEvent e) {

        SceneViewerTopComponent svtc = SceneViewerTopComponent.findInstance();
        if (svtc.hasFocus()) {
            SceneApplication.getApplication().getActiveCamController().toggleOrthoPerspMode();
        }

    }
}
