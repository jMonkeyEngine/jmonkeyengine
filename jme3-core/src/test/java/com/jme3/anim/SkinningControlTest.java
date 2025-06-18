package com.jme3.anim;

import com.jme3.anim.util.AnimMigrationUtils;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.MatParamOverride;
import com.jme3.scene.Spatial;
import com.jme3.util.SafeArrayList;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author capdevon
 */
public class SkinningControlTest {

    @Test
    public void testSkinningControl() {
        AssetManager assetManager = new DesktopAssetManager(true);

        Spatial model = assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
        AnimMigrationUtils.migrate(model);

        SkinningControl sc = model.getControl(SkinningControl.class);
        model.removeControl(sc);
        model.addControl(sc);

        validateSkinningControl(sc);
    }

    private void validateSkinningControl(SkinningControl sc) {

        SafeArrayList<MatParamOverride> mpos = sc.getSpatial().getLocalMatParamOverrides();
        Assert.assertEquals(2, mpos.size());

        for (MatParamOverride mpo : mpos) {
            Assert.assertTrue(mpo.isEnabled());
            Assert.assertNull(mpo.getValue());
        }

        Assert.assertTrue(sc.isHardwareSkinningPreferred());
        Assert.assertFalse(sc.isHardwareSkinningUsed());

        Assert.assertNotNull(sc.getTargets());
        Assert.assertEquals(7, sc.getTargets().length);
    }
}
