package com.jme3.texture.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Image;
import java.io.IOException;
import java.io.InputStream;

/**
 * Native image loader to deal with filetypes that support alpha channels.
 * The Android Bitmap class premultiplies the channels by the alpha when
 * loading.  This loader does not.
 *
 * @author iwgeric
 * @author Kirill Vainer
 */
public class AndroidNativeImageLoader  implements AssetLoader {
    
    private final byte[] tmpArray = new byte[1024];
    
    static {
         System.loadLibrary("stbijme");
    }
    
    private static native Image load(InputStream in, boolean flipY, byte[] tmpArray) throws IOException;
    
    public Image load(AssetInfo info) throws IOException {
        boolean flip = ((TextureKey) info.getKey()).isFlipY();
        InputStream in = null;
        try {
            in = info.openStream();
            return load(info.openStream(), flip, tmpArray);
        } finally {
            if (in != null){
                in.close();
            }
        }
    }
}
