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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.export.xml.XMLExporter;
import com.jme3.export.xml.XMLImporter;
import com.jme3.material.Material;
import com.jme3.material.plugin.export.material.J3MExporter;
import com.jme3.scene.Node;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests the methods on classes that implements the JmeExporter interface.
 */
@RunWith(Parameterized.class)
public class JmeExporterTest {

    // test saving with a material since the J3MExporter expects one
    private static Material material;

    private final JmeExporter exporter;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void beforeClass() {
        AssetManager assetManager = new DesktopAssetManager(true);
        material = new Material(assetManager, "Common/MatDefs/Gui/Gui.j3md");
    }

    public JmeExporterTest(JmeExporter exporter) {
        this.exporter = exporter;
    }

    @Parameterized.Parameters
    public static Collection<JmeExporter> defineExporters() {
        return Arrays.asList(new BinaryExporter(), new XMLExporter(), new J3MExporter());
    }

    private File fileWithMissingParent() {
        File dir = new File(folder.getRoot(), "missingDir");
        return new File(dir, "afile.txt");
    }

    private File fileWithExistingParent() throws IOException {
        File dir = folder.newFolder();
        return new File(dir, "afile.txt");
    }

    @Test
    public void testSaveWhenPathDoesntExist() throws IOException {
        File file = fileWithMissingParent();
        Assert.assertFalse(file.exists());
        exporter.save(material, file);
        Assert.assertTrue(file.exists());
    }

    @Test
    public void testSaveWhenPathDoesExist() throws IOException {
        File file = fileWithExistingParent();
        exporter.save(material, file);
        Assert.assertTrue(file.exists());
    }

    @Test(expected = FileNotFoundException.class)
    public void testSaveWhenPathDoesntExistWithoutCreateDirs() throws IOException {
        File file = fileWithMissingParent();
        exporter.save(material, file, false);
        Assert.assertTrue(file.exists());
    }

    @Test
    public void testSaveWithNullParent() throws IOException {
        File file = new File("someFile.txt");
        try {
            exporter.save(material, file);
            Assert.assertTrue(file.exists());
        } finally {
            file.delete();
        }
    }

    @Test
    public void testExporterConsistency() {
        //
        final boolean testXML = true;
        final boolean testLists = false;
        final boolean testMaps = true;
        final boolean printXML = false;

        // initialize data
        AssetManager assetManager = new DesktopAssetManager(true);
        ArrayList<JmeExporter> exporters = new ArrayList<JmeExporter>();
        ArrayList<JmeImporter> importers = new ArrayList<JmeImporter>();

        BinaryExporter be = new BinaryExporter();
        BinaryImporter bi = new BinaryImporter();
        exporters.add(be);
        importers.add(bi);

        if (testXML) {
            XMLExporter xe = new XMLExporter();
            XMLImporter xi = new XMLImporter();
            exporters.add(xe);
            importers.add(xi);
        }

        Node origin = new Node("origin");

        origin.setUserData("testInt", 10);
        origin.setUserData("testString", "ABC");
        origin.setUserData("testBoolean", true);
        origin.setUserData("testFloat", 1.5f);
        origin.setUserData("1", "test");
        if (testLists) {
            origin.setUserData("string-list", Collections.singletonList("abc"));
            origin.setUserData("int-list", Arrays.asList(1, 2, 3));
            origin.setUserData("float-list", Arrays.asList(1f, 2f, 3f));
        }

        if (testMaps) {
            Map<String, Object> map = new HashMap<>();
            map.put("int", 1);
            map.put("string", "abc");
            map.put("float", 1f);
            origin.setUserData("map", map);
        }

        // export
        ByteArrayOutputStream outs[] = new ByteArrayOutputStream[exporters.size()];
        for (int i = 0; i < exporters.size(); i++) {
            JmeExporter exporter = exporters.get(i);
            outs[i] = new ByteArrayOutputStream();
            try {
                exporter.save(origin, outs[i]);
            } catch (IOException ex) {
                Assert.fail(ex.getMessage());
            }
        }

        // print
        if (printXML) {
            for (int i = 0; i < exporters.size(); i++) {
                ByteArrayOutputStream out = outs[i];
                if (exporters.get(i) instanceof XMLExporter) {
                    System.out.println("XML: \n" + new String(out.toByteArray()) + "\n\n");
                } else if (exporters.get(i) instanceof BinaryExporter) {
                    System.out.println("Binary: " + out.size() + " bytes");
                } else {
                    System.out.println("Unknown exporter: " + exporters.get(i).getClass().getName());
                }
            }
        }

        // import
        Node nodes[] = new Node[importers.size() + 1];
        nodes[0] = origin;
        for (int i = 0; i < importers.size(); i++) {
            JmeImporter importer = importers.get(i);
            ByteArrayOutputStream out = outs[i];
            try {
                AssetInfo info = new AssetInfo(assetManager, new ModelKey("origin")) {
                    @Override
                    public InputStream openStream() {
                        return new ByteArrayInputStream(out.toByteArray());
                    }
                };
                nodes[i + 1] = (Node) importer.load(info);
            } catch (IOException ex) {
                Assert.fail(ex.getMessage());
            }
        }

        // compare
        Map<String, Object> userData[] = new Map[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            Node n = nodes[i];
            userData[i] = new HashMap<String, Object>();
            for (String k : n.getUserDataKeys()) {
                userData[i].put(k, n.getUserData(k));
            }
        }
        compareMaps(userData);
    }

    private static final void compareMaps(Map<String, Object>[] maps) {
        String[] keys = maps[0].keySet().toArray(new String[0]);
        // check if all maps have the same keys and values for those keys
        for (int i = 1; i < maps.length; i++) {
            Map<String, Object> map = maps[i];
            Assert.assertEquals("Map " + i + " keys do not match", keys.length, map.size());
            for (String key : keys) {
                Assert.assertTrue("Missing key " + key + " in map " + i, map.containsKey(key));
                Object v1 = maps[0].get(key);
                Object v2 = map.get(key);
                if (v1.getClass().isArray()) {
                    boolean c = Arrays.equals((Object[]) v1, (Object[]) v2);
                    if (c) System.out.println(key + " match");
                    Assert.assertTrue("Value does not match in map " + i + " for key " + key + " expected "
                            + Arrays.deepToString((Object[]) v1) + " but got "
                            + Arrays.deepToString((Object[]) v2), c);
                } else {
                    boolean c = v1.equals(v2);
                    if (c) System.out.println(key + " match");
                    Assert.assertTrue("Value does not match in map " + i + " for key " + key + " expected "
                            + v1 + " but got " + v2, c);
                }
            }
        }

    }
}
