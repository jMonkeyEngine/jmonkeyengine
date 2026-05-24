package org.jmonkeyengine.screenshottests.scenarios.animation;

import static org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase.screenshotTest;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.MotionPathListener;
import com.jme3.cinematic.events.MotionEvent;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;

public class ScenarioMotionPath {

    public static ScreenshotTest testMotionPath() {
        return screenshotTest(new BaseAppState() {
            private Spatial teapot;
            private MotionPath path;
            private MotionEvent motionControl;
            private BitmapText wayPointsText;

            @Override
            protected void initialize(Application app) {
                SimpleApplication simpleApplication = (SimpleApplication) app;
                Node rootNode = simpleApplication.getRootNode();
                Node guiNode = simpleApplication.getGuiNode();
                AssetManager assetManager = simpleApplication.getAssetManager();

                // Set camera position
                app.getCamera().setLocation(new Vector3f(8.4399185f, 11.189463f, 14.267577f));

                // Create the scene
                createScene(rootNode, assetManager);

                // Create the motion path
                path = new MotionPath();
                path.addWayPoint(new Vector3f(10, 3, 0));
                path.addWayPoint(new Vector3f(10, 3, 10));
                path.addWayPoint(new Vector3f(-40, 3, 10));
                path.addWayPoint(new Vector3f(-40, 3, 0));
                path.addWayPoint(new Vector3f(-40, 8, 0));
                path.addWayPoint(new Vector3f(10, 8, 0));
                path.addWayPoint(new Vector3f(10, 8, 10));
                path.addWayPoint(new Vector3f(15, 8, 10));
                path.enableDebugShape(assetManager, rootNode);

                // Create the motion event
                motionControl = new MotionEvent(teapot, path);
                motionControl.setDirectionType(MotionEvent.Direction.PathAndRotation);
                motionControl.setRotation(new Quaternion().fromAngleNormalAxis(-FastMath.HALF_PI, Vector3f.UNIT_Y));
                motionControl.setInitialDuration(10f);
                motionControl.setSpeed(2f);

                // Create text for waypoint notifications
                wayPointsText = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
                wayPointsText.setSize(wayPointsText.getFont().getCharSet().getRenderedSize());
                guiNode.attachChild(wayPointsText);

                // Add listener for waypoint events
                path.addListener(new MotionPathListener() {
                    @Override
                    public void onWayPointReach(MotionEvent control, int wayPointIndex) {
                        if (path.getNbWayPoints() == wayPointIndex + 1) {
                            wayPointsText.setText(control.getSpatial().getName() + " Finished!!! ");
                        } else {
                            wayPointsText.setText(control.getSpatial().getName() + " Reached way point " + wayPointIndex);
                        }
                        wayPointsText.setLocalTranslation(
                                (app.getCamera().getWidth() - wayPointsText.getLineWidth()) / 2,
                                app.getCamera().getHeight(),
                                0);
                    }
                });

                // note that the ChaseCamera is self-initialising, so just creating this object attaches it
                new ChaseCamera(app.getCamera(), teapot);

                // Start the animation automatically
                motionControl.play();
            }

            private void createScene(Node rootNode, AssetManager assetManager) {
                // Create materials
                Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                mat.setFloat("Shininess", 1f);
                mat.setBoolean("UseMaterialColors", true);
                mat.setColor("Ambient", ColorRGBA.Black);
                mat.setColor("Diffuse", ColorRGBA.DarkGray);
                mat.setColor("Specular", ColorRGBA.White.mult(0.6f));

                Material matSoil = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                matSoil.setBoolean("UseMaterialColors", true);
                matSoil.setColor("Ambient", ColorRGBA.Black);
                matSoil.setColor("Diffuse", ColorRGBA.Black);
                matSoil.setColor("Specular", ColorRGBA.Black);

                // Create teapot
                teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
                teapot.setName("Teapot");
                teapot.setLocalScale(3);
                teapot.setMaterial(mat);
                rootNode.attachChild(teapot);

                // Create ground
                Geometry soil = new Geometry("soil", new Box(50, 1, 50));
                soil.setLocalTranslation(0, -1, 0);
                soil.setMaterial(matSoil);
                rootNode.attachChild(soil);

                // Add light
                DirectionalLight light = new DirectionalLight();
                light.setDirection(new Vector3f(0, -1, 0).normalizeLocal());
                light.setColor(ColorRGBA.White.mult(1.5f));
                rootNode.addLight(light);
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
        })
        .setFramesToTakeScreenshotsOn(10, 60);
    }
}
