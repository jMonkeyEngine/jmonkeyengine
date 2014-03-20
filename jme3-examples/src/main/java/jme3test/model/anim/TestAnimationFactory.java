package jme3test.model.anim;

import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimationFactory;
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
        TestSpatialAnim app = new TestSpatialAnim();
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
        Node childModel = new Node("childmodel");
        childModel.setLocalTranslation(2, 2, 2);
        childModel.attachChild(childGeom);
        model.attachChild(childModel);
        TangentBinormalGenerator.generate(model);

        //creating quite complex animation witht the AnimationHelper
        // animation of 6 seconds named "anim" and with 25 frames per second
        AnimationFactory animationFactory = new AnimationFactory(6, "anim", 25);
        
        //creating a translation keyFrame at time = 3 with a translation on the x axis of 5 WU        
        animationFactory.addTimeTranslation(3, new Vector3f(5, 0, 0));
        //reseting the translation to the start position at time = 6
        animationFactory.addTimeTranslation(6, new Vector3f(0, 0, 0));

        //Creating a scale keyFrame at time = 2 with the unit scale.
        animationFactory.addTimeScale(2, new Vector3f(1, 1, 1));
        //Creating a scale keyFrame at time = 4 scaling to 1.5
        animationFactory.addTimeScale(4, new Vector3f(1.5f, 1.5f, 1.5f));
        //reseting the scale to the start value at time = 5
        animationFactory.addTimeScale(5, new Vector3f(1, 1, 1));

        
        //Creating a rotation keyFrame at time = 0.5 of quarter PI around the Z axis
        animationFactory.addTimeRotation(0.5f,new Quaternion().fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_Z));
        //rotating back to initial rotation value at time = 1
        animationFactory.addTimeRotation(1,Quaternion.IDENTITY);
        //Creating a rotation keyFrame at time = 2. Note that i used the Euler angle version because the angle is higher than PI
        //this should result in a complete revolution of the spatial around the x axis in 1 second (from 1 to 2)
        animationFactory.addTimeRotationAngles(2, FastMath.TWO_PI,0, 0);


        AnimControl control = new AnimControl();
        control.addAnim(animationFactory.buildAnimation());

        model.addControl(control);

        rootNode.attachChild(model);

        //run animation
        control.createChannel().setAnim("anim");
    }
}
