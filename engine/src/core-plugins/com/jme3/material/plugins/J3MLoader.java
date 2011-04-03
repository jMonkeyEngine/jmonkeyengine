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

package com.jme3.material.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.material.*;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.material.TechniqueDef.ShadowMode;
import com.jme3.shader.Shader.ShaderType;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Scanner;

public class J3MLoader implements AssetLoader {

    private AssetManager owner;
    private Scanner scan;
    private String fileName;

    private MaterialDef materialDef;
    private Material material;
    private TechniqueDef technique;
    private RenderState renderState;

    private String shaderLang;
    private String vertName;
    private String fragName;

    public J3MLoader(){
    }

    private void throwIfNequal(String expected, String got) throws IOException {
        if (expected == null)
            throw new IOException("Expected a statement, got '"+got+"'!");

        if (!expected.equals(got))
            throw new IOException("Expected '"+expected+"', got '"+got+"'!");
    }

    private void nextStatement(){
        while (true){
            if (scan.hasNext("\\}")){
                break;
            }else if (scan.hasNext("[\n;]")){
                scan.next();
            }else if (scan.hasNext("//")){
                scan.useDelimiter("\n");
                scan.next();
                scan.useDelimiter("\\p{javaWhitespace}+");
            }else{
                break;
            }
        }
    }

    private String readString(String end){
        scan.useDelimiter(end);
        String str = scan.next();
        scan.useDelimiter("\\p{javaWhitespace}+");
        return str.trim();
    }

    private Image createColorTexture(ColorRGBA color){
        if (color.getAlpha() == 1.0f){
            // create RGB texture
            ByteBuffer data = BufferUtils.createByteBuffer(3);
            byte[] bytes = color.asBytesRGBA();
            data.put(bytes[0]).put(bytes[1]).put(bytes[2]);
            data.flip();

            return new Image(Format.RGB8, 1, 1, data);
        }else{
            // create RGBA texture
            ByteBuffer data = BufferUtils.createByteBuffer(4);
            data.putInt(color.asIntRGBA());
            data.flip();

            return new Image(Format.RGBA8, 1, 1, data);
        }
    }

    private void readShaderStatement(ShaderType type) throws IOException {
        String lang = readString(":");

        String word = scan.next();
        throwIfNequal(":", word);

        word = readString("[\n;(\\})]"); // new line, semicolon, comment or brace will end a statement
        // locate source code

        if (type == ShaderType.Vertex)
            vertName = word;
        else if (type == ShaderType.Fragment)
            fragName = word;

        shaderLang = lang;
    }

    private void readLightMode(){
        String mode = readString("[\n;(\\})]");
        LightMode lm = LightMode.valueOf(mode);
        technique.setLightMode(lm);
    }

    private void readShadowMode(){
        String mode = readString("[\n;(\\})]");
        ShadowMode sm = ShadowMode.valueOf(mode);
        technique.setShadowMode(sm);
    }

    private void readParam() throws IOException{
        String word = scan.next();
        VarType type;
        if (word.equals("Color")){
            type = VarType.Vector4;
        }else{
            type = VarType.valueOf(word);
        }
        
        word = readString("[\n;(//)(\\})]");
        FixedFuncBinding ffBinding = null;
        if (word.contains(":")){
            // contains fixed func binding
            String[] split = word.split(":");
            word = split[0].trim();

            try {
                ffBinding = FixedFuncBinding.valueOf(split[1].trim());
            } catch (IllegalArgumentException ex){
                throw new IOException("FixedFuncBinding '" +
                                      split[1] + "' does not exist!");
            }
        }
        // TODO: add support for default vals
        materialDef.addMaterialParam(type, word, null, ffBinding);
    }

    private void readValueParam() throws IOException{
        String name = readString(":");
        throwIfNequal(":", scan.next());

        // parse value
        MatParam p = material.getMaterialDef().getMaterialParam(name);
        if (p == null)
            throw new IOException("The material parameter: "+name+" is undefined.");

        VarType type = p.getVarType();
        if (type.isTextureType()){
//            String texturePath = readString("[\n;(//)(\\})]");
            String texturePath = readString("[\n;(\\})]");
            boolean flipY = false;
            boolean repeat = false;
            if (texturePath.startsWith("Flip Repeat ")){
                texturePath = texturePath.substring(12).trim();
                flipY = true;
                repeat = true;
            }else if (texturePath.startsWith("Flip ")){
                texturePath = texturePath.substring(5).trim();
                flipY = true;
            }else if (texturePath.startsWith("Repeat ")){
                texturePath = texturePath.substring(7).trim();
                repeat = true;
            }

            TextureKey key = new TextureKey(texturePath, flipY);
            key.setAsCube(type == VarType.TextureCubeMap);
            key.setGenerateMips(true);

            Texture tex = owner.loadTexture(key);
            if (tex != null){
                if (repeat)
                    tex.setWrap(WrapMode.Repeat);

                material.setTextureParam(name, type, tex);
            }
        }else{
            switch (type){
                case Float:
                    material.setParam(name, type, scan.nextFloat());
                    break;
                case Vector2:
                    material.setParam(name, type, new Vector2f(scan.nextFloat(),
                                                               scan.nextFloat()));
                    break;
                case Vector3:
                    material.setParam(name, type, new Vector3f(scan.nextFloat(),
                                                               scan.nextFloat(),
                                                               scan.nextFloat()));
                    break;
                case Vector4:
                    material.setParam(name, type, new ColorRGBA(scan.nextFloat(),
                                                                scan.nextFloat(),
                                                                scan.nextFloat(),
                                                                scan.nextFloat()));
                    break;
                case Int:
                    material.setParam(name, type, scan.nextInt());
                    break;
                case Boolean:
                    material.setParam(name, type, scan.nextBoolean());
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown type: "+type);
            }
        }
    }

    private void readMaterialParams() throws IOException{
        nextStatement();

        String word = scan.next();
        throwIfNequal("{", word);

        nextStatement();

        while (true){
            if (scan.hasNext("\\}")){
                scan.next();
                break;
            }

            readParam();
            nextStatement();
        }
    }

    private void readExtendingMaterialParams() throws IOException{
        nextStatement();

        String word = scan.next();
        throwIfNequal("{", word);

        nextStatement();

        while (true){
            if (scan.hasNext("\\}")){
                scan.next();
                break;
            }

            readValueParam();
            nextStatement();
        }
    }

    private void readWorldParams() throws IOException{
        nextStatement();

        String word = scan.next();
        throwIfNequal("{", word);

        nextStatement();

        while (true){
            if (scan.hasNext("\\}")){
                scan.next();
                break;
            }

            word = readString("[\n;(//)(\\})]");
            if (word != null && !word.equals("")){
                technique.addWorldParam(word);
            }
            nextStatement();
        }
    }

    private boolean parseBoolean(String word){
        return word != null && word.equals("On");
    }

    private void readRenderStateStatement() throws IOException{
        String word = scan.next();
        if (word.equals("Wireframe")){
            renderState.setWireframe(parseBoolean(scan.next()));
        }else if (word.equals("FaceCull")){
            renderState.setFaceCullMode(FaceCullMode.valueOf(scan.next()));
        }else if (word.equals("DepthWrite")){
            renderState.setDepthWrite(parseBoolean(scan.next()));
        }else if (word.equals("DepthTest")){
            renderState.setDepthTest(parseBoolean(scan.next()));
        }else if (word.equals("Blend")){
            renderState.setBlendMode(BlendMode.valueOf(scan.next()));
        }else if (word.equals("AlphaTestFalloff")){
            renderState.setAlphaTest(true);
            renderState.setAlphaFallOff(scan.nextFloat());
        }else if (word.equals("PolyOffset")){
            float factor = scan.nextFloat();
            float units = scan.nextFloat();
            renderState.setPolyOffset(factor, units);
        }else if (word.equals("ColorWrite")){
            renderState.setColorWrite(parseBoolean(scan.next()));
        }else if (word.equals("PointSprite")){
            renderState.setPointSprite(parseBoolean(scan.next()));
        }else{
            throwIfNequal(null, word);
        }
    }

    private void readAdditionalRenderState() throws IOException{
        nextStatement();

        String word = scan.next();
        throwIfNequal("{", word);

        nextStatement();

        renderState = material.getAdditionalRenderState();

        while (true){
            if (scan.hasNext("\\}")){
                scan.next();
                break;
            }

            readRenderStateStatement();
            nextStatement();
        }

        renderState = null;
    }

    private void readRenderState() throws IOException{
        nextStatement();

        String word = scan.next();
        throwIfNequal("{", word);

        nextStatement();

        renderState = new RenderState();

        while (true){
            if (scan.hasNext("\\}")){
                scan.next();
                break;
            }

            readRenderStateStatement();
            nextStatement();
        }
        
        technique.setRenderState(renderState);
        renderState = null;
    }
    
    private void readDefine(){
        // stops at either next statement or colon
        // ways to end a statement:
        /*
        Block {
            Statement<new line>
            Statement;
            Statement //comment
            Statement }
        */
        String defineName = readString("[\n;:(//)(\\})]");
        if (defineName.equals(""))
            return;

        String matParamName = null;
        if (scan.hasNext(":")){
            scan.next();
            // this time without colon
            matParamName = readString("[\n;(//)(\\})]");
            // add define <-> param mapping
            technique.addShaderParamDefine(matParamName, defineName);
        }else{
            // add preset define
            technique.addShaderPresetDefine(defineName, VarType.Boolean, true);
        }
    }

    private void readDefines() throws IOException{
        nextStatement();

        String word = scan.next();
        throwIfNequal("{", word);

        nextStatement();

        while (true){
            if (scan.hasNext("\\}")){
                scan.next();
                break;
            }

            readDefine();
            nextStatement();
        }

    }

    private void readTechniqueStatement() throws IOException{
        String word = scan.next();
        if (word.equals("VertexShader")){
            readShaderStatement(ShaderType.Vertex);
        }else if (word.equals("FragmentShader")){
            readShaderStatement(ShaderType.Fragment);
        }else if (word.equals("LightMode")){
            readLightMode();
        }else if (word.equals("ShadowMode")){
            readShadowMode();
        }else if (word.equals("WorldParameters")){
            readWorldParams();
        }else if (word.equals("RenderState")){
            readRenderState();
        }else if (word.equals("Defines")){
            readDefines();
        }else{
            throwIfNequal(null, word);
        }
        nextStatement();
    }

    private void readTransparentStatement() throws IOException{
        String on = readString("[\n;(\\})]");
        material.setTransparent(parseBoolean(on));
    }

    private void readTechnique() throws IOException{
        String name = null;
        if (!scan.hasNext("\\{")){
            name = scan.next();
        }
        technique = new TechniqueDef(name);

        String word = scan.next();
        throwIfNequal("{", word);

        nextStatement();

        while (true){
            if (scan.hasNext("\\}")){
                scan.next();
                break;
            }

            readTechniqueStatement();
        }

        if (vertName != null && fragName != null){
            technique.setShaderFile(vertName, fragName, shaderLang);
        }
        
        materialDef.addTechniqueDef(technique);
        technique = null;
        vertName = null;
        fragName = null;
        shaderLang = null;
    }

    private void loadFromScanner() throws IOException{
        nextStatement();

        boolean extending = false;
        String name = null;
        String word = scan.next();
        if (word.equals("Material")){
            extending = true;
        }else if (word.equals("MaterialDef")){
            extending = false;
        }else{
            throw new IOException("Specified file is not a Material file");
        }

        nextStatement();

        word = readString("[(\\{)(//)\n:]");
        if (word == null || word.equals(""))
            throw new IOException("Material name cannot be empty");

        name = word;

        nextStatement();

        if (scan.hasNext(":")){
            if (!extending){
                throw new IOException("Must use 'Material' when extending.");
            }

            scan.next(); // skip colon
            String extendedMat = readString("\\{");

            MaterialDef def = (MaterialDef) owner.loadAsset(new AssetKey(extendedMat));
            if (def == null)
                throw new IOException("Extended material "+extendedMat+" cannot be found.");

            material = new Material(def);
            material.setAssetName(fileName);
        }else if (scan.hasNext("\\{")){
            if (extending){
                throw new IOException("Expected ':', got '{'");
            }
            materialDef = new MaterialDef(owner, name);
            // NOTE: pass file name for defs so they can be loaded later
            materialDef.setAssetName(fileName);
        }
        scan.next(); // skip {

        nextStatement();

        while (true){
            if (scan.hasNext("\\}")){
                scan.next();
                break;
            }

            word = scan.next();
            if (extending){
                if (word.equals("MaterialParameters")){
                    readExtendingMaterialParams();
                    nextStatement();
                }else if (word.equals("AdditionalRenderState")){
                    readAdditionalRenderState();
                    nextStatement();
                }else if (word.equals("Transparent")){
                    readTransparentStatement();
                    nextStatement();
                }
            }else{
                if (word.equals("Technique")){
                    readTechnique();
                    nextStatement();
                }else if (word.equals("MaterialParameters")){
                    readMaterialParams();
                    nextStatement();
                }else{
                    throw new IOException("Expected material statement, got '"+scan.next()+"'");
                }
            }
        }
    }

    public Object load(AssetInfo info) throws IOException {
        this.owner = info.getManager();

        InputStream in = info.openStream();
        try {
            scan = new Scanner(in);
            scan.useLocale(Locale.US);
            this.fileName = info.getKey().getName();
            loadFromScanner();
        } finally {
            if (in != null)
                in.close();
        }
        
        if (material != null){
            // material implementation
            return material;
        }else{
            // material definition
            return materialDef;
        }
    }

}
