package com.jme3.system;

import android.content.res.Resources;
import com.jme3.util.AndroidLogHandler;
import com.jme3.asset.AndroidAssetManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.AudioParam;
import com.jme3.audio.Environment;
import com.jme3.audio.Listener;
import com.jme3.audio.ListenerParam;
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
    private static Resources res;

    public static void initialize(AppSettings settings){
        if (initialized)
            return;

        initialized = true;
        try {
            JmeFormatter formatter = new JmeFormatter();

            Handler consoleHandler = new AndroidLogHandler();
            consoleHandler.setFormatter(formatter);
//            Logger.getLogger("").removeHandler(Logger.getLogger("").getHandlers()[0]);
//            Logger.getLogger("").addHandler(consoleHandler);

//            Logger.getLogger("com.g3d").setLevel(Level.FINEST);
        } catch (SecurityException ex){
            logger.log(Level.SEVERE, "Security error in creating log file", ex);
        }
        logger.info("Running on "+getFullName());
    }

    public static String getFullName(){
        return "jMonkey Engine 3 ALPHA 0.50";
    }
    
    public static JmeContext newContext(AppSettings settings, Type contextType) {
        initialize(settings);
        return new OGLESContext();
    }

    public static AudioRenderer newAudioRenderer(AppSettings settings) {
		return new AudioRenderer() {
			public void setListener(Listener listener) {}
			public void setEnvironment(Environment env) {}
			public void playSourceInstance(AudioNode src) {}
			public void playSource(AudioNode src) {}
			public void pauseSource(AudioNode src) {}
			public void stopSource(AudioNode src) {}
			public void deleteAudioData(AudioData ad) {}
			public void initialize() {}
			public void update(float tpf) {}
			public void cleanup() {}
			public void updateListenerParam(Listener listener, ListenerParam parameter) {}
			public void updateSourceParam(AudioNode node, AudioParam parameter) {}
		};
    }

    public static void setResources(Resources res){
        JmeSystem.res = res;
    }

    public static Resources getResources(){
        return res;
    }

    public static AssetManager newAssetManager(){
	logger.info("newAssetManager()");
        return new AndroidAssetManager(true);
    }

    public static AssetManager newAssetManager(URL url){
	logger.info("newAssetManager(" + url + ")");
        return new AndroidAssetManager(true);
    }

    public static boolean showSettingsDialog(AppSettings settings) {
        return true;
    }

}
