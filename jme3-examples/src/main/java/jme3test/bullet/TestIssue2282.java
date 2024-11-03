/*
 * Copyright (c) 2024 jMonkeyEngine
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

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.PhysicsSpace;

/**
 * Test case for JME issue #2282: VerifyError while creating
 * PhysicsSpace with AXIS_SWEEP_3 broadphase acceleration.
 *
 * <p>If successful, the application will print "SUCCESS" and terminate without
 * crashing. If unsuccessful, the application will terminate with a VerifyError
 * and no stack trace.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestIssue2282 extends SimpleApplication {

    /**
     * Main entry point for the TestIssue2282 application.
     *
     * @param args array of command-line arguments (unused)
     */
    public static void main(String[] args) {
        TestIssue2282 test = new TestIssue2282();
        test.start();
    }

    /**
     * Initialize the TestIssue2282 application.
     */
    @Override
    public void simpleInitApp() {
        new PhysicsSpace(PhysicsSpace.BroadphaseType.AXIS_SWEEP_3);
        System.out.println("SUCCESS");
        stop();
    }
}
