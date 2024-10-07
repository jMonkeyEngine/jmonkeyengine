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
package com.jme3.material.plugin.export.materialdef;

import com.jme3.material.MatParam;
import com.jme3.material.RenderState;
import com.jme3.material.TechniqueDef;
import com.jme3.shader.*;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author nehon
 */
public class J3mdTechniqueDefWriter {

    public J3mdTechniqueDefWriter() {
    }

    public void write(TechniqueDef techniqueDef, Collection<MatParam> matParams, Writer out) throws IOException {
        out.write("    Technique");
        if(!techniqueDef.getName().equals("Default")) {
            out.write(" ");
            out.write(techniqueDef.getName());
        }
        out.write(" {\n");

        //Light mode
        if(techniqueDef.getLightMode() != TechniqueDef.LightMode.Disable ){
            out.write("        LightMode ");
            out.write(techniqueDef.getLightMode().name());
            out.write("\n\n");
        }

        //Shadow mode
        if(techniqueDef.getShadowMode() != TechniqueDef.ShadowMode.Disable ){
            out.write("        ShadowMode ");
            out.write(techniqueDef.getShadowMode().name());
            out.write("\n\n");
        }

        //Shaders
        if(!techniqueDef.isUsingShaderNodes()) {
            writeShaders(techniqueDef, out);
        }

        //World params
        if(!techniqueDef.getWorldBindings().isEmpty()){
            writeWorldParams(techniqueDef, out);
        }

        //ShaderNodes
        if(techniqueDef.isUsingShaderNodes()){
            writeShaderNodes(techniqueDef, matParams, out);

        } else {
            // When we have ShaderNodes, defines are handled differently, so we don't have to write them.
            //Defines
            if (techniqueDef.getDefineNames().length != 0) {
                writeDefines(techniqueDef, matParams, out);
            }
        }

        //render state
        RenderState rs = techniqueDef.getRenderState();
        if(rs != null){
            out.write("        RenderState {\n");
            writeRenderState(rs, out);
            out.write("        }\n\n");
        }

        //forced render state
        rs = techniqueDef.getForcedRenderState();
        if(rs != null){
            out.write("        ForcedRenderState {\n");
            writeRenderState(rs, out);
            out.write("        }\n\n");
        }

        //no render
        if(techniqueDef.isNoRender()){
            out.write("        NoRender\n\n");
        }

        out.write("    }\n");
    }

    private void writeDefines(TechniqueDef techniqueDef, Collection<MatParam> matParams, Writer out) throws IOException {
        out.write("        Defines {\n");

        for (int i = 0; i < techniqueDef.getDefineNames().length; i++) {
            String matParamName = getMatParamNameForDefineId(techniqueDef, matParams, i);
            if (matParamName != null) {
                String defineName = techniqueDef.getDefineNames()[i];
                out.write("            ");
                out.write(defineName);
                out.write(": ");
                out.write(matParamName);
                out.write("\n");
            }
        }
        out.write("        }\n\n");
    }

    private void writeShaderNodes(TechniqueDef techniqueDef, Collection<MatParam> matParams, Writer out) throws IOException {
        out.write("        VertexShaderNodes {\n");
        for (ShaderNode shaderNode : techniqueDef.getShaderNodes()) {
            if(shaderNode.getDefinition().getType() == Shader.ShaderType.Vertex){
                writeShaderNode(out, shaderNode, matParams);
            }
        }
        out.write("        }\n\n");

        out.write("        FragmentShaderNodes {\n");
        for (ShaderNode shaderNode : techniqueDef.getShaderNodes()) {
            if(shaderNode.getDefinition().getType() == Shader.ShaderType.Fragment){
                writeShaderNode(out, shaderNode, matParams);
            }
        }
        out.write("        }\n\n");
    }

    private void writeWorldParams(TechniqueDef techniqueDef, Writer out) throws IOException {
        out.write("        WorldParameters {\n");
        for (UniformBinding uniformBinding : techniqueDef.getWorldBindings()) {
            out.write("            ");
            out.write(uniformBinding.toString());
            out.write("\n");
        }
        out.write("        }\n\n");
    }

    private void writeShaders(TechniqueDef techniqueDef, Writer out) throws IOException {
        if (techniqueDef.getShaderProgramNames().size() > 0) {
            for (Shader.ShaderType shaderType : techniqueDef.getShaderProgramNames().keySet()) {
                //   System.err.println(shaderType + " " +techniqueDef.getShaderProgramNames().get(shaderType) + " " +techniqueDef.getShaderProgramLanguage(shaderType))
                out.write("        ");
                out.write(shaderType.name());
                out.write("Shader ");
                out.write(techniqueDef.getShaderProgramLanguage(shaderType));
                out.write(": ");
                out.write(techniqueDef.getShaderProgramNames().get(shaderType));
                out.write("\n");
            }
            out.write("\n");
        }
    }

    private void writeShaderNode(Writer out, ShaderNode shaderNode, Collection<MatParam> matParams) throws IOException {
        out.write("            ShaderNode ");
        out.write(shaderNode.getName());
        out.write(" {\n");

        if (shaderNode.getCondition() != null){
            out.write("                Condition: ");
            out.write(formatCondition(shaderNode.getCondition(), matParams));
            out.write("\n");
        }

        out.write("                Definition: ");
        out.write(shaderNode.getDefinition().getName());
        out.write(": ");
        out.write(shaderNode.getDefinition().getPath());
        out.write("\n");

        final List<VariableMapping> inputMapping = shaderNode.getInputMapping();
        final List<VariableMapping> outputMapping = shaderNode.getOutputMapping();

        if (!inputMapping.isEmpty()) {
            out.write("                InputMappings {\n");
            for (VariableMapping mapping : inputMapping) {
                writeVariableMapping(out, shaderNode, mapping, matParams);
            }
            out.write("                }\n");
        }

        if (!outputMapping.isEmpty()) {
            out.write("                OutputMappings {\n");
            for (VariableMapping mapping : outputMapping) {
                writeVariableMapping(out, shaderNode, mapping, matParams);
            }
            out.write("                }\n");
        }

        out.write("            }\n");
    }

    private void writeVariableMapping(final Writer out, final ShaderNode shaderNode,                                      final VariableMapping mapping, final Collection<MatParam> matParams)
            throws IOException {

        final ShaderNodeVariable leftVar = mapping.getLeftVariable();
        final ShaderNodeVariable rightVar = mapping.getRightVariable();
        final String rightExpression = mapping.getRightExpression();

        out.write("                    ");

        if (!leftVar.getNameSpace().equals(shaderNode.getName())) {
            out.write(leftVar.getNameSpace());
            out.write(".");
        }

        out.write(leftVar.getName());

        if (!mapping.getLeftSwizzling().equals("")) {
            out.write(".");
            out.write(mapping.getLeftSwizzling());
        }

        out.write(" = ");

        if (rightVar != null) {

            if (!rightVar.getNameSpace().equals(shaderNode.getName())) {
                out.write(rightVar.getNameSpace());
                out.write(".");
            }

            String rightVarName = rightVar.getName();
            if (rightVarName.startsWith("g_") || rightVarName.startsWith("m_")) {
                rightVarName = rightVarName.substring(2, rightVarName.length());
            }

            out.write(rightVarName);

            if (!mapping.getRightSwizzling().equals("")) {
                out.write(".");
                out.write(mapping.getRightSwizzling());
            }
        } else {
            out.write("%%");
            out.write(rightExpression);
            out.write("%%");
        }

        if (mapping.getCondition() != null) {
            out.write(" : ");
            out.write(formatCondition(mapping.getCondition(), matParams));
        }

        out.write("\n");
    }

    private String formatCondition(String condition, Collection<MatParam> matParams){
        //condition = condition.replaceAll("defined\\(","");

        String res = condition;
        Pattern pattern = Pattern.compile("defined\\(([A-Z0-9]*)\\)");
        Matcher m = pattern.matcher(condition);

        while(m.find()){
            String match = m.group(0);
            String defineName = m.group(1).toLowerCase();
            for (MatParam matParam : matParams) {
                if(matParam.getName().toLowerCase().equals(defineName)){
                    res = res.replace(match, matParam.getName());
                }
            }
        }

        return res;
    }

    private void writeRenderStateAttribute(Writer out, String name, String value) throws IOException {
        out.write("            ");
        out.write(name);
        out.write(" ");
        out.write(value);
        out.write("\n");
    }

    private void writeRenderState(RenderState rs, Writer out) throws IOException {
        RenderState defRs = RenderState.DEFAULT;
        if(rs.getBlendMode() != defRs.getBlendMode()) {
            writeRenderStateAttribute(out, "Blend", rs.getBlendMode().name());
        }
        if(rs.isWireframe() != defRs.isWireframe()) {
            writeRenderStateAttribute(out, "Wireframe", rs.isWireframe()?"On":"Off");
        }
        if(rs.getFaceCullMode() != defRs.getFaceCullMode()) {
            writeRenderStateAttribute(out, "FaceCull", rs.getFaceCullMode().name());
        }
        if(rs.isDepthWrite() != defRs.isDepthWrite()) {
            writeRenderStateAttribute(out, "DepthWrite", rs.isDepthWrite()?"On":"Off");
        }
        if(rs.isDepthTest() != defRs.isDepthTest()) {
            writeRenderStateAttribute(out, "DepthTest",  rs.isDepthTest()?"On":"Off");
        }
        if(rs.getBlendEquation() != defRs.getBlendEquation()) {
            writeRenderStateAttribute(out, "BlendEquation", rs.getBlendEquation().name());
        }
        if(rs.getBlendEquationAlpha() != defRs.getBlendEquationAlpha()) {
            writeRenderStateAttribute(out, "BlendEquationAlpha", rs.getBlendEquationAlpha().name());
        }
        if(rs.getPolyOffsetFactor() != defRs.getPolyOffsetFactor() || rs.getPolyOffsetUnits() != defRs.getPolyOffsetUnits()) {
            writeRenderStateAttribute(out, "PolyOffset", rs.getPolyOffsetFactor() + " " + rs.getPolyOffsetUnits());
        }
        if(rs.isColorWrite() != defRs.isColorWrite()) {
            writeRenderStateAttribute(out, "ColorWrite",  rs.isColorWrite()?"On":"Off");
        }
        if(rs.getDepthFunc() != defRs.getDepthFunc()) {
            writeRenderStateAttribute(out, "DepthFunc", rs.getDepthFunc().name());
        }
        if(rs.getLineWidth() != defRs.getLineWidth()) {
            writeRenderStateAttribute(out, "LineWidth", Float.toString(rs.getLineWidth()));
        }
    }

    private String getMatParamNameForDefineId(TechniqueDef techniqueDef, Collection<MatParam> matParams, int defineId) {
        for (MatParam matParam : matParams) {
            Integer id = techniqueDef.getShaderParamDefineId(matParam.getName());
            if(id !=null && id == defineId){
                return matParam.getName();
            }
        }
        return null;
    }
}