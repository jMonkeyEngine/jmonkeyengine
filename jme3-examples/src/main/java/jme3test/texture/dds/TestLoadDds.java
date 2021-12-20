/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package jme3test.texture.dds;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.DDSLoader;
import com.jme3.ui.Picture;

/**
 * Test various supported BC* textures in DDS file format
 * 
 * @author Toni Helenius
 */
public class TestLoadDds extends SimpleApplication {

    public static void main(String[] args) {
        TestLoadDds app = new TestLoadDds();
        //app.setShowSettings(false);
        app.start();
    }
   
    @Override
    public void simpleInitApp() {
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        assetManager.registerLoader(DDSLoader.class, "dds");
           
        loadTexture(0, "Textures/dds/Monkey_PNG_BC7_1.DDS", "BC7");
        loadTexture(1, "Textures/dds/Monkey_PNG_BC6H_3.DDS", "BC6");
        loadTexture(2, "Textures/dds/Monkey_PNG_BC6H_SF_2.DDS", "BC6_SF");
        loadTexture(3, "Textures/dds/Monkey_PNG_BC5_S_6.DDS", "BC5_S");
        loadTexture(4, "Textures/dds/Monkey_PNG_BC5_7.DDS", "BC5");
        loadTexture(5, "Textures/dds/Monkey_PNG_BC4_S_8.DDS", "BC4_S");
        loadTexture(6, "Textures/dds/Monkey_PNG_BC4_9.DDS", "BC4");
        loadTexture(7, "Textures/dds/Monkey_PNG_BC3_10.DDS", "BC3");
        loadTexture(8, "Textures/dds/Monkey_PNG_BC2_11.DDS", "BC2");
        loadTexture(9, "Textures/dds/Monkey_PNG_BC1_12.DDS", "BC1");
        
        flyCam.setDragToRotate(true);
               
    }

    private void loadTexture(int index, String texture, String description) {
        Texture2D t = (Texture2D)assetManager.loadTexture(new TextureKey(texture, false));
        Picture p = new Picture(description, true);
        p.setTexture(assetManager, t, false);
        p.setLocalTranslation((index % 4) * 200, Math.floorDiv(index, 4) * 200, 0);
        p.setWidth(200);
        p.setHeight(200);
        guiNode.attachChild(p);
    }
    
    
    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
