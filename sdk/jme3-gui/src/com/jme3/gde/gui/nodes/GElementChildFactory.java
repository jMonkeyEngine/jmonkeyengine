package com.jme3.gde.gui.nodes;


import jada.ngeditor.model.elements.GElement;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

public class GElementChildFactory extends ChildFactory<GElement> implements Observer{
    private final GElement parent;
     
    public GElementChildFactory(GElement parent) {
        this.parent = parent;
        this.parent.addObserver(this);
    
    }
    @Override
    protected boolean createKeys(List<GElement> list) {
        list.addAll(parent.getElements());
        return true;
    }

    @Override
    protected Node createNodeForKey(GElement element) {
        return new GElementNode(element);
    }  

    @Override
    public void update(Observable o, Object arg) {
       this.refresh(true);
    }
}