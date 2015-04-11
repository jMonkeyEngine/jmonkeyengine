package jme3test.light.pbr;

import com.jme3.app.SimpleApplication;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.pbr.EnvironmentCamera;
import com.jme3.texture.plugins.ktx.KTXLoader;
import com.jme3.util.SkyFactory;

/**
 * A test case for PBR lighting.
 * Still experimental.
 *
 * @author nehon
 */
public class TestPBRLighting extends SimpleApplication {

    public static void main(String[] args) {
        TestPBRLighting app = new TestPBRLighting();
        app.start();
    }
    private Geometry model;
    private DirectionalLight dl;
    private Node modelNode;
    private int frame = 0;
    private boolean indirectLighting = true;
    private Material pbrMat;
    private Material adHocMat;

    @Override
    public void simpleInitApp() {
        assetManager.registerLoader(KTXLoader.class, "ktx");

        viewPort.setBackgroundColor(ColorRGBA.White);
        modelNode = (Node) new Node("modelNode");
        model = (Geometry) assetManager.loadModel("Models/Tank/tank.j3o");
        modelNode.attachChild(model);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(dl);
        dl.setColor(ColorRGBA.White);
        rootNode.attachChild(modelNode);

        final EnvironmentCamera envCam = new EnvironmentCamera(128, new Vector3f(0, 3f, 0));
        stateManager.attach(envCam);
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(new FXAAFilter());
        fpp.addFilter(new ToneMapFilter(Vector3f.UNIT_XYZ.mult(2.0f)));
        viewPort.addProcessor(fpp);

        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Sky_Cloudy.hdr", SkyFactory.EnvMapType.EquirectMap);
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Stonewall.hdr", SkyFactory.EnvMapType.EquirectMap);
        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/road.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);

        pbrMat = assetManager.loadMaterial("Models/Tank/tank.j3m");
        model.setMaterial(pbrMat);

        ChaseCamera chaser = new ChaseCamera(cam, modelNode, inputManager);
        chaser.setDragToRotate(true);
        chaser.setMinVerticalRotation(-FastMath.HALF_PI);
        chaser.setMaxDistance(1000);
        chaser.setSmoothMotion(true);
        chaser.setRotationSensitivity(10);
        chaser.setZoomSensitivity(5);
        flyCam.setEnabled(false);
        //flyCam.setMoveSpeed(100);

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("toggle") && isPressed) {
                    if (!indirectLighting) {
                        toggleIBL();

                    } else {
                        pbrMat.clearParam("IntegrateBRDF");
                        indirectLighting = false;
                    }
                }

                if (name.equals("switchMats") && isPressed) {
                    if (model.getMaterial() == pbrMat) {
                        model.setMaterial(adHocMat);
                    } else {
                        model.setMaterial(pbrMat);
                    }
                }

                if (name.equals("debug") && isPressed) {
                    envCam.toggleDebug();
                }

                if (name.equals("up") && isPressed) {
                    model.move(0, tpf * 100f, 0);
                }

                if (name.equals("down") && isPressed) {
                    model.move(0, -tpf * 100f, 0);
                }
                if (name.equals("left") && isPressed) {
                    model.move(0, 0, tpf * 100f);
                }
                if (name.equals("right") && isPressed) {
                    model.move(0, 0, -tpf * 100f);
                }
                if (name.equals("light") && isPressed) {
                    dl.setDirection(cam.getDirection().normalize());
                }
            }
        }, "toggle", "light", "up", "down", "left", "right", "debug");

        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("light", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("debug", new KeyTrigger(KeyInput.KEY_D));

    }

    private void toggleIBL() {
        ensurePbrMat();
        pbrMat.setTexture("IrradianceMap", stateManager.getState(EnvironmentCamera.class).getIrradianceMap());
        pbrMat.setTexture("PrefEnvMap", stateManager.getState(EnvironmentCamera.class).getPrefilteredEnvMap());
        pbrMat.setTexture("IntegrateBRDF", assetManager.loadTexture("Common/Textures/integrateBRDF.ktx"));
        indirectLighting = true;
    }

    private void ensurePbrMat() {
        if (model.getMaterial() != pbrMat && model.getMaterial() != adHocMat) {
            pbrMat = model.getMaterial();
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        frame++;

        if (frame == 2) {
            modelNode.removeFromParent();
            stateManager.getState(EnvironmentCamera.class).snapshot(rootNode, new Runnable() {
                 
                //this code is ensured to be called in the update loop, the run method is called by the EnvCamera app state in it's update cycle
                @Override
                public void run() {                    
                  toggleIBL();
                }
            });
        }
        if (frame > 2 && modelNode.getParent() == null) {
            rootNode.attachChild(modelNode);
        }
    }

}
