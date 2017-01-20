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
package com.jme3.material.plugins;

import com.jme3.material.logic.*;
import com.jme3.asset.*;
import com.jme3.material.*;
import com.jme3.material.RenderState.BlendEquation;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.material.TechniqueDef.ShadowMode;
import com.jme3.material.logic.StaticPassLightingLogic;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.shader.*;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.PlaceholderAssets;
import com.jme3.util.blockparser.BlockLanguageParser;
import com.jme3.util.blockparser.Statement;
import com.jme3.util.clone.Cloner;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class J3MLoader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(J3MLoader.class.getName());
   // private ErrorLogger errors;
    private ShaderNodeLoaderDelegate nodesLoaderDelegate;
    boolean isUseNodes = false;
    int langSize = 0;

    private AssetManager assetManager;
    private AssetKey key;

    private MaterialDef materialDef;
    private Material material;
    private TechniqueDef technique;
    private RenderState renderState;
    private ArrayList<String> presetDefines = new ArrayList<String>();

    private List<EnumMap<Shader.ShaderType, String>> shaderLanguages;
    private EnumMap<Shader.ShaderType, String> shaderNames;

    private static final String whitespacePattern = "\\p{javaWhitespace}+";

    public J3MLoader() {
        shaderLanguages = new ArrayList<>();// EnumMap<>(Shader.ShaderType.class);
        shaderNames = new EnumMap<>(Shader.ShaderType.class);
    }


    // <TYPE> <LANG> : <SOURCE>
    private void readShaderStatement(String statement) throws IOException {
        String[] split = statement.split(":");
        if (split.length != 2) {
            throw new IOException("Shader statement syntax incorrect" + statement);
        }
        String[] typeAndLang = split[0].split(whitespacePattern);

        for (Shader.ShaderType shaderType : Shader.ShaderType.values()) {
            if (typeAndLang[0].equals(shaderType.toString() + "Shader")) {

                readShaderDefinition(shaderType, split[1].trim(), Arrays.copyOfRange(typeAndLang, 1, typeAndLang.length));
            }
        }
    }


    private void readShaderDefinition(Shader.ShaderType shaderType, String name, String... languages) {
        shaderNames.put(shaderType, name);

        if (langSize != 0 && langSize != languages.length) {
            throw new AssetLoadException("Technique " + technique.getName() + " must have the same number of languages for each shader type.");
        }
        langSize = languages.length;
        for (int i = 0; i < languages.length; i++) {
            if (i >= shaderLanguages.size()) {
                shaderLanguages.add(new EnumMap<Shader.ShaderType, String>(Shader.ShaderType.class));
            }
            shaderLanguages.get(i).put(shaderType, languages[i]);
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
    
    
    // LightMode <SPACE>
    private void readLightSpace(String statement) throws IOException{
        String[] split = statement.split(whitespacePattern);
        if (split.length != 2){
            throw new IOException("LightSpace statement syntax incorrect");
        }
        TechniqueDef.LightSpace ls = TechniqueDef.LightSpace.valueOf(split[1]);        
        technique.setLightSpace(ls);
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

    private List<String> tokenizeTextureValue(final String value) {
        final List<String> matchList = new ArrayList<String>();
        final Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        final Matcher regexMatcher = regex.matcher(value.trim());

        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                matchList.add(regexMatcher.group(2));
            } else {
                matchList.add(regexMatcher.group());
            }
        }

        return matchList;
    }

    private List<TextureOptionValue> parseTextureOptions(final List<String> values) {
        final List<TextureOptionValue> matchList = new ArrayList<TextureOptionValue>();

        if (values.isEmpty() || values.size() == 1) {
            return matchList;
        }

        // Loop through all but the last value, the last one is going to be the path.
        for (int i = 0; i < values.size() - 1; i++) {
            final String value = values.get(i);
            final TextureOption textureOption = TextureOption.getTextureOption(value);

            if (textureOption == null && !value.contains("\\") && !value.contains("/") && !values.get(0).equals("Flip") && !values.get(0).equals("Repeat")) {
                logger.log(Level.WARNING, "Unknown texture option \"{0}\" encountered for \"{1}\" in material \"{2}\"", new Object[]{value, key, material.getKey().getName()});
            } else if (textureOption != null){
                final String option = textureOption.getOptionValue(value);
                matchList.add(new TextureOptionValue(textureOption, option));
            }
        }

        return matchList;
    }

    private boolean isTexturePathDeclaredTheTraditionalWay(final List<TextureOptionValue> optionValues, final String texturePath) {
        final boolean startsWithOldStyle = texturePath.startsWith("Flip Repeat ") || texturePath.startsWith("Flip ") ||
                texturePath.startsWith("Repeat ") || texturePath.startsWith("Repeat Flip ");

        if (!startsWithOldStyle) {
            return false;
        }

        if (optionValues.size() == 1 && (optionValues.get(0).textureOption == TextureOption.Flip || optionValues.get(0).textureOption == TextureOption.Repeat)) {
            return true;
        } else if (optionValues.size() == 2 && optionValues.get(0).textureOption == TextureOption.Flip && optionValues.get(1).textureOption == TextureOption.Repeat) {
            return true;
        } else if (optionValues.size() == 2 && optionValues.get(0).textureOption == TextureOption.Repeat && optionValues.get(1).textureOption == TextureOption.Flip) {
            return true;
        }

        return false;
    }

    private Texture parseTextureType(final VarType type, final String value) {
        final List<String> textureValues = tokenizeTextureValue(value);
        final List<TextureOptionValue> textureOptionValues = parseTextureOptions(textureValues);

        TextureKey textureKey = null;

        // If there is only one token on the value, it must be the path to the texture.
        if (textureValues.size() == 1) {
            textureKey = new TextureKey(textureValues.get(0), false);
        } else {
            String texturePath = value.trim();

            // If there are no valid "new" texture options specified but the path is split into several parts, lets parse the old way.
            if (isTexturePathDeclaredTheTraditionalWay(textureOptionValues, texturePath)) {
                boolean flipY = false;

                if (texturePath.startsWith("Flip Repeat ") || texturePath.startsWith("Repeat Flip ")) {
                    texturePath = texturePath.substring(12).trim();
                    flipY = true;
                } else if (texturePath.startsWith("Flip ")) {
                    texturePath = texturePath.substring(5).trim();
                    flipY = true;
                } else if (texturePath.startsWith("Repeat ")) {
                    texturePath = texturePath.substring(7).trim();
                }

                // Support path starting with quotes (double and single)
                if (texturePath.startsWith("\"") || texturePath.startsWith("'")) {
                    texturePath = texturePath.substring(1);
                }

                // Support path ending with quotes (double and single)
                if (texturePath.endsWith("\"") || texturePath.endsWith("'")) {
                    texturePath = texturePath.substring(0, texturePath.length() - 1);
                }

                textureKey = new TextureKey(texturePath, flipY);
            }

            if (textureKey == null) {
                textureKey = new TextureKey(textureValues.get(textureValues.size() - 1), false);
            }

            // Apply texture options to the texture key
            if (!textureOptionValues.isEmpty()) {
                for (final TextureOptionValue textureOptionValue : textureOptionValues) {
                    textureOptionValue.applyToTextureKey(textureKey);
                }
            }
        }

        switch (type) {
            case Texture3D:
                textureKey.setTextureTypeHint(Texture.Type.ThreeDimensional);
                break;
            case TextureArray:
                textureKey.setTextureTypeHint(Texture.Type.TwoDimensionalArray);
                break;
            case TextureCubeMap:
                textureKey.setTextureTypeHint(Texture.Type.CubeMap);
                break;
        }

        textureKey.setGenerateMips(true);

        Texture texture;

        try {
            texture = assetManager.loadTexture(textureKey);
        } catch (AssetNotFoundException ex){
            logger.log(Level.WARNING, "Cannot locate {0} for material {1}", new Object[]{textureKey, key});
            texture = null;
        }

        if (texture == null){
            texture = new Texture2D(PlaceholderAssets.getPlaceholderImage(assetManager));
            texture.setKey(textureKey);
            texture.setName(textureKey.getName());
        }

        // Apply texture options to the texture
        if (!textureOptionValues.isEmpty()) {
            for (final TextureOptionValue textureOptionValue : textureOptionValues) {
                textureOptionValue.applyToTexture(texture);
            }
        }

        return texture;
    }

    private Object readValue(final VarType type, final String value) throws IOException{
        if (type.isTextureType()) {
            return parseTextureType(type, value);
        } else {
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

    // <TYPE> <NAME> [ "(" <FFBINDING> ")" ] [-LINEAR] [ ":" <DEFAULTVAL> ]
    private void readParam(String statement) throws IOException{
        String name;
        String defaultVal = null;
        ColorSpace colorSpace = null;

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

        if (statement.endsWith("-LINEAR")) {
            colorSpace = ColorSpace.Linear;
            statement = statement.substring(0, statement.length() - "-LINEAR".length());
        }

        // Parse ffbinding
        int startParen = statement.indexOf("(");
        if (startParen != -1){
            // get content inside parentheses
            int endParen = statement.indexOf(")", startParen);
            String bindingStr = statement.substring(startParen+1, endParen).trim();
            // don't care about bindingStr
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
        if(type.isTextureType()){
            materialDef.addMaterialParamTexture(type, name, colorSpace);
        }else{
            materialDef.addMaterialParam(type, name, defaultValObj);
        }

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

    private void readRenderStateStatement(Statement statement) throws IOException{
        String[] split = statement.getLine().split(whitespacePattern);
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
        }else if (split[0].equals("BlendEquation")){
            renderState.setBlendEquation(BlendEquation.valueOf(split[1]));
        }else if (split[0].equals("BlendEquationAlpha")){
            renderState.setBlendEquationAlpha(RenderState.BlendEquationAlpha.valueOf(split[1]));
        }else if (split[0].equals("AlphaTestFalloff")){
            // Ignore for backwards compatbility
        }else if (split[0].equals("PolyOffset")){
            float factor = Float.parseFloat(split[1]);
            float units = Float.parseFloat(split[2]);
            renderState.setPolyOffset(factor, units);
        }else if (split[0].equals("ColorWrite")){
            renderState.setColorWrite(parseBoolean(split[1]));
        }else if (split[0].equals("PointSprite")){
            // Ignore for backwards compatbility
        }else if (split[0].equals("DepthFunc")){
            renderState.setDepthFunc(RenderState.TestFunction.valueOf(split[1]));
        }else if (split[0].equals("AlphaFunc")){
            renderState.setAlphaFunc(RenderState.TestFunction.valueOf(split[1]));
        }else if (split[0].equals("LineWidth")){
            renderState.setLineWidth(Float.parseFloat(split[1]));
        } else {
            throw new MatParseException(null, split[0], statement);
        }
    }

    private void readAdditionalRenderState(List<Statement> renderStates) throws IOException{
        renderState = material.getAdditionalRenderState();
        for (Statement statement : renderStates){
            readRenderStateStatement(statement);
        }
        renderState = null;
    }

    private void readRenderState(List<Statement> renderStates) throws IOException{
        renderState = new RenderState();
        for (Statement statement : renderStates){
            readRenderStateStatement(statement);
        }
        technique.setRenderState(renderState);
        renderState = null;
    }

    private void readForcedRenderState(List<Statement> renderStates) throws IOException{
        renderState = new RenderState();
        for (Statement statement : renderStates){
            readRenderStateStatement(statement);
        }
        technique.setForcedRenderState(renderState);
        renderState = null;
    }

    // <DEFINENAME> [ ":" <PARAMNAME> ]
    private void readDefine(String statement) throws IOException{
        String[] split = statement.split(":");
        if (split.length == 1){
            String defineName = split[0].trim();
            presetDefines.add(defineName);
        }else if (split.length == 2){
            String defineName = split[0].trim();
            String paramName = split[1].trim();
            MatParam param = materialDef.getMaterialParam(paramName);
            if (param == null) {
                logger.log(Level.WARNING, "In technique ''{0}'':\n"
                        + "Define ''{1}'' mapped to non-existent"
                        + " material parameter ''{2}'', ignoring.",
                        new Object[]{technique.getName(), defineName, paramName});
                return;
            }
            
            VarType paramType = param.getVarType();
            technique.addShaderParamDefine(paramName, paramType, defineName);
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
                split[0].equals("FragmentShader") ||
                split[0].equals("GeometryShader") ||
                split[0].equals("TessellationControlShader") ||
                split[0].equals("TessellationEvaluationShader")) {
            readShaderStatement(statement.getLine());
        }else if (split[0].equals("LightMode")){
            readLightMode(statement.getLine());
        }else if (split[0].equals("LightSpace")){
            readLightSpace(statement.getLine());
        }else if (split[0].equals("ShadowMode")){
            readShadowMode(statement.getLine());
        }else if (split[0].equals("WorldParameters")){
            readWorldParams(statement.getContents());
        }else if (split[0].equals("RenderState")){
            readRenderState(statement.getContents());
        }else if (split[0].equals("ForcedRenderState")){
            readForcedRenderState(statement.getContents());
        }else if (split[0].equals("Defines")){
            readDefines(statement.getContents());
        } else if (split[0].equals("ShaderNodesDefinitions")) {
            initNodesLoader();
            if (isUseNodes) {
                nodesLoaderDelegate.readNodesDefinitions(statement.getContents());
            }
        } else if (split[0].equals("VertexShaderNodes")) {
            initNodesLoader();
            if (isUseNodes) {
                nodesLoaderDelegate.readVertexShaderNodes(statement.getContents());
            }
        } else if (split[0].equals("FragmentShaderNodes")) {
            initNodesLoader();
            if (isUseNodes) {
                nodesLoaderDelegate.readFragmentShaderNodes(statement.getContents());
            }
        } else if (split[0].equals("NoRender")) {
            technique.setNoRender(true);
        } else {
            throw new MatParseException(null, split[0], statement);
        }
    }

    private void readTransparentStatement(String statement) throws IOException{
        String[] split = statement.split(whitespacePattern);
        if (split.length != 2){
            throw new IOException("Transparent statement syntax incorrect");
        }
        material.setTransparent(parseBoolean(split[1]));
    }
    
    private static String createShaderPrologue(List<String> presetDefines) {
        DefineList dl = new DefineList(presetDefines.size());
        for (int i = 0; i < presetDefines.size(); i++) {
            dl.set(i, 1);
        }
        StringBuilder sb = new StringBuilder();
        dl.generateSource(sb, presetDefines, null);
        return sb.toString();
    }

    private void readTechnique(Statement techStat) throws IOException{
        isUseNodes = false;
        String[] split = techStat.getLine().split(whitespacePattern);
        Cloner cloner = new Cloner();

        String name;
        if (split.length == 1) {
            name = TechniqueDef.DEFAULT_TECHNIQUE_NAME;
        } else if (split.length == 2) {
            name = split[1];
        } else {
            throw new IOException("Technique statement syntax incorrect");
        }

        String techniqueUniqueName = materialDef.getAssetName() + "@" + name;
        technique = new TechniqueDef(name, techniqueUniqueName.hashCode());

        for (Statement statement : techStat.getContents()){
            readTechniqueStatement(statement);
        }

        technique.setShaderPrologue(createShaderPrologue(presetDefines));

        switch (technique.getLightMode()) {
            case Disable:
                technique.setLogic(new DefaultTechniqueDefLogic(technique));
                break;
            case MultiPass:
                technique.setLogic(new MultiPassLightingLogic(technique));
                break;
            case SinglePass:
                technique.setLogic(new SinglePassLightingLogic(technique));
                break;
            case StaticPass:
                technique.setLogic(new StaticPassLightingLogic(technique));
                break;
            case SinglePassAndImageBased:
                technique.setLogic(new SinglePassAndImageBasedLightingLogic(technique));
                break;
            default:
                throw new UnsupportedOperationException();
        }

        List<TechniqueDef> techniqueDefs = new ArrayList<>();

        if(isUseNodes){
            nodesLoaderDelegate.computeConditions();
            
            //used for caching later, the shader here is not a file.
            
            // KIRILL 9/19/2015
            // Not sure if this is needed anymore, since shader caching
            // is now done by TechniqueDef.
            technique.setShaderFile(technique.hashCode() + "", technique.hashCode() + "", "GLSL100", "GLSL100");
            techniqueDefs.add(technique);
        }else if (shaderNames.containsKey(Shader.ShaderType.Vertex) && shaderNames.containsKey(Shader.ShaderType.Fragment)) {
            if (shaderLanguages.size() > 1) {
                for (int i = 1; i < shaderLanguages.size(); i++) {
                    cloner.clearIndex();
                    TechniqueDef td = cloner.clone(technique);
                    td.setShaderFile(shaderNames, shaderLanguages.get(i));
                    techniqueDefs.add(td);
                }
            }
            technique.setShaderFile(shaderNames, shaderLanguages.get(0));
            techniqueDefs.add(technique);

        } else {
            technique = null;
            shaderLanguages.clear();
            shaderNames.clear();
            presetDefines.clear();
            langSize = 0;
            logger.log(Level.WARNING, "Fixed function technique was ignored");
            logger.log(Level.WARNING, "Fixed function technique ''{0}'' was ignored for material {1}",
                    new Object[]{name, key});
            return;
        }

        for (TechniqueDef techniqueDef : techniqueDefs) {
            materialDef.addTechniqueDef(techniqueDef);
        }

        technique = null;
        langSize = 0;
        shaderLanguages.clear();
        shaderNames.clear();
        presetDefines.clear();
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
            throw new MatParseException("Material name cannot be empty", materialStat);
        }

        if (split.length == 2){
            if (!extending){
                throw new MatParseException("Must use 'Material' when extending.", materialStat);
            }

            String extendedMat = split[1].trim();

            MaterialDef def = (MaterialDef) assetManager.loadAsset(new AssetKey(extendedMat));
            if (def == null) {
                throw new MatParseException("Extended material " + extendedMat + " cannot be found.", materialStat);
            }

            material = new Material(def);
            material.setKey(key);
            material.setName(split[0].trim());
//            material.setAssetName(fileName);
        }else if (split.length == 1){
            if (extending){
                throw new MatParseException("Expected ':', got '{'", materialStat);
            }
            materialDef = new MaterialDef(assetManager, materialName);
            // NOTE: pass file name for defs so they can be loaded later
            materialDef.setAssetName(key.getName());
        }else{
            throw new MatParseException("Cannot use colon in material name/path", materialStat);
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
                    throw new MatParseException("Expected material statement, got '"+statType+"'", statement);
                }
            }
        }
    }

    public Object load(AssetInfo info) throws IOException {
        this.assetManager = info.getManager();

        InputStream in = info.openStream();
        try {
            key = info.getKey();
            if (key.getExtension().equals("j3m") && !(key instanceof MaterialKey)) {
                throw new IOException("Material instances must be loaded via MaterialKey");
            } else if (key.getExtension().equals("j3md") && key instanceof MaterialKey) {
                throw new IOException("Material definitions must be loaded via AssetKey");
            }
            loadFromRoot(BlockLanguageParser.parse(in));
        } finally {
            if (in != null){
                in.close();
            }
        }

        if (material != null){
            // material implementation
            return material;
        }else{
            // material definition
            return materialDef;
        }
    }

    public MaterialDef loadMaterialDef(List<Statement> roots, AssetManager manager, AssetKey key) throws IOException {
        this.key = key;
        this.assetManager = manager;
        loadFromRoot(roots);
        return materialDef;
    }

    protected void initNodesLoader() {
        if (!isUseNodes) {
            isUseNodes = shaderNames.get(Shader.ShaderType.Vertex) == null && shaderNames.get(Shader.ShaderType.Fragment) == null;
            if (isUseNodes) {
                if (nodesLoaderDelegate == null) {
                    nodesLoaderDelegate = new ShaderNodeLoaderDelegate();
                }else{
                    nodesLoaderDelegate.clear();
                }
                nodesLoaderDelegate.setTechniqueDef(technique);
                nodesLoaderDelegate.setMaterialDef(materialDef);
                nodesLoaderDelegate.setAssetManager(assetManager);
            }
        }
    }

    /**
     * Texture options allow you to specify how a texture should be initialized by including an option before
     * the path to the texture in the .j3m file.
     * <p>
     *     <b>Example:</b>
     *     <pre>
     *     DiffuseMap: MinTrilinear MagBilinear WrapRepeat_S "some/path/to a/texture.png"
     *     </pre>
     *     This would apply a minification filter of "Trilinear", a magnification filter of "Bilinear" and set the wrap mode to "Repeat".
     * </p>
     * <p>
     *     <b>Note:</b> If several filters of the same type are added, eg. MinTrilinear MinNearestLinearMipMap, the last one will win.
     * </p>
     */
    private enum TextureOption {

        /**
         * Applies a {@link com.jme3.texture.Texture.MinFilter} to the texture.
         */
        Min {
            @Override
            public void applyToTexture(final String option, final Texture texture) {
                texture.setMinFilter(Texture.MinFilter.valueOf(option));
            }
        },

        /**
         * Applies a {@link com.jme3.texture.Texture.MagFilter} to the texture.
         */
        Mag {
            @Override
            public void applyToTexture(final String option, final Texture texture) {
                texture.setMagFilter(Texture.MagFilter.valueOf(option));
            }
        },

        /**
         * Applies a {@link com.jme3.texture.Texture.WrapMode} to the texture. This also supports {@link com.jme3.texture.Texture.WrapAxis}
         * by adding "_AXIS" to the texture option. For instance if you wanted to repeat on the S (horizontal) axis, you
         * would use <pre>WrapRepeat_S</pre> as a texture option.
         */
        Wrap {
            @Override
            public void applyToTexture(final String option, final Texture texture) {
                final int separatorPosition = option.indexOf("_");

                if (separatorPosition >= option.length() - 2) {
                    final String axis = option.substring(separatorPosition + 1);
                    final String mode = option.substring(0, separatorPosition);
                    final Texture.WrapAxis wrapAxis = Texture.WrapAxis.valueOf(axis);
                    texture.setWrap(wrapAxis, Texture.WrapMode.valueOf(mode));
                } else {
                    texture.setWrap(Texture.WrapMode.valueOf(option));
                }
            }
        },

        /**
         * Applies a {@link com.jme3.texture.Texture.WrapMode#Repeat} to the texture. This is simply an alias for
         * WrapRepeat, please use WrapRepeat instead if possible as this may become deprecated later on.
         */
        Repeat {
            @Override
            public void applyToTexture(final String option, final Texture texture) {
                Wrap.applyToTexture("Repeat", texture);
            }
        },

        /**
         * Applies flipping on the Y axis to the {@link TextureKey#setFlipY(boolean)}.
         */
        Flip {
            @Override
            public void applyToTextureKey(final String option, final TextureKey textureKey) {
                textureKey.setFlipY(true);
            }
        };

        public String getOptionValue(final String option) {
            return option.substring(name().length());
        }

        public void applyToTexture(final String option, final Texture texture) {
        }

        public void applyToTextureKey(final String option, final TextureKey textureKey) {
        }

        public static TextureOption getTextureOption(final String option) {
            for(final TextureOption textureOption : TextureOption.values()) {
                if (option.startsWith(textureOption.name())) {
                    return textureOption;
                }
            }

            return null;
        }
    }

    /**
     * Internal object used for holding a {@link com.jme3.material.plugins.J3MLoader.TextureOption} and it's value. Also
     * contains a couple of convenience methods for applying the TextureOption to either a TextureKey or a Texture.
     */
    private static class TextureOptionValue {

        private final TextureOption textureOption;
        private final String value;

        public TextureOptionValue(TextureOption textureOption, String value) {
            this.textureOption = textureOption;
            this.value = value;
        }

        public void applyToTextureKey(final TextureKey textureKey) {
            textureOption.applyToTextureKey(value, textureKey);
        }

        public void applyToTexture(final Texture texture) {
            textureOption.applyToTexture(value, texture);
        }
    }
}
