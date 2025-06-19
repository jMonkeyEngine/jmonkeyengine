package com.jme3.anim;

import com.jme3.anim.util.AnimMigrationUtils;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.MatParamOverride;
import com.jme3.scene.Spatial;
import com.jme3.util.SafeArrayList;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author capdevon
 */
public class SkinningControlTest {

    @Test
    public void testSkinningControl() {
        AssetManager assetManager = new DesktopAssetManager(true);

        List<String> assets = Arrays.asList(
                "Models/Jaime/Jaime.j3o",
                "Models/Oto/Oto.mesh.xml",
                "Models/Sinbad/Sinbad.mesh.xml",
                "Models/Elephant/Elephant.mesh.xml");

        for (String asset : assets) {
            Spatial model = assetManager.loadModel(asset);
            AnimMigrationUtils.migrate(model);

            SkinningControl sc = model.getControl(SkinningControl.class);
            model.removeControl(sc);
            model.addControl(sc);

            validateSkinningControl(sc);

            Spatial copy = BinaryExporter.saveAndLoad(assetManager, model);
            SkinningControl scCopy = copy.getControl(SkinningControl.class);

            validateSkinningControl(scCopy);
        }
    }

    private void validateSkinningControl(SkinningControl sc) {

        SafeArrayList<MatParamOverride> mpos = sc.getSpatial().getLocalMatParamOverrides();
        Assert.assertEquals(2, mpos.size());

        int numberOfBones = 0;
        int boneMatrices = 0;
        for (MatParamOverride mpo : mpos) {
            Assert.assertTrue(mpo.isEnabled());
            Assert.assertNull(mpo.getValue());

            if (mpo.getName().equals("NumberOfBones")) {
                numberOfBones++;
            } else if (mpo.getName().equals("BoneMatrices")) {
                boneMatrices++;
            }
        }

        Assert.assertEquals(1, numberOfBones);
        Assert.assertEquals(1, boneMatrices);

        Assert.assertTrue(sc.isHardwareSkinningPreferred());
        Assert.assertFalse(sc.isHardwareSkinningUsed());
    }
}
