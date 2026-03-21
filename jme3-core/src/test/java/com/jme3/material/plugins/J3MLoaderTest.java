package com.jme3.material.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.RenderState;
import com.jme3.material.TechniqueDef;
import com.jme3.renderer.Caps;
import com.jme3.shader.Shader;
import com.jme3.shader.UniformBinding;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
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

    // ---- MaterialDef Inheritance helpers ----

    private MaterialDef loadParentDef() throws IOException {
        J3MLoader parentLoader = new J3MLoader();
        AssetInfo parentInfo = Mockito.mock(AssetInfo.class);
        @SuppressWarnings("unchecked")
        AssetKey<MaterialDef> parentKey = Mockito.mock(AssetKey.class);
        when(parentKey.getExtension()).thenReturn("j3md");
        when(parentKey.getName()).thenReturn("parent-matdef.j3md");
        when(parentInfo.getManager()).thenReturn(assetManager);
        when(parentInfo.getKey()).thenReturn(parentKey);
        when(parentInfo.openStream()).thenReturn(J3MLoader.class.getResourceAsStream("/parent-matdef.j3md"));
        return (MaterialDef) parentLoader.load(parentInfo);
    }

    @SuppressWarnings("unchecked")
    private MaterialDef loadChildDef(String resourcePath) throws IOException {
        MaterialDef parentDef = loadParentDef();

        when(assetManager.loadAsset(any(AssetKey.class))).thenReturn(parentDef);

        J3MLoader childLoader = new J3MLoader();
        AssetInfo childInfo = Mockito.mock(AssetInfo.class);
        AssetKey<MaterialDef> childKey = Mockito.mock(AssetKey.class);
        when(childKey.getExtension()).thenReturn("j3md");
        when(childKey.getName()).thenReturn(resourcePath);
        when(childInfo.getManager()).thenReturn(assetManager);
        when(childInfo.getKey()).thenReturn(childKey);
        when(childInfo.openStream()).thenReturn(J3MLoader.class.getResourceAsStream("/" + resourcePath));
        return (MaterialDef) childLoader.load(childInfo);
    }

    // ---- MaterialDef Inheritance tests ----

    @Test
    public void materialDefInheritance_shouldInheritParentParams() throws IOException {
        MaterialDef def = loadChildDef("child-matdef.j3md");

        // Should have parent's params + child's new param
        assertNotNull(def.getMaterialParam("Roughness"));
        assertNotNull(def.getMaterialParam("DiffuseMap"));
        assertNotNull(def.getMaterialParam("Wetness"));

        // Roughness default overridden to 0.8
        assertEquals(0.8f, (float) def.getMaterialParam("Roughness").getValue(), 0.001f);

        // Wetness default is 0.0
        assertEquals(0.0f, (float) def.getMaterialParam("Wetness").getValue(), 0.001f);
    }

    @Test(expected = IOException.class)
    public void materialDefInheritance_paramTypeMismatch_shouldThrow() throws IOException {
        loadChildDef("child-matdef-type-mismatch.j3md");
    }

    @Test
    public void materialDefInheritance_shouldOverrideFragShaderOnly() throws IOException {
        MaterialDef def = loadChildDef("child-matdef.j3md");

        List<TechniqueDef> defaultTechs = def.getTechniqueDefs("Default");
        assertNotNull(defaultTechs);
        assertEquals(2, defaultTechs.size());

        // Both variants should have child's frag shader and parent's vert shader
        for (TechniqueDef td : defaultTechs) {
            assertEquals("child.frag", td.getShaderProgramNames().get(Shader.ShaderType.Fragment));
            assertEquals("parent.vert", td.getShaderProgramNames().get(Shader.ShaderType.Vertex));
        }

        // Check languages per variant
        // When parent is loaded, the clone() in TechniqueDef produces variants.
        // After child inheritance cloning, variant order depends on addTechniqueDef order.
        // Find each variant by checking its required caps.
        TechniqueDef glsl150 = null, glsl100 = null;
        for (TechniqueDef td : defaultTechs) {
            if (td.getRequiredCaps().contains(Caps.GLSL150)) {
                glsl150 = td;
            } else {
                glsl100 = td;
            }
        }
        assertNotNull(glsl150);
        assertNotNull(glsl100);
        assertEquals("GLSL150", glsl150.getShaderProgramLanguages().get(Shader.ShaderType.Fragment));
        assertEquals("GLSL150", glsl150.getShaderProgramLanguages().get(Shader.ShaderType.Vertex));
        assertEquals("GLSL100", glsl100.getShaderProgramLanguages().get(Shader.ShaderType.Fragment));
        assertEquals("GLSL100", glsl100.getShaderProgramLanguages().get(Shader.ShaderType.Vertex));
    }

    @Test
    public void materialDefInheritance_shouldMergeDefinesAdditively() throws IOException {
        MaterialDef def = loadChildDef("child-matdef.j3md");

        List<TechniqueDef> defaultTechs = def.getTechniqueDefs("Default");
        assertNotNull(defaultTechs);

        // Parent's HAS_DIFFUSEMAP define should still exist
        TechniqueDef td = defaultTechs.get(0);
        assertNotNull(td.getShaderParamDefine("DiffuseMap"));
        assertEquals("HAS_DIFFUSEMAP", td.getShaderParamDefine("DiffuseMap"));

        // Child's WETNESS define should be added
        assertNotNull(td.getShaderParamDefine("Wetness"));
        assertEquals("WETNESS", td.getShaderParamDefine("Wetness"));
    }

    @Test
    public void materialDefInheritance_shouldSupportNewTechniques() throws IOException {
        MaterialDef def = loadChildDef("child-matdef-new-technique.j3md");

        // Parent techniques should still exist
        assertNotNull(def.getTechniqueDefs("Default"));
        assertNotNull(def.getTechniqueDefs("PreShadow"));

        // New technique should be added
        List<TechniqueDef> glowTechs = def.getTechniqueDefs("Glow");
        assertNotNull(glowTechs);
        assertEquals(2, glowTechs.size());
        assertEquals("glow.frag", glowTechs.get(0).getShaderProgramNames().get(Shader.ShaderType.Fragment));
    }

    @Test
    public void materialDefInheritance_paramsOnly_shouldInheritTechniques() throws IOException {
        MaterialDef def = loadChildDef("child-matdef-params-only.j3md");

        // New param should exist
        assertNotNull(def.getMaterialParam("Metallic"));

        // Parent params should exist
        assertNotNull(def.getMaterialParam("Roughness"));
        assertNotNull(def.getMaterialParam("DiffuseMap"));

        // Parent techniques should be inherited unchanged
        assertNotNull(def.getTechniqueDefs("Default"));
        assertNotNull(def.getTechniqueDefs("PreShadow"));
        assertEquals(2, def.getTechniqueDefs("Default").size());
        assertEquals(2, def.getTechniqueDefs("PreShadow").size());
    }

    @Test
    public void materialDefInheritance_techniqueOnly_shouldInheritParams() throws IOException {
        MaterialDef def = loadChildDef("child-matdef-technique-only.j3md");

        // Parent params should be inherited
        assertNotNull(def.getMaterialParam("Roughness"));
        assertNotNull(def.getMaterialParam("DiffuseMap"));
        assertEquals(0.5f, (float) def.getMaterialParam("Roughness").getValue(), 0.001f);

        // Default technique should have overridden frag shader
        List<TechniqueDef> defaultTechs = def.getTechniqueDefs("Default");
        for (TechniqueDef td : defaultTechs) {
            assertEquals("childtech.frag", td.getShaderProgramNames().get(Shader.ShaderType.Fragment));
            assertEquals("parent.vert", td.getShaderProgramNames().get(Shader.ShaderType.Vertex));
        }
    }

    @Test
    public void materialDefInheritance_emptyChild_shouldInheritEverything() throws IOException {
        MaterialDef def = loadChildDef("child-matdef-empty.j3md");

        // All parent params should be inherited
        assertNotNull(def.getMaterialParam("Roughness"));
        assertNotNull(def.getMaterialParam("DiffuseMap"));
        assertEquals(0.5f, (float) def.getMaterialParam("Roughness").getValue(), 0.001f);

        // All parent techniques should be inherited
        assertNotNull(def.getTechniqueDefs("Default"));
        assertNotNull(def.getTechniqueDefs("PreShadow"));
        assertEquals(2, def.getTechniqueDefs("Default").size());
        assertEquals(2, def.getTechniqueDefs("PreShadow").size());

        // Shader files should be parent's
        TechniqueDef td = def.getTechniqueDefs("Default").get(0);
        assertEquals("parent.vert", td.getShaderProgramNames().get(Shader.ShaderType.Vertex));
        assertEquals("parent.frag", td.getShaderProgramNames().get(Shader.ShaderType.Fragment));
    }

    @Test
    public void materialDefInheritance_shouldOverrideRenderState() throws IOException {
        MaterialDef def = loadChildDef("child-matdef-renderstate.j3md");

        List<TechniqueDef> defaultTechs = def.getTechniqueDefs("Default");
        assertNotNull(defaultTechs);

        for (TechniqueDef td : defaultTechs) {
            RenderState rs = td.getRenderState();
            assertNotNull(rs);
            assertEquals(RenderState.FaceCullMode.Off, rs.getFaceCullMode());
            assertEquals(RenderState.BlendMode.Alpha, rs.getBlendMode());
        }
    }

    @Test
    public void materialDefInheritance_shouldMergeWorldParamsAdditively() throws IOException {
        MaterialDef def = loadChildDef("child-matdef-worldparams.j3md");

        List<TechniqueDef> defaultTechs = def.getTechniqueDefs("Default");
        assertNotNull(defaultTechs);

        for (TechniqueDef td : defaultTechs) {
            List<UniformBinding> worldBinds = td.getWorldBindings();
            assertTrue(worldBinds.contains(UniformBinding.WorldViewProjectionMatrix));
            assertTrue(worldBinds.contains(UniformBinding.ViewMatrix));
        }
    }

    @Test
    public void materialDefInheritance_shouldOverrideVertShaderOnly() throws IOException {
        MaterialDef def = loadChildDef("child-matdef-vertshader.j3md");

        List<TechniqueDef> defaultTechs = def.getTechniqueDefs("Default");
        assertNotNull(defaultTechs);

        for (TechniqueDef td : defaultTechs) {
            assertEquals("childvert.vert", td.getShaderProgramNames().get(Shader.ShaderType.Vertex));
            assertEquals("parent.frag", td.getShaderProgramNames().get(Shader.ShaderType.Fragment));
        }
    }

    @Test
    public void materialDefInheritance_definesOnBothVariants() throws IOException {
        MaterialDef def = loadChildDef("child-matdef.j3md");

        List<TechniqueDef> defaultTechs = def.getTechniqueDefs("Default");
        assertNotNull(defaultTechs);
        assertEquals(2, defaultTechs.size());

        // Both variants should have the child's WETNESS define
        for (TechniqueDef td : defaultTechs) {
            assertNotNull("WETNESS define should be present on variant with caps " + td.getRequiredCaps(),
                    td.getShaderParamDefine("Wetness"));
            assertEquals("WETNESS", td.getShaderParamDefine("Wetness"));
        }

        // Both variants should still have the parent's HAS_DIFFUSEMAP define
        for (TechniqueDef td : defaultTechs) {
            assertNotNull("HAS_DIFFUSEMAP define should be present on variant with caps " + td.getRequiredCaps(),
                    td.getShaderParamDefine("DiffuseMap"));
            assertEquals("HAS_DIFFUSEMAP", td.getShaderParamDefine("DiffuseMap"));
        }
    }
}
