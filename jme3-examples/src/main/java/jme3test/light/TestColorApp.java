package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.light.DirectionalLight;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
 
public class TestColorApp extends SimpleApplication {
    public static void main(String[] args) {
        TestColorApp app = new TestColorApp();
        app.start();
    }
 
    @Override
    public void simpleInitApp() {
        // Lights
        DirectionalLight sun = new DirectionalLight();
        Vector3f sunPosition = new Vector3f(1, -1, 1);
        sun.setDirection(sunPosition);
        sun.setColor(new ColorRGBA(1f,1f,1f,1f));
        rootNode.addLight(sun);
 
        //DirectionalLightShadowFilter sun_renderer = new DirectionalLightShadowFilter(assetManager, 2048, 4);
        DirectionalLightShadowRenderer sun_renderer = new DirectionalLightShadowRenderer(assetManager, 2048, 1);
        sun_renderer.setLight(sun);
        viewPort.addProcessor(sun_renderer);
        
//        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
//        fpp.addFilter(sun_renderer);
//        viewPort.addProcessor(fpp);
        
        rootNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
 
        // Camera
        viewPort.setBackgroundColor(new ColorRGBA(.6f, .6f, .6f, 1f));
        ChaseCamera chaseCam = new ChaseCamera(cam, inputManager);
 
 
        // Objects
        // Ground Object
        final Geometry groundBoxWhite = new Geometry("Box", new Box(7.5f, 7.5f, .25f));
        float[] f = {-FastMath.PI / 2, 3 * FastMath.PI / 2, 0f};
        groundBoxWhite.setLocalRotation(new Quaternion(f));
        groundBoxWhite.move(7.5f, -.75f, 7.5f);
        final Material groundMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        groundMaterial.setColor("Diffuse", new ColorRGBA(.9f, .9f, .9f, .9f));
        groundBoxWhite.setMaterial(groundMaterial);
        groundBoxWhite.addControl(chaseCam);
        rootNode.attachChild(groundBoxWhite);
 
        // Planter
        Geometry planterBox = new Geometry("Box", new Box(.5f, .5f, .5f));
        final Material planterMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        planterMaterial.setTexture("DiffuseMap", assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg"));
        planterBox.setMaterial(groundMaterial);
        planterBox.setLocalTranslation(10, 0, 9);
        rootNode.attachChild(planterBox);
 
        // Action!
        inputManager.addMapping("on", new KeyTrigger(KeyInput.KEY_Z));
        inputManager.addMapping("off", new KeyTrigger(KeyInput.KEY_X));
 
        inputManager.addListener(new AnalogListener() {
            @Override
            public void onAnalog(String s, float v, float v1) {
                if (s.equals("on")) {
                    groundBoxWhite.setMaterial(planterMaterial);
                }
                if (s.equals("off")) {
                    groundBoxWhite.setMaterial(groundMaterial);
                }
            }
        }, "on", "off");
 
        inputEnabled = true;
    }
}