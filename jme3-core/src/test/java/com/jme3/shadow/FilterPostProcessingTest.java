package com.jme3.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import org.junit.Assert;
import org.junit.Test;

/**
 * Automated tests for the {@code FilterPostProcessing} class.
 *
 * @author capdevon
 */
public class FilterPostProcessingTest {

    /**
     * Tests serialization and de-serialization of a {@code FilterPostProcessing}.
     */
    @Test
    public void testSaveAndLoad() {
        AssetManager assetManager = new DesktopAssetManager(true);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(createDirectionalLightShadowFilter(assetManager));
        fpp.addFilter(createSpotLightShadowFilter(assetManager));
        fpp.addFilter(createPointLightShadowFilter(assetManager));

        BinaryExporter.saveAndLoad(assetManager, fpp);
    }

    private DirectionalLightShadowFilter createDirectionalLightShadowFilter(AssetManager assetManager) {
        DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3f(-1, -2, -3).normalizeLocal());
        light.setColor(new ColorRGBA(0.8f, 0.8f, 0.8f, 1f));

        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, 2048, 1);
        dlsf.setLight(light);

        return dlsf;
    }

    private SpotLightShadowFilter createSpotLightShadowFilter(AssetManager assetManager) {
        SpotLight light = new SpotLight();
        light.setColor(new ColorRGBA(0.8f, 0.8f, 0.8f, 1f));

        SpotLightShadowFilter slsf = new SpotLightShadowFilter(assetManager, 2048);
        slsf.setLight(light);

        return slsf;
    }

    private PointLightShadowFilter createPointLightShadowFilter(AssetManager assetManager) {
        PointLight light = new PointLight();
        light.setColor(new ColorRGBA(0.8f, 0.8f, 0.8f, 1f));

        PointLightShadowFilter plsf = new PointLightShadowFilter(assetManager, 2048);
        plsf.setLight(light);

        return plsf;
    }
}
