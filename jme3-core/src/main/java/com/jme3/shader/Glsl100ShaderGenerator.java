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
package com.jme3.shader;

import com.jme3.asset.AssetManager;
import com.jme3.material.ShaderGenerationInfo;
import com.jme3.material.plugins.ConditionParser;
import com.jme3.shader.Shader.ShaderType;

import java.util.ArrayList;
import java.util.List;

/**
 * This shader Generator can generate Vertex and Fragment shaders from
 * shadernodes for GLSL 1.0
 *
 * @author Nehon
 */
public class Glsl100ShaderGenerator extends ShaderGenerator {

    /**
     * the indentation characters 1à tabulation characters
     */
    private final static String INDENTCHAR = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t";

    protected ShaderNodeVariable inPosTmp;

    /**
     * creates a Glsl100ShaderGenerator
     * @param assetManager the assetManager
     */
    public Glsl100ShaderGenerator(AssetManager assetManager) {
        super(assetManager);
    }

    @Override
    protected void generateUniforms(StringBuilder source, ShaderGenerationInfo info, ShaderType type) {
        generateUniforms(source, type == ShaderType.Vertex ? info.getVertexUniforms() : info.getFragmentUniforms());
    }

    /**
     * declare a list of uniforms
     *
     * @param source the source to append to
     * @param uniforms the list of uniforms
     */
    protected void generateUniforms(StringBuilder source, List<ShaderNodeVariable> uniforms) {
        source.append("\n");
        for (ShaderNodeVariable var : uniforms) {
            declareVariable(source, var, false, "uniform");
        }
    }

    /**
     * {@inheritDoc}
     *
     * attributes are all declared, inPosition is declared even if it's not in
     * the list and its condition is nulled.
     */
    @Override
    protected void generateAttributes(StringBuilder source, ShaderGenerationInfo info) {
        source.append("\n");
        boolean inPosition = false;
        for (ShaderNodeVariable var : info.getAttributes()) {
            if (var.getName().equals("inPosition")) {
                inPosition = true;
                var.setCondition(null);
                fixInPositionType(var);
                //keep track on the InPosition variable to avoid iterating through attributes again
                inPosTmp = var;
            }
            declareAttribute(source, var);

        }
        if (!inPosition) {
            inPosTmp = new ShaderNodeVariable("vec3", "inPosition");
            declareAttribute(source, inPosTmp);
        }

    }

    @Override
    protected void generateVaryings(StringBuilder source, ShaderGenerationInfo info, ShaderType type) {
        source.append("\n");
        for (ShaderNodeVariable var : info.getVaryings()) {
            declareVarying(source, var, type != ShaderType.Vertex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * if the declaration contains no code nothing is done, else it's appended
     */
    @Override
    protected void generateDeclarativeSection(StringBuilder source, ShaderNode shaderNode, String nodeSource, ShaderGenerationInfo info) {
        if (nodeSource.replaceAll("\\n", "").trim().length() > 0) {
            nodeSource = updateDefinesName(nodeSource, shaderNode);
            source.append("\n");
            unIndent();
            startCondition(shaderNode.getCondition(), source);
            source.append(nodeSource);
            source.append("\n");
            endCondition(shaderNode.getCondition(), source);
            indent();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Shader outputs are declared and initialized inside the main section
     */
    @Override
    protected void generateStartOfMainSection(StringBuilder source, ShaderGenerationInfo info, ShaderType type) {
        source.append("\n");
        source.append("void main() {\n");
        indent();
        appendIndent(source);
        if (type == ShaderType.Vertex) {
            declareGlobalPosition(info, source);
        } else if (type == ShaderType.Fragment) {
            for (ShaderNodeVariable global : info.getFragmentGlobals()) {
                declareVariable(source, global, "vec4(1.0)");
            }
        }
        source.append("\n");
    }

    /**
     * {@inheritDoc}
     *
     * outputs are assigned to built in glsl output. then the main section is
     * closed
     *
     * This code accounts for multi render target and correctly output to
     * gl_FragData if several output are declared for the fragment shader
     */
    @Override
    protected void generateEndOfMainSection(StringBuilder source, ShaderGenerationInfo info, ShaderType type) {
        source.append("\n");
        if (type == ShaderType.Vertex) {
            appendOutput(source, "gl_Position", info.getVertexGlobal());
        } else if (type == ShaderType.Fragment) {
            List<ShaderNodeVariable> globals = info.getFragmentGlobals();
            if (globals.size() == 1) {
                appendOutput(source, "gl_FragColor", globals.get(0));
            } else {
                int i = 0;
                //Multi Render Target
                for (ShaderNodeVariable global : globals) {
                    appendOutput(source, "gl_FragData[" + i + "]", global);
                    i++;
                }
            }
        }
        unIndent();
        appendIndent(source);
        source.append("}\n");
    }

    /**
     * Appends an output assignment to a shader globalOutputName =
     * nameSpace_varName;
     *
     * @param source the source StringBuilder to append the code.
     * @param globalOutputName the name of the global output (can be gl_Position
     * or gl_FragColor etc...).
     * @param var the variable to assign to the output.
     */
    protected void appendOutput(StringBuilder source, String globalOutputName, ShaderNodeVariable var) {
        appendIndent(source);
        source.append(globalOutputName);
        source.append(" = ");
        source.append(var.getNameSpace());
        source.append("_");
        source.append(var.getName());
        source.append(";\n");
    }

    /**
     * {@inheritDoc}
     *
     * This method does things in the following order:
     * 
     * 1. declaring and mapping input<br>
     * variables : variable replaced with MatParams or WorldParams that are Samplers are not
     * declared and are replaced by the param actual name in the code. For others
     * variables, the name space is appended with a "_" before the variable name
     * in the code to avoid names collision between shaderNodes. <br>
     * 
     * 2. declaring output variables : <br>
     * variables are declared if they were not already
     * declared as input (inputs can also be outputs) or if they are not
     * declared as varyings. The variable name is also prefixed with the s=name
     * space and "_" in the shaderNode code <br>
     * 
     * 3. append of the actual ShaderNode code <br>
     * 
     * 4. mapping outputs to global output if needed<br>
     * 
     *
     *<br>
     * All of this is embedded in a #if conditional statement if necessary.
     */
    @Override
    protected void generateNodeMainSection(StringBuilder source, ShaderNode shaderNode, String nodeSource, ShaderGenerationInfo info) {

        nodeSource = updateDefinesName(nodeSource, shaderNode);
        source.append("\n");
        comment(source, shaderNode, "Begin");
        startCondition(shaderNode.getCondition(), source);

        final List<String> declaredInputs = new ArrayList<>();

        // Declaring variables with default values first
        final ShaderNodeDefinition definition = shaderNode.getDefinition();

        for (final ShaderNodeVariable var : definition.getInputs()) {

            if (var.getType().startsWith("sampler")) {
                continue;
            }

            final String fullName = shaderNode.getName() + "_" + var.getName();

            final ShaderNodeVariable variable = new ShaderNodeVariable(var.getType(), shaderNode.getName(),
                    var.getName(), var.getMultiplicity());

            if (!isVarying(info, variable)) {
                declareVariable(source, variable, var.getDefaultValue(), true, null);
            }

            nodeSource = replaceVariableName(nodeSource, variable);
            declaredInputs.add(fullName);
        }

        for (VariableMapping mapping : shaderNode.getInputMapping()) {

            final ShaderNodeVariable rightVariable = mapping.getRightVariable();
            final ShaderNodeVariable leftVariable = mapping.getLeftVariable();

            String newName = shaderNode.getName() + "_" + leftVariable.getName();
            boolean isDeclared = declaredInputs.contains(newName);
            //Variables fed with a sampler matparam or world param are replaced by the matparam itself
            //It avoids issue with samplers that have to be uniforms.
            if (rightVariable != null && isWorldOrMaterialParam(rightVariable) && rightVariable.getType().startsWith("sampler")) {
                nodeSource = replace(nodeSource, leftVariable, rightVariable.getPrefix() + rightVariable.getName());
            } else {

                if (leftVariable.getType().startsWith("sampler")) {
                    throw new IllegalArgumentException("a Sampler must be a uniform");
                }
                map(mapping, source, !isDeclared);
            }

            if (!isDeclared) {
                nodeSource = replace(nodeSource, leftVariable, newName);
                declaredInputs.add(newName);
            }
        }



        for (ShaderNodeVariable var : definition.getOutputs()) {
            ShaderNodeVariable v = new ShaderNodeVariable(var.getType(), shaderNode.getName(), var.getName(), var.getMultiplicity());
            if (!declaredInputs.contains(shaderNode.getName() + "_" + var.getName())) {
                if (!isVarying(info, v)) {
                    declareVariable(source, v);
                }
                nodeSource = replaceVariableName(nodeSource, v);
            }
        }
        
        source.append(nodeSource);
   
        for (VariableMapping mapping : shaderNode.getOutputMapping()) {
            map(mapping, source, true);
        }
        endCondition(shaderNode.getCondition(), source);
        comment(source, shaderNode, "End");
    }

    /**
     * declares a variable, embed in a conditional block if needed
     * @param source the StringBuilder to use
     * @param var the variable to declare
     * @param appendNameSpace true to append the nameSpace + "_"
     */
    protected void declareVariable(StringBuilder source, ShaderNodeVariable var, boolean appendNameSpace) {
        declareVariable(source, var, appendNameSpace, null);
    }

    /**
     * declares a variable, embed in a conditional block if needed. the namespace is appended with "_"
     * @param source the StringBuilder to use
     * @param var the variable to declare    
     */
    protected void declareVariable(StringBuilder source, ShaderNodeVariable var) {
        declareVariable(source, var, true, null);
    }

     /**
     * declares a variable, embed in a conditional block if needed. the namespace is appended with "_"
     * @param source the StringBuilder to use
     * @param var the variable to declare    
     * @param value the initialization value to assign the variable
     */
    protected void declareVariable(StringBuilder source, ShaderNodeVariable var, String value) {
        declareVariable(source, var, value, true, null);
    }

    /**
     * declares a variable, embed in a conditional block if needed.
     * @param source the StringBuilder to use
     * @param var the variable to declare    
     * @param appendNameSpace true to append the nameSpace + "_"
     * @param modifier the modifier of the variable (attribute, varying, in , out,...)
     */
    protected void declareVariable(StringBuilder source, ShaderNodeVariable var, boolean appendNameSpace, String modifier) {
        declareVariable(source, var, null, appendNameSpace, modifier);
    }

    /**
     * declares a variable, embed in a conditional block if needed.
     * @param source the StringBuilder to use
     * @param var the variable to declare    
     * @param value the initialization value to assign the variable
     * @param appendNameSpace true to append the nameSpace + "_"
     * @param modifier the modifier of the variable (attribute, varying, in , out,...)
     */
    protected void declareVariable(StringBuilder source, ShaderNodeVariable var, String value, boolean appendNameSpace, String modifier) {
        startCondition(var.getCondition(), source);
        appendIndent(source);
        if (modifier != null) {
            source.append(modifier);
            source.append(" ");
        }

        source.append(var.getType());
        source.append(" ");
        if (appendNameSpace) {
            source.append(var.getNameSpace());
            source.append("_");
        }
        source.append(var.getPrefix());
        source.append(var.getName());
        if (var.getMultiplicity() != null) {
            source.append("[");
            source.append(var.getMultiplicity().toUpperCase());
            source.append("]");
        }
        if (value != null) {
            source.append(" = ");
            source.append(value);
        }
        source.append(";\n");
        endCondition(var.getCondition(), source);
    }

    /**
     * Starts a conditional block
     * @param condition the block condition
     * @param source the StringBuilder to use
     */
    protected void startCondition(String condition, StringBuilder source) {
        if (condition != null) {
            appendIndent(source);
            source.append("#if ");
            source.append(condition);
            source.append("\n");
            indent();
        }
    }

    /**
     * Ends a conditional block
     * @param condition the block condition
     * @param source the StringBuilder to use
     */
    protected void endCondition(String condition, StringBuilder source) {
        if (condition != null) {
            unIndent();
            appendIndent(source);
            source.append("#endif\n");

        }
    }

    /**
     * Appends a mapping to the source, embed in a conditional block if needed,
     * with variables nameSpaces and swizzle.
     *
     * @param mapping the VariableMapping to append
     * @param source  the StringBuilder to use
     * @param declare true to declare the variable, false if already declared
     */
    protected void map(VariableMapping mapping, StringBuilder source, boolean declare) {

        final ShaderNodeVariable leftVariable = mapping.getLeftVariable();
        final ShaderNodeVariable rightVariable = mapping.getRightVariable();
        final String rightExpression = mapping.getRightExpression();

        startCondition(mapping.getCondition(), source);
        appendIndent(source);
        if (!leftVariable.isShaderOutput() &&  declare) {
            source.append(leftVariable.getType());
            source.append(" ");
        }
        source.append(leftVariable.getNameSpace());
        source.append("_");
        source.append(leftVariable.getName());
        if (leftVariable.getMultiplicity() != null){
            source.append("[");
            source.append(leftVariable.getMultiplicity());
            source.append("]");
        }

        // left swizzle, the variable can't be declared and assigned on the same line.
        if (mapping.getLeftSwizzling().length() > 0) {
            //initialize the declared variable to 0.0
            source.append(" = ");
            source.append(leftVariable.getType());
            source.append("(0.0);\n");
            appendIndent(source);
            // assign the value on a new line
            source.append(leftVariable.getNameSpace());
            source.append("_");
            source.append(leftVariable.getName());
            source.append(".");
            source.append(mapping.getLeftSwizzling());
        }
        source.append(" = ");

        if (rightVariable != null) {

            String namePrefix = getAppendableNameSpace(rightVariable);
            source.append(namePrefix);
            source.append(rightVariable.getPrefix());
            source.append(rightVariable.getName());

            if (mapping.getRightSwizzling().length() > 0) {
                source.append(".");
                source.append(mapping.getRightSwizzling());
            }

        } else {
            source.append(rightExpression);
        }

        source.append(";\n");
        endCondition(mapping.getCondition(), source);
    }

    /**
     * replaces a variable name in a shaderNode source code by prefixing it
     * with its nameSpace and "_" if needed.
     * @param nodeSource the source to modify
     * @param var the variable to replace
     * @return the modified source
     */
    protected String replaceVariableName(String nodeSource, ShaderNodeVariable var) {
        String namePrefix = getAppendableNameSpace(var);
        String newName = namePrefix + var.getName();
        nodeSource = replace(nodeSource, var, newName);
        return nodeSource;
    }

    /**
     * Finds if a variable is a varying
     * @param info the ShaderGenerationInfo
     * @param v the variable
     * @return true is the given variable is a varying
     */
    protected boolean isVarying(ShaderGenerationInfo info, ShaderNodeVariable v) {
        boolean isVarying = false;
        for (ShaderNodeVariable shaderNodeVariable : info.getVaryings()) {
            if (shaderNodeVariable.equals(v)) {
                isVarying = true;
            }
        }
        return isVarying;
    }

    /**
     * Appends a comment to the generated code
     * @param source the StringBuilder to use 
     * @param shaderNode the shader node being processed (to append its name)
     * @param comment the comment to append
     */
    protected void comment(StringBuilder source, ShaderNode shaderNode, String comment) {        
        appendIndent(source);
        source.append("//");
        source.append(shaderNode.getName());
        source.append(" : ");
        source.append(comment);
        source.append("\n");
    }

    /**
     * returns the name space to append for a variable. 
     * Attributes, WorldParam and MatParam names space must not be appended
     * @param var the variable
     * @return the namespace to append for this variable
     */
    protected String getAppendableNameSpace(ShaderNodeVariable var) {
        String namePrefix = var.getNameSpace() + "_";
        if (namePrefix.equals("Attr_") || namePrefix.equals("WorldParam_") || namePrefix.equals("MatParam_")) {
            namePrefix = "";
        }
        return namePrefix;
    }

    /**
     * transforms defines name is the shader node code.
     * One can use a #if defined(inputVariableName) in a shaderNode code.
     * This method is responsible for changing the variable name with the 
     * appropriate defined based on the mapping condition of this variable.
     * Complex condition syntax are handled.     
     * 
     * @param nodeSource the shaderNode source code
     * @param shaderNode the ShaderNode being processed
     * @return the modified shaderNode source.
     */
    protected String updateDefinesName(String nodeSource, ShaderNode shaderNode) {
        String[] lines = nodeSource.split("\\n");
        ConditionParser parser = new ConditionParser();
        for (String line : lines) {

            if (line.trim().startsWith("#if")) {
                List<String> params = parser.extractDefines(line.trim());
                String l = line.trim().replaceAll("defined", "").replaceAll("#if ", "").replaceAll("#ifdef", "");//parser.getFormattedExpression();
                boolean match = false;
                for (String param : params) {
                    for (VariableMapping map : shaderNode.getInputMapping()) {
                        if ((map.getLeftVariable().getName()).equals(param)) {
                            if (map.getCondition() != null) {
                                l = l.replaceAll(param, map.getCondition());
                                match = true;
                            }
                        }
                    }
                }
                if (match) {
                    nodeSource = nodeSource.replace(line.trim(), "#if " + l);
                }
            }
        }
        return nodeSource;
    }

    /**
     * replaced a variable name in a source code with the given name
     * @param nodeSource the source to use
     * @param var the variable
     * @param newName the new name of the variable
     * @return the modified source code
     */
    protected String replace(String nodeSource, ShaderNodeVariable var, String newName) {
        nodeSource = nodeSource.replaceAll("(?<=\\W)" + var.getName() + "(?=\\W)",  newName);
        return nodeSource;
    }

    /**
     * Finds if a variable is a world or a material parameter
     * @param var the variable
     * @return true if the variable is a Word or material parameter
     */
    protected boolean isWorldOrMaterialParam(ShaderNodeVariable var) {
        return var.getNameSpace().equals("MatParam") || var.getNameSpace().equals("WorldParam");
    }

    @Override
    protected String getLanguageAndVersion(ShaderType type) {
        return "GLSL100";
    }

    /**
     * appends indentation.
     * @param source the builder to append to
     */
    protected void appendIndent(StringBuilder source) {
        source.append(INDENTCHAR.substring(0, indent));
    }

    /**
     * Declares an attribute
     * @param source the StringBuilder to use
     * @param var the variable to declare as an attribute
     */    
    protected void declareAttribute(StringBuilder source, ShaderNodeVariable var) {
        declareVariable(source, var, false, "attribute");
    }

    /**
     * Declares a varying
     * @param source the StringBuilder to use
     * @param var the variable to declare as a varying
     * @param input a boolean set to true if the varying is an input.
     * This in not used in this implementation, but can be used in overriding
     * implementations.
     */
    protected void declareVarying(StringBuilder source, ShaderNodeVariable var, boolean input) {
        declareVariable(source, var, true, "varying");
    }

    /**
     * Decrease indentation with a check so the indent is never negative.
     */
    protected void unIndent() {
        indent--;
        indent = Math.max(0, indent);
    }

    /**
     * increase indentation with a check so that indentation is never over 10
     */
    protected void indent() {
        indent++;
        indent = Math.min(10, indent);
    }

    /**
     * makes sure inPosition attribute is of type vec3 or vec4
     * @param var the inPosition attribute
     */
    protected void fixInPositionType(ShaderNodeVariable var) {
        if(!var.getType().equals("vec3") || !var.getType().equals("vec4")){
            var.setType("vec3");
        }
    }

    /**
     * declare and assign the global position in the vertex shader.
     * @param info the shader generation info
     * @param source the shader source being generated
     */
    protected void declareGlobalPosition(ShaderGenerationInfo info, StringBuilder source) {
        if(inPosTmp.getType().equals(info.getVertexGlobal().getType())){
            declareVariable(source, info.getVertexGlobal(), "inPosition");
        }else{
            declareVariable(source, info.getVertexGlobal(), "vec4(inPosition,1.0)");
        }
    }
}
