package com.jme3.audio;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.system.JmeSystem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Automated tests for the {@code AudioNode} class.
 *
 * @author capdevon
 */
public class AudioNodeTest {

    @Test
    public void testAudioNodeClone() {
        AssetManager assetManager = JmeSystem.newAssetManager(AudioNodeTest.class.getResource("/com/jme3/asset/Desktop.cfg"));

        AudioNode audio = new AudioNode(assetManager,
                "Sound/Effects/Bang.wav", AudioData.DataType.Buffer);
        audio.setDirection(new Vector3f(0, 1, 0));
        audio.setVelocity(new Vector3f(1, 1, 1));
        audio.setDryFilter(new LowPassFilter(1f, .1f));
        audio.setReverbFilter(new LowPassFilter(.5f, .5f));

        AudioNode clone = audio.clone();

        Assertions.assertNotNull(clone.previousWorldTranslation);
        Assertions.assertNotSame(audio.previousWorldTranslation, clone.previousWorldTranslation);
        Assertions.assertEquals(audio.previousWorldTranslation, clone.previousWorldTranslation);

        Assertions.assertNotNull(clone.getDirection());
        Assertions.assertNotSame(audio.getDirection(), clone.getDirection());
        Assertions.assertEquals(audio.getDirection(), clone.getDirection());

        Assertions.assertNotNull(clone.getVelocity());
        Assertions.assertNotSame(audio.getVelocity(), clone.getVelocity());
        Assertions.assertEquals(audio.getVelocity(), clone.getVelocity());

        Assertions.assertNotNull(clone.getDryFilter());
        Assertions.assertNotSame(audio.getDryFilter(), clone.getDryFilter());

        Assertions.assertNotNull(clone.getReverbFilter());
        Assertions.assertNotSame(audio.getReverbFilter(), clone.getReverbFilter());
    }

}
