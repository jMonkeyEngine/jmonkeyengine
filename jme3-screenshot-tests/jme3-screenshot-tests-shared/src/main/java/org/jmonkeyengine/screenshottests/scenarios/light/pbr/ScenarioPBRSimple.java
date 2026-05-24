package org.jmonkeyengine.screenshottests.scenarios.light.pbr;

import static org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase.screenshotTest;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;

public class ScenarioPBRSimple {

    public static ScreenshotTest testPBRSimple(boolean realtimeBaking) {
        return screenshotTest(new BaseAppState() {
            private int frame = 0;

            @Override
            protected void initialize(Application app) {
                Camera cam = app.getCamera();
                cam.setLocation(new Vector3f(18, 10, 0));
                cam.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);

                AssetManager assetManager = app.getAssetManager();
                SimpleApplication simpleApp = (SimpleApplication) app;

                // Create the tank model
                Geometry model = (Geometry) assetManager.loadModel("Models/Tank/tank.j3o");
                MikktspaceTangentGenerator.generate(model);

                Material pbrMat = assetManager.loadMaterial("Models/Tank/tank.j3m");
                model.setMaterial(pbrMat);
                simpleApp.getRootNode().attachChild(model);

                // Create sky
                Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
                simpleApp.getRootNode().attachChild(sky);

                // Create baker control
                EnvironmentProbeControl envProbe = new EnvironmentProbeControl(assetManager, 256);
                simpleApp.getRootNode().addControl(envProbe);

                // Tag the sky, only the tagged spatials will be rendered in the env map
                envProbe.tag(sky);
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
                if (realtimeBaking) {
                    frame++;
                    if (frame == 2) {
                        SimpleApplication simpleApp = (SimpleApplication) getApplication();
                        simpleApp.getRootNode().getControl(EnvironmentProbeControl.class).rebake();
                    }
                }
            }
        }).setFramesToTakeScreenshotsOn(10);
    }
}
