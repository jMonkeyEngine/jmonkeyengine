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

import com.jme3.asset.*;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.material.*;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.material.TechniqueDef.ShadowMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.PlaceholderAssets;
import com.jme3.util.blockparser.BlockLanguageParser;
import com.jme3.util.blockparser.Statement;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class J3MLoader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(J3MLoader.class.getName());
    
    private AssetManager assetManager;
    private AssetKey key;

    private MaterialDef materialDef;
    private Material material;
    private TechniqueDef technique;
    private RenderState renderState;

    private String shaderLang;
    private String vertName;
    private String fragName;
    
    private static final String whitespacePattern = "\\p{javaWhitespace}+";

    public J3MLoader(){
    }

    private void throwIfNequal(String expected, String got) throws IOException {
        if (expected == null)
            throw new IOException("Expected a statement, got '"+got+"'!");

        if (!expected.equals(got))
            throw new IOException("Expected '"+expected+"', got '"+got+"'!");
    }

    // <TYPE> <LANG> : <SOURCE>
    private void readShaderStatement(String statement) throws IOException {
        String[] split = statement.split(":");
        if (split.length != 2){
            throw new IOException("Shader statement syntax incorrect" + statement);
        }
        String[] typeAndLang = split[0].split(whitespacePattern);
        if (typeAndLang.length != 2){
            throw new IOException("Shader statement syntax incorrect: " + statement);
        }
        shaderLang = typeAndLang[1];
        if (typeAndLang[0].equals("VertexShader")){
            vertName = split[1].trim();
        }else if (typeAndLang[0].equals("FragmentShader")){
            fragName = split[1].trim();
        }
    }

    // LightMode <MODE>
    private void readLightMode(String statement) throws IOException{
        String[] split = statement.split(whitespacePattern);
        if (split.length != 2){
            throw new IOException("LightMode statement syntax incorrect");
        }
        LightMode lm = LightMode.valueOf(split[1]);
        technique.setLightMode(lm);
    }

    // ShadowMode <MODE>
    private void readShadowMode(String statement) throws IOException{
        String[] split = statement.split(whitespacePattern);
        if (split.length != 2){
            throw new IOException("ShadowMode statement syntax incorrect");
        }
        ShadowMode sm = ShadowMode.valueOf(split[1]);
        technique.setShadowMode(sm);
    }

    private Object readValue(VarType type, String value) throws IOException{
        if (type.isTextureType()){
//            String texturePath = readString("[\n;(//)(\\})]");
            String texturePath = value.trim();
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

            TextureKey texKey = new TextureKey(texturePath, flipY);
            texKey.setAsCube(type == VarType.TextureCubeMap);
            texKey.setGenerateMips(true);

            Texture tex;
            try {
                tex = assetManager.loadTexture(texKey);
            } catch (AssetNotFoundException ex){
                logger.log(Level.WARNING, "Cannot locate {0} for material {1}", new Object[]{texKey, key});
                tex = null;
            }
            if (tex != null){
                if (repeat){
                    tex.setWrap(WrapMode.Repeat);
                }
            }else{
                tex = new Texture2D(PlaceholderAssets.getPlaceholderImage());
            }
            return tex;
        }else{
            String[] split = value.trim().split(whitespacePattern);
            switch (type){
                case Float:
                    if (split.length != 1){
                        throw new IOException("Float value parameter must have 1 entry: " + value);
                    }
                     return Float.parseFloat(split[0]);
                case Vector2:
                    if (split.length != 2){
                        throw new IOException("Vector2 value parameter must have 2 entries: " + value);
                    }
                    return new Vector2f(Float.parseFloat(split[0]),
                                                               Float.parseFloat(split[1]));
                case Vector3:
                    if (split.length != 3){
                        throw new IOException("Vector3 value parameter must have 3 entries: " + value);
                    }
                    return new Vector3f(Float.parseFloat(split[0]),
                                                               Float.parseFloat(split[1]),
                                                               Float.parseFloat(split[2]));
                case Vector4:
                    if (split.length != 4){
                        throw new IOException("Vector4 value parameter must have 4 entries: " + value);
                    }
                    return new ColorRGBA(Float.parseFloat(split[0]),
                                                                Float.parseFloat(split[1]),
                                                                Float.parseFloat(split[2]),
                                                                Float.parseFloat(split[3]));
                case Int:
                    if (split.length != 1){
                        throw new IOException("Int value parameter must have 1 entry: " + value);
                    }
                    return Integer.parseInt(split[0]);
                case Boolean:
                    if (split.length != 1){
                        throw new IOException("Boolean value parameter must have 1 entry: " + value);
                    }
                    return Boolean.parseBoolean(split[0]);
                default:
                    throw new UnsupportedOperationException("Unknown type: "+type);
            }
        }
    }
    
    // <TYPE> <NAME> [ "(" <FFBINDING> ")" ] [ ":" <DEFAULTVAL> ]
    private void readParam(String statement) throws IOException{
        String name;
        String defaultVal = null;
        FixedFuncBinding ffBinding = null;
        
        String[] split = statement.split(":");
        
        // Parse default val
        if (split.length == 1){
            // Doesn't contain default value
        }else{
            if (split.length != 2){
                throw new IOException("Parameter statement syntax incorrect");
            }
            statement = split[0].trim();
            defaultVal = split[1].trim();           
        }
        
        // Parse ffbinding
        int startParen = statement.indexOf("(");
        if (startParen != -1){
            // get content inside parentheses
            int endParen = statement.indexOf(")", startParen);
            String bindingStr = statement.substring(startParen+1, endParen).trim();
            try {
                ffBinding = FixedFuncBinding.valueOf(bindingStr);
            } catch (IllegalArgumentException ex){
                throw new IOException("FixedFuncBinding '" +
                                      split[1] + "' does not exist!");
            }
            statement = statement.substring(0, startParen);
        }
        
        // Parse type + name
        split = statement.split(whitespacePattern);
        if (split.length != 2){
            throw new IOException("Parameter statement syntax incorrect");
        }
        
        VarType type;
        if (split[0].equals("Color")){
            type = VarType.Vector4;
        }else{
            type = VarType.valueOf(split[0]);
        }
        
        name = split[1];
        
        Object defaultValObj = null;
        if (defaultVal != null){ 
            defaultValObj = readValue(type, defaultVal);
        }
        
        materialDef.addMaterialParam(type, name, defaultValObj, ffBinding);
    }

    private void readValueParam(String statement) throws IOException{
        // Use limit=1 incase filename contains colons
        String[] split = statement.split(":", 2);
        if (split.length != 2){
            throw new IOException("Value parameter statement syntax incorrect");
        }
        String name = split[0].trim();

        // parse value
        MatParam p = material.getMaterialDef().getMaterialParam(name);
        if (p == null){
            throw new IOException("The material parameter: "+name+" is undefined.");
        }

        Object valueObj = readValue(p.getVarType(), split[1]);
        if (p.getVarType().isTextureType()){
            material.setTextureParam(name, p.getVarType(), (Texture) valueObj);
        }else{
            material.setParam(name, p.getVarType(), valueObj);
        }
    }

    private void readMaterialParams(List<Statement> paramsList) throws IOException{
        for (Statement statement : paramsList){
            readParam(statement.getLine());
        }
    }

    private void readExtendingMaterialParams(List<Statement> paramsList) throws IOException{
        for (Statement statement : paramsList){
            readValueParam(statement.getLine());
        }
    }

    private void readWorldParams(List<Statement> worldParams) throws IOException{
        for (Statement statement : worldParams){
            technique.addWorldParam(statement.getLine());
        }
    }

    private boolean parseBoolean(String word){
        return word != null && word.equals("On");
    }

    private void readRenderStateStatement(String statement) throws IOException{
        String[] split = statement.split(whitespacePattern);
        if (split[0].equals("Wireframe")){
            renderState.setWireframe(parseBoolean(split[1]));
        }else if (split[0].equals("FaceCull")){
            renderState.setFaceCullMode(FaceCullMode.valueOf(split[1]));
        }else if (split[0].equals("DepthWrite")){
            renderState.setDepthWrite(parseBoolean(split[1]));
        }else if (split[0].equals("DepthTest")){
            renderState.setDepthTest(parseBoolean(split[1]));
        }else if (split[0].equals("Blend")){
            renderState.setBlendMode(BlendMode.valueOf(split[1]));
        }else if (split[0].equals("AlphaTestFalloff")){
            renderState.setAlphaTest(true);
            renderState.setAlphaFallOff(Float.parseFloat(split[1]));
        }else if (split[0].equals("PolyOffset")){
            float factor = Float.parseFloat(split[1]);
            float units = Float.parseFloat(split[2]);
            renderState.setPolyOffset(factor, units);
        }else if (split[0].equals("ColorWrite")){
            renderState.setColorWrite(parseBoolean(split[1]));
        }else if (split[0].equals("PointSprite")){
            renderState.setPointSprite(parseBoolean(split[1]));
        }else{
            throwIfNequal(null, split[0]);
        }
    }

    private void readAdditionalRenderState(List<Statement> renderStates) throws IOException{
        renderState = material.getAdditionalRenderState();
        for (Statement statement : renderStates){
            readRenderStateStatement(statement.getLine());
        }
        renderState = null;
    }

    private void readRenderState(List<Statement> renderStates) throws IOException{
        renderState = new RenderState();
        for (Statement statement : renderStates){
            readRenderStateStatement(statement.getLine());
        }
        technique.setRenderState(renderState);
        renderState = null;
    }
    
    // <DEFINENAME> [ ":" <PARAMNAME> ]
    private void readDefine(String statement) throws IOException{
        String[] split = statement.split(":");
        if (split.length == 1){
            // add preset define
            technique.addShaderPresetDefine(split[0].trim(), VarType.Boolean, true);
        }else if (split.length == 2){
            technique.addShaderParamDefine(split[1].trim(), split[0].trim());
        }else{
            throw new IOException("Define syntax incorrect");
        }
    }

    private void readDefines(List<Statement> defineList) throws IOException{
        for (Statement statement : defineList){
            readDefine(statement.getLine());
        }

    }

    private void readTechniqueStatement(Statement statement) throws IOException{
        String[] split = statement.getLine().split("[ \\{]");
        if (split[0].equals("VertexShader") ||
            split[0].equals("FragmentShader")){
            readShaderStatement(statement.getLine());
        }else if (split[0].equals("LightMode")){
            readLightMode(statement.getLine());
        }else if (split[0].equals("ShadowMode")){
            readShadowMode(statement.getLine());
        }else if (split[0].equals("WorldParameters")){
            readWorldParams(statement.getContents());
        }else if (split[0].equals("RenderState")){
            readRenderState(statement.getContents());
        }else if (split[0].equals("Defines")){
            readDefines(statement.getContents());
        }else{
            throwIfNequal(null, split[0]);
        }
    }

    private void readTransparentStatement(String statement) throws IOException{
        String[] split = statement.split(whitespacePattern);
        if (split.length != 2){
            throw new IOException("Transparent statement syntax incorrect");
        }
        material.setTransparent(parseBoolean(split[1]));
    }

    private void readTechnique(Statement techStat) throws IOException{
        String[] split = techStat.getLine().split(whitespacePattern);
        if (split.length == 1){
            technique = new TechniqueDef(null);
        }else if (split.length == 2){
            technique = new TechniqueDef(split[1]);
        }else{
            throw new IOException("Technique statement syntax incorrect");
        }
        
        for (Statement statement : techStat.getContents()){
            readTechniqueStatement(statement);
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

    private void loadFromRoot(List<Statement> roots) throws IOException{
        if (roots.size() == 2){
            Statement exception = roots.get(0);
            String line = exception.getLine();
            if (line.startsWith("Exception")){
                throw new AssetLoadException(line.substring("Exception ".length()));
            }else{
                throw new IOException("In multiroot material, expected first statement to be 'Exception'");
            }
        }else if (roots.size() != 1){
            throw new IOException("Too many roots in J3M/J3MD file");
        }
        
        boolean extending = false;
        Statement materialStat = roots.get(0);
        String materialName = materialStat.getLine();
        if (materialName.startsWith("MaterialDef")){
            materialName = materialName.substring("MaterialDef ".length()).trim();
            extending = false;
        }else if (materialName.startsWith("Material")){
            materialName = materialName.substring("Material ".length()).trim();
            extending = true;
        }else{
            throw new IOException("Specified file is not a Material file");
        }
        
        String[] split = materialName.split(":", 2);
        
        if (materialName.equals("")){
            throw new IOException("Material name cannot be empty");
        }

        if (split.length == 2){
            if (!extending){
                throw new IOException("Must use 'Material' when extending.");
            }

            String extendedMat = split[1].trim();

            MaterialDef def = (MaterialDef) assetManager.loadAsset(new AssetKey(extendedMat));
            if (def == null)
                throw new IOException("Extended material "+extendedMat+" cannot be found.");

            material = new Material(def);
            material.setKey(key);
//            material.setAssetName(fileName);
        }else if (split.length == 1){
            if (extending){
                throw new IOException("Expected ':', got '{'");
            }
            materialDef = new MaterialDef(assetManager, materialName);
            // NOTE: pass file name for defs so they can be loaded later
            materialDef.setAssetName(key.getName());
        }else{
            throw new IOException("Cannot use colon in material name/path");
        }
        
        for (Statement statement : materialStat.getContents()){
            split = statement.getLine().split("[ \\{]");
            String statType = split[0];
            if (extending){
                if (statType.equals("MaterialParameters")){
                    readExtendingMaterialParams(statement.getContents());
                }else if (statType.equals("AdditionalRenderState")){
                    readAdditionalRenderState(statement.getContents());
                }else if (statType.equals("Transparent")){
                    readTransparentStatement(statement.getLine());
                }
            }else{
                if (statType.equals("Technique")){
                    readTechnique(statement);
                }else if (statType.equals("MaterialParameters")){
                    readMaterialParams(statement.getContents());
                }else{
                    throw new IOException("Expected material statement, got '"+statType+"'");
                }
            }
        }
    }

    public Object load(AssetInfo info) throws IOException {
        this.assetManager = info.getManager();

        InputStream in = info.openStream();
        try {
            key = info.getKey();
            loadFromRoot(BlockLanguageParser.parse(in));
        } finally {
            if (in != null){
                in.close();
            }
        }
        
        if (material != null){
            if (!(info.getKey() instanceof MaterialKey)){
                throw new IOException("Material instances must be loaded via MaterialKey");
            }
            // material implementation
            return material;
        }else{
            // material definition
            return materialDef;
        }
    }

}
