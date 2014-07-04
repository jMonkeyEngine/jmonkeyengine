package com.jme3.gde.gui.nodes;


import com.google.common.base.Predicate;
import jada.ngeditor.model.GUI;
import jada.ngeditor.model.elements.GControl;
import jada.ngeditor.model.elements.GElement;
import java.util.List;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public class ScreenChildFactory extends ChildFactory<GElement> {
    private final GUI gui;
    
    public ScreenChildFactory(GUI gui){
        this.gui = gui;
        
    }
    @Override
    protected boolean createKeys(List<GElement> list) {
        list.addAll(gui.getScreens());
        return true;
    }

    @Override
    protected Node createNodeForKey(GElement screen) {
        return new GElementNode(screen);
    }
}