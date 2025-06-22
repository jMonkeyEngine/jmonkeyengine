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
    public void testSaveAndLoad_LowPassFilter() {
        AssetManager assetManager = new DesktopAssetManager(true);

        LowPassFilter f = new LowPassFilter(.5f, .5f);
        LowPassFilter copy = BinaryExporter.saveAndLoad(assetManager, f);

        float delta = 0.001f;
        Assert.assertEquals(f.getVolume(), copy.getVolume(), delta);
        Assert.assertEquals(f.getHighFreqVolume(), copy.getHighFreqVolume(), delta);
    }

    /**
     * Tests serialization and de-serialization of a {@code HighPassFilter}.
     */
    @Test
    public void testSaveAndLoad_HighPassFilter() {
        AssetManager assetManager = new DesktopAssetManager(true);

        HighPassFilter f = new HighPassFilter(.5f, .5f);
        HighPassFilter copy = BinaryExporter.saveAndLoad(assetManager, f);

        float delta = 0.001f;
        Assert.assertEquals(f.getVolume(), copy.getVolume(), delta);
        Assert.assertEquals(f.getLowFreqVolume(), copy.getLowFreqVolume(), delta);
    }

    /**
     * Tests serialization and de-serialization of a {@code BandPassFilter}.
     */
    @Test
    public void testSaveAndLoad_BandPassFilter() {
        AssetManager assetManager = new DesktopAssetManager(true);

        BandPassFilter f = new BandPassFilter(.5f, .5f, .5f);
        BandPassFilter copy = BinaryExporter.saveAndLoad(assetManager, f);

        float delta = 0.001f;
        Assert.assertEquals(f.getVolume(), copy.getVolume(), delta);
        Assert.assertEquals(f.getHighFreqVolume(), copy.getHighFreqVolume(), delta);
        Assert.assertEquals(f.getLowFreqVolume(), copy.getLowFreqVolume(), delta);
    }

}
