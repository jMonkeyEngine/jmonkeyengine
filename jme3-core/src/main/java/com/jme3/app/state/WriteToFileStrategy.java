package com.jme3.app.state;

import java.io.File;
import com.jme3.app.state.ScreenshotAppState.Screenshot;
import com.jme3.system.JmeSystem;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kwando
 */
public class WriteToFileStrategy implements ScreenshotAppState.ScreenshotHandler {

    public interface NamingScheme {

        public File filenameForScreenshot(Screenshot screenshot);
    }

    private static final Logger logger = Logger.getLogger(ScreenshotAppState.class.getName());
    private NamingScheme namingScheme;

    public WriteToFileStrategy(NamingScheme namingScheme) {
        if (namingScheme == null) {
            throw new IllegalArgumentException("Namingscheme cannot be nulll");
        }
        this.namingScheme = namingScheme;
    }

    public void screenshotCaptured(Screenshot screenshot) {
        writeScreenshotToFile(screenshot, namingScheme.filenameForScreenshot(screenshot));
    }

    private void writeScreenshotToFile(Screenshot screenshot, File file) {
        writeFile(file, screenshot.getBuffer(), screenshot.getWidth(), screenshot.getHeight());
    }

    private void writeFile(File file, ByteBuffer outBuf, int width, int height) {
        OutputStream outStream = null;
        try {
            outStream = new FileOutputStream(file);
            JmeSystem.writeImageFile(outStream, "png", outBuf, width, height);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error while saving screenshot", ex);
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Error while saving screenshot", ex);
                }
            }
        }
    }
}
