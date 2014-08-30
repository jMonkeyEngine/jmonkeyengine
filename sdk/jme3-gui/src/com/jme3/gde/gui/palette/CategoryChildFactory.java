package com.jme3.gde.gui.palette;


import com.google.common.base.Predicate;
import jada.ngeditor.model.elements.GControl;
import jada.ngeditor.model.elements.GElement;
import java.util.List;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public class CategoryChildFactory extends ChildFactory<ElementFilter> {

    @Override
    protected boolean createKeys(List<ElementFilter> list) {
        ElementFilter elements = new ElementFilter("Elements") {
            @Override
            public boolean apply(Class t) {
                return t.getGenericSuperclass().equals(GElement.class);
            }
        };
        list.add(elements);
         ElementFilter controls = new ElementFilter("Controls") {
             @Override
             public boolean apply(Class t) {
                  return t.getGenericSuperclass().equals(GControl.class); 
                 //To change body of generated methods, choose Tools | Templates.
             }
         };
         list.add(controls);
        return true;
    }

    @Override
    protected Node createNodeForKey(ElementFilter category) {
        return new CategoryNode(category);
    }

    public class CategoryNode extends AbstractNode {
        public CategoryNode(ElementFilter category) {
            super(Children.create(new WidgetChildFactory(category), true));
            setDisplayName(category.getName());
        }
    }
    
    
}