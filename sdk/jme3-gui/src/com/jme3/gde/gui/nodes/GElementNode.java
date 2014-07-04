/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui.nodes;

import jada.ngeditor.model.elements.GElement;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;

/**
 *
 * @author cris
 */
public class GElementNode extends AbstractNode{
    private final GElement element;

    public GElementNode(GElement element) {
        super(Children.create(new GElementChildFactory(element), false));
        
        this.element = element;
        this.setName(element.getID());
        
    }
    
    public GElement getGelement(){
        return element;
    }
    
    public void updateChildren(){
        
    }
    @Override
    protected Sheet createSheet() {
        Sheet s = super.createSheet();
        Sheet.Set set = s.get(Sheet.PROPERTIES);
        if (set == null) {
            set = Sheet.createPropertiesSet();
            s.put(set);
        }
        set.setName("Element Properties");
        set.setShortDescription("You can set element properties");
        for(Entry<String,String> pair : this.element.listAttributes().entrySet()){
            set.put(new ElementAttributeProperty(element,pair.getKey()));
        }
        s.put(set);
        return s; 
    }
    
    public  class ElementAttributeProperty extends Node.Property {

        private String attributeName;
        private GElement element;

        public ElementAttributeProperty(GElement element, String attributeName) {
            super(String.class);
            this.element = element;
            this.attributeName = attributeName;
            this.setName(attributeName);
            this.setDisplayName(attributeName);
           
        }

        @Override
        public boolean canRead() {
            return true;
        }

        @Override
        public String getValue() throws IllegalAccessException, InvocationTargetException{
            return element.getAttribute(attributeName);
        }

        @Override
        public boolean canWrite() {
            return true;
        }

        @Override
        public PropertyEditor getPropertyEditor() {  
            return super.getPropertyEditor(); //To change body of generated methods, choose Tools | Templates.
        }
        
        

        @Override
        public void setValue(Object val) throws IllegalAccessException, IllegalArgumentException {
            element.addAttribute(attributeName, val.toString());
            element.refresh();
        }
    };
}
