/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
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
package com.jme3.network.serializing;

import com.jme3.network.serializing.serializers.FieldSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class PredicateSerializerTest {

    // A dummy message representing a custom data structure (e.g., mimicking a Record or Kotlin Data Class)
    @Serializable
    public static class SpecialMessage {
        public String data;
        public SpecialMessage() {}
    }

    // A standard message that should fall back to the default FieldSerializer
    @Serializable
    public static class StandardMessage {
        public int id;
        public StandardMessage() {}
    }
    
    @Serializable(serializer = Serializer.class)
    public static class SpecialMessageWithAlternateDefault {
        public SpecialMessageWithAlternateDefault() {}
    }

    // Simulates a class where the user EXPLICITLY requested a custom serializer
    @Serializable(serializer = SecondMockSerializer.class)
    public static class ExplicitCustomMessage {
        public ExplicitCustomMessage() {}
    }
    
    public static class SecondMockSerializer extends Serializer {
        @Override
        public <T> T readObject(ByteBuffer data, Class<T> c) { return null; }
        @Override
        public void writeObject(ByteBuffer buffer, Object object) {}
    }

    // A mock custom serializer to test the routing mechanism
    public static class MockCustomSerializer extends Serializer {
        @Override
        public <T> T readObject(ByteBuffer data, Class<T> c) {
            return null; // Mock implementation
        }

        @Override
        public void writeObject(ByteBuffer buffer, Object object) {
            // Mock implementation
        }
    }

    @BeforeEach
    public void setupRegistry() {
        Serializer.initialize();
        
        // Register the predicate: any class matching SpecialMessage gets the MockCustomSerializer
        Serializer.registerSerializerPredicate(
            cls -> cls == SpecialMessage.class, 
            new MockCustomSerializer()
        );
    }

    @Test
    public void testPredicateSerializerSelectionOverridesDefault() {
        // Register the class
        Serializer.registerClass(SpecialMessage.class);
        
        SerializerRegistration registration = Serializer.getExactSerializerRegistration(SpecialMessage.class);
        
        assertNotNull(registration, "Registration should not be null");
        // Verify the predicate successfully intercepted the registration and applied the mock serializer
        assertEquals(MockCustomSerializer.class, registration.getSerializer().getClass(), 
                "SpecialMessage should be routed to MockCustomSerializer via predicate");
    }

    @Test
    public void testStandardClassFallsBackToFieldSerializer() {
        // Register a normal POJO
        Serializer.registerClass(StandardMessage.class);
        
        SerializerRegistration registration = Serializer.getExactSerializerRegistration(StandardMessage.class);
        
        assertNotNull(registration, "Registration should not be null");
        // Verify classes that do not match the predicate still receive the default FieldSerializer
        assertEquals(FieldSerializer.class, registration.getSerializer().getClass(), 
                "StandardMessage should fall back to the default FieldSerializer");
    }

    @Test
    public void testPredicateAutoRegistrationFallback() {
        // Clear registrations
        Serializer.initialize();
        Serializer.registerSerializerPredicate(cls -> cls == SpecialMessage.class, new MockCustomSerializer());
        
        // Fetch the registration using the auto-registration fallback mechanism (passing failOnMiss=false implicitly or explicitly)
        SerializerRegistration registration = Serializer.getSerializerRegistration(SpecialMessage.class, false);
        
        assertNotNull(registration, "Auto-registration should successfully create a registration");
        assertEquals(MockCustomSerializer.class, registration.getSerializer().getClass(), 
                "Auto-registration should respect the registered predicates");
    }
    
    @Test
    public void testInitializeClearsPredicateRegistrations() {
        // @BeforeEach already called initialize() and registered MockCustomSerializer for SpecialMessage.
        
        // Re-initialize the registry. This MUST clear the predicateSerializers list.
        Serializer.initialize();
        
        // Register the exact same predicate condition, but with a different serializer (SecondMockSerializer)
        Serializer.registerSerializerPredicate(
            cls -> cls == SpecialMessage.class, 
            new SecondMockSerializer()
        );
        
        // Register the class
        Serializer.registerClass(SpecialMessage.class);
        
        SerializerRegistration registration = Serializer.getExactSerializerRegistration(SpecialMessage.class);
        
        assertNotNull(registration, "Registration should not be null");
        
        // If initialize() failed to clear the list, MockCustomSerializer would still be first and win.
        // If it correctly cleared the list, SecondMockSerializer should be assigned.
        assertEquals(SecondMockSerializer.class, registration.getSerializer().getClass(), 
                "initialize() failed to clear predicateSerializers, causing a stale serializer to be selected.");
    }
    
    @Test
    public void testPredicateEvaluatesForAlternateDefaultSerializer() {
        Serializer.initialize();
        Serializer.registerSerializerPredicate(
            cls -> cls == SpecialMessageWithAlternateDefault.class, 
            new MockCustomSerializer()
        );
        
        Serializer.registerClass(SpecialMessageWithAlternateDefault.class);
        SerializerRegistration registration = Serializer.getExactSerializerRegistration(SpecialMessageWithAlternateDefault.class);
        
        assertNotNull(registration);
        
        // FAILS HERE in the flawed implementation: 
        // The registry sees `Serializer.class`, skips the predicate loop, 
        // and falls back to `FieldSerializer`, ignoring your mock!
        assertEquals(MockCustomSerializer.class, registration.getSerializer().getClass(), 
                "Predicate should intercept the alternate default (Serializer.class)");
    }

    @Test
    public void testExplicitCustomSerializerOverridesPredicate() {
        Serializer.initialize();
        // Register a predicate that TRIES to intercept this class
        Serializer.registerSerializerPredicate(
            cls -> cls == ExplicitCustomMessage.class, 
            new MockCustomSerializer()
        );
        
        // Because the user explicitly annotated this with `@Serializable(serializer = SecondMockSerializer.class)`,
        // the explicit request must win, and the predicate must be ignored.
        Serializer.registerClass(ExplicitCustomMessage.class);
        SerializerRegistration registration = Serializer.getExactSerializerRegistration(ExplicitCustomMessage.class);
        
        assertNotNull(registration);
        assertEquals(SecondMockSerializer.class, registration.getSerializer().getClass(), 
                "Explicit custom serializer must override predicate selection");
    }
}