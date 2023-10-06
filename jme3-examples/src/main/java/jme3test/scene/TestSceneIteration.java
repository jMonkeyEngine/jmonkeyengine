/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jme3test.scene;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphIterator;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

/**
 *
 * @author codex
 */
public class TestSceneIteration extends SimpleApplication {
    
    public static void main(String[] args) {
        new TestSceneIteration().start();
    }
    
    @Override
    public void simpleInitApp() {
        
        // setup scene graph
        Node n1 = new Node("town");
        rootNode.attachChild(n1);
        n1.attachChild(new Node("car"));
        n1.attachChild(new Node("tree"));
        Node n2 = new Node("house");
        n1.attachChild(n2);
        n2.attachChild(new Node("chairs"));
        n2.attachChild(new Node("tables"));
        n2.attachChild(createGeometry("house-geometry"));
        Node n3 = new Node("sky");
        rootNode.attachChild(n3);
        n3.attachChild(new Node("airplane"));
        Node ignore = new Node("ignore");
        n3.attachChild(ignore);
        ignore.attachChild(new Node("this should not be iterated"));
        ignore.attachChild(new Node("this should not be iterated"));
        ignore.attachChild(new Node("this should not be iterated"));
        
        // change this boolean to see the effects of ignoreChildren()
        boolean ignoreThoseThings = true;
        
        // iterate
        SceneGraphIterator iterator = new SceneGraphIterator(rootNode);
        for (Spatial spatial : iterator) {
            // create a hierarchy in the console
            System.out.println(constructTabs(iterator.getDepth()) + spatial.getName());
            // see if the children of this spatial should be ignored
            if (ignoreThoseThings && spatial.getName().equals("ignore")) {
                // ignore all children of this spatial
                iterator.ignoreChildren();
            }
        }
        
    }
    
    private Geometry createGeometry(String name) {
        Geometry g = new Geometry(name, new Box(1, 1, 1));
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", ColorRGBA.Blue);
        g.setMaterial(m);
        return g;
    }
    private String constructTabs(int n) {
        StringBuilder render = new StringBuilder();
        for (; n > 0; n--) {
            render.append(" | ");
        }
        return render.toString();
    }
    
}
