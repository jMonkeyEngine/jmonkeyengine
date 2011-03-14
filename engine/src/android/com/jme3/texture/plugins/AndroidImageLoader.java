package com.jme3.texture.plugins;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class AndroidImageLoader implements AssetLoader {

    public Object load2(AssetInfo info) throws IOException {
        ByteBuffer bb = BufferUtils.createByteBuffer(1 * 1 * 2);
        bb.put( (byte) 0xff ).put( (byte) 0xff );
        bb.clear();
        return new Image(Format.RGB5A1, 1, 1, bb);
    }

    public Object load(AssetInfo info) throws IOException {
        InputStream in = null;
        Bitmap bitmap = null;
        try {
            in = info.openStream();
            bitmap = BitmapFactory.decodeStream(in);
            if (bitmap == null){
                throw new IOException("Failed to load image: "+info.getKey().getName());
            }
        } finally {
            if (in != null)
                in.close();
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int bytesPerPixel = -1;
        Format fmt;

        switch (bitmap.getConfig()){
            case ALPHA_8:
                bytesPerPixel = 1;
                fmt = Format.Alpha8;
                break;
            case ARGB_4444:
                bytesPerPixel = 2;
                fmt = Format.ARGB4444;
                break;
            case ARGB_8888:
                bytesPerPixel = 4;
                fmt = Format.RGBA8;
                break;
            case RGB_565:
                bytesPerPixel = 2;
                fmt = Format.RGB565;
                break;
            default:
                return null;
        }

//        if (width > 128 || height > 128){
//            if (width > height){
//                float aspect = (float) height / width;
//                width = 128;
//                height = (int) (128 * aspect);
//
//            }else{
//                float aspect = (float) width / height;
//                width = (int) (128 * aspect);
//                height = 128;
//            }
//            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
//        }

//        if ( ((TextureKey)info.getKey()).isFlipY() ){
//            Bitmap newBitmap = null;
//            Matrix flipMat = new Matrix();
//            flipMat.preScale(1.0f, -1.0f);
//            newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), flipMat, false);
//            bitmap.recycle();
//            bitmap = newBitmap;
//
//            if (bitmap == null){
//                throw new IOException("Failed to load image2: "+info.getKey().getName());
//            }
//        }

        Image image = new Image(fmt, width, height, null);
        image.setEfficentData(bitmap);
        return image;
    }

}
