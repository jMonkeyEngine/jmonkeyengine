package com.jme3.system.android;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import com.jme3.util.AndroidScreenshots;
import com.jme3.util.functional.VoidFunction;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Level;

public class JmeAndroidSystem extends JmeSystemDelegate {

    private static View view;
    private static String audioRendererType = AppSettings.ANDROID_OPENAL_SOFT;

    static {
        try {
            System.loadLibrary("bulletjme");
        } catch (UnsatisfiedLinkError e) {
        }
    }

    public JmeAndroidSystem(){
        setErrorMessageHandler((message) -> {
            String finalMsg = message;
            String finalTitle = "Error in application";
            Context context = JmeAndroidSystem.getView().getContext();
            view.getHandler().post(() -> {
                AlertDialog dialog = new AlertDialog.Builder(context).setTitle(finalTitle).setMessage(finalMsg).create();
                dialog.show();
            });
        });
    }
    
    @Override
    public URL getPlatformAssetConfigURL() {
        return Thread.currentThread().getContextClassLoader().getResource("com/jme3/asset/Android.cfg");
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
    public JmeContext newContext(AppSettings settings, Type contextType) {
        if (settings.getAudioRenderer().equals(AppSettings.ANDROID_MEDIAPLAYER)) {
            audioRendererType = AppSettings.ANDROID_MEDIAPLAYER;
        } else if (settings.getAudioRenderer().equals(AppSettings.ANDROID_OPENAL_SOFT)) {
            audioRendererType = AppSettings.ANDROID_OPENAL_SOFT;
        } else {
            logger.log(Level.INFO, "AudioRenderer not set. Defaulting to OpenAL Soft");
            audioRendererType = AppSettings.ANDROID_OPENAL_SOFT;
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
    }

    @Override
    public void initialize(AppSettings settings) {
        if (initialized) {
            return;
        }
        initialized = true;
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
        logger.log(Level.INFO, getBuildInfo());
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
            } else if (arch.contains("v8")) {
                return Platform.Android_ARM8;
            } else {
                return Platform.Android_ARM5; // unknown ARM
            }
        } else if (arch.contains("aarch")) {
            return Platform.Android_ARM8;
        } else {
            return Platform.Android_Other;
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
                    storageFolder = view.getContext().getDir("", Context.MODE_PRIVATE);
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
                //   so the files can be copied to the PC (i.e. screenshots)
                storageFolder = storageFolders.get(type);
                if (storageFolder == null) {
                    String state = Environment.getExternalStorageState();
                    logger.log(Level.FINE, "ExternalStorageState: {0}", state);
                    if (state.equals(Environment.MEDIA_MOUNTED)) {
                        storageFolder = view.getContext().getExternalFilesDir(null);
                        storageFolders.put(type, storageFolder);
                    }
                }
                break;
            default:
                break;
        }
        if (logger.isLoggable(Level.FINE)) {
            if (storageFolder != null) {
                logger.log(Level.FINE, "Base Storage Folder Path: {0}", storageFolder.getAbsolutePath());
            } else {
                logger.log(Level.FINE, "Base Storage Folder not found!");
            }
        }
        return storageFolder;
    }

    public static void setView(View view) {
        JmeAndroidSystem.view = view;
    }

    public static View getView() {
        return view;
    }

    public static String getAudioRendererType() {
        return audioRendererType;
    }

    @Override
    public void showSoftKeyboard(final boolean show) {
        view.getHandler().post(new Runnable() {

            @Override
            public void run() {
                InputMethodManager manager =
                        (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                if (show) {
                    manager.showSoftInput(view, 0);
                } else {
                    manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });
    }
}
