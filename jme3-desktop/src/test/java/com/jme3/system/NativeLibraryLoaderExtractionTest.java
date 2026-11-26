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

import com.jme3.nativebootstrap.common.OperatingSystem;
import com.jme3.nativebootstrap.directories.DirectoryCandidate;
import com.jme3.nativebootstrap.directories.NativeDirectories;
import com.jme3.util.res.Resources;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NativeLibraryLoaderExtractionTest {
    private static final String RESOURCE = "com/jme3/system/NativeLibraryLoaderExtractionTest.class";
    @TempDir Path directory;
    private final Map<String, String> properties = new HashMap<>();
    private final Map<String, Object> fields = new HashMap<>();
    private Path tmp;
    private Path home;
    private Path cache;

    @BeforeEach
    void isolateLoaderState() throws Exception {
        for (String key : new String[]{"java.io.tmpdir", "user.home",
                "natives.tempDir", "natives.userHome", "natives.cacheDir", "natives.namespace",
                NativeLibraryLoader.CUSTOM_EXTRACTION_FOLDER_PROPERTY,
                NativeLibraryLoader.EXTRACT_NATIVE_LIBRARIES_PROPERTY,
                NativeLibraryLoader.CACHE_FOLDER_PROPERTY}) {
            properties.put(key, System.getProperty(key));
            System.clearProperty(key);
        }
        for (String key : new String[]{"extractionFolder", "extractionFolderOverride",
                "extractNativeLibrariesOverride", "extractionCandidates", "extractionRootIndex"}) {
            Field field = field(key);
            fields.put(key, field.get(null));
            field.set(null, key.equals("extractionRootIndex") ? 0 : null);
        }
        tmp = Files.createDirectory(directory.resolve("tmp"));
        home = directory.resolve("home");
        cache = Files.createDirectory(directory.resolve("cache"));
        System.setProperty("java.io.tmpdir", tmp.toString());
        System.setProperty("user.home", home.toString());
        System.setProperty(NativeLibraryLoader.CACHE_FOLDER_PROPERTY, cache.toString());
    }

    @AfterEach
    void restoreLoaderState() throws Exception {
        System.clearProperty(NativeLibraryLoader.CUSTOM_EXTRACTION_FOLDER_PROPERTY);
        System.clearProperty(NativeLibraryLoader.CACHE_FOLDER_PROPERTY);
        NativeLibraryLoader.setCustomExtractionFolder(null);
        for (Map.Entry<String, Object> entry : fields.entrySet()) field(entry.getKey()).set(null, entry.getValue());
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getValue() == null) System.clearProperty(entry.getKey());
            else System.setProperty(entry.getKey(), entry.getValue());
        }
    }

    @Test
    void defaultNamespaceAlsoAppliesToOtherBootstrapLibraries() {
        assertNull(System.getProperty("natives.namespace"));
        List<DirectoryCandidate> candidates = NativeDirectories.candidates("other-library", () -> OperatingSystem.LINUX);
        assertEquals(tmp.resolve(".jme3/other-library"), candidates.get(0).root());
    }

    @Test
    void customFolderDoesNotOverwriteBootstrapTempOverride() throws Exception {
        Path nativeTemp = Files.createDirectory(directory.resolve("bootstrap-temp"));
        System.setProperty("natives.tempDir", nativeTemp.toString());
        Path custom = Files.createDirectory(directory.resolve("custom"));
        NativeLibraryLoader.setCustomExtractionFolder(custom.toString());
        assertEquals(nativeTemp.toString(), System.getProperty("natives.tempDir"));
        assertEquals(extractionRoot(custom.toRealPath()), NativeLibraryLoader.getExtractionFolder().toPath().getParent());
        NativeLibraryLoader.setCustomExtractionFolder(null);
        assertEquals(nativeTemp.toString(), System.getProperty("natives.tempDir"));
        assertEquals(extractionRoot(nativeTemp.toRealPath()),
                NativeLibraryLoader.getExtractionFolder().toPath().getParent());
    }

    @Test
    void cachePropertyOnlyChangesWithJmeOverride() throws Exception {
        Path nativeCache = Files.createDirectory(directory.resolve("bootstrap-cache"));
        Path jmeCache = Files.createDirectory(directory.resolve("jme-cache"));
        System.setProperty("natives.cacheDir", nativeCache.toString());
        System.clearProperty(NativeLibraryLoader.CACHE_FOLDER_PROPERTY);
        NativeLibraryLoader.setCustomExtractionFolder(null);
        assertEquals(nativeCache.toString(), System.getProperty("natives.cacheDir"));
        System.setProperty(NativeLibraryLoader.CACHE_FOLDER_PROPERTY, jmeCache.toString());
        NativeLibraryLoader.getExtractionFolder();
        assertEquals(jmeCache.toString(), System.getProperty("natives.cacheDir"));
        System.clearProperty(NativeLibraryLoader.CACHE_FOLDER_PROPERTY);
        NativeLibraryLoader.setCustomExtractionFolder(null);
        assertEquals(jmeCache.toString(), System.getProperty("natives.cacheDir"));
    }

    @Test
    void tempIsPrivateAndDoesNotCreateHome() throws Exception {
        Path selected = NativeLibraryLoader.getExtractionFolder().toPath();
        assertEquals(extractionRoot(tmp).toRealPath(), selected.getParent());
        assertFalse(Files.exists(home));
        if (Files.getFileStore(selected).supportsFileAttributeView("posix")) {
            assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(selected));
        } else if (Files.getFileStore(selected).supportsFileAttributeView("acl")) {
            assertTrue(Files.isWritable(selected));
        }
    }

    @Test
    void relativeHomeDoesNotBreakTemp() {
        System.setProperty("user.home", "relative-home");
        assertTrue(NativeLibraryLoader.getExtractionFolder().isDirectory());
    }

    @Test
    void relativeTempFallsBackToCache() throws Exception {
        System.setProperty("java.io.tmpdir", "relative-temp");
        Path selected = NativeLibraryLoader.getExtractionFolder().toPath();
        assertEquals(cacheRoot().toRealPath(), selected.getParent());
    }

    @Test
    void malformedDirectorySettingsReportRequiredAndOptionalLoadFailures() {
        for (String key : new String[]{"user.home", "java.io.tmpdir"}) {
            String previous = System.getProperty(key);
            System.setProperty(key, "invalid" + (char) 0 + "path");
            String name = register("fixture.so", path -> fail("Must not load"));
            UnsatisfiedLinkError failure = assertThrows(UnsatisfiedLinkError.class,
                    () -> NativeLibraryLoader.loadNativeLibrary(name, true));
            assertTrue(failure.getSuppressed()[0].getCause().getSuppressed()[0]
                    instanceof java.nio.file.InvalidPathException);
            assertNull(NativeLibraryLoader.loadNativeLibrary(name, false));
            System.setProperty(key, previous);
        }
    }

    @Test
    void configuredCacheReplacesPlatformCacheAfterTemp() throws Exception {
        Path cache = Files.createDirectory(directory.resolve("configured-cache"));
        System.setProperty(NativeLibraryLoader.CACHE_FOLDER_PROPERTY, cache.toString());
        AtomicInteger attempts = new AtomicInteger();
        String loaded = NativeLibraryLoader.loadNativeLibrary(register("fixture.so", path -> {
            if (attempts.getAndIncrement() == 0) throw new UnsatisfiedLinkError("temp failure");
        }), true);
        assertEquals(2, attempts.get());
        assertEquals(cache.toString(), System.getProperty("natives.cacheDir"));
        assertEquals(extractionRoot(cache).toRealPath(), Paths.get(loaded).getParent().getParent());
        assertFalse(Files.exists(home));
    }

    @Test
    void tempFileFallsBackToCacheDuringExtraction() throws Exception {
        Path blockedTemp = Files.createFile(directory.resolve("temp-file"));
        System.setProperty("java.io.tmpdir", blockedTemp.toString());

        String loaded = NativeLibraryLoader.loadNativeLibrary(register("fixture.so", path -> {}), true);
        assertTrue(Paths.get(loaded).startsWith(cacheRoot().toRealPath()));
        assertTrue(Files.isRegularFile(Paths.get(loaded)));
    }

    @Test
    void unusableTempAndCacheFallBackToHome() throws Exception {
        System.setProperty("java.io.tmpdir", Files.createFile(directory.resolve("tmp-file")).toString());
        Files.createDirectories(cacheRoot().getParent());
        Files.createFile(cacheRoot());
        Files.createDirectory(home);
        String loaded = NativeLibraryLoader.loadNativeLibrary(register("fixture.so", path -> {}), true);
        assertTrue(Paths.get(loaded).startsWith(extractionRoot(home).toRealPath()));
        assertTrue(Files.isRegularFile(Paths.get(loaded)));
    }

    @Test
    void ignoresLegacyPredictableTempPath() throws Exception {
        Path legacy = Files.createDirectory(tmp.resolve("jme3"));
        Path poison = Files.write(legacy.resolve("poison"), new byte[]{1, 2, 3});
        Path selected = NativeLibraryLoader.getExtractionFolder().toPath();
        assertEquals(extractionRoot(tmp).toRealPath(), selected.getParent());
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
        setExtractionRoots(tmp, cache, home);
        String name = register("fixture.so", path -> { throw new UnsatisfiedLinkError("mapping denied"); });
        UnsatisfiedLinkError error = assertThrows(UnsatisfiedLinkError.class,
                () -> NativeLibraryLoader.loadNativeLibrary(name, true));
        assertEquals(3, error.getSuppressed().length);
        setExtractionRoots(tmp, cache, home);
        assertNull(NativeLibraryLoader.loadNativeLibrary(name, false));
    }

    @Test
    void allUnusableRootsFailWithoutRetryingForever() throws Exception {
        Path notDirectory = Files.createFile(directory.resolve("not-a-directory"));
        Files.createDirectories(cacheRoot().getParent());
        Files.createFile(cacheRoot());
        Files.createFile(home);
        setExtractionRoots(notDirectory, cache, home);

        String name = register("fixture.so", path -> fail("Must not load"));
        UnsatisfiedLinkError error = assertThrows(UnsatisfiedLinkError.class,
                () -> NativeLibraryLoader.loadNativeLibrary(name, true));
        assertEquals(1, error.getSuppressed().length);
        assertEquals(3, error.getSuppressed()[0].getCause().getSuppressed().length);
        setExtractionRoots(notDirectory, cache, home);
        assertNull(NativeLibraryLoader.loadNativeLibrary(name, false));
    }

    @Test
    void failedLoadRediscoversUpdatedCacheOverride() throws Exception {
        System.setProperty("java.io.tmpdir", Files.createFile(directory.resolve("tmp-file")).toString());
        System.setProperty("user.home", Files.createFile(directory.resolve("home-file")).toString());
        setExtractionRoots(cache);
        AtomicBoolean denyLoad = new AtomicBoolean(true);
        String name = register("fixture.so", path -> {
            if (denyLoad.get()) throw new UnsatisfiedLinkError("mapping denied");
        });
        assertThrows(UnsatisfiedLinkError.class, () -> NativeLibraryLoader.loadNativeLibrary(name, true));

        Path newCache = Files.createDirectory(directory.resolve("new-cache"));
        System.setProperty(NativeLibraryLoader.CACHE_FOLDER_PROPERTY, newCache.toString());
        denyLoad.set(false);
        String loaded = NativeLibraryLoader.loadNativeLibrary(name, true);
        assertEquals(extractionRoot(newCache).toRealPath(), Paths.get(loaded).getParent().getParent());
    }

    @Test
    void invalidLegacyCacheOverridePreservesBootstrapCache() throws Exception {
        System.setProperty("java.io.tmpdir", Files.createFile(directory.resolve("tmp-file")).toString());
        System.setProperty("natives.cacheDir", cache.toString());
        Path missingCache = directory.resolve("missing-cache");
        Path cacheFile = Files.createFile(directory.resolve("cache-file"));
        for (String override : new String[]{"relative-cache", missingCache.toString(), cacheFile.toString()}) {
            System.setProperty(NativeLibraryLoader.CACHE_FOLDER_PROPERTY, override);
            NativeLibraryLoader.setCustomExtractionFolder(null);
            String loaded = NativeLibraryLoader.loadNativeLibrary(register("fixture.so", path -> {}), true);
            assertTrue(Files.isRegularFile(Paths.get(loaded)));
            assertTrue(Paths.get(loaded).startsWith(cacheRoot().toRealPath()));
        }
        assertFalse(Files.exists(missingCache));
    }

    @Test
    void explicitBootstrapNamespaceIsPreserved() throws Exception {
        System.setProperty("natives.namespace", ".custom-app");
        Path selected = NativeLibraryLoader.getExtractionFolder().toPath();
        assertEquals(tmp.resolve(".custom-app/jme3").toRealPath(), selected.getParent());
        assertEquals(".custom-app", System.getProperty("natives.namespace"));
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
            try (Stream<Path> children = Files.list(extractionRoot(tmp))) {
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
    void customDirectoryMatchingTempIsTriedOnlyOnce() throws Exception {
        NativeLibraryLoader.setCustomExtractionFolder(tmp.toString());
        List<Path> attempted = new ArrayList<>();
        String name = register("fixture.so", path -> {
            attempted.add(Paths.get(path));
            if (attempted.size() == 1) throw new UnsatisfiedLinkError("custom failure");
        });

        String loaded = NativeLibraryLoader.loadNativeLibrary(name, true);
        assertEquals(2, attempted.size());
        assertEquals(extractionRoot(tmp).toRealPath(), attempted.get(0).getParent().getParent());
        assertEquals(cacheRoot().toRealPath(), Paths.get(loaded).getParent().getParent());
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
        assertEquals(extractionRoot(custom).toRealPath(), selected.getParent());
        assertNull(System.getProperty("natives.tempDir"));
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
        assertEquals(extractionRoot(custom).toRealPath(), selected.getParent());
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
        Path noexec = createNoexecDirectory();
        try {
            System.setProperty("java.io.tmpdir", noexec.toString());
            String loaded = NativeLibraryLoader.loadNativeLibrary(register("fixture.so", path -> {}), true);
            assertTrue(Paths.get(loaded).startsWith(cacheRoot().toRealPath()));
        } finally {
            // A rejected extraction may still have created the stable namespace directories.
            Files.deleteIfExists(extractionRoot(noexec));
            Files.deleteIfExists(noexec.resolve(".jme3"));
            Files.delete(noexec);
        }
    }

    private String register(String filename, Consumer<String> loader) {
        String name = "test-" + System.nanoTime();
        NativeLibraryLoader.registerNativeLibrary(new NativeLibrary(name, JmeSystem.getPlatform(), RESOURCE, filename, loader));
        return name;
    }

    private Path cacheRoot() {
        return extractionRoot(cache);
    }

    private static Path extractionRoot(Path base) {
        return base.resolve(".jme3").resolve("jme3");
    }

    private static void setExtractionRoots(Path... bases) throws Exception {
        OperatingSystem os = OperatingSystem.valueOf(JmeSystem.getPlatform().getOs().name().toUpperCase(Locale.ROOT));
        field("extractionCandidates").set(null, NativeDirectories.fromRoots("jme3", List.of(bases), () -> os));
    }

    private static Path createNoexecDirectory() throws Exception {
        assumeTrue(JmeSystem.getPlatform().getOs() == Platform.Os.Linux);
        Path executable = Paths.get("/bin/true");
        assumeTrue(Files.isRegularFile(executable));
        for (String name : new String[]{"/dev/shm", "/run/lock"}) {
            Path root = Paths.get(name);
            if (!Files.isDirectory(root) || !Files.isWritable(root)) continue;
            Path probe = null;
            try {
                probe = Files.createTempFile(root, "jme-noexec-", null);
                Files.copy(executable, probe, StandardCopyOption.REPLACE_EXISTING);
                Files.setPosixFilePermissions(probe, PosixFilePermissions.fromString("rwx------"));
                try {
                    // Exercise actual execution instead of repeating the loader's isExecutable check.
                    Process process = new ProcessBuilder(probe.toString()).start();
                    if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly();
                } catch (IOException cannotExecute) {
                    return Files.createTempDirectory(root, "jme-test-");
                }
            } catch (IOException unavailable) {
                // Try another writable candidate.
            } finally {
                if (probe != null) Files.deleteIfExists(probe);
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
