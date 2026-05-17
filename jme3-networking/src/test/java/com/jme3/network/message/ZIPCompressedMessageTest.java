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
package com.jme3.network.message;

import com.jme3.network.AbstractMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ZIPCompressedMessageTest {

    @AfterEach
    public void resetCompressionDefault() {
        ZIPCompressedMessage.setLevel(6);
    }

    @Test
    public void testMessageSpecificCompressionLevelDoesNotLeakToOtherMessages() {
        ZIPCompressedMessage.setLevel(6);

        ZIPCompressedMessage first = new ZIPCompressedMessage(new TestMessage());
        ZIPCompressedMessage second = new ZIPCompressedMessage(new TestMessage(), 1);
        ZIPCompressedMessage third = new ZIPCompressedMessage(new TestMessage());

        assertEquals(6, first.getLevel());
        assertEquals(1, second.getLevel());
        assertEquals(6, third.getLevel());
    }

    @Test
    public void testStaticLevelRemainsDefaultForNewMessages() {
        ZIPCompressedMessage.setLevel(9);

        ZIPCompressedMessage first = new ZIPCompressedMessage(new TestMessage());

        ZIPCompressedMessage.setLevel(4);
        ZIPCompressedMessage second = new ZIPCompressedMessage(new TestMessage());

        assertEquals(9, first.getLevel());
        assertEquals(4, second.getLevel());
    }

    private static class TestMessage extends AbstractMessage {
    }
}
