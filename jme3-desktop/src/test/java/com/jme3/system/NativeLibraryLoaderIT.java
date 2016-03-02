/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.experimental.categories.Category;

import com.jme3.IntegrationTest;
import org.junit.Ignore;

/**
 * Integration test for {@link NativeLibraryLoader}.
 *
 * Note that it uses the file system.
 *
 * @author Kirill Vainer
 */
@Category(IntegrationTest.class)
@FixMethodOrder
public class NativeLibraryLoaderIT {

    private File extractFolder;

    static {
        NativeLibraryLoader.registerNativeLibrary("test", Platform.Linux64, "natives/linux64/libtest.so");
        NativeLibraryLoader.registerNativeLibrary("notexist", Platform.Linux64, "natives/linux64/libnotexist.so");
        NativeLibraryLoader.registerNativeLibrary("nativesfolder", Platform.Linux64, "natives/linux64/libnativesfolder.so");
        NativeLibraryLoader.registerNativeLibrary("jarroot", Platform.Linux64, "natives/linux64/libjarroot.so");
        NativeLibraryLoader.registerNativeLibrary("nullpath", Platform.Linux64, null);
        NativeLibraryLoader.registerNativeLibrary("jawt", Platform.Linux64, "whatever/doesnt/matter/libjawt.so");
        NativeLibraryLoader.registerNativeLibrary("asname", Platform.Linux64, "natives/linux64/libasname.so", "other.name");
    }

    @Before
    public void setUp() {
        extractFolder = NativeLibraryLoader.getExtractionFolder();
    }

    @Test(expected = UnsatisfiedLinkError.class)
    public void testRequiredNonExistentFile() {
        NativeLibraryLoader.loadNativeLibrary("notexist", true, false);
    }

    @Test
    public void testOptionalNonExistentFile() throws Exception {
        NativeLibraryLoader.loadNativeLibrary("notexist", false, false);
    }

    @Test(expected = UnsatisfiedLinkError.class)
    public void testRequiredUnregisteredLibrary() {
        NativeLibraryLoader.loadNativeLibrary("unregistered", true, false);
    }

    @Test
    public void testOptionalUnregisteredLibrary() {
        NativeLibraryLoader.loadNativeLibrary("unregistered", false, false);
    }

    @Test
    public void testLibraryNullPath() {
        NativeLibraryLoader.loadNativeLibrary("nullpath", true, false);
        NativeLibraryLoader.loadNativeLibrary("nullpath", false, false);
    }

    private static void fudgeLastModifiedTime(File file) {
        // fudge last modified date to force extraction attempt
        long yesterdayModifiedtime = file.lastModified() - 24 * 60 * 60 * 1000;
        assertTrue(file.setLastModified(yesterdayModifiedtime));
        assertTrue(Math.abs(file.lastModified() - yesterdayModifiedtime) < 10000);
    }

    @Test
    public void testDifferentLastModifiedDates() throws IOException {
        File libFile = new File(extractFolder, "libtest.so");

        assertTrue(libFile.createNewFile());
        assertTrue(libFile.exists() && libFile.length() == 0);

        fudgeLastModifiedTime(libFile);
        NativeLibraryLoader.loadNativeLibrary("test", true, false);
        assertTrue(libFile.length() == 12);

        assertTrue(libFile.delete());
        assertTrue(!libFile.exists());
    }

    @Test
    public void testLibraryInUse() throws IOException {
        File libFile = new File(extractFolder, "libtest.so");

        NativeLibraryLoader.loadNativeLibrary("test", true, false);
        assertTrue(libFile.exists());

        fudgeLastModifiedTime(libFile);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(libFile);
            FileLock lock = out.getChannel().lock();
            assertTrue(lock.isValid());

            NativeLibraryLoader.loadNativeLibrary("test", true, false);
        } finally {
            if (out != null) {
                out.close();
            }
        }

        libFile.delete();
    }

    @Test
    public void testLoadSystemLibrary() {
        NativeLibraryLoader.loadNativeLibrary("jawt", true, true);
    }

    @Test
    public void testExtractAsName() {
        NativeLibraryLoader.loadNativeLibrary("asname", true, false);
        assertTrue(new File(extractFolder, "other.name").exists());
        assertTrue(new File(extractFolder, "other.name").delete());
    }

    @Test
    public void testCustomExtractFolder() {
        File customExtractFolder = new File(System.getProperty("java.io.tmpdir"), "jme3_test_tmp");
        if (!customExtractFolder.exists()) {
            assertTrue(customExtractFolder.mkdir());
        }

        NativeLibraryLoader.setCustomExtractionFolder(customExtractFolder.getAbsolutePath());
        NativeLibraryLoader.loadNativeLibrary("test", true, false);

        assertTrue(new File(customExtractFolder, "libtest.so").exists());
        assertTrue(new File(customExtractFolder, "libtest.so").delete());
        assertTrue(!new File(customExtractFolder, "libtest.so").exists());

        NativeLibraryLoader.setCustomExtractionFolder(null);
        NativeLibraryLoader.loadNativeLibrary("test", true, false);

        assertTrue(new File(extractFolder, "libtest.so").exists());
        new File(extractFolder, "libtest.so").delete();
        customExtractFolder.delete();
    }

    @Test
    public void testExtractFromNativesFolderInJar() {
        NativeLibraryLoader.loadNativeLibrary("nativesfolder", true, false);

        File libFile = new File(extractFolder, "libnativesfolder.so");
        assertTrue(libFile.exists() && libFile.length() == 12);

        libFile.delete();
    }

    @Test
    public void testExtractFromJarRoot() {
        NativeLibraryLoader.loadNativeLibrary("jarroot", true, false);

        File libFile = new File(extractFolder, "libjarroot.so");
        assertTrue(libFile.exists() && libFile.length() == 12);

        libFile.delete();
    }
}
