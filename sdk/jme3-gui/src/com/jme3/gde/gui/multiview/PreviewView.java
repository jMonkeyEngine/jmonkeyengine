/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui.multiview;

import com.jme3.gde.gui.NiftyGuiDataObject;
import org.netbeans.modules.xml.multiview.DesignMultiViewDesc;

/**
 *
 * @author normenhansen
 */
public class PreviewView extends DesignMultiViewDesc {

    private int type;

    public PreviewView(NiftyGuiDataObject dObj, int type) {
        super(dObj, "Design");
        this.type = type;
    }

    public org.netbeans.core.spi.multiview.MultiViewElement createElement() {
        NiftyGuiDataObject dObj = (NiftyGuiDataObject) getDataObject();
        return new PreviewToolbarElement(dObj);
    }

    public java.awt.Image getIcon() {
        return org.openide.util.Utilities.loadImage("com/jme3/gde/gui/Computer_File_043.gif"); //NOI18N
    }

    public String preferredID() {
        return "Toc_multiview_design" + String.valueOf(type);
    }
}
