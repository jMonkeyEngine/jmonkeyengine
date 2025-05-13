package com.jme3.audio;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import org.junit.Assert;
import org.junit.Test;

/**
 * Automated tests for the Filter class.
 *
 * @author capdevon
 */
public class AudioFilterTest {

    /**
     * Tests serialization and de-serialization of a {@code LowPassFilter}.
     */
    @Test
    public void testSaveAndLoad() {
        AssetManager assetManager = new DesktopAssetManager(true);

        LowPassFilter f = new LowPassFilter(.5f, .5f);
        LowPassFilter copy = BinaryExporter.saveAndLoad(assetManager, f);

        float delta = 0.001f;
        Assert.assertEquals(f.getVolume(), copy.getVolume(), delta);
        Assert.assertEquals(f.getHighFreqVolume(), copy.getHighFreqVolume(), delta);
    }

}
