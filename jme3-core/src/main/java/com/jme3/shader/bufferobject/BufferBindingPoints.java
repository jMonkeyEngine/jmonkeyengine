/*
 * Copyright (c) 2026 jMonkeyEngine
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
package com.jme3.shader.bufferobject;

/**
 * Shared binding-point policy for engine-owned buffer objects.
 */
public final class BufferBindingPoints {

    public static final String MAT_PARAMS_BLOCK_NAME = "m_MatParams";
    public static final String FRAME_BLOCK_NAME = "g_Frame";
    public static final String OBJECT_BLOCK_NAME = "g_Object";
    public static final String LIGHTS_BLOCK_NAME = "g_Lights";

    public static final String MAT_PARAMS_DEFINE = "JME_UBO_MAT_PARAMS";
    public static final String FRAME_DEFINE = "JME_UBO_FRAME";
    public static final String OBJECT_DEFINE = "JME_UBO_OBJECT";
    public static final String LIGHTS_DEFINE = "JME_UBO_LIGHTS";

    public static final int ENGINE_RESERVED_BINDINGS = 7;

    private BufferBindingPoints() {
    }

    public enum EngineBinding {
        Lights(1),
        Data1(2),
        Data2(3),
        Data3(4),
        Frame(5),
        Object(6),
        MatParams(7);

        private final int offsetFromEnd;

        EngineBinding(int offsetFromEnd) {
            this.offsetFromEnd = offsetFromEnd;
        }
    }

    /**
     * Returns the first binding point available to users. Engine-owned bindings
     * are allocated from the end of the available range.
     *
     * @param maxBindings runtime binding-point limit
     * @return number of user binding points
     */
    public static int getUserBindingCount(int maxBindings) {
        if (maxBindings < ENGINE_RESERVED_BINDINGS) {
            return Math.max(0, maxBindings);
        }
        return maxBindings - ENGINE_RESERVED_BINDINGS;
    }

    /**
     * Returns the binding point reserved for an engine-owned block.
     *
     * @param maxBindings runtime binding-point limit
     * @param binding engine binding
     * @return binding point, or -1 if the limit cannot reserve engine slots
     */
    public static int getEngineBinding(int maxBindings, EngineBinding binding) {
        if (maxBindings < ENGINE_RESERVED_BINDINGS) {
            return -1;
        }
        return maxBindings - binding.offsetFromEnd;
    }

    /**
     * Returns true if the binding point belongs to the engine-reserved range.
     *
     * @param maxBindings runtime binding-point limit
     * @param bindingPoint binding point to test
     * @return true if reserved for engine use
     */
    public static boolean isEngineReserved(int maxBindings, int bindingPoint) {
        if (maxBindings < ENGINE_RESERVED_BINDINGS) {
            return false;
        }
        return bindingPoint >= getUserBindingCount(maxBindings) && bindingPoint < maxBindings;
    }
}
