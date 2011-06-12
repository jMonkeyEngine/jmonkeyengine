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

package jme3test.conversion;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import jme3tools.converters.MipMapGenerator;

public class TestMipMapGen extends SimpleApplication {

    public static void main(String[] args){
        TestMipMapGen app = new TestMipMapGen();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        BitmapText txt = guiFont.createLabel("Left: HW Mips");
        txt.setLocalTranslation(0, settings.getHeight() - txt.getLineHeight() * 4, 0);
        guiNode.attachChild(txt);

        txt = guiFont.createLabel("Right: AWT Mips");
        txt.setLocalTranslation(0, settings.getHeight() - txt.getLineHeight() * 3, 0);
        guiNode.attachChild(txt);

        // create a simple plane/quad
        Quad quadMesh = new Quad(1, 1);
        quadMesh.updateGeometry(1, 1, false);
        quadMesh.updateBound();

        Geometry quad1 = new Geometry("Textured Quad", quadMesh);
        Geometry quad2 = new Geometry("Textured Quad 2", quadMesh);

        Texture tex = assetManager.loadTexture("Interface/Logo/Monkey.png");
        tex.setMinFilter(Texture.MinFilter.Trilinear);

        Texture texCustomMip = tex.clone();
        Image imageCustomMip = texCustomMip.getImage().clone();
        MipMapGenerator.generateMipMaps(imageCustomMip);
        texCustomMip.setImage(imageCustomMip);

        Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setTexture("ColorMap", tex);

        Material mat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat2.setTexture("ColorMap", texCustomMip);

        quad1.setMaterial(mat1);
//        quad1.setLocalTranslation(1, 0, 0);

        quad2.setMaterial(mat2);
        quad2.setLocalTranslation(1, 0, 0);

        rootNode.attachChild(quad1);
        rootNode.attachChild(quad2);
    }

}
