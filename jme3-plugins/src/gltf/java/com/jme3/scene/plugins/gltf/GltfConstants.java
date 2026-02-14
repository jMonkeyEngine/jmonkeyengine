/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.scene.plugins.gltf;

/**
 * A package-private class summarizing GL constants that are used in the context of glTF loading.
 */
class GltfConstants {

    /**
     * GL_BYTE, 5120, 0x1400
     */
    static final int GL_BYTE = 0x1400;

    /**
     * GL_UNSIGNED_BYTE, 5121, 0x1401
     */
    static final int GL_UNSIGNED_BYTE = 0x1401;

    /**
     * GL_SHORT, 5122, 0x1402
     */
    static final int GL_SHORT = 0x1402;

    /**
     * GL_UNSIGNED_SHORT, 5123, 0x1403
     */
    static final int GL_UNSIGNED_SHORT = 0x1403;

    /**
     * GL_UNSIGNED_INT, 5125, 0x1405
     */
    static final int GL_UNSIGNED_INT = 0x1405;

    /**
     * GL_FLOAT, 5126, 0x1406
     */
    static final int GL_FLOAT = 0x1406;

    /**
     * Private constructor to prevent instantiation
     */
    private GltfConstants() {
        // Private constructor to prevent instantiation
    }
}
