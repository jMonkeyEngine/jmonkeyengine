/*
 * Copyright (c) 2023 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.post.filters;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.ColorRGBA;
import org.junit.Assert;
import org.junit.Test;

/**
 * Automated tests for the {@code CrossHatchFilter} class.
 *
 * @author sgold
 */
public class CrossHatchFilterTest {
    /**
     * Tests serialization and de-serialization of a {@code CrossHatchFilter}.
     * This test would've detected JME issue #2169, for instance.
     */
    @Test
    public void testSaveAndLoad() {
        CrossHatchFilter filter = new CrossHatchFilter();

        // Verify the default parameter values:
        verifyDefaults(filter);

        // Set parameters to new values:
        filter.setColorInfluenceLine(0.7f);
        filter.setColorInfluencePaper(0.2f);
        filter.setEnabled(false);
        filter.setFillValue(0.95f);
        filter.setLineColor(ColorRGBA.Blue.clone());
        filter.setLineDistance(3f);
        filter.setLineThickness(0.9f);
        filter.setLuminanceLevels(0.95f, 0.75f, 0.54f, 0.32f, 0.21f);
        filter.setPaperColor(ColorRGBA.Yellow.clone());

        // Create a duplicate filter using serialization:
        AssetManager assetManager = new DesktopAssetManager();
        CrossHatchFilter copy = BinaryExporter.saveAndLoad(assetManager, filter);

        // Verify the parameter values of the copy:
        Assert.assertEquals(0.7f, copy.getColorInfluenceLine(), 0f);
        Assert.assertEquals(0.2f, copy.getColorInfluencePaper(), 0f);
        Assert.assertEquals(0.95f, copy.getFillValue(), 0f);
        Assert.assertEquals(ColorRGBA.Blue, copy.getLineColor());
        Assert.assertEquals(3f, copy.getLineDistance(), 0f);
        Assert.assertEquals(0.9f, copy.getLineThickness(), 0f);
        Assert.assertEquals(0.95f, copy.getLuminance1(), 0f);
        Assert.assertEquals(0.75f, copy.getLuminance2(), 0f);
        Assert.assertEquals(0.54f, copy.getLuminance3(), 0f);
        Assert.assertEquals(0.32f, copy.getLuminance4(), 0f);
        Assert.assertEquals(0.21f, copy.getLuminance5(), 0f);
        Assert.assertEquals("CrossHatchFilter", copy.getName());
        Assert.assertEquals(ColorRGBA.Yellow, copy.getPaperColor());
        Assert.assertFalse(copy.isEnabled());
    }

    /**
     * Verify some default values of a newly instantiated
     * {@code CrossHatchFilter}.
     *
     * @param filter (not null, unaffected)
     */
    private void verifyDefaults(CrossHatchFilter filter) {
        Assert.assertEquals(0.8f, filter.getColorInfluenceLine(), 0f);
        Assert.assertEquals(0.1f, filter.getColorInfluencePaper(), 0f);
        Assert.assertEquals(0.9f, filter.getFillValue(), 0f);
        Assert.assertEquals(ColorRGBA.Black, filter.getLineColor());
        Assert.assertEquals(4f, filter.getLineDistance(), 0f);
        Assert.assertEquals(1f, filter.getLineThickness(), 0f);
        Assert.assertEquals(0.9f, filter.getLuminance1(), 0f);
        Assert.assertEquals(0.7f, filter.getLuminance2(), 0f);
        Assert.assertEquals(0.5f, filter.getLuminance3(), 0f);
        Assert.assertEquals(0.3f, filter.getLuminance4(), 0f);
        Assert.assertEquals(0f, filter.getLuminance5(), 0f);
        Assert.assertEquals("CrossHatchFilter", filter.getName());
        Assert.assertEquals(ColorRGBA.White, filter.getPaperColor());
        Assert.assertTrue(filter.isEnabled());
    }
}
