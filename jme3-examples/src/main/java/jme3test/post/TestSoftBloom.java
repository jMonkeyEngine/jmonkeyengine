/*
 * Copyright (c) 2024 jMonkeyEngine
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

package jme3test.post;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.SoftBloomFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.util.SkyFactory;
import com.jme3.util.SkyFactory.EnvMapType;

/**
 * Tests {@link SoftBloomFilter} with HDR.
 * <p>
 * Note: the camera is pointed directly at the ground, which is completely
 * black for some reason.
 * 
 * @author codex
 */
public class TestSoftBloom extends SimpleApplication implements ActionListener, AnalogListener {

    private SoftBloomFilter bloom;
    private BitmapText passes, factor, bilinear;
    private BitmapText power, intensity;
    private Material tankMat;
    private float emissionPower = 50;
    private float emissionIntensity = 50;
    private final int maxPasses = 10;
    private final float factorRate = 0.1f;
    
    public static void main(String[] args){
        TestSoftBloom app = new TestSoftBloom();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        
        cam.setLocation(new Vector3f(10, 10, 10));
        flyCam.setMoveSpeed(20);

        Material mat = new Material(assetManager,"Common/MatDefs/Light/Lighting.j3md");
        mat.setFloat("Shininess", 15f);
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Yellow.mult(0.2f));
        mat.setColor("Diffuse", ColorRGBA.Yellow.mult(0.2f));
        mat.setColor("Specular", ColorRGBA.Yellow.mult(0.8f));

        Material matSoil = new Material(assetManager,"Common/MatDefs/Light/Lighting.j3md");
        matSoil.setFloat("Shininess", 15f);
        matSoil.setBoolean("UseMaterialColors", true);
        matSoil.setColor("Ambient", ColorRGBA.Gray);
        matSoil.setColor("Diffuse", ColorRGBA.Gray);
        matSoil.setColor("Specular", ColorRGBA.Gray);

        Spatial teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
        teapot.setLocalTranslation(0,0,10);

        teapot.setMaterial(mat);
        teapot.setShadowMode(ShadowMode.CastAndReceive);
        teapot.setLocalScale(10.0f);
        rootNode.attachChild(teapot);

        Geometry soil = new Geometry("soil", new Box(800, 10, 700));
        soil.setLocalTranslation(0, -13, 550);
        soil.setMaterial(matSoil);
        soil.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(soil);
        
        tankMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        tankMat.setTexture("BaseColorMap", assetManager.loadTexture(new TextureKey("Models/HoverTank/tank_diffuse.jpg", !true)));
        tankMat.setTexture("SpecularMap", assetManager.loadTexture(new TextureKey("Models/HoverTank/tank_specular.jpg", !true)));
        tankMat.setTexture("NormalMap", assetManager.loadTexture(new TextureKey("Models/HoverTank/tank_normals.png", !true)));
        tankMat.setTexture("EmissiveMap", assetManager.loadTexture(new TextureKey("Models/HoverTank/tank_glow_map.jpg", !true)));
        tankMat.setFloat("EmissivePower", emissionPower);
        tankMat.setFloat("EmissiveIntensity", 50);
        tankMat.setFloat("Metallic", .5f);
        Spatial tank = assetManager.loadModel("Models/HoverTank/Tank2.mesh.xml");
        tank.setLocalTranslation(-10, 5, -10);
        tank.setMaterial(tankMat);
        rootNode.attachChild(tank);

        DirectionalLight light=new DirectionalLight();
        light.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        light.setColor(ColorRGBA.White);
        //rootNode.addLight(light);
        
        PointLight pl = new PointLight();
        pl.setPosition(new Vector3f(5, 5, 5));
        pl.setRadius(1000);
        pl.setColor(ColorRGBA.White);
        rootNode.addLight(pl);

        // load sky
        Spatial sky = SkyFactory.createSky(assetManager, 
                "Textures/Sky/Bright/FullskiesBlueClear03.dds", 
                EnvMapType.CubeMap);
        sky.setCullHint(Spatial.CullHint.Never);
        rootNode.attachChild(sky);
        EnvironmentProbeControl.tagGlobal(sky);
        
        rootNode.addControl(new EnvironmentProbeControl(assetManager, 256));
        
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        bloom = new SoftBloomFilter();
        fpp.addFilter(bloom);
        viewPort.addProcessor(fpp);
        
        int textY = context.getSettings().getHeight()-5;
        float xRow1 = 10, xRow2 = 250;
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        passes = createText("", xRow1, textY);
        createText("[ R / F ]", xRow2, textY);
        factor = createText("", xRow1, textY-25);
        createText("[ T / G ]", xRow2, textY-25);
        bilinear = createText("", xRow1, textY-25*2);
        createText("[ space ]", xRow2, textY-25*2);
        power = createText("", xRow1, textY-25*3);
        createText("[ Y / H ]", xRow2, textY-25*3);
        intensity = createText("", xRow1, textY-25*4);
        createText("[ U / J ]", xRow2, textY-25*4);
        updateHud();
        
        inputManager.addMapping("incr-passes", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("decr-passes", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("incr-factor", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("decr-factor", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping("toggle-bilinear", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("incr-power", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("decr-power", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("incr-intensity", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("decr-intensity", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addListener(this, "incr-passes", "decr-passes", "incr-factor", "decr-factor",
                "toggle-bilinear", "incr-power", "decr-power", "incr-intensity", "decr-intensity");
        
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        updateHud();
    }
    
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
            if (name.equals("incr-passes")) {
                bloom.setNumSamplingPasses(Math.min(bloom.getNumSamplingPasses()+1, maxPasses));
            } else if (name.equals("decr-passes")) {
                bloom.setNumSamplingPasses(Math.max(bloom.getNumSamplingPasses()-1, 1));
            } else if (name.equals("toggle-bilinear")) {
                bloom.setBilinearFiltering(!bloom.isBilinearFiltering());
            }
            updateHud();
        }
    }
    
    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (name.equals("incr-factor")) {
            bloom.setGlowFactor(bloom.getGlowFactor()+factorRate*tpf);
        } else if (name.equals("decr-factor")) {
            bloom.setGlowFactor(bloom.getGlowFactor()-factorRate*tpf);
        } else if (name.equals("incr-power")) {
            emissionPower += 10f*tpf;
            updateTankMaterial();
        } else if (name.equals("decr-power")) {
            emissionPower -= 10f*tpf;
            updateTankMaterial();
        } else if (name.equals("incr-intensity")) {
            emissionIntensity += 10f*tpf;
            updateTankMaterial();
        } else if (name.equals("decr-intensity")) {
            emissionIntensity -= 10f*tpf;
            updateTankMaterial();
        }
        updateHud();
    }
    
    private BitmapText createText(String string, float x, float y) {
        BitmapText text = new BitmapText(guiFont);
        text.setSize(guiFont.getCharSet().getRenderedSize());
        text.setLocalTranslation(x, y, 0);
        text.setText(string);
        guiNode.attachChild(text);
        return text;
    }
    
    private void updateHud() {
        passes.setText("Passes = " + bloom.getNumSamplingPasses());
        factor.setText("Glow Factor = " + floatToString(bloom.getGlowFactor(), 5));
        bilinear.setText("Bilinear Filtering = " + bloom.isBilinearFiltering());
        power.setText("Emission Power = " + floatToString(emissionPower, 5));
        intensity.setText("Emission Intensity = " + floatToString(emissionIntensity, 5));
    }
    
    private String floatToString(float value, int length) {
        String string = Float.toString(value);
        return string.substring(0, Math.min(length, string.length()));
    }
    
    private void updateTankMaterial() {
        emissionPower = Math.max(emissionPower, 0);
        emissionIntensity = Math.max(emissionIntensity, 0);
        tankMat.setFloat("EmissivePower", emissionPower);
        tankMat.setFloat("EmissiveIntensity", emissionIntensity);
    }
    
}
