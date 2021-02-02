package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.LightControl;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Dome;
import com.jme3.scene.shape.Sphere;

/**
 * Similar to {@link TestLightControlDirectional}, except that the spatial is controlled by the light this
 * time.
 *
 * @author Markil 3
 */
public class TestLightControl2Directional extends SimpleApplication {
    private final Vector3f rotAxis = new Vector3f(Vector3f.UNIT_X);
    private final float[] angles = new float[3];

    public static void main(String[] args) {
        TestLightControl2Directional app = new TestLightControl2Directional();
        app.start();
    }

    Node lightNode;
    DirectionalLight direction;
    Geometry lightMdl;

    public void setupLighting() {
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(2f));
        rootNode.addLight(al);

        direction = new DirectionalLight();
        direction.setColor(ColorRGBA.White.mult(10));
        rootNode.addLight(direction);


//        PointLight pl=new PointLight();
//      pl.setPosition(new Vector3f(77.70334f, 34.013165f, 27.1017f));
//      pl.setRadius(1000);
//      pl.setColor(ColorRGBA.White.mult(2));
//      rootNode.addLight(pl);
        lightMdl = new Geometry("Light", new Dome(Vector3f.ZERO, 2, 32, 5, false));
        lightMdl.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        lightMdl.setLocalTranslation(new Vector3f(0, 0, 0));
        lightMdl.setLocalRotation(new Quaternion().fromAngles(FastMath.PI / 2F, 0, 0));
        rootNode.attachChild(lightMdl);

        /*
         * We need this Dome doesn't have a "floor."
         */
        Geometry lightFloor = new Geometry("LightFloor", new Cylinder(2, 32, 5, .1F, true));
        lightFloor.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        lightFloor.getMaterial().setColor("Color", ColorRGBA.White);
//        lightFloor.setLocalRotation(new Quaternion().fromAngles(FastMath.PI / 2F, 0, 0));

        lightNode = new Node();
        lightNode.addControl(new LightControl(direction, LightControl.ControlDirection.LightToSpatial));
//        lightNode.setLocalTranslation(-5, 0, 0);
        lightNode.attachChild(lightMdl);
        lightNode.attachChild(lightFloor);

        /*
         * Offset the light node to check global stuff
         */
        Node axis = new Node();
        axis.setLocalTranslation(5, -3, 2.5F);
        axis.setLocalRotation(new Quaternion().fromAngles(FastMath.PI / -4F, FastMath.PI / 2F, 0));
        axis.attachChild(lightNode);
        rootNode.attachChild(lightNode);
    }

    public void setupDome() {
        Geometry dome = new Geometry("Dome", new Sphere(16, 32, 30, false, true));
        dome.setMaterial(new Material(this.assetManager, "Common/MatDefs/Light/PBRLighting.j3md"));
        dome.setLocalTranslation(new Vector3f(0, 0, 0));
        rootNode.attachChild(dome);
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(30);

        setupLighting();
        setupDome();
    }

    @Override
    public void simpleUpdate(float tpf) {
        final Vector3f INIT_DIR = Vector3f.UNIT_Z.negate();
        /*
         * In Radians per second
         */
        final float ROT_SPEED = FastMath.PI * 2;
        /*
         * 360 degree rotation
         */
        final float FULL_ROT = FastMath.PI * 2;
        super.simpleUpdate(tpf);
        angles[0] += rotAxis.x * ROT_SPEED * tpf;
        angles[1] += rotAxis.y * ROT_SPEED * tpf;
        angles[2] += rotAxis.z * ROT_SPEED * tpf;
        direction.setDirection(new Quaternion().fromAngles(angles).mult(INIT_DIR));
        if (angles[0] >= FULL_ROT || angles[1] >= FULL_ROT || angles[2] >= FULL_ROT) {
            direction.setDirection(INIT_DIR);
            angles[0] = 0;
            angles[1] = 0;
            angles[2] = 0;
            if (rotAxis.x > 0 && rotAxis.y == 0 && rotAxis.z == 0) {
                rotAxis.set(0, 1, 0);
            } else if (rotAxis.y > 0 && rotAxis.x == 0 && rotAxis.z == 0) {
                rotAxis.set(0, 0, 1);
            } else if (rotAxis.z > 0 && rotAxis.x == 0 && rotAxis.y == 0) {
                rotAxis.set(FastMath.nextRandomFloat() % 1, FastMath.nextRandomFloat() % 1, FastMath.nextRandomFloat() % 1);
            } else {
                rotAxis.set(1, 0, 0);
            }
        }
//        lightNode.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }
}
