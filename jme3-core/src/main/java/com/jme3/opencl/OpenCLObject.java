/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.opencl;

/**
 * Base interface of all native OpenCL objects.
 * This interface provides the functionality for savely release the object.
 * @author shaman
 */
public interface OpenCLObject {
    
    /**
     * Releaser for an {@link OpenCLObject}.
     * Implementations of this interface must not hold a reference to the
     * {@code OpenCLObject} directly.
     */
    public static interface ObjectReleaser {
        /**
         * Releases the native resources of the associated {@link OpenCLObject}.
         * This method must be guarded against multiple calls: only the first
         * call should release, the next ones must not throw an exception.
         */
        void release();
    }
    /**
     * Returns the releaser object. Multiple calls should return the same object.
     * The ObjectReleaser is used to release the OpenCLObject when it is garbage
     * collected. Therefore, the returned object must not hold a reference to
     * the OpenCLObject.
     * @return the object releaser
     */
    ObjectReleaser getReleaser();
    /**
     * Releases this native object.
	 * 
     * Should delegate to {@code getReleaser().release()}.
     */
    void release();
    /**
     * Registers this object for automatic releasing on garbage collection.
     * By default, OpenCLObjects are not registered in the
     * {@link OpenCLObjectManager}, you have to release it manually 
     * by calling {@link #release() }.
     * Without registering or releasing, a memory leak might occur.
	 * <br>
	 * Returns {@code this} to allow calls like
	 * {@code Buffer buffer = clContext.createBuffer(1024).register();}.
	 * @return {@code this}
     */
    OpenCLObject register();
}
