/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.texture;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class TestTexture3D extends SimpleApplication {

    public static void main(String[] args) {
        TestTexture3D app = new TestTexture3D();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        //mouseInput.setCursorVisible(true);
        flyCam.setMoveSpeed(10);
        //creating a sphere
        Sphere sphere = new Sphere(32, 32, 1);
        //getting the boundingbox
        sphere.updateBound();
        BoundingBox bb = (BoundingBox) sphere.getBound();
        Vector3f min = bb.getMin(null);
        float[] ext = new float[]{bb.getXExtent() * 2, bb.getYExtent() * 2, bb.getZExtent() * 2};
        //we need to change the UV coordinates (the sphere is assumet to be inside the 3D image box)
        sphere.clearBuffer(Type.TexCoord);
        VertexBuffer vb = sphere.getBuffer(Type.Position);
        FloatBuffer fb = (FloatBuffer) vb.getData();
        float[] uvCoordinates = BufferUtils.getFloatArray(fb);
        //now transform the coordinates so that they are in the range of <0; 1>
        for (int i = 0; i < uvCoordinates.length; i += 3) {
            uvCoordinates[i] = (uvCoordinates[i] - min.x) / ext[0];
            uvCoordinates[i + 1] = (uvCoordinates[i + 1] - min.y) / ext[1];
            uvCoordinates[i + 2] = (uvCoordinates[i + 2] - min.z) / ext[2];
        }
        //apply new texture coordinates
        VertexBuffer uvCoordsBuffer = new VertexBuffer(Type.TexCoord);
        uvCoordsBuffer.setupData(Usage.Static, 3, com.jme3.scene.VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(uvCoordinates));
        sphere.setBuffer(uvCoordsBuffer);
        //create geometry, and apply material and our 3D texture
        Geometry g = new Geometry("sphere", sphere);
        Material material = new Material(assetManager, "jme3test/texture/tex3D.j3md");
        try {
            Texture texture = this.getTexture();
            material.setTexture("Texture", texture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        g.setMaterial(material);
        rootNode.attachChild(g);
        //add some light so that it is visible
        PointLight light = new PointLight();
        light.setColor(ColorRGBA.White);
        light.setPosition(new Vector3f(5, 5, 5));
        light.setRadius(20);
        rootNode.addLight(light);
        light = new PointLight();
        light.setColor(ColorRGBA.White);
        light.setPosition(new Vector3f(-5, -5, -5));
        light.setRadius(20);
        rootNode.addLight(light);
    }

    /**
         * This method creates a RGB8 texture with the sizes of 10x10x10 pixels.
         */
    private Texture getTexture() throws IOException {
        ArrayList<ByteBuffer> data = new ArrayList<ByteBuffer>(1);
        ByteBuffer bb = BufferUtils.createByteBuffer(10 * 10 * 10 * 3);//all data must be inside one buffer
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10 * 10; ++j) {
                bb.put((byte) (255f*i/10f));
                bb.put((byte) (255f*i/10f));
                bb.put((byte) (255f));
            }
        }
        bb.rewind();
        data.add(bb);
        return new Texture3D(new Image(Format.RGB8, 10, 10, 10, data));
    }
}