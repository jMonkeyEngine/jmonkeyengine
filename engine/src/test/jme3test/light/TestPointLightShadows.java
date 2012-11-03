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
package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.PointLight;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.PointLightShadowFilter;
import com.jme3.shadow.PointLightShadowRenderer;

public class TestPointLightShadows extends SimpleApplication implements ActionListener {

    public static void main(String[] args) {
        TestPointLightShadows app = new TestPointLightShadows();
        app.start();
    }
    Node lightNode;
    private boolean hardwareShadows = true;
    PointLightShadowRenderer plsr ;
    PointLightShadowFilter plsf;

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(10);
        cam.setLocation(new Vector3f(0.040581334f, 1.7745866f, 6.155161f));
        cam.setRotation(new Quaternion(4.3868728E-5f, 0.9999293f, -0.011230096f, 0.0039059948f));


        Node scene = (Node) assetManager.loadModel("Models/Test/CornellBox.j3o");
        scene.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(scene);
        rootNode.getChild("Cube").setShadowMode(RenderQueue.ShadowMode.Receive);
        lightNode = (Node) rootNode.getChild("Lamp");
        Geometry lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
        //Geometry  lightMdl = new Geometry("Light", new Box(.1f,.1f,.1f));
        lightMdl.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        lightMdl.setShadowMode(RenderQueue.ShadowMode.Off);
        lightNode.attachChild(lightMdl);
        //lightMdl.setLocalTranslation(lightNode.getLocalTranslation());


        Geometry box = new Geometry("box", new Box(0.2f, 0.2f, 0.2f));
        //Geometry  lightMdl = new Geometry("Light", new Box(.1f,.1f,.1f));
        box.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        box.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(box);
        box.setLocalTranslation(-1f, 0.5f, -2);



        plsr = new PointLightShadowRenderer(assetManager, 512);
        plsr.setLight((PointLight) scene.getLocalLightList().get(0));
        plsr.setFilterMode(PointLightShadowRenderer.FilterMode.Nearest);
    //   plsr.displayDebug();
        viewPort.addProcessor(plsr);


        plsf = new PointLightShadowFilter(assetManager, 512);
        plsf.setLight((PointLight) scene.getLocalLightList().get(0));
        plsf.setFilterMode(PointLightShadowRenderer.FilterMode.Nearest);
        plsf.setEnabled(false);
        
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(plsf);   
        viewPort.addProcessor(fpp);

        initUIAndInputs();
    }
    
     BitmapText shadowTypeText;
     BitmapText shadowCompareText;
     BitmapText shadowFilterText;
     BitmapText shadowIntensityText;
     final static String TYPE_TEXT = "(Space) Shadow type : ";
     final static String COMPARE_TEXT = "(enter) Shadow compare ";
     final static String FILTERING_TEXT = "(f) Edge filtering : ";
     final static String INTENSITY_TEXT = "(t:up, g:down) Shadow intensity : ";
     
     private void initUIAndInputs() {     
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        shadowTypeText = createText();
        shadowCompareText = createText();
        shadowFilterText = createText();
        shadowIntensityText = createText();
        
        shadowTypeText.setText(TYPE_TEXT+"Processor");
        shadowCompareText.setText(COMPARE_TEXT+(hardwareShadows?"Hardware":"Software"));
        shadowFilterText.setText(FILTERING_TEXT+plsr.getFilterMode().toString());
        shadowIntensityText.setText(INTENSITY_TEXT+plsr.getShadowIntensity());        

        shadowTypeText.setLocalTranslation(10, cam.getHeight()-20, 0);
        shadowCompareText.setLocalTranslation(10, cam.getHeight()-40, 0);
        shadowFilterText.setLocalTranslation(10, cam.getHeight()-60, 0);
        shadowIntensityText.setLocalTranslation(10, cam.getHeight()-80, 0);
        
        guiNode.attachChild(shadowTypeText);
        guiNode.attachChild(shadowCompareText);
        guiNode.attachChild(shadowFilterText);
        guiNode.attachChild(shadowIntensityText);
        
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("changeFiltering", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("ShadowUp", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("ShadowDown", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping("ThicknessUp", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("ThicknessDown", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("toggleHW", new KeyTrigger(KeyInput.KEY_RETURN));


        inputManager.addListener(this, "toggleHW", "toggle", "ShadowUp", "ShadowDown", "ThicknessUp", "ThicknessDown", "changeFiltering");

    }
    
    int filteringIndex = 0;
    int renderModeIndex = 0;
    
     public void onAction(String name, boolean keyPressed, float tpf) {
        if (name.equals("toggle") && keyPressed) {
            renderModeIndex += 1;
            renderModeIndex %= 3;

            switch (renderModeIndex) {
                case 0:
                    viewPort.addProcessor(plsr);
                    shadowTypeText.setText(TYPE_TEXT+"Processor");
                    break;
                case 1:
                    viewPort.removeProcessor(plsr);
                    plsf.setEnabled(true);
                    shadowTypeText.setText(TYPE_TEXT+"Filter");
                    break;
                case 2:
                    plsf.setEnabled(false);
                    shadowTypeText.setText(TYPE_TEXT+"None");
                    break;
            }



        } else if (name.equals("toggleHW") && keyPressed) {
            hardwareShadows = !hardwareShadows;
            plsr.setCompareMode(hardwareShadows ? PointLightShadowRenderer.CompareMode.Hardware : PointLightShadowRenderer.CompareMode.Software);
            plsf.setCompareMode(hardwareShadows ? PointLightShadowRenderer.CompareMode.Hardware : PointLightShadowRenderer.CompareMode.Software);
       
            shadowCompareText.setText(COMPARE_TEXT+(hardwareShadows?"Hardware":"Software"));       
        }


        if (name.equals("changeFiltering") && keyPressed) {
            filteringIndex = plsr.getFilterMode().ordinal();
            filteringIndex = (filteringIndex + 1) % PointLightShadowRenderer.FilterMode.values().length;
            PointLightShadowRenderer.FilterMode m = PointLightShadowRenderer.FilterMode.values()[filteringIndex];
            plsr.setFilterMode(m);
            plsf.setFilterMode(m);           
            shadowFilterText.setText(FILTERING_TEXT+m.toString());      
        }

     
        if (name.equals("ShadowUp") && keyPressed) {
            plsr.setShadowIntensity(plsr.getShadowIntensity() + 0.1f);
            plsf.setShadowIntensity(plsr.getShadowIntensity() + 0.1f);
           
            shadowIntensityText.setText(INTENSITY_TEXT+plsr.getShadowIntensity());        
        }
        if (name.equals("ShadowDown") && keyPressed) {
            plsr.setShadowIntensity(plsr.getShadowIntensity() - 0.1f);
            plsf.setShadowIntensity(plsr.getShadowIntensity() - 0.1f);
            shadowIntensityText.setText(INTENSITY_TEXT+plsr.getShadowIntensity());   
        }
        if (name.equals("ThicknessUp") && keyPressed) {
            plsr.setEdgesThickness(plsr.getEdgesThickness() + 1);
            plsf.setEdgesThickness(plsr.getEdgesThickness() + 1);
            System.out.println("Shadow thickness : " + plsr.getEdgesThickness());
        }
        if (name.equals("ThicknessDown") && keyPressed) {
            plsr.setEdgesThickness(plsr.getEdgesThickness() - 1);
            plsf.setEdgesThickness(plsr.getEdgesThickness() - 1);
            System.out.println("Shadow thickness : " + plsr.getEdgesThickness());
        }
        
     }
    float time;

    @Override
    public void simpleUpdate(float tpf) {
        time += tpf;
        lightNode.setLocalTranslation(FastMath.cos(time)*0.4f,lightNode.getLocalTranslation().y , FastMath.sin(time)*0.4f);

    }
 
    private BitmapText createText() {
        BitmapText t = new BitmapText(guiFont, false);
        t.setSize(guiFont.getCharSet().getRenderedSize()*0.75f);
        return t;
    }
}