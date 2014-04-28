/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.navigator.node;

import com.jme3.gde.materialdefinition.MatDefDataObject;
import com.jme3.gde.materialdefinition.editor.Selectable;
import com.jme3.gde.materialdefinition.fileStructure.leaves.InputMappingBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.MappingBlock;
import com.jme3.gde.materialdefinition.icons.Icons;
import com.jme3.gde.materialdefinition.navigator.node.properties.DefaultProperty;
import com.jme3.gde.materialdefinition.utils.MaterialUtils;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.WeakListeners;

/**
 *
 * @author Nehon
 */
public class MappingNode extends AbstractMatDefNode implements Selectable, PropertyChangeListener {

    MappingBlock mapping; 
    String key;

    public MappingNode(final MappingBlock mapping) {
        this(null, mapping);
    }

    public MappingNode(Lookup lookup, final MappingBlock mapping) {
        super(Children.LEAF, lookup);
        this.mapping = mapping;
        setName(makeName(mapping));      
        key = makeKey();
    }

    private String makeName(MappingBlock mapping) {
//        ShaderNodeVariable left = mapping.getLeftVar();
//        ShaderNodeVariable right = mapping.getRightVariable();
//        String rightName = right.getName().replaceAll("g_", "").replaceAll("m_", "");
//        String leftName = left.getName().replaceAll("g_", "").replaceAll("m_", "");
//        String leftSwizzle = mapping.getLeftSwizzling().length() > 0 ? "." + mapping.getLeftSwizzling() : "";
//        String rightSwizzle = mapping.getRightSwizzling().length() > 0 ? "." + mapping.getRightSwizzling() : "";
//        if (isInput()) {
//            return leftName + leftSwizzle + " = " + right.getNameSpace() + "." + rightName + rightSwizzle;
//        }
        return mapping.toString();//right.getNameSpace() + "." + leftName + leftSwizzle + " = " + rightName + rightSwizzle;
    }

    private boolean isInput() {
        return (mapping instanceof InputMappingBlock);
    }

    @Override
    public Image getIcon(int type) {
        if (isInput()) {
            return Icons.in.getImage();
        }
        return Icons.out.getImage();
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set set = new Sheet.Set();
        set.setName(mapping.getLeftVar() + " <- " + mapping.getRightVar());
        set.setDisplayName(mapping.getLeftVar() + " <- " + mapping.getRightVar());


        try {
            set.put(new DefaultProperty<String>(mapping, String.class, "Condition", "getCondition", "setCondition"));

            set.put(new DefaultProperty<String>(mapping, String.class, "Left name space", "getLeftNameSpace", "setLeftNameSpace", true));
            set.put(new DefaultProperty<String>(mapping, String.class, "Left variable", "getLeftVar", "setLeftVar", true));
            set.put(new DefaultProperty<String>(mapping, String.class, "Left swizzle", "getLeftVarSwizzle", "setLeftVarSwizzle"));

            set.put(new DefaultProperty<String>(mapping, String.class, "Right name space", "getRightNameSpace", "setRightNameSpace", true));
            set.put(new DefaultProperty<String>(mapping, String.class, "Right variable", "getRightVar", "setRightVar", true));
            set.put(new DefaultProperty<String>(mapping, String.class, "Right swizzle", "getRightVarSwizzle", "setRightVarSwizzle"));


        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }

        mapping.addPropertyChangeListener(WeakListeners.propertyChange(this, mapping));


        sheet.put(set);

        return sheet;
    }

    private String makeKey() {  
        return MaterialUtils.makeKey(mapping, lookup.lookup(MatDefDataObject.class).getEditableFile().getCurrentTechnique().getName());
    }

    public String getKey() {
        return key;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        setName(makeName(mapping));        
        key = makeKey();
    }
}
