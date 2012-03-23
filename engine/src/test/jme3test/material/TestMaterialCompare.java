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
package jme3test.material;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestMaterialCompare extends SimpleApplication {

    public static void main(String[] args) {
        TestMaterialCompare app = new TestMaterialCompare();
        app.start();
    }

    @Override
    public void simpleInitApp() {
      
        Logger.getLogger("com.jme3").setLevel(Level.SEVERE);
        
        //clonned mats
        Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setName("mat1");
        mat1.setColor("Color", ColorRGBA.Blue);

        Material mat2 = mat1.clone();
        mat2.setName("mat2");
        testMats(mat1,mat2,true);

        //clonned mat with different additional render state
        Material mat3 = mat1.clone();;
        mat3.setName("mat3");    
        mat3.getAdditionalRenderState().setBlendMode(BlendMode.ModulateX2);        
        testMats(mat1,mat3,false);
        
        //two separately loaded materials
        Material mat4 = assetManager.loadMaterial("Models/Sign Post/Sign Post.j3m");
        mat4.setName("mat4");    
        Material mat5 = assetManager.loadMaterial("Models/Sign Post/Sign Post.j3m");
        mat5.setName("mat5");    
        testMats(mat4,mat5,true);       
        
        //two materials created the same way
        Material mat6 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat6.setName("mat6");
        mat6.setColor("Color", ColorRGBA.Blue);
        testMats(mat1,mat6,true);
        
        //changing a material param
        mat6.setColor("Color", ColorRGBA.Green);
        testMats(mat1,mat6,false);
                
        
    }

    private void testMats(Material mat1, Material mat2, boolean expected) {
        if (mat2.isEqual(mat1)) {
            System.out.print(mat1.getName() + " equals " + mat2.getName());
            if(expected){
                System.out.println(" success");
            }else{
                System.out.println(" fail");
            }
        }else{
            System.out.print(mat1.getName() + " is not equal " + mat2.getName());
            if(!expected){
                System.out.println(" success");
            }else{
                System.out.println(" fail");
            }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
    }
}
