/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.shadow.PointLightShadowFilter;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.shadow.SpotLightShadowFilter;
import com.jme3.shadow.SpotLightShadowRenderer;

/**
 *
 * @author codex
 */
public class TestShadows extends SimpleApplication {
    
    private final boolean useDirectionalLight = true;
    private final boolean usePointLight = true;
    private final boolean useSpotLight = true;
    private final boolean useRenderers = true;
    private final int shadowMapSize = 4096;
    private final int splits = 3;
    
    public static void main(String[] args) {
        new TestShadows().start();
    }

    @Override
    public void simpleInitApp() {
        
        flyCam.setMoveSpeed(20);
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        
        Geometry floor = new Geometry("floor", new Quad(100, 100));
        floor.setLocalTranslation(-50, 0, 50);
        floor.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        floor.setMaterial(createMaterial(ColorRGBA.Gray));
        //floor.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        floor.setShadowMode(RenderQueue.ShadowMode.Receive);
        rootNode.attachChild(floor);
        
        Geometry cube = new Geometry("cube", new Box(.5f, .5f, .5f));
        cube.setLocalTranslation(0, 0, 0);
        cube.setLocalScale(1);
        cube.setMaterial(createMaterial(ColorRGBA.LightGray));
        cube.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(cube);
        
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(1, -1, 1));
        dl.setColor(ColorRGBA.Blue);
        if (useDirectionalLight) {
            rootNode.addLight(dl);
        }
        
        PointLight pl = new PointLight();
        pl.setPosition(new Vector3f(0, 6, -3));
        pl.setRadius(1000);
        pl.setColor(ColorRGBA.Red);
        if (usePointLight) {
            rootNode.addLight(pl);
        }
        
        SpotLight sl = new SpotLight();
        sl.setPosition(new Vector3f(3, 6, -3));
        sl.setDirection(new Vector3f(-1, -1, 1));
        sl.setSpotRange(0);
        sl.setSpotInnerAngle(FastMath.PI/6);
        sl.setSpotOuterAngle(FastMath.PI/3);
        sl.setColor(ColorRGBA.Green);
        if (useSpotLight) {
            rootNode.addLight(sl);
        }
        
        if (useRenderers) {
            initShadowRenderers(dl, pl, sl);
        } else {
            initShadowFilters(dl, pl, sl);
        }
        
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }
    
    private Material createMaterial(ColorRGBA color) {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", color);
        return mat;
    }
    
    private void initShadowFilters(DirectionalLight dl, PointLight pl, SpotLight sl) {
        
        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, shadowMapSize*4, splits);
        dlsf.setLight(dl);
        
        PointLightShadowFilter plsf = new PointLightShadowFilter(assetManager, shadowMapSize);
        plsf.setLight(pl);
        
        SpotLightShadowFilter slsf = new SpotLightShadowFilter(assetManager, shadowMapSize);
        slsf.setLight(sl);    
        slsf.setShadowIntensity(1f);
        slsf.setShadowZExtend(100);
        slsf.setShadowZFadeLength(5);
        slsf.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        slsf.setEnabled(true);
        
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        if (useDirectionalLight) {
            fpp.addFilter(dlsf);
        }
        if (usePointLight) {
            fpp.addFilter(plsf);
        }
        if (useSpotLight) {
            fpp.addFilter(slsf);
        }
        viewPort.addProcessor(fpp);
        
    }
    
    private void initShadowRenderers(DirectionalLight dl, PointLight pl, SpotLight sl) {
        
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, shadowMapSize*4, splits);
        dlsr.setLight(dl);
        
        PointLightShadowRenderer plsr = new PointLightShadowRenderer(assetManager, shadowMapSize);
        plsr.setLight(pl);
        
        SpotLightShadowRenderer slsr = new SpotLightShadowRenderer(assetManager, 512);
        slsr.setLight(sl);       
        slsr.setShadowIntensity(1f);
        slsr.setShadowZExtend(100);
        slsr.setShadowZFadeLength(5);
        slsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);   
        
        if (useDirectionalLight) {
            viewPort.addProcessor(dlsr);
        }
        if (usePointLight) {
            viewPort.addProcessor(plsr);
        }
        if (useSpotLight) {
            viewPort.addProcessor(slsr);
        }
        
    }
    
}
