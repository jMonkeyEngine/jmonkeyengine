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
package com.jme3.network.serializing;

import com.jme3.network.AbstractMessage;
import com.jme3.network.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class RecordSerializerTest {

    @Serializable
    public record SimpleRecord(int id, String name, float value) {}

    @Serializable
    public record NestedMessageRecord(SimpleRecord data, boolean isReliable) implements Message {
        
        // Convenience constructor
        public NestedMessageRecord(SimpleRecord data) {
            this(data, true);
        }

        // Immutable pattern: returns a new instance instead of mutating 'this'
        @Override
        public Message setReliable(boolean f) {
            return new NestedMessageRecord(this.data, f);
        }
    }
    
    @Serializable
    public static class LegacyPojoMessage extends AbstractMessage {
        private int id;
        private String text;

        // Required default constructor for FieldSerializer
        public LegacyPojoMessage() {}

        public LegacyPojoMessage(int id, String text) {
            this.id = id;
            this.text = text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LegacyPojoMessage that = (LegacyPojoMessage) o;
            return id == that.id && java.util.Objects.equals(text, that.text);
        }
    }

    @BeforeEach
    public void setupSerializerRegistry() {
        Serializer.initialize();
        Serializer.registerClass(SimpleRecord.class);
        Serializer.registerClass(NestedMessageRecord.class);
    }

    @Test
    public void testBasicRecordSerialization() throws Exception {
        SimpleRecord original = new SimpleRecord(42, "jMonkeyEngine", 3.14f);
        ByteBuffer buffer = ByteBuffer.allocate(256);

        Serializer.writeClassAndObject(buffer, original);
        buffer.flip();

        Object result = Serializer.readClassAndObject(buffer);

        assertNotNull(result);
        assertTrue(result instanceof SimpleRecord);
        
        // Records automatically generate equals() evaluating all fields
        assertEquals(original, result); 
    }

    @Test
    public void testNestedRecordSerialization() throws Exception {
        SimpleRecord inner = new SimpleRecord(99, "InnerData", 1.0f);
        NestedMessageRecord original = new NestedMessageRecord(inner, false);
        ByteBuffer buffer = ByteBuffer.allocate(256);

        Serializer.writeClassAndObject(buffer, original);
        buffer.flip();

        Object result = Serializer.readClassAndObject(buffer);

        assertNotNull(result);
        assertTrue(result instanceof NestedMessageRecord);
        assertEquals(original, result);
    }

    @Test
    public void testRecordImmutabilityAndReliabilityReassignment() {
        SimpleRecord inner = new SimpleRecord(1, "Test", 0f);
        NestedMessageRecord original = new NestedMessageRecord(inner, true);

        // Simulate the internal KernelAdapter / ConnectorAdapter reassignment
        Message modified = original.setReliable(false);

        // Verify the original object was untouched (strict immutability)
        assertTrue(original.isReliable());
        
        // Verify a new object was returned with the updated flag
        assertFalse(modified.isReliable());
        assertNotSame(original, modified);
        
        // Verify the nested data remained perfectly intact
        assertEquals(inner, ((NestedMessageRecord) modified).data());
    }

    @Test
    public void testRegistryAssignsCorrectSerializerType() {
        SerializerRegistration registration = Serializer.getExactSerializerRegistration(SimpleRecord.class);
        
        assertNotNull(registration);
        // Ensure that the fallback did not assign the legacy FieldSerializer
        assertEquals("RecordSerializer", registration.getSerializer().getClass().getSimpleName());
    }
    
    @Test
    public void testRecordSerializationPerformance() throws Exception {
        SimpleRecord original = new SimpleRecord(123, "PerformanceTestRecord", 999.99f);
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        // Warm up / populate cache
        Serializer.writeClassAndObject(buffer, original);
        buffer.clear();

        int iterations = 50_000;
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            buffer.clear();
            Serializer.writeClassAndObject(buffer, original);
            buffer.flip();
            Object deserialized = Serializer.readClassAndObject(buffer);
            assertEquals(original, deserialized);
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        
        // Output result to console for visibility during local test runs
        System.out.println("RecordSerializer Performance: " + iterations + " iterations took " + durationMs + " ms.");
        
        // Sanity check: 50,000 serializations with cached reflection should easily finish under 1 second
        assertTrue(durationMs < 1000, "Performance test took too long, caching might be failing: " + durationMs + " ms");
    }

    @Test
    public void testLegacyPojoSerializationRegression() throws Exception {
        // Register the legacy POJO class
        Serializer.registerClass(LegacyPojoMessage.class);

        // Verify it was correctly assigned the legacy FieldSerializer instead of RecordSerializer
        SerializerRegistration registration = Serializer.getExactSerializerRegistration(LegacyPojoMessage.class);
        assertNotNull(registration);
        assertEquals("FieldSerializer", registration.getSerializer().getClass().getSimpleName());

        // Verify read/write roundtrip works exactly as it always did
        LegacyPojoMessage original = new LegacyPojoMessage(777, "BackwardCompatibilityTest");
        ByteBuffer buffer = ByteBuffer.allocate(256);

        Serializer.writeClassAndObject(buffer, original);
        buffer.flip();

        Object result = Serializer.readClassAndObject(buffer);

        assertNotNull(result);
        assertTrue(result instanceof LegacyPojoMessage);
        assertEquals(original, result);
    }
}