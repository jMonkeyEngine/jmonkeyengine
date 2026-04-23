package com.jme3.post.filters;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.post.ssao.SSAOFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Automated tests for the {@code SSAOFilter} class.
 *
 * @author capdevon
 */
public class SSAOFilterTest {

    /**
     * Tests serialization and de-serialization of an {@code SSAOFilter}.
     */
    @Test
    public void testSaveAndLoad() {
        SSAOFilter filter = new SSAOFilter();

        // Verify the default parameter values:
        verifyDefaults(filter);

        // Set parameters to new values:
        filter.setEnabled(false);
        filter.setSampleRadius(4.5f);
        filter.setIntensity(1.8f);
        filter.setScale(0.4f);
        filter.setBias(0.5f);
        filter.setApproximateNormals(true);

        // Create a duplicate filter using serialization:
        AssetManager assetManager = new DesktopAssetManager();
        SSAOFilter copy = BinaryExporter.saveAndLoad(assetManager, filter);

        // Verify the parameter values of the copy:
        Assertions.assertEquals("SSAOFilter", copy.getName());
        Assertions.assertEquals(4.5f, copy.getSampleRadius(), 0f);
        Assertions.assertEquals(1.8f, copy.getIntensity(), 0f);
        Assertions.assertEquals(0.4f, copy.getScale(), 0f);
        Assertions.assertEquals(0.5f, copy.getBias(), 0f);
        Assertions.assertTrue(copy.isApproximateNormals());
        Assertions.assertFalse(copy.isEnabled());
    }

    /**
     * Verify some default values of a newly instantiated {@code SSAOFilter}.
     *
     * @param filter (not null, unaffected)
     */
    private void verifyDefaults(SSAOFilter filter) {
        Assertions.assertEquals("SSAOFilter", filter.getName());
        Assertions.assertEquals(5.1f, filter.getSampleRadius(), 0f);
        Assertions.assertEquals(1.5f, filter.getIntensity(), 0f);
        Assertions.assertEquals(0.2f, filter.getScale(), 0f);
        Assertions.assertEquals(0.1f, filter.getBias(), 0f);
        Assertions.assertFalse(filter.isApproximateNormals());
        Assertions.assertTrue(filter.isEnabled());
    }
}
