/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.navigator.node;

import com.jme3.gde.materialdefinition.editor.Selectable;
import com.jme3.gde.materialdefinition.fileStructure.MatDefBlock;
import com.jme3.gde.materialdefinition.fileStructure.TechniqueBlock;
import com.jme3.gde.materialdefinition.icons.Icons;
import com.jme3.gde.materialdefinition.navigator.node.properties.DefaultProperty;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.Action;
import org.openide.actions.RenameAction;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.WeakListeners;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author Nehon
 */
public class MatDefNode extends AbstractMatDefNode implements Selectable, PropertyChangeListener {
  

    public MatDefNode(final Lookup lookup) {
        super(Children.create(new ChildFactory<TechniqueBlock>() {
            @Override
            protected boolean createKeys(List<TechniqueBlock> list) {
                list.addAll(lookup.lookup(MatDefBlock.class).getTechniques());
                return true;
            }

            @Override
            protected Node createNodeForKey(TechniqueBlock key) {
                return new TechniqueNode(lookup, key);
            }
        }, true),lookup);

        setName(lookup.lookup(MatDefBlock.class).getName());

    }

  

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        MatDefBlock def = lookup.lookup(MatDefBlock.class);

        Sheet.Set set = new Sheet.Set();
        set.setName(def.getName() + "MaterialDefinition");
        set.setDisplayName(def.getName() + " Material Definition");
        set.setShortDescription(def.getName() + " Material Definition");
        try {
            DefaultProperty<String> prop = new DefaultProperty<String>(def, String.class, "Name", "getName", "setName");
            set.put(prop);

        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }

        def.addPropertyChangeListener(WeakListeners.propertyChange(this, def));
 
//        for (MatParam matParam : def.getMaterialParams()) {
//            set.put(MatParamProperty.makeProperty(matParam, lookup));
//        }
        sheet.put(set);

        return sheet;

    }

    @Override
    public Image getIcon(int type) {
        return Icons.matDef.getImage();
    }

    @Override
    public Image getOpenedIcon(int type) {
        return Icons.matDef.getImage();
    }

    public String getKey() {
        return getName();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals("name")){
            setName((String)evt.getNewValue());
            setDisplayName((String)evt.getNewValue());
        }

    }
}
