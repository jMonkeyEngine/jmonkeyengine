/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import com.jme3.network.Message;
import com.jme3.network.serializing.Serializable;

/**
 * Compress a message using this ZIPCompressedMessage class
 *
 * @author Lars Wesselius
 */
@Serializable()
public class ZIPCompressedMessage extends CompressedMessage {
    private static int defaultCompressionLevel = 6;
    private int compressionLevel = defaultCompressionLevel;

    /**
     * Creates an empty ZIP-compressed message for serialization.
     */
    public ZIPCompressedMessage() {
        super();
    }

    /**
     * Creates a ZIP-compressed wrapper for the specified message.
     *
     * @param msg the message to compress
     */
    public ZIPCompressedMessage(Message msg) {
        super(msg);
    }

    /**
     * Creates a ZIP-compressed wrapper for the specified message and compression level.
     *
     * @param msg the message to compress
     * @param level the compression level to apply
     */
    public ZIPCompressedMessage(Message msg, int level) {
        super(msg);
        this.compressionLevel = level;
    }

    /**
     * Set the default compression level for newly created ZIP compressed messages,
     * where 1 is the weakest compression but quickest and 9 is the best
     * compression but slowest. Default is 6.
     *
     * @param level The level.
     * @deprecated Use {@link #setCompressionLevel(int)} to configure an instance-specific
     * compression level.
     */
    @Deprecated
    public static void setLevel(int level) {
        defaultCompressionLevel = level;
    }

    /**
     * Sets this message's compression level.
     *
     * @param level The level.
     */
    public void setCompressionLevel(int level) {
        compressionLevel = level;
    }

    /**
     * Returns this message's configured ZIP compression level.
     *
     * @return the ZIP compression level
     */
    public int getLevel() { return compressionLevel; }
}
