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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.xml.XMLExporter;
import com.jme3.material.Material;
import com.jme3.material.plugin.export.material.J3MExporter;

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
}
