/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jme3test.framegraph.examples;

import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.passes.QueueMergePass;
import com.jme3.renderer.framegraph.passes.BucketPass;
import com.jme3.renderer.framegraph.passes.OutputPass;
import com.jme3.renderer.framegraph.passes.SceneEnqueuePass;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

/**
 *
 * @author codex
 */
public class HelloCustomFrameGraph extends SimpleApplication {
    
    public static void main(String[] args) {
        HelloCustomFrameGraph app = new HelloCustomFrameGraph();
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
        
        SceneEnqueuePass enqueue = fg.add(new SceneEnqueuePass(true, true));
        QueueMergePass merge = fg.add(new QueueMergePass(5));
        BucketPass bucket = fg.add(new BucketPass());
        DownsamplingPass[] downsamples = fg.addLoop(new DownsamplingPass[4],
                (i) -> new DownsamplingPass(), "Input", "Output");
        OutputPass out = fg.add(new OutputPass());
        
        merge.makeInput(enqueue, "Opaque", "Queues[0]");
        merge.makeInput(enqueue, "Sky", "Queues[1]");
        merge.makeInput(enqueue, "Transparent", "Queues[2]");
        merge.makeInput(enqueue, "Gui", "Queues[3]");
        merge.makeInput(enqueue, "Translucent", "Queues[4]");
        
        bucket.makeInput(merge, "Result", "Geometry");
        
        downsamples[0].makeInput(bucket, "Color", "Input");
        
        out.makeInput(downsamples[downsamples.length-1], "Output", "Color");
        out.makeInput(bucket, "Depth", "Depth");
        
        Geometry box = new Geometry("box", new Box(1, 1, 1));
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.Blue);
        box.setMaterial(mat);
        rootNode.attachChild(box);
        
        PointLight pl = new PointLight();
        pl.setPosition(new Vector3f(2, 5, 5));
        pl.setRadius(100);
        rootNode.addLight(pl);
        
        flyCam.setMoveSpeed(15);
        flyCam.setDragToRotate(true);
        viewPort.setBackgroundColor(ColorRGBA.White.mult(0.05f));
        
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }
    
}
