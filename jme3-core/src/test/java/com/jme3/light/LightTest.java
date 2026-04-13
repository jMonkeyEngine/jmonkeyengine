/*
 * Copyright (c) 2009-2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.light;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the light classes work correctly.
 */
public class LightTest {

    private static final float TOLERANCE = 1e-6f;

    // -----------------------------------------------------------------------
    // DirectionalLight
    // -----------------------------------------------------------------------

    @Test
    public void testDirectionalLightDefaultConstructor() {
        DirectionalLight light = new DirectionalLight();
        Assert.assertEquals(Light.Type.Directional, light.getType());
        Assert.assertNotNull(light.getDirection());
        Assert.assertNotNull(light.getColor());
        Assert.assertTrue(light.isEnabled());
    }

    @Test
    public void testDirectionalLightConstructorWithDirection() {
        Vector3f dir = new Vector3f(0f, -1f, 0f);
        DirectionalLight light = new DirectionalLight(dir);
        Assert.assertEquals(dir, light.getDirection());
    }

    @Test
    public void testDirectionalLightConstructorWithDirectionAndColor() {
        Vector3f dir = new Vector3f(0f, -1f, 0f);
        ColorRGBA color = new ColorRGBA(1f, 0f, 0f, 1f);
        DirectionalLight light = new DirectionalLight(dir, color);
        Assert.assertEquals(dir, light.getDirection());
        Assert.assertEquals(color, light.getColor());
    }

    @Test
    public void testDirectionalLightSetDirectionNormalizesVector() {
        DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3f(0f, -2f, 0f)); // not unit vector
        float len = light.getDirection().length();
        Assert.assertEquals(1f, len, TOLERANCE);
    }

    @Test
    public void testDirectionalLightNameAndEnabled() {
        DirectionalLight light = new DirectionalLight();
        light.setName("sun");
        light.setEnabled(false);
        Assert.assertEquals("sun", light.getName());
        Assert.assertFalse(light.isEnabled());
    }

    @Test
    public void testDirectionalLightClone() {
        DirectionalLight original = new DirectionalLight(
                new Vector3f(0f, -1f, 0f), ColorRGBA.White);
        DirectionalLight cloned = original.clone();
        Assert.assertNotSame(original, cloned);
        Assert.assertEquals(original.getDirection(), cloned.getDirection());
        Assert.assertNotSame(original.getDirection(), cloned.getDirection());
    }

    @Test
    public void testDirectionalLightToString() {
        DirectionalLight light = new DirectionalLight();
        String s = light.toString();
        Assert.assertNotNull(s);
        Assert.assertTrue(s.contains("DirectionalLight"));
    }

    // -----------------------------------------------------------------------
    // PointLight
    // -----------------------------------------------------------------------

    @Test
    public void testPointLightDefaultConstructor() {
        PointLight light = new PointLight();
        Assert.assertEquals(Light.Type.Point, light.getType());
        Assert.assertNotNull(light.getPosition());
        Assert.assertEquals(0f, light.getRadius(), 0f);
    }

    @Test
    public void testPointLightConstructorWithPosition() {
        Vector3f pos = new Vector3f(1f, 2f, 3f);
        PointLight light = new PointLight(pos);
        Assert.assertEquals(pos, light.getPosition());
    }

    @Test
    public void testPointLightRadius() {
        PointLight light = new PointLight();
        light.setRadius(10f);
        Assert.assertEquals(10f, light.getRadius(), TOLERANCE);
        // invRadius should be 1/10
        Assert.assertEquals(0.1f, light.getInvRadius(), TOLERANCE);
    }

    @Test
    public void testPointLightRadiusZeroMeansUnlimited() {
        PointLight light = new PointLight();
        light.setRadius(0f);
        Assert.assertEquals(0f, light.getRadius(), 0f);
        Assert.assertEquals(0f, light.getInvRadius(), 0f);
    }

    @Test
    public void testPointLightClone() {
        PointLight original = new PointLight(new Vector3f(1f, 2f, 3f), ColorRGBA.White, 5f);
        PointLight cloned = original.clone();
        Assert.assertNotSame(original, cloned);
        Assert.assertEquals(original.getPosition(), cloned.getPosition());
        Assert.assertEquals(original.getRadius(), cloned.getRadius(), 0f);
    }

    @Test
    public void testPointLightToString() {
        PointLight light = new PointLight();
        Assert.assertNotNull(light.toString());
    }

    // -----------------------------------------------------------------------
    // AmbientLight
    // -----------------------------------------------------------------------

    @Test
    public void testAmbientLightDefaultConstructor() {
        AmbientLight light = new AmbientLight();
        Assert.assertEquals(Light.Type.Ambient, light.getType());
    }

    @Test
    public void testAmbientLightConstructorWithColor() {
        ColorRGBA color = new ColorRGBA(0.1f, 0.1f, 0.1f, 1f);
        AmbientLight light = new AmbientLight(color);
        Assert.assertEquals(color, light.getColor());
    }

    @Test
    public void testAmbientLightToString() {
        AmbientLight light = new AmbientLight();
        Assert.assertNotNull(light.toString());
    }

    // -----------------------------------------------------------------------
    // SpotLight
    // -----------------------------------------------------------------------

    @Test
    public void testSpotLightDefaultConstructor() {
        SpotLight light = new SpotLight();
        Assert.assertEquals(Light.Type.Spot, light.getType());
        Assert.assertNotNull(light.getPosition());
        Assert.assertNotNull(light.getDirection());
    }

    @Test
    public void testSpotLightPositionAndDirection() {
        Vector3f pos = new Vector3f(0f, 5f, 0f);
        Vector3f dir = new Vector3f(0f, -1f, 0f);
        SpotLight light = new SpotLight(pos, dir);
        Assert.assertEquals(pos, light.getPosition());
        Assert.assertEquals(dir, light.getDirection());
    }

    @Test
    public void testSpotLightRange() {
        SpotLight light = new SpotLight();
        light.setSpotRange(20f);
        Assert.assertEquals(20f, light.getSpotRange(), TOLERANCE);
    }

    @Test
    public void testSpotLightAngles() {
        SpotLight light = new SpotLight();
        light.setSpotInnerAngle(FastMath.DEG_TO_RAD * 10f);
        light.setSpotOuterAngle(FastMath.DEG_TO_RAD * 20f);
        Assert.assertEquals(FastMath.DEG_TO_RAD * 10f, light.getSpotInnerAngle(), TOLERANCE);
        Assert.assertEquals(FastMath.DEG_TO_RAD * 20f, light.getSpotOuterAngle(), TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Light base class
    // -----------------------------------------------------------------------

    @Test
    public void testLightColorSetGet() {
        DirectionalLight light = new DirectionalLight();
        light.setColor(ColorRGBA.Red);
        Assert.assertEquals(ColorRGBA.Red, light.getColor());
    }

    @Test
    public void testLightFrustumFlags() {
        DirectionalLight light = new DirectionalLight();
        light.setFrustumCheckNeeded(false);
        Assert.assertFalse(light.isFrustumCheckNeeded());
        light.setIntersectsFrustum(true);
        Assert.assertTrue(light.isIntersectsFrustum());
    }
}
