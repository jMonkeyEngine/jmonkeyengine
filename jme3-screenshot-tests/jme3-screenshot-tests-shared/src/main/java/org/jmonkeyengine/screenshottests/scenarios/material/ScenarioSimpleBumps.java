package org.jmonkeyengine.screenshottests.scenarios.material;

import static org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase.screenshotTest;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;

public class ScenarioSimpleBumps {

    public static ScreenshotTest testSimpleBumps() {
        return screenshotTest(new BaseAppState() {
            private float angle;
            private PointLight pl;
            private Spatial lightMdl;

            @Override
            protected void initialize(Application app) {
                SimpleApplication simpleApplication = (SimpleApplication) app;
                Node rootNode = simpleApplication.getRootNode();
                AssetManager assetManager = simpleApplication.getAssetManager();

                // Create quad with bump map material
                Quad quadMesh = new Quad(1, 1);
                Geometry sphere = new Geometry("Rock Ball", quadMesh);
                Material mat = assetManager.loadMaterial("Textures/BumpMapTest/SimpleBump.j3m");
                sphere.setMaterial(mat);
                MikktspaceTangentGenerator.generate(sphere);
                rootNode.attachChild(sphere);

                // Create light representation
                lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
                lightMdl.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
                rootNode.attachChild(lightMdl);

                // Create point light
                pl = new PointLight();
                pl.setColor(ColorRGBA.White);
                pl.setPosition(new Vector3f(0f, 0f, 4f));
                rootNode.addLight(pl);
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
                super.update(tpf);

                angle += tpf * 2f;
                angle %= FastMath.TWO_PI;

                pl.setPosition(new Vector3f(FastMath.cos(angle) * 4f, 0.5f, FastMath.sin(angle) * 4f));
                lightMdl.setLocalTranslation(pl.getPosition());
            }
        }).setFramesToTakeScreenshotsOn(10, 60);
    }
}
