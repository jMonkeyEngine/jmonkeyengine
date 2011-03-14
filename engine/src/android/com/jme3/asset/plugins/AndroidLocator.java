package com.jme3.asset.plugins;

import android.content.res.AssetManager;
import android.content.res.Resources;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLocator;
import com.jme3.system.JmeSystem;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class AndroidLocator implements AssetLocator {

    private static final Logger logger = Logger.getLogger(AndroidLocator.class.getName());
    private Resources resources;
    private AssetManager androidManager;

    private class AndroidAssetInfo extends AssetInfo {

        private final InputStream in;

        public AndroidAssetInfo(com.jme3.asset.AssetManager manager, AssetKey key,
                InputStream in){
            super(manager, key);
            this.in = in;
        }

        @Override
        public InputStream openStream() {
            return in;
        }
    }


    public AndroidLocator(){
        resources = JmeSystem.getResources();
        androidManager = resources.getAssets();
    }
    
    public void setRootPath(String rootPath) {
    }

    public AssetInfo locate(com.jme3.asset.AssetManager manager, AssetKey key) {
        InputStream in = null;
        try {
            in = androidManager.open(key.getName());
            if (in == null)
                return null;

            return new AndroidAssetInfo(manager, key, in);
        } catch (IOException ex) {
            if (in != null)
                try {
                    in.close();
                } catch (IOException ex1) {
                }
        }
        return null;
    }

}
