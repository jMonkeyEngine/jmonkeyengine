/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.navigator.node;

import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.materialdefinition.MatDefDataObject;
import com.jme3.gde.materialdefinition.editor.Selectable;
import com.jme3.gde.materialdefinition.fileStructure.MatDefBlock;
import com.jme3.gde.materialdefinition.fileStructure.ShaderNodeBlock;
import com.jme3.gde.materialdefinition.fileStructure.TechniqueBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.MappingBlock;
import com.jme3.gde.materialdefinition.icons.Icons;
import com.jme3.gde.materialdefinition.navigator.node.properties.DefaultProperty;
import com.jme3.gde.materialdefinition.utils.MaterialUtils;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderNodeDefinition;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
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
public class ShaderNodeNode extends AbstractMatDefNode implements Selectable, PropertyChangeListener {

    ShaderNodeBlock shaderNode;
    ShaderNodeDefinition def;
    String key = "";

    public ShaderNodeNode(final Lookup lookup, final ShaderNodeBlock shaderNode) {
//        super(Children.create(new ChildFactory<MappingBlock>() {
//            @Override
//            protected boolean createKeys(List<MappingBlock> list) {
//                list.addAll(shaderNode.getInputs());
//                List<OutputMappingBlock> out = shaderNode.getOutputs();
//                if (out != null) {
//                    list.addAll(shaderNode.getOutputs());
//                }
//
//                return true;
//            }
//
//            @Override
//            protected Node createNodeForKey(MappingBlock key) {
//                return new MappingNode(lookup, key);
//            }
//        }, true), lookup);
        super(new MappingNodeChildren(lookup, shaderNode), lookup);
        this.shaderNode = shaderNode;
        setName(shaderNode.getName());
        key = makeKey();
        ProjectAssetManager manager = lookup.lookup(ProjectAssetManager.class);
        def = MaterialUtils.loadShaderNodeDefinition(shaderNode, manager);

    }

    @Override
    public Image getIcon(int type) {
        return getImageIcon();
    }

    @Override
    protected Sheet createSheet() {

        Sheet sheet = super.createSheet();
        Sheet.Set set = new Sheet.Set();
        set.setName(shaderNode.getName() + "ShaderNode");
        set.setDisplayName(shaderNode.getName() + " ShaderNode");
        set.setShortDescription(def.getDocumentation());
        try {
            set.put(new DefaultProperty<String>(shaderNode, String.class, "Name", "getName", "setName") {
                @Override
                public void setValue(String val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                    val = fixNodeName(val);
                    //glsl variable can't start with a number
                    if(val.matches("^\\d.*")){
                        return;
                    }
                    super.setValue(val);
                }
            });
            set.put(new DefaultProperty<String>(shaderNode, String.class, "Condition", "getCondition", "setCondition"));

        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }

        shaderNode.addPropertyChangeListener(WeakListeners.propertyChange(this, shaderNode));


        sheet.put(set);

        return sheet;
    }

    private String fixNodeName(String name) {
        TechniqueBlock tech = ((TechniqueNode) this.getParentNode()).getDef();
        List<ShaderNodeBlock> list = tech.getShaderNodes();
        return fixNodeName(list, name, 0);
    }

    private String fixNodeName(List<ShaderNodeBlock> nodes, String name, int count) {
        for (ShaderNodeBlock nodePanel : nodes) {
            if ((name + (count == 0 ? "" : count)).equals(nodePanel.getName())) {
                return fixNodeName(nodes, name, count + 1);
            }
        }
        return name + (count == 0 ? "" : count);
    }

    @Override
    public String getShortDescription() {
        return def.getDocumentation();
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getImageIcon();
    }

    private Image getImageIcon() {
        if (def.getType() == Shader.ShaderType.Vertex) {
            return Icons.vert.getImage();
        } else {
            return Icons.frag.getImage();
        }
    }

    private String makeKey() {
        String defName = lookup.lookup(MatDefDataObject.class).getEditableFile().getCurrentTechnique().getName();
        return defName + "/" + getName();
    }

    public String getKey() {
        return key;
    }

    public ShaderNodeDefinition getShaderNodeDefinition() {
        return def;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("name")) {
            setName((String) evt.getNewValue());
            setDisplayName((String) evt.getNewValue());
            key = makeKey();
        } else if (evt.getPropertyName().equals(ShaderNodeBlock.ADD_MAPPING)
                || evt.getPropertyName().equals(ShaderNodeBlock.REMOVE_MAPPING)) {
            ((MappingNodeChildren) getChildren()).reload();
        }
    }

    public static class MappingNodeChildren extends Children.Keys<MappingBlock> {

        Lookup lookup;
        ShaderNodeBlock node;

        public MappingNodeChildren(Lookup lookup, ShaderNodeBlock node) {
            this.lookup = lookup;
            this.node = node;
        }

        @Override
        protected void addNotify() {
            super.addNotify();
            setKeys(createKeys());
        }

        public void reload() {
            setKeys(createKeys());
        }

        public List<MappingBlock> createKeys() {
            List<MappingBlock> l = new ArrayList<MappingBlock>();
            if (node.getInputs() != null) {
                l.addAll(node.getInputs());
            }
            if (node.getOutputs() != null) {
                l.addAll(node.getOutputs());
            }

            return l;
        }

        @Override
        protected Node[] createNodes(MappingBlock key) {
            return new Node[]{new MappingNode(lookup, key)};
        }
    }
}
