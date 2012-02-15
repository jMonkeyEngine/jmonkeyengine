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

package com.jme3.scene.plugins.ogre;

import com.jme3.asset.*;
import com.jme3.material.Material;
import com.jme3.material.MaterialList;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.plugins.ogre.matext.MaterialExtensionLoader;
import com.jme3.scene.plugins.ogre.matext.MaterialExtensionSet;
import com.jme3.scene.plugins.ogre.matext.OgreMaterialKey;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.PlaceholderAssets;
import com.jme3.util.blockparser.BlockLanguageParser;
import com.jme3.util.blockparser.Statement;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MaterialLoader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(MaterialLoader.class.getName());

    private String folderName;
    private AssetManager assetManager;
    private ColorRGBA ambient, diffuse, specular, emissive;
    private Texture[] textures = new Texture[4];
    private String texName;
    private String matName;
    private float shinines;
    private boolean vcolor = false;
    private boolean blend = false;
    private boolean twoSide = false;
    private boolean noLight = false;
    private boolean separateTexCoord = false;
    private int texUnit = 0;

    private ColorRGBA readColor(String content){
        String[] split = content.split("\\s");
        
        ColorRGBA color = new ColorRGBA();
        color.r = Float.parseFloat(split[0]);
        color.g = Float.parseFloat(split[1]);
        color.b = Float.parseFloat(split[2]);
        if (split.length >= 4){
            color.a = Float.parseFloat(split[3]);
        }
        return color;
    }

    private void readTextureImage(String content){
        // texture image def
        String path = null;

        // find extension
        int extStart = content.lastIndexOf(".");
        for (int i = extStart; i < content.length(); i++){
            char c = content.charAt(i);
            if (Character.isWhitespace(c)){
                // extension ends here
                path = content.substring(0, i).trim();
                content   = content.substring(i+1).trim();
                break;
            }
        }
        if (path == null){
            path = content.trim();
            content = "";
        }

        Scanner lnScan = new Scanner(content);
        String mips = null;
        String type = null;
        if (lnScan.hasNext()){
            // more params
            type = lnScan.next();
//            if (!lnScan.hasNext("\n") && lnScan.hasNext()){
//                mips = lnScan.next();
//                if (lnScan.hasNext()){
                    // even more params..
                    // will have to ignore
//                }
//            }
        }

        boolean genMips = true;
        boolean cubic = false;
        if (type != null && type.equals("0"))
            genMips = false;

        if (type != null && type.equals("cubic")){
            cubic = true;
        }

        TextureKey texKey = new TextureKey(folderName + path, false);
        texKey.setGenerateMips(genMips);
        texKey.setAsCube(cubic);

        try {
            Texture loadedTexture = assetManager.loadTexture(texKey);
            
            textures[texUnit].setImage(loadedTexture.getImage());
            textures[texUnit].setMinFilter(loadedTexture.getMinFilter());
            textures[texUnit].setKey(loadedTexture.getKey());

            // XXX: Is this really neccessary?
            textures[texUnit].setWrap(WrapMode.Repeat);
            if (texName != null){
                textures[texUnit].setName(texName);
                texName = null;
            }else{
                textures[texUnit].setName(texKey.getName());
            }
        } catch (AssetNotFoundException ex){
            logger.log(Level.WARNING, "Cannot locate {0} for material {1}", new Object[]{texKey, matName});
            textures[texUnit].setImage(PlaceholderAssets.getPlaceholderImage());
        }
    }

    private void readTextureUnitStatement(Statement statement){
        String[] split = statement.getLine().split(" ", 2);
        String keyword = split[0];
        if (keyword.equals("texture")){
            readTextureImage(split[1]);
        }else if (keyword.equals("tex_address_mode")){
            String mode = split[1];
            if (mode.equals("wrap")){
                textures[texUnit].setWrap(WrapMode.Repeat);
            }else if (mode.equals("clamp")){
                textures[texUnit].setWrap(WrapMode.Clamp);
            }else if (mode.equals("mirror")){
                textures[texUnit].setWrap(WrapMode.MirroredRepeat);
            }else if (mode.equals("border")){
                textures[texUnit].setWrap(WrapMode.BorderClamp);
            }
        }else if (keyword.equals("filtering")){
            // ignored.. only anisotropy is considered
        }else if (keyword.equals("tex_coord_set")){
            int texCoord = Integer.parseInt(split[1]);
            if (texCoord == 1){
                separateTexCoord = true;
            }
        }else if (keyword.equals("max_anisotropy")){
            int amount = Integer.parseInt(split[1]);
            textures[texUnit].setAnisotropicFilter(amount);
        }else{
            logger.log(Level.WARNING, "Unsupported texture_unit directive: {0}", keyword);
        }
    }

    private void readTextureUnit(Statement statement){
        String[] split = statement.getLine().split(" ", 2); 
        // name is optional
        if (split.length == 2){
            texName = split[1];
        }else{
            texName = null;
        }

        textures[texUnit] = new Texture2D();
        for (Statement texUnitStat : statement.getContents()){
            readTextureUnitStatement(texUnitStat);
        }
        if (textures[texUnit].getImage() != null){
            texUnit++;
        }else{
            // no image was loaded, ignore
            textures[texUnit] = null;
        }
    }

    private void readPassStatement(Statement statement){
        // read until newline
        String[] split = statement.getLine().split(" ", 2);
        String keyword = split[0];
        if (keyword.equals("diffuse")){
            if (split[1].equals("vertexcolour")){
                // use vertex colors
                diffuse = ColorRGBA.White;
                vcolor = true;
            }else{
                diffuse = readColor(split[1]);
            }
        }else if(keyword.equals("ambient")) {
           if (split[1].equals("vertexcolour")){
                // use vertex colors
               ambient = ColorRGBA.White;
            }else{
               ambient = readColor(split[1]);
            }
        }else if (keyword.equals("emissive")){
            emissive = readColor(split[1]);
        }else if (keyword.equals("specular")){
            String[] subsplit = split[1].split("\\s");
            specular = new ColorRGBA();
            specular.r = Float.parseFloat(subsplit[0]);
            specular.g = Float.parseFloat(subsplit[1]);
            specular.b = Float.parseFloat(subsplit[2]);
            float unknown = Float.parseFloat(subsplit[3]);
            if (subsplit.length >= 5){
                // using 5 float values
                specular.a = unknown;
                shinines = Float.parseFloat(subsplit[4]);
            }else{
                // using 4 float values
                specular.a = 1f;
                shinines = unknown;
            }
        }else if (keyword.equals("texture_unit")){
            readTextureUnit(statement);
        }else if (keyword.equals("scene_blend")){
            String mode = split[1];
            if (mode.equals("alpha_blend")){
                blend = true;
            }
        }else if (keyword.equals("cull_hardware")){
            String mode = split[1];
            if (mode.equals("none")){
                twoSide = true;
            }
        }else if (keyword.equals("cull_software")){
            // ignore
        }else if (keyword.equals("lighting")){
            String isOn = split[1];
            if (isOn.equals("on")){
                noLight = false;
            }else if (isOn.equals("off")){
                noLight = true;
            }
        }else{
            logger.log(Level.WARNING, "Unsupported pass directive: {0}", keyword);
        }
    }

    private void readPass(Statement statement){
        String name;
        String[] split = statement.getLine().split(" ", 2);
        if (split.length == 1){
            // no name
            name = null;
        }else{
            name = split[1];
        }
        
        for (Statement passStat : statement.getContents()){
            readPassStatement(passStat);
        }
        
        texUnit = 0;
    }

    private void readTechnique(Statement statement){
        String[] split = statement.getLine().split(" ", 2);
        String name;
        if (split.length == 1){
            // no name
            name = null;
        }else{
            name = split[1];
        }
        for (Statement techStat : statement.getContents()){
            readPass(techStat);
        }
    }

    private void readMaterialStatement(Statement statement){
        if (statement.getLine().startsWith("technique")){
            readTechnique(statement);
        }else if (statement.getLine().startsWith("receive_shadows")){
            String isOn = statement.getLine().split("\\s")[1];
            if (isOn != null && isOn.equals("true")){
            }
        }
    }

    @SuppressWarnings("empty-statement")
    private void readMaterial(Statement statement){
        for (Statement materialStat : statement.getContents()){
            readMaterialStatement(materialStat);
        }
    }

    private Material compileMaterial(){
        Material mat;
        if (noLight){
           mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        }else{
           mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        }
        if (blend){
            RenderState rs = mat.getAdditionalRenderState();
            rs.setAlphaTest(true);
            rs.setAlphaFallOff(0.01f);
            rs.setBlendMode(RenderState.BlendMode.Alpha);
            
            if (twoSide){
                rs.setFaceCullMode(RenderState.FaceCullMode.Off);
            }
            
//            rs.setDepthWrite(false);
            mat.setTransparent(true);
            if (!noLight){
                mat.setBoolean("UseAlpha", true);
            }
        }else{
            if (twoSide){
                RenderState rs = mat.getAdditionalRenderState();
                rs.setFaceCullMode(RenderState.FaceCullMode.Off);
            }
        }

        if (!noLight){
            if (shinines > 0f) {
                mat.setFloat("Shininess", shinines);
            } else {
                mat.setFloat("Shininess", 16f); // set shininess to some value anyway..
            }
            
            if (vcolor)
                mat.setBoolean("UseVertexColor", true);

            if (textures[0] != null)
                mat.setTexture("DiffuseMap", textures[0]);

            mat.setBoolean("UseMaterialColors", true);
            if(diffuse != null){
                mat.setColor("Diffuse",  diffuse);
            }else{
                mat.setColor("Diffuse", ColorRGBA.White);
            }

            if(ambient != null){
                mat.setColor("Ambient",  ambient);
            }else{
                mat.setColor("Ambient", ColorRGBA.DarkGray);
            }

            if(specular != null){
                mat.setColor("Specular", specular);
            }else{
                mat.setColor("Specular", ColorRGBA.Black);
            }
            
            if (emissive != null){
                mat.setColor("GlowColor", emissive);
            }
        }else{
            if (vcolor) {
                mat.setBoolean("VertexColor", true);
            }

            if (textures[0] != null && textures[1] == null){
                if (separateTexCoord){
                    mat.setTexture("LightMap", textures[0]);
                    mat.setBoolean("SeparateTexCoord", true);
                }else{
                    mat.setTexture("ColorMap", textures[0]);
                }
            }else if (textures[1] != null){
                mat.setTexture("ColorMap", textures[0]);
                mat.setTexture("LightMap", textures[1]);
                if (separateTexCoord){
                    mat.setBoolean("SeparateTexCoord", true);
                }
            }
                 
            if(diffuse != null){
                mat.setColor("Color", diffuse);
            }
            
            if (emissive != null){
                mat.setColor("GlowColor", emissive);
            }
        }

        noLight = false;
        Arrays.fill(textures, null);
        diffuse = null;
        specular = null;
        shinines = 0f;
        vcolor = false;
        blend = false;
        texUnit = 0;
        separateTexCoord = false;
        return mat;
    }
    
    private MaterialList load(AssetManager assetManager, AssetKey key, InputStream in) throws IOException{
        folderName = key.getFolder();
        this.assetManager = assetManager;
        
        MaterialList list = null;
        List<Statement> statements = BlockLanguageParser.parse(in);
        
        for (Statement statement : statements){
            if (statement.getLine().startsWith("import")){
                MaterialExtensionSet matExts = null;
                if (key instanceof OgreMaterialKey){
                     matExts = ((OgreMaterialKey)key).getMaterialExtensionSet();
                }

                if (matExts == null){
                    throw new IOException("Must specify MaterialExtensionSet when loading\n"+
                                          "Ogre3D materials with extended materials");
                }

                list = new MaterialExtensionLoader().load(assetManager, key, matExts, statements);
                break;
            }else if (statement.getLine().startsWith("material")){
                if (list == null){
                    list = new MaterialList();
                }
                String[] split = statement.getLine().split(" ", 2);
                matName = split[1].trim();
                readMaterial(statement);
                Material mat = compileMaterial();
                list.put(matName, mat);
            }
        }
        
        return list;
    }

    public Object load(AssetInfo info) throws IOException {
        InputStream in = null;
        try {
            in = info.openStream();
            return load(info.getManager(), info.getKey(), in);
        } finally {
            if (in != null){
                in.close();
            }
        }
    }

}
