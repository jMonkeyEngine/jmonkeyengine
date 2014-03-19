/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.modelimporter;


import com.jme3.asset.AssetKey;
import com.jme3.gde.core.assets.AssetData;
import com.jme3.gde.core.util.PropertyUtils;
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
public class ImportKeyNode extends AbstractNode {
    private AssetKey key;

    public ImportKeyNode(AssetKey key) {
        super(Children.LEAF);
        this.key=key;
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set set = sheet.createPropertiesSet();
        set.setName("AssetKey");
        set.setDisplayName("Conversion Settings");
        for (Field field : key.getClass().getDeclaredFields()) {
            PropertyDescriptor prop = PropertyUtils.getPropertyDescriptor(key.getClass(), field);
            if (prop != null) {
                try {
                    Property sup = new PropertySupport.Reflection(key, prop.getPropertyType(), prop.getReadMethod(), prop.getWriteMethod());
                    sup.setName(prop.getName());
                    sup.setDisplayName(prop.getDisplayName());
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
