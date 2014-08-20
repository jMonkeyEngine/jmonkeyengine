/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui.nodes;

import jada.ngeditor.model.GUI;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

/**
 *
 * @author cris
 */
public class GUINode extends AbstractNode{
    private final GUI gui;
    private static final String basePath="com/jme3/gde/gui/multiview/icons";
    public GUINode(GUI gui) {
        super(Children.create(new ScreenChildFactory(gui),false));
        this.gui = gui;
        this.setIconBaseWithExtension(basePath+"/"+"game-monitor"+".png");
    }

    /**
     * @return the gui
     */
    public GUI getGui() {
        return gui;
    }

    @Override
    public String getName() {
        return super.getName()+" "+this.gui; //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
