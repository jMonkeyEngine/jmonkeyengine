package jme3test.gui;

import com.jme3.app.SimpleApplication;
import com.jme3.cursors.plugins.CursorConverter;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.texture.Image;
import com.jme3.texture.image.ImageRaster;
import com.jme3.texture.Texture2D;
import com.jme3.math.ColorRGBA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * This test class demonstrate how to change cursor in jME3.
 * 
 * The different methods of {@link com.jme3.cursors.plugins.CursorConverter} will be tested.
 *
 * NOTE: This will not work on Android as it does not support cursors.
 *
 * Cursor test
 * @author MadJack
 */
public class TestCursor extends SimpleApplication {

    final private ArrayList<JmeCursor> cursors = new ArrayList<>();
    private long sysTime;
    private int count = 0;

    public static void main(String[] args){
        TestCursor app = new TestCursor();

        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        // We need the cursor to be visible. If it is not visible the cursor
        // will still be "used" and loaded, you just won't see it on the screen.
        inputManager.setCursorVisible(true);

        /*
         * To make jME3 use a custom cursor it is as simple as putting the
         * image file in an asset directory. Here we use
         * "Textures/GUI/Cursors".
         *
         * For the purpose of this demonstration we load 3 different cursors and
         * switch cursor every 5 seconds, applying differents configurations with
         * {@link com.jme3.cursors.plugins.CursorConverter}.
         *
         * At date of 2026/06/01:
         * 
         * The nyan cat cursor has been made by Sirea. Under the Attribution Required (CC by) license:
         * http://www.rw-designer.com/icon-set/nyan-cat
         *
         * The meme face cursor has been made by Virum64. Released to Public Domain.
         * http://www.rw-designer.com/cursor-set/memes-faces-v64
         *
         * The animated monkey cursor has been made by Pointer Adic. Released to Public Domain:
         * http://www.rw-designer.com/cursor-set/monkey
         * 
         * The three cursor examples have been converted to png format.
         * Checking and following the license restrictions in the process.
         */

        Image memeImage = ((Image) assetManager.loadAsset("Textures/Cursors/meme.png")).deepClone();
        flipVertically(memeImage);
        Texture2D memeTexture = new Texture2D(memeImage);

        Image nyancatImage = ((Image) assetManager.loadAsset("Textures/Cursors/nyancat.png")).deepClone();
        flipVertically(nyancatImage);
        Texture2D nyancatTexture = new Texture2D(nyancatImage);

        //Shows a cursor with default hot spot.
        cursors.add(CursorConverter.fromTexture(memeTexture));

        //Shows a cursor with custom hot spot.
        cursors.add(CursorConverter.fromTexture(nyancatTexture, 10, 10));

        /*
         * For animated cursors. Each frame must be loaded.
         * 
         * Differents configuration with the monkey animated cursor will be
         * showed by changing the frame delays.
         */
        String[] monkeyFramePaths = {
            "Textures/Cursors/monkey/frame_0001.png",
            "Textures/Cursors/monkey/frame_0002.png",
            "Textures/Cursors/monkey/frame_0003.png",
            "Textures/Cursors/monkey/frame_0004.png",
            "Textures/Cursors/monkey/frame_0005.png",
            "Textures/Cursors/monkey/frame_0006.png"
        };

        Texture2D[] monkeyFrames = Arrays.stream(monkeyFramePaths)
          .map(framePath -> ((Image) assetManager.loadAsset(framePath)).deepClone())
          .peek(frameImage -> flipVertically(frameImage))
          .map(frameImage -> new Texture2D(frameImage))
          .toArray(Texture2D[]::new);
        
        //Shows monkey cursor with same frame delay for each frame and default hot spot.
        cursors.add(CursorConverter.fromTextureFrames(monkeyFrames, 60));
        
        //Shows monkey cursor with custom frame delay for each frame and default hot spot.
        cursors.add(CursorConverter.fromTextureFrames(monkeyFrames, new int[]{40, 40, 40, 200, 200, 200}));

        //Shows monkey cursor with same frame delay for each frame and custom hot spot.
        cursors.add(CursorConverter.fromTextureFrames(monkeyFrames, 150, 5, 7));

        //Shows monkey cursor with custom frame delay for each frame and hot spot.
        cursors.add(CursorConverter.fromTextureFrames(monkeyFrames, new int[]{40, 200, 40, 200, 40, 200}, 10, 20));

        sysTime = System.currentTimeMillis();
        inputManager.setMouseCursor(cursors.get(count));
    }

    private void flipVertically(Image image) {
        int height = image.getHeight();
        int width = image.getWidth();

        ImageRaster raster = ImageRaster.create(image);
        
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height / 2; j++){
                ColorRGBA reserve = raster.getPixel(i, j);
                raster.setPixel(i, j, raster.getPixel(i, height - j -1));
                raster.setPixel(i, height - j -1, reserve);
            }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - sysTime > 5000) {
            count++;
            if (count >= cursors.size()) {
                count = 0;
            }
            sysTime = currentTime;
            // 8 seconds have passed,
            // tell jME3 to switch to a different cursor.
            inputManager.setMouseCursor(cursors.get(count));
        }

    }
}

