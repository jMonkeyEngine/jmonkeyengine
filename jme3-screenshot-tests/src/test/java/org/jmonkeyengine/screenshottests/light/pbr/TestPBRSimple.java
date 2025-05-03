package org.jmonkeyengine.screenshottests.light.pbr;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * A simpler PBR example that uses EnvironmentProbeControl to bake the environment
 *
 * @author Richard Tingle (aka richtea) - screenshot test adaptation
 */
public class TestPBRSimple extends ScreenshotTestBase {

    private static Stream<Arguments> testParameters() {
        return Stream.of(
            Arguments.of("WithRealtimeBaking", true),
            Arguments.of("WithoutRealtimeBaking", false)
        );
    }

    /**
     * Test PBR simple with different parameters
     * 
     * @param testName The name of the test (used for screenshot filename)
     * @param realtimeBaking Whether to use realtime baking
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("testParameters")
    public void testPBRSimple(String testName, boolean realtimeBaking, TestInfo testInfo) {
        if(!testInfo.getTestClass().isPresent() || !testInfo.getTestMethod().isPresent()) {
            throw new RuntimeException("Test preconditions not met");
        }

        String imageName = testInfo.getTestClass().get().getName() + "." + testInfo.getTestMethod().get().getName() + "_" + testName;

        screenshotTest(new BaseAppState() {
            private EnvironmentProbeControl envProbe;
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
                envProbe = new EnvironmentProbeControl(assetManager, 256);
                simpleApp.getRootNode().addControl(envProbe);
                
                // Tag the sky, only the tagged spatials will be rendered in the env map
                envProbe.tag(sky);
            }

            @Override
            protected void cleanup(Application app) {
                // Nothing to clean up
            }

            @Override
            protected void onEnable() {
                // Nothing to do
            }

            @Override
            protected void onDisable() {
                // Nothing to do
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
        }).setBaseImageFileName(imageName)
          .setFramesToTakeScreenshotsOn(10)
          .run();
    }
}