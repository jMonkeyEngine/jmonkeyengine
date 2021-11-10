package com.jme3.post.filters;

import org.junit.Assert;
import org.junit.Test;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.ColorRGBA;

/**
 *
 * @author capdevon
 */
public class CartoonEdgeFilterTest {

    /**
     * Test saving/loading a CartoonEdgeFilter
     */
    @Test
    public void testSaveAndLoad() {
        AssetManager assetManager = new DesktopAssetManager();

        CartoonEdgeFilter cartoon = new CartoonEdgeFilter();
        cartoon.setEdgeColor(ColorRGBA.Red);
        cartoon.setEdgeIntensity(.5f);
        cartoon.setEdgeWidth(1);
        cartoon.setNormalSensitivity(2);
        cartoon.setNormalThreshold(1);
        cartoon.setDepthSensitivity(20);
        cartoon.setDepthThreshold(2);

        CartoonEdgeFilter filter = BinaryExporter.saveAndLoad(assetManager, cartoon);

        Assert.assertEquals(ColorRGBA.Red, filter.getEdgeColor());
        Assert.assertEquals(.5f, filter.getEdgeIntensity(), 0.0001);
        Assert.assertEquals(1, filter.getEdgeWidth(), 0.0001);
        Assert.assertEquals(2, filter.getNormalSensitivity(), 0.0001);
        Assert.assertEquals(1, filter.getNormalThreshold(), 0.0001);
        Assert.assertEquals(20, filter.getDepthSensitivity(), 0.0001);
        Assert.assertEquals(2, filter.getDepthThreshold(), 0.0001);
    }

}
