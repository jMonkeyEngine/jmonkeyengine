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
import com.jme3.material.*;
import com.jme3.material.plugins.ConditionParser;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.plugins.ShaderAssetKey;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderUtils {

    private static ConditionParser parser = new ConditionParser();

    // matches "defaults (<defaultParam1>, <defaultParam2>, ...)"
    private final static Pattern defaultsPattern = Pattern.compile("defaults\\s*\\(\\s*(.*)\\s*\\)");
    // matches "<type> <functionName>("
    private final static Pattern typeNamePattern = Pattern.compile("(\\w+)\\s+(\\w+)\\s*\\(");
    // matches "const? <in/out> <type> <parmaName>,"
    private final static Pattern paramsPattern = Pattern.compile("((const)?\\s*(\\w+)\\s+(\\w+)\\s+(\\w+)\\s*[,\\)])");

    /**
     * Check if a mapping is valid by checking the types and swizzle of both of
     * the variables
     *
     * @param mapping the mapping
     * @return true if this mapping is valid
     */
    public static boolean typesMatch(VariableMapping mapping) {
        String leftType = mapping.getLeftVariable().getType();
        String rightType = mapping.getRightVariable().getType();
        String leftSwizzling = mapping.getLeftSwizzling();
        String rightSwizzling = mapping.getRightSwizzling();

        //types match : no error
        if (leftType.equals(rightType) && leftSwizzling.length() == rightSwizzling.length()) {
            return true;
        }
        if (isSwizzlable(leftType) && isSwizzlable(rightType)) {
            if (getCardinality(leftType, leftSwizzling) == getCardinality(rightType, rightSwizzling)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a mapping is valid by checking the multiplicity of both of
     * the variables if they are arrays
     *
     * @param mapping the mapping
     * @return true if this mapping is valid
     */
    public static boolean multiplicityMatch(VariableMapping mapping) {
        String leftMult = mapping.getLeftVariable().getMultiplicity();
        String rightMult = mapping.getRightVariable().getMultiplicity();

        if (leftMult == null) {
            if (rightMult != null) {
                return false;
            }
        } else {
            if (rightMult == null) {
                return false;
            } else {
                if (!leftMult.equalsIgnoreCase(rightMult)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * return the cardinality of a type and a swizzle example : vec4 cardinality
     * is 4 float cardinality is 1 vec4.xyz cardinality is 3. sampler2D
     * cardinality is 0
     *
     * @param type      the glsl type
     * @param swizzling the swizzling of a variable
     * @return the cardinality
     */
    public static int getCardinality(String type, String swizzling) {
        int card = 0;
        if (isSwizzlable(type)) {
            if (type.equals("float")) {
                card = 1;
                if (swizzling.length() != 0) {
                    card = 0;
                }
            } else {
                card = Integer.parseInt(type.replaceAll(".*vec", ""));

                if (swizzling.length() > 0) {
                    card = swizzling.length();
                }
            }
        }
        return card;
    }

    /**
     * returns true if a variable of the given type can have a swizzle
     *
     * @param type the glsl type
     * @return true if a variable of the given type can have a swizzle
     */
    public static boolean isSwizzlable(String type) {
        return type.indexOf("vec4") > -1 || type.indexOf("vec3") > -1 || type.indexOf("vec2") > -1 || type.equals("float");
    }

    public static List<ShaderNodeDefinition> parseDefinitions(String glsl) throws ParseException {
        List<ShaderNodeDefinition> defs = new ArrayList<>();
        String nodesCode[] = glsl.split("#pragma ShaderNode");
        for (String code : nodesCode) {
            if (code.trim().length() == 0) {
                continue;
            }
            int firstCr = code.indexOf("\n");
            int firstBracket = code.indexOf("{");
            String pragma = code.substring(0, firstCr);
            Matcher m1 = defaultsPattern.matcher(pragma);
            String[] defaults = null;
            if (m1.find()) {
                defaults = m1.group(1).split(",");
            }

            code = code.substring(firstCr + 1, firstBracket);

            Matcher m = typeNamePattern.matcher(code);

            String returnType = null;
            String functionName = null;
            while (m.find()) {
                returnType = m.group(1);
                functionName = m.group(2);
            }
            if (returnType == null || functionName == null) {
                throw new ParseException("Unmatched return type or function name in \n" + code, firstCr + 1);
            }

            ShaderNodeDefinition def = new ShaderNodeDefinition();
            def.setName(functionName);
            def.setReturnType(returnType);

            m.reset();
            m.usePattern(paramsPattern);

            List<ShaderNodeVariable> inputs = new ArrayList<>();
            List<ShaderNodeVariable> outputs = new ArrayList<>();
            List<ShaderNodeVariable> params = new ArrayList<>();

            if (!returnType.equals("void")) {
                ShaderNodeVariable result = new ShaderNodeVariable(returnType, "result");
                outputs.add(result);
            }

            int cpt = 0;
            while (m.find()) {
                String dir = m.group(3);
                String type = m.group(4);
                String varName = m.group(5);
                ShaderNodeVariable v = new ShaderNodeVariable(type, varName);
                params.add(v);
                String defVal = null;
                if (defaults != null && defaults.length > cpt) {
                    defVal = defaults[cpt].trim();
                    defVal = defVal.isEmpty() ? null : defVal;
                }
                v.setDefaultValue(defVal);
                switch (dir) {
                    case "in":
                        inputs.add(v);
                        break;
                    case "out":
                        outputs.add(v);
                        break;
                    default:
                        throw new ParseException("Missing in or out keyword for variable " + varName + " in function " + functionName, m.start());
                }
                cpt++;
            }

            def.setParams(params);
            def.setInputs(inputs);
            if (outputs.isEmpty()) {
                def.setNoOutput(true);
            } else {
                def.setOutputs(outputs);
            }

            defs.add(def);
        }

        return defs;
    }

    public static Shader.ShaderType getShaderType(String shaderPath) {
        String ext = shaderPath.substring(shaderPath.lastIndexOf(".") + 1);
        return Shader.ShaderType.fromExtention(ext);
    }

    public static List<ShaderNodeDefinition> loadSahderNodeDefinition(AssetManager assetManager, String definitionPath) throws ParseException {
        Map<String, String> sources = (Map<String, String>) assetManager.loadAsset(new ShaderAssetKey(definitionPath, false));
        String glsl = sources.get("[main]");
        List<ShaderNodeDefinition> defs = ShaderUtils.parseDefinitions(glsl);
        Shader.ShaderType type = ShaderUtils.getShaderType(definitionPath);
        for (ShaderNodeDefinition d : defs) {
            d.setType(type);
            d.getShadersLanguage().add("GLSL100");
            d.setPath(definitionPath);
            d.getShadersPath().add(definitionPath);
        }

        return defs;
    }

    public static String getDefaultAttributeType(VertexBuffer.Type type) {
        switch (type) {

            case BoneWeight:
            case BindPoseNormal:
            case Binormal:
            case Normal:
                return "vec3";
            case Size:
                return "float";
            case Position:
            case BindPosePosition:
            case BindPoseTangent:
            case Tangent:
            case Color:
                return "vec4";
            case InterleavedData:
                return "int";
            case Index:
                return "uint";
            case BoneIndex:
                return "uvec4";
            case TexCoord:
            case TexCoord2:
            case TexCoord3:
            case TexCoord4:
            case TexCoord5:
            case TexCoord6:
            case TexCoord7:
            case TexCoord8:
                return "vec2";
            default:
                return "float";
        }
    }

    /**
     * TODO put this in core but this should be changed to handle all kinds of shader not just Vertex and Fragments.
     * TODO Also getShaderGenerationInfo ArrayLists should be sets really, to avoid duplicated and not have to avoid them ourselves.
     * This method could be in core actually and be used after loading a techniqueDef.
     * It computes all the information needed to generate the shader faster, from the ShaderNodes.
     *
     * @param technique
     */
    public static void computeShaderNodeGenerationInfo(TechniqueDef technique, MaterialDef matDef) {


        List<ShaderNodeVariable> attributes = technique.getShaderGenerationInfo().getAttributes();
        List<ShaderNodeVariable> fragmentGlobals = technique.getShaderGenerationInfo().getFragmentGlobals();
        List<ShaderNodeVariable> fragmentUniforms = technique.getShaderGenerationInfo().getFragmentUniforms();
        List<ShaderNodeVariable> vertexUniforms = technique.getShaderGenerationInfo().getVertexUniforms();
        List<ShaderNodeVariable> varyings = technique.getShaderGenerationInfo().getVaryings();
        List<String> unusedNodes = technique.getShaderGenerationInfo().getUnusedNodes();
        attributes.clear();
        fragmentGlobals.clear();
        fragmentUniforms.clear();
        vertexUniforms.clear();
        varyings.clear();
        unusedNodes.clear();

        //considering that none of the nodes are used, we'll remove them from the list when we have proof they are actually used.
        for (ShaderNode shaderNode : technique.getShaderNodes()) {
            unusedNodes.add(shaderNode.getName());
        }
        for (ShaderNode sn : technique.getShaderNodes()) {
            checkDefineFromCondition(sn.getCondition(), matDef, technique);
            ShaderNodeDefinition def = sn.getDefinition();
            List<VariableMapping> in = sn.getInputMapping();
            if (in != null) {
                for (VariableMapping map : in) {
                    checkDefineFromCondition(map.getCondition(), matDef, technique);
                    if (map.getRightExpression() != null) {
                        continue;
                    }
                    ShaderNodeVariable var = map.getRightVariable();
                    if (var.getNameSpace().equals("Global")) {
                        computeGlobals(technique, fragmentGlobals, def, var);
                    } else if (var.getNameSpace().equals("Attr")) {
                        addUnique(attributes, var);
                    } else if (var.getNameSpace().equals("MatParam") || var.getNameSpace().equals("WorldParam")) {
                        checkMultiplicity(technique, matDef, map, var);
                        if (def.getType() == Shader.ShaderType.Fragment) {
                            addUnique(fragmentUniforms, var);
                        } else {
                            addUnique(vertexUniforms, var);
                        }
                    } else {
                        //the nameSpace is the name of another node, if it comes from a different type of node the var is a varying
                        ShaderNode otherNode = null;
                        otherNode = findShaderNodeByName(technique, var.getNameSpace());
                        if (otherNode == null) {
                            //we have a problem this should not happen...but let's not crash...
                            //TODO Maybe we could have an error list and report in it, then present the errors to the user.
                            continue;
                        }
                        if (otherNode.getDefinition().getType() != def.getType()) {
                            addUnique(varyings, var);
                            var.setShaderOutput(true);
                            for (VariableMapping variableMapping : otherNode.getInputMapping()) {
                                if (variableMapping.getLeftVariable().getName().equals(var.getName())) {
                                    variableMapping.getLeftVariable().setShaderOutput(true);
                                }
                            }
                        }
                        //and this other node is apparently used so we remove it from the unusedNodes list
                        unusedNodes.remove(otherNode.getName());
                    }
                }

            }
            List<VariableMapping> out = sn.getOutputMapping();
            if (out != null && !out.isEmpty()) {
                for (VariableMapping map : out) {
                    checkDefineFromCondition(map.getCondition(), matDef, technique);
                    ShaderNodeVariable var = map.getLeftVariable();
                    if (var.getNameSpace().equals("Global")) {
                        computeGlobals(technique, fragmentGlobals, def, var);
                    }
                }
                //shader has an output it's used in the shader code.
                unusedNodes.remove(sn.getName());
            } else {
                //some nodes has no output by design ans their def specifies so.
                if (sn.getDefinition().isNoOutput()) {
                    unusedNodes.remove(sn.getName());
                }
            }
        }
    }

    private static void checkDefineFromCondition(String condition, MaterialDef matDef, TechniqueDef techniqueDef) {
        if (condition == null) {
            return;
        }
        List<String> defines = parser.extractDefines(condition);
        for (String define : defines) {
            MatParam param = findMatParam(define, matDef);
            if (param != null) {
                addDefine(techniqueDef, param.getName(), param.getVarType());
            }
        }
    }

    public static void checkMultiplicity(TechniqueDef technique, MaterialDef matDef, VariableMapping map, ShaderNodeVariable var) {
        if (map.getLeftVariable().getMultiplicity() != null) {
            MatParam param = findMatParam(map.getRightVariable().getName(), matDef);
            if (!param.getVarType().name().endsWith("Array")) {
                throw new IllegalArgumentException(param.getName() + " is not of Array type");
            }
            String multiplicity = map.getLeftVariable().getMultiplicity();
            try {
                Integer.parseInt(multiplicity);
            } catch (NumberFormatException nfe) {
                //multiplicity is not an int attempting to find for a material parameter.
                MatParam mp = findMatParam(multiplicity, matDef);
                if (mp != null) {
                    //It's tied to a material param, let's create a define and use this as the multiplicity
                    addDefine(technique, multiplicity, VarType.Int);
                    multiplicity = multiplicity.toUpperCase();
                    map.getLeftVariable().setMultiplicity(multiplicity);
                    //only declare the variable if the define is defined.
                    map.getLeftVariable().setCondition(mergeConditions(map.getLeftVariable().getCondition(), "defined(" + multiplicity + ")", "||"));
                } else {
                    throw new IllegalArgumentException("Wrong multiplicity for variable" + map.getLeftVariable().getName() + ". " + multiplicity + " should be an int or a declared material parameter.");
                }
            }
            //the right variable must have the same multiplicity and the same condition.
            var.setMultiplicity(multiplicity);
            var.setCondition(map.getLeftVariable().getCondition());
        }
    }

    /**
     * merges 2 condition with the given operator
     *
     * @param condition1 the first condition
     * @param condition2 the second condition
     * @param operator   the operator ("&&" or "||&)
     * @return the merged condition
     */
    public static String mergeConditions(String condition1, String condition2, String operator) {
        if (condition1 != null) {
            if (condition1.equals(condition2)) {
                return condition1;
            }
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

    public static void addDefine(TechniqueDef techniqueDef, String paramName, VarType paramType) {
        if (techniqueDef.getShaderParamDefine(paramName) == null) {
            techniqueDef.addShaderParamDefine(paramName, paramType, paramName.toUpperCase());
        }
    }


    public static MatParam findMatParam(String varName, MaterialDef matDef) {
        for (MatParam matParam : matDef.getMaterialParams()) {
            if (varName.toLowerCase().equals(matParam.getName().toLowerCase())) {
                return matParam;
            }
        }
        return null;
    }

    private static void addUnique(List<ShaderNodeVariable> variables, ShaderNodeVariable var) {
        for (ShaderNodeVariable variable : variables) {
            if (var.equals(variable)) {
                return;
            }
        }
        variables.add(var);
    }

    /**
     * Retrieve a shader node by name
     *
     * @param technique
     * @param name
     * @return
     */
    private static ShaderNode findShaderNodeByName(TechniqueDef technique, String name) {
        for (ShaderNode shaderNode : technique.getShaderNodes()) {
            if (shaderNode.getName().equals(name)) {
                return shaderNode;
            }
        }
        return null;
    }

    /**
     * Some parameters may not be used, or not used as an input, but as a flag to command a define.
     * We didn't get them when looking into shader nodes mappings so let's do that now.
     *
     * @param uniforms
     */
    public static void getAllUniforms(TechniqueDef technique, MaterialDef matDef, List<ShaderNodeVariable> uniforms) {
        uniforms.clear();
        uniforms.addAll(technique.getShaderGenerationInfo().getFragmentUniforms());
        uniforms.addAll(technique.getShaderGenerationInfo().getVertexUniforms());

        for (UniformBinding worldParam : technique.getWorldBindings()) {
            ShaderNodeVariable var = new ShaderNodeVariable(worldParam.getGlslType(), "WorldParam", worldParam.name());
            if (!contains(uniforms, var)) {
                uniforms.add(var);
            }
        }

        for (MatParam matParam : matDef.getMaterialParams()) {
            ShaderNodeVariable var = new ShaderNodeVariable(matParam.getVarType().getGlslType(), "MatParam", matParam.getName());
            if (!contains(uniforms, var)) {
                uniforms.add(var);
            }
        }
    }

    private static void computeGlobals(TechniqueDef technique, List<ShaderNodeVariable> fragmentGlobals, ShaderNodeDefinition def, ShaderNodeVariable var) {
        var.setShaderOutput(true);
        if (def.getType() == Shader.ShaderType.Vertex) {
            if (technique.getShaderGenerationInfo().getVertexGlobal() == null) {
                technique.getShaderGenerationInfo().setVertexGlobal(var);
            }
        } else {
            if (!contains(fragmentGlobals, var)) {
                fragmentGlobals.add(var);
            }
        }
    }

    /**
     * returns true if a ShaderNode variable is already contained in a list of variables.
     * TODO This could be handled with a Collection.contains, if ShaderNodeVariable had a proper equals and hashcode
     *
     * @param vars
     * @param var
     * @return
     */
    public static boolean contains(List<ShaderNodeVariable> vars, ShaderNodeVariable var) {
        for (ShaderNodeVariable shaderNodeVariable : vars) {
            if (shaderNodeVariable.getName().equals(var.getName()) && shaderNodeVariable.getNameSpace().equals(var.getNameSpace())) {
                return true;
            }
        }
        return false;
    }
}
