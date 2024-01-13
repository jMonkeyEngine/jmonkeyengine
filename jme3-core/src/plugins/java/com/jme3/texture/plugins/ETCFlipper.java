package com.jme3.texture.plugins;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.jme3.texture.Image.Format;

/**
 * A place-holder for future implementation of ETC texture flipping.
 * 
*/
public class ETCFlipper {
    private static final Logger logger = Logger.getLogger(ETCFlipper.class.getName());
    
    public static ByteBuffer flipETC(ByteBuffer img, int w, int h, Format format) {
        logger.warning("ETC texture flipping is not supported yet");
        return img;
    }
}
