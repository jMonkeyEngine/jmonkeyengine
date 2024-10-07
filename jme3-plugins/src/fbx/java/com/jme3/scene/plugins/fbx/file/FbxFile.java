/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.scene.plugins.fbx.file;

import java.util.ArrayList;
import java.util.List;

public class FbxFile {

    public List<FbxElement> rootElements = new ArrayList<>();
    public long version;

    /**
     * Between file versions 7400 and 7500, the "endOffset", "propCount", and
     * "propsLength" fields in an FBX element were extended, from 4 bytes to 8
     * bytes each.
     *
     * @return true for 8-byte offsets, otherwise false
     */
    public boolean hasExtendedOffsets() {
        if (version >= 7500L) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Between file versions 7400 and 7500, the FBX block sentinel was reduced,
     * from 13 bytes to 9 bytes.
     *
     * @return the number of bytes in the block sentinel (&ge;0)
     */
    public int numSentinelBytes() {
        if (version >= 7500L) {
            return 9;
        } else {
            return 13;
        }
    }

    @Override
    public String toString() {
        return "FBXFile[version=" + version + ",numElements=" + rootElements.size() + "]";
    }
}
