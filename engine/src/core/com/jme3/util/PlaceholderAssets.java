package com.jme3.util;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioBuffer;
import com.jme3.audio.AudioData;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import java.nio.ByteBuffer;

public class PlaceholderAssets {
    
    /**
     * Checkerboard of white and red squares
     */
    private static final byte[] imageData = {
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0x00, (byte)0x00,
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0x00, (byte)0x00,
        
        (byte)0xFF, (byte)0x00, (byte)0x00,
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0x00, (byte)0x00,
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0x00, (byte)0x00,
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0x00, (byte)0x00,
        
        (byte)0xFF, (byte)0x00, (byte)0x00,
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0x00, (byte)0x00,
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
    };
    
    public static Image getPlaceholderImage(){
        ByteBuffer tempData = BufferUtils.createByteBuffer(3 * 4 * 4);
        tempData.put(imageData).flip();
        return new Image(Format.RGB8, 4, 4, tempData);
    }
    
    public static Material getPlaceholderMaterial(AssetManager assetManager){
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        return mat;
    }
    
    public static Spatial getPlaceholderModel(AssetManager assetManager){
        // What should be the size? Nobody knows
        // the user's expected scale...
        Box box = new Box(1, 1, 1);
        Geometry geom = new Geometry("placeholder", box);
        geom.setMaterial(getPlaceholderMaterial(assetManager));
        return geom;
    }
    
    public static AudioData getPlaceholderAudio(){
        AudioBuffer audioBuf = new AudioBuffer();
        audioBuf.setupFormat(1, 8, 44100);
        ByteBuffer bb = BufferUtils.createByteBuffer(1);
        bb.put((byte)0).flip();
        audioBuf.updateData(bb);
        return audioBuf;
    }
    
}
