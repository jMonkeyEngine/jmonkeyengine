/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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

/**
 * Enumerate known operating system/architecture pairs.
 */
public enum Platform {

    /**
     * Microsoft Windows 32-bit AMD/Intel
     */
    Windows32(Os.Windows),

    /**
     * Microsoft Windows 64-bit AMD/Intel
     */
    Windows64(Os.Windows, true),

    /**
     * Microsoft Windows 32-bit ARM
     */
    Windows_ARM32(Os.Windows),

    /**
     * Microsoft Windows 64-bit ARM
     */
    Windows_ARM64(Os.Windows, true),

    /**
     * Linux 32-bit Intel
     */
    Linux32(Os.Linux),

    /**
     * Linux 64-bit Intel
     */
    Linux64(Os.Linux, true),

    /**
     * Linux 32-bit ARM
     */
    Linux_ARM32(Os.Linux),

    /**
     * Linux 64-bit ARM
     */
    Linux_ARM64(Os.Linux, true),

    /**
     * Apple Mac OS X 32-bit Intel
     */
    MacOSX32(Os.MacOS),

    /**
     * Apple Mac OS X 64-bit Intel
     */
    MacOSX64(Os.MacOS, true),

    /**
     * Apple Mac OS X 64-bit ARM
     */
    MacOSX_ARM64(Os.MacOS, true),

    /**
     * Apple Mac OS X 32 bit PowerPC
     */
    MacOSX_PPC32(Os.MacOS),

    /**
     * Apple Mac OS X 64 bit PowerPC
     */
    MacOSX_PPC64(Os.MacOS, true),

    /**
     * Android ARM5
     */
    Android_ARM5(Os.Android),

    /**
     * Android ARM6
     */
    Android_ARM6(Os.Android),

    /**
     * Android ARM7
     */
    Android_ARM7(Os.Android),

    /**
     * Android ARM8
     */
    Android_ARM8(Os.Android),

    /**
     * Android x86
     */
    Android_X86(Os.Android),

    /**
     * iOS on x86
     */
    iOS_X86(Os.iOS),

    /**
     * iOS on ARM
     */
    iOS_ARM(Os.iOS),

    /**
     * Android running on unknown platform (could be x86 or mips for example).
     */
    Android_Other(Os.Android);

    
    /**
     * Enumerate generic names of operating systems
     */
    public enum Os {
        /**
         * Linux operating systems
         */
        Linux,
        /**
         * Microsoft Windows operating systems
         */
        Windows,
        /**
         * iOS operating systems
         */
        iOS,
        /**
         * macOS operating systems
         */
        MacOS,
        /**
         * Android operating systems
         */
        Android
    }

    private final boolean is64bit;
    private final Os os;

    /**
     * Test for a 64-bit address space.
     *
     * @return true if 64 bits, otherwise false
     */
    public boolean is64Bit() {
        return is64bit;
    }

    /**
     * Returns the operating system of this platform.
     *
     * @return the generic name of the operating system of this platform
     */
    public Os getOs() {
        return os;
    }

    private Platform(Os os, boolean is64bit) {
        this.os = os;
        this.is64bit = is64bit;
    }

    private Platform(Os os) {
        this(os, false);
    }
}
