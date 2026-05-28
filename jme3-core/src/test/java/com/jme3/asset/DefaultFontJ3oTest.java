package com.jme3.asset;

import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.export.binary.BinaryLoader;
import com.jme3.font.BitmapFont;
import com.jme3.font.plugins.BitmapFontLoader;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.shader.plugins.GLSLLoader;
import com.jme3.texture.plugins.DDSLoader;
import com.jme3.texture.plugins.PFMLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that Default.j3o (with embedded image data) can be loaded using only
 * the loaders available in jme3-core, without depending on the PNG loader from
 * jme3-plugins. This is needed for Android/iOS which don't depend on jme3-plugins.
 */
public class DefaultFontJ3oTest {

    private static DesktopAssetManager createCoreOnlyAssetManager() {
        DesktopAssetManager assetManager = new DesktopAssetManager(false);
        assetManager.registerLocator("/", ClasspathLocator.class);
        // Register only the loaders available in jme3-core (matching General.cfg, minus StbImageLoader/WebpImageLoader)
        assetManager.registerLoader(BitmapFontLoader.class, "fnt");
        assetManager.registerLoader(J3MLoader.class, "j3m", "j3md");
        assetManager.registerLoader(DDSLoader.class, "dds");
        assetManager.registerLoader(PFMLoader.class, "pfm");
        assetManager.registerLoader(BinaryLoader.class, "j3o", "j3f");
        assetManager.registerLoader(GLSLLoader.class, "vert", "frag", "geom", "tsctrl", "tseval", "glsl", "glsllib", "comp");
        return assetManager;
    }

    private static void assertFontLoaded(DesktopAssetManager assetManager, String path) {
        BitmapFont font = assetManager.loadFont(path);
        assertNotNull(font, path + " should load without a PNG loader");
        assertNotNull(font.getPage(0), path + " should have at least one material page");
        assertNotNull(font.getPage(0).getTextureParam("ColorMap"),
                path + " material page should have a ColorMap texture");
        assertNotNull(font.getPage(0).getTextureParam("ColorMap").getTextureValue().getImage(),
                path + " texture should have embedded image data (no key needed)");
    }

    @Test
    public void testDefaultFontJ3oLoadsWithoutPngLoader() {
        // This should succeed: Default.j3o has embedded image data (no PNG loader needed)
        assertFontLoaded(createCoreOnlyAssetManager(), "Interface/Fonts/Default.j3o");
    }

    @Test
    public void testConsoleFontJ3oLoadsWithoutPngLoader() {
        // This should succeed: Console.j3o has embedded image data (no PNG loader needed)
        assertFontLoaded(createCoreOnlyAssetManager(), "Interface/Fonts/Console.j3o");
    }
}
