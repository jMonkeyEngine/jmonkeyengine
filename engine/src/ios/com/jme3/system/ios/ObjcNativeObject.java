/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.system.ios;

/**
 * Java Object that represents a native iOS class. You can call methods and
 * retrieve objects from the native object via this class.
 *
 * @author normenhansen
 */
public class ObjcNativeObject {

    private final long nativeObject;

    /**
     * Creates a new native object representation
     *
     * @param nativeObject The id of the native object, created via cast of
     * object to jlong
     */
    public ObjcNativeObject(long nativeObject) {
        this.nativeObject = nativeObject;
    }

    /**
     * Performs the given selector on the native AppDelegate, equivalent to
     * calling a method in java.
     *
     * @param selector The selector (name of the method) to perform.
     * @return The object in form of a long id if any is returned.
     */
    public ObjcNativeObject performSelector(String selector) {
        return new ObjcNativeObject(performSelector(nativeObject, selector));
    }

    /**
     * Performs the given selector on the native AppDelegate, equivalent to
     * calling a method in java.
     *
     * @param selector The selector (name of the method) to perform.
     * @param object An object that was before returned from native code.
     * @return The object in form of a long id if any is returned.
     */
    public ObjcNativeObject performSelectorWithObject(String selector, ObjcNativeObject object) {
        return new ObjcNativeObject(performSelectorWithObject(nativeObject, selector, object.nativeObject));
    }

    /**
     * Performs the given selector on the native AppDelegate, run it on the main
     * thread.
     *
     * @param selector The selector (name of the method) to perform.
     */
    public void performSelectorOnMainThread(String selector) {
        performSelectorOnMainThread(nativeObject, selector);
    }

    /**
     * Performs the given selector on the native AppDelegate, run it on the main
     * thread.
     *
     * @param selector The selector (name of the method) to perform.
     * @param object An object that was before returned from native code.
     */
    public void performSelectorOnMainThreadWithObject(String selector, ObjcNativeObject object) {
        performSelectorOnMainThreadWithObject(nativeObject, selector, object.nativeObject);
    }

    /**
     * Performs the given selector on the native AppDelegate, run it in the
     * background.
     *
     * @param selector The selector (name of the method) to perform.
     */
    public void performSelectorInBackground(String selector) {
        performSelectorInBackground(nativeObject, selector);
    }

    /**
     * Performs the given selector on the native AppDelegate, run it in the
     * background
     *
     * @param selector The selector (name of the method) to perform.
     * @param object An object that was before returned from native code.
     */
    public void performSelectorInBackgroundWithObject(String selector, ObjcNativeObject object) {
        performSelectorInBackgroundWithObject(nativeObject, selector, object.nativeObject);
    }

    private static native long performSelector(long nativeObject, String selector);

    private static native long performSelectorWithObject(long nativeObject, String selector, long object);

    private static native void performSelectorOnMainThread(long nativeObject, String selector);

    private static native void performSelectorOnMainThreadWithObject(long nativeObject, String selector, long object);

    private static native void performSelectorInBackground(long nativeObject, String selector);

    private static native void performSelectorInBackgroundWithObject(long nativeObject, String selector, long object);
}
