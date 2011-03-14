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

package jme3test.texture;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

/**
 * Compares RGB8, DXT5-YCoCg and DXT1 with a skybox texture
 * @author Kirill
 */
public class TestYCoCgDds extends SimpleApplication {

    private Quad quadMesh;

    public static void main(String[] args){
        TestYCoCgDds app = new TestYCoCgDds();
        app.start();
    }

    public Geometry createQuad(float side, String texName, boolean ycocg){
        Geometry quad = new Geometry("Textured Quad", quadMesh);

        Texture tex = assetManager.loadTexture(texName);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/SimpleTextured.j3md");
        mat.setTexture("ColorMap", tex);
        if (ycocg)
            mat.setBoolean("YCoCg", true);

        quad.setMaterial(mat);

        float aspect = tex.getImage().getWidth() / (float) tex.getImage().getHeight();
        quad.setLocalScale(new Vector3f(aspect * 5, 5, 1));
        quad.center();
        quad.setLocalTranslation(quad.getLocalTranslation().x + quad.getLocalScale().x  * side, 0, 0);

        return quad;
    }

    public void simpleInitApp() {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText txt = new BitmapText(font, false);
        txt.setText("Left: Original, Middle: DXT5-YCoCg, Right: DXT1");
        txt.setLocalTranslation(0, txt.getLineHeight() * 2, 0);
        guiNode.attachChild(txt);

        // create a simple plane/quad
        quadMesh = new Quad(1, 1);
        quadMesh.updateGeometry(1, 1, false);

        rootNode.attachChild(createQuad(-1f, "Textures/Sky/Night/Night.png", false));
        rootNode.attachChild(createQuad(0,   "Textures/Sky/Night/Night_ycc.dds", true));
        rootNode.attachChild(createQuad(1f,  "Textures/Sky/Night/Night_dxt1.dds", false));
    }

}
