/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.export.binary;

class BinaryClassField {

    public static final byte BYTE = 0;
    public static final byte BYTE_1D = 1;
    public static final byte BYTE_2D = 2;

    public static final byte INT = 10;
    public static final byte INT_1D = 11;
    public static final byte INT_2D = 12;

    public static final byte FLOAT = 20;
    public static final byte FLOAT_1D = 21;
    public static final byte FLOAT_2D = 22;

    public static final byte DOUBLE = 30;
    public static final byte DOUBLE_1D = 31;
    public static final byte DOUBLE_2D = 32;

    public static final byte LONG = 40;
    public static final byte LONG_1D = 41;
    public static final byte LONG_2D = 42;

    public static final byte SHORT = 50;
    public static final byte SHORT_1D = 51;
    public static final byte SHORT_2D = 52;

    public static final byte BOOLEAN = 60;
    public static final byte BOOLEAN_1D = 61;
    public static final byte BOOLEAN_2D = 62;

    public static final byte STRING = 70;
    public static final byte STRING_1D = 71;
    public static final byte STRING_2D = 72;

    public static final byte BITSET = 80;

    public static final byte SAVABLE = 90;
    public static final byte SAVABLE_1D = 91;
    public static final byte SAVABLE_2D = 92;

    public static final byte SAVABLE_ARRAYLIST = 100;
    public static final byte SAVABLE_ARRAYLIST_1D = 101;
    public static final byte SAVABLE_ARRAYLIST_2D = 102;
    
    public static final byte SAVABLE_MAP = 105;
    public static final byte STRING_SAVABLE_MAP = 106;
    public static final byte INT_SAVABLE_MAP = 107;
    
    public static final byte FLOATBUFFER_ARRAYLIST = 110;
    public static final byte BYTEBUFFER_ARRAYLIST = 111;

    public static final byte FLOATBUFFER = 120;
    public static final byte INTBUFFER = 121;
    public static final byte BYTEBUFFER = 122;
    public static final byte SHORTBUFFER = 123;

    
    byte type;
    String name;
    byte alias;

    BinaryClassField(String name, byte alias, byte type) {
        this.name = name;
        this.alias = alias;
        this.type = type;
    }
}
