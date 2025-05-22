package jme3test.math;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.debug.WireSphere;
import com.jme3.scene.shape.Sphere;

/**
 * @author capdevon
 */
public class TestRandomPoints extends SimpleApplication {

    public static void main(String[] args) {
        TestRandomPoints app = new TestRandomPoints();
        app.start();
    }

    private float radius = 5;

    @Override
    public void simpleInitApp() {
        configureCamera();
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);

        Geometry grid = makeShape("DebugGrid", new Grid(21, 21, 2), ColorRGBA.LightGray);
        grid.center().move(0, 0, 0);
        rootNode.attachChild(grid);

        Geometry bsphere = makeShape("BoundingSphere", new WireSphere(radius), ColorRGBA.Red);
        rootNode.attachChild(bsphere);

        for (int i = 0; i < 100; i++) {
            Vector2f v = FastMath.insideUnitCircle().multLocal(radius);
            Arrow arrow = new Arrow(Vector3f.UNIT_Y.negate());
            Geometry geo = makeShape("Arrow." + i, arrow, ColorRGBA.Green);
            geo.setLocalTranslation(new Vector3f(v.x, 0, v.y));
            rootNode.attachChild(geo);
        }

        for (int i = 0; i < 100; i++) {
            Vector3f v = FastMath.insideUnitSphere().multLocal(radius);
            Geometry geo = makeShape("Sphere." + i, new Sphere(16, 16, 0.05f), ColorRGBA.Blue);
            geo.setLocalTranslation(v);
            rootNode.attachChild(geo);
        }

        for (int i = 0; i < 100; i++) {
            Vector3f v = FastMath.onUnitSphere().multLocal(radius);
            Geometry geo = makeShape("Sphere." + i, new Sphere(16, 16, 0.06f), ColorRGBA.Cyan);
            geo.setLocalTranslation(v);
            rootNode.attachChild(geo);
        }

        for (int i = 0; i < 100; i++) {
            float value = FastMath.nextRandomFloat(-5, 5);
            System.out.println(value);
        }
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(15f);
        flyCam.setDragToRotate(true);

        cam.setLocation(Vector3f.UNIT_XYZ.mult(12));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    private Geometry makeShape(String name, Mesh mesh, ColorRGBA color) {
        Geometry geo = new Geometry(name, mesh);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geo.setMaterial(mat);
        return geo;
    }

}
