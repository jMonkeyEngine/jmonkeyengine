/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.gde.gui.multiview;

import com.jme3.gde.gui.NiftyGuiDataObject;
import org.netbeans.modules.xml.multiview.ToolBarMultiViewElement;
import org.netbeans.modules.xml.multiview.ui.SectionView;
import org.netbeans.modules.xml.multiview.ui.ToolBarDesignEditor;
import org.openide.nodes.Node;

/**
 *
 * @author normenhansen
 */
public class PreviewToolbarElement extends ToolBarMultiViewElement {
//    private NiftyGuiDataObject dObj;
    private ToolBarDesignEditor comp;
    private NiftyPreviewPanel viewPanel;

    public PreviewToolbarElement(NiftyGuiDataObject dObj) {
        super(dObj);
//        this.dObj = dObj;
        comp = new ToolBarDesignEditor();
        setVisualEditor(comp);
        viewPanel=new NiftyPreviewPanel(dObj, comp);
    }

    @Override
    public SectionView getSectionView() {
        return null;
    }

    @Override
    public void componentShowing() {
        super.componentShowing();
        viewPanel.updatePreView();
    }

    @Override
    public void componentClosed() {
        super.componentClosed();
        viewPanel.cleanup();
    }

}
