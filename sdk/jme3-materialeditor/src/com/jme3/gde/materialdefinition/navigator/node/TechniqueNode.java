/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.navigator.node;

import com.jme3.gde.materialdefinition.editor.Selectable;
import com.jme3.gde.materialdefinition.fileStructure.ShaderNodeBlock;
import com.jme3.gde.materialdefinition.fileStructure.TechniqueBlock;
import com.jme3.gde.materialdefinition.icons.Icons;
import com.jme3.gde.materialdefinition.navigator.node.properties.DefaultProperty;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.WeakListeners;

/**
 *
 * @author Nehon
 */
public class TechniqueNode extends AbstractMatDefNode implements Selectable, PropertyChangeListener {

    TechniqueBlock def;

    public TechniqueNode(final Lookup lookup, final TechniqueBlock def) {
        super(new ShaderNodeChildren(lookup, def), lookup);
        this.def = def;
        def.addPropertyChangeListener(WeakListeners.propertyChange(this, def));
        setName(def.getName());
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setName("Technique");
        try {
            DefaultProperty<String> prop = new DefaultProperty<String>(def, String.class, "Name", "getName", "setName");
            set.put(prop);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }

        sheet.put(set);



        return sheet;

    }

    protected TechniqueBlock getDef() {
        return def;
    }

    @Override
    public Image getIcon(int type) {
        return Icons.tech.getImage();
    }

    @Override
    public Image getOpenedIcon(int type) {
        return Icons.tech.getImage();
    }

    public String getKey() {
        return getName();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("name")) {
            setName((String) evt.getNewValue());
            setDisplayName((String) evt.getNewValue());
        }
        if (evt.getPropertyName().equals("reorder")
                || evt.getPropertyName().equals(TechniqueBlock.ADD_SHADER_NODE)
                || evt.getPropertyName().equals(TechniqueBlock.REMOVE_SHADER_NODE)) {
            ((ShaderNodeChildren) getChildren()).reload();
        }
    }

    public static class ShaderNodeChildren extends Children.Keys<ShaderNodeBlock> {

        Lookup lookup;
        TechniqueBlock def;

        public ShaderNodeChildren(Lookup lookup, TechniqueBlock def) {
            this.lookup = lookup;
            this.def = def;
        }

        @Override
        protected void addNotify() {
            super.addNotify();
            setKeys(createKeys());
        }

        public void reload() {
            setKeys(createKeys());
        }

        public List<ShaderNodeBlock> createKeys() {
            List<ShaderNodeBlock> l = new ArrayList<ShaderNodeBlock>();
            if (def.getVertexShader() == null) {
                l.addAll(def.getShaderNodes());
            }
            return l;
        }

        @Override
        protected Node[] createNodes(ShaderNodeBlock key) {
            return new Node[]{new ShaderNodeNode(lookup, key)};
        }
    }
}
