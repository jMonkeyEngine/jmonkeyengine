package com.jme3.math;

import junit.framework.TestCase;

public class QuaternionTest extends TestCase{

    public void testIsValidQuaternion(){
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(Float.NaN, 2.1f, 3.0f, 1.5f)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(1f, Float.NaN, 3.0f, 1.5f)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(1f, 2.1f, Float.NaN, 1.5f)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(1f, 2.1f, 3.0f, Float.NaN)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(Float.POSITIVE_INFINITY, 1.5f, 1.9f, 2.0f)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(Float.NEGATIVE_INFINITY, 2.5f, 8.2f, 3.0f)));
        assertFalse(Quaternion.isValidQuaternion(null));

        assertTrue(Quaternion.isValidQuaternion(new Quaternion()));
        assertTrue(Quaternion.isValidQuaternion(new Quaternion(1.5f, -5.7f, 8.2f, 3.0f)));
    }
}