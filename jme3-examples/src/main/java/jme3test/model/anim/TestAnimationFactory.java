package jme3test.model.anim;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimFactory;
import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.util.TangentBinormalGenerator;

public class TestAnimationFactory extends SimpleApplication {

    public static void main(String[] args) {
        TestAnimationFactory app = new TestAnimationFactory();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        AmbientLight al = new AmbientLight();
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(Vector3f.UNIT_XYZ.negate());
        rootNode.addLight(dl);

        // Create model
        Box box = new Box(1, 1, 1);
        Geometry geom = new Geometry("box", box);
        geom.setMaterial(assetManager.loadMaterial("Textures/Terrain/BrickWall/BrickWall.j3m"));
        Node model = new Node("model");
        model.attachChild(geom);

        Box child = new Box(0.5f, 0.5f, 0.5f);
        Geometry childGeom = new Geometry("box", child);
        childGeom.setMaterial(assetManager.loadMaterial("Textures/Terrain/BrickWall/BrickWall.j3m"));
        Node childModel = new Node("child model");
        childModel.setLocalTranslation(2, 2, 2);
        childModel.attachChild(childGeom);
        model.attachChild(childModel);
        TangentBinormalGenerator.generate(model);

        // Construct a complex animation using AnimFactory:
        // 6 seconds in duration, named "anim", running at 25 frames per second
        AnimFactory animationFactory = new AnimFactory(6f, "anim", 25f);
        
        // Create a translation keyFrame at time = 3 with a translation on the X axis of 5 WU.
        animationFactory.addTimeTranslation(3, new Vector3f(5, 0, 0));
        //resetting the translation to the start position at time = 6
        animationFactory.addTimeTranslation(6, new Vector3f(0, 0, 0));

        //Creating a scale keyFrame at time = 2 with the unit scale.
        animationFactory.addTimeScale(2, new Vector3f(1, 1, 1));
        //Creating a scale keyFrame at time = 4 scaling to 1.5
        animationFactory.addTimeScale(4, new Vector3f(1.5f, 1.5f, 1.5f));
        //resetting the scale to the start value at time = 5
        animationFactory.addTimeScale(5, new Vector3f(1, 1, 1));

        
        //Creating a rotation keyFrame at time = 0.5 of quarter PI around the Z axis
        animationFactory.addTimeRotation(0.5f,new Quaternion().fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_Z));
        //rotating back to initial rotation value at time = 1
        animationFactory.addTimeRotation(1,Quaternion.IDENTITY);
        /*
         * Perform a 360-degree rotation around the X axis between t=1 and t=2.
         */
        for (int i = 1; i <= 3; ++i) {
            float rotTime = i / 3f;
            float xAngle = FastMath.TWO_PI * rotTime;
            animationFactory.addTimeRotation(1f + rotTime, xAngle, 0f, 0f);
        }

        AnimClip clip = animationFactory.buildAnimation(model);
        AnimComposer control = new AnimComposer();
        control.addAnimClip(clip);
        model.addControl(control);

        rootNode.attachChild(model);

        //run animation
        control.setCurrentAction("anim");
    }
}
