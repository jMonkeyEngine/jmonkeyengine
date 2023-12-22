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
 * Automated tests for the {@code PosterizationFilter} class.
 *
 * @author sgold
 */
public class PosterizationFilterTest {
    /**
     * Tests serialization and de-serialization of a
     * {@code PosterizationFilter}. This test would've detected JME issue #2167,
     * for instance.
     */
    @Test
    public void testSaveAndLoad() {
        PosterizationFilter filter = new PosterizationFilter();

        // Verify the default parameter values:
        verifyDefaults(filter);

        // Set parameters to new values:
        filter.setEnabled(false);
        filter.setGamma(0.7f);
        filter.setNumColors(4);
        filter.setStrength(0.8f);

        // Create a duplicate filter using serialization:
        AssetManager assetManager = new DesktopAssetManager();
        PosterizationFilter copy
                = BinaryExporter.saveAndLoad(assetManager, filter);

        // Verify the parameter values of the duplicate:
        Assert.assertEquals(0.7f, copy.getGamma(), 0f);
        Assert.assertEquals(4, copy.getNumColors(), 0f);
        Assert.assertEquals(0.8f, copy.getStrength(), 0f);
        Assert.assertFalse(copy.isEnabled());
    }

    /**
     * Verify some default values of a newly instantiated
     * {@code PosterizationFilter}.
     *
     * @param filter (not null, unaffected)
     */
    private void verifyDefaults(PosterizationFilter filter) {
        Assert.assertEquals(0.6f, filter.getGamma(), 0f);
        Assert.assertEquals(8, filter.getNumColors(), 0f);
        Assert.assertEquals(1f, filter.getStrength(), 0f);
        Assert.assertTrue(filter.isEnabled());
    }
}
