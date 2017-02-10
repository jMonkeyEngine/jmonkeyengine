package com.jme3.input.vr;

import com.jme3.math.Vector2f;
import com.jme3.system.jopenvr.JOpenVRLibrary;
import com.jme3.system.jopenvr.VR_IVRChaperone_FnTable;
import com.sun.jna.ptr.FloatByReference;

import java.util.logging.Logger;

/**
 * A class that represents VR world bounds.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public class VRBounds {

	private static Logger logger = Logger.getLogger(VRBounds.class.getName());
	
    private static VR_IVRChaperone_FnTable vrChaperone;
    private static Vector2f playSize;
    
    /**
     * Initialize the VR bounds.
     * @return <code>true</code> if the initialization is a success and <code>false</code> otherwise.
     */
    public static boolean init() {
    	
    	logger.config("Initialize VR bounds...");
    	
        if( vrChaperone == null ) {
            vrChaperone = new VR_IVRChaperone_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRChaperone_Version, OpenVR.hmdErrorStore).getPointer());
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
    
    /**
     * Get the size of the VR world.
     * @return the size of the VR world.
     */
    public static Vector2f getPlaySize() {
        return playSize;
    }
    
}

