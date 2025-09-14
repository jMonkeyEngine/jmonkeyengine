package com.jme3.material.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.renderer.Caps;
import com.jme3.shader.VarType;
import com.jme3.texture.GlTexture;
import java.io.IOException;
import java.util.EnumSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Daniel Johansson
 * @since 2015-07-20
 */
@RunWith(MockitoJUnitRunner.class)
public class J3MLoaderTest {

    private J3MLoader j3MLoader;

    @Mock
    private AssetInfo assetInfo;

    @Mock
    private AssetManager assetManager;

    @Mock
    private AssetKey<Material> assetKey;

    @Mock
    private MaterialDef materialDef;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(assetKey.getExtension()).thenReturn(".j3m");
        when(assetInfo.getManager()).thenReturn(assetManager);
        when(assetInfo.getKey()).thenReturn(assetKey);
        when(assetManager.loadAsset(any(AssetKey.class))).thenReturn(materialDef);

        j3MLoader = new J3MLoader();
    }

    @Test
    public void noDefaultTechnique_shouldBeSupported() throws IOException {
        when(assetInfo.openStream()).thenReturn(J3MLoader.class.getResourceAsStream("/no-default-technique.j3md"));
        MaterialDef def = (MaterialDef) j3MLoader.load(assetInfo);
        assertEquals(1, def.getTechniqueDefs("Test").size());
    }

    @Test
    public void fixedPipelineTechnique_shouldBeIgnored() throws IOException {
        when(assetInfo.openStream()).thenReturn(J3MLoader.class.getResourceAsStream("/no-shader-specified.j3md"));
        MaterialDef def = (MaterialDef) j3MLoader.load(assetInfo);
        assertEquals(null, def.getTechniqueDefs("A"));
        assertEquals(1, def.getTechniqueDefs("B").size());
    }

    @Test
    public void multipleSameNamedTechniques_shouldBeSupported() throws IOException {
        when(assetInfo.openStream()).thenReturn(J3MLoader.class.getResourceAsStream("/same-name-technique.j3md"));
        MaterialDef def = (MaterialDef) j3MLoader.load(assetInfo);
        assertEquals(2, def.getTechniqueDefs("Test").size());
        assertEquals(EnumSet.of(Caps.GLSL150), def.getTechniqueDefs("Test").get(0).getRequiredCaps());
        assertEquals(EnumSet.of(Caps.GLSL100), def.getTechniqueDefs("Test").get(1).getRequiredCaps());
    }

    @Test
    public void oldStyleTextureParameters_shouldBeSupported() throws Exception {
        when(assetInfo.openStream()).thenReturn(J3MLoader.class.getResourceAsStream("/texture-parameters-oldstyle.j3m"));

        final GlTexture textureOldStyle = Mockito.mock(GlTexture.class);
        final GlTexture textureOldStyleUsingQuotes = Mockito.mock(GlTexture.class);

        final TextureKey textureKeyUsingQuotes = setupMockForTexture("OldStyleUsingQuotes", "old style using quotes/texture.png", true, true, textureOldStyleUsingQuotes);
        final TextureKey textureKeyOldStyle = setupMockForTexture("OldStyle", "old style/texture.png", true, true, textureOldStyle);

        j3MLoader.load(assetInfo);

        verify(assetManager).loadTexture(textureKeyUsingQuotes);
        verify(assetManager).loadTexture(textureKeyOldStyle);
        verify(textureOldStyle).setWrap(GlTexture.WrapMode.Repeat);
        verify(textureOldStyleUsingQuotes).setWrap(GlTexture.WrapMode.Repeat);
    }

    @Test
    public void newStyleTextureParameters_shouldBeSupported() throws Exception {
        when(assetInfo.openStream()).thenReturn(J3MLoader.class.getResourceAsStream("/texture-parameters-newstyle.j3m"));

        final GlTexture textureNoParameters = Mockito.mock(GlTexture.class);
        final GlTexture textureFlip = Mockito.mock(GlTexture.class);
        final GlTexture textureRepeat = Mockito.mock(GlTexture.class);
        final GlTexture textureRepeatAxis = Mockito.mock(GlTexture.class);
        final GlTexture textureMin = Mockito.mock(GlTexture.class);
        final GlTexture textureMag = Mockito.mock(GlTexture.class);
        final GlTexture textureCombined = Mockito.mock(GlTexture.class);
        final GlTexture textureLooksLikeOldStyle = Mockito.mock(GlTexture.class);

        final TextureKey textureKeyNoParameters = setupMockForTexture("Empty", "empty.png", false, true, textureNoParameters);
        final TextureKey textureKeyFlip = setupMockForTexture("Flip", "flip.png", true, true, textureFlip);
        setupMockForTexture("Repeat", "repeat.png", false, true, textureRepeat);
        setupMockForTexture("RepeatAxis", "repeat-axis.png", false, true, textureRepeatAxis);
        setupMockForTexture("Min", "min.png", false, true, textureMin);
        setupMockForTexture("Mag", "mag.png", false, true, textureMag);
        setupMockForTexture("Combined", "combined.png", true, false, textureCombined);
        setupMockForTexture("LooksLikeOldStyle", "oldstyle.png", true, true, textureLooksLikeOldStyle);

        j3MLoader.load(assetInfo);

        verify(assetManager).loadTexture(textureKeyNoParameters);
        verify(assetManager).loadTexture(textureKeyFlip);

        verify(textureRepeat).setWrap(GlTexture.WrapMode.Repeat);
        verify(textureRepeatAxis).setWrap(GlTexture.WrapAxis.T, GlTexture.WrapMode.Repeat);
        verify(textureMin).setMinFilter(GlTexture.MinFilter.Trilinear);
        verify(textureMag).setMagFilter(GlTexture.MagFilter.Bilinear);

        verify(textureCombined).setMagFilter(GlTexture.MagFilter.Nearest);
        verify(textureCombined).setMinFilter(GlTexture.MinFilter.BilinearNoMipMaps);
        verify(textureCombined).setWrap(GlTexture.WrapMode.Repeat);
    }

    private TextureKey setupMockForTexture(final String paramName, final String path, final boolean flipY, boolean generateMips, final GlTexture texture) {
        when(materialDef.getMaterialParam(paramName)).thenReturn(new MatParamTexture(VarType.Texture2D, paramName, texture, null));

        final TextureKey textureKey = new TextureKey(path, flipY);
        textureKey.setGenerateMips(generateMips);

        when(assetManager.loadTexture(textureKey)).thenReturn(texture);

        return textureKey;
    }
}
