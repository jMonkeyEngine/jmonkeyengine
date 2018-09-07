/*
 * Copyright (c) 2018 jMonkeyEngine
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
package jme3test.bullet;

/**
 * Test case for JME issue #883: extra physicsTicks in ThreadingType.PARALLEL.
 *
 * If successful, physics time and frame time will advance at the same rate.
 */
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;

public class TestIssue883 extends SimpleApplication {

    boolean firstPrint = true;
    float timeToNextPrint = 1f; // in seconds
    double frameTime; // in seconds
    double physicsTime; // in seconds

    public static void main(String[] args) {
        TestIssue883 app = new TestIssue883();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        BulletAppState bulletAppState = new BulletAppState() {
            @Override
            public void physicsTick(PhysicsSpace space, float timeStep) {
                physicsTime += timeStep;
            }
        };
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
    }

    @Override
    public void simpleUpdate(float tpf) {
        frameTime += tpf;

        if (timeToNextPrint > 0f) {
            timeToNextPrint -= tpf;
            return;
        }

        if (firstPrint) { // synchronize
            frameTime = 0.;
            physicsTime = 0.;
            firstPrint = false;
        }

        System.out.printf(" frameTime= %s   physicsTime= %s%n",
                frameTime, physicsTime);
        timeToNextPrint = 1f;
    }
}
