package org.jmonkeyengine.screenshottests.scenarios.model.shape;

import static org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase.screenshotTest;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Quad;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;

public class ScenarioBillboard {

    public static ScreenshotTest testBillboard(Vector3f cameraPosition) {
        return screenshotTest(new BaseAppState() {
            @Override
            protected void initialize(Application app) {
                SimpleApplication simpleApplication = (SimpleApplication) app;
                Node rootNode = simpleApplication.getRootNode();

                // Set up the camera
                simpleApplication.getCamera().setLocation(cameraPosition);
                simpleApplication.getCamera().lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

                // Set background color
                simpleApplication.getViewPort().setBackgroundColor(ColorRGBA.DarkGray);

                // Create grid
                Geometry grid = makeShape(simpleApplication, "DebugGrid", new Grid(21, 21, 2), ColorRGBA.Gray);
                grid.center().move(0, 0, 0);
                rootNode.attachChild(grid);

                // Create billboards with different alignments
                Node node = createBillboard(simpleApplication, BillboardControl.Alignment.Screen, ColorRGBA.Red);
                node.setLocalTranslation(-6f, 0, 0);
                rootNode.attachChild(node);

                node = createBillboard(simpleApplication, BillboardControl.Alignment.Camera, ColorRGBA.Green);
                node.setLocalTranslation(-2f, 0, 0);
                rootNode.attachChild(node);

                node = createBillboard(simpleApplication, BillboardControl.Alignment.AxialY, ColorRGBA.Blue);
                node.setLocalTranslation(2f, 0, 0);
                rootNode.attachChild(node);
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

            private Node createBillboard(SimpleApplication app, BillboardControl.Alignment alignment, ColorRGBA color) {
                Node node = new Node("Parent");
                Quad quad = new Quad(2, 2);
                Geometry g = makeShape(app, alignment.name(), quad, color);
                BillboardControl bc = new BillboardControl();
                bc.setAlignment(alignment);
                g.addControl(bc);
                node.attachChild(g);
                node.attachChild(makeShape(app, "ZAxis", new Arrow(Vector3f.UNIT_Z), ColorRGBA.Blue));
                return node;
            }

            private Geometry makeShape(SimpleApplication app, String name, Mesh shape, ColorRGBA color) {
                Geometry geo = new Geometry(name, shape);
                Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                mat.setColor("Color", color);
                geo.setMaterial(mat);
                return geo;
            }
        }).setFramesToTakeScreenshotsOn(1);
    }
}
