package com.jme3.input.vr.openvr;

import com.jme3.input.vr.VRBounds;
import com.jme3.math.Vector2f;
import org.lwjgl.openvr.VRChaperone;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.logging.Logger;

import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * A class that represents VR world bounds.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 */
public class OpenVRBounds implements VRBounds {

    private static Logger logger = Logger.getLogger(OpenVRBounds.class.getName());

    private Vector2f playSize;

      /**
       * Initialize the VR bounds.
       * @return <code>true</code> if the initialization is a success and <code>false</code> otherwise.
       */
    public boolean init(OpenVR api) {
        logger.config("Initialize VR bounds...");

        try (MemoryStack stack = stackPush()) {
            FloatBuffer fbX = stack.mallocFloat(1);
            FloatBuffer fbZ = stack.mallocFloat(1);

            VRChaperone.VRChaperone_GetPlayAreaSize(fbX, fbZ);

            playSize = new Vector2f(fbX.get(0), fbZ.get(0));

            logger.config("Initialize VR bounds [SUCCESS]");
            return true; // init success
        }
    }

    @Override
    public Vector2f getPlaySize() {
        return playSize;
    }

}

