package org.jmonkeyengine.screenshottests.scenarios.scene.instancing;

import static org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase.screenshotTest;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapText;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Box;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;

import java.util.Locale;

public class ScenarioInstanceNodeWithPbr {

    public static ScreenshotTest testInstanceNodeWithPbr() {
        return screenshotTest(new BaseAppState() {
            private Geometry box;
            private float pos = -5;
            private float vel = 50;
            private BitmapText bmp;

            @Override
            protected void initialize(Application app) {
                SimpleApplication simpleApp = (SimpleApplication) app;

                app.getCamera().setLocation(Vector3f.UNIT_XYZ.mult(12));
                app.getCamera().lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

                bmp = new BitmapText(app.getAssetManager().loadFont("Interface/Fonts/Default.fnt"));
                bmp.setText("<placeholder>");
                bmp.setLocalTranslation(10, app.getContext().getSettings().getHeight() - 20, 0);
                bmp.setColor(ColorRGBA.Red);
                simpleApp.getGuiNode().attachChild(bmp);

                InstancedNode instancedNode = new InstancedNode("InstancedNode");
                simpleApp.getRootNode().attachChild(instancedNode);

                Box mesh = new Box(0.5f, 0.5f, 0.5f);
                box = new Geometry("Box", mesh);
                Material pbrMaterial = createPbrMaterial(app, ColorRGBA.Red);
                box.setMaterial(pbrMaterial);

                instancedNode.attachChild(box);
                instancedNode.instance();

                DirectionalLight light = new DirectionalLight();
                light.setDirection(new Vector3f(-1, -2, -3).normalizeLocal());
                simpleApp.getRootNode().addLight(light);
            }

            private Material createPbrMaterial(Application app, ColorRGBA color) {
                Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Light/PBRLighting.j3md");
                mat.setColor("BaseColor", color);
                mat.setFloat("Roughness", 0.8f);
                mat.setFloat("Metallic", 0.1f);
                mat.setBoolean("UseInstancing", true);
                return mat;
            }

            @Override
            public void update(float tpf) {
                pos += tpf * vel;
                box.setLocalTranslation(pos, 0f, 0f);

                bmp.setText(String.format(Locale.ENGLISH, "BoxPosition: (%.2f, %.1f, %.1f)", pos, 0f, 0f));
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
        }).setFramesToTakeScreenshotsOn(1, 10);
    }
}
