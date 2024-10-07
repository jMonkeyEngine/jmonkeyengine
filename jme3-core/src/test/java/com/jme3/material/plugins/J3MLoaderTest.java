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
import com.jme3.texture.Texture;
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

        final Texture textureOldStyle = Mockito.mock(Texture.class);
        final Texture textureOldStyleUsingQuotes = Mockito.mock(Texture.class);

        final TextureKey textureKeyUsingQuotes = setupMockForTexture("OldStyleUsingQuotes", "old style using quotes/texture.png", true, true, textureOldStyleUsingQuotes);
        final TextureKey textureKeyOldStyle = setupMockForTexture("OldStyle", "old style/texture.png", true, true, textureOldStyle);

        j3MLoader.load(assetInfo);

        verify(assetManager).loadTexture(textureKeyUsingQuotes);
        verify(assetManager).loadTexture(textureKeyOldStyle);
        verify(textureOldStyle).setWrap(Texture.WrapMode.Repeat);
        verify(textureOldStyleUsingQuotes).setWrap(Texture.WrapMode.Repeat);
    }

    @Test
    public void newStyleTextureParameters_shouldBeSupported() throws Exception {
        when(assetInfo.openStream()).thenReturn(J3MLoader.class.getResourceAsStream("/texture-parameters-newstyle.j3m"));

        final Texture textureNoParameters = Mockito.mock(Texture.class);
        final Texture textureFlip = Mockito.mock(Texture.class);
        final Texture textureRepeat = Mockito.mock(Texture.class);
        final Texture textureRepeatAxis = Mockito.mock(Texture.class);
        final Texture textureMin = Mockito.mock(Texture.class);
        final Texture textureMag = Mockito.mock(Texture.class);
        final Texture textureCombined = Mockito.mock(Texture.class);
        final Texture textureLooksLikeOldStyle = Mockito.mock(Texture.class);

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

        verify(textureRepeat).setWrap(Texture.WrapMode.Repeat);
        verify(textureRepeatAxis).setWrap(Texture.WrapAxis.T, Texture.WrapMode.Repeat);
        verify(textureMin).setMinFilter(Texture.MinFilter.Trilinear);
        verify(textureMag).setMagFilter(Texture.MagFilter.Bilinear);

        verify(textureCombined).setMagFilter(Texture.MagFilter.Nearest);
        verify(textureCombined).setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        verify(textureCombined).setWrap(Texture.WrapMode.Repeat);
    }

    private TextureKey setupMockForTexture(final String paramName, final String path, final boolean flipY, boolean generateMips, final Texture texture) {
        when(materialDef.getMaterialParam(paramName)).thenReturn(new MatParamTexture(VarType.Texture2D, paramName, texture, null));

        final TextureKey textureKey = new TextureKey(path, flipY);
        textureKey.setGenerateMips(generateMips);

        when(assetManager.loadTexture(textureKey)).thenReturn(texture);

        return textureKey;
    }
}
