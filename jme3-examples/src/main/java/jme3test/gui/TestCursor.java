package jme3test.gui;

import com.jme3.app.SimpleApplication;
import com.jme3.cursors.plugins.JmeCursor;
import java.util.ArrayList;

/**
 * This test class demonstrate how to change cursor in jME3.
 *
 * NOTE: This will not work on Android as it does not support cursors.
 *
 * Cursor test
 * @author MadJack
 */
public class TestCursor extends SimpleApplication {

    private ArrayList<JmeCursor> cursors = new ArrayList<JmeCursor>();
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
         * .cur/.ico/.ani file in an asset directory. Here we use
         * "Textures/GUI/Cursors".
         *
         * For the purpose of this demonstration we load 3 different cursors and add them
         * into an array list and switch cursor every 8 seconds.
         *
         * The first ico has been made by Sirea and the set can be found here:
         * http://www.rw-designer.com/icon-set/nyan-cat
         *
         * The second cursor has been made by Virum64 and is Public Domain.
         * http://www.rw-designer.com/cursor-set/memes-faces-v64
         *
         * The animated cursor has been made by Pointer Adic and can be found here:
         * http://www.rw-designer.com/cursor-set/monkey
         */
        cursors.add((JmeCursor) assetManager.loadAsset("Textures/Cursors/meme.cur"));
        cursors.add((JmeCursor) assetManager.loadAsset("Textures/Cursors/nyancat.ico"));
        cursors.add((JmeCursor) assetManager.loadAsset("Textures/Cursors/monkey.ani"));

        sysTime = System.currentTimeMillis();
        inputManager.setMouseCursor(cursors.get(count));
    }

    @Override
    public void simpleUpdate(float tpf) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - sysTime > 8000) {
            count++;
            if (count >= cursors.size()) {
                count = 0;
            }
            sysTime = currentTime;
            // 8 seconds have passed,
            // tell jME3 to swith to a different cursor.
            inputManager.setMouseCursor(cursors.get(count));
        }

    }
}

