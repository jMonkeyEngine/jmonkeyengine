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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.jme3.util.res.Resources;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AutomaticNativeLibraryLoaderTest {
    private static final String RESOURCE = "com/jme3/system/AutomaticNativeLibraryLoaderTest.class";
    @TempDir Path directory;
    private final Map<String, String> properties = new HashMap<>();
    private final Map<String, Object> fields = new HashMap<>();
    private Path tmp;
    private Path home;

    @BeforeEach
    void isolateLoaderState() throws Exception {
        for (String key : new String[]{"java.io.tmpdir", "user.home",
                NativeLibraryLoader.CUSTOM_EXTRACTION_FOLDER_PROPERTY,
                NativeLibraryLoader.EXTRACT_NATIVE_LIBRARIES_PROPERTY,
                NativeLibraryLoader.CACHE_FOLDER_PROPERTY}) {
            properties.put(key, System.getProperty(key));
            System.clearProperty(key);
        }
        for (String key : new String[]{"extractionFolder", "extractionFolderOverride",
                "extractNativeLibrariesOverride", "extractionRootIndex"}) {
            Field field = field(key);
            fields.put(key, field.get(null));
            field.set(null, key.equals("extractionRootIndex") ? 0 : null);
        }
        tmp = Files.createDirectory(directory.resolve("tmp"));
        home = directory.resolve("home");
        System.setProperty("java.io.tmpdir", tmp.toString());
        System.setProperty("user.home", home.toString());
        System.setProperty(NativeLibraryLoader.CACHE_FOLDER_PROPERTY, home.resolve(".cache").toString());
    }

    @AfterEach
    void restoreLoaderState() throws Exception {
        for (Map.Entry<String, Object> entry : fields.entrySet()) field(entry.getKey()).set(null, entry.getValue());
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getValue() == null) System.clearProperty(entry.getKey());
            else System.setProperty(entry.getKey(), entry.getValue());
        }
    }

    @Test
    void tempIsPrivateAndDoesNotCreateHome() throws Exception {
        Path selected = NativeLibraryLoader.getExtractionFolder().toPath();
        assertEquals(tmp.toRealPath(), selected.getParent());
        assertFalse(Files.exists(home));
        if (Files.getFileStore(selected).supportsFileAttributeView("posix")) {
            assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(selected));
        } else if (Files.getFileStore(selected).supportsFileAttributeView("acl")) {
            assertTrue(Files.isWritable(selected));
        }
    }

    @Test
    void invalidHomeDoesNotBreakTemp() {
        System.setProperty("user.home", "invalid" + (char) 0 + "home");
        assertTrue(NativeLibraryLoader.getExtractionFolder().isDirectory());
    }

    @Test
    void invalidTempFallsBackToCache() throws Exception {
        Files.createDirectories(home.resolve(".cache"));
        System.setProperty("java.io.tmpdir", "invalid" + (char) 0 + "temp");
        Path selected = NativeLibraryLoader.getExtractionFolder().toPath();
        assertEquals(cacheRoot().toRealPath(), selected.getParent());
    }

    @Test
    void tempFileFallsBackToCacheDuringExtraction() throws Exception {
        Files.createDirectories(home.resolve(".cache"));
        Path blockedTemp = Files.createFile(directory.resolve("temp-file"));
        System.setProperty("java.io.tmpdir", blockedTemp.toString());

        String loaded = NativeLibraryLoader.loadNativeLibrary(register("fixture.so", path -> {}), true);
        assertTrue(Paths.get(loaded).startsWith(cacheRoot().toRealPath()));
        assertTrue(Files.isRegularFile(Paths.get(loaded)));
    }

    @Test
    void unusableTempAndCacheFallBackToHome() throws Exception {
        System.setProperty("java.io.tmpdir", Files.createFile(directory.resolve("tmp-file")).toString());
        Path cache = cacheRoot();
        Files.createDirectories(cache.getParent());
        Files.createFile(cache);
        String loaded = NativeLibraryLoader.loadNativeLibrary(register("fixture.so", path -> {}), true);
        assertTrue(Paths.get(loaded).startsWith(home.resolve(".jme3").toRealPath()));
        assertTrue(Files.isRegularFile(Paths.get(loaded)));
    }

    @Test
    void ignoresLegacyPredictableTempPath() throws Exception {
        Path legacy = Files.createDirectory(tmp.resolve("jme3"));
        Path poison = Files.write(legacy.resolve("poison"), new byte[]{1, 2, 3});
        Path selected = NativeLibraryLoader.getExtractionFolder().toPath();
        assertEquals(tmp.toRealPath(), selected.getParent());
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(poison));
    }

    @Test
    void rejectsExistingLibraryInsteadOfTrustingTimestamp() throws Exception {
        Path selected = NativeLibraryLoader.getExtractionFolder().toPath();
        Path poison = Files.write(selected.resolve("fixture.so"), new byte[]{1, 2, 3});
        String name = register("fixture.so", path -> {});
        String loaded = NativeLibraryLoader.loadNativeLibrary(name, true);
        assertFalse(Paths.get(loaded).startsWith(selected));
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(poison));
    }

    @Test
    void rejectsSymlinkWithoutChangingTarget() throws Exception {
        assumeTrue(Files.getFileStore(tmp).supportsFileAttributeView("posix"));
        Path victim = Files.write(directory.resolve("victim"), new byte[]{7, 8, 9});
        Path selected = NativeLibraryLoader.getExtractionFolder().toPath();
        Files.createSymbolicLink(selected.resolve("fixture.so"), victim);
        String loaded = NativeLibraryLoader.loadNativeLibrary(register("fixture.so", path -> {}), true);
        assertFalse(Paths.get(loaded).startsWith(selected));
        assertArrayEquals(new byte[]{7, 8, 9}, Files.readAllBytes(victim));
    }

    @Test
    void failedLoadFallsBackAndRemovesFailedFile() throws Exception {
        Files.createDirectories(home.resolve(".cache"));
        AtomicInteger calls = new AtomicInteger();
        List<Path> attempted = new ArrayList<>();
        String name = register("fixture.so", path -> {
            attempted.add(Paths.get(path));
            if (calls.getAndIncrement() == 0) throw new UnsatisfiedLinkError("simulated mapping failure");
        });
        String loaded = NativeLibraryLoader.loadNativeLibrary(name, true);
        assertEquals(2, calls.get());
        assertFalse(Files.exists(attempted.get(0)));
        assertTrue(Paths.get(loaded).startsWith(cacheRoot().toRealPath()));
        assertEquals(loaded, NativeLibraryLoader.loadNativeLibrary(name, true));
        assertEquals(2, calls.get(), "Successful loads must not be rewritten or reloaded");
    }

    @Test
    void errorsIncludeAllLoadAttemptsAndOptionalFailureReturnsNull() throws Exception {
        Files.createDirectories(home.resolve(".cache"));
        String name = register("fixture.so", path -> { throw new UnsatisfiedLinkError("mapping denied"); });
        UnsatisfiedLinkError error = assertThrows(UnsatisfiedLinkError.class,
                () -> NativeLibraryLoader.loadNativeLibrary(name, true));
        assertEquals(3, error.getSuppressed().length);
        assertNull(NativeLibraryLoader.loadNativeLibrary(name, false));
    }

    @Test
    void allUnusableRootsFailWithoutRetryingForever() throws Exception {
        Path notDirectory = Files.createFile(directory.resolve("not-a-directory"));
        Path cache = Files.createDirectory(directory.resolve("cache"));
        Files.createFile(cache.resolve(".jme3"));
        NativeLibraryLoader.setCustomExtractionFolder(notDirectory.toString());
        System.setProperty("java.io.tmpdir", notDirectory.toString());
        System.setProperty(NativeLibraryLoader.CACHE_FOLDER_PROPERTY, cache.toString());
        System.setProperty("user.home", directory.resolve("missing/home").toString());

        String name = register("fixture.so", path -> fail("Must not load"));
        UnsatisfiedLinkError error = assertThrows(UnsatisfiedLinkError.class,
                () -> NativeLibraryLoader.loadNativeLibrary(name, true));
        assertEquals(1, error.getSuppressed().length);
        assertEquals(4, error.getSuppressed()[0].getCause().getSuppressed().length);
        assertNull(NativeLibraryLoader.loadNativeLibrary(name, false));
    }

    @Test
    void fatalVmErrorIsNotSwallowed() {
        OutOfMemoryError fatal = new OutOfMemoryError("test sentinel");
        String name = register("fixture.so", path -> { throw fatal; });
        assertSame(fatal, assertThrows(OutOfMemoryError.class, () -> NativeLibraryLoader.loadNativeLibrary(name, true)));
    }

    @Test
    void concurrentCallsShareOneDirectoryAndLoadOnce() throws Exception {
        AtomicInteger loads = new AtomicInteger();
        String name = register("fixture.so", path -> loads.incrementAndGet());
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                futures.add(executor.submit(() -> NativeLibraryLoader.loadNativeLibrary(name, true)));
            }
            String first = futures.get(0).get(10, TimeUnit.SECONDS);
            for (Future<String> future : futures) assertEquals(first, future.get(10, TimeUnit.SECONDS));
            assertEquals(1, loads.get());
            try (Stream<Path> children = Files.list(tmp)) {
                assertEquals(1, children.count());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void extractionNameCannotEscapePrivateDirectory() {
        for (String filename : new String[]{"../outside.so", "..\\outside.dll", "C:outside.dll", "library.dll:stream"}) {
            String name = register(filename, path -> fail("Must not load"));
            assertThrows(IllegalArgumentException.class, () -> NativeLibraryLoader.loadNativeLibrary(name, true));
        }
    }

    @Test
    void customLoadFailureFallsBackToTemp() throws Exception {
        Path custom = Files.createDirectory(directory.resolve("custom"));
        NativeLibraryLoader.setCustomExtractionFolder(custom.toString());
        AtomicInteger attempts = new AtomicInteger();
        List<Path> attempted = new ArrayList<>();
        String name = register("fixture.so", path -> {
            attempted.add(Paths.get(path));
            if (attempts.getAndIncrement() == 0) throw new UnsatisfiedLinkError("custom failure");
        });
        String loaded = NativeLibraryLoader.loadNativeLibrary(name, true);
        assertEquals(2, attempts.get());
        assertTrue(Paths.get(loaded).startsWith(tmp.toRealPath()));
        assertTrue(attempted.get(0).startsWith(custom.toRealPath()));
        assertFalse(Files.exists(attempted.get(0)));
    }

    @Test
    void unusableCustomDirectoryFallsBackToTemp() throws Exception {
        Path custom = Files.createFile(directory.resolve("not-a-directory"));
        NativeLibraryLoader.setCustomExtractionFolder(custom.toString());
        String loaded = NativeLibraryLoader.loadNativeLibrary(register("fixture.so", path -> {}), true);
        assertTrue(Paths.get(loaded).startsWith(tmp.toRealPath()));
    }

    @Test
    void customDirectoryIsCreatedWhenExtracting() throws Exception {
        Path custom = directory.resolve("new-custom");
        NativeLibraryLoader.setCustomExtractionFolder(custom.toString());
        Path selected = NativeLibraryLoader.getExtractionFolder().toPath();
        assertEquals(custom.toRealPath(), selected.getParent());
        assertTrue(Files.isDirectory(selected));

        String loaded = NativeLibraryLoader.loadNativeLibrary(register("fixture.so", path -> {}), true);
        assertEquals(selected.resolve("fixture.so").toString(), loaded);
        assertTrue(Files.isDirectory(custom));
        try (InputStream source = Resources.getResource(RESOURCE).openStream()) {
            ByteArrayOutputStream expected = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int read; (read = source.read(buffer)) != -1;) {
                expected.write(buffer, 0, read);
            }
            assertArrayEquals(expected.toByteArray(), Files.readAllBytes(Paths.get(loaded)));
        }
    }

    @Test
    void settingCustomDirectoryAfterAutomaticSelectionTakesEffect() throws Exception {
        AtomicInteger loads = new AtomicInteger();
        String name = register("fixture.so", path -> loads.incrementAndGet());
        String alreadyLoaded = NativeLibraryLoader.loadNativeLibrary(name, true);
        Path custom = directory.resolve("late-custom");
        NativeLibraryLoader.setCustomExtractionFolder(custom.toString());
        Path selected = NativeLibraryLoader.getExtractionFolder().toPath();
        assertEquals(custom.toRealPath(), selected.getParent());
        assertEquals(alreadyLoaded, NativeLibraryLoader.loadNativeLibrary(name, true));
        assertEquals(1, loads.get());
        assertEquals(selected.resolve("other.so").toString(),
                NativeLibraryLoader.loadNativeLibrary(register("other.so", path -> {}), true));
    }

    @Test
    void customDirectoryDoesNotTrustExistingFile() throws Exception {
        Path custom = Files.createDirectory(directory.resolve("existing-custom"));
        NativeLibraryLoader.setCustomExtractionFolder(custom.toString());
        Path selected = NativeLibraryLoader.getExtractionFolder().toPath();
        Path target = Files.write(selected.resolve("fixture.so"), new byte[]{1, 2, 3});

        String loaded = NativeLibraryLoader.loadNativeLibrary(register("fixture.so", path -> {}), true);
        assertTrue(Paths.get(loaded).startsWith(tmp.toRealPath()));
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(target));
    }

    @Test
    void noexecTempIsSkippedEvenForDelegatedLoaders() throws Exception {
        Files.createDirectories(home.resolve(".cache"));
        Path noexec = createNoexecDirectory();
        try {
            System.setProperty("java.io.tmpdir", noexec.toString());
            String loaded = NativeLibraryLoader.loadNativeLibrary(register("fixture.so", path -> {}), true);
            assertTrue(Paths.get(loaded).startsWith(cacheRoot().toRealPath()));
            try (Stream<Path> children = Files.list(noexec)) {
                assertEquals(0, children.count());
            }
        } finally {
            Files.delete(noexec);
        }
    }

    private String register(String filename, Consumer<String> loader) {
        String name = "test-" + System.nanoTime();
        NativeLibraryLoader.registerNativeLibrary(new NativeLibrary(name, JmeSystem.getPlatform(), RESOURCE, filename, loader));
        return name;
    }

    private Path cacheRoot() {
        return home.resolve(".cache/.jme3");
    }

    private static Path createNoexecDirectory() throws Exception {
        assumeTrue(JmeSystem.getPlatform().getOs() == Platform.Os.Linux);
        for (String name : new String[]{"/dev/shm", "/run/lock"}) {
            Path root = Paths.get(name);
            if (!Files.isDirectory(root) || !Files.isWritable(root)) continue;
            Path probe = Files.createTempFile(root, "jme-noexec-", null);
            try {
                Files.setPosixFilePermissions(probe, PosixFilePermissions.fromString("rwx------"));
                if (!Files.isExecutable(probe)) return Files.createTempDirectory(root, "jme-test-");
            } finally {
                Files.delete(probe);
            }
        }
        assumeTrue(false, "No writable noexec filesystem available");
        return null;
    }

    private static Field field(String name) throws Exception {
        Field result = NativeLibraryLoader.class.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

}
