package com.jme3.math;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import org.junit.Ignore;

public class RayTest {

    @Test
    public void testAimAtTarget() {
        Ray ray = new Ray();
        ray.setOrigin(new Vector3f(1, 2, 3));
        ray.aimAtTarget(new Vector3f(2, 3, 4));
        assertEquals(Vector3f.UNIT_XYZ.normalize(), ray.getDirection());
    }

}