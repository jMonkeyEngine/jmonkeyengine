package org.jmonkeyengine.screenshottests.export;

import com.jme3.anim.AnimComposer;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TestOgreConvert extends ScreenshotTestBase{

    /**
     * This tests loads an Ogre model, converts it to binary, and then reloads it.
     * <p>
     * Note that the model is animated and the animation is played back. That is why
     * two screenshots are taken
     * </p>
     */
    @Test
    public void testOgreConvert(){

        screenshotTest(
                new BaseAppState(){
                    @Override
                    protected void initialize(Application app){
                        AssetManager assetManager = app.getAssetManager();
                        Node rootNode = ((SimpleApplication)app).getRootNode();
                        Camera cam = app.getCamera();
                        Spatial ogreModel = assetManager.loadModel("Models/Oto/Oto.mesh.xml");

                        DirectionalLight dl = new DirectionalLight();
                        dl.setColor(ColorRGBA.White);
                        dl.setDirection(new Vector3f(0,-1,-1).normalizeLocal());
                        rootNode.addLight(dl);

                        cam.setLocation(new Vector3f(0, 0, 15));

                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            BinaryExporter exp = new BinaryExporter();
                            exp.save(ogreModel, baos);

                            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                            BinaryImporter imp = new BinaryImporter();
                            imp.setAssetManager(assetManager);
                            Node ogreModelReloaded = (Node) imp.load(bais, null, null);

                            AnimComposer composer = ogreModelReloaded.getControl(AnimComposer.class);
                            composer.setCurrentAction("Walk");

                            rootNode.attachChild(ogreModelReloaded);
                        } catch (IOException ex){
                            throw new RuntimeException(ex);
                        }
                    }

                    @Override
                    protected void cleanup(Application app){}

                    @Override
                    protected void onEnable(){}

                    @Override
                    protected void onDisable(){}
                }
        )
        .setFramesToTakeScreenshotsOn(1, 5)
        .run();

    }
}
