package com.jme3.gde.gui.nodes;

import jada.ngeditor.model.GUI;
import jada.ngeditor.model.elements.specials.GUseStyle;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

public class GUseStyleNode extends AbstractNode {
     private static final String basePath="com/jme3/gde/gui/multiview/icons";
    private final GUI gui;
    private final GUseStyle style;
    
    public GUseStyleNode(GUI gui,GUseStyle style) {
        super(Children.LEAF);
        this.setIconBaseWithExtension(basePath+"/"+"style"+".png");
        this.gui = gui;
        this.style = style;
        this.setName(style.getFilename());
    }

    @Override
    public Action[] getActions(boolean context) {
        return new Action[]{new Refresh(),new Delete()};
    }

    private  class Refresh extends AbstractAction {

        public Refresh() {
            super("Refresh");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
           gui.reoloadStyles(style.getFilename());
        }
    }
    
     private  class Delete extends AbstractAction {

        public Delete() {
            super("Delete");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
           gui.removeStyle(style);
        }
    }
    
    
    
}