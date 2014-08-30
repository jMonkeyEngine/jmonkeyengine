package com.jme3.gde.gui.nodes;

import jada.ngeditor.model.GUI;
import jada.ngeditor.model.elements.specials.GUseControls;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

public class GUseControlsNode extends AbstractNode {
      private static final String basePath="com/jme3/gde/gui/multiview/icons";
    private final GUseControls controls;
      
    public GUseControlsNode(GUI gui,GUseControls controls) {
        super(Children.LEAF);
        this.setIconBaseWithExtension(basePath+"/"+"control"+".png");
        this.controls = controls;
        this.setName(controls.getFilename());
    }
    
    
}