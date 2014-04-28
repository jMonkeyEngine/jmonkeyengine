/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
     * decalre a list of uniforms
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
     * attributes are all declared, inPositon is decalred even if it's not in
     * the list and it's condition is nulled.
     */
    @Override
    protected void generateAttributes(StringBuilder source, ShaderGenerationInfo info) {
        source.append("\n");
        boolean inPosition = false;
        for (ShaderNodeVariable var : info.getAttributes()) {
            if (var.getName().equals("inPosition")) {
                inPosition = true;
                var.setCondition(null);
            }
            declareAttribute(source, var);

        }
        if (!inPosition) {
            declareAttribute(source, new ShaderNodeVariable("vec4", "inPosition"));
        }

    }

    @Override
    protected void generateVaryings(StringBuilder source, ShaderGenerationInfo info, ShaderType type) {
        source.append("\n");
        for (ShaderNodeVariable var : info.getVaryings()) {
            declareVarying(source, var, type == ShaderType.Vertex ? false : true);
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
        source.append("void main(){\n");
        indent();
        appendIndent(source);
        if (type == ShaderType.Vertex) {
            declareVariable(source, info.getVertexGlobal(), "inPosition");
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
     * Appends an ouput assignment to a shader globalOutputName =
     * nameSpace_varName;
     *
     * @param source the source StringBuilter to append the code.
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
     * this methods does things in this order : 
     * 
     * 1. declaring and mapping input<br>
     * variables : variable replaced with MatParams or WorldParams are not
     * declared and are replaced by the parma acual name in the code. For others
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
     *<br>
     * All of this is embed in a #if coditional statement if needed
     */
    @Override
    protected void generateNodeMainSection(StringBuilder source, ShaderNode shaderNode, String nodeSource, ShaderGenerationInfo info) {

        nodeSource = updateDefinesName(nodeSource, shaderNode);
        source.append("\n");
        comment(source, shaderNode, "Begin");
        startCondition(shaderNode.getCondition(), source);

        List<String> declaredInputs = new ArrayList<String>();
        for (VariableMapping mapping : shaderNode.getInputMapping()) {

            //all variables fed with a matparam or world param are replaced but the matparam itself
            //it avoids issue with samplers that have to be uniforms, and it optimize a but the shader code.
            if (isWorldOrMaterialParam(mapping.getRightVariable())) {
                nodeSource = replace(nodeSource, mapping.getLeftVariable(), mapping.getRightVariable().getName());
            } else {
                if (mapping.getLeftVariable().getType().startsWith("sampler")) {
                    throw new IllegalArgumentException("a Sampler must be a uniform");
                }
                map(mapping, source);
                String newName = shaderNode.getName() + "_" + mapping.getLeftVariable().getName();
                if (!declaredInputs.contains(newName)) {
                    nodeSource = replace(nodeSource, mapping.getLeftVariable(), newName);
                    declaredInputs.add(newName);
                }
            }
        }
       
        for (ShaderNodeVariable var : shaderNode.getDefinition().getOutputs()) {
            ShaderNodeVariable v = new ShaderNodeVariable(var.getType(), shaderNode.getName(), var.getName());
            if (!declaredInputs.contains(shaderNode.getName() + "_" + var.getName())) {
                if (!isVarying(info, v)) {
                    declareVariable(source, v);
                }
                nodeSource = replaceVariableName(nodeSource, v);
            }
        }
        
        source.append(nodeSource);
   
        for (VariableMapping mapping : shaderNode.getOutputMapping()) {
            map(mapping, source);
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
     * @param value the initialization value to assign the the variable
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
     * @param value the initialization value to assign the the variable
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
     * @param mapping the VariableMapping to append
     * @param source the StringBuilder to use    
     */
    protected void map(VariableMapping mapping, StringBuilder source) {
        startCondition(mapping.getCondition(), source);
        appendIndent(source);
        if (!mapping.getLeftVariable().isShaderOutput()) {
            source.append(mapping.getLeftVariable().getType());
            source.append(" ");
        }
        source.append(mapping.getLeftVariable().getNameSpace());
        source.append("_");
        source.append(mapping.getLeftVariable().getName());
        if (mapping.getLeftSwizzling().length() > 0) {
            source.append(".");
            source.append(mapping.getLeftSwizzling());
        }
        source.append(" = ");
        String namePrefix = getAppendableNameSpace(mapping.getRightVariable());
        source.append(namePrefix);
        source.append(mapping.getRightVariable().getName());
        if (mapping.getRightSwizzling().length() > 0) {
            source.append(".");
            source.append(mapping.getRightSwizzling());
        }
        source.append(";\n");
        endCondition(mapping.getCondition(), source);
    }

    /**
     * replaces a variable name in a shaderNode source code by prefixing it 
     * with its nameSpace and "_" if needed.
     * @param nodeSource the source ot modify
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
     * Attributes, WorldParam and MatParam names psace must not be appended
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
     * Complex condition synthax are handled.     
     * 
     * @param nodeSource the sahderNode source code
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
        nodeSource = nodeSource.replaceAll("(\\W)" + var.getName() + "(\\W)", "$1" + newName + "$2");
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
     * @param source 
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
     * @param var the variable to declare as an varying
     * @param input a boolean set to true if the this varying is an input.
     * this in not used in this implementaiton but can be used in overidings 
     * implementation
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
}
