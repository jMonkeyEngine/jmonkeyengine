package org.jmonkeyengine.screenshottests.light.pbr;

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
import com.jme3.scene.shape.Sphere;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

public class TestPBRGlossinessMap extends ScreenshotTestBase {

    @Test
    public void testPBRGlossinessMap(TestInfo testInfo) {
        if(!testInfo.getTestClass().isPresent() || !testInfo.getTestMethod().isPresent()) {
            throw new RuntimeException("Test preconditions not met");
        }

        String imageName = testInfo.getTestClass().get().getName() + "." + testInfo.getTestMethod().get().getName();

        screenshotTest(new BaseAppState() {
            @Override
            protected void initialize(Application app) {
                Camera cam = app.getCamera();
                cam.setLocation(new Vector3f(0, 0, 5));
                cam.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);

                AssetManager assetManager = app.getAssetManager();
                SimpleApplication simpleApp = (SimpleApplication) app;
                
                Geometry model = new Geometry("Sphere", new Sphere(32, 32, 1.5f));
                MikktspaceTangentGenerator.generate(model);

                Material pbrMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
                pbrMat.setBoolean("UseSpecGloss", true);
                pbrMat.setTexture("GlossinessMap", assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg"));
                pbrMat.setTexture("BaseColorMap", assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg"));
                model.setMaterial(pbrMat);
                simpleApp.getRootNode().attachChild(model);

                Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
                simpleApp.getRootNode().attachChild(sky);

                EnvironmentProbeControl envProbe = new EnvironmentProbeControl(assetManager, 256);
                simpleApp.getRootNode().addControl(envProbe);
                envProbe.tag(sky);
            }

            @Override
            protected void cleanup(Application app) {}

            @Override
            protected void onEnable() {}

            @Override
            protected void onDisable() {}
        }).setBaseImageFileName(imageName)
          .setFramesToTakeScreenshotsOn(5)
          .run();
    }
}
