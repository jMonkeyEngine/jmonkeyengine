package jme3test.light.pbr;

import com.jme3.anim.SkinningControl;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;

/**
 * This test case validates a shader compilation fix with a model using a PBR material in combination with a
 * SkinningControl. When you run this application and don't see a RenderException, the test is successful.
 * For a detailed explanation consult the GitHub issue: https://github.com/jMonkeyEngine/jmonkeyengine/issues/1340
 * -rvandoosselaer
 */
public class TestIssue1340 extends SimpleApplication {

    private Spatial model;
    private int frame;

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

        model = assetManager.loadModel("/glTF-Embedded/RiggedFigure.gltf");
        SkinningControl skinningControl = getSkinningControl(model);
        if (skinningControl == null || !skinningControl.isHardwareSkinningPreferred()) {
            throw new IllegalArgumentException("This test case requires a model with a SkinningControl and with Hardware skinning preferred!");
        }

        viewPort.setBackgroundColor(ColorRGBA.LightGray);
    }

    @Override
    public void simpleUpdate(float tpf) {
        frame++;
        if (frame == 2) {
            LightProbeFactory.makeProbe(stateManager.getState(EnvironmentCamera.class), rootNode, new JobProgressAdapter<LightProbe>() {
                @Override
                public void done(LightProbe result) {
                    enqueue(() -> {
                        rootNode.attachChild(model);
                        rootNode.addLight(result);
                    });
                }
            });
        }
    }

    private SkinningControl getSkinningControl(Spatial model) {
        SkinningControl control = model.getControl(SkinningControl.class);
        if (control != null) {
            return control;
        }

        if (model instanceof Node) {
            for (Spatial child : ((Node) model).getChildren()) {
                SkinningControl skinningControl = getSkinningControl(child);
                if (skinningControl != null) {
                    return skinningControl;
                }
            }
        }

        return null;
    }

}
