/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.math;

import java.util.Random;
import org.junit.*;
import static org.junit.Assert.*;
import static com.jme3.math.FastMath.*;

public class QuatMultTest {
    Random rand;
    @Before
    public void setUp() {
        rand = new Random(0);
    }
    
    @After
    public void tearDown() {
    }
   
    @Test
    public void test() {
        Quaternion quat = new Quaternion();
        Vector3f vect = new Vector3f();
        Vector3f store60 = new Vector3f(); //stores result of the current implementation
        Vector3f store15 = new Vector3f(); //stores result of the proposed change

//        final int reps = 100000;
        final int reps = 10;
         
        for(int i = 0; i < reps; i++) {
            randQuat(quat);
            
            randVect(vect);
            
            quat.mult(vect, store60);
            mult15V2(quat, vect, store15);
    
            //eps stands for the epsillon in the approximateEquals method
            //set your desired eps
            
            //final float eps = 0.00001f; 
            //final float eps = 0.001f; 
            final float eps = 0.08f;
            
            final boolean equals = approximateEquals(store60, store15, eps);
            if(!equals) {
                //print data if they are not equal with given epsillon
                System.out.println("Iteration: " + i);
                System.out.println("quat " + quat + ", vect " + vect );
                System.out.println("store60 " + store60 + ", store15 " + store15);
                System.out.println("diff " + store60.subtract(store15));
                
                System.out.println("("+approximateEquals(store60.x, store15.x, eps)+","+
                        approximateEquals(store60.y, store15.y, eps)+","+
                        approximateEquals(store60.z, store15.z, eps)+")");
                //Failing this test means that the two methods produce results that 
                //are different with respect to the eps
                
                //To determine which method provides more accurate result, use
                //your favourite maths software or calculator to calculate the
                //quaternion vector product from the data provided and compare
                //the results
            }
            assertTrue(equals);
        }        
    }
    public static boolean approximateEquals(Vector3f a, Vector3f b, float eps) {
        return approximateEquals(a.x, b.x, eps) && approximateEquals(a.y, b.y, eps) &&
                approximateEquals(a.z, b.z, eps);
    }
    public static boolean approximateEquals(float a, float b, float eps) {
        if (a == b) {
            return true;
        } else {
            return (abs(a - b) / Math.max(abs(a), abs(b))) <= eps;
        }
    }
    
    public void randQuat(Quaternion quat) {
        quat.fromAngles(TWO_PI*rand.nextFloat(), TWO_PI*rand.nextFloat(), TWO_PI*rand.nextFloat());
    }
    public void randVect(Vector3f v) {
        v.set(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
        v.multLocal(10000f).subtractLocal(5000f, 5000f, 5000f);
    }
    public static void mult15V2(Quaternion q, Vector3f v, Vector3f s) {
        //15 mult, 15 add
        float x = q.getX();
        float y = q.getY();
        float z = q.getZ();
        float w = q.getW();
        
        //v + 2*q.xyz cross (q.xyz cross v + w*v )
        //q.xyz x v.xyz + w*v
        float vx = y*v.z - z*v.y + w*v.x;
        float vy = z*v.x - x*v.z + w*v.y;
        float vz = x*v.y - y*v.x + w*v.z;
        vx += vx; vy += vy; vz += vz;
        s.x = v.x + y*vz - z*vy;
        s.y = v.y + z*vx - x*vz;
        s.z = v.z + x*vy - y*vx;
    }
}
