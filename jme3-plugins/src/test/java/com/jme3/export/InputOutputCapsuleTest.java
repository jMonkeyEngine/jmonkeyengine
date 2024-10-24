/*
 * Copyright (c) 2023 jMonkeyEngine
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
package com.jme3.export;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.BitSet;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.AssetKey;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.export.xml.XMLExporter;
import com.jme3.export.xml.XMLImporter;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Matrix4f;

/**
 * Test suite for implementations of the JmeExporter and JmeImporter interfaces.
 * There are tests here for all write* and read* methods of the OutputCapsule and InputCapsule interfaces respectively.
 */
public class InputOutputCapsuleTest {
    private static final List<JmeExporter> exporters = new ArrayList<>();
    private static final List<JmeImporter> importers = new ArrayList<>();
    static {
        exporters.add(new BinaryExporter());
        importers.add(new BinaryImporter());

        exporters.add(new XMLExporter());
        importers.add(new XMLImporter());

        // add any future implementations here
    }

    @Test
    public void testPrimitives() {
        saveAndLoad(new TestPrimitives());
    }

    @Test
    public void testStrings() {
        saveAndLoad(new TestStrings());
    }

    @Test
    public void testEnums() {
        saveAndLoad(new TestEnums());
    }

    @Test
    public void testBitSets() {
        saveAndLoad(new TestBitSets());
    }

    @Test
    public void testSavables() {
        saveAndLoad(new TestSavables());
    }

    @Test
    public void testSavableReferences() {
        saveAndLoad(new TestSavableReferences());
    }

    @Test
    public void testArrays() {
        saveAndLoad(new TestArrays());
    }

    @Test
    public void testBuffers() {
        saveAndLoad(new TestBuffers());
    }

    @Test
    public void testLists() {
        saveAndLoad(new TestLists());
    }

    @Test
    public void testMaps() {
        saveAndLoad(new TestMaps());
    }

    // attempts to save and load a Savable using the JmeExporter/JmeImporter implementations listed at the top of this class.
    // the Savable inner classes in this file run assertions in their read() methods
    // to ensure the data loaded is the same as what was written.  more or less stole this from JmeExporterTest.java
    private static void saveAndLoad(Savable savable) {
        for (int i = 0; i < exporters.size(); i++) {
            JmeExporter exporter = exporters.get(i);
            JmeImporter importer = importers.get(i);

            // export
            byte[] exportedBytes = null;
            try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
                exporter.save(savable, outStream);
                exportedBytes = outStream.toByteArray();
            } catch (IOException e) {
                Assert.fail(exporter.getClass().getSimpleName() + ": " + e.toString());
            }

            // write the xml into files for debugging.
            // leave this commented out unless you need it since it makes a mess of the jme3-plugins directory.
            /*if (exporter instanceof XMLExporter) {
                try {
                    File outFile = new File(savable.getClass().getSimpleName() + ".xml");
                    outFile.createNewFile();
                    PrintWriter out = new PrintWriter(outFile);
                    out.print(new String(exportedBytes));
                    out.close();
                } catch(IOException ioEx) {

                }
            }*/

            // import
            try (ByteArrayInputStream inStream = new ByteArrayInputStream(exportedBytes)) {
                AssetInfo info = new AssetInfo(null, null) {
                    @Override
                    public InputStream openStream() {
                        return inStream;
                    }
                };
                importer.load(info);    // this is where assertions will fail if loaded data does not match saved data.
            } catch (IOException e) {
                Assert.fail(exporter.getClass().getSimpleName() + ": " + e.toString());
            }
        }
    }

    // test data.  I tried to include as many edge cases as I could think of.
    private static final byte[] testByteArray = new byte[] {Byte.MIN_VALUE, Byte.MAX_VALUE};
    private static final short[] testShortArray = new short[] {Short.MIN_VALUE, Short.MAX_VALUE};
    private static final int[] testIntArray = new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE};
    private static final long[] testLongArray = new long[] {Long.MIN_VALUE, Long.MAX_VALUE};
    private static final float[] testFloatArray = new float[] {
        Float.MIN_VALUE, Float.MAX_VALUE, Float.MIN_NORMAL,
        Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN
    };
    private static final double[] testDoubleArray = new double[] {
        Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_NORMAL,
        Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN
    };
    private static final boolean[] testBooleanArray = new boolean[] {false, true};
    private static final String[] testStringArray = new String[] {
        "hello, world!",
        null,
        "",
        " ",   // blank string (whitespace)
        "mind    the gap",   // multiple consecutive spaces (some xml processors would normalize this to a single space)
        //new String(new char[10_000_000]).replace('\0', 'a'),    // long string. kinda slows down the test too much so I'm leaving it out for now.
        "\t",
        "\n",
        "\r",
        "hello  こんにちは 你好  Здравствуйте  안녕하세요  🙋",
        "&apos; &quot; &lt; &gt; &amp;", // xml entities
        // xml metacharacters
        "\'",
        "\"",
        "<",
        ">",
        "&",
        "<!--", // xml comment
        "-->",
        "]]>"   // xml close cdata
    };

    private static final Savable[] testSavableArray = new Savable[] {
        new Vector3f(0f, 1f, 2f),
        null,
        new Quaternion(0f, 1f, 2f, 3f),
        new Transform(new Vector3f(1f, 2f, 3f), new Quaternion(1f, 2f, 3f, 4f), new Vector3f(1f, 2f, 3f)),
        new Matrix4f()
    };

    private static final byte[][] testByteArray2D = new byte[][] {
        testByteArray,
        null,
        new byte[0]
    };

    private static final short[][] testShortArray2D = new short[][] {
        testShortArray,
        null,
        new short[0]
    };

    private static final int[][] testIntArray2D = new int[][] {
        testIntArray,
        null,
        new int[0]
    };

    private static final long[][] testLongArray2D = new long[][] {
        testLongArray,
        null,
        new long[0]
    };

    private static final float[][] testFloatArray2D = new float[][] {
        testFloatArray,
        null,
        new float[0]
    };

    private static final double[][] testDoubleArray2D = new double[][] {
        testDoubleArray,
        null,
        new double[0]
    };

    private static final boolean[][] testBooleanArray2D = new boolean[][] {
        testBooleanArray,
        null,
        new boolean[0]
    };

    private static final String[][] testStringArray2D = new String[][] {
        testStringArray,
        null,
        new String[0]
    };

    private static final CullHint[] testEnumArray = new CullHint[] {
        CullHint.Never,
        null,
        CullHint.Always,
        CullHint.Inherit
    };

    private static final BitSet[] testBitSetArray = new BitSet[] {
        BitSet.valueOf("BitSet".getBytes()),
        null,
        new BitSet()
    };

    private static final Savable[][] testSavableArray2D = new Savable[][] {
        testSavableArray,
        null,
        new Savable[0]
    };

    private static final ByteBuffer testByteBuffer = (ByteBuffer) BufferUtils.createByteBuffer(testByteArray).rewind();
    private static final ShortBuffer testShortBuffer = (ShortBuffer) BufferUtils.createShortBuffer(testShortArray).rewind();
    private static final IntBuffer testIntBuffer = (IntBuffer) BufferUtils.createIntBuffer(testIntArray).rewind();
    private static final FloatBuffer testFloatBuffer = (FloatBuffer) BufferUtils.createFloatBuffer(testFloatArray).rewind();

    private static final ArrayList<ByteBuffer> testByteBufferArrayList = new ArrayList<>(Arrays.asList(
        BufferUtils.createByteBuffer(testByteArray2D[0]),
        null,
        BufferUtils.createByteBuffer(testByteArray2D[2])
    ));

    private static final ArrayList<FloatBuffer> testFloatBufferArrayList = new ArrayList<>(Arrays.asList(
        BufferUtils.createFloatBuffer(testFloatArray2D[0]),
        null,
        BufferUtils.createFloatBuffer(testFloatArray2D[2])
    ));

    private static final ArrayList<Savable> testSavableArrayList = new ArrayList<>(Arrays.asList(testSavableArray));

    @SuppressWarnings("unchecked")
    private static final ArrayList<Savable>[] testSavableArrayListArray = new ArrayList[] {
        testSavableArrayList,
        null,
        new ArrayList()
    };

    // "array" and "list" don't sound like real words anymore
    @SuppressWarnings("unchecked")
    private static final ArrayList<Savable>[][] testSavableArrayListArray2D = new ArrayList[][] {
        testSavableArrayListArray,
        null,
        {},
    };

    private static final Map<Savable, Savable> testSavableMap = new HashMap<Savable, Savable>() {{
        put(Vector3f.UNIT_X, Vector3f.UNIT_X);
        put(Vector3f.UNIT_Y, Quaternion.IDENTITY);
        put(Vector3f.UNIT_Z, null);
    }};

    private static final Map<String, Savable> testStringSavableMap = new HashMap<String, Savable>() {{
        put("v", Vector3f.UNIT_X);
        put("q", Quaternion.IDENTITY);
        put("n", null);
    }};

    private static final IntMap<Savable> testIntSavableMap = new IntMap<Savable>();
    static {    //IntMap is final so we gotta use a static block here.
        testIntSavableMap.put(0, Vector3f.UNIT_X);
        testIntSavableMap.put(1, Quaternion.IDENTITY);
        testIntSavableMap.put(2, null);
    }

    // the rest of this file is inner classes that implement Savable.
    // these classes write the test data, then verify that it's the same data in their read() methods.
    private static class TestPrimitives implements Savable {
        TestPrimitives() {

        }

        @Override
        public void write(JmeExporter je) throws IOException {
            OutputCapsule capsule = je.getCapsule(this);

            for (int i = 0; i < testByteArray.length; i++)
                capsule.write(testByteArray[i], "test_byte_" + i, (byte) 0);

            for (int i = 0; i < testShortArray.length; i++)
                capsule.write(testShortArray[i], "test_short_" + i, (short) 0);

            for (int i = 0; i < testIntArray.length; i++)
                capsule.write(testIntArray[i], "test_int_" + i, 0);

            for (int i = 0; i < testLongArray.length; i++)
                capsule.write(testLongArray[i], "test_long_" + i, 0l);

            for (int i = 0; i < testFloatArray.length; i++)
                capsule.write(testFloatArray[i], "test_float_" + i, 0f);

            for (int i = 0; i < testDoubleArray.length; i++)
                capsule.write(testDoubleArray[i], "test_double_" + i, 0d);

            for (int i = 0; i < testBooleanArray.length; i++)
                capsule.write(testBooleanArray[i], "test_boolean_" + i, false);
        }

        @Override
        public void read(JmeImporter ji) throws IOException {
            InputCapsule capsule = ji.getCapsule(this);

            for (int i = 0; i < testByteArray.length; i++)
                Assert.assertEquals("readByte()", testByteArray[i], capsule.readByte("test_byte_" + i, (byte) 0));

            for (int i = 0; i < testShortArray.length; i++)
                Assert.assertEquals("readShort()", testShortArray[i], capsule.readShort("test_short_" + i, (short) 0));

            for (int i = 0; i < testIntArray.length; i++)
                Assert.assertEquals("readInt()", testIntArray[i], capsule.readInt("test_int_" + i, 0));

            for (int i = 0; i < testLongArray.length; i++)
                Assert.assertEquals("readLong()", testLongArray[i], capsule.readLong("test_long_" + i, 0l));

            for (int i = 0; i < testFloatArray.length; i++)
                Assert.assertEquals("readFloat()", testFloatArray[i], capsule.readFloat("test_float_" + i, 0f), 0f);

            for (int i = 0; i < testDoubleArray.length; i++)
                Assert.assertEquals("readDouble()", testDoubleArray[i], capsule.readDouble("test_double_" + i, 0d), 0d);

            for (int i = 0; i < testBooleanArray.length; i++)
                Assert.assertEquals("readBoolean()", testBooleanArray[i], capsule.readBoolean("test_boolean_" + i, false));
        }
    }

    private static class TestStrings implements Savable {
        TestStrings() {

        }

        @Override
        public void write(JmeExporter je) throws IOException {
            OutputCapsule capsule = je.getCapsule(this);

            for (int i = 0; i < testStringArray.length; i++) {
                capsule.write(testStringArray[i], "test_string_" + i, null);
            }
        }

        @Override
        public void read(JmeImporter ji) throws IOException {
            InputCapsule capsule = ji.getCapsule(this);

            for (int i = 0; i < testStringArray.length; i++) {
                Assert.assertEquals("readString()", testStringArray[i], capsule.readString("test_string_" + i, null));
            }
        }
    }

    private static class TestEnums implements Savable {
        TestEnums() {

        }

        @Override
        public void write(JmeExporter je) throws IOException {
            OutputCapsule capsule = je.getCapsule(this);

            for (int i = 0; i < testEnumArray.length; i++) {
                capsule.write(testEnumArray[i], "test_enum_" + i, null);
            }
        }

        @Override
        public void read(JmeImporter ji) throws IOException {
            InputCapsule capsule = ji.getCapsule(this);

            for (int i = 0; i < testEnumArray.length; i++) {
                Assert.assertEquals("readEnum()", testEnumArray[i], capsule.readEnum("test_enum_" + i, CullHint.class, null));
            }
        }
    }

    private static class TestBitSets implements Savable {
        TestBitSets() {

        }

        @Override
        public void write(JmeExporter je) throws IOException {
            OutputCapsule capsule = je.getCapsule(this);

            for (int i = 0; i < testBitSetArray.length; i++) {
                capsule.write(testBitSetArray[i], "test_bit_set_" + i, null);
            }
        }

        @Override
        public void read(JmeImporter ji) throws IOException {
            InputCapsule capsule = ji.getCapsule(this);

            for (int i = 0; i < testBitSetArray.length; i++) {
                Assert.assertEquals("readBitSet()", testBitSetArray[i], capsule.readBitSet("test_bit_set_" + i, null));
            }
        }
    }

    private static class TestSavables implements Savable {
        TestSavables() {

        }

        @Override
        public void write(JmeExporter je) throws IOException {
            OutputCapsule capsule = je.getCapsule(this);

            for(int i = 0; i < testSavableArray.length; i++)
                capsule.write(testSavableArray[i], "test_savable_" + i, null);
        }

        @Override
        public void read(JmeImporter ji) throws IOException {
            InputCapsule capsule = ji.getCapsule(this);

            for(int i = 0; i < testSavableArray.length; i++)
                Assert.assertEquals("readSavable()", testSavableArray[i], capsule.readSavable("test_savable_" + i, null));
        }
    }

    private static class TestSavableReferences implements Savable {
        TestSavableReferences() {

        }

        @Override
        public void write(JmeExporter je) throws IOException {
            OutputCapsule capsule = je.getCapsule(this);

            Vector3f v1 = new Vector3f(1f, 2f, 3f);
            Vector3f notV1 = v1.clone();

            capsule.write(v1, "v1", null);
            capsule.write(v1, "also_v1", null);
            capsule.write(notV1, "not_v1", null);

            // testing reference loop.  this used to cause infinite recursion.
            Node n1 = new Node("node_1");
            Node n2 = new Node("node_2");

            n1.setUserData("node_2", n2);
            n2.setUserData("node_1", n1);

            capsule.write(n1, "node_1", null);
            capsule.write(n2, "node_2", null);
        }

        @Override
        public void read(JmeImporter ji) throws IOException {
            InputCapsule capsule = ji.getCapsule(this);

            Vector3f v1 = (Vector3f) capsule.readSavable("v1", null);
            Vector3f alsoV1 = (Vector3f) capsule.readSavable("also_v1", null);
            Vector3f notV1 = (Vector3f) capsule.readSavable("not_v1", null);

            Assert.assertTrue("readSavable() savable duplicated, references not preserved.", v1 == alsoV1);
            Assert.assertTrue("readSavable() unique savables merged, unexpected shared references.", v1 != notV1);

            Node n1 = (Node) capsule.readSavable("node_1", null);
            Node n2 = (Node) capsule.readSavable("node_2", null);

            Assert.assertTrue("readSavable() reference loop not preserved.", n1.getUserData("node_2") == n2 && n2.getUserData("node_1") == n1);
        }
    }

    private static class TestArrays implements Savable {
        TestArrays() {

        }

        @Override
        public void write(JmeExporter je) throws IOException {
            OutputCapsule capsule = je.getCapsule(this);

            capsule.write(testByteArray, "testByteArray", null);
            capsule.write(testShortArray, "testShortArray", null);
            capsule.write(testIntArray, "testIntArray", null);
            capsule.write(testLongArray, "testLongArray", null);
            capsule.write(testFloatArray, "testFloatArray", null);
            capsule.write(testDoubleArray, "testDoubleArray", null);
            capsule.write(testBooleanArray, "testBooleanArray", null);
            capsule.write(testStringArray, "testStringArray", null);
            capsule.write(testSavableArray, "testSavableArray", null);

            capsule.write(new byte[0], "emptyByteArray", null);
            capsule.write(new short[0], "emptyShortArray", null);
            capsule.write(new int[0], "emptyIntArray", null);
            capsule.write(new long[0], "emptyLongArray", null);
            capsule.write(new float[0], "emptyFloatArray", null);
            capsule.write(new double[0], "emptyDoubleArray", null);
            capsule.write(new boolean[0], "emptyBooleanArray", null);
            capsule.write(new String[0], "emptyStringArray", null);
            capsule.write(new Savable[0], "emptySavableArray", null);
            
            capsule.write(testByteArray2D, "testByteArray2D", null);
            capsule.write(testShortArray2D, "testShortArray2D", null);
            capsule.write(testIntArray2D, "testIntArray2D", null);
            capsule.write(testLongArray2D, "testLongArray2D", null);
            capsule.write(testFloatArray2D, "testFloatArray2D", null);
            capsule.write(testDoubleArray2D, "testDoubleArray2D", null);
            capsule.write(testBooleanArray2D, "testBooleanArray2D", null);
            capsule.write(testStringArray2D, "testStringArray2D", null);
            capsule.write(testSavableArray2D, "testSavableArray2D", null);
        }

        @Override
        public void read(JmeImporter ji) throws IOException {
            InputCapsule capsule = ji.getCapsule(this);

            Assert.assertArrayEquals("readByteArray()", testByteArray, capsule.readByteArray("testByteArray", null));
            Assert.assertArrayEquals("readShortArray()", testShortArray, capsule.readShortArray("testShortArray", null));
            Assert.assertArrayEquals("readIntArray()", testIntArray, capsule.readIntArray("testIntArray", null));
            Assert.assertArrayEquals("readLongArray()", testLongArray, capsule.readLongArray("testLongArray", null));
            Assert.assertArrayEquals("readFloatArray()", testFloatArray, capsule.readFloatArray("testFloatArray", null), 0f);
            Assert.assertArrayEquals("readDoubleArray()", testDoubleArray, capsule.readDoubleArray("testDoubleArray", null), 0d);
            Assert.assertArrayEquals("readBooleanArray()", testBooleanArray, capsule.readBooleanArray("testBooleanArray", null));
            Assert.assertArrayEquals("readStringArray()", testStringArray, capsule.readStringArray("testStringArray", null));
            Assert.assertArrayEquals("readSavableArray()", testSavableArray, capsule.readSavableArray("testSavableArray", null));

            Assert.assertArrayEquals("readByteArray()", new byte[0], capsule.readByteArray("emptyByteArray", null));
            Assert.assertArrayEquals("readShortArray()", new short[0], capsule.readShortArray("emptyShortArray", null));
            Assert.assertArrayEquals("readIntArray()", new int[0], capsule.readIntArray("emptyIntArray", null));
            Assert.assertArrayEquals("readLongArray()", new long[0], capsule.readLongArray("emptyLongArray", null));
            Assert.assertArrayEquals("readFloatArray()", new float[0], capsule.readFloatArray("emptyFloatArray", null), 0f);
            Assert.assertArrayEquals("readDoubleArray()", new double[0], capsule.readDoubleArray("emptyDoubleArray", null), 0d);
            Assert.assertArrayEquals("readBooleanArray()", new boolean[0], capsule.readBooleanArray("emptyBooleanArray", null));
            Assert.assertArrayEquals("readStringArray()", new String[0], capsule.readStringArray("emptyStringArray", null));
            Assert.assertArrayEquals("readSavableArray()", new Savable[0], capsule.readSavableArray("emptySavableArray", null));

            Assert.assertArrayEquals("readByteArray2D()", testByteArray2D, capsule.readByteArray2D("testByteArray2D", null));
            Assert.assertArrayEquals("readShortArray2D()", testShortArray2D, capsule.readShortArray2D("testShortArray2D", null));
            Assert.assertArrayEquals("readIntArray2D()", testIntArray2D, capsule.readIntArray2D("testIntArray2D", null));
            Assert.assertArrayEquals("readLongArray2D()", testLongArray2D, capsule.readLongArray2D("testLongArray2D", null));
            Assert.assertArrayEquals("readFloatArray2D()", testFloatArray2D, capsule.readFloatArray2D("testFloatArray2D", null));
            Assert.assertArrayEquals("readDoubleArray2D()", testDoubleArray2D, capsule.readDoubleArray2D("testDoubleArray2D", null));
            Assert.assertArrayEquals("readBooleanArray2D()", testBooleanArray2D, capsule.readBooleanArray2D("testBooleanArray2D", null));
            Assert.assertArrayEquals("readStringArray2D()", testStringArray2D, capsule.readStringArray2D("testStringArray2D", null));
            Assert.assertArrayEquals("readSavableArray2D()", testSavableArray2D, capsule.readSavableArray2D("testSavableArray2D", null));
        }
    }

    private static class TestBuffers implements Savable {
        TestBuffers() {
        }

        @Override
        public void write(JmeExporter je) throws IOException {
            OutputCapsule capsule = je.getCapsule(this);

            capsule.write(testByteBuffer, "testByteBuffer", null);
            capsule.write(testShortBuffer, "testShortBuffer", null);
            capsule.write(testIntBuffer, "testIntBuffer", null);
            capsule.write(testFloatBuffer, "testFloatBuffer", null);

            capsule.write(BufferUtils.createByteBuffer(0), "emptyByteBuffer", null);
            capsule.write(BufferUtils.createShortBuffer(0), "emptyShortBuffer", null);
            capsule.write(BufferUtils.createIntBuffer(0), "emptyIntBuffer", null);
            capsule.write(BufferUtils.createFloatBuffer(0), "emptyFloatBuffer", null);
        }

        @Override
        public void read(JmeImporter ji) throws IOException {
            InputCapsule capsule = ji.getCapsule(this);

            Assert.assertEquals("readByteBuffer()", testByteBuffer, capsule.readByteBuffer("testByteBuffer", null));
            Assert.assertEquals("readShortBuffer()", testShortBuffer, capsule.readShortBuffer("testShortBuffer", null));
            Assert.assertEquals("readIntBuffer()", testIntBuffer, capsule.readIntBuffer("testIntBuffer", null));
            Assert.assertEquals("readFloatBuffer()", testFloatBuffer, capsule.readFloatBuffer("testFloatBuffer", null));

            Assert.assertEquals("readByteBuffer()", BufferUtils.createByteBuffer(0), capsule.readByteBuffer("emptyByteBuffer", null));
            Assert.assertEquals("readShortBuffer()", BufferUtils.createShortBuffer(0), capsule.readShortBuffer("emptyShortBuffer", null));
            Assert.assertEquals("readIntBuffer()", BufferUtils.createIntBuffer(0), capsule.readIntBuffer("emptyIntBuffer", null));
            Assert.assertEquals("readFloatBuffer()", BufferUtils.createFloatBuffer(0), capsule.readFloatBuffer("emptyFloatBuffer", null));
        }
    }

    private static class TestLists implements Savable {
        TestLists() {

        }

        @Override
        public void write(JmeExporter je) throws IOException {
            OutputCapsule capsule = je.getCapsule(this);

            capsule.writeByteBufferArrayList(testByteBufferArrayList, "testByteBufferArrayList", null);
            capsule.writeFloatBufferArrayList(testFloatBufferArrayList, "testFloatBufferArrayList", null);
            capsule.writeSavableArrayList(testSavableArrayList, "testSavableArrayList", null);
            capsule.writeSavableArrayListArray(testSavableArrayListArray, "testSavableArrayListArray", null);
            capsule.writeSavableArrayListArray2D(testSavableArrayListArray2D, "testSavableArrayListArray2D", null);
        }

        @Override
        public void read(JmeImporter ji) throws IOException {
            InputCapsule capsule = ji.getCapsule(this);

            Assert.assertEquals("readByteBufferArrayList()", testByteBufferArrayList, capsule.readByteBufferArrayList("testByteBufferArrayList", null));
            Assert.assertEquals("readFloatBufferArrayList()", testFloatBufferArrayList, capsule.readFloatBufferArrayList("testFloatBufferArrayList", null));
            Assert.assertEquals("readSavableArrayList()", testSavableArrayList, capsule.readSavableArrayList("testSavableArrayList", null));
            Assert.assertEquals("readSavableArrayListArray()", testSavableArrayListArray, capsule.readSavableArrayListArray("testSavableArrayListArray", null));
            Assert.assertEquals("readSavableArrayListArray2D()", testSavableArrayListArray2D, capsule.readSavableArrayListArray2D("testSavableArrayListArray2D", null));
        }
    }

    private static class TestMaps implements Savable {
        TestMaps() {

        }

        @Override
        public void write(JmeExporter je) throws IOException {
            OutputCapsule capsule = je.getCapsule(this);

            capsule.writeSavableMap(testSavableMap, "testSavableMap", null);
            capsule.writeStringSavableMap(testStringSavableMap, "testStringSavableMap", null);
            capsule.writeIntSavableMap(testIntSavableMap, "testIntSavableMap", null);
        }

        @Override
        public void read(JmeImporter ji) throws IOException {
            InputCapsule capsule = ji.getCapsule(this);

            Assert.assertEquals("readSavableMap()", testSavableMap, capsule.readSavableMap("testSavableMap", null));
            Assert.assertEquals("readStringSavableMap()", testStringSavableMap, capsule.readStringSavableMap("testStringSavableMap", null));

            // IntMap does not implement equals() so we have to do it manually
            IntMap loadedIntMap = capsule.readIntSavableMap("testIntSavableMap", null);
            Iterator iterator = testIntSavableMap.iterator();
            while(iterator.hasNext()) {
                IntMap.Entry entry = (IntMap.Entry) iterator.next();
                Assert.assertEquals("readIntSavableMap()", entry.getValue(), loadedIntMap.get(entry.getKey()));
            }
        }
    }
}
