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

package com.jme3.scene.plugins;

import com.jme3.asset.*;
import com.jme3.material.Material;
import com.jme3.material.MaterialList;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.PlaceholderAssets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MTLLoader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(MTLLoader.class.getName());
    
    protected Scanner scan;
    protected MaterialList matList;
    //protected Material material;
    protected AssetManager assetManager;
    protected String folderName;
    protected AssetKey key;
    
    protected Texture diffuseMap, normalMap, specularMap, alphaMap;
    protected ColorRGBA ambient = new ColorRGBA();
    protected ColorRGBA diffuse = new ColorRGBA();
    protected ColorRGBA specular = new ColorRGBA();
    protected float shininess = 16;
    protected boolean shadeless;
    protected String matName;
    protected float alpha = 1;
    protected boolean transparent = false;
    protected boolean disallowTransparency = false;
    protected boolean disallowAmbient = false;
    protected boolean disallowSpecular = false;
    
    public void reset(){
        scan = null;
        matList = null;
//        material = null;
        
        resetMaterial();
    }

    protected ColorRGBA readColor(){
        ColorRGBA v = new ColorRGBA();
        v.set(scan.nextFloat(), scan.nextFloat(), scan.nextFloat(), 1.0f);
        return v;
    }

    protected String nextStatement(){
        scan.useDelimiter("\n");
        String result = scan.next();
        scan.useDelimiter("\\p{javaWhitespace}+");
        return result;
    }
    
    protected boolean skipLine(){
        try {
            scan.skip(".*\r{0,1}\n");
            return true;
        } catch (NoSuchElementException ex){
            // EOF
            return false;
        }
    }
    
    protected void resetMaterial(){
        ambient.set(ColorRGBA.DarkGray);
        diffuse.set(ColorRGBA.LightGray);
        specular.set(ColorRGBA.Black);
        shininess = 16;
        disallowTransparency = false;
        disallowAmbient = false;
        disallowSpecular = false;
        shadeless = false;
        transparent = false;
        matName = null;
        diffuseMap = null;
        specularMap = null;
        normalMap = null;
        alphaMap = null;
        alpha = 1;
    }
    
    protected void createMaterial(){
        Material material;
        
        if (alpha < 1f && transparent && !disallowTransparency){
            diffuse.a = alpha;
        }
        
        if (shadeless){
            material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            material.setColor("Color", diffuse.clone());
            material.setTexture("ColorMap", diffuseMap);
            // TODO: Add handling for alpha map?
        }else{
            material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            material.setBoolean("UseMaterialColors", true);
            material.setColor("Ambient",  ambient.clone());
            material.setColor("Diffuse",  diffuse.clone());
            material.setColor("Specular", specular.clone());
            material.setFloat("Shininess", shininess); // prevents "premature culling" bug
            
            if (diffuseMap != null)  material.setTexture("DiffuseMap", diffuseMap);
            if (specularMap != null) material.setTexture("SpecularMap", specularMap);
            if (normalMap != null)   material.setTexture("NormalMap", normalMap);
            if (alphaMap != null)    material.setTexture("AlphaMap", alphaMap);
        }
        
        if (transparent && !disallowTransparency){
            material.setTransparent(true);
            material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            material.getAdditionalRenderState().setAlphaTest(true);
            material.getAdditionalRenderState().setAlphaFallOff(0.01f);
        }
        
        matList.put(matName, material);
    }

    protected void startMaterial(String name){
        if (matName != null){
            // material is already in cache, generate it
            createMaterial();
        }
        
        // now, reset the params and set the name to start a new material
        resetMaterial();
        matName = name;
    }
    
    protected Texture loadTexture(String path){
        String[] split = path.trim().split("\\p{javaWhitespace}+");
        
        // will crash if path is an empty string
        path = split[split.length-1];
        
        String name = new File(path).getName();
        TextureKey texKey = new TextureKey(folderName + name);
        texKey.setGenerateMips(true);
        Texture texture;
        try {
            texture = assetManager.loadTexture(texKey);
            texture.setWrap(WrapMode.Repeat);
        } catch (AssetNotFoundException ex){
            logger.log(Level.WARNING, "Cannot locate {0} for material {1}", new Object[]{texKey, key});
            texture = new Texture2D(PlaceholderAssets.getPlaceholderImage());
        }
        return texture;
    }

    protected boolean readLine(){
        if (!scan.hasNext()){
            return false;
        }

        String cmd = scan.next().toLowerCase();
        if (cmd.startsWith("#")){
            // skip entire comment until next line
            return skipLine();
        }else if (cmd.equals("newmtl")){
            String name = scan.next();
            startMaterial(name);
        }else if (cmd.equals("ka")){
            ambient.set(readColor());
        }else if (cmd.equals("kd")){
            diffuse.set(readColor());
        }else if (cmd.equals("ks")){
            specular.set(readColor());
        }else if (cmd.equals("ns")){
            float shiny = scan.nextFloat();
            if (shiny >= 1){
                shininess = shiny; /* (128f / 1000f)*/
                if (specular.equals(ColorRGBA.Black)){
                    specular.set(ColorRGBA.White);
                }
            }else{
                // For some reason blender likes to export Ns 0 statements
                // Ignore Ns 0 instead of setting it
            }
            
        }else if (cmd.equals("d") || cmd.equals("tr")){
            alpha = scan.nextFloat();
            transparent = true;
        }else if (cmd.equals("map_ka")){
            // ignore it for now
            return skipLine();
        }else if (cmd.equals("map_kd")){
            String path = nextStatement();
            diffuseMap = loadTexture(path);
        }else if (cmd.equals("map_bump") || cmd.equals("bump")){
            if (normalMap == null){
                String path = nextStatement();
                normalMap = loadTexture(path);
            }
        }else if (cmd.equals("map_ks")){
            String path = nextStatement();
            specularMap = loadTexture(path);
            if (specularMap != null){
                // NOTE: since specular color is modulated with specmap
                // make sure we have it set
                if (specular.equals(ColorRGBA.Black)){
                    specular.set(ColorRGBA.White);
                }
            }
        }else if (cmd.equals("map_d")){
            String path = scan.next();
            alphaMap = loadTexture(path);
            transparent = true;
        }else if (cmd.equals("illum")){
            int mode = scan.nextInt();
            
            switch (mode){
                case 0:
                    // no lighting
                    shadeless = true;
                    disallowTransparency = true;
                    break;
                case 1:
                    disallowSpecular = true;
                    disallowTransparency = true;
                    break;
                case 2:
                case 3:
                case 5:
                case 8:
                    disallowTransparency = true;
                    break;
                case 4:
                case 6:
                case 7:
                case 9:
                    // Enable transparency
                    // Works best if diffuse map has an alpha channel
                    transparent = true;
                    break;
            }
        }else if (cmd.equals("ke") || cmd.equals("ni")){
            // Ni: index of refraction - unsupported in jME
            // Ke: emission color
            return skipLine();
        }else{
            logger.log(Level.WARNING, "Unknown statement in MTL! {0}", cmd);
            return skipLine();
        }
        
        return true;
    }

    @SuppressWarnings("empty-statement")
    public Object load(AssetInfo info) throws IOException{
        reset();
        
        this.key = info.getKey();
        this.assetManager = info.getManager();
        folderName = info.getKey().getFolder();
        matList = new MaterialList();

        InputStream in = null;
        try {
            in = info.openStream();
            scan = new Scanner(in);
            scan.useLocale(Locale.US);
            
            while (readLine());
        } finally {
            if (in != null){
                in.close();
            }
        }
        
        if (matName != null){
            // still have a material in the vars
            createMaterial();
            resetMaterial();
        }
        
        MaterialList list = matList;

        

        return list;
    }
}
