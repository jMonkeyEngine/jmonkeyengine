package com.jme3.material;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.ColorRGBA;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Texture;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Test cases for verifying the `contentEquals` and `contentHashCode` methods of the
 * {@link Material} class. These tests ensure that material comparison and hashing
 * behave as expected under various scenarios, including cloning, loading,
 * and modifying material parameters and textures.
 *
 * @author capdevon
 */
public class MaterialCompareTest {

    private static AssetManager assetManager;

    /**
     * Initializes the asset manager before any tests are run.
     * This method loads a desktop configuration for the asset manager.
     */
    @BeforeClass
    public static void init() {
        URL config = MaterialCompareTest.class.getResource("/com/jme3/asset/Desktop.cfg");
        assetManager = JmeSystem.newAssetManager(config);
    }

    @Test
    public void testMaterialCompare() {
        // Test case 1: Cloned materials should be content-equal.
        Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setName("mat1");
        mat1.setColor("Color", ColorRGBA.Blue);

        Material mat2 = mat1.clone();
        mat2.setName("mat2");
        Assert.assertTrue(mat1.contentEquals(mat2));

        // Test case 2: Cloned material with a different render state should not be content-equal.
        Material mat3 = mat1.clone();
        mat3.setName("mat3");
        mat3.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.ModulateX2);
        Assert.assertFalse(mat1.contentEquals(mat3));

        // Test case 3: Two separately loaded materials with identical content should be content-equal.
        Material mat4 = assetManager.loadMaterial("Models/Sign Post/Sign Post.j3m");
        mat4.setName("mat4");
        Material mat5 = assetManager.loadMaterial("Models/Sign Post/Sign Post.j3m");
        mat5.setName("mat5");
        Assert.assertTrue(mat4.contentEquals(mat5));

        // Test case 4: Comparing textures - ensuring texture keys are identical for content equality.
        TextureKey originalKey = (TextureKey) mat4.getTextureParam("DiffuseMap").getTextureValue().getKey();
        TextureKey tex1key = new TextureKey("Models/Sign Post/Sign Post.jpg", false);
        tex1key.setGenerateMips(true);

        // The texture keys from the original and the newly loaded texture must be identical
        // for their corresponding textures, and thus the materials, to be considered identical.
        Assert.assertEquals(originalKey, tex1key);

        Texture tex1 = assetManager.loadTexture(tex1key);
        mat4.setTexture("DiffuseMap", tex1);
        Assert.assertTrue(mat4.contentEquals(mat5));

        // Test case 5: Changing texture properties should make materials no longer content-equal.
        tex1.setWrap(Texture.WrapMode.MirroredRepeat);
        Assert.assertFalse(mat4.contentEquals(mat5));

        // Test case 6: Comparing materials with different textures should result in non-equality.
        Texture tex2 = assetManager.loadTexture("Interface/Logo/Monkey.jpg");
        mat4.setTexture("DiffuseMap", tex2);
        Assert.assertFalse(mat4.contentEquals(mat5));

        // Test case 7: Two materials created with the same initial properties should be content-equal.
        Material mat6 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat6.setName("mat6");
        mat6.setColor("Color", ColorRGBA.Blue);
        Assert.assertTrue(mat1.contentEquals(mat6));

        // Test case 8: Changing a material parameter should make materials no longer content-equal.
        mat6.setColor("Color", ColorRGBA.Green);
        Assert.assertFalse(mat1.contentEquals(mat6));
    }

    @Test
    public void testMaterialCloneAndSave() {

        List<String> matDefs = Arrays.asList(Materials.UNSHADED, Materials.LIGHTING, Materials.PBR);

        Texture tex = assetManager.loadTexture("Common/Textures/MissingTexture.png");
        tex.setWrap(Texture.WrapMode.Repeat);

        int index = 0;
        for (String matDef : matDefs) {
            Material mat = new Material(assetManager, matDef);

            String name = index == 0 ? "ColorMap" : index == 1 ? "DiffuseMap" : "BaseColorMap";
            mat.setTexture(name, tex);

            Material clone = mat.clone();
            Assert.assertTrue(mat.contentEquals(clone));
            Assert.assertEquals(mat.contentHashCode(), clone.contentHashCode());
            test(mat, clone);

            Material copy = BinaryExporter.saveAndLoad(assetManager, mat);
            Assert.assertTrue(mat.contentEquals(copy));
            Assert.assertEquals(mat.contentHashCode(), copy.contentHashCode());
            test(mat, copy);

            index++;
        }
    }

    private void test(Material mat, Material copy) {
        for (MatParam def : mat.getMaterialDef().getMaterialParams()) {
            MatParam param = mat.getParam(def.getName());

            if (param != null) {
                MatParam mCopy = copy.getParam(def.getName());
                Assert.assertEquals(param, mCopy);
                Assert.assertEquals(param.hashCode(), mCopy.hashCode());

            } else {
                MatParam mCopy = copy.getMaterialDef().getMaterialParam(def.getName());
                Assert.assertEquals(def, mCopy);
                Assert.assertEquals(def.hashCode(), mCopy.hashCode());
            }
        }
    }
}
