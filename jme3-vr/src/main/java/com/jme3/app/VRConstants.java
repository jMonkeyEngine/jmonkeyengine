package com.jme3.app;

import java.util.HashMap;

import com.jme3.system.AppSettings;

/**
 * Some constants dedicated to the VR module.
 * @author Julien Seinturier - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 * @since 3.1.0
 */
public class VRConstants {
	
	/**
	 * An AppSettings parameter that set if the VR compositor has to be used.
	 * <p>
	 * <b>Type: </b><code>boolean</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_USE_COMPOSITOR, value)</code>
	 */
    public static final String SETTING_USE_COMPOSITOR = "VRUseCompositor";
    
    /**
     * An AppSettings parameter that set if the rendering has to use two eyes, 
     * regardless of VR API detection (turning this setting on without a VR system should lead to errors).
     * <p>
	 * <b>Type: </b><code>boolean</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_VR_FORCE, value)</code>

     */
    public static final String SETTING_VR_FORCE = "VRForce";
    
    /**
     * An AppSettings parameter that set to invert the eyes of the HMD.
     * <b>Type: </b><code>boolean</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_FLIP_EYES, value)</code>
     */
    public static final String SETTING_FLIP_EYES = "VRFlipEyes";
    
    /**
     * An AppSettings parameter that set if the GUI has to be displayed even if it is behind objects.
     * <b>Type: </b><code>boolean</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_GUI_OVERDRAW, value)</code>
     * 
     */
    public static final String SETTING_GUI_OVERDRAW = "VRGUIOverdraw";
    
    /**
     * An AppSettings parameter that set if the GUI surface has to be curved.
     * <b>Type: </b><code>boolean</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_GUI_CURVED_SURFACE, value)</code>
     */
    public static final String SETTING_GUI_CURVED_SURFACE = "VRGUICurvedSurface";
  
    /**
     * An AppSettings parameter that set if a mirror rendering has to be displayed on the screen. 
     * Runs faster when set to <code>false</code>.
     * <b>Type: </b><code>boolean</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_ENABLE_MIRROR_WINDOW, value)</code>
     */
    public static final String SETTING_ENABLE_MIRROR_WINDOW = "VREnableMirrorWindow";
    
    /**
     * An AppSettings parameter that set if the VR rendering has to be disabled, 
     * regardless VR API and devices are presents.
     * <b>Type: </b><code>boolean</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_DISABLE_VR, value)</code>
     */
    public static final String SETTING_DISABLE_VR = "VRDisable";
    
    
    /**
     * An AppSettings parameter that set if the VR user is seated.
     * <b>Type: </b><code>boolean</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_SEATED_EXPERIENCE, value)</code>
     */
    public static final String SETTING_SEATED_EXPERIENCE = "VRSeatedExperience";
    
    /**
     * An AppSettings parameter that set if the GUI has to be ignored.
     * <b>Type: </b><code>boolean</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_NO_GUI, value)</code>
     */
    public static final String SETTING_NO_GUI = "VRNoGUI";
    
    /**
     * An AppSettings parameter that set if instance rendering has to be used. 
     * This setting requires some vertex shader changes (see Common/MatDefs/VR/Unshaded.j3md).
     * <b>Type: </b><code>boolean</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_INSTANCE_RENDERING, value)</code>
     */
    public static final String SETTING_INSTANCE_RENDERING = "VRInstanceRendering";
    
    /**
     * An AppSettings parameter that set if Multi Sample Anti Aliasing has to be enabled.
     * <b>Type: </b><code>boolean</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_DISABLE_MSAA, value)</code>
     */
    public static final String SETTING_DISABLE_MSAA = "VRDisableMSAA";
    
    /**
     * An AppSettings parameter that set the default field of view (FOV) value.
     * <b>Type: </b><code>float</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_DEFAULT_FOV, value)</code>
     */
    public static final String SETTING_DEFAULT_FOV = "VRDefaultFOV";
    
    /**
     * An AppSettings parameter that set the default aspect ratio.
     * <b>Type: </b><code>float</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_DEFAULT_ASPECT_RATIO, value)</code>
     */
    public static final String SETTING_DEFAULT_ASPECT_RATIO = "VRDefaultAspectRatio";
    
    /**
     * An AppSettings parameter that specifies the underlying VR API. Possible values are:<br>
     * <ul>
     * <li>{@link VRConstants#SETTING_VRAPI_OPENVR_VALUE SETTING_VRAPI_OPENVR_VALUE}: Use OpenVR binding.
     * <li>{@link VRConstants#SETTING_VRAPI_OSVR_VALUE SETTING_VRAPI_OSVR_VALUE}: Use OSVR binding.
     * <li>{@link VRConstants#SETTING_VRAPI_OPENVR_LWJGL_VALUE SETTING_VRAPI_OPENVR_LWJGL_VALUE}: Use OpenVR binding from LWJGL.
     * </ul>
     * <b>Type: </b><code>int</code><br>
	 * <b>Usage: </b><code>{@link AppSettings appSettings}.{@link HashMap#put(Object, Object) put}(VRConstants.SETTING_VRAPI, value)</code>

     */
    public static final String SETTING_VRAPI = "VRAPI";
    
    /**
     * The identifier of the OpenVR system.
     * @see #SETTING_VRAPI
     */
    public static final int SETTING_VRAPI_OPENVR_VALUE       = 1;
    
    /**
     * The identifier of the OSVR system.
     * @see #SETTING_VRAPI
     */
    public static final int SETTING_VRAPI_OSVR_VALUE         = 2;
    
    /**
     * The identifier of the OpenVR from LWJGL system.
     * @see #SETTING_VRAPI
     */
    public static final int SETTING_VRAPI_OPENVR_LWJGL_VALUE = 3;
    
    
    
}
