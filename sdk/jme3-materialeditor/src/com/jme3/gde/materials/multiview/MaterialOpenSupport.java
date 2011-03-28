/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materials.multiview;

import com.jme3.gde.core.sceneviewer.SceneViewerTopComponent;
import com.jme3.gde.materials.JMEMaterialDataObject;
import org.openide.cookies.CloseCookie;
import org.openide.cookies.OpenCookie;
import org.openide.loaders.OpenSupport;
import org.openide.windows.CloneableTopComponent;

/**
 *
 * @author normenhansen
 */
public class MaterialOpenSupport extends OpenSupport implements OpenCookie, CloseCookie {

    public MaterialOpenSupport(JMEMaterialDataObject.Entry entry) {
        super(entry);
    }

    protected CloneableTopComponent createCloneableTopComponent() {
        if (!SceneViewerTopComponent.findInstance().isOpened()) {
            SceneViewerTopComponent.findInstance().open();
        }
        JMEMaterialDataObject dobj = (JMEMaterialDataObject) entry.getDataObject();
        MaterialEditorTopComponent tc = new MaterialEditorTopComponent(dobj);
        return tc;
    }
}
