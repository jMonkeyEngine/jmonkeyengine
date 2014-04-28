/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.utils;

import com.jme3.asset.ShaderNodeDefinitionKey;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.materialdefinition.fileStructure.ShaderNodeBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.InputMappingBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.MappingBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.MatParamBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.WorldParamBlock;
import com.jme3.shader.ShaderNodeDefinition;
import com.jme3.shader.ShaderNodeVariable;
import com.jme3.shader.ShaderUtils;
import com.jme3.shader.UniformBinding;
import com.jme3.shader.VarType;
import java.util.List;

/**
 *
 * @author Nehon
 */
public class MaterialUtils {
    
    public static String makeKey(MappingBlock mapping, String techName) {
        
        String rightName = mapping.getRightVar();
        String leftName = mapping.getLeftVar();
        String leftSwizzle = mapping.getLeftVarSwizzle() != null ? "." + mapping.getLeftVarSwizzle() : "";
        String rightSwizzle = mapping.getRightVarSwizzle() != null ? "." + mapping.getRightVarSwizzle() : "";
        return techName + "/" + mapping.getLeftNameSpace() + "." + leftName + leftSwizzle + "=" + mapping.getRightNameSpace() + "." + rightName + rightSwizzle;
    }

    /**
     * trims a line and removes comments
     *
     * @param line
     * @return
     */
    public static String trimLine(String line) {
        int idx = line.indexOf("//");
        if (idx != -1) {
            line = line.substring(0, idx);
        }
        return line.trim();
    }

    /**
     * trims a line and removes everything behind colon
     *
     * @param line
     * @return
     */
    public static String trimName(String line) {
        line = trimLine(line);
        int idx = line.indexOf("(");
        if (idx == -1) {
            idx = line.indexOf(":");
        }
        if (idx != -1) {
            line = line.substring(0, idx);
        }
        return line.trim();
    }
    
    public static ShaderNodeDefinition loadShaderNodeDefinition(ShaderNodeBlock shaderNode, ProjectAssetManager manager) {
        return loadShaderNodeDefinition(shaderNode.getDefinition().getPath(), shaderNode.getDefinition().getName(), manager);
    }
    
    public static ShaderNodeDefinition loadShaderNodeDefinition(String path, String name, ProjectAssetManager manager) {
        ShaderNodeDefinitionKey k = new ShaderNodeDefinitionKey(path);
        k.setLoadDocumentation(true);
        List<ShaderNodeDefinition> defs = (List<ShaderNodeDefinition>) manager.loadAsset(k);
        for (ShaderNodeDefinition shaderNodeDefinition : defs) {
            if (shaderNodeDefinition.getName().equals(name)) {
                return shaderNodeDefinition;
            }
        }
        return null;
    }

    /**
     * updates the type of the right variable of a mapping from the type of the
     * left variable
     *
     * @param mapping the mapping to consider
     */
    public static String guessType(InputMappingBlock mapping, ShaderNodeVariable left) {
        String type = left.getType();
        int card = ShaderUtils.getCardinality(type, mapping.getRightVarSwizzle() == null ? "" : mapping.getRightVarSwizzle());
        if (card > 0) {
            if (card == 1) {
                type = "float";
            } else {
                type = "vec" + card;
            }
        }
        return type;
    }
    
    public static ShaderNodeVariable getVar(List<ShaderNodeVariable> ins, String name) {
        for (ShaderNodeVariable shaderNodeVariable : ins) {
            if (shaderNodeVariable.getName().equals(name)) {
                return shaderNodeVariable;
            }
        }
        return null;
    }
    
    public static String getMatParamType(MatParamBlock param) {
        String type = param.getType();        
        if (type.equals("Color")) {
            type = "Vector4";
        }
        return VarType.valueOf(type).getGlslType();
    }
    
    public static String getWorldParamType(String name) {
        return UniformBinding.valueOf(name).getGlslType();
    }
    
    public static boolean contains(List<ShaderNodeVariable> vars, ShaderNodeVariable var) {
        for (ShaderNodeVariable shaderNodeVariable : vars) {
            if (shaderNodeVariable.getName().equals(var.getName()) && shaderNodeVariable.getNameSpace().equals(var.getNameSpace())) {
                return true;
            }
        }
        return false;
    }
}
