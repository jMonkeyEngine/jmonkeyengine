package com.jme3.asset;

import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.HttpZipLocator;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.audio.plugins.WAVLoader;
import com.jme3.system.JmeSystem;
import com.jme3.texture.plugins.AWTLoader;
import org.junit.Assert;
import org.junit.Test;

public class TestLocators {

    @Test
    public void testAbsoluteLocators() {
        AssetManager am = JmeSystem.newAssetManager(TestLocators.class.getResource("/com/jme3/asset/Desktop.cfg"));
        am.registerLocator("/",  ClasspathLocator.class);
        am.registerLoader(WAVLoader.class, "wav");
        am.registerLoader(AWTLoader.class, "jpg");

        Assert.assertNotNull(am.loadAudio("Sound/Effects/Gun.wav"));
        Assert.assertNotNull(am.loadTexture("Textures/Terrain/Pond/Pond.jpg"));
    }

    /**
     * Demonstrates loading a file from a custom {@link AssetLoader}
     */
    @Test
    public void testCustomLoader() {
        AssetManager am = new DesktopAssetManager(true);
        am.registerLocator("/", ClasspathLocator.class);
        am.registerLoader(TextLoader.class, "fnt");
        String result = (String)am.loadAsset("Interface/Fonts/Console.fnt");
        Assert.assertTrue(result.startsWith("info face=\"Lucida Console\" size=11 bold=0 italic=0 charset=\"\" unicode=1" +
                " stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=1,1 outline=0"));
    }

    @Test
    public void testManyLocators() {
        AssetManager am = new DesktopAssetManager(true);
        am.registerLocator(
                "https://github.com/jMonkeyEngine/wiki/raw/master/docs/modules/tutorials/assets/images/beginner/",
                UrlLocator.class);

        am.registerLocator("../jme3-examples/town.zip", ZipLocator.class);
        am.registerLocator(
                "https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/jmonkeyengine/wildhouse.zip",
                HttpZipLocator.class);
        am.registerLocator("/", ClasspathLocator.class);

        // Try loading from jme3-core resources using the ClasspathLocator.
        Assert.assertNotNull("Failed to load from classpath",
                am.locateAsset(new AssetKey<>("Interface/Fonts/Default.fnt")));

        // Try loading from the "town.zip" file using the ZipLocator.
        Assert.assertNotNull("Failed to load from town.zip file",
                am.locateAsset(new ModelKey("casaamarela.jpg")));

        // Try loading from the Google Code Archive website using the HttpZipLocator.
        Assert.assertNotNull("Failed to load from wildhouse.zip on googleapis.com",
                am.locateAsset(new ModelKey("glasstile2.png")));

        // Try loading from the GitHub website using the UrlLocator.
        Assert.assertNotNull("Failed to load from HTTP",
                am.locateAsset(new TextureKey("beginner-physics.png")));
    }
}
