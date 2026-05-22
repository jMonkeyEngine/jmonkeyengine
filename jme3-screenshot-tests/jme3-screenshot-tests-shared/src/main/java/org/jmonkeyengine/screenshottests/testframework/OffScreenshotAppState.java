package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.renderer.Renderer;
import com.jme3.system.JmeSystem;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Optional;

public class OffScreenshotAppState extends AbstractAppState{

    private final Texture2D renderTexture;
    private Renderer renderer;
    private final FrameBuffer frameBuffer;
    private Optional<Path> capture = Optional.empty();

    private ByteBuffer outBuf;

    public void takeScreenshot(Path pathToSaveTo) {
        capture = Optional.of(pathToSaveTo);
    }

    public OffScreenshotAppState(Texture2D renderTexture, FrameBuffer frameBuffer) {
        this.renderTexture = renderTexture;
        this.frameBuffer = frameBuffer;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        renderer = app.getRenderManager().getRenderer();
        outBuf = BufferUtils.createByteBuffer(renderTexture.getImage().getWidth() * renderTexture.getImage().getHeight() * 4);
    }

    @Override
    public void postRender() {
        super.postRender();
        if (capture.isPresent()) {

            renderer.readFrameBuffer(frameBuffer, outBuf);
            try (FileOutputStream fileOutBuf = new FileOutputStream(capture.get().toFile())) {
                JmeSystem.writeImageFile(fileOutBuf, "png",outBuf, renderTexture.getImage().getWidth(), renderTexture.getImage().getHeight());
            }catch (IOException e) {
                throw new RuntimeException(e);
            }
            capture = Optional.empty();
        }
    }
}