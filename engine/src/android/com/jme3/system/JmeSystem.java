package com.jme3.system;

import android.app.Activity;
import android.content.res.Resources;
import com.jme3.util.AndroidLogHandler;
import com.jme3.asset.AndroidAssetManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.android.AndroidAudioRenderer;
//import com.jme3.audio.DummyAudioRenderer;
import com.jme3.system.JmeContext.Type;
import com.jme3.system.android.OGLESContext;
import com.jme3.util.JmeFormatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.net.URL;

public class JmeSystem {

    private static final Logger logger = Logger.getLogger(JmeSystem.class.getName());
    private static boolean initialized = false;
    private static boolean lowPermissions = false;
    private static Resources res;
    private static Activity activity;

    public static void initialize(AppSettings settings) {
        if (initialized) {
            return;
        }

        initialized = true;
        try {
            JmeFormatter formatter = new JmeFormatter();

            Handler consoleHandler = new AndroidLogHandler();
            consoleHandler.setFormatter(formatter);
        } catch (SecurityException ex) {
            logger.log(Level.SEVERE, "Security error in creating log file", ex);
        }
        logger.log(Level.INFO, "Running on {0}", getFullName());
    }

    public static String getFullName() {
        return "jMonkeyEngine 3.0.0 Beta (Android)";
    }

    public static void setLowPermissions(boolean lowPerm) {
        lowPermissions = lowPerm;
    }

    public static boolean isLowPermissions() {
        return lowPermissions;
    }

    public static JmeContext newContext(AppSettings settings, Type contextType) {
        initialize(settings);
        return new OGLESContext();
    }

    public static AudioRenderer newAudioRenderer(AppSettings settings) {
        return new AndroidAudioRenderer(activity);
    }

    public static void setResources(Resources res) {
        JmeSystem.res = res;
    }

    public static Resources getResources() {
        return res;
    }

    public static void setActivity(Activity activity) {
        JmeSystem.activity = activity;
    }

    public static Activity getActivity() {
        return activity;
    }

    public static AssetManager newAssetManager() {
        logger.log(Level.INFO, "newAssetManager()");
        return new AndroidAssetManager(null);
    }

    public static AssetManager newAssetManager(URL url) {
        logger.log(Level.INFO, "newAssetManager({0})", url);
        return new AndroidAssetManager(url);
    }

    public static boolean showSettingsDialog(AppSettings settings, boolean loadSettings) {
        return true;
    }

    public static Platform getPlatform() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("arm")){
            if (arch.contains("v5")){
                return Platform.Android_ARM5;
            }else if (arch.contains("v6")){
                return Platform.Android_ARM6;
            }else if (arch.contains("v7")){
                return Platform.Android_ARM7;
            }else{
                return Platform.Android_ARM5; // unknown ARM
            }
        }else{
            throw new UnsupportedOperationException("Unsupported Android Platform");
        }
    }
}
