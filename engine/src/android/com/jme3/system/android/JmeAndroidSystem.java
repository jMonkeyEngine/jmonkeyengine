package com.jme3.system.android;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import com.jme3.asset.AndroidAssetManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.android.AndroidAudioRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeContext.Type;
import com.jme3.system.JmeSystemDelegate;
import com.jme3.system.Platform;
import com.jme3.util.JmeFormatter;
import java.io.File;
import java.net.URL;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JmeAndroidSystem extends JmeSystemDelegate {

    private static Activity activity;

    static {
        try {
            System.loadLibrary("bulletjme");
        } catch (UnsatisfiedLinkError e) {
        }
    }
    
    @Override
    public AssetManager newAssetManager(URL configFile) {
        logger.log(Level.INFO, "Creating asset manager with config {0}", configFile);
        return new AndroidAssetManager(configFile);
    }

    @Override
    public AssetManager newAssetManager() {
        logger.log(Level.INFO, "Creating asset manager with default config");
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
//            JmeFormatter formatter = new JmeFormatter();
//            Handler consoleHandler = new AndroidLogHandler();
//            consoleHandler.setFormatter(formatter);
//            
//            Logger log = Logger.getLogger("");
//            for (Handler h : log.getHandlers()) {
//                log.removeHandler(h);
//            }
//            log.addHandler(consoleHandler);
            Logger log = Logger.getLogger(JmeAndroidSystem.class.getName());
            boolean bIsLogFormatSet = false;
            do {
                log.setLevel(Level.ALL);
                if (log.getHandlers().length == 0) {
                    log = logger.getParent();
                    if (log != null) {
                        for (Handler h : log.getHandlers()) {
                            h.setFormatter(new JmeFormatter());
                            h.setLevel(Level.ALL);
                            bIsLogFormatSet = true;
                        }
                    }
                }
            } while (log != null && !bIsLogFormatSet);
        } catch (SecurityException ex) {
            logger.log(Level.SEVERE, "Security error in creating log file", ex);
        }
        logger.log(Level.INFO, "Running on {0}", getFullName());
    }

    @Override
    public Platform getPlatform() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("arm")) {
            if (arch.contains("v5")) {
                return Platform.Android_ARM5;
            } else if (arch.contains("v6")) {
                return Platform.Android_ARM6;
            } else if (arch.contains("v7")) {
                return Platform.Android_ARM7;
            } else {
                return Platform.Android_ARM5; // unknown ARM
            }
        } else {
            throw new UnsupportedOperationException("Unsupported Android Platform");
        }
    }

    @Override
    public synchronized File getStorageFolder() {
        //http://developer.android.com/reference/android/content/Context.html#getExternalFilesDir
        //http://developer.android.com/guide/topics/data/data-storage.html

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // getExternalFilesDir automatically creates the directory if necessary.
            // directory structure should be: /mnt/sdcard/Android/data/<packagename>/files
            // when created this way, the directory is automatically removed by the Android
            //   system when the app is uninstalled
            storageFolder = activity.getApplicationContext().getExternalFilesDir(null);
            logger.log(Level.INFO, "Storage Folder Path: {0}", storageFolder.getAbsolutePath());

            return storageFolder;
        } else {
            return null;
        }

    }

    public static void setActivity(Activity activity) {
        JmeAndroidSystem.activity = activity;
    }

    public static Activity getActivity() {
        return activity;
    }
}
