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
import com.jme3.shader.Shader.ShaderType;

/**
 * This shader Generator can generate Vertex and Fragment shaders from
 * ShaderNodes for GLSL 1.5
 *
 * @author Nehon
 */
public class Glsl150ShaderGenerator extends Glsl100ShaderGenerator {

    /**
     * Creates a Glsl150ShaderGenerator
     *
     * @param assetManager the assetmanager
     */
    public Glsl150ShaderGenerator(AssetManager assetManager) {
        super(assetManager);
    }

    @Override
    protected String getLanguageAndVersion(ShaderType type) {
        return "GLSL150";
    }

    /**
     * {@inheritDoc} in glsl 1.5 attributes are prefixed with the "in" keyword
     * and not the "attribute" keyword
     */
    @Override
    protected void declareAttribute(StringBuilder source, ShaderNodeVariable var) {
        declareVariable(source, var, false, "in");
    }

    /**
     * {@inheritDoc} in glsl 1.5 varying are prefixed with the "in" or "out"
     * keyword and not the "varying" keyword.
     *
     * "in" is used for Fragment shader (maybe Geometry shader later) "out" is
     * used for Vertex shader (maybe Geometry shader later)
     */
    @Override
    protected void declareVarying(StringBuilder source, ShaderNodeVariable var, boolean input) {
        declareVariable(source, var, true, input ? "in" : "out");
    }

    /**
     * {@inheritDoc}
     *
     * Fragment shader outputs are declared before the "void main(){" with the
     * "out" keyword.
     *
     * after the "void main(){", the vertex output are declared and initialized
     * and the fragment outputs are declared
     */
    @Override
    protected void generateStartOfMainSection(StringBuilder source, ShaderGenerationInfo info, Shader.ShaderType type) {
        source.append("\n");

        if (type == Shader.ShaderType.Fragment) {
            for (ShaderNodeVariable global : info.getFragmentGlobals()) {
                declareVariable(source, global, null, true, "out");
            }
        }
        source.append("\n");

        appendIndent(source);
        source.append("void main(){\n");
        indent();

        if (type == Shader.ShaderType.Vertex) {
            declareGlobalPosition(info, source);
        } else if (type == Shader.ShaderType.Fragment) {
            for (ShaderNodeVariable global : info.getFragmentGlobals()) {
                initVariable(source, global, "vec4(1.0)");
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * only vertex shader output are mapped here, since fragment shader outputs
     * must have been mapped in the main section.
     */
    @Override
    protected void generateEndOfMainSection(StringBuilder source, ShaderGenerationInfo info, Shader.ShaderType type) {
        if (type == Shader.ShaderType.Vertex) {
            appendOutput(source, "gl_Position", info.getVertexGlobal());
        }
        unIndent();
        appendIndent(source);
        source.append("}\n");
    }

    /**
     * Append a variable initialization to the code
     *
     * @param source the StringBuilder to use
     * @param var the variable to initialize
     * @param initValue the init value to assign to the variable
     */
    protected void initVariable(StringBuilder source, ShaderNodeVariable var, String initValue) {
        appendIndent(source);
        source.append(var.getNameSpace());
        source.append("_");
        source.append(var.getName());
        source.append(" = ");
        source.append(initValue);
        source.append(";\n");
    }
}
