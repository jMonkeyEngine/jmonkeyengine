package com.jme3.audio.android;

import com.jme3.asset.AssetKey;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioRenderer;

public class AndroidAudioData extends AudioData 
{
    protected AssetKey assetKey;
    protected int soundId = 0;
    
    public AssetKey getAssetKey() {
        return assetKey;
    }

    public void setAssetKey(AssetKey assetKey) {
        this.assetKey = assetKey;
    }

    public int getSoundId() {
        return soundId;
    }

    public void setSoundId(int soundId) {
        this.soundId = soundId;
    }

    @Override
    public DataType getDataType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public float getDuration() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void resetObject() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteObject(AudioRenderer r) {
        // TODO Auto-generated method stub
        
    }
    
    

}
