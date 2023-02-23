package com.jme3.input.vr.openvr;

import com.jme3.input.vr.VRBounds;
import com.jme3.math.Vector2f;
import com.jme3.system.jopenvr.JOpenVRLibrary;
import com.jme3.system.jopenvr.VR_IVRChaperone_FnTable;
import com.sun.jna.ptr.FloatByReference;

import java.util.logging.Logger;

/**
 * A class that represents VR world bounds.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 */
public class OpenVRBounds implements VRBounds {
    private static Logger logger = Logger.getLogger(OpenVRBounds.class.getName());

    private VR_IVRChaperone_FnTable vrChaperone;
    private Vector2f playSize;

    /**
     * Initialize the VR bounds.
     * @return <code>true</code> if the initialization is a success and <code>false</code> otherwise.
     */
    public boolean init(OpenVR api) {
        logger.config("Initialize VR bounds...");

        if( vrChaperone == null ) {
            vrChaperone = new VR_IVRChaperone_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRChaperone_Version, api.hmdErrorStore).getPointer());
            if( vrChaperone != null ) {
                vrChaperone.setAutoSynch(false);
                vrChaperone.read();
                FloatByReference fbX = new FloatByReference();
                FloatByReference fbZ = new FloatByReference();
                vrChaperone.GetPlayAreaSize.apply(fbX, fbZ);
                playSize = new Vector2f(fbX.getValue(), fbZ.getValue());

                logger.config("Initialize VR bounds [SUCCESS]");
                return true; // init success
            }

            logger.warning("Initialize VR bounds [FAILED].");
            return false; // failed to init
        }

        logger.config("Initialize VR bounds already done.");
        return true; // already initialized
    }

    @Override
    public Vector2f getPlaySize() {
        return playSize;
    }
}

