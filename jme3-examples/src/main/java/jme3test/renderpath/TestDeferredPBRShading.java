package jme3test.renderpath;

import com.jme3.app.SimpleApplication;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.environment.util.LightsDebugState;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.plugins.ktx.KTXLoader;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

public class TestDeferredPBRShading extends SimpleApplication {
    private DirectionalLight dl;

    private float roughness = 0.0f;

    private Node modelNode;
    private int frame = 0;
    private Material pbrMat;
    private Geometry model;
    private Node tex;
    @Override
    public void simpleInitApp() {
        // Baking irradiance data requires the Forward path!
        renderManager.setRenderPath(RenderManager.RenderPath.Forward);

        roughness = 1.0f;
        assetManager.registerLoader(KTXLoader.class, "ktx");

        viewPort.setBackgroundColor(ColorRGBA.White);
        modelNode = new Node("modelNode");
        model = (Geometry) assetManager.loadModel("Models/Tank/tank.j3o");
        MikktspaceTangentGenerator.generate(model);
        modelNode.attachChild(model);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(dl);
        dl.setColor(ColorRGBA.White);
        rootNode.attachChild(modelNode);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        int numSamples = context.getSettings().getSamples();
        if (numSamples > 0) {
            fpp.setNumSamples(numSamples);
        }

//        fpp.addFilter(new FXAAFilter());
        fpp.addFilter(new ToneMapFilter(Vector3f.UNIT_XYZ.mult(1.0f)));
//        fpp.addFilter(new SSAOFilter(0.5f, 3, 0.2f, 0.2f));
        viewPort.addProcessor(fpp);

        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Sky_Cloudy.hdr", SkyFactory.EnvMapType.EquirectMap);
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.CubeMap);
        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/road.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);

        pbrMat = assetManager.loadMaterial("Models/Tank/tank.j3m");
        model.setMaterial(pbrMat);


        final EnvironmentCamera envCam = new EnvironmentCamera(256, new Vector3f(0, 3f, 0));
        stateManager.attach(envCam);

        LightsDebugState debugState = new LightsDebugState();
        stateManager.attach(debugState);

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
                if (name.equals("debug") && isPressed) {
                    if (tex == null) {
                        return;
                    }
                    if (tex.getParent() == null) {
                        guiNode.attachChild(tex);
                    } else {
                        tex.removeFromParent();
                    }
                }

                if (name.equals("rup") && isPressed) {
                    roughness = FastMath.clamp(roughness + 0.1f, 0.0f, 1.0f);
                    pbrMat.setFloat("Roughness", roughness);
                }
                if (name.equals("rdown") && isPressed) {
                    roughness = FastMath.clamp(roughness - 0.1f, 0.0f, 1.0f);
                    pbrMat.setFloat("Roughness", roughness);
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
        }, "toggle", "light", "up", "down", "left", "right", "debug", "rup", "rdown");

        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("light", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("debug", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("rup", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("rdown", new KeyTrigger(KeyInput.KEY_G));
    }

    @Override
    public void simpleUpdate(float tpf) {
        frame++;

        if (frame == 2) {
            modelNode.removeFromParent();
            final LightProbe probe = LightProbeFactory.makeProbe(stateManager.getState(EnvironmentCamera.class), rootNode, new JobProgressAdapter<LightProbe>() {

                @Override
                public void done(LightProbe result) {
                    System.err.println("Done rendering env maps");
                    tex = EnvMapUtils.getCubeMapCrossDebugViewWithMipMaps(result.getPrefilteredEnvMap(), assetManager);
                    // Now, switching to the Deferred rendering path.
                    renderManager.setRenderPath(RenderManager.RenderPath.Deferred);
                }
            });
            probe.getArea().setRadius(100);
            rootNode.addLight(probe);
            //getStateManager().getState(EnvironmentManager.class).addEnvProbe(probe);

        }
        if (frame > 10 && modelNode.getParent() == null) {
            rootNode.attachChild(modelNode);
        }
    }

    public static void main(String[] args) {
        TestDeferredPBRShading testDeferredPBRShading = new TestDeferredPBRShading();
        testDeferredPBRShading.start();
    }
}
