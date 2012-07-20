package com.jme3.app.state;

import com.jme3.app.Application;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.system.JmeSystem;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScreenshotAppState extends AbstractAppState implements ActionListener, SceneProcessor {

    private static final Logger logger = Logger.getLogger(ScreenshotAppState.class.getName());
    private boolean capture = false;
    private Renderer renderer;
    private ByteBuffer outBuf;
    private String appName;
    private int shotIndex = 0;
    private int width, height;
    
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        if (!super.isInitialized()){
            InputManager inputManager = app.getInputManager();
            inputManager.addMapping("ScreenShot", new KeyTrigger(KeyInput.KEY_SYSRQ));
            inputManager.addListener(this, "ScreenShot");

            List<ViewPort> vps = app.getRenderManager().getPostViews();
            ViewPort last = vps.get(vps.size()-1);
            last.addProcessor(this);

            appName = app.getClass().getSimpleName();
        }
        
        super.initialize(stateManager, app);
    }

    public void onAction(String name, boolean value, float tpf) {
        if (value){
            capture = true;
        }
    }

    public void initialize(RenderManager rm, ViewPort vp) {
        renderer = rm.getRenderer();
        reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
    }

    @Override
    public boolean isInitialized() {
        return super.isInitialized() && renderer != null;
    }

    public void reshape(ViewPort vp, int w, int h) {
        outBuf = BufferUtils.createByteBuffer(w * h * 4);
        width = w;
        height = h;
    }

    public void preFrame(float tpf) {
    }

    public void postQueue(RenderQueue rq) {
    }

    public void postFrame(FrameBuffer out) {
        if (capture){
            capture = false;
            shotIndex++;

            renderer.readFrameBuffer(out, outBuf);
            File file = new File(appName + shotIndex + ".png").getAbsoluteFile();
            OutputStream outStream = null;
            try {
                outStream = new FileOutputStream(file);
                JmeSystem.writeImageFile(outStream, "png", outBuf, width, height);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Error while saving screenshot", ex);
            } finally {
                if (outStream != null){
                    try {
                        outStream.close();
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "Error while saving screenshot", ex);
                    }
                }
            }
        }
    }
}
