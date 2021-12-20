package jme3test.texture;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Limits;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.RectangleMesh;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;

public class TestAnisotropicFilter extends SimpleApplication implements ActionListener {

    private int globalAniso = 1;
    private int maxAniso = 1;

    @Override
    public void simpleInitApp() {
        maxAniso = renderer.getLimits().get(Limits.TextureAnisotropy);

        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(100);
        cam.setLocation(new Vector3f(197.02617f, 4.6769195f, -194.89545f));
        cam.setRotation(new Quaternion(0.07921988f, 0.8992258f, -0.18292196f, 0.38943136f));

        RectangleMesh rm = new RectangleMesh(
                new Vector3f(-500, 0, 500),
                new Vector3f(500, 0, 500),
                new Vector3f(-500, 0, -500));
        rm.scaleTextureCoordinates(new Vector2f(1000, 1000));
        Geometry geom = new Geometry("rectangle", rm);
        geom.setMaterial(createCheckerBoardMaterial(assetManager));
        rootNode.attachChild(geom);

        inputManager.addMapping("higher", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("lower", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addListener(this, "higher");
        inputManager.addListener(this, "lower");
    }
    
    private static Material createCheckerBoardMaterial(AssetManager assetManager) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = createCheckerBoardTexture(); // assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.dds");
        tex.setMagFilter(Texture.MagFilter.Bilinear);
        tex.setMinFilter(Texture.MinFilter.Trilinear);
        tex.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("ColorMap", tex);
        return mat;
    }
    
    private static Texture2D createCheckerBoardTexture() {
        Image image = new Image(Format.RGBA8, 1024, 1024, BufferUtils.createByteBuffer(1024 * 1024 * 4), ColorSpace.sRGB);
        
        ImageRaster raster = ImageRaster.create(image);
        for (int y = 0; y < 1024; y++) {
            for (int x = 0; x < 1024; x++) {
                if (y < 512) {
                    if (x < 512) {
                        raster.setPixel(x, y, ColorRGBA.Black);
                    } else {
                        raster.setPixel(x, y, ColorRGBA.White);
                    }
                } else {
                    if (x < 512) {
                        raster.setPixel(x, y, ColorRGBA.White);
                    } else {
                        raster.setPixel(x, y, ColorRGBA.Black);
                    }
                }
            }
        }

        return new Texture2D(image);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
            return;
        }
        switch (name) {
            case "higher":
                globalAniso++;
                if (globalAniso > 32) {
                    globalAniso = 32;
                }
                renderer.setDefaultAnisotropicFilter(globalAniso);
                System.out.format("Global Aniso: %d / %d\r\n", globalAniso, maxAniso);
                break;
            case "lower":
                globalAniso--;
                if (globalAniso < 1) {
                    globalAniso = 1;
                }
                renderer.setDefaultAnisotropicFilter(globalAniso);
                System.out.format("Global Aniso: %d / %d\r\n", globalAniso, maxAniso);
                break;
        }
    }

    public static void main(String[] args) {
        TestAnisotropicFilter app = new TestAnisotropicFilter();
        app.start();
    }
}
