package com.jme3.input.vr.lwjgl_openvr;

import com.jme3.input.vr.VRAPI;
import com.jme3.input.vr.VRBounds;
import com.jme3.math.Vector2f;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;

import java.util.logging.Logger;

/**
 * A class that represents VR world bounds.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 * @author Rickard Edén
 */
public class LWJGLOpenVRBounds implements VRBounds {
    private static Logger logger = Logger.getLogger(LWJGLOpenVRBounds.class.getName());

    private Vector2f playSize;
    private boolean setup = false;

    /**
     * Initialize the VR bounds.
     * @return <code>true</code> if the initialization is a success and <code>false</code> otherwise.
     */
    public boolean init(VRAPI api) {
        logger.config("Initialize VR bounds...");

        if( !setup ) {
//            vrChaperone = new VR_IVRChaperone_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRChaperone_Version, api.hmdErrorStore).getPointer());
            FloatBuffer fbX = BufferUtils.createFloatBuffer(1);
            FloatBuffer fbZ = BufferUtils.createFloatBuffer(1);
            org.lwjgl.openvr.VRChaperone.VRChaperone_GetPlayAreaSize(fbX, fbZ);

            playSize = new Vector2f(fbX.get(0), fbZ.get(0));
            setup = true;
            logger.config("Initialize VR bounds [SUCCESS]");
            return true; // init success
        }

        logger.config("Initialize VR bounds already done.");
        return true; // already initialized
    }

    @Override
    public Vector2f getPlaySize() {
        return playSize;
    }
}