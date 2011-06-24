package com.jme3.system;


import android.app.Activity;
import android.content.res.Resources;
import com.jme3.util.AndroidLogHandler;
import com.jme3.asset.AndroidAssetManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioParam;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.Environment;
import com.jme3.audio.Listener;
import com.jme3.audio.ListenerParam;
import com.jme3.audio.android.AndroidAudioRenderer;
//import com.jme3.audio.DummyAudioRenderer;
import com.jme3.system.JmeContext.Type;
import com.jme3.system.android.OGLESContext;
import com.jme3.util.JmeFormatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.net.URL;



public class JmeSystem 
{

    public static enum Platform {

        /**
         * Microsoft Windows 32 bit
         */
        Windows32,

        /**
         * Microsoft Windows 64 bit
         */
        Windows64,

        /**
         * Linux 32 bit
         */
        Linux32,


        /**
         * Linux 64 bit
         */
        Linux64,

        /**
         * Apple Mac OS X 32 bit
         */
        MacOSX32,

        /**
         * Apple Mac OS X 64 bit
         */
        MacOSX64,

        /**
         * Apple Mac OS X 32 bit PowerPC
         */
        MacOSX_PPC32,

        /**
         * Apple Mac OS X 64 bit PowerPC
         */
        MacOSX_PPC64,
        
        /**
         * Android 2.2
         */
        Android_Froyo,
        
        /**
         * Android 2.3
         */
        Android_Gingerbread,
        
        /**
         * Android 3.0
         */
        Android_Honeycomb,
        
        
    }

    private static final Logger logger = Logger.getLogger(JmeSystem.class.getName());

    private static boolean initialized = false;
    private static boolean lowPermissions = false;
    private static Resources res;
    private static Activity activity;

    public static void initialize(AppSettings settings)
    {
        
        if (initialized)
            return;

        initialized = true;
        try 
        {
            JmeFormatter formatter = new JmeFormatter();

            Handler consoleHandler = new AndroidLogHandler();
            consoleHandler.setFormatter(formatter);
        } 
        catch (SecurityException ex)
        {
            logger.log(Level.SEVERE, "Security error in creating log file", ex);
        }
        logger.info("Running on "+getFullName());
    }

    public static String getFullName()
    {
        return "jMonkey Engine 3 ALPHA 0.7 Android";
    }
    
    public static void setLowPermissions(boolean lowPerm)
    {
        lowPermissions = lowPerm;
    }

    public static boolean isLowPermissions() 
    {
        return lowPermissions;
    }
    
    public static JmeContext newContext(AppSettings settings, Type contextType) 
    {
        initialize(settings);
        return new OGLESContext();
    }

    public static AudioRenderer newAudioRenderer(AppSettings settings) 
    {
		return new AndroidAudioRenderer(activity); 
    }

    public static void setResources(Resources res)
    {
        JmeSystem.res = res;
    }

    public static Resources getResources()
    {
        return res;
    }

    public static void setActivity(Activity activity)
    {
        JmeSystem.activity = activity;
    }

    public static Activity getActivity()
    {
        return activity;
    }    
    

    public static AssetManager newAssetManager()
    {
	    logger.info("newAssetManager()");
        return new AndroidAssetManager(null);
    }

    public static AssetManager newAssetManager(URL url)
    {
	    logger.info("newAssetManager(" + url + ")");
        return new AndroidAssetManager(url);
    }

    public static boolean showSettingsDialog(AppSettings settings, boolean loadSettings) 
    {
        return true;
    }
    
    public static Platform getPlatform()
    {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        return Platform.Android_Froyo;
        //    throw new UnsupportedOperationException("The specified platform: "+os+" is not supported.");
        
    }

}
