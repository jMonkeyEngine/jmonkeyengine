package jme3test.app.state.testsimpleappstate;

import com.jme3.app.state.SimpleAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

/**
 * @author john01dav
 */
public class ExampleSimpleAppStateRotatingCube extends SimpleAppState<TestSimpleAppState>{
    private Geometry box;
    private Quaternion boxRotation;
    private float rotationRadians = 0f;
    
    @Override
    protected void onInit(){
        Material boxMaterial = new Material(getAssetManager(), "/Common/MatDefs/Misc/Unshaded.j3md");
        boxMaterial.setColor("Color", ColorRGBA.Blue);
        
        box = new Geometry("Box", new Box(1, 1, 1));
        box.setMaterial(boxMaterial);
        getRootNode().attachChild(box);
        
        boxRotation = new Quaternion();
    }
    
    @Override
    protected void onDeinit(){
        getRootNode().detachChild(box);
    }

    @Override
    protected void onEnabledUpdate(float tpf){
        rotationRadians += tpf;
        rotationRadians %= FastMath.TWO_PI;
        
        boxRotation.fromAngleAxis(rotationRadians, new Vector3f(0, 1, 0));
        box.setLocalRotation(boxRotation);
    }
    
}
