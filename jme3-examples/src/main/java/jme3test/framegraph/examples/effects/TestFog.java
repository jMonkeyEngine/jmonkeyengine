/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jme3test.framegraph.examples.effects;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.post.framegraph.HazePass;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.PassIndex;
import com.jme3.renderer.framegraph.passes.GeometryPass;
import com.jme3.renderer.framegraph.passes.OutputPass;
import com.jme3.renderer.framegraph.passes.QueueMergePass;
import com.jme3.renderer.framegraph.passes.SceneEnqueuePass;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;

/**
 *
 * @author codex
 */
public class TestFog extends SimpleApplication {

    public static void main(String[] args) {
        TestFog app = new TestFog();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(700);
        settings.setHeight(700);
        app.setSettings(settings);
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        
        FrameGraph fg = new FrameGraph(assetManager);
        viewPort.setFrameGraph(fg);
        //viewPort.setFrameGraph(FrameGraphFactory.forward(assetManager));
        
        SceneEnqueuePass enqueue = fg.add(new SceneEnqueuePass(true, true));
        QueueMergePass merge = fg.add(new QueueMergePass(5));
        GeometryPass geom = fg.add(new GeometryPass());
        HazePass haze = fg.add(new HazePass());
        OutputPass out = fg.add(new OutputPass());
        OutputPass out2 = fg.add(new OutputPass());
        
        merge.makeInput(enqueue, "Opaque", "Queues[0]");
        merge.makeInput(enqueue, "Sky", "Queues[1]");
        merge.makeInput(enqueue, "Transparent", "Queues[2]");
        merge.makeInput(enqueue, "Gui", "Queues[3]");
        merge.makeInput(enqueue, "Translucent", "Queues[4]");
        
        geom.makeInput(merge, "Result", "Geometry");
        
        haze.makeInput(geom, "Color", "Color");
        haze.makeInput(geom, "Depth", "Depth");
        
        out.makeInput(haze, "Result", "Color");
        out.makeInput(geom, "Depth", "Depth");
        
        //out2.makeInput(sky, "Color", "Color");
        //out2.makeInput(sky, "Depth", "Depth");
        
        flyCam.setMoveSpeed(100);
        flyCam.setDragToRotate(true);
        
        for (int i = 0; i < 1000; i++) {
            Geometry g = new Geometry("cube", new Box(1, 1, 1));
            g.setLocalTranslation(
                FastMath.nextRandomFloat(-100, 100),
                FastMath.nextRandomFloat(-100, 100),
                FastMath.nextRandomFloat(-100, 100));
            g.setLocalRotation(new Quaternion().fromAngles(
                FastMath.nextRandomFloat(0, FastMath.TWO_PI),
                FastMath.nextRandomFloat(0, FastMath.TWO_PI),
                FastMath.nextRandomFloat(0, FastMath.TWO_PI)));
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.randomColor());
            g.setMaterial(mat);
            rootNode.attachChild(g);
        }
        
        Spatial skyBox = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(skyBox);
        
    }
    
}
