package com.jme3.texture.plugins;

import android.graphics.Bitmap;
import com.jme3.asset.AndroidImageInfo;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.texture.Image;
import java.io.IOException;

public class AndroidImageLoader implements AssetLoader {

    public Object load(AssetInfo info) throws IOException {
        AndroidImageInfo imageInfo = new AndroidImageInfo(info);
        Bitmap bitmap = imageInfo.getBitmap();
        
        Image image = new Image(imageInfo.getFormat(), bitmap.getWidth(), bitmap.getHeight(), null);
        image.setEfficentData(imageInfo);
        return image;
    }
}
