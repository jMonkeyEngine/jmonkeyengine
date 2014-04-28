/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure;

import com.jme3.gde.materialdefinition.fileStructure.leaves.FragmentShaderFileBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.InputMappingBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.LightModeBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.ShaderFileBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.UnsupportedStatement;
import com.jme3.gde.materialdefinition.fileStructure.leaves.VertexShaderFileBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.WorldParamBlock;
import com.jme3.util.blockparser.Statement;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import org.openide.util.WeakListeners;

/**
 *
 * @author Nehon
 */
public class TechniqueBlock extends UberStatement {

    public static final String ADD_SHADER_NODE = "addShaderNode";
    public static final String REMOVE_SHADER_NODE = "removeShaderNode";
    public static final String ADD_WORLD_PARAM = "addWorldParam";
    public static final String REMOVE_WORLD_PARAM = "removeWorldParam";
    protected String name;

    protected TechniqueBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public TechniqueBlock(Statement sta) {
        this(sta.getLineNumber(), sta.getLine());
        for (Statement statement : sta.getContents()) {
            if (statement.getLine().trim().startsWith("WorldParameters")) {
                addStatement(new WorldParametersBlock(statement));
            } else if (statement.getLine().trim().startsWith("LightMode")) {
                addStatement(new LightModeBlock(statement));
            } else if (statement.getLine().trim().startsWith("VertexShader ")) {
                addStatement(new VertexShaderFileBlock(statement));
            } else if (statement.getLine().trim().startsWith("FragmentShader ")) {
                addStatement(new FragmentShaderFileBlock(statement));
            } else if (statement.getLine().trim().startsWith("VertexShaderNodes")) {
                addStatement(new VertexShaderNodesBlock(statement));
            } else if (statement.getLine().trim().startsWith("FragmentShaderNodes")) {
                addStatement(new FragmentShaderNodesBlock(statement));
            } else {
                addStatement(new UnsupportedStatement(statement));
            }
        }
        String[] s = line.split("\\s");
        if (s.length == 1) {
            name = "Default";
        } else {
            name = s[1];
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        line = "Technique " + (name.equals("Default") ? "" : name);
        fire("name", oldName, name);
    }

    public String getLightMode() {
        LightModeBlock l = getBlock(LightModeBlock.class);
        if (l == null) {
            return null;
        }
        return l.getLightMode();
    }

    public void setLightMode(String lightMode) {
        String oldLightMode = null;
        LightModeBlock l = getBlock(LightModeBlock.class);
        if (l == null) {
            l = new LightModeBlock(lightMode);
            addStatement(0, l);
        } else {
            oldLightMode = l.getLightMode();
            l.setLightMode(lightMode);
        }
        fire("lightMode", oldLightMode, lightMode);
    }

    protected WorldParametersBlock getWorldParameters() {
        return getBlock(WorldParametersBlock.class);
    }

    public List<WorldParamBlock> getWorldParams() {
        return getWorldParameters().getWorldParams();
    }

    public void addWorldParam(WorldParamBlock block) {
        WorldParametersBlock wpBlock = getBlock(WorldParametersBlock.class);
        if (wpBlock == null) {
            wpBlock = new WorldParametersBlock(0, "WorldParameters");
            addStatement(0, wpBlock);
        }
        wpBlock.addWorldParam(block);
        fire(ADD_WORLD_PARAM, null, block);
    }

    public void addVertexShaderNode(ShaderNodeBlock block) {
        VertexShaderNodesBlock vblock = getBlock(VertexShaderNodesBlock.class);

        if (vblock == null) {
            vblock = new VertexShaderNodesBlock(0, "VertexShaderNodes ");
            addStatement(0, vblock);
        }
        vblock.addShaderNode(block);
        fire(ADD_SHADER_NODE, null, block);
    }

    public void addFragmentShaderNode(ShaderNodeBlock block) {
        FragmentShaderNodesBlock fblock = getBlock(FragmentShaderNodesBlock.class);

        if (fblock == null) {
            fblock = new FragmentShaderNodesBlock(0, "FragmentShaderNodes ");
            addStatement(0, fblock);
        }
        fblock.addShaderNode(block);
        fire(ADD_SHADER_NODE, null, block);
    }

    public void removeShaderNode(ShaderNodeBlock block) {
        VertexShaderNodesBlock vblock = getBlock(VertexShaderNodesBlock.class);
        FragmentShaderNodesBlock fblock = getBlock(FragmentShaderNodesBlock.class);
        boolean removed = false;
        String eventToFire = null;
        if (vblock != null) {
            removed = vblock.removeShaderNode(block);
            eventToFire = REMOVE_SHADER_NODE;
        }
        if (fblock != null) {
            if (!removed) {
                removed = fblock.removeShaderNode(block);
                eventToFire = REMOVE_SHADER_NODE;
            }
        }

        if (removed) {
            cleanMappings(vblock, block);
            cleanMappings(fblock, block);
        }

        if (eventToFire != null) {
            fire(eventToFire, block, null);
        }
    }

    public void removeWorldParam(WorldParamBlock worldParam) {
        WorldParametersBlock wpBlock = getWorldParameters();
        if (wpBlock == null) {
            return;
        }
        wpBlock.removeWorldParam(worldParam);

        VertexShaderNodesBlock vblock = getBlock(VertexShaderNodesBlock.class);
        FragmentShaderNodesBlock fblock = getBlock(FragmentShaderNodesBlock.class);
        cleanMappings(vblock, "WorldParam", worldParam.getName());
        cleanMappings(fblock, "WorldParam", worldParam.getName());

        fire(REMOVE_WORLD_PARAM, null, worldParam);
    }

    public List<ShaderNodeBlock> getShaderNodes() {
        List<ShaderNodeBlock> list = new ArrayList<ShaderNodeBlock>();
        list.addAll(getBlock(VertexShaderNodesBlock.class).getShaderNodes());
        list.addAll(getBlock(FragmentShaderNodesBlock.class).getShaderNodes());
        return list;
    }

    public ShaderFileBlock getVertexShader() {
        return getBlock(VertexShaderFileBlock.class);
    }

    public ShaderFileBlock getFragmentShader() {
        return getBlock(FragmentShaderFileBlock.class);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        super.addPropertyChangeListener(pcl);
        VertexShaderNodesBlock vblock = getBlock(VertexShaderNodesBlock.class);
        if (vblock != null) {
            vblock.addPropertyChangeListener(WeakListeners.propertyChange(pcl, vblock));
        }
        FragmentShaderNodesBlock fblock = getBlock(FragmentShaderNodesBlock.class);
        if (fblock != null) {
            fblock.addPropertyChangeListener(WeakListeners.propertyChange(pcl, fblock));
        }
    }

    private void cleanMappings(ShaderNodesBlock vblock, ShaderNodeBlock block) {
        if (vblock != null) {
            for (ShaderNodeBlock shaderNodeBlock : vblock.getShaderNodes()) {
                List<InputMappingBlock> lInput = shaderNodeBlock.getInputs();
                if (lInput != null) {
                    for (InputMappingBlock inputMappingBlock : lInput) {
                        if (inputMappingBlock.getRightNameSpace().equals(block.getName())) {
                            shaderNodeBlock.removeInputMapping(inputMappingBlock);
                        }
                    }
                }
            }
        }
    }

    protected void cleanMappings(ShaderNodesBlock vblock, String nameSpace, String name) {
        if (vblock != null) {
            for (ShaderNodeBlock shaderNodeBlock : vblock.getShaderNodes()) {
                List<InputMappingBlock> lInput = shaderNodeBlock.getInputs();
                if (lInput != null) {
                    for (InputMappingBlock inputMappingBlock : lInput) {
                        if (inputMappingBlock.getRightNameSpace().equals(nameSpace) && inputMappingBlock.getRightVar().equals(name)) {
                            shaderNodeBlock.removeInputMapping(inputMappingBlock);
                        }
                    }
                }
            }
        }
    }
    
    public void cleanupMappings(String nameSpace, String name) {
        cleanMappings(getBlock(VertexShaderNodesBlock.class), nameSpace, name);
        cleanMappings(getBlock(FragmentShaderNodesBlock.class), nameSpace, name);
    }
}
