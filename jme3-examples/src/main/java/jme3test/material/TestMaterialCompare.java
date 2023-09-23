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
package jme3test.material;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Texture;

public class TestMaterialCompare {

    public static void main(String[] args) {
        AssetManager assetManager = JmeSystem.newAssetManager(
                TestMaterialCompare.class.getResource("/com/jme3/asset/Desktop.cfg"));
        
        // Cloned materials
        Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setName("mat1");
        mat1.setColor("Color", ColorRGBA.Blue);

        Material mat2 = mat1.clone();
        mat2.setName("mat2");
        testEquality(mat1, mat2, true);

        // Cloned material with different render states
        Material mat3 = mat1.clone();
        mat3.setName("mat3");
        mat3.getAdditionalRenderState().setBlendMode(BlendMode.ModulateX2);
        testEquality(mat1, mat3, false);

        // Two separately loaded materials
        Material mat4 = assetManager.loadMaterial("Models/Sign Post/Sign Post.j3m");
        mat4.setName("mat4");
        Material mat5 = assetManager.loadMaterial("Models/Sign Post/Sign Post.j3m");
        mat5.setName("mat5");
        testEquality(mat4, mat5, true);
        
        // Comparing same textures
        TextureKey originalKey = (TextureKey) mat4.getTextureParam("DiffuseMap").getTextureValue().getKey();
        TextureKey tex1key = new TextureKey("Models/Sign Post/Sign Post.jpg", false);
        tex1key.setGenerateMips(true);
        
        // Texture keys from the original and the loaded texture
        // must be identical, otherwise the resultant textures not identical
        // and thus materials are not identical!
        if (!originalKey.equals(tex1key)){
            System.out.println("TEXTURE KEYS ARE NOT EQUAL");
        }
        
        Texture tex1 = assetManager.loadTexture(tex1key);
        mat4.setTexture("DiffuseMap", tex1);
        testEquality(mat4, mat5, true);
        
        // Change some stuff on the texture and compare, materials no longer equal
        tex1.setWrap(Texture.WrapMode.MirroredRepeat);
        testEquality(mat4, mat5, false);
        
        // Comparing different textures
        Texture tex2 = assetManager.loadTexture("Interface/Logo/Monkey.jpg");
        mat4.setTexture("DiffuseMap", tex2);
        testEquality(mat4, mat5, false);

        // Two materials created the same way
        Material mat6 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat6.setName("mat6");
        mat6.setColor("Color", ColorRGBA.Blue);
        testEquality(mat1, mat6, true);

        // Changing a material param
        mat6.setColor("Color", ColorRGBA.Green);
        testEquality(mat1, mat6, false);
    }

    private static void testEquality(Material mat1, Material mat2, boolean expected) {
        if (mat2.contentEquals(mat1)) {
            System.out.print(mat1.getName() + " == " + mat2.getName());
            if (expected) {
                System.out.println(" EQUAL OK");
            } else {
                System.out.println(" EQUAL FAIL!");
            }
        } else {
            System.out.print(mat1.getName() + " != " + mat2.getName());
            if (!expected) {
                System.out.println(" EQUAL OK");
            } else {
                System.out.println(" EQUAL FAIL!");
            }
        }
        if (mat2.hashCode() == mat1.hashCode()){
            System.out.print(mat1.getName() + " == " + mat2.getName());
            if (expected) {
                System.out.println(" HASH OK");
            } else {
                System.out.println(" HASH FAIL!");
            }
        } else {
            System.out.print(mat1.getName() + " != " + mat2.getName());
            if (!expected) {
                System.out.println(" HASH OK");
            } else {
                System.out.println(" HASH FAIL!");
            }
        }
    }
}
