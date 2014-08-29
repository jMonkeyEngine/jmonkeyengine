package com.jme3.gde.gui.nodes;


import jada.ngeditor.listeners.events.ElementEvent;
import jada.ngeditor.model.GUI;
import jada.ngeditor.model.elements.GElement;
import jada.ngeditor.model.elements.GScreen;
import jada.ngeditor.model.elements.specials.GUseControls;
import jada.ngeditor.model.elements.specials.GUseStyle;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

public class ScreenChildFactory extends ChildFactory<Object> implements Observer{
    private final GUI gui;
    
    public ScreenChildFactory(GUI gui){
        this.gui = gui;
        this.gui.addObserver(ScreenChildFactory.this);
        
    }
    @Override
    protected boolean createKeys(List<Object> list) {
        list.addAll(gui.getUseStyles());
        list.addAll(gui.getUseControls());
        list.addAll(gui.getScreens());
        
        
        return true;
    }

    @Override
    protected Node createNodeForKey(Object node) {
        if(node instanceof GElement){
            return new GElementNode((GElement)node);
        }else if (node instanceof GUseStyle){
            return new GUseStyleNode(gui, (GUseStyle)node);
        }else
            return new GUseControlsNode(gui,(GUseControls)node);
    }

    @Override
    public void update(Observable o, Object arg) {
       if(arg == null){
           this.refresh(true);
       }else{
           ElementEvent e = (ElementEvent) arg;
           if(e.getElement() instanceof GScreen){
               this.refresh(true);
           }
       }
    }
}