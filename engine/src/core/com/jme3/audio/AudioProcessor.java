package com.jme3.audio;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetProcessor;

public class AudioProcessor implements AssetProcessor{

    public Object postProcess(AssetKey key, Object obj) {
        AudioKey audioKey = (AudioKey) key;
        AudioData audioData = (AudioData) obj;
        return new AudioNode(audioData, audioKey);
    }

    public Object createClone(Object obj) {
        AudioNode node = (AudioNode) obj;
        return node.clone();
    }
    
}
