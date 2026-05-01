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
 *
 * Standalone J3O scanner for CI/pre-commit.
 *
 * It validates jME3 binary asset headers without instantiating Savables.
 * This is intentional: using BinaryImporter would execute class loading,
 * constructors and Savable.read() for data supplied by the asset.
 * 
 * Note: the default baseline used by this scanner is not intended to be comprehensive. 
 *       It is mostly what we may use in our tests and examples and intended to be used as part of jme CI.
 *
 * Usage:
 *   ./gradlew :jme3-desktop:scanJ3O
 *
 * Optional:
 *   java com.jme3.system.J3OScanner --write-baseline j3o-baseline.txt .
 *   java com.jme3.system.J3OScanner --baseline j3o-baseline.txt .
 *   java com.jme3.system.J3OScanner --allow-prefix com.mycompany.game. .
 */
package com.jme3.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class J3OScanner {
    private static final int SIGNATURE = 0x4A4D4533; // "JME3"
    private static final int CURRENT_FORMAT_VERSION = 3;
    private static final String DEFAULT_BASELINE_RESOURCE = "/com/jme3/system/j3o-baseline.txt";

    private static final long MAX_FILE_BYTES = 64L * 1024L * 1024L;
    private static final int MAX_CLASSES = 2048;
    private static final int MAX_CLASS_NAME_BYTES = 512;
    private static final int MAX_FIELDS_PER_CLASS = 8192;
    private static final int MAX_FIELD_NAME_BYTES = 1024;
    private static final int MAX_LOCATION_ENTRIES = 2_000_000;

    private static final Pattern JAVA_BINARY_NAME = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*(\\$[A-Za-z_$][A-Za-z0-9_$]*)*");

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        if (config.roots.isEmpty()) {
            printUsage();
            System.exit(2);
        }

        Set<String> baseline = readBaseline(config.baselineFile);
        Set<String> allowed = new HashSet<>(baseline);
        allowed.addAll(config.extraAllowedClasses);

        List<Path> files = findJ3OFiles(config.roots);
        System.out.println("J3O baseline classes: " + baseline.size());
        if (files.isEmpty()) {
            System.out.println("No .j3o files found.");
            return;
        }

        List<String> errors = new ArrayList<>();
        List<ScanResult> results = new ArrayList<>();
        Set<String> observedClasses = new TreeSet<>();

        for (Path file : files) {
            try {
                J3OInfo info = parse(file);
                observedClasses.addAll(info.classNames);

                Set<String> rejectedClasses = new TreeSet<>();
                for (String className : info.classNames) {
                    if (!isAllowed(className, allowed, config.allowedPrefixes)) {
                        rejectedClasses.add(className);
                    }
                }
                if (rejectedClasses.isEmpty()) {
                    results.add(new ScanResult(file, info.classNames.size(), "SAFE", "-"));
                } else {
                    String detail = "Disallowed: " + join(rejectedClasses);
                    results.add(new ScanResult(file, info.classNames.size(), "UNSAFE", detail));
                    errors.add(file + ": " + detail);
                }
            } catch (IOException | RuntimeException e) {
                String detail = "Invalid: " + e.getMessage();
                results.add(new ScanResult(file, 0, "UNSAFE", detail));
                errors.add(file + ": " + detail);
            }
        }

        if (config.writeBaselineFile != null) {
            List<String> lines = new ArrayList<>(observedClasses);
            Path parent = config.writeBaselineFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(config.writeBaselineFile, lines, StandardCharsets.UTF_8);
            System.out.println("Wrote " + lines.size() + " allowed classes to " + config.writeBaselineFile);
        }

        printTable(results);
        printFinalReport(files.size(), results, errors);

        if (!errors.isEmpty()) {
            System.out.println();
            System.out.println("Unsafe J3O report:");
            for (String error : errors) {
                System.out.println("  - " + error);
            }
            System.exit(1);
        }
    }

    private static boolean isAllowed(String className, Set<String> allowed, List<String> prefixes) {
        if (allowed.contains(className)) {
            return true;
        }
        for (String prefix : prefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> readBaseline(Path path) throws IOException {
        if (path != null) {
            return readBaseline(Files.newInputStream(path), path.toString());
        }
        InputStream stream = J3OScanner.class.getResourceAsStream(DEFAULT_BASELINE_RESOURCE);
        if (stream == null) {
            throw new IOException("missing default J3O baseline resource: " + DEFAULT_BASELINE_RESOURCE);
        }
        return readBaseline(stream, DEFAULT_BASELINE_RESOURCE);
    }

    private static Set<String> readBaseline(InputStream stream, String source) throws IOException {
        Set<String> result = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#")) {
                    continue;
                }
                if (!JAVA_BINARY_NAME.matcher(s).matches()) {
                    throw new SecurityException("invalid class name in baseline " + source + ": " + s);
                }
                result.add(s);
            }
        }
        return result;
    }

    private static List<Path> findJ3OFiles(List<Path> roots) throws IOException {
        List<Path> result = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                throw new IOException("path does not exist: " + root);
            }
            if (Files.isRegularFile(root)) {
                if (root.getFileName().toString().toLowerCase().endsWith(".j3o")) {
                    result.add(root);
                }
                continue;
            }
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".j3o"))
                        .forEach(result::add);
            }
        }
        Collections.sort(result);
        return result;
    }

    private static J3OInfo parse(Path file) throws IOException {
        long size = Files.size(file);
        if (size < 4) {
            throw new SecurityException("file too small");
        }
        if (size > MAX_FILE_BYTES) {
            throw new SecurityException("file exceeds max size " + MAX_FILE_BYTES + " bytes: " + size);
        }

        byte[] data = Files.readAllBytes(file);
        Cursor c = new Cursor(data);
        int first = c.readInt();

        int formatVersion;
        int numClasses;
        if (first == SIGNATURE) {
            formatVersion = c.readInt();
            numClasses = c.readInt();
            if (formatVersion < 0 || formatVersion > CURRENT_FORMAT_VERSION) {
                throw new SecurityException("unsupported J3O format version: " + formatVersion);
            }
        } else {
            formatVersion = 0;
            numClasses = first;
        }

        if (numClasses <= 0 || numClasses > MAX_CLASSES) {
            throw new SecurityException("unsafe class count: " + numClasses);
        }

        int aliasWidth = aliasWidth(numClasses);
        List<String> classNames = new ArrayList<>(numClasses);

        for (int i = 0; i < numClasses; i++) {
            c.skip(aliasWidth, "class alias");

            if (formatVersion >= 1) {
                int hierarchySize = c.readUnsignedByte();
                // Defensive cap: one class plus a few Savable superclasses is normal.
                if (hierarchySize > 64) {
                    throw new SecurityException("unreasonable class hierarchy size: " + hierarchySize);
                }
                c.skip(4 * hierarchySize, "class hierarchy versions");
            }

            int classNameLength = c.readInt();
            if (classNameLength <= 0 || classNameLength > MAX_CLASS_NAME_BYTES) {
                throw new SecurityException("unsafe class name length: " + classNameLength);
            }

            String className = c.readUtf8(classNameLength);
            if (!JAVA_BINARY_NAME.matcher(className).matches()) {
                throw new SecurityException("invalid class name syntax: " + className);
            }
            classNames.add(className);

            int fields = c.readInt();
            if (fields < 0 || fields > MAX_FIELDS_PER_CLASS) {
                throw new SecurityException("unsafe field count for " + className + ": " + fields);
            }

            Set<Integer> fieldAliases = new HashSet<>();
            for (int j = 0; j < fields; j++) {
                int fieldAlias = c.readUnsignedByte();
                c.readUnsignedByte(); // field type
                if (!fieldAliases.add(fieldAlias)) {
                    throw new SecurityException("duplicate field alias " + fieldAlias + " in " + className);
                }

                int fieldNameLength = c.readInt();
                if (fieldNameLength < 0 || fieldNameLength > MAX_FIELD_NAME_BYTES) {
                    throw new SecurityException("unsafe field name length in " + className + ": " + fieldNameLength);
                }
                c.skip(fieldNameLength, "field name");
            }
        }

        int numLocations = c.readInt();
        if (numLocations < 0 || numLocations > MAX_LOCATION_ENTRIES) {
            throw new SecurityException("unsafe object-location count: " + numLocations);
        }

        int payloadStart = c.position() + (numLocations * 8) + 8; // locations + numbIDs + root id
        if (payloadStart > data.length) {
            throw new SecurityException("truncated location/root table");
        }

        for (int i = 0; i < numLocations; i++) {
            c.readInt(); // object id
            int loc = c.readInt();
            if (loc < 0 || loc >= data.length - payloadStart) {
                throw new SecurityException("object location outside payload: " + loc);
            }
        }

        c.readInt(); // numbIDs, unused by BinaryImporter
        c.readInt(); // root object id

        return new J3OInfo(classNames);
    }

    private static int aliasWidth(int numClasses) {
        int width = 1;
        int n = numClasses;
        while (n >= 256) {
            width++;
            n /= 256;
        }
        return width;
    }

    private static void printUsage() {
        System.err.println("Usage: java com.jme3.system.J3OScanner [options] <repo-or-file>...");
        System.err.println("Options:");
        System.err.println("  --baseline <file>          one allowed class per line; defaults to " + DEFAULT_BASELINE_RESOURCE);
        System.err.println("  --write-baseline <file>    write observed classes and validate using the configured baseline");
        System.err.println("  --allow-class <class>      allow one additional exact class");
        System.err.println("  --allow-prefix <prefix>    allow classes under prefix; use sparingly");
    }

    private static void printTable(List<ScanResult> results) {
        System.out.println();
        System.out.println("| J3O file | Classes | Status | Details |");
        System.out.println("| --- | ---: | --- | --- |");
        for (ScanResult result : results) {
            System.out.println("| " + markdown(result.file.toString())
                    + " | " + result.classCount
                    + " | " + result.status
                    + " | " + markdown(result.details) + " |");
        }
    }

    private static void printFinalReport(int fileCount, List<ScanResult> results, List<String> errors) {
        int unsafe = 0;
        for (ScanResult result : results) {
            if (!"SAFE".equals(result.status)) {
                unsafe++;
            }
        }
        int safe = results.size() - unsafe;
        System.out.println();
        if (errors.isEmpty()) {
            System.out.println("J3O scan passed: " + fileCount + " file(s), "
                    + safe + " safe, 0 unsafe.");
        } else {
            System.out.println("J3O scan failed: " + fileCount + " file(s), "
                    + safe + " safe, " + unsafe + " unsafe.");
            System.out.println("Review the asset. If a new class is legitimate, add it explicitly to the baseline.");
        }
    }

    private static String join(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static String markdown(String value) {
        return value.replace("|", "\\|").replace("\n", " ");
    }

    private static final class ScanResult {
        final Path file;
        final int classCount;
        final String status;
        final String details;

        ScanResult(Path file, int classCount, String status, String details) {
            this.file = file;
            this.classCount = classCount;
            this.status = status;
            this.details = details;
        }
    }

    private static final class J3OInfo {
        final List<String> classNames;
        J3OInfo(List<String> classNames) {
            this.classNames = classNames;
        }
    }

    private static final class Cursor {
        private final byte[] data;
        private int offset;

        Cursor(byte[] data) {
            this.data = data;
        }

        int position() {
            return offset;
        }

        int readUnsignedByte() {
            require(1, "byte");
            return data[offset++] & 0xff;
        }

        int readInt() {
            require(4, "int");
            int value = ((data[offset] & 0xff) << 24)
                    | ((data[offset + 1] & 0xff) << 16)
                    | ((data[offset + 2] & 0xff) << 8)
                    | (data[offset + 3] & 0xff);
            offset += 4;
            return value;
        }

        String readUtf8(int length) {
            require(length, "UTF-8 string");
            String value = new String(data, offset, length, StandardCharsets.UTF_8);
            offset += length;
            return value;
        }

        void skip(int bytes, String what) {
            if (bytes < 0) {
                throw new SecurityException("negative skip for " + what + ": " + bytes);
            }
            require(bytes, what);
            offset += bytes;
        }

        void require(int bytes, String what) {
            if (offset + bytes < offset || offset + bytes > data.length) {
                throw new SecurityException("truncated while reading " + what + " at offset " + offset);
            }
        }
    }

    private static final class Config {
        final List<Path> roots = new ArrayList<>();
        final List<String> allowedPrefixes = new ArrayList<>();
        final Set<String> extraAllowedClasses = new HashSet<>();
        Path baselineFile;
        Path writeBaselineFile;

        static Config parse(String[] args) {
            Config c = new Config();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--baseline":
                        c.baselineFile = Paths.get(nextArg(args, ++i, arg));
                        break;
                    case "--write-baseline":
                        c.writeBaselineFile = Paths.get(nextArg(args, ++i, arg));
                        break;
                    case "--allow-class":
                        String cls = nextArg(args, ++i, arg);
                        if (!JAVA_BINARY_NAME.matcher(cls).matches()) {
                            throw new IllegalArgumentException("invalid --allow-class value: " + cls);
                        }
                        c.extraAllowedClasses.add(cls);
                        break;
                    case "--allow-prefix":
                        String prefix = nextArg(args, ++i, arg);
                        if (prefix.trim().isEmpty() || prefix.startsWith(".") || prefix.contains("..")) {
                            throw new IllegalArgumentException("invalid --allow-prefix value: " + prefix);
                        }
                        c.allowedPrefixes.add(prefix);
                        break;
                    default:
                        c.roots.add(Paths.get(arg));
                        break;
                }
            }
            return c;
        }

        private static String nextArg(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException(option + " requires a value");
            }
            return args[index];
        }
    }
}
