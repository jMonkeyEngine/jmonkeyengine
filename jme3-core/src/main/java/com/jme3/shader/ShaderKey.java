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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Set;

public class ShaderKey extends AssetKey<Shader> {

    protected EnumMap<Shader.ShaderType,String> shaderLanguage;
    protected EnumMap<Shader.ShaderType,String> shaderName;
    protected DefineList defines;
    protected int cachedHashedCode = 0;
    protected boolean usesShaderNodes = false;

    public ShaderKey(){
        shaderLanguage=new EnumMap<Shader.ShaderType, String>(Shader.ShaderType.class);
        shaderName=new EnumMap<Shader.ShaderType, String>(Shader.ShaderType.class);
    }

    public ShaderKey(DefineList defines, EnumMap<Shader.ShaderType,String> shaderLanguage,EnumMap<Shader.ShaderType,String> shaderName){
        super("");
        this.name = reducePath(getShaderName(Shader.ShaderType.Vertex));
        this.shaderLanguage=new EnumMap<Shader.ShaderType, String>(Shader.ShaderType.class);
        this.shaderName=new EnumMap<Shader.ShaderType, String>(Shader.ShaderType.class);
        this.defines = defines;
        for (Shader.ShaderType shaderType : shaderName.keySet()) {
            this.shaderName.put(shaderType,shaderName.get(shaderType));
            this.shaderLanguage.put(shaderType,shaderLanguage.get(shaderType));
        }
    }
    
    @Override
    public ShaderKey clone() {
        ShaderKey clone = (ShaderKey) super.clone();
        clone.cachedHashedCode = 0;
        clone.defines = defines.clone();
        return clone;
    }
    
    @Override
    public String toString(){
        //todo:
        return "V="+name+";";
    }
    
    private final String getShaderName(Shader.ShaderType type) {
        if (shaderName == null) {
            return "";
        }
        String shName = shaderName.get(type);
        return shName != null ? shName : "";
    }

    //todo: make equals and hashCode work
    @Override
    public boolean equals(Object obj) {
        final ShaderKey other = (ShaderKey) obj;
        if (name.equals(other.name) && getShaderName(Shader.ShaderType.Fragment).equals(other.getShaderName(Shader.ShaderType.Fragment))){
            if (defines != null && other.defines != null) {
                return defines.equals(other.defines);
            } else if (defines != null || other.defines != null) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (cachedHashedCode == 0) {
            int hash = 7;
            hash = 41 * hash + name.hashCode();
            hash = 41 * hash + getShaderName(Shader.ShaderType.Fragment).hashCode();
            hash = getShaderName(Shader.ShaderType.Geometry) == null ? hash : 41 * hash + getShaderName(Shader.ShaderType.Geometry).hashCode();
            hash = getShaderName(Shader.ShaderType.TessellationControl) == null ? hash : 41 * hash + getShaderName(Shader.ShaderType.TessellationControl).hashCode();
            hash = getShaderName(Shader.ShaderType.TessellationEvaluation) == null ? hash : 41 * hash + getShaderName(Shader.ShaderType.TessellationEvaluation).hashCode();
            hash = 41 * hash + (defines != null ? defines.hashCode() : 0);
            cachedHashedCode = hash;
        }
        return cachedHashedCode;
    }

    public DefineList getDefines() {
        return defines;
    }

    public String getVertName(){
        return getShaderName(Shader.ShaderType.Vertex);
    }

    public String getFragName() {
        return getShaderName(Shader.ShaderType.Fragment);
    }

    /**
     * @deprecated Use {@link #getVertexShaderLanguage() } instead.
     */
    @Deprecated
    public String getLanguage() {
        return shaderLanguage.get(Shader.ShaderType.Vertex);
    }
    
    public String getVertexShaderLanguage() { 
        return shaderLanguage.get(Shader.ShaderType.Vertex);
    }
    
    public String getFragmentShaderLanguage() {
        return shaderLanguage.get(Shader.ShaderType.Vertex);
    }

    public boolean isUsesShaderNodes() {
        return usesShaderNodes;
    }

    public void setUsesShaderNodes(boolean usesShaderNodes) {
        this.usesShaderNodes = usesShaderNodes;
    }

    public Set<Shader.ShaderType> getUsedShaderPrograms(){
        return shaderName.keySet();
    }

    public String getShaderProgramLanguage(Shader.ShaderType shaderType){
        return shaderLanguage.get(shaderType);
    }

    public String getShaderProgramName(Shader.ShaderType shaderType){
        return getShaderName(shaderType);
    }

    @Override
    public void write(JmeExporter ex) throws IOException{
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(shaderName.get(Shader.ShaderType.Fragment), "fragment_name", null);
        oc.write(shaderName.get(Shader.ShaderType.Geometry), "geometry_name", null);
        oc.write(shaderName.get(Shader.ShaderType.TessellationControl), "tessControl_name", null);
        oc.write(shaderName.get(Shader.ShaderType.TessellationEvaluation), "tessEval_name", null);
        oc.write(shaderLanguage.get(Shader.ShaderType.Vertex), "language", null);
        oc.write(shaderLanguage.get(Shader.ShaderType.Fragment), "frag_language", null);
        oc.write(shaderLanguage.get(Shader.ShaderType.Geometry), "geom_language", null);
        oc.write(shaderLanguage.get(Shader.ShaderType.TessellationControl), "tsctrl_language", null);
        oc.write(shaderLanguage.get(Shader.ShaderType.TessellationEvaluation), "tseval_language", null);

    }

    @Override
    public void read(JmeImporter im) throws IOException{
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        shaderName.put(Shader.ShaderType.Vertex,name);
        shaderName.put(Shader.ShaderType.Fragment,ic.readString("fragment_name", null));
        shaderName.put(Shader.ShaderType.Geometry,ic.readString("geometry_name", null));
        shaderName.put(Shader.ShaderType.TessellationControl,ic.readString("tessControl_name", null));
        shaderName.put(Shader.ShaderType.TessellationEvaluation,ic.readString("tessEval_name", null));
        shaderLanguage.put(Shader.ShaderType.Vertex,ic.readString("language", null));
        shaderLanguage.put(Shader.ShaderType.Fragment,ic.readString("frag_language", null));
        shaderLanguage.put(Shader.ShaderType.Geometry,ic.readString("geom_language", null));
        shaderLanguage.put(Shader.ShaderType.TessellationControl,ic.readString("tsctrl_language", null));
        shaderLanguage.put(Shader.ShaderType.TessellationEvaluation,ic.readString("tseval_language", null));
    }

}
