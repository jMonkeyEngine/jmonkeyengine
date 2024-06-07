package org.jmonkeyengine.screenshottests.gui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.Test;

public class TestBitmapText3D extends ScreenshotTestBase{

    /**
     * This tests both that bitmap text is rendered correctly and that it is
     * wrapped correctly.
     */
    @Test
    public void testBitmapText3D(){

        screenshotTest(
                new BaseAppState(){
                    @Override
                    protected void initialize(Application app){
                        String txtB = "ABCDEFGHIKLMNOPQRSTUVWXYZ1234567890`~!@#$%^&*()-=_+[]\\;',./{}|:<>?";

                        AssetManager assetManager = app.getAssetManager();
                        Node rootNode = ((SimpleApplication)app).getRootNode();

                        Quad q = new Quad(6, 3);
                        Geometry g = new Geometry("quad", q);
                        g.setLocalTranslation(-1.5f, -3, -0.0001f);
                        g.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
                        rootNode.attachChild(g);

                        BitmapFont fnt = assetManager.loadFont("Interface/Fonts/Default.fnt");
                        BitmapText txt = new BitmapText(fnt);
                        txt.setBox(new Rectangle(0, 0, 6, 3));
                        txt.setQueueBucket(RenderQueue.Bucket.Transparent);
                        txt.setSize( 0.5f );
                        txt.setText(txtB);
                        txt.setLocalTranslation(-1.5f,0,0);
                        rootNode.attachChild(txt);
                    }

                    @Override
                    protected void cleanup(Application app){}

                    @Override
                    protected void onEnable(){}

                    @Override
                    protected void onDisable(){}
                }
        ).run();


    }
}
