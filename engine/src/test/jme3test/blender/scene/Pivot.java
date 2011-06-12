package jme3test.blender.scene;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Sphere;

/**
 * The pivot displayed in the scene.
 * @author Marcin Roguski
 */
public class Pivot extends Node {

    public Pivot(AssetManager assetManager) {
        this.attachChild(this.getAxis("x", new Vector3f(10, 0, 0), ColorRGBA.Red, assetManager));
        this.attachChild(this.getAxis("y", new Vector3f(0, 10, 0), ColorRGBA.Green, assetManager));
        this.attachChild(this.getAxis("z", new Vector3f(0, 0, 10), ColorRGBA.Blue, assetManager));
        this.assignPoints(assetManager);
    }

    private void assignPoints(AssetManager assetManager) {
        Material defaultMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        defaultMaterial.setColor("Color", ColorRGBA.DarkGray);
        for (int i = -10; i <= 10; ++i) {
            Geometry g = new Geometry("", new Sphere(3, 3, 0.05f));
            g.setLocalTranslation(i, 0, 0);
            g.setMaterial(defaultMaterial);
            this.attachChild(g);

            g = new Geometry("", new Sphere(3, 3, 0.05f));
            g.setLocalTranslation(0, i, 0);
            g.setMaterial(defaultMaterial);
            this.attachChild(g);

            g = new Geometry("", new Sphere(3, 3, 0.05f));
            g.setLocalTranslation(0, 0, i);
            g.setMaterial(defaultMaterial);
            this.attachChild(g);
        }
    }

    private Geometry getAxis(String name, Vector3f endPoint, ColorRGBA color, AssetManager assetManager) {
        Line axis = new Line(new Vector3f(0, 0, 0), endPoint);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        Geometry geom = new Geometry(name, axis);
        geom.setMaterial(mat);
        return geom;
    }
}