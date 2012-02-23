package com.jme3.asset;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import java.io.IOException;
import java.io.InputStream;

/**
  * <code>AndroidImageInfo</code> is set in a jME3 image via the {@link Image#setEfficientData(java.lang.Object)}
  * method to retrieve a {@link Bitmap} when it is needed by the renderer. 
  * User code may extend <code>AndroidImageInfo</code> and provide their own implementation of the 
  * {@link AndroidImageInfo#loadBitmap()} method to acquire a bitmap by their own means.
  *
  * @author Kirill Vainer
  */
public class AndroidImageInfo {
    
    protected AssetInfo assetInfo;
    protected Bitmap bitmap;
    protected Format format;

    public AndroidImageInfo(AssetInfo assetInfo) {
        this.assetInfo = assetInfo;
    }
    
    public Bitmap getBitmap(){
        if (bitmap == null || bitmap.isRecycled()){
            try {
                loadBitmap();
            } catch (IOException ex) {
                // If called first inside AssetManager, the error will propagate
                // correctly. Assuming that if the first calls succeeds
                // then subsequent calls will as well.
                throw new AssetLoadException("Failed to load image " + assetInfo.getKey(), ex);
            }
        }
        return bitmap;
    }
    
    
    
    public Format getFormat(){
        return format;
    }
    
    /**
     * Loads the bitmap directly from the asset info, possibly updating
     * or creating the image object.
     */
    protected void loadBitmap() throws IOException{
        InputStream in = null;
        try {
            in = assetInfo.openStream();
            bitmap = BitmapFactory.decodeStream(in);
            if (bitmap == null) {
                throw new IOException("Failed to load image: " + assetInfo.getKey().getName());
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }

        switch (bitmap.getConfig()) {
            case ALPHA_8:
                format = Image.Format.Alpha8;
                break;
            case ARGB_4444:
                format = Image.Format.ARGB4444;
                break;
            case ARGB_8888:
                format = Image.Format.RGBA8;
                break;
            case RGB_565:
                format = Image.Format.RGB565;
                break;
            default:
                // This should still work as long
                // as renderer doesn't check format
                // but just loads bitmap directly.
                format = null;
        }

        TextureKey texKey = (TextureKey) assetInfo.getKey();
        if (texKey.isFlipY()) {
            // Flip the image, then delete the old one.
            Matrix flipMat = new Matrix();
            flipMat.preScale(1.0f, -1.0f);
            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), flipMat, false);
            bitmap.recycle();
            bitmap = newBitmap;

            if (bitmap == null) {
                throw new IOException("Failed to flip image: " + texKey);
            }
        }  
    }
}
