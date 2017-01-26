package com.jme3.input.vr;

/**
 * The type of VR Head Mounted Device (HMD)
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public enum HmdType {
	
	/**
	 * <a href="https://www.vive.com/fr/">HTC vive</a> Head Mounted Device (HMD).
	 */
    HTC_VIVE, 
    
    /**
     * <a href="https://www3.oculus.com/en-us/rift/">Occulus Rift</a> Head Mounted Device (HMD).
     */
    OCULUS_RIFT, 
    
    /**
     * <a href="http://www.osvr.org/">OSVR</a> generic Head Mounted Device (HMD).
     */
    OSVR, 
    
    /**
     * <a href="https://www.getfove.com/">FOVE</a> Head Mounted Device (HMD).
     */
    FOVE, 
    
    /**
     * <a href="http://www.starvr.com/">STARVR</a> Head Mounted Device (HMD).
     */
    STARVR, 
    
    /**
     * <a href="http://gamefacelabs.com/">GameFace</a> Head Mounted Device (HMD).
     */
    GAMEFACE, 
    
    /**
     * <a href="https://www.playstation.com/en-us/explore/playstation-vr/">PlayStation VR</a> (formely Morpheus) Head Mounted Device (HMD).
     */
    MORPHEUS, 
    
    /**
     * <a href="http://www.samsung.com/fr/galaxynote4/gear-vr/">Samsung GearVR</a> Head Mounted Device (HMD).
     */
    GEARVR, 
    
    /**
     * a null Head Mounted Device (HMD).
     */
    NULL, 
    
    /**
     * a none Head Mounted Device (HMD).
     */
    NONE, 
    
    /**
     * a not referenced Head Mounted Device (HMD).
     */
    OTHER
}