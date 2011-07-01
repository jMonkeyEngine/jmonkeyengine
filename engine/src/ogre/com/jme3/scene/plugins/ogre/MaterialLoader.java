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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.MaterialList;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.plugins.ogre.matext.MaterialExtensionLoader;
import com.jme3.scene.plugins.ogre.matext.MaterialExtensionSet;
import com.jme3.scene.plugins.ogre.matext.OgreMaterialKey;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MaterialLoader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(MaterialLoader.class.getName());

    private String folderName;
    private AssetManager assetManager;
    private Scanner scan;
    private ColorRGBA ambient, diffuse, specular, emissive;
    private Texture texture;
    private String texName;
    private String matName;
    private float shinines;
    private boolean vcolor = false;
    private boolean blend = false;
    private boolean twoSide = false;
    private boolean noLight = false;
    private boolean readTexUnit = false;

    private String readString(String end){
        scan.useDelimiter(end);
        String str = scan.next();
        scan.useDelimiter("\\p{javaWhitespace}+");
        return str.trim();
    }
    
    private ColorRGBA readColor(){
        ColorRGBA color = new ColorRGBA();
        color.r = scan.nextFloat();
        color.g = scan.nextFloat();
        color.b = scan.nextFloat();
        if (scan.hasNextFloat()){
            color.a = scan.nextFloat();
        }
        return color;
    }

    private void readTextureImage(){
        // texture image def
        String ln = scan.nextLine();
        String path = null;

        // find extension
        int extStart = ln.lastIndexOf(".");
        for (int i = extStart; i < ln.length(); i++){
            char c = ln.charAt(i);
            if (Character.isWhitespace(c)){
                // extension ends here
                path = ln.substring(0, i).trim();
                ln   = ln.substring(i+1).trim();
                break;
            }
        }
        if (path == null){
            path = ln.trim();
            ln = "";
        }

        Scanner lnScan = new Scanner(ln);
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

        TextureKey key = new TextureKey(folderName + path, false);
        key.setGenerateMips(genMips);
        key.setAsCube(cubic);

        Texture loadedTexture = assetManager.loadTexture(key);
        if (loadedTexture == null){
            ByteBuffer tempData = BufferUtils.createByteBuffer(3);
            tempData.put((byte)0xFF).put((byte)0x00).put((byte)0x00);
            texture = new Texture2D(new Image(Format.RGB8, 1,1,tempData));
            logger.log(Level.WARNING, "Using RED texture instead of {0}", path);
        }else{
            texture.setImage(loadedTexture.getImage());
            texture.setMinFilter(loadedTexture.getMinFilter());
            texture.setKey(loadedTexture.getKey());

            // XXX: Is this really neccessary?
            texture.setWrap(WrapMode.Repeat);
            if (texName != null){
                texture.setName(texName);
                texName = null;
            }else{
                texture.setName(key.getName());
            }
        }
        
        
    }

    private void readTextureUnitStatement(){
        String keyword = scan.next();
        if (keyword.equals("texture")){
            readTextureImage();
        }else if (keyword.equals("tex_address_mode")){
            String mode = scan.next();
            if (mode.equals("wrap")){
                texture.setWrap(WrapMode.Repeat);
            }else if (mode.equals("clamp")){
                texture.setWrap(WrapMode.Clamp);
            }else if (mode.equals("mirror")){
                texture.setWrap(WrapMode.MirroredRepeat);
            }else if (mode.equals("border")){
                texture.setWrap(WrapMode.BorderClamp);
            }
        }else if (keyword.equals("filtering")){
            // ignored.. only anisotropy is considered
            readString("\n");
        }else if (keyword.equals("max_anisotropy")){
            int amount = scan.nextInt();
            texture.setAnisotropicFilter(amount);
        }else{
            logger.log(Level.WARNING, "Unsupported texture_unit directive: {0}", keyword);
            readString("\n");
        }
    }

    private void readTextureUnit(boolean skipIt){
        // name is optional
        if (!scan.hasNext("\\{")){
            texName = readString("\\{");
        }else{
            texName = null;
        }
        scan.next(); // skip "{"

        if (!skipIt){
            texture = new Texture2D();
        }

        while (!scan.hasNext("\\}")){
            if (skipIt){
                readString("\n");
            }else{
                readTextureUnitStatement();
            }
        }
        scan.next(); // skip "}"
    }

    private void readPassStatement(){
        // read until newline
        String keyword = scan.next();
        if (keyword.equals(""))
            return;

        if (keyword.equals("diffuse")){
            if (scan.hasNext("vertexcolour")){
                // use vertex colors
                diffuse = ColorRGBA.White;
                vcolor = true;
                scan.next(); // skip it
            }else{
                diffuse = readColor();
            }
        }else if(keyword.equals("ambient")) {
           if (scan.hasNext("vertexcolour")){
                // use vertex colors
               ambient = ColorRGBA.White;
               scan.next(); // skip it
            }else{
               ambient = readColor();
            }
        }else if (keyword.equals("emissive")){
            emissive = readColor();
        }else if (keyword.equals("specular")){
            specular = new ColorRGBA();
            specular.r = scan.nextFloat();
            specular.g = scan.nextFloat();
            specular.b = scan.nextFloat();
            float unknown = scan.nextFloat();
            if (scan.hasNextFloat()){
                // using 5 float values
                specular.a = unknown;
                shinines = scan.nextFloat();
            }else{
                // using 4 float values
                specular.a = 1f;
                shinines = unknown;
            }
        }else if (keyword.equals("texture_unit")){
            readTextureUnit(readTexUnit);
            // After reading the first texunit, ignore the rest
            if (!readTexUnit) {
                readTexUnit = true;
            }
        }else if (keyword.equals("scene_blend")){
            if (scan.hasNextInt()){
                readString("\n"); // blender2ogre workaround
                return; 
            }
            String mode = scan.next();
            if (mode.equals("alpha_blend")){
                blend = true;
            }else{
                // skip the rest
                readString("\n");
            }
        }else if (keyword.equals("cull_hardware")){
            String mode = scan.next();
            if (mode.equals("none")){
                twoSide = true;
            }
        }else if (keyword.equals("cull_software")){
            // ignore
            scan.next();
        }else if (keyword.equals("lighting")){
            String isOn = scan.next();
            if (isOn.equals("on")){
                noLight = false;
            }else if (isOn.equals("off")){
                noLight = true;
            }
        }else{
            logger.log(Level.WARNING, "Unsupported pass directive: {0}", keyword);
            readString("\n");
        }
    }

    private void readPass(){
        scan.next(); // skip "pass"
        // name is optional
        String name;
        if (scan.hasNext("\\{")){
            // no name
            name = null;
        }else{
            name = readString("\\{");
        }
        scan.next(); // skip "{"
        
        // Has not yet read a tex unit for this pass
        readTexUnit = false;
        
        while (!scan.hasNext("\\}")){
            readPassStatement();
        }
        scan.next(); // skip "}"
    }

    private void readTechnique(){
        scan.next(); // skip "technique"
        // name is optional
        String name;
        if (scan.hasNext("\\{")){
            // no name
            name = null;
        }else{
            name = readString("\\{");
        }
        scan.next(); // skip "{"
        while (!scan.hasNext("\\}")){
            readPass();
        }
        scan.next();
    }

    private boolean readMaterialStatement(){
        if (scan.hasNext("technique")){
            readTechnique();
            return true;
        }else if (scan.hasNext("receive_shadows")){
            // skip "receive_shadows"
            scan.next();
            String isOn = scan.next();
            if (isOn != null && isOn.equals("true")){

            }
            return true;
        }else{
            return false;
        }
    }

    @SuppressWarnings("empty-statement")
    private void readMaterial(){
        scan.next(); // skip "material"
        // read name
        matName = readString("\\{");
        scan.next(); // skip "{"
        while (!scan.hasNext("\\}")){
            readMaterialStatement();
        }
        scan.next();
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
            if (twoSide)
                rs.setFaceCullMode(RenderState.FaceCullMode.Off);
//            rs.setDepthWrite(false);
            mat.setTransparent(true);
            if (!noLight)
                mat.setBoolean("UseAlpha", true);
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

            if (texture != null)
                mat.setTexture("DiffuseMap", texture);

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

            if (texture != null) {
                mat.setTexture("ColorMap", texture);
            }

            if(diffuse != null){
                mat.setColor("Color", diffuse);
            }
            
            if (emissive != null){
                mat.setColor("GlowColor", emissive);
            }
        }

        noLight = false;
        texture = null;
        diffuse = null;
        specular = null;
        texture = null;
        shinines = 0f;
        vcolor = false;
        blend = false;
        return mat;
    }

    public Object load(AssetInfo info) throws IOException {
        folderName = info.getKey().getFolder();
        assetManager = info.getManager();

        MaterialList list;
        
        scan = new Scanner(info.openStream());
        scan.useLocale(Locale.US);
        if (scan.hasNext("import")){
            MaterialExtensionSet matExts = null;
            if (info.getKey() instanceof OgreMaterialKey){
                 matExts = ((OgreMaterialKey)info.getKey()).getMaterialExtensionSet();
            }
            
            if (matExts == null){
                throw new IOException("Must specify MaterialExtensionSet when loading\n"+
                                      "Ogre3D materials with extended materials");
            }

            list = new MaterialExtensionLoader().load(assetManager, matExts, scan);
        }else{
            list = new MaterialList();
            while (scan.hasNext("material")){
                readMaterial();
                Material mat = compileMaterial();
                list.put(matName, mat);
            }
        }
        scan.close();
        return list;
    }

}
