/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jme3test.renderpath;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.util.LinkedList;

/**
 * Temporary test to narrow down a bug involving render to texture.
 * 
 * @author codex
 */
public class TestRTT extends SimpleApplication {
    
    private FrameBuffer fbo;
    private Material targetMat1, targetMat2, targetMat3;
    private Texture targetTex;
    private final LinkedList<Texture> stack = new LinkedList<>();
    private final int frameDelay = 50;
    private int frame = frameDelay;
    
    private static final int w = 768, h = 768;
    
    public static void main(String[] args) {
        TestRTT app = new TestRTT();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(w);
        settings.setHeight(h);
        app.setSettings(settings);
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        
        flyCam.setDragToRotate(true);
        guiViewPort.setClearFlags(true, true, true);
        guiViewPort.setBackgroundColor(ColorRGBA.Green.mult(.05f));
        
        fbo = new FrameBuffer(w, h, 1);
        viewPort.setOutputFrameBuffer(fbo);
        
        Geometry g = new Geometry("box", new Box(1, 1, 1));
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", ColorRGBA.Blue);
        g.setMaterial(m);
        rootNode.attachChild(g);
        
        Geometry quad1 = new Geometry("quad1", new Quad(300, 300));
        quad1.setLocalTranslation(70, 410, 0);
        targetMat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        quad1.setMaterial(targetMat1);
        guiNode.attachChild(quad1);
        
        Geometry quad2 = new Geometry("quad2", new Quad(300, 300));
        quad2.setLocalTranslation(380, 410, 0);
        targetMat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        quad2.setMaterial(targetMat2);
        guiNode.attachChild(quad2);
        
        Geometry quad3 = new Geometry("quad3", new Quad(300, 300));
        quad3.setLocalTranslation(310, 50, 0);
        targetMat3 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        quad3.setMaterial(targetMat3);
        guiNode.attachChild(quad3);
        
        stack.add(new Texture2D(w, h, Image.Format.RGBA8));
        stack.add(new Texture2D(w, h, Image.Format.RGBA8));
        
    }
    @Override
    public void simpleUpdate(float tpf) {
        if (frame++ >= frameDelay) {
            frame = 0;
            if (targetTex == stack.getFirst()) {
                targetTex = stack.getLast();
            } else {
                targetTex = stack.getFirst();
            }
            targetMat1.setTexture("ColorMap", stack.getFirst());
            targetMat2.setTexture("ColorMap", stack.getLast());
            targetMat3.setTexture("ColorMap", targetTex);
            fbo.clearColorTargets();
            fbo.addColorTarget(FrameBuffer.target(targetTex));
            // this fixes the entire issue
            fbo.setUpdateNeeded();
        }
    }
    
}
