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
 * Automated tests for the {@code FXAAFilter} class.
 *
 * @author sgold
 */
public class FXAAFilterTest {
    /**
     * Tests serialization and de-serialization of an {@code FXAAFilter}. This
     * test would've detected JME issue #2168, for instance.
     */
    @Test
    public void testSaveAndLoad() {
        FXAAFilter filter = new FXAAFilter();

        // Verify the default parameter values:
        verifyDefaults(filter);

        // Set parameters to new values:
        filter.setEnabled(false);
        filter.setReduceMul(0.22f);
        filter.setSpanMax(7f);
        filter.setSubPixelShift(0.33f);
        filter.setVxOffset(0.03f);

        // Create a duplicate filter using serialization:
        AssetManager assetManager = new DesktopAssetManager();
        FXAAFilter copy = BinaryExporter.saveAndLoad(assetManager, filter);

        // Verify the parameter values of the copy:
        Assert.assertEquals("FXAAFilter", copy.getName());
        Assert.assertEquals(0.22f, copy.getReduceMul(), 0f);
        Assert.assertEquals(7f, copy.getSpanMax(), 0f);
        Assert.assertEquals(0.33f, copy.getSubPixelShift(), 0f);
        Assert.assertEquals(0.03f, copy.getVxOffset(), 0f);
        Assert.assertFalse(copy.isEnabled());
    }

    /**
     * Verify some default values of a newly instantiated {@code FXAAFilter}.
     *
     * @param filter (not null, unaffected)
     */
    private void verifyDefaults(FXAAFilter filter) {
        Assert.assertEquals("FXAAFilter", filter.getName());
        Assert.assertEquals(0.125f, filter.getReduceMul(), 0f);
        Assert.assertEquals(8f, filter.getSpanMax(), 0f);
        Assert.assertEquals(0.25f, filter.getSubPixelShift(), 0f);
        Assert.assertEquals(0f, filter.getVxOffset(), 0f);
        Assert.assertTrue(filter.isEnabled());
    }
}
