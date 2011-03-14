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

package com.jme3.scene.plugins.ogre.matext;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.MaterialList;
import com.jme3.scene.plugins.ogre.MaterialLoader;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used internally by {@link MaterialLoader}
 */
public class MaterialExtensionLoader {

    private static final Logger logger = Logger.getLogger(MaterialExtensionLoader.class.getName());

    private AssetManager assetManager;
    private Scanner scan;
    private MaterialList list;
    private MaterialExtensionSet matExts;
    private MaterialExtension matExt;
    private String matName;
    private Material material;

    private String readString(String end){
        scan.useDelimiter(end);
        String str = scan.next();
        scan.useDelimiter("\\p{javaWhitespace}+");
        return str.trim();
    }

    private boolean readExtendingMaterialStatement() throws IOException{
        if (scan.hasNext("set_texture_alias")){
            scan.next(); // skip "set_texture_alias"

            String aliasName = scan.next();
            String texturePath = readString("\n");

            String jmeParamName = matExt.getTextureMapping(aliasName);

            Texture tex = assetManager.loadTexture(texturePath);
            if (tex == null)
                throw new IOException("Cannot load texture: " + texturePath);

            material.setTexture(jmeParamName, tex);

            return true;
        }else{
            return false;
        }
    }

    private Material readExtendingMaterial() throws IOException{
        scan.next(); // skip "material"
        matName = readString(":").trim();
        scan.next();
        String extendedMat = readString("\\{").trim();
        scan.next();

        matExt = matExts.getMaterialExtension(extendedMat);
        if (matExt == null){
            logger.log(Level.WARNING, "Cannot find MaterialExtension for: {0}. Ignoring material.", extendedMat);
            readString("\\}");
            scan.next();
            matExt = null;
            return null;
        }

        material = new Material(assetManager, matExt.getJmeMatDefName());

        material.setFloat("Shininess", 16f);

        while (!scan.hasNext("\\}")){
            readExtendingMaterialStatement();
        }

        return material;
    }

    public MaterialList load(AssetManager assetManager, MaterialExtensionSet matExts, Scanner scan) throws IOException{
        this.assetManager = assetManager;
        this.matExts = matExts;
        this.scan = scan;
        
        list = new MaterialList();
        
        if (scan.hasNext("import")){
            scan.nextLine(); // skip this line
        }

        toploop: while (scan.hasNext()){
            while (!scan.hasNext("material")){
                if (!scan.hasNext())
                    break toploop;
                
                scan.next();
            }

            Material material = readExtendingMaterial();
            list.put(matName, material);
        }

        return list;
    }
}
