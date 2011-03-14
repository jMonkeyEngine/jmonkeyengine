package com.jme3.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import org.junit.Test;

public class TrigonometryTest {

    @Test
    public void testVector2(){
        Vector2f original = new Vector2f(1, 2);
        Vector2f recreated = new Vector2f();

        float angle  = original.getAngle();
        float length = original.length();

        recreated.set( FastMath.cos(angle), FastMath.sin(angle) );
        recreated.multLocal(length);
        
        assertEquals( original.getX(), recreated.getX(), 0.000001 );
        assertEquals( original.getY(), recreated.getY(), 0.000001 );
    }
    
}
