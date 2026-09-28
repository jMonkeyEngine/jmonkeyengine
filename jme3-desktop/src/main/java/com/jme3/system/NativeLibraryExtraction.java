/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/** Creates isolated extraction directories without trusting predictable cache files. */
final class NativeLibraryExtraction {
    private NativeLibraryExtraction() {}

    static Path createDirectory(Path root, String prefix) throws IOException {
        Path existing = root.toAbsolutePath();
        while (!Files.exists(existing)) {
            existing = existing.getParent();
            if (existing == null) throw new IOException("No existing ancestor for " + root);
        }
        FileAttribute<?> permissions = privatePermissions(existing);
        Files.createDirectories(root, permissions);
        // Resolve symlinks such as macOS /var -> /private/var before using the path.
        root = root.toRealPath();
        if (Files.getFileStore(root).supportsFileAttributeView("posix")) {
            checkPosixParents(root);
        }
        Path directory = Files.createTempDirectory(root, prefix, permissions);
        boolean usable = false;
        try {
            directory.toFile().deleteOnExit();
            if (permissions.name().equals("acl:acl")) {
                // Remove any inherited grants before extracting native code. The initial
                // ACL also requests owner-only access at creation time.
                AclFileAttributeView view = Files.getFileAttributeView(directory, AclFileAttributeView.class);
                @SuppressWarnings("unchecked")
                List<AclEntry> acl = (List<AclEntry>) permissions.value();
                view.setAcl(acl);
                for (AclEntry entry : view.getAcl()) {
                    if (entry.type() == AclEntryType.ALLOW && !entry.principal().equals(acl.get(0).principal())) {
                        throw new IOException("Cannot restrict native directory ACL: " + directory);
                    }
                }
            }
            checkExecutable(directory);
            usable = true;
            return directory;
        } finally {
            if (!usable) {
                Files.deleteIfExists(directory);
            }
        }
    }

    private static void checkPosixParents(Path root) throws IOException {
        UserPrincipal user = root.getFileSystem().getUserPrincipalLookupService()
                .lookupPrincipalByName(System.getProperty("user.name"));
        List<Path> parents = new ArrayList<>();
        for (Path parent = root; parent != null; parent = parent.getParent()) parents.add(parent);
        Collections.reverse(parents);
        boolean privateAncestor = false;
        for (Path parent : parents) {
            PosixFileAttributes attributes = Files.readAttributes(parent, PosixFileAttributes.class);
            // A private child is not safe if another user can rename/replace its parent.
            boolean systemOwned = attributes.owner().getName().equals("root");
            if (!attributes.owner().equals(user) && !systemOwned) {
                throw new IOException("Native extraction ancestor belongs to another user: " + parent);
            }
            if (!privateAncestor && (attributes.permissions().contains(PosixFilePermission.GROUP_WRITE)
                    || attributes.permissions().contains(PosixFilePermission.OTHERS_WRITE))) {
                // Shared system temp directories are safe only with the sticky bit.
                int mode = ((Number) Files.getAttribute(parent, "unix:mode")).intValue();
                if ((mode & 01000) == 0) {
                    throw new IOException("Native extraction ancestor is writable by other users: " + parent);
                }
            }
            // Permissions below an owner-only directory cannot grant outsiders access.
            if (attributes.owner().equals(user)
                    && !attributes.permissions().contains(PosixFilePermission.GROUP_EXECUTE)
                    && !attributes.permissions().contains(PosixFilePermission.OTHERS_EXECUTE)) {
                privateAncestor = true;
            }
        }
    }

    private static FileAttribute<?> privatePermissions(Path root) throws IOException {
        if (Files.getFileStore(root).supportsFileAttributeView("posix")) {
            return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
        }
        if (Files.getFileStore(root).supportsFileAttributeView("acl")) {
            UserPrincipal owner = root.getFileSystem().getUserPrincipalLookupService()
                    .lookupPrincipalByName(System.getProperty("user.name"));
            final List<AclEntry> acl = Collections.singletonList(AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(owner)
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                    .setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)
                    .build());
            return new FileAttribute<List<AclEntry>>() {
                @Override
                public String name() { return "acl:acl"; }
                @Override
                public List<AclEntry> value() { return acl; }
            };
        }
        throw new IOException("Filesystem cannot create private native directories: " + root);
    }

    private static void checkExecutable(Path directory) throws IOException {
        if (!Files.getFileStore(directory).supportsFileAttributeView("posix")) {
            // Windows DLL loading is checked by the native loader, not Unix execute bits.
            return;
        }
        Path probe = Files.createTempFile(directory, "exec-probe-", null);
        try {
            Files.setPosixFilePermissions(probe, PosixFilePermissions.fromString("rwx------"));
            // On Linux, access(X_OK) on a regular file detects noexec. Checking the
            // directory itself only checks traversal. No subprocess is executed.
            if (!Files.isExecutable(probe)) {
                throw new IOException("Native execution is not permitted (possibly noexec): " + directory);
            }
        } finally {
            Files.deleteIfExists(probe);
        }
    }
}
