package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.SpotLight;
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
 * Similar to {@link TestLightControlSpot}, except that the spatial is controlled by the light this
 * time.
 *
 * @author Markil 3
 */
public class TestLightControl2Spot extends SimpleApplication {
    private final Vector3f rotAxis = new Vector3f(Vector3f.UNIT_X);
    private final float[] angles = new float[3];

    private Node lightNode;
    private SpotLight spot;

    public static void main(String[] args) {
        TestLightControl2Spot app = new TestLightControl2Spot();
        app.start();
    }

    public void setupLighting() {
        Geometry lightMdl;
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(2f));
        rootNode.addLight(al);

        spot = new SpotLight();
        spot.setSpotRange(1000);
        spot.setSpotInnerAngle(5 * FastMath.DEG_TO_RAD);
        spot.setSpotOuterAngle(10 * FastMath.DEG_TO_RAD);
        spot.setColor(ColorRGBA.White.mult(10));
        rootNode.addLight(spot);


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
        lightNode.addControl(new LightControl(spot, LightControl.ControlDirection.LightToSpatial));
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
        angles[0] += rotAxis.x * ROT_SPEED * tpf;
        angles[1] += rotAxis.y * ROT_SPEED * tpf;
        angles[2] += rotAxis.z * ROT_SPEED * tpf;
        spot.setDirection(new Quaternion().fromAngles(angles).mult(INIT_DIR));
        super.simpleUpdate(tpf);
        /*
         * Make sure they are equal.
         */
        if (spot.getPosition().subtract(lightNode.getWorldTranslation()).lengthSquared() > 0.1F) {
            System.err.printf("Translation not equal: is %s (%s), needs to be %s\n", lightNode.getWorldTranslation(), lightNode.getLocalTranslation(), spot.getPosition());
        }
        if (FastMath.abs(spot.getDirection().normalize().subtractLocal(lightNode.getWorldRotation().mult(Vector3f.UNIT_Z).negateLocal().normalizeLocal()).lengthSquared()) > .1F) {
            System.err.printf("Rotation not equal: is %s (%s), needs to be %s (%f)\n", lightNode.getWorldRotation().mult(Vector3f.UNIT_Z).normalizeLocal(), lightNode.getLocalRotation().mult(Vector3f.UNIT_Z).normalizeLocal(), spot.getDirection().normalize(), FastMath.abs(spot.getDirection().normalize().subtract(lightNode.getWorldRotation().mult(Vector3f.UNIT_Z).normalizeLocal()).lengthSquared()));
        }
        if (angles[0] >= FULL_ROT || angles[1] >= FULL_ROT || angles[2] >= FULL_ROT) {
            spot.setDirection(INIT_DIR);
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
    }
}
