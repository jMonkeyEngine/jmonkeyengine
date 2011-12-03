package com.jme3.asset.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Handles loading of assets from a URL
 * 
 * @author Kirill Vainer
 */
public class UrlAssetInfo extends AssetInfo {
    
    private URL url;
    private InputStream in;
    
    public static UrlAssetInfo create(AssetManager assetManager, AssetKey key, URL url) throws IOException {
        // Check if URL can be reached. This will throw
        // IOException which calling code will handle.
        URLConnection conn = url.openConnection();
        conn.setUseCaches(false);
        InputStream in = conn.getInputStream();
        
        // For some reason url cannot be reached?
        if (in == null){
            return null;
        }else{
            return new UrlAssetInfo(assetManager, key, url, in);
        }
    }
    
    private UrlAssetInfo(AssetManager assetManager, AssetKey key, URL url, InputStream in) throws IOException {
        super(assetManager, key);
        this.url = url;
        this.in = in;
    }
    
    public boolean hasInitialConnection(){
        return in != null;
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
                URLConnection conn = url.openConnection();
                conn.setUseCaches(false);
                return conn.getInputStream();
            } catch (IOException ex) {
                throw new AssetLoadException("Failed to read URL " + url, ex);
            }
        }
    }
}
