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
package com.jme3.export.binary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Covers one-byte class aliases and unchanged decoding of text fields.
 * The larger, two-byte alias boundary is covered by a separate ordinary-exporter
 * smoke test with 255, 256, 257, and 261 distinct classes, as documented in PR #2991.
 */
public class BinaryImporterClassAliasTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 126, 127, 128, 253})
    public void distinctClassAliasesPreserveObjectTypes(int firstAlias) throws IOException {
        Root original = new Root();
        original.values = new Savable[]{new FirstValue(), new SecondValue()};
        BinaryExporter exporter = new BinaryExporter() {
            @Override
            protected byte[] generateTag() {
                // Exercise aliases used in larger scenes without defining hundreds of fixture classes.
                return new byte[]{(byte) (firstAlias + aliasCount++ - 1)};
            }
        };
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        exporter.save(original, output);

        Root loaded = (Root) new BinaryImporter().load(output.toByteArray());
        assertNotNull(loaded);
        assertNotNull(loaded.values);
        assertEquals(2, loaded.values.length);
        assertEquals(FirstValue.class, loaded.values[0].getClass());
        assertEquals(SecondValue.class, loaded.values[1].getClass());
    }

    @Test
    public void nonAsciiFieldNamesAndStringValuesRemainReadable() {
        Root original = new Root();
        original.text = "R\u00e9sum\u00e9 \u65e5\u672c\u8a9e";

        Root loaded = BinaryExporter.saveAndLoad(null, original);

        assertEquals(original.text, loaded.text);
    }

    public static class Root implements Savable {
        Savable[] values;
        String text;

        @Override
        public void write(JmeExporter exporter) throws IOException {
            exporter.getCapsule(this).write(values, "values", null);
            exporter.getCapsule(this).write(text, "text-\u00e9", null);
        }

        @Override
        public void read(JmeImporter importer) throws IOException {
            values = importer.getCapsule(this).readSavableArray("values", null);
            text = importer.getCapsule(this).readString("text-\u00e9", null);
        }
    }

    public static class FirstValue implements Savable {
        @Override
        public void write(JmeExporter exporter) {
        }

        @Override
        public void read(JmeImporter importer) {
        }
    }

    public static class SecondValue implements Savable {
        @Override
        public void write(JmeExporter exporter) {
        }

        @Override
        public void read(JmeImporter importer) {
        }
    }
}
