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
package com.jme3.system;

public enum Platform {

    /**
     * Microsoft Windows 32 bit
     */
    Windows32,
    
    /**
     * Microsoft Windows 64 bit
     */
    Windows64(true),
    
    /**
     * Linux 32 bit
     */
    Linux32,
    
    /**
     * Linux 64 bit
     */
    Linux64(true),
    
    /**
     * Apple Mac OS X 32 bit
     */
    MacOSX32,
    
    /**
     * Apple Mac OS X 64 bit
     */
    MacOSX64(true),
    
    /**
     * Apple Mac OS X 32 bit PowerPC
     */
    MacOSX_PPC32,
    
    /**
     * Apple Mac OS X 64 bit PowerPC
     */
    MacOSX_PPC64(true),
    
    /**
     * Android ARM5
     */
    Android_ARM5,
    
    /**
     * Android ARM6
     */
    Android_ARM6,

    /**
     * Android ARM7
     */
    Android_ARM7,

    /**
     * Android ARM8
     */
    Android_ARM8,

    /**
     * Android x86
     */
    Android_X86,
    
    iOS_X86,
    
    iOS_ARM,
    
    /**
     * Android running on unknown platform (could be x86 or mips for example).
     */
    Android_Other;
    
    private final boolean is64bit;
    
    public boolean is64Bit() {
        return is64bit;
    }
    
    private Platform(boolean is64bit) {
        this.is64bit = is64bit;
    }
    
    private Platform() {
        this(false);
    }
}