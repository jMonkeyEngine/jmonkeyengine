/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jme3test.texture;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureImage;

/**
 *
 * @author codex
 */
public class TestShaderImage extends SimpleApplication {
    
    private int frame = 0;
    
    public static void main(String[] args) {
        new TestShaderImage().start();
    }
    
    @Override
    public void simpleInitApp() {
        
        Geometry box = new Geometry("Box", new Box(1, 1, 1));
        Material mat = new Material(assetManager, "Materials/ImageTest.j3md");
        box.setMaterial(mat);
        rootNode.attachChild(box);
        
        int width = context.getFramebufferWidth();
        int height = context.getFramebufferHeight();
        Texture2D target = new Texture2D(width, height, Image.Format.RGBA8);
        TextureImage targetImage = new TextureImage(target, TextureImage.Access.WriteOnly);
        mat.setParam("TargetImage", VarType.Image2D, targetImage);
        
        Geometry pic = new Geometry("gui_pic", new Quad(200, 200));
        pic.setLocalTranslation(0, height - 200, 0);
        Material picMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        picMat.setTexture("ColorMap", target);
        pic.setMaterial(mat);
        guiNode.attachChild(pic);
        
    }
    @Override
    public void simpleUpdate(float tpf) {
        if (frame++ < 5) {
            cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        }
    }
    
}
