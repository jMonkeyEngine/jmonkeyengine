package jme3test.light.pbr;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;

/**
 * @author: rvandoosselaer
 */
public class TestIssue1340 extends SimpleApplication {

    int frame;

    public static void main(String[] args) {
        TestIssue1340 testIssue1340 = new TestIssue1340();
        testIssue1340.setSettings(createSettings());
        testIssue1340.start();
    }

    private static AppSettings createSettings() {
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL32);
        return settings;
    }

    @Override
    public void simpleInitApp() {
        stateManager.attach(new EnvironmentCamera(32));

        DirectionalLight light = new DirectionalLight(Vector3f.UNIT_Y.negate(), ColorRGBA.White);
        rootNode.addLight(light);

        assetManager.registerLocator("https://github.com/KhronosGroup/glTF-Sample-Models/raw/master/2.0/RiggedFigure/", UrlLocator.class);

        Spatial model = assetManager.loadModel("/glTF-Embedded/RiggedFigure.gltf");
        rootNode.attachChild(model);

        viewPort.setBackgroundColor(ColorRGBA.LightGray);
    }

    @Override
    public void simpleUpdate(float tpf) {
        frame++;
        if (frame == 2) {
            LightProbeFactory.makeProbe(stateManager.getState(EnvironmentCamera.class), rootNode, new JobProgressAdapter<LightProbe>() {
                @Override
                public void done(LightProbe result) {
                    enqueue(() -> rootNode.addLight(result));
                }
            });
        }
    }
}
