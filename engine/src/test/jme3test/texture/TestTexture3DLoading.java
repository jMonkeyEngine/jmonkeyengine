/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.texture;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestTexture3DLoading extends SimpleApplication {

    public static void main(String[] args) {
        TestTexture3DLoading app = new TestTexture3DLoading();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        flyCam.setEnabled(false);


        Quad q = new Quad(10, 10);

        Geometry geom = new Geometry("Quad", q);
        Material material = new Material(assetManager, "jme3test/texture/tex3DThumb.j3md");
        TextureKey key = new TextureKey("Textures/3D/flame.dds");
        key.setGenerateMips(true);
        key.setAsTexture3D(true);

        Texture t = assetManager.loadTexture(key);

        int rows = 4;//4 * 4

        q.scaleTextureCoordinates(new Vector2f(rows, rows));

        //The image only have 8 pictures and we have 16 thumbs, the data will be interpolated by the GPU
        material.setFloat("InvDepth", 1f / 16f);
        material.setInt("Rows", rows);
        material.setTexture("Texture", t);
        geom.setMaterial(material);

        rootNode.attachChild(geom);

        cam.setLocation(new Vector3f(4.7444625f, 5.160054f, 13.1939f));
    }
}