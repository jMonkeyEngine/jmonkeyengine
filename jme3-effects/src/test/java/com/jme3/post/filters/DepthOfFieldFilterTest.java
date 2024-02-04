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
import org.junit.Assert;
import org.junit.Test;

/**
 * Automated tests for the {@code DepthOfFieldFilter} class.
 *
 * @author sgold
 */
public class DepthOfFieldFilterTest {
    /**
     * Tests serialization and de-serialization of a {@code DepthOfFieldFilter}.
     * This test would've detected JME issue #2166, for instance.
     */
    @Test
    public void testSaveAndLoad() {
        DepthOfFieldFilter filter = new DepthOfFieldFilter();

        // Verify the default parameter values:
        verifyDefaults(filter);

        // Set parameters to new values:
        filter.setBlurScale(10.5f);
        filter.setBlurThreshold(0.1f);
        filter.setDebugUnfocus(true);
        filter.setEnabled(false);
        filter.setFocusDistance(66f);
        filter.setFocusRange(15f);

        // Create a duplicate filter using serialization:
        AssetManager assetManager = new DesktopAssetManager();
        DepthOfFieldFilter copy = BinaryExporter.saveAndLoad(assetManager, filter);

        // Verify the parameter values of the copy:
        Assert.assertEquals(10.5f, copy.getBlurScale(), 0f);
        Assert.assertEquals(0.1f, copy.getBlurThreshold(), 0f);
        Assert.assertTrue(copy.getDebugUnfocus());
        Assert.assertEquals(66f, copy.getFocusDistance(), 0f);
        Assert.assertEquals(15f, copy.getFocusRange(), 0f);
        Assert.assertEquals("Depth Of Field", copy.getName());
        Assert.assertFalse(copy.isEnabled());
    }

    /**
     * Verify some default values of a newly instantiated
     * {@code DepthOfFieldFilter}.
     *
     * @param filter (not null, unaffected)
     */
    private void verifyDefaults(DepthOfFieldFilter filter) {
        Assert.assertEquals(1f, filter.getBlurScale(), 0f);
        Assert.assertEquals(0.2f, filter.getBlurThreshold(), 0f);
        Assert.assertFalse(filter.getDebugUnfocus());
        Assert.assertEquals(50f, filter.getFocusDistance(), 0f);
        Assert.assertEquals(10f, filter.getFocusRange(), 0f);
        Assert.assertEquals("Depth Of Field", filter.getName());
        Assert.assertTrue(filter.isEnabled());
    }
}
