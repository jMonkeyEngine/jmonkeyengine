/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.material.plugins;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.ShaderNodeDefinitionKey;
import com.jme3.material.MatParam;
import com.jme3.material.MaterialDef;
import com.jme3.material.ShaderGenerationInfo;
import com.jme3.material.TechniqueDef;
import com.jme3.shader.Shader.ShaderType;
import com.jme3.shader.*;
import com.jme3.util.blockparser.Statement;

import java.io.IOException;
import java.util.*;

/**
 * This class is here to be able to load shaderNodeDefinition from both the
 * J3MLoader and ShaderNodeDefinitionLoader.
 *
 * It also allows loading shader nodes from a j3md file and building the
 * ShaderNodes list of each technique and the ShaderGenerationInfo needed to
 * generate shaders.
 *
 * @author Nehon
 */
public class ShaderNodeLoaderDelegate {

    private static final boolean[] IM_HAS_NAME_SPACE = {false, true};
    private static final boolean[] OM_HAS_NAME_SPACE = {true, false};

    protected Map<String, ShaderNodeDefinition> nodeDefinitions;
    protected Map<String, ShaderNode> nodes;
    protected ShaderNodeDefinition shaderNodeDefinition;
    protected ShaderNode shaderNode;
    protected TechniqueDef techniqueDef;
    protected Map<String, DeclaredVariable> attributes = new HashMap<>();
    protected Map<String, DeclaredVariable> vertexDeclaredUniforms = new HashMap<>();
    protected Map<String, DeclaredVariable> fragmentDeclaredUniforms = new HashMap<>();
    protected Map<String, DeclaredVariable> varyings = new HashMap<>();
    protected MaterialDef materialDef;
    protected String shaderLanguage;
    protected String shaderName;
    protected Set<String> varNames = new HashSet<>();
    protected AssetManager assetManager;
    protected ConditionParser conditionParser = new ConditionParser();
    protected List<String> nulledConditions = new ArrayList<>();

    protected class DeclaredVariable {

        ShaderNodeVariable var;
        List<ShaderNode> nodes = new ArrayList<>();

        public DeclaredVariable(ShaderNodeVariable var) {
            this.var = var;
        }

        public final void addNode(ShaderNode c) {
            if (!nodes.contains(c)) {
                nodes.add(c);
            }
        }
    }
    /**
     * Read the ShaderNodesDefinitions block and returns a list of
     * ShaderNodesDefinition This method is used by the j3sn loader
     *
     * note that the order of the definitions in the list is not guaranteed.
     *
     * @param statements the list statements to parse
     * @param key the ShaderNodeDefinitionKey
     * @return a list of ShaderNodesDefinition
     * @throws IOException if an I/O error occurs
     */
    public List<ShaderNodeDefinition> readNodesDefinitions(List<Statement> statements, ShaderNodeDefinitionKey key) throws IOException {

        for (Statement statement : statements) {
            String[] split = statement.getLine().split("[ \\{]");
            if (statement.getLine().startsWith("ShaderNodeDefinition")) {
                String name = statement.getLine().substring("ShaderNodeDefinition".length()).trim();


                if (!getNodeDefinitions().containsKey(name)) {
                    shaderNodeDefinition = new ShaderNodeDefinition();
                    getNodeDefinitions().put(name, shaderNodeDefinition);
                    shaderNodeDefinition.setName(name);
                    shaderNodeDefinition.setPath(key.getName());
                    readShaderNodeDefinition(statement.getContents(), key);

                }
            } else {
                throw new MatParseException("ShaderNodeDefinition", split[0], statement);
            }
        }

        return new ArrayList<ShaderNodeDefinition>(getNodeDefinitions().values());
    }

    /**
     * Read the ShaderNodesDefinitions block and internally stores a map of
     * ShaderNodesDefinition This method is used by the j3m loader.
     *
     * When loaded in a material, the definitions are not stored as a list, but
     * they are stored in shader nodes based on this definition.
     *
     * The map is here to map the definition to the nodes, and ovoid reloading
     * already loaded definitions
     *
     * @param statements the list of statements to parse
     * @throws IOException if an I/O error occurs
     */
    public void readNodesDefinitions(List<Statement> statements) throws IOException {
        readNodesDefinitions(statements, new ShaderNodeDefinitionKey());
    }

    /**
     * effectively reads the ShaderNodesDefinitions block
     *
     * @param statements the list of statements to parse
     * @param key the ShaderNodeDefinitionKey
     * @throws IOException if an I/O error occurs
     */
    protected void readShaderNodeDefinition(List<Statement> statements, ShaderNodeDefinitionKey key) throws IOException {
        boolean isLoadDoc = key instanceof ShaderNodeDefinitionKey && key.isLoadDocumentation();
        for (Statement statement : statements) {
            try {
                String[] split = statement.getLine().split("[ \\{]");
                String line = statement.getLine();

                if (line.startsWith("Type")) {
                    String type = line.substring(line.lastIndexOf(':') + 1).trim();
                    shaderNodeDefinition.setType(ShaderType.valueOf(type));
                } else if (line.startsWith("Shader ")) {
                    readShaderStatement(statement);
                    shaderNodeDefinition.getShadersLanguage().add(shaderLanguage);
                    shaderNodeDefinition.getShadersPath().add(shaderName);
                } else if (line.startsWith("Documentation")) {
                    if (isLoadDoc) {
                        String doc = "";
                        for (Statement statement1 : statement.getContents()) {
                            doc += "\n" + statement1.getLine();
                        }
                        shaderNodeDefinition.setDocumentation(doc);
                    }
                } else if (line.startsWith("Input")) {
                    varNames.clear();
                    for (Statement statement1 : statement.getContents()) {
                        try {
                            shaderNodeDefinition.getInputs().add(readVariable(statement1));
                        } catch (RuntimeException e) {
                            throw new MatParseException(e.getMessage(), statement1, e);
                        }
                    }
                } else if (line.startsWith("Output")) {
                    varNames.clear();
                    for (Statement statement1 : statement.getContents()) {
                        try {
                            if (statement1.getLine().trim().equals("None")) {
                                shaderNodeDefinition.setNoOutput(true);
                            } else {
                                shaderNodeDefinition.getOutputs().add(readVariable(statement1));
                            }
                        } catch (RuntimeException e) {
                            throw new MatParseException(e.getMessage(), statement1, e);
                        }
                    }
                } else {
                    throw new MatParseException("one of Type, Shader, Documentation, Input, Output", split[0], statement);
                }
            } catch (RuntimeException e) {
                throw new MatParseException(e.getMessage(), statement, e);
            }
        }
    }

    /**
     * reads a variable declaration statement &lt;glslType&gt; &lt;varName&gt;
     *
     * @param statement the statement to parse
     * @return a ShaderNodeVariable extracted from the statement
     * @throws IOException if an I/O error occurs
     */
    protected ShaderNodeVariable readVariable(Statement statement) throws IOException {

        String line = statement.getLine().trim().replaceAll("\\s*\\[", "[");
        String[] splitVar = line.split("\\s");

        if (splitVar.length > 3) {
            throw new MatParseException("More than 3 arguments", splitVar.length + "", statement);
        }

        String defaultValue = splitVar.length > 2? splitVar[2] : null;
        String varName = splitVar[1];
        String varType = splitVar[0];
        String multiplicity = null;

        if (varName.contains("[")) {
            //we have an array
            String[] arr = splitVar[1].split("\\[");
            varName = arr[0].trim();
            multiplicity = arr[1].replaceAll("\\]", "").trim();
        }

        if (varNames.contains(varName)) {
            throw new MatParseException("Duplicate variable name " + varName, statement);
        }

        varNames.add(varName);

        final ShaderNodeVariable variable = new ShaderNodeVariable(varType, "", varName, multiplicity);
        variable.setDefaultValue(defaultValue);

        return variable;
    }

    /**
     * reads the VertexShaderNodes{} block
     *
     * @param statements the list of statements to parse
     * @throws IOException if an I/O error occurs
     */
    public void readVertexShaderNodes(List<Statement> statements) throws IOException {
        attributes.clear();
        readNodes(statements);
    }

    /**
     * reads a list of ShaderNode{} blocks
     *
     * @param statements the list of statements to parse
     * @throws IOException if an I/O error occurs
     */
    protected void readShaderNode(List<Statement> statements) throws IOException {

        final ShaderGenerationInfo generationInfo = techniqueDef.getShaderGenerationInfo();
        final List<String> unusedNodes = generationInfo.getUnusedNodes();

        for (Statement statement : statements) {

            String line = statement.getLine();
            String[] split = statement.getLine().split("[ \\{]");

            if (line.startsWith("Definition")) {
                ShaderNodeDefinition def = findDefinition(statement);
                shaderNode.setDefinition(def);
                if(def.isNoOutput()){
                    unusedNodes.remove(shaderNode.getName());
                }
            } else if (line.startsWith("Condition")) {
                String condition = line.substring(line.lastIndexOf(":") + 1).trim();
                extractCondition(condition, statement);
                shaderNode.setCondition(conditionParser.getFormattedExpression());
            } else if (line.startsWith("InputMappings")) {
                for (final Statement subStatement : statement.getContents()) {

                    VariableMapping mapping = readInputMapping(subStatement);

                    final ShaderNodeVariable rightVariable = mapping.getRightVariable();
                    if (rightVariable != null) {
                        unusedNodes.remove(rightVariable.getNameSpace());
                    }

                    shaderNode.getInputMapping().add(mapping);
                }
            } else if (line.startsWith("OutputMappings")) {
                for (Statement statement1 : statement.getContents()) {
                    VariableMapping mapping = readOutputMapping(statement1);
                    unusedNodes.remove(shaderNode.getName());
                    shaderNode.getOutputMapping().add(mapping);
                }
            } else {
                throw new MatParseException("ShaderNodeDefinition", split[0], statement);
            }
        }
    }

    /**
     * Reads a mapping statement. Sets the nameSpace, name and swizzling of the
     * left variable. Sets the name, nameSpace and swizzling of the right
     * variable types will be determined later. Also, we can have the right part as expression.
     * <pre>
     * Format variable to variable: &lt;nameSpace&gt;.&lt;varName&gt;[.&lt;swizzling&gt;] = &lt;nameSpace&gt;.&lt;varName&gt;[.&lt;swizzling&gt;][:Condition]
     * Format expression to variable: &lt;nameSpace&gt;.&lt;varName&gt;[.&lt;swizzling&gt;] = %% expression %% [:Condition]
     * </pre>
     *
     * @param statement the statement to read.
     * @param hasNameSpace indicate which vars have namespaces
     * @return the read mapping.
     * @throws MatParseException if the statement isn't valid.
     */
    protected VariableMapping parseMapping(Statement statement, boolean[] hasNameSpace) throws MatParseException {

        VariableMapping mapping = new VariableMapping();
        String[] cond = statement.getLine().split(":");
        String[] vars = cond[0].split("=");

        checkMappingFormat(vars, statement);

        ShaderNodeVariable[] variables = new ShaderNodeVariable[2];
        String[] swizzle = new String[2];
        String rightExpression = null;

        for (int i = 0; i < vars.length; i++) {

            final String var = vars[i].trim();

            // it seems that is expression, not variable
            if (var.contains("%%")) {
                rightExpression = var.substring(2, var.length() - 2);
                continue;
            }

            String[] expression = var.split("\\.");

            if (hasNameSpace[i]) {
                if (expression.length <= 3) {
                    variables[i] = new ShaderNodeVariable("", expression[0].trim(), expression[1].trim());
                }
                if (expression.length == 3) {
                    swizzle[i] = expression[2].trim();
                }
            } else {
                if (expression.length <= 2) {
                    variables[i] = new ShaderNodeVariable("", expression[0].trim());
                }
                if (expression.length == 2) {
                    swizzle[i] = expression[1].trim();
                }
            }
        }

        mapping.setLeftVariable(variables[0]);
        mapping.setLeftSwizzling(swizzle[0] != null ? swizzle[0] : "");

        if (rightExpression != null) {
            mapping.setRightExpression(rightExpression);
        } else {
            mapping.setRightVariable(variables[1]);
            mapping.setRightSwizzling(swizzle[1] != null ? swizzle[1] : "");
        }

        if (cond.length > 1) {
            extractCondition(cond[1], statement);
            mapping.setCondition(conditionParser.getFormattedExpression());
        }

        return mapping;
    }

    /**
     * reads the FragmentShaderNodes{} block
     *
     * @param statements the list of statements to parse
     * @throws IOException if an I/O error occurs
     */
    public void readFragmentShaderNodes(List<Statement> statements) throws IOException {
        readNodes(statements);
    }

    /**
     * Reads a Shader statement of the form TYPE LANG : SOURCE
     *
     * @param statement the shader statement (not null)
     * @throws IOException if an I/O error occurs
     */
    protected void readShaderStatement(Statement statement) throws IOException {
        String[] split = statement.getLine().split(":");
        if (split.length != 2) {
            throw new MatParseException("Shader statement syntax incorrect", statement);
        }
        String[] typeAndLang = split[0].split("\\p{javaWhitespace}+");
        if (typeAndLang.length != 2) {
            throw new MatParseException("Shader statement syntax incorrect", statement);
        }
        shaderName = split[1].trim();
        shaderLanguage = typeAndLang[1];
    }

    /**
     * Sets the technique definition currently being loaded
     *
     * @param techniqueDef the technique def
     */
    public void setTechniqueDef(TechniqueDef techniqueDef) {
        this.techniqueDef = techniqueDef;
    }

    /**
     * sets the material def currently being loaded
     *
     * @param materialDef (alias created)
     */
    public void setMaterialDef(MaterialDef materialDef) {
        this.materialDef = materialDef;
    }

    /**
     * Searches a variable in the given list and updates its type and namespace.
     *
     * @param var  the variable to update.
     * @param list the variables list.
     * @return true if the variable has been found and updated.
     */
    protected boolean updateVariableFromList(ShaderNodeVariable var, List<ShaderNodeVariable> list) {
        for (ShaderNodeVariable shaderNodeVariable : list) {
            if (shaderNodeVariable.getName().equals(var.getName())) {
                var.setType(shaderNodeVariable.getType());
                var.setMultiplicity(shaderNodeVariable.getMultiplicity());
                var.setNameSpace(shaderNode.getName());
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the type of the right variable of a mapping from the type of the
     * left variable.
     *
     * @param mapping the mapping to consider.
     */
    protected void updateRightTypeFromLeftType(VariableMapping mapping) {
        String type = mapping.getLeftVariable().getType();
        int card = ShaderUtils.getCardinality(type, mapping.getRightSwizzling());
        if (card > 0) {
            if (card == 1) {
                type = "float";
            } else {
                type = "vec" + card;
            }
        }
        mapping.getRightVariable().setType(type);
    }

    /**
     * Checks if once a mapping expression is split by "=" the resulting array
     * have 2 elements.
     *
     * @param vars      the array.
     * @param statement the statement.
     * @throws MatParseException if the array isn't correct.
     */
    protected void checkMappingFormat(String[] vars, Statement statement) throws MatParseException {
        if (vars.length != 2) {
            throw new MatParseException("Not a valid expression should be '<varName>[.<swizzling>] = " +
                    "<nameSpace>.<varName>[.<swizzling>][:Condition]'", statement);
        }
    }

    /**
     * Finds a {@link MatParam} in the {@link MaterialDef} from the given name.
     *
     * @param varName the material param name.
     * @return the found {@link MatParam} or null.
     */
    protected MatParam findMatParam(String varName) {
        for (MatParam matParam : materialDef.getMaterialParams()) {
            if (varName.equals(matParam.getName())) {
                return matParam;
            }
        }
        return null;
    }

    /**
     * finds an UniformBinding representing a WorldParam from the techniqueDef
     *
     * @param varName the name of the WorldParam
     * @return the corresponding UniformBinding to the WorldParam
     */
    protected UniformBinding findWorldParam(String varName) {
        for (UniformBinding worldParam : techniqueDef.getWorldBindings()) {
            if (varName.equals(worldParam.toString())) {
                return worldParam;
            }
        }
        return null;
    }

    /**
     * updates the right variable of the given mapping from a UniformBinding (a
     * WorldParam) it checks if the uniform hasn't already been loaded, add it
     * to the maps if not.
     *
     * @param param the WorldParam UniformBinding
     * @param mapping the mapping
     * @param map the map of uniforms to search into
     * @return true if the param was added to the map
     */
    protected boolean updateRightFromUniforms(UniformBinding param, VariableMapping mapping, Map<String, DeclaredVariable> map) {

        ShaderNodeVariable right = mapping.getRightVariable();
        String name = param.toString();

        DeclaredVariable dv = map.get(name);
        if (dv == null) {
            right.setType(param.getGlslType());
            right.setName(name);
            right.setPrefix("g_");
            dv = new DeclaredVariable(right);
            map.put(right.getName(), dv);
            dv.addNode(shaderNode);
            mapping.setRightVariable(right);
            return true;
        }
        dv.addNode(shaderNode);
        mapping.setRightVariable(dv.var);
        return false;
    }

    /**
     * Updates the right variable of the given mapping from a {@link MatParam} (a
     * WorldParam) it checks if the uniform hasn't already been loaded, add it
     * to the maps if not.
     *
     * @param param   the mat param.
     * @param mapping the mapping.
     * @param map     the map of uniforms to search into.
     * @param statement the statement being read
     * @return true if the param was added to the map.
     * @throws MatParseException in case of a syntax error
     */
    public boolean updateRightFromUniforms(MatParam param, VariableMapping mapping, Map<String, DeclaredVariable> map,
                                           Statement statement) throws MatParseException {

        final ShaderNodeVariable left = mapping.getLeftVariable();
        final ShaderNodeVariable right = mapping.getRightVariable();

        DeclaredVariable dv = map.get(param.getName());

        if (dv == null) {

            right.setType(param.getVarType().getGlslType());
            right.setName(param.getName());
            right.setPrefix("m_");

            if (left.getMultiplicity() != null) {

                if (!param.getVarType().name().endsWith("Array")) {
                    throw new MatParseException(param.getName() + " is not of Array type", statement);
                }

                String multiplicity = left.getMultiplicity();
                try {
                    Integer.parseInt(multiplicity);
                } catch (final NumberFormatException nfe) {
                    // multiplicity is not an int attempting to find for a material parameter.
                    MatParam mp = findMatParam(multiplicity);
                    if (mp != null) {
                        // It's tied to a material param, let's create a define and use this as the multiplicity
                        addDefine(multiplicity, VarType.Int);
                        multiplicity = multiplicity.toUpperCase();
                        left.setMultiplicity(multiplicity);
                        // only declare the variable if the define is defined.
                        left.setCondition(mergeConditions(left.getCondition(), "defined(" + multiplicity + ")", "||"));
                    } else {
                        throw new MatParseException("Wrong multiplicity for variable" + left.getName() + ". " +
                                multiplicity + " should be an int or a declared material parameter.", statement);
                    }
                }

                // the right variable must have the same multiplicity and the same condition.
                right.setMultiplicity(multiplicity);
                right.setCondition(left.getCondition());
            }

            dv = new DeclaredVariable(right);
            map.put(right.getName(), dv);
            dv.addNode(shaderNode);
            mapping.setRightVariable(right);
            return true;
        }

        dv.addNode(shaderNode);
        mapping.setRightVariable(dv.var);

        return false;
    }

    /**
     * Updates a variable from the attribute list.
     *
     * @param right the variable
     * @param mapping the mapping
     */
    public void updateVarFromAttributes(ShaderNodeVariable right, VariableMapping mapping) {
        DeclaredVariable dv = attributes.get(right.getName());
        if (dv == null) {
            dv = new DeclaredVariable(right);
            attributes.put(right.getName(), dv);
            updateRightTypeFromLeftType(mapping);
        } else {
            mapping.setRightVariable(dv.var);
        }
        dv.addNode(shaderNode);
    }

    /**
     * Adds a define to the technique def
     *
     * @param paramName the name of the material parameter
     * @param paramType the type of the material parameter
     */
    public void addDefine(String paramName, VarType paramType) {
        if (techniqueDef.getShaderParamDefine(paramName) == null) {
            techniqueDef.addShaderParamDefine(paramName, paramType, paramName.toUpperCase());
        }
    }

    /**
     * Finds a variable with the given name from the list of variable.
     *
     * @param vars         the list of shader node variables.
     * @param rightVarName the variable name to search for.
     * @return the found variable or null is not found.
     */
    public ShaderNodeVariable findNodeOutput(List<ShaderNodeVariable> vars, String rightVarName) {
        ShaderNodeVariable var = null;
        for (ShaderNodeVariable variable : vars) {
            if (variable.getName().equals(rightVarName)) {
                var = variable;
            }
        }
        return var;
    }

    /**
     * Extracts and checks a condition expression.
     *
     * @param condition the condition expression.
     * @param statement the statement being read.
     * @throws MatParseException if the condition isn't valid.
     */
    public void extractCondition(String condition, Statement statement) throws MatParseException {
        List<String> defines = conditionParser.extractDefines(condition);
        for (String string : defines) {
            MatParam param = findMatParam(string);
            if (param != null) {
                addDefine(param.getName(), param.getVarType());
            } else {
                throw new MatParseException("Invalid condition, condition must match a Material Parameter named " + condition, statement);
            }
        }
    }

    /**
     * Reads an input mapping.
     *
     * @param statement the statement being read.
     * @return the variable mapping.
     * @throws MatParseException if we have a problem with parsing input mapping statement.
     */
    public VariableMapping readInputMapping(Statement statement) throws MatParseException {

        VariableMapping mapping;
        try {
            mapping = parseMapping(statement, IM_HAS_NAME_SPACE);
        } catch (final Exception e) {
            throw new MatParseException("Unexpected mapping format", statement, e);
        }

        final ShaderNodeDefinition definition = shaderNode.getDefinition();
        final ShaderNodeVariable left = mapping.getLeftVariable();
        final ShaderNodeVariable right = mapping.getRightVariable();
        final String expression = mapping.getRightExpression();

        if (!updateVariableFromList(left, definition.getInputs())) {
            throw new MatParseException(left.getName() + " is not an input variable of " + definition.getName(), statement);
        } else if (left.getType().startsWith("sampler") && (right == null || !right.getNameSpace().equals(ShaderGenerator.NAME_SPACE_MAT_PARAM))) {
            throw new MatParseException("Samplers can only be assigned to MatParams", statement);
        }

        if (right == null && expression == null) {
            throw new MatParseException("The mapping doesn't have a right variable or a right expression.", statement);
        }

        if (right == null) {
            return mapping;
        }

        if (right.getNameSpace().equals(ShaderGenerator.NAME_SPACE_GLOBAL)) {
            right.setType("vec4"); // Globals are all vec4 for now (maybe forever...)
            storeGlobal(right, statement);
        } else if (right.getNameSpace().equals(ShaderGenerator.NAME_SPACE_VERTEX_ATTRIBUTE)) {
            if (definition.getType() == ShaderType.Fragment) {
                throw new MatParseException("Cannot have an attribute as input in a fragment shader" + right.getName(), statement);
            }
            updateVarFromAttributes(mapping.getRightVariable(), mapping);
            storeAttribute(mapping.getRightVariable());
        } else if (right.getNameSpace().equals(ShaderGenerator.NAME_SPACE_MAT_PARAM)) {

            MatParam param = findMatParam(right.getName());
            if (param == null) {
                throw new MatParseException("Could not find a Material Parameter named " + right.getName(), statement);
            }

            if (definition.getType() == ShaderType.Vertex) {
                if (updateRightFromUniforms(param, mapping, vertexDeclaredUniforms, statement)) {
                    updateMaterialTextureType(statement, mapping, left, param);
                    storeVertexUniform(mapping.getRightVariable());
                }
            } else {
                if (updateRightFromUniforms(param, mapping, fragmentDeclaredUniforms, statement)) {
                    updateMaterialTextureType(statement, mapping, left, param);
                    storeFragmentUniform(mapping.getRightVariable());
                }
            }

        } else if (right.getNameSpace().equals(ShaderGenerator.NAME_SPACE_WORLD_PARAM)) {

            UniformBinding worldParam = findWorldParam(right.getName());
            if (worldParam == null) {
                throw new MatParseException("Could not find a World Parameter named " + right.getName(), statement);
            }

            if (definition.getType() == ShaderType.Vertex) {
                if (updateRightFromUniforms(worldParam, mapping, vertexDeclaredUniforms)) {
                    storeVertexUniform(mapping.getRightVariable());
                }
            } else {
                if (updateRightFromUniforms(worldParam, mapping, fragmentDeclaredUniforms)) {
                    storeFragmentUniform(mapping.getRightVariable());
                }
            }

        } else {

            ShaderNode node = nodes.get(right.getNameSpace());

            if (node == null) {
                throw new MatParseException("Undeclared node" + right.getNameSpace() +
                        ". Make sure this node is declared before the current node", statement);
            }

            ShaderNodeVariable var = findNodeOutput(node.getDefinition().getOutputs(), right.getName());

            if (var == null) {
                throw new MatParseException("Cannot find output variable" + right.getName() +
                        " form ShaderNode " + node.getName(), statement);
            }

            right.setNameSpace(node.getName());
            right.setType(var.getType());
            right.setMultiplicity(var.getMultiplicity());

            mapping.setRightVariable(right);

            storeVaryings(node, mapping.getRightVariable());
        }

        checkTypes(mapping, statement);

        return mapping;
    }

    /**
     * Updates the material texture type of the variable mapping.
     *
     * @param statement the statement.
     * @param mapping the variable mapping.
     * @param left the left variable.
     * @param param the material parameter.
     * @throws MatParseException if the texture type isn't valid.
     */
    private void updateMaterialTextureType(final Statement statement, final VariableMapping mapping,
                                           final ShaderNodeVariable left, final MatParam param) throws MatParseException {

        if (!mapping.getRightVariable().getType().contains("|")) {
            return;
        }

        final String type = fixSamplerType(left.getType(), mapping.getRightVariable().getType());

        if (type != null) {
            mapping.getRightVariable().setType(type);
        } else {
            throw new MatParseException(param.getVarType().toString() + " can only be matched to one of " +
                    param.getVarType().getGlslType().replaceAll("\\|", ",") + " found " + left.getType(), statement);
        }
    }

    /**
     * Reads an output mapping.
     *
     * @param statement the statement being read.
     * @return the mapping
     * @throws MatParseException if we have a problem with parsing the statement.
     */
    public VariableMapping readOutputMapping(Statement statement) throws MatParseException {

        VariableMapping mapping;
        try {
            mapping = parseMapping(statement, OM_HAS_NAME_SPACE);
        } catch (final Exception e) {
            throw new MatParseException("Unexpected mapping format", statement, e);
        }

        final ShaderNodeDefinition definition = shaderNode.getDefinition();
        final ShaderNodeVariable left = mapping.getLeftVariable();
        final ShaderNodeVariable right = mapping.getRightVariable();

        if (left.getType().startsWith("sampler") || right.getType().startsWith("sampler")) {
            throw new MatParseException("Samplers can only be inputs", statement);
        }

        if (left.getNameSpace().equals(ShaderGenerator.NAME_SPACE_GLOBAL)) {
            left.setType("vec4"); // Globals are all vec4 for now (maybe forever...)
            storeGlobal(left, statement);
        } else {
            throw new MatParseException("Only Global nameSpace is allowed for outputMapping, got" + left.getNameSpace(), statement);
        }

        if (!updateVariableFromList(right, definition.getOutputs())) {
            throw new MatParseException(right.getName() + " is not an output variable of " + definition.getName(), statement);
        }

        checkTypes(mapping, statement);

        return mapping;
    }

    /**
     * Reads a list of ShaderNodes
     *
     * @param statements the list of statements to read
     * @throws IOException if an I/O error occurs
     */
    public void readNodes(List<Statement> statements) throws IOException {
        if (techniqueDef.getShaderNodes() == null) {
            techniqueDef.setShaderNodes(new ArrayList<ShaderNode>());
            techniqueDef.setShaderGenerationInfo(new ShaderGenerationInfo());
        }

        for (Statement statement : statements) {
            String[] split = statement.getLine().split("[ \\{]");
            if (statement.getLine().startsWith("ShaderNode ")) {
                String name = statement.getLine().substring("ShaderNode".length()).trim();
                if (nodes == null) {
                    nodes = new HashMap<String, ShaderNode>();
                }
                if (!nodes.containsKey(name)) {
                    shaderNode = new ShaderNode();
                    shaderNode.setName(name);
                    techniqueDef.getShaderGenerationInfo().getUnusedNodes().add(name);
                    readShaderNode(statement.getContents());
                    nodes.put(name, shaderNode);
                    techniqueDef.getShaderNodes().add(shaderNode);
                } else {
                    throw new MatParseException("ShaderNode " + name + " is already defined", statement);
                }

            } else {
                throw new MatParseException("ShaderNode", split[0], statement);
            }
        }
    }

    /**
     * retrieve the leftType corresponding sampler type from the rightType
     *
     * @param leftType the left samplerType
     * @param rightType the right sampler type (can be multiple types separated
     * by "|"
     * @return the type or null if not found
     */
    public String fixSamplerType(String leftType, String rightType) {
        String[] types = rightType.split("\\|");
        for (String string : types) {
            if (leftType.equals(string)) {
                return string;
            }
        }
        return null;
    }

    /**
     * Stores a global output.
     *
     * @param var the variable to store.
     * @param varStatement the statement being read.
     * @throws MatParseException if we have duplicates of a global vertex output variable.
     */
    public void storeGlobal(ShaderNodeVariable var, Statement varStatement) throws MatParseException {
        var.setShaderOutput(true);

        final ShaderGenerationInfo generationInfo = techniqueDef.getShaderGenerationInfo();
        final ShaderNodeDefinition definition = shaderNode.getDefinition();

        if (definition.getType() == ShaderType.Vertex) {

            ShaderNodeVariable global = generationInfo.getVertexGlobal();

            if (global != null) {

                if (!global.getName().equals(var.getName())) {
                    throw new MatParseException("A global output is already defined for the vertex shader: " +
                            global.getName() + ". vertex shader can only have one global output", varStatement);
                }

            } else {
                generationInfo.setVertexGlobal(var);
            }

        } else if (definition.getType() == ShaderType.Fragment) {
            storeVariable(var, generationInfo.getFragmentGlobals());
        }
    }

    /**
     * Stores an attribute.
     *
     * @param var the variable to store.
     */
    public void storeAttribute(ShaderNodeVariable var) {
        storeVariable(var, techniqueDef.getShaderGenerationInfo().getAttributes());
    }

    /**
     * Stores a vertex uniform.
     *
     * @param var the variable to store.
     */
    public void storeVertexUniform(ShaderNodeVariable var) {
        storeVariable(var, techniqueDef.getShaderGenerationInfo().getVertexUniforms());
    }

    /**
     * store a fragment uniform
     *
     * @param var the variable to store
     */
    public void storeFragmentUniform(ShaderNodeVariable var) {
        storeVariable(var, techniqueDef.getShaderGenerationInfo().getFragmentUniforms());
    }

    /**
     * sets the assetManager
     *
     * @param assetManager for loading assets (alias created)
     */
    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * Find the definition from this statement (loads it if necessary)
     *
     * @param statement the statement being read
     * @return the definition
     * @throws IOException if an I/O error occurs
     */
    public ShaderNodeDefinition findDefinition(Statement statement) throws IOException {

        final String defLine[] = statement.getLine().split(":");

        if (defLine.length != 3) {
            throw new MatParseException("Can't find shader node definition for: ", statement);
        }

        final Map<String, ShaderNodeDefinition> nodeDefinitions = getNodeDefinitions();
        final String definitionName = defLine[1].trim();
        final String definitionPath = defLine[2].trim();
        final String fullName = definitionName + ":" + definitionPath;

        ShaderNodeDefinition def = nodeDefinitions.get(fullName);
        if (def != null) {
            return def;
        }

        List<ShaderNodeDefinition> defs;
        try {
            defs = assetManager.loadAsset(new ShaderNodeDefinitionKey(definitionPath));
        } catch (final AssetNotFoundException e) {
            throw new MatParseException("Couldn't find " + definitionPath, statement, e);
        }

        for (final ShaderNodeDefinition definition : defs) {
            if (definitionName.equals(definition.getName())) {
                def = definition;
            }
            final String key = definition.getName() + ":" + definitionPath;
            if (!(nodeDefinitions.containsKey(key))) {
                nodeDefinitions.put(key, definition);
            }
        }

        if (def == null) {
            throw new MatParseException(definitionName + " is not a declared as Shader Node Definition", statement);
        }

        return def;
    }

    /**
     * store a varying
     *
     * @param node the shaderNode
     * @param variable the variable to store
     */
    public void storeVaryings(ShaderNode node, ShaderNodeVariable variable) {
        variable.setShaderOutput(true);

        final ShaderNodeDefinition nodeDefinition = node.getDefinition();
        final ShaderNodeDefinition currentDefinition = shaderNode.getDefinition();

        if (nodeDefinition.getType() != ShaderType.Vertex ||
                currentDefinition.getType() != ShaderType.Fragment) {
            return;
        }

        final String fullName = node.getName() + "." + variable.getName();

        DeclaredVariable declaredVar = varyings.get(fullName);

        if (declaredVar == null) {
            techniqueDef.getShaderGenerationInfo().getVaryings().add(variable);
            declaredVar = new DeclaredVariable(variable);
            varyings.put(fullName, declaredVar);
        }

        declaredVar.addNode(shaderNode);

        // If a variable is declared with the same name as an input and an output and is a varying,
        // set it as a shader output, so it's declared as a varying only once.
        for (final VariableMapping variableMapping : node.getInputMapping()) {
            final ShaderNodeVariable leftVariable = variableMapping.getLeftVariable();
            if (leftVariable.getName().equals(variable.getName())) {
                leftVariable.setShaderOutput(true);
            }
        }
    }

    /**
     * Merges 2 conditions with the given operator
     *
     * @param condition1 the first condition
     * @param condition2 the second condition
     * @param operator the operator {@literal ("&&" or "||&)}
     * @return the merged condition
     */
    public String mergeConditions(String condition1, String condition2, String operator) {
        if (condition1 != null) {
            if (condition2 == null) {
                return condition1;
            } else {
                String mergedCondition = "(" + condition1 + ") " + operator + " (" + condition2 + ")";
                return mergedCondition;
            }
        } else {
            return condition2;
        }
    }

    /**
     * Searches a variable in a list from its name and merges the conditions of the
     * variables.
     *
     * @param variable the variable.
     * @param varList the variable list.
     */
    public void storeVariable(ShaderNodeVariable variable, List<ShaderNodeVariable> varList) {
        for (ShaderNodeVariable var : varList) {
            if (var.getName().equals(variable.getName())) {
                return;
            }
        }
        varList.add(variable);
    }

    /**
     * check the types of a mapping, left type must match right type take the
     * swizzle into account
     *
     * @param mapping the mapping
     * @param statement1 the statement being read
     * @throws MatParseException in case of a syntax error
     */
    protected void checkTypes(VariableMapping mapping, Statement statement1) throws MatParseException {
        if (!ShaderUtils.typesMatch(mapping)) {
            String ls = mapping.getLeftSwizzling().length() == 0 ? "" : "." + mapping.getLeftSwizzling();
            String rs = mapping.getRightSwizzling().length() == 0 ? "" : "." + mapping.getRightSwizzling();
            throw new MatParseException("Type mismatch, cannot convert " + mapping.getRightVariable().getType() + rs + " to " + mapping.getLeftVariable().getType() + ls, statement1);
        }
        if (!ShaderUtils.multiplicityMatch(mapping)) {
            String type1 = mapping.getLeftVariable().getType() + "[" + mapping.getLeftVariable().getMultiplicity() + "]";
            String type2 = mapping.getRightVariable().getType() + "[" + mapping.getRightVariable().getMultiplicity() + "]";
            throw new MatParseException("Type mismatch, cannot convert " + type1 + " to " + type2, statement1);
        }
    }

    private Map<String, ShaderNodeDefinition> getNodeDefinitions() {
        if (nodeDefinitions == null) {
            nodeDefinitions = new HashMap<>();
        }
        return nodeDefinitions;
    }


    public void clear() {
        nodeDefinitions.clear();
        nodes.clear();
        shaderNodeDefinition = null;
        shaderNode = null;
        techniqueDef = null;
        attributes.clear();
        vertexDeclaredUniforms.clear();
        fragmentDeclaredUniforms.clear();
        varyings.clear();
        materialDef = null;
        shaderLanguage = "";
        shaderName = "";
        varNames.clear();
        assetManager = null;
        nulledConditions.clear();
    }
}
