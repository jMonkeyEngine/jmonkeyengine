/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import com.jme3.export.*;
import com.jme3.renderer.Renderer;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import com.jme3.util.ListMap;
import com.jme3.util.NativeObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public final class Shader extends NativeObject implements Savable {

    private String language;

    /**
     * True if the shader is fully compiled & linked.
     * (e.g no GL error will be invoked if used).
     */
    private boolean usable = false;

    /**
     * A list of all shaders currently attached.
     */
    private ArrayList<ShaderSource> shaderList;

    /**
     * Maps uniform name to the uniform variable.
     */
//    private HashMap<String, Uniform> uniforms;
    private ListMap<String, Uniform> uniforms;

    /**
     * Maps attribute name to the location of the attribute in the shader.
     */
    private IntMap<Attribute> attribs;

    /**
     * Type of shader. The shader will control the pipeline of it's type.
     */
    public static enum ShaderType {
        /**
         * Control fragment rasterization. (e.g color of pixel).
         */
        Fragment,

        /**
         * Control vertex processing. (e.g transform of model to clip space)
         */
        Vertex,

        /**
         * Control geometry assembly. (e.g compile a triangle list from input data)
         */
        Geometry;
    }

    /**
     * Shader source describes a shader object in OpenGL. Each shader source
     * is assigned a certain pipeline which it controls (described by it's type).
     */
    public static class ShaderSource extends NativeObject implements Savable {

        ShaderType shaderType;

        boolean usable = false;
        String name = null;
        String source = null;
        String defines = null;

        public ShaderSource(ShaderType type){
            super(ShaderSource.class);
            this.shaderType = type;
            if (type == null)
                throw new NullPointerException("The shader type must be specified");
        }
        
        protected ShaderSource(ShaderSource ss){
            super(ShaderSource.class, ss.id);
            this.shaderType = ss.shaderType;
            usable = false;
            name = ss.name;
            // forget source & defines
        }

        public ShaderSource(){
            super(ShaderSource.class);
        }

        public void write(JmeExporter ex) throws IOException{
            OutputCapsule oc = ex.getCapsule(this);
            oc.write(shaderType, "shaderType", null);
            oc.write(name, "name", null);
            oc.write(source, "source", null);
            oc.write(defines, "defines", null);
        }

        public void read(JmeImporter im) throws IOException{
            InputCapsule ic = im.getCapsule(this);
            shaderType = ic.readEnum("shaderType", ShaderType.class, null);
            name = ic.readString("name", null);
            source = ic.readString("source", null);
            defines = ic.readString("defines", null);
        }

        public void setName(String name){
            this.name = name;
        }

        public String getName(){
            return name;
        }

        public ShaderType getType() {
            return shaderType;
        }

        public void setSource(String source){
            if (source == null)
                throw new NullPointerException("Shader source cannot be null");

            this.source = source;
            setUpdateNeeded();
        }

        public void setDefines(String defines){
            if (defines == null)
                throw new NullPointerException("Shader defines cannot be null");

            this.defines = defines;
            setUpdateNeeded();
        }

        public String getSource(){
            return source;
        }

        public String getDefines(){
            return defines;
        }
        
        public boolean isUsable(){
            return usable;
        }

        public void setUsable(boolean usable){
            this.usable = usable;
        }

        @Override
        public String toString(){
            String nameTxt = "";
            if (name != null)
                nameTxt = "name="+name+", ";
            if (defines != null)
                nameTxt += "defines, ";
            

            return getClass().getSimpleName() + "["+nameTxt+"type="
                                              + shaderType.name()+"]";
        }

        public void resetObject(){
            id = -1;
            usable = false;
            setUpdateNeeded();
        }

        public void deleteObject(Object rendererObject){
            ((Renderer)rendererObject).deleteShaderSource(ShaderSource.this);
        }

        public NativeObject createDestructableClone(){
            return new ShaderSource(ShaderSource.this);
        }
    }

    /**
     * Create an empty shader.
     */
    public Shader(String language){
        super(Shader.class);
        this.language = language;
        shaderList = new ArrayList<ShaderSource>();
//        uniforms = new HashMap<String, Uniform>();
        uniforms = new ListMap<String, Uniform>();
        attribs = new IntMap<Attribute>();
    }

    /**
     * Do not use this constructor. Serialization purposes only.
     */
    public Shader(){
        super(Shader.class);
    }

    protected Shader(Shader s){
        super(Shader.class, s.id);
        shaderList = new ArrayList<ShaderSource>();
        //uniforms = new ListMap<String, Uniform>();
        //attribs = new IntMap<Attribute>();
        
        // NOTE: Because ShaderSources are registered separately with
        // the GLObjectManager
        for (ShaderSource source : s.shaderList){
            shaderList.add( (ShaderSource)source.createDestructableClone() );
        }
    }

    public void write(JmeExporter ex) throws IOException{
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(language, "language", null);
        oc.writeSavableArrayList(shaderList, "shaderList", null);
        oc.writeIntSavableMap(attribs, "attribs", null);
        oc.writeStringSavableMap(uniforms, "uniforms", null);
    }

    public void read(JmeImporter im) throws IOException{
        InputCapsule ic = im.getCapsule(this);
        language = ic.readString("language", null);
        shaderList = ic.readSavableArrayList("shaderList", null);
        attribs = (IntMap<Attribute>) ic.readIntSavableMap("attribs", null);

        HashMap<String, Uniform> uniMap = (HashMap<String, Uniform>) ic.readStringSavableMap("uniforms", null);
        uniforms = new ListMap<String, Uniform>(uniMap);
    }

    /**
     * Creates a deep clone of the shader, where the sources are available
     * but have not been compiled yet. Does not copy the uniforms or attribs.
     * @return
     */
//    public Shader createDeepClone(String defines){
//        Shader newShader = new Shader(language);
//        for (ShaderSource source : shaderList){
//            if (!source.getDefines().equals(defines)){
//                // need to clone the shadersource so
//                // the correct defines can be placed
//                ShaderSource newSource = new ShaderSource(source.getType());
//                newSource.setSource(source.getSource());
//                newSource.setDefines(defines);
//                newShader.addSource(newSource);
//            }else{
//                // no need to clone source, also saves
//                // having to compile the shadersource
//                newShader.addSource(source);
//            }
//        }
//        return newShader;
//    }

    /**
     * Adds source code to a certain pipeline.
     *
     * @param type The pipeline to control
     * @param source The shader source code (in GLSL).
     */
    public void addSource(ShaderType type, String name, String source, String defines){
        ShaderSource shader = new ShaderSource(type);
        shader.setSource(source);
        shader.setName(name);
        if (defines != null)
            shader.setDefines(defines);
        
        shaderList.add(shader);
        setUpdateNeeded();
    }

    public void addSource(ShaderType type, String source, String defines){
        addSource(type, null, source, defines);
    }

    public void addSource(ShaderType type, String source){
        addSource(type, source, null);
    }

    /**
     * Adds an existing shader source to this shader.
     * @param source
     */
    private void addSource(ShaderSource source){
        shaderList.add(source);
        setUpdateNeeded();
    }

    public Uniform getUniform(String name){
        Uniform uniform = uniforms.get(name);
        if (uniform == null){
            uniform = new Uniform();
            uniform.name = name;
            uniforms.put(name, uniform);
        }
        return uniform;
    }

    public void removeUniform(String name){
        uniforms.remove(name);
    }

    public Attribute getAttribute(VertexBuffer.Type attribType){
        int ordinal = attribType.ordinal();
        Attribute attrib = attribs.get(ordinal);
        if (attrib == null){
            attrib = new Attribute();
            attrib.name = attribType.name();
            attribs.put(ordinal, attrib);
        }
        return attrib;
    }

//    public Collection<Uniform> getUniforms(){
//        return uniforms.values();
//    }

    public ListMap<String, Uniform> getUniformMap(){
        return uniforms;
    }

//    public Collection<Attribute> getAttributes() {
//        return attribs.
//    }

    public Collection<ShaderSource> getSources(){
        return shaderList;
    }

    public String getLanguage(){
        return language;
    }

    @Override
    public String toString(){
        return getClass().getSimpleName() + "[language="+language
                                           + ", numSources="+shaderList.size()
                                           + ", numUniforms="+uniforms.size()
                                           + ", shaderSources="+getSources()+"]";
    }

    /**
     * Clears all sources. Assuming that they have already been detached and
     * removed on the GL side.
     */
    public void resetSources(){
        shaderList.clear();
    }

    /**
     * Returns true if this program and all it's shaders have been compiled,
     * linked and validated successfuly.
     */
    public boolean isUsable(){
        return usable;
    }

    /**
     * Sets if the program can be used. Should only be called by the Renderer.
     * @param usable
     */
    public void setUsable(boolean usable){
        this.usable = usable;
    }

    /**
     * Usually called when the shader itself changes or during any
     * time when the var locations need to be refreshed.
     */
    public void resetLocations(){
        // NOTE: Shader sources will be reset seperately from the shader itself.
        for (Uniform uniform : uniforms.values()){
            uniform.reset(); // fixes issue with re-initialization
        }
        for (Entry<Attribute> entry : attribs){
            entry.getValue().location = -2;
        }
    }

    @Override
    public void setUpdateNeeded(){
        super.setUpdateNeeded();
        resetLocations();
    }

    /**
     * Called by the object manager to reset all object IDs. This causes
     * the shader to be reuploaded to the GPU incase the display was restarted.
     */
    @Override
    public void resetObject() {
        this.id = -1;
        this.usable = false;
        
        for (ShaderSource source : shaderList){
            source.resetObject();
        }
        
        setUpdateNeeded();
    }

    @Override
    public void deleteObject(Object rendererObject) {
        ((Renderer)rendererObject).deleteShader(this);
    }

    public NativeObject createDestructableClone(){
        return new Shader(this);
    }

}
