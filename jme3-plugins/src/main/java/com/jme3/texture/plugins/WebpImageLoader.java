package com.jme3.texture.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.ngengine.webp.decoder.DecodedWebP;
import org.ngengine.webp.decoder.WebPDecoder;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.TextureKey;
import com.jme3.export.binary.ByteUtils;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

public class WebpImageLoader implements AssetLoader{

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        try{
            AssetKey<?> key = assetInfo.getKey();
            TextureKey textureKey = null;
            if(key instanceof TextureKey){
                textureKey = (TextureKey) key;
            }
            boolean flip = textureKey != null && textureKey.isFlipY();
            try(InputStream is = assetInfo.openStream()) {
                byte[] data = ByteUtils.getByteContent(is);
                DecodedWebP decoded = WebPDecoder.decode(data, BufferUtils::createByteBuffer);
                int w = decoded.width;
                int h = decoded.height;
                ByteBuffer rgba = decoded.rgba;
                if(flip){
                    ByteBuffer flipped = BufferUtils.createByteBuffer(rgba.capacity());
                    for(int y = h - 1; y >= 0; y--){
                        int rowStart = y * w * 4;
                        for(int x = 0; x < w * 4; x++){
                            flipped.put(rgba.get(rowStart + x));
                        }
                    }
                    flipped.flip();
                    rgba = flipped;
                } 
                Image jmeImage = new Image(Image.Format.RGBA8, w, h, rgba, ColorSpace.sRGB);
                return jmeImage;
            }
        }catch(Exception e){
            throw new IOException("Failed to load WebP image", e);
        }
            
    }
    
}
