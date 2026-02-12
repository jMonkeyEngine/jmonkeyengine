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
package com.jme3.util.struct;

import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.memory.MemorySize;

/**
 * Classes implementing this interface are considered struct-like constructs.
 * A struct's members are independently interpreted and managed by
 * {@link StructLayout StructLayouts}.
 *
 * <pre>{@code public class Projectile implements Struct {
 *     @Member(0) public float mass;
 *     @Member(1) public int damage;
 *     @Member(2) public Vector3f velocity;
 * }}</pre>
 *
 * <p>Fields that should be considered members of the struct must be annotated with
 * {@link Member}. Struct members are required to be public and non-static. Fields
 * not annotated with {@link Member} are ignored. Be aware that layouts may have
 * trouble writing to final members in cases where writing directly to the member's
 * value is not possible (i.e. all primitive types).</p>
 *
 * <p>Struct members may not be null, array and collection members must contain at
 * least one element. Array and collection elements may not be null and should be
 * the same size in bytes.</p>
 *
 * <p>The integer position passed to the {@link Member} annotation on each struct member
 * determines the order in which the members are arranged in the struct, where lower
 * values place the member closer to the front of the struct. The member positions do
 * not need to follow any particular pattern and can be any valid integer. Having two
 * or more members sharing the same position value results in undefined behavior.</p>
 * 
 * @author Riccardo Balbo
 * @author codex
 */
public interface Struct {

    default void push(StructLayout layout, BufferMapping mapping) {
        push(layout, mapping, false);
    }

    default void push(StructLayout layout, BufferMapping mapping, boolean force) {
        layout.updateBuffer(this, mapping, force);
    }

    default void pull(StructLayout layout, BufferMapping mapping) {
        layout.updateStruct(mapping.getBytes().clear(), this);
    }

    default MemorySize size(StructLayout layout) {
        return MemorySize.bytes(layout.structSize(this));
    }

}