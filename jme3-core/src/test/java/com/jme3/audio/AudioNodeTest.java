package com.jme3.audio;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.system.JmeSystem;
import org.junit.Assert;
import org.junit.Test;

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

        Assert.assertNotNull(clone.previousWorldTranslation);
        Assert.assertNotSame(audio.previousWorldTranslation, clone.previousWorldTranslation);
        Assert.assertEquals(audio.previousWorldTranslation, clone.previousWorldTranslation);

        Assert.assertNotNull(clone.getDirection());
        Assert.assertNotSame(audio.getDirection(), clone.getDirection());
        Assert.assertEquals(audio.getDirection(), clone.getDirection());

        Assert.assertNotNull(clone.getVelocity());
        Assert.assertNotSame(audio.getVelocity(), clone.getVelocity());
        Assert.assertEquals(audio.getVelocity(), clone.getVelocity());

        Assert.assertNotNull(clone.getDryFilter());
        Assert.assertNotSame(audio.getDryFilter(), clone.getDryFilter());

        Assert.assertNotNull(clone.getReverbFilter());
        Assert.assertNotSame(audio.getReverbFilter(), clone.getReverbFilter());
    }

}
