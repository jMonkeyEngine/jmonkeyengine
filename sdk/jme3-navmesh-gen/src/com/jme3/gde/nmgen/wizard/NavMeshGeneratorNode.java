/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.nmgen.wizard;

import com.jme3.gde.core.util.PropertyUtils;
import com.jme3.gde.nmgen.NavMeshAction;
import com.jme3.gde.nmgen.NavMeshGenerator;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node.Property;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public class NavMeshGeneratorNode extends AbstractNode {

    private NavMeshGenerator key;

    public NavMeshGeneratorNode(NavMeshGenerator key) {
        super(Children.LEAF);
        this.key = key;
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set set = sheet.createPropertiesSet();
        set.setName("NavMeshGenerator");
        set.setDisplayName("Settings");
        for (Field field : key.getClass().getDeclaredFields()) {
            PropertyDescriptor prop = PropertyUtils.getPropertyDescriptor(key.getClass(), field);
            if (prop != null) {
                try {
                    Property sup = new PropertySupport.Reflection(key, prop.getPropertyType(), prop.getReadMethod(), prop.getWriteMethod());
                    sup.setDisplayName(prop.getDisplayName());
                    sup.setShortDescription(org.openide.util.NbBundle.getMessage(NavMeshAction.class, "properties."+prop.getName()));
                    set.put(sup);
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                }
            }
        }
        sheet.put(set);
        return sheet;
    }
}
