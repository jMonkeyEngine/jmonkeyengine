package org.jmonkeyengine.screenshottests.scenarios.light.pbr;

import static org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase.screenshotTest;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.FastLightProbeFactory;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.plugins.ktx.KTXLoader;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;

public class ScenarioPBRLighting {

    public static ScreenshotTest testPBRLighting(float roughness, boolean updateLight) {
        return screenshotTest(new BaseAppState() {
            private static final int RESOLUTION = 256;
            private Node modelNode;
            private int frame = 0;

            @Override
            protected void initialize(Application app) {
                Camera cam = app.getCamera();
                cam.setLocation(new Vector3f(18, 10, 0));
                cam.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);

                AssetManager assetManager = app.getAssetManager();
                assetManager.registerLoader(KTXLoader.class, "ktx");

                app.getViewPort().setBackgroundColor(ColorRGBA.White);

                modelNode = new Node("modelNode");
                Geometry model = (Geometry) assetManager.loadModel("Models/Tank/tank.j3o");
                MikktspaceTangentGenerator.generate(model);
                modelNode.attachChild(model);

                DirectionalLight dl = new DirectionalLight();
                dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
                SimpleApplication simpleApp = (SimpleApplication) app;
                simpleApp.getRootNode().addLight(dl);
                dl.setColor(ColorRGBA.White);

                if (updateLight) {
                    dl.setDirection(app.getCamera().getDirection().normalize());
                }

                simpleApp.getRootNode().attachChild(modelNode);

                FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
                int numSamples = app.getContext().getSettings().getSamples();
                if (numSamples > 0) {
                    fpp.setNumSamples(numSamples);
                }

                fpp.addFilter(new ToneMapFilter(Vector3f.UNIT_XYZ.mult(4.0f)));
                app.getViewPort().addProcessor(fpp);

                Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
                simpleApp.getRootNode().attachChild(sky);

                Material pbrMat = assetManager.loadMaterial("Models/Tank/tank.j3m");
                pbrMat.setFloat("Roughness", roughness);
                model.setMaterial(pbrMat);

                EnvironmentCamera envCam = new EnvironmentCamera(RESOLUTION, new Vector3f(0, 3f, 0));
                app.getStateManager().attach(envCam);
            }

            @Override
            protected void cleanup(Application app) {
            }

            @Override
            protected void onEnable() {
            }

            @Override
            protected void onDisable() {
            }

            @Override
            public void update(float tpf) {
                frame++;

                if (frame == 2) {
                    modelNode.removeFromParent();
                    LightProbe probe;

                    SimpleApplication simpleApp = (SimpleApplication) getApplication();
                    probe = FastLightProbeFactory.makeProbe(simpleApp.getRenderManager(),
                            simpleApp.getAssetManager(),
                            RESOLUTION,
                            Vector3f.ZERO,
                            1f,
                            1000f,
                            simpleApp.getRootNode());

                    probe.getArea().setRadius(100);
                    simpleApp.getRootNode().addLight(probe);
                }

                if (frame > 10 && modelNode.getParent() == null) {
                    SimpleApplication simpleApp = (SimpleApplication) getApplication();
                    simpleApp.getRootNode().attachChild(modelNode);
                }
            }
        }).setFramesToTakeScreenshotsOn(12);
    }
}
