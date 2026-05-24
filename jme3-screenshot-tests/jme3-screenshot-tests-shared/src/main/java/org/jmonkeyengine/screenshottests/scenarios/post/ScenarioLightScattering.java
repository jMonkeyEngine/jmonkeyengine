package org.jmonkeyengine.screenshottests.scenarios.post;

import static org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase.screenshotTest;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;

public class ScenarioLightScattering {

    public static ScreenshotTest testLightScattering() {
        return screenshotTest(new BaseAppState() {
            @Override
            protected void initialize(Application app) {
                SimpleApplication simpleApplication = (SimpleApplication) app;
                Node rootNode = simpleApplication.getRootNode();

                simpleApplication.getCamera().setLocation(new Vector3f(55.35316f, -0.27061665f, 27.092093f));
                simpleApplication.getCamera().setRotation(new Quaternion(0.010414706f, 0.9874893f, 0.13880467f, -0.07409228f));

                Material mat = simpleApplication.getAssetManager().loadMaterial("Textures/Terrain/Rocky/Rocky.j3m");
                Spatial scene = simpleApplication.getAssetManager().loadModel("Models/Terrain/Terrain.mesh.xml");
                MikktspaceTangentGenerator.generate(((Geometry) ((Node) scene).getChild(0)).getMesh());
                scene.setMaterial(mat);
                scene.setShadowMode(ShadowMode.CastAndReceive);
                scene.setLocalScale(400);
                scene.setLocalTranslation(0, -10, -120);
                rootNode.attachChild(scene);

                rootNode.attachChild(SkyFactory.createSky(simpleApplication.getAssetManager(),
                        "Textures/Sky/Bright/FullskiesBlueClear03.dds",
                        SkyFactory.EnvMapType.CubeMap));

                DirectionalLight sun = new DirectionalLight();
                Vector3f lightDir = new Vector3f(-0.12f, -0.3729129f, 0.74847335f);
                sun.setDirection(lightDir);
                sun.setColor(ColorRGBA.White.clone().multLocal(2));
                scene.addLight(sun);

                FilterPostProcessor fpp = new FilterPostProcessor(simpleApplication.getAssetManager());

                Vector3f lightPos = lightDir.normalize().negate().multLocal(3000);
                LightScatteringFilter filter = new LightScatteringFilter(lightPos);

                filter.setLightDensity(1.0f);
                filter.setBlurStart(0.02f);
                filter.setBlurWidth(0.9f);
                filter.setLightPosition(lightPos);

                fpp.addFilter(filter);
                simpleApplication.getViewPort().addProcessor(fpp);
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
        }).setFramesToTakeScreenshotsOn(1);
    }
}
