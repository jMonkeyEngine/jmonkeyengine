package com.jme3.audio.plugins;

import java.io.IOException;
import java.io.InputStream;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.audio.android.AndroidAudioData;

public class AndroidAudioLoader implements AssetLoader 
{

    @Override
    public Object load(AssetInfo assetInfo) throws IOException 
    {

        InputStream in = assetInfo.openStream();
        if (in != null)
        {            
            in.close();
        }
        AndroidAudioData result = new AndroidAudioData();
        result.setAssetKey( assetInfo.getKey() );
        return result;
    }

}
