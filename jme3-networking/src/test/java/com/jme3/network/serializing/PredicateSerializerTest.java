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
}