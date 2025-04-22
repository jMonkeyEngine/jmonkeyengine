package com.jme3.effect.influencers;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.Vector3f;
import org.junit.Assert;
import org.junit.Test;

/**
 * Automated tests for the {@code ParticleInfluencer} class.
 *
 * @author capdevon
 */
public class ParticleInfluencerTest {

    /**
     * Tests cloning, serialization and de-serialization of a {@code NewtonianParticleInfluencer}.
     */
    @Test
    public void testNewtonianParticleInfluencer() {
        AssetManager assetManager = new DesktopAssetManager(true);

        NewtonianParticleInfluencer inf = new NewtonianParticleInfluencer();
        inf.setNormalVelocity(1);
        inf.setSurfaceTangentFactor(0.5f);
        inf.setSurfaceTangentRotation(2.5f);
        inf.setInitialVelocity(new Vector3f(0, 1, 0));
        inf.setVelocityVariation(2f);

        NewtonianParticleInfluencer clone = (NewtonianParticleInfluencer) inf.clone();
        assertEquals(inf, clone);
        Assert.assertNotSame(inf.temp, clone.temp);

        NewtonianParticleInfluencer copy = BinaryExporter.saveAndLoad(assetManager, inf);
        assertEquals(inf, copy);
    }

    private void assertEquals(NewtonianParticleInfluencer inf, NewtonianParticleInfluencer clone) {
        Assert.assertEquals(inf.getNormalVelocity(), clone.getNormalVelocity(), 0.001f);
        Assert.assertEquals(inf.getSurfaceTangentFactor(), clone.getSurfaceTangentFactor(), 0.001f);
        Assert.assertEquals(inf.getSurfaceTangentRotation(), clone.getSurfaceTangentRotation(), 0.001f);
        Assert.assertEquals(inf.getInitialVelocity(), clone.getInitialVelocity());
        Assert.assertEquals(inf.getVelocityVariation(), clone.getVelocityVariation(), 0.001f);
    }

    /**
     * Tests cloning, serialization and de-serialization of a {@code RadialParticleInfluencer}.
     */
    @Test
    public void testRadialParticleInfluencer() {
        AssetManager assetManager = new DesktopAssetManager(true);

        RadialParticleInfluencer inf = new RadialParticleInfluencer();
        inf.setHorizontal(true);
        inf.setOrigin(new Vector3f(0, 1, 0));
        inf.setRadialVelocity(2f);
        inf.setInitialVelocity(new Vector3f(0, 1, 0));
        inf.setVelocityVariation(2f);

        RadialParticleInfluencer clone = (RadialParticleInfluencer) inf.clone();
        assertEquals(inf, clone);
        Assert.assertNotSame(inf.temp, clone.temp);

        RadialParticleInfluencer copy = BinaryExporter.saveAndLoad(assetManager, inf);
        assertEquals(inf, copy);
    }

    private void assertEquals(RadialParticleInfluencer inf, RadialParticleInfluencer clone) {
        Assert.assertEquals(inf.isHorizontal(), clone.isHorizontal());
        Assert.assertEquals(inf.getOrigin(), clone.getOrigin());
        Assert.assertEquals(inf.getRadialVelocity(), clone.getRadialVelocity(), 0.001f);
        Assert.assertEquals(inf.getInitialVelocity(), clone.getInitialVelocity());
        Assert.assertEquals(inf.getVelocityVariation(), clone.getVelocityVariation(), 0.001f);
    }

}
