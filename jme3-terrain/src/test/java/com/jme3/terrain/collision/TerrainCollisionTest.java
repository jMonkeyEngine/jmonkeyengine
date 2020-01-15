package com.jme3.terrain.collision;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TerrainCollisionTest extends BaseAWTTest {
    TerrainQuad quad;

    @Before
    public void initQuad() {
        Texture heightMapImage = getAssetManager().loadTexture("Textures/Terrain/splat/mountains512.png");
        AbstractHeightMap map = new ImageBasedHeightMap(heightMapImage.getImage(), 0.25f);
        map.load();
        quad = new TerrainQuad("terrain", 65, 513, map.getHeightMap());
    }

    /**
     * Due to a previous bug, when no collision should happen, the CollisionResults struct was still populated, leading
     * to an incoherency of data and ghost collisions when passing a non-empty CR.
     */
    @Test
    public void testNoCollision() {
        Ray r = new Ray(new Vector3f(0f, 40f, 0f), Vector3f.UNIT_Y.negate());
        r.setLimit(0.1f);
        CollisionResults cr = new CollisionResults();
        long l = System.nanoTime();
        int cw = quad.collideWith(r, cr);
        System.out.println((System.nanoTime() - l) + " ns");

        Assert.assertEquals(0, cw);
        Assert.assertEquals(0, cr.size());
        Assert.assertEquals(null, cr.getClosestCollision());
        Assert.assertEquals(null, cr.getFarthestCollision());
    }

    @Test
    public void testPerpendicularCollision() {
        Ray r = new Ray(new Vector3f(0f, 40f, 0f), Vector3f.UNIT_Y.negate());
        CollisionResults cr = new CollisionResults();
        int cw = quad.collideWith(r, cr);

        Assert.assertEquals(1, cw);
        Assert.assertEquals(1, cr.size());
        Assert.assertEquals(new Vector3f(0f, 28f, 0f), cr.getClosestCollision().getContactPoint());
        Assert.assertEquals(new Vector3f(-0.5144958f, 0.6859944f, 0.5144958f), cr.getClosestCollision().getContactNormal());
        Assert.assertEquals(12, cr.getClosestCollision().getDistance(), 0.01d);
        Assert.assertEquals(0, cr.getClosestCollision().getTriangleIndex());
    }

    @Test
    public void testMultiCollision() {
        // Ray parameters obtained by using TerrainTestCollision (manual inspection of a feasible ray and commenting out setLocalScale(2)
        Ray r = new Ray(new Vector3f(-38.689114f, 35.622643f, -40.222355f), new Vector3f(0.68958646f, 0.0980845f, 0.7175304f));

        CollisionResults cr = new CollisionResults();
        long l = System.nanoTime();
        int cw = quad.collideWith(r, cr);
        System.out.println((System.nanoTime() - l) + " ns");
        Assert.assertEquals(6, cw);
        Assert.assertEquals(6, cr.size());

    }

    @Test
    public void testPreventRegression() {
        // This test is as the multi collision changes lead to a regression where sometimes a collision was ignored
        // Ray parameters obtained by using TerrainTestCollision (manual inspection of a feasible ray and commenting out setLocalScale(2))
        Ray r = new Ray(new Vector3f(101.61858f, 78.35965f, 17.645157f), new Vector3f(-0.4188528f, -0.56462675f, 0.71116734f));

        CollisionResults cr = new CollisionResults();
        quad.collideWith(r, cr);

        Assert.assertEquals(3, cr.size());
        Assert.assertEquals(68.1499f, cr.getClosestCollision().getDistance(), 0.01f);
        Assert.assertEquals(new Vector3f(73.07381f, 39.88039f, 66.11114f), cr.getClosestCollision().getContactPoint());
        Assert.assertEquals(new Vector3f(0.9103665f, 0.33104235f, -0.24828176f), cr.getClosestCollision().getContactNormal());
    }

}
