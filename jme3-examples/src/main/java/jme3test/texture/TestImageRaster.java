package jme3test.texture;


import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;

public class TestImageRaster extends SimpleApplication {
    
    private Image convertImage(Image image, Format newFormat) {
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer data = BufferUtils.createByteBuffer( (int)Math.ceil(newFormat.getBitsPerPixel() / 8.0) * width * height);
        Image convertedImage = new Image(newFormat, width, height, data,null, image.getColorSpace());
        
        ImageRaster sourceReader = ImageRaster.create(image);
        ImageRaster targetWriter = ImageRaster.create(convertedImage);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ColorRGBA color = sourceReader.getPixel(x, y);
                targetWriter.setPixel(x, y, color);
            }
        }
        
        return convertedImage;
    }
    
    private void convertAndPutImage(Image image, float posX, float posY) {
        Texture tex = new Texture2D(image);
        tex.setMagFilter(MagFilter.Nearest);
        tex.setMinFilter(MinFilter.NearestNoMipMaps);
        tex.setAnisotropicFilter(16);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);

        Quad q = new Quad(5, 5);
        Geometry g = new Geometry("quad", q);
        g.setLocalTranslation(posX, posY - 5, -0.0001f);
        g.setMaterial(mat);
        rootNode.attachChild(g);

        BitmapFont fnt = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText txt = new BitmapText(fnt);
        txt.setBox(new Rectangle(0, 0, 5, 5));
        txt.setQueueBucket(RenderQueue.Bucket.Transparent);
        txt.setSize(0.5f);
        txt.setText(image.getFormat().name());
        txt.setLocalTranslation(posX, posY, 0);
        rootNode.attachChild(txt);
    }
    
    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(16, 6, 36));
        flyCam.setMoveSpeed(10);
        
        Texture tex = assetManager.loadTexture("com/jme3/app/Monkey.png");
//        Texture tex = assetManager.loadTexture("Textures/HdrTest/Memorial.hdr");
        Image originalImage = tex.getImage();
        
        Image image = convertImage(originalImage, Format.RGBA32F);
        convertAndPutImage(image, 0, 0);
        
        image = convertImage(image, Format.RGB32F);
        convertAndPutImage(image, 5, 0);
        
        image = convertImage(image, Format.RGBA16F);
        convertAndPutImage(image, 10, 0);
        
        image = convertImage(image, Format.RGB16F);
        convertAndPutImage(image, 15, 0);
        
        image = convertImage(image, Format.RGB16F_to_RGB9E5);
        convertAndPutImage(image, 20, 0);
        
        image = convertImage(image, Format.RGB16F_to_RGB111110F);
        convertAndPutImage(image, 25, 0);
        
        image = convertImage(image, Format.RGBA8);
        convertAndPutImage(image, 10, 5);
        
        image = convertImage(image, Format.RGB8);
        convertAndPutImage(image, 15, 5);
        
        image = convertImage(image, Format.ABGR8);
        convertAndPutImage(image, 20, 5);
        
        image = convertImage(image, Format.BGR8);
        convertAndPutImage(image, 25, 5);
        
        image = convertImage(image, Format.ARGB8);
        convertAndPutImage(image, 30, 5);
        
        image = convertImage(image, Format.BGRA8);
        convertAndPutImage(image, 35, 5);
        
        image = convertImage(image, Format.RGB5A1);
        convertAndPutImage(image, 0, 10);
        
        image = convertImage(image, Format.RGB565);
        convertAndPutImage(image, 5, 10);
        
        image = convertImage(image, Format.Luminance32F);
        convertAndPutImage(image, 0, 15);
        
        image = convertImage(image, Format.Luminance16FAlpha16F);
        convertAndPutImage(image, 5, 15);
        
        image = convertImage(image, Format.Luminance16F);
        convertAndPutImage(image, 10, 15);
        
        image = convertImage(image, Format.Luminance8Alpha8);
        convertAndPutImage(image, 15, 15);
        
        image = convertImage(image, Format.Luminance8);
        convertAndPutImage(image, 20, 15);
    }
    
    public static void main(String[] args) {
        TestImageRaster app = new TestImageRaster();
        app.start();
    }
}
