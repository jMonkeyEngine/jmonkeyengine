package com.jme3.asset.plugins;

import com.jme3.asset.*;
import com.jme3.system.android.JmeAndroidSystem;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class AndroidLocator implements AssetLocator {

    private static final Logger logger = Logger.getLogger(AndroidLocator.class.getName());
    
    private android.content.res.AssetManager androidManager;
    private String rootPath = "";

    private class AndroidAssetInfo extends AssetInfo {

        private InputStream in;
        private final String assetPath;

        public AndroidAssetInfo(com.jme3.asset.AssetManager assetManager, AssetKey<?> key, String assetPath, InputStream in) {
            super(assetManager, key);
            this.assetPath = assetPath;
            this.in = in;
        }
        
        @Override
        public InputStream openStream() {
            if (in != null){
                // Reuse the already existing stream (only once)
                InputStream in2 = in;
                in = null;
                return in2;
            }else{
                // Create a new stream for subsequent invocations.
                try {
                    return androidManager.open(assetPath);
                } catch (IOException ex) {
                    throw new AssetLoadException("Failed to open asset " + assetPath, ex);
                }
            }
        }
    }

    private AndroidAssetInfo create(AssetManager assetManager, AssetKey key, String assetPath) throws IOException {
        try {
            InputStream in = androidManager.open(assetPath);
            if (in == null){
                return null;
            }else{
                return new AndroidAssetInfo(assetManager, key, assetPath, in);
            }
        } catch (IOException ex) {
            // XXX: Prefer to show warning here?
            // Should only surpress exceptions for "file missing" type errors.
            return null;
        }
    }
    
    public AndroidLocator() {
        androidManager = JmeAndroidSystem.getResources().getAssets();
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public AssetInfo locate(com.jme3.asset.AssetManager manager, AssetKey key) {
        String assetPath = rootPath + key.getName();
        // Fix path issues
        if (assetPath.startsWith("/")) {
            // Remove leading /
            assetPath = assetPath.substring(1);
        }
        assetPath = assetPath.replace("//", "/");
        try {
            return create(manager, key, assetPath);
        } catch (IOException ex) {
            // This is different handling than URL locator
            // since classpath locating would return null at the getResource() 
            // call, otherwise there's a more critical error...
            throw new AssetLoadException("Failed to open asset " + assetPath, ex);
        }
    }
}
