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
package com.jme3.bullet.collision;

/**
 * Named collision flags for a {@link PhysicsCollisionObject}. Values must agree
 * with those in BulletCollision/CollisionDispatch/btCollisionObject.h
 *
 * @author Stephen Gold sgold@sonic.net
 * @see PhysicsCollisionObject#getCollisionFlags(long)
 */
public class CollisionFlag {
    /**
     * flag for a static object
     */
    final public static int STATIC_OBJECT = 0x1;
    /**
     * flag for a kinematic object
     */
    final public static int KINEMATIC_OBJECT = 0x2;
    /**
     * flag for an object with no contact response, such as a PhysicsGhostObject
     */
    final public static int NO_CONTACT_RESPONSE = 0x4;
    /**
     * flag to enable a custom material callback for per-triangle
     * friction/restitution (not used by JME)
     */
    final public static int CUSTOM_MATERIAL_CALLBACK = 0x8;
    /**
     * flag for a character object, such as a PhysicsCharacter
     */
    final public static int CHARACTER_OBJECT = 0x10;
    /**
     * flag to disable debug visualization (not used by JME)
     */
    final public static int DISABLE_VISUALIZE_OBJECT = 0x20;
    /**
     * flag to disable parallel/SPU processing (not used by JME)
     */
    final public static int DISABLE_SPU_COLLISION_PROCESSING = 0x40;
    /**
     * flag not used by JME
     */
    final public static int HAS_CONTACT_STIFFNESS_DAMPING = 0x80;
    /**
     * flag not used by JME
     */
    final public static int HAS_CUSTOM_DEBUG_RENDERING_COLOR = 0x100;
    /**
     * flag not used by JME
     */
    final public static int HAS_FRICTION_ANCHOR = 0x200;
    /**
     * flag not used by JME
     */
    final public static int HAS_COLLISION_SOUND_TRIGGER = 0x400;
}
