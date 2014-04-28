/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure;

import com.jme3.gde.materialdefinition.fileStructure.leaves.ConditionBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.DefinitionBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.InputMappingBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.OutputMappingBlock;
import com.jme3.shader.ShaderNodeDefinition;
import com.jme3.util.blockparser.Statement;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nehon
 */
public class ShaderNodeBlock extends UberStatement implements Comparable<ShaderNodeBlock>, PropertyChangeListener {

    public final static String POSITION = "position";
    public final static String INPUT = "input";
    public final static String OUTPUT = "output";
    public static final String ADD_MAPPING = "addMapping";
    public static final String REMOVE_MAPPING = "removeMapping";
    protected String name;
    //built up data for fast sorting 
    protected int spatialOrder;
    protected List<String> inputNodes = new ArrayList<String>();
    protected boolean globalInput = false;
    protected boolean globalOutput = false;

    protected ShaderNodeBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public ShaderNodeBlock(Statement sta) {
        this(sta.getLineNumber(), sta.getLine());
        String[] s = line.split("\\s");
        name = s[1];
        for (Statement statement : sta.getContents()) {
            if (statement.getLine().trim().startsWith("Definition")) {
                addStatement(new DefinitionBlock(statement));
            } else if (statement.getLine().trim().startsWith("Condition")) {
                addStatement(new ConditionBlock(statement));
            } else if (statement.getLine().trim().startsWith("InputMapping")) {
                addStatement(new InputMappingsBlock(statement, name));
            } else if (statement.getLine().trim().startsWith("OutputMapping")) {
                addStatement(new OutputMappingsBlock(statement, name));
            } else {
                addStatement(statement);
            }
        }

    }

    public ShaderNodeBlock(ShaderNodeDefinition def, String path) {
        super(0, "ShaderNode " + def.getName());
        name = def.getName();
        DefinitionBlock b = new DefinitionBlock(name, path);
        addStatement(0, b);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        line = "ShaderNode " + name;
        fire("name", oldName, name);
    }

    public void setSpatialOrder(int spatialOrder) {
        this.spatialOrder = spatialOrder;
    }

    public void addInputNode(String name) {
        if (!inputNodes.contains(name)) {
            inputNodes.add(name);
        }
    }

    public void setGlobalInput(boolean globalInput) {
        this.globalInput = globalInput;
    }

    public void setGlobalOutput(boolean globalOutput) {
        this.globalOutput = globalOutput;
    }

    public DefinitionBlock getDefinition() {
        return getBlock(DefinitionBlock.class);
    }

    public void setDefinition(String name, String path) {
        DefinitionBlock b = getDefinition();

        if (b == null) {
            b = new DefinitionBlock(name, path);
            addStatement(0, b);
        } else {
            b.setName(name);
            b.setPath(path);
        }
        fire("definition", null, b);
    }

    public String getCondition() {
        ConditionBlock b = getBlock(ConditionBlock.class);
        if (b == null) {
            return null;
        }
        return b.getCondition();
    }

    public void setCondition(String condition) {
        ConditionBlock b = getBlock(ConditionBlock.class);
        String prevCond = null;
        if (condition.trim().length() == 0 || condition.equals("<null value>")) {
            condition = null;
        }
        if (b == null) {
            if (condition == null) {
                return;
            }
            b = new ConditionBlock(condition);
            addStatement(b);
        } else {
            if (condition == null) {
                contents.remove(b);
            }
            prevCond = b.getCondition();
            b.setCondition(condition);
        }
        fire("condition", prevCond, condition);
    }

    public List<InputMappingBlock> getInputs() {
        InputMappingsBlock b = getBlock(InputMappingsBlock.class);
        if (b == null) {
            return null;
        }
        return b.getMappings();
    }

    public List<OutputMappingBlock> getOutputs() {
        OutputMappingsBlock b = getBlock(OutputMappingsBlock.class);
        if (b == null) {
            return null;
        }
        return b.getMappings();
    }

    public void addInputMapping(InputMappingBlock mapping) {
        InputMappingsBlock b = getBlock(InputMappingsBlock.class);
        if (b == null) {
            b = new InputMappingsBlock(0, "InputMappings");
            addStatement(b);
        }
        b.addMapping(mapping);
        fire(ADD_MAPPING, null, mapping);
    }

    public void addOutputMapping(OutputMappingBlock mapping) {
        OutputMappingsBlock b = getBlock(OutputMappingsBlock.class);
        if (b == null) {
            b = new OutputMappingsBlock(0, "OutputMappings");
            addStatement(b);
        }
        b.addMapping(mapping);
        fire(ADD_MAPPING, null, mapping);
    }

    public void removeInputMapping(InputMappingBlock mapping) {
        InputMappingsBlock b = getBlock(InputMappingsBlock.class);
        if (b != null) {
            b.removeMapping(mapping);
        }
        fire(REMOVE_MAPPING, mapping, null);
    }

    public void removeOutputMapping(OutputMappingBlock mapping) {
        OutputMappingsBlock b = getBlock(OutputMappingsBlock.class);
        if (b != null) {
            b.removeMapping(mapping);
        }
        fire(REMOVE_MAPPING, mapping, null);
    }

    public int compareTo(ShaderNodeBlock o) {
        if (inputNodes.contains(o.getName())) {
            return 1;
        }
        if (o.inputNodes.contains(name)) {
            return -1;
        }
        if ((globalInput && o.globalOutput) || (o.globalInput && globalOutput)) {
            return (int) Math.signum(spatialOrder - o.spatialOrder);
        }
        return 0;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(POSITION)) {
            spatialOrder = (Integer) evt.getNewValue();
            fire("order", null, null);
        } else if (evt.getPropertyName().equals(INPUT)) {
            InputMappingBlock mapping = (InputMappingBlock) evt.getNewValue();
            if (mapping != null) {
                if (mapping.getRightNameSpace().equals("Global")) {
                    globalInput = true;
                } else {
                    inputNodes.add(mapping.getRightNameSpace());
                }
                addInputMapping(mapping);
            } else {
                InputMappingBlock oldMapping = (InputMappingBlock) evt.getOldValue();
                if (oldMapping.getRightNameSpace().equals("Global")) {
                    globalInput = false;
                } else {
                    inputNodes.remove(oldMapping.getRightNameSpace());
                }
                removeInputMapping(oldMapping);
            }
            fire("order", null, null);

        } else if (evt.getPropertyName().equals(OUTPUT)) {
            OutputMappingBlock mapping = (OutputMappingBlock) evt.getNewValue();
            if (mapping != null) {
                if (mapping.getLeftNameSpace().equals("Global")) {
                    globalOutput = true;
                }
                addOutputMapping(mapping);
            } else {
                OutputMappingBlock oldMapping = (OutputMappingBlock) evt.getOldValue();

                if (oldMapping.getLeftNameSpace().equals("Global")) {
                    globalOutput = false;
                }
                removeOutputMapping(oldMapping);
            }
            fire("order", null, null);
        }
    }
}
