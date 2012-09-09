package com.jme3.system.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Environment;
import com.jme3.asset.AndroidAssetManager;
import com.jme3.asset.AndroidImageInfo;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.android.AndroidAudioRenderer;
import com.jme3.system.*;
import com.jme3.system.JmeContext.Type;
import com.jme3.texture.Image;
import com.jme3.texture.image.DefaultImageRaster;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.AndroidScreenshots;
import com.jme3.util.JmeFormatter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
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
    public void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height) throws IOException {
        Bitmap bitmapImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        AndroidScreenshots.convertScreenShot(imageData, bitmapImage);
        Bitmap.CompressFormat compressFormat;
        if (format.equals("png")) {
            compressFormat = Bitmap.CompressFormat.PNG;
        } else if (format.equals("jpg")) {
            compressFormat = Bitmap.CompressFormat.JPEG;
        } else {
            throw new UnsupportedOperationException("Only 'png' and 'jpg' formats are supported on Android");
        }
        bitmapImage.compress(compressFormat, 95, outStream);
        bitmapImage.recycle();
    }
    
    @Override
    public ImageRaster createImageRaster(Image image, int slice) {
        if (image.getEfficentData() != null) {
            return (AndroidImageInfo) image.getEfficentData();
        } else {
            return new DefaultImageRaster(image, slice);
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
    public void showErrorDialog(String message) {
        final String finalMsg = message;
        final String finalTitle = "Error in application";
        final Activity context = JmeAndroidSystem.getActivity();
        
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setTitle(finalTitle).setMessage(finalMsg).create();
                dialog.show();
            }
        });
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
                    log = log.getParent();
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
