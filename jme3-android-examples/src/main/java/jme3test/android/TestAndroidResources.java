package jme3test.android;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

/**
 * Test case to look for images stored in the Android drawable and mipmap directories.  Image files are
 * stored in the main->res->drawable-xxxx directories and main->res->mipmap-xxxx directories.  The Android OS
 * will choose the best matching image based on the device capabilities.
 *
 * @author iwgeric
 */
public class TestAndroidResources extends SimpleApplication {

    @Override
    public void simpleInitApp() {
        // Create boxes with textures that are stored in the Android Resources Folders
        // Images are stored in multiple Drawable and Mipmap directories.  Android picks the ones that
        // match the device size and density.
        Box box1Mesh = new Box(1, 1, 1);
        Geometry box1 = new Geometry("Monkey Box 1", box1Mesh);
        Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setTexture("ColorMap", assetManager.loadTexture("drawable_monkey.png"));
        box1.setMaterial(mat1);
        box1.setLocalTranslation(-2, 0, 0);
        rootNode.attachChild(box1);

        Box box2Mesh = new Box(1, 1, 1);
        Geometry box2 = new Geometry("Monkey Box 2", box2Mesh);
        Material mat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat2.setTexture("ColorMap", assetManager.loadTexture("mipmap_monkey.png"));
        box2.setMaterial(mat2);
        box2.setLocalTranslation(2, 0, 0);
        rootNode.attachChild(box2);

    }
}
