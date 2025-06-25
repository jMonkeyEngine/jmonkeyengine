package com.jme3.material;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.audio.AudioNodeTest;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Texture;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * @author capdevon
 */
public class MaterialCompareTest {

    @Test
    public void testMaterialCloneAndSave() {

        URL config = MaterialCompareTest.class.getResource("/com/jme3/asset/Desktop.cfg");
        AssetManager assetManager = JmeSystem.newAssetManager(config);

        List<String> matDefs = Arrays.asList(Materials.UNSHADED, Materials.LIGHTING, Materials.PBR);

        Texture tex = assetManager.loadTexture("Common/Textures/MissingMaterial.png");
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
