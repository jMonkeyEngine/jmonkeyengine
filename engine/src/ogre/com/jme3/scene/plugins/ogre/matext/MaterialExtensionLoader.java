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

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.MaterialList;
import com.jme3.scene.plugins.ogre.MaterialLoader;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.PlaceholderAssets;
import com.jme3.util.blockparser.Statement;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used internally by {@link MaterialLoader}
 */
public class MaterialExtensionLoader {

    private static final Logger logger = Logger.getLogger(MaterialExtensionLoader.class.getName());

    private AssetKey key;
    private AssetManager assetManager;
    private MaterialList list;
    private MaterialExtensionSet matExts;
    private MaterialExtension matExt;
    private String matName;
    private Material material;

    
    private void readExtendingMaterialStatement(Statement statement) throws IOException {
        if (statement.getLine().startsWith("set_texture_alias")){
            String[] split = statement.getLine().split(" ", 3);
            String aliasName = split[1];
            String texturePath = split[2];

            String jmeParamName = matExt.getTextureMapping(aliasName);

            TextureKey texKey = new TextureKey(texturePath, false);
            texKey.setGenerateMips(true);
            texKey.setAsCube(false);
            Texture tex;
            
            try {
                tex = assetManager.loadTexture(texKey);
                tex.setWrap(WrapMode.Repeat);
            } catch (AssetNotFoundException ex){
                logger.log(Level.WARNING, "Cannot locate {0} for material {1}", new Object[]{texKey, key});
                tex = new Texture2D( PlaceholderAssets.getPlaceholderImage() );
            }
            
            material.setTexture(jmeParamName, tex);
        }
    }

    private Material readExtendingMaterial(Statement statement) throws IOException{
        String[] split = statement.getLine().split(" ", 2);
        String[] subsplit = split[1].split(":");
        matName = subsplit[0].trim();
        String extendedMat = subsplit[1].trim();

        matExt = matExts.getMaterialExtension(extendedMat);
        if (matExt == null){
            logger.log(Level.WARNING, "Cannot find MaterialExtension for: {0}. Ignoring material.", extendedMat);
            matExt = null;
            return null;
        }

        material = new Material(assetManager, matExt.getJmeMatDefName());
        for (Statement extMatStat : statement.getContents()){
            readExtendingMaterialStatement(extMatStat);
        }
        return material;
    }

    public MaterialList load(AssetManager assetManager, AssetKey key, MaterialExtensionSet matExts,
            List<Statement> statements) throws IOException{
        this.assetManager = assetManager;
        this.matExts = matExts;
        this.key = key;
        
        list = new MaterialList();
        
        for (Statement statement : statements){
            if (statement.getLine().startsWith("import")){
                // ignore
                continue;
            }else if (statement.getLine().startsWith("material")){
                Material material = readExtendingMaterial(statement);
                list.put(matName, material);
                List<String> matAliases = matExts.getNameMappings(matName);
                if(matAliases != null){
                    for (String string : matAliases) {
                        list.put(string, material);
                    }
                }
            }
        }
        return list;
    }
}
