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

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.material.ShaderGenerationInfo;
import com.jme3.material.Technique;
import com.jme3.material.TechniqueDef;
import com.jme3.shader.Shader.ShaderType;
import java.util.List;

/**
 * This class is the base for a shader generator using the ShaderNodes system,
 * it contains basis mechnaism of generation, but no actual generation code.
 * This class is abstract, any Shader generator must extend it.
 *
 * @author Nehon
 */
public abstract class ShaderGenerator {

    //the asset manager
    protected AssetManager assetManager;
    //indentation value for generation
    protected int indent;
    //the technique to use for the shader generation
    protected Technique technique = null;    

    /**
     * Build a shaderGenerator
     *
     * @param assetManager
     */
    protected ShaderGenerator(AssetManager assetManager) {
        this.assetManager = assetManager;        
    }
    
    public void initialize(Technique technique){
        this.technique = technique;
    }
    
    /**
     * Generate vertex and fragment shaders for the given technique
     *
     * @param technique the technique to use to generate the shaders
     * @return a Shader program
     */
    public Shader generateShader() {
        if(technique == null){
            throw new UnsupportedOperationException("The shaderGenerator was not properly initialized, call initialize(Technique) before any generation");
        }

        DefineList defines = technique.getAllDefines();
        TechniqueDef def = technique.getDef();
        ShaderGenerationInfo info = def.getShaderGenerationInfo();

        String vertexSource = buildShader(def.getShaderNodes(), info, ShaderType.Vertex);
        String fragmentSource = buildShader(def.getShaderNodes(), info, ShaderType.Fragment);

        Shader shader = new Shader();
        shader.initialize();
        shader.addSource(Shader.ShaderType.Vertex, technique.getDef().getName() + ".vert", vertexSource, defines.getCompiled(), getLanguageAndVersion(ShaderType.Vertex));
        shader.addSource(Shader.ShaderType.Fragment, technique.getDef().getName() + ".frag", fragmentSource, defines.getCompiled(), getLanguageAndVersion(ShaderType.Fragment));
        
        technique = null;
        return shader;
    }

    /**
     * This method is responsible for the shader generation.
     *
     * @param shaderNodes the list of shader nodes
     * @param info the ShaderGenerationInfo filled during the Technique loading
     * @param type the type of shader to generate
     * @return the code of the generated vertex shader
     */
    protected String buildShader(List<ShaderNode> shaderNodes, ShaderGenerationInfo info, ShaderType type) {
        indent = 0;

        StringBuilder sourceDeclaration = new StringBuilder();
        StringBuilder source = new StringBuilder();

        generateUniforms(sourceDeclaration, info, type);

        if (type == ShaderType.Vertex) {
            generateAttributes(sourceDeclaration, info);
        }
        generateVaryings(sourceDeclaration, info, type);

        generateStartOfMainSection(source, info, type);

        generateDeclarationAndMainBody(shaderNodes, sourceDeclaration, source, info, type);

        generateEndOfMainSection(source, info, type);

        sourceDeclaration.append(source);

        return sourceDeclaration.toString();
    }

    /**
     * iterates through shader nodes to load them and generate the shader
     * declaration part and main body extracted from the shader nodes, for the
     * given shader type
     *
     * @param shaderNodes the list of shader nodes
     * @param sourceDeclaration the declaration part StringBuilder of the shader
     * to generate
     * @param source the main part StringBuilder of the shader to generate
     * @param info the ShaderGenerationInfo
     * @param type the Shader type
     */
    protected void generateDeclarationAndMainBody(List<ShaderNode> shaderNodes, StringBuilder sourceDeclaration, StringBuilder source, ShaderGenerationInfo info, Shader.ShaderType type) {
        for (ShaderNode shaderNode : shaderNodes) {
            if (info.getUnusedNodes().contains(shaderNode.getName())) {
                continue;
            }
            if (shaderNode.getDefinition().getType() == type) {
                int index = findShaderIndexFromVersion(shaderNode, type);
                String loadedSource = (String) assetManager.loadAsset(new AssetKey(shaderNode.getDefinition().getShadersPath().get(index)));
                appendNodeDeclarationAndMain(loadedSource, sourceDeclaration, source, shaderNode, info);
            }
        }
    }

    /**
     * Appends declaration and main part of a node to the shader declaration and
     * main part. the loadedSource is split by "void main(){" to split
     * declaration from main part of the node source code.The trailing "}" is
     * removed from the main part. Each part is then respectively passed to
     * generateDeclarativeSection and generateNodeMainSection.
     *
     * @see ShaderGenerator#generateDeclarativeSection
     * @see ShaderGenerator#generateNodeMainSection
     *
     * @param loadedSource the actual source code loaded for this node.
     * @param sourceDeclaration the Shader declaration part string builder.
     * @param source the Shader main part StringBuilder.
     * @param shaderNode the shader node.
     * @param info the ShaderGenerationInfo.
     */
    protected void appendNodeDeclarationAndMain(String loadedSource, StringBuilder sourceDeclaration, StringBuilder source, ShaderNode shaderNode, ShaderGenerationInfo info) {
        if (loadedSource.length() > 1) {
            loadedSource = loadedSource.substring(0, loadedSource.lastIndexOf("}"));
            String[] sourceParts = loadedSource.split("void main\\(\\)\\{");
            generateDeclarativeSection(sourceDeclaration, shaderNode, sourceParts[0], info);
            generateNodeMainSection(source, shaderNode, sourceParts[1], info);
        } else {
            //if source is empty, we still call generateNodeMainSection so that mappings can be done.
            generateNodeMainSection(source, shaderNode, loadedSource, info);
        }

    }

    /**
     * returns the laguage + version of the shader should be somthing like
     * "GLSL100" for glsl 1.0 "GLSL150" for glsl 1.5.
     *
     * @param type the shader type for wich the version should be returned.
     *
     * @return the shaderLanguage and version.
     */
    protected abstract String getLanguageAndVersion(Shader.ShaderType type);

    /**
     * generates the uniforms declaration for a shader of the given type.
     *
     * @param source the source StringBuilder to append generated code.
     * @param info the ShaderGenerationInfo.
     * @param type the shader type the uniforms have to be generated for.
     */
    protected abstract void generateUniforms(StringBuilder source, ShaderGenerationInfo info, ShaderType type);

    /**
     * generates the attributes declaration for the vertex shader. There is no
     * Shader type passed here as attributes are only used in vertex shaders
     *
     * @param source the source StringBuilder to append generated code.
     * @param info the ShaderGenerationInfo.
     */
    protected abstract void generateAttributes(StringBuilder source, ShaderGenerationInfo info);

    /**
     * generates the varyings for the given shader type shader. Note that
     * varyings are deprecated in glsl 1.3, but this method will still be called
     * to generate all non global inputs and output of the shaders.
     *
     * @param source the source StringBuilder to append generated code.
     * @param info the ShaderGenerationInfo.
     * @param type the shader type the varyings have to be generated for.
     */
    protected abstract void generateVaryings(StringBuilder source, ShaderGenerationInfo info, ShaderType type);

    /**
     * Appends the given shaderNode declarative part to the shader declarative
     * part. If needed the sahder type can be determined by fetching the
     * shaderNode's definition type.
     *
     * @see ShaderNode#getDefinition()
     * @see ShaderNodeDefinition#getType()
     *
     * @param source the StringBuilder to append generated code.
     * @param shaderNode the shaderNode.
     * @param nodeSource the declaration part of the loaded shaderNode source.
     * @param info the ShaderGenerationInfo.
     */
    protected abstract void generateDeclarativeSection(StringBuilder source, ShaderNode shaderNode, String nodeDecalarationSource, ShaderGenerationInfo info);

    /**
     * generates the start of the shader main section. this method is
     * responsible of appending the "void main(){" in the shader and declaring
     * all global outputs of the shader
     *
     * @param source the StringBuilder to append generated code.
     * @param info the ShaderGenerationInfo.
     * @param type the shader type the section has to be generated for.
     */
    protected abstract void generateStartOfMainSection(StringBuilder source, ShaderGenerationInfo info, ShaderType type);

    /**
     * generates the end of the shader main section. this method is responsible
     * of appending the last "}" in the shader and mapping all global outputs of
     * the shader
     *
     * @param source the StringBuilder to append generated code.
     * @param info the ShaderGenerationInfo.
     * @param type the shader type the section has to be generated for.
     */
    protected abstract void generateEndOfMainSection(StringBuilder source, ShaderGenerationInfo info, ShaderType type);

    /**
     * Appends the given shaderNode main part to the shader declarative part. If
     * needed the sahder type can be determined by fetching the shaderNode's
     * definition type.
     *
     * @see ShaderNode#getDefinition()
     * @see ShaderNodeDefinition#getType()
     *
     * @param source the StringBuilder to append generated code.
     * @param shaderNode the shaderNode.
     * @param nodeSource the declaration part of the loaded shaderNode source.
     * @param info the ShaderGenerationInfo.
     */
    protected abstract void generateNodeMainSection(StringBuilder source, ShaderNode shaderNode, String nodeSource, ShaderGenerationInfo info);

    /**
     * returns the shaderpath index according to the version of the generator.
     * This allow to select the higher version of the shader that the generator
     * can handle
     *
     * @param shaderNode the shaderNode being processed
     * @param type the shaderType
     * @return the index of the shader path in ShaderNodeDefinition shadersPath
     * list
     * @throws NumberFormatException
     */
    protected int findShaderIndexFromVersion(ShaderNode shaderNode, ShaderType type) throws NumberFormatException {
        int index = 0;
        List<String> lang = shaderNode.getDefinition().getShadersLanguage();
        int genVersion = Integer.parseInt(getLanguageAndVersion(type).substring(4));
        int curVersion = 0;
        for (int i = 0; i < lang.size(); i++) {
            int version = Integer.parseInt(lang.get(i).substring(4));
            if (version > curVersion && version <= genVersion) {
                curVersion = version;
                index = i;
            }
        }
        return index;
    }    
}
