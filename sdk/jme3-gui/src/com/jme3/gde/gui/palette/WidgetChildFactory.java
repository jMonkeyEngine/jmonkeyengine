/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui.palette;

import jada.ngeditor.guiviews.palettecomponents.NWidget;
import jada.ngeditor.model.elements.GControl;
import jada.ngeditor.model.elements.GElement;
import jada.ngeditor.model.utils.ClassUtils;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

/**
 *
 * @author cris
 */
public class WidgetChildFactory extends ChildFactory<Class>{
    private final ElementFilter category;

    WidgetChildFactory(ElementFilter category) {
        this.category = category;
       
    }

    @Override
    protected boolean createKeys(List<Class> toPopulate) {
        Set<Class<? extends GElement>> classes = ClassUtils.findAllGElements();
            for(Class c : classes){
                if(this.isConcreteClass(c) && category.apply(c)){
                    toPopulate.add(c);
                
            }
            }
        return true;
    }

   private boolean isConcreteClass(Class object){
        boolean abs = Modifier.isAbstract( object.getModifiers() );
        return !abs && !object.isAnonymousClass() && GElement.class.isAssignableFrom(object);
    }

    @Override
    protected Node createNodeForKey(Class key) {
       return new WidgetNode(key);
    }
   
   

   
    
}
