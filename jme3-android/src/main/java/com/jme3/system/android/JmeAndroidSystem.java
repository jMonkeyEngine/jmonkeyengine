package com.jme3.system.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import com.jme3.asset.AndroidAssetManager;
import com.jme3.asset.AndroidImageInfo;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.android.AndroidAL;
import com.jme3.audio.android.AndroidALC;
import com.jme3.audio.android.AndroidEFX;
import com.jme3.audio.openal.AL;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.audio.openal.ALC;
import com.jme3.audio.openal.EFX;
import com.jme3.system.*;
import com.jme3.system.JmeContext.Type;
import com.jme3.texture.Image;
import com.jme3.texture.image.DefaultImageRaster;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.AndroidScreenshots;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Level;

public class JmeAndroidSystem extends JmeSystemDelegate {

    private static Activity activity;
    private static String audioRendererType = AppSettings.ANDROID_OPENAL_SOFT;

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
        logger.log(Level.FINE, "Creating asset manager with config {0}", configFile);
        return new AndroidAssetManager(configFile);
    }

    @Override
    public AssetManager newAssetManager() {
        logger.log(Level.FINE, "Creating asset manager with default config");
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
        if (settings.getAudioRenderer().equals(AppSettings.ANDROID_MEDIAPLAYER)) {
            audioRendererType = AppSettings.ANDROID_MEDIAPLAYER;
        } else if (settings.getAudioRenderer().equals(AppSettings.ANDROID_OPENAL_SOFT)) {
            audioRendererType = AppSettings.ANDROID_OPENAL_SOFT;
        } else {
            logger.log(Level.INFO, "AudioRenderer not set. Defaulting to Android MediaPlayer / SoundPool");
            audioRendererType = AppSettings.ANDROID_MEDIAPLAYER;
        }
        initialize(settings);
        JmeContext ctx = new OGLESContext();
        ctx.setSettings(settings);
        return ctx;
    }

    @Override
    public AudioRenderer newAudioRenderer(AppSettings settings) {
        ALC alc = new AndroidALC();
        AL al = new AndroidAL();
        EFX efx = new AndroidEFX();
        return new ALAudioRenderer(al, alc, efx);
/*
        if (settings.getAudioRenderer().equals(AppSettings.ANDROID_MEDIAPLAYER)) {
            logger.log(Level.INFO, "newAudioRenderer settings set to Android MediaPlayer / SoundPool");
            audioRendererType = AppSettings.ANDROID_MEDIAPLAYER;
            return new AndroidMediaPlayerAudioRenderer(activity);
        } else if (settings.getAudioRenderer().equals(AppSettings.ANDROID_OPENAL_SOFT)) {
            logger.log(Level.INFO, "newAudioRenderer settings set to Android OpenAL Soft");
            audioRendererType = AppSettings.ANDROID_OPENAL_SOFT;
            return new AndroidMediaPlayerAudioRenderer(activity);
        } else {
            logger.log(Level.INFO, "AudioRenderer not set. Defaulting to Android MediaPlayer / SoundPool");
            audioRendererType = AppSettings.ANDROID_MEDIAPLAYER;
            return new AndroidMediaPlayerAudioRenderer(activity);
        }
*/
    }

    @Override
    public void initialize(AppSettings settings) {
        if (initialized) {
            return;
        }
        initialized = true;
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
    public synchronized File getStorageFolder(JmeSystem.StorageFolderType type) {
        File storageFolder = null;

        switch (type) {
            case Internal:
                // http://developer.android.com/guide/topics/data/data-storage.html
                // http://developer.android.com/guide/topics/data/data-storage.html#filesInternal
                // http://developer.android.com/reference/android/content/Context.html#getFilesDir()
                // http://developer.android.com/reference/android/content/Context.html#getDir(java.lang.String, int)

                // getDir automatically creates the directory if necessary.
                // Directory structure should be: /data/data/<packagename>/app_
                // When created this way, the directory is automatically removed by the Android
                //   system when the app is uninstalled.
                // The directory is NOT accessible by a PC connected to the device
                // The files can only be accessed by this application
                storageFolder = storageFolders.get(type);
                if (storageFolder == null) {
                    storageFolder = activity.getApplicationContext().getDir("", Context.MODE_PRIVATE);
                    storageFolders.put(type, storageFolder);
                }
                break;
            case External:
                //http://developer.android.com/reference/android/content/Context.html#getExternalFilesDir
                //http://developer.android.com/guide/topics/data/data-storage.html

                // getExternalFilesDir automatically creates the directory if necessary.
                // Directory structure should be: /mnt/sdcard/Android/data/<packagename>/files
                // When created this way, the directory is automatically removed by the Android
                //   system when the app is uninstalled.
                // The directory is also accessible by a PC connected to the device
                //   so the files can be copied to the PC (ie. screenshots)
                storageFolder = storageFolders.get(type);
                if (storageFolder == null) {
                    String state = Environment.getExternalStorageState();
                    logger.log(Level.FINE, "ExternalStorageState: {0}", state);
                    if (state.equals(Environment.MEDIA_MOUNTED)) {
                        storageFolder = activity.getApplicationContext().getExternalFilesDir(null);
                        storageFolders.put(type, storageFolder);
                    }
                }
                break;
            default:
                break;
        }
        if (storageFolder != null) {
            logger.log(Level.FINE, "Base Storage Folder Path: {0}", storageFolder.getAbsolutePath());
        } else {
            logger.log(Level.FINE, "Base Storage Folder not found!");
        }
        return storageFolder;
    }

    public static void setActivity(Activity activity) {
        JmeAndroidSystem.activity = activity;
    }

    public static Activity getActivity() {
        return activity;
    }

    public static String getAudioRendererType() {
        return audioRendererType;
    }
}
