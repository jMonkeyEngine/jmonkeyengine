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
package com.jme3.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NativeLibraryExtractionTest {
    @TempDir Path root;

    @Test
    void createsUniquePrivateDirectories() throws Exception {
        Path first = NativeLibraryExtraction.createDirectory(root, "native-");
        Path second = NativeLibraryExtraction.createDirectory(root, "native-");
        assertNotEquals(first, second);
        if (Files.getFileStore(first).supportsFileAttributeView("posix")) {
            assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(first));
        } else {
            AclFileAttributeView view = Files.getFileAttributeView(first, AclFileAttributeView.class);
            assertNotNull(view);
            assertTrue(view.getAcl().stream().anyMatch(entry -> entry.type() == AclEntryType.ALLOW
                    && entry.principal().equals(viewOwner(first))));
            assertTrue(view.getAcl().stream().filter(entry -> entry.type() == AclEntryType.ALLOW)
                    .allMatch(entry -> entry.principal().equals(viewOwner(first))));
        }
    }

    @Test
    void refusesSharedDirectoryWithoutStickyBit() throws Exception {
        assumeTrue(Files.getFileStore(root).supportsFileAttributeView("posix"));
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwxr-xr-x"));
        Path shared = Files.createDirectory(root.resolve("shared"));
        Files.setPosixFilePermissions(shared, PosixFilePermissions.fromString("rwxrwxrwx"));
        assertThrows(IOException.class, () -> NativeLibraryExtraction.createDirectory(shared, "native-"));
    }

    @Test
    void refusesFilesystemsWithoutPrivatePermissions() throws Exception {
        URI zip = URI.create("jar:" + root.resolve("unsupported.zip").toUri());
        try (FileSystem fs = FileSystems.newFileSystem(zip, Collections.singletonMap("create", "true"))) {
            assertThrows(IOException.class, () -> NativeLibraryExtraction.createDirectory(fs.getPath("/"), "native-"));
        }
    }

    private static java.nio.file.attribute.UserPrincipal viewOwner(Path directory) {
        try {
            return directory.getFileSystem().getUserPrincipalLookupService()
                    .lookupPrincipalByName(System.getProperty("user.name"));
        } catch (IOException failure) {
            throw new AssertionError(failure);
        }
    }
}
