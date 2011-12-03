package com.jme3.system.android;

import android.app.Activity;
import android.content.res.Resources;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.Platform;
import com.jme3.util.AndroidLogHandler;
import com.jme3.asset.AndroidAssetManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.android.AndroidAudioRenderer;
import com.jme3.system.JmeContext.Type;
import com.jme3.system.JmeSystemDelegate;
import com.jme3.util.JmeFormatter;
import java.util.logging.Handler;
import java.util.logging.Level;

import java.net.URL;

public class JmeAndroidSystem extends JmeSystemDelegate{

    private static Resources res;
    private static Activity activity;

    @Override
    public AssetManager newAssetManager(URL configFile) {
        logger.log(Level.INFO, "newAssetManager({0})", configFile);
        return new AndroidAssetManager(configFile);
    }

    @Override
    public AssetManager newAssetManager() {
        logger.log(Level.INFO, "newAssetManager()");
        return new AndroidAssetManager(null);
    }

    @Override
    public boolean showSettingsDialog(AppSettings sourceSettings, boolean loadFromRegistry) {
        return true;
    }

    @Override
    public JmeContext newContext(AppSettings settings, Type contextType) {
        initialize(settings);
        return new OGLESContext();
    }

    @Override
    public AudioRenderer newAudioRenderer(AppSettings settings) {
        return new AndroidAudioRenderer(activity);
    }

    @Override
    public void initialize(AppSettings settings) {
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

    @Override
    public Platform getPlatform() {
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
    
    public static void setResources(Resources res) {
        JmeAndroidSystem.res = res;
    }

    public static Resources getResources() {
        return res;
    }

    public static void setActivity(Activity activity) {
        JmeAndroidSystem.activity = activity;
    }

    public static Activity getActivity() {
        return activity;
    }
}
