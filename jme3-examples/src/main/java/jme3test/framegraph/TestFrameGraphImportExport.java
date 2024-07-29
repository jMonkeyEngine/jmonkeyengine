/*
 * Copyright (c) 2024 jMonkeyEngine
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
package jme3test.framegraph;

import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.export.xml.XMLExporter;
import com.jme3.export.xml.XMLImporter;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.FrameGraphFactory;
import com.jme3.system.AppSettings;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests framegraph export and import with xml and binary formats.
 * <p>
 * The test succeeds if both export and import operations for both xml and binary
 * formats functions without error. For importing, the resulting framegraph must
 * show up on the detailed profiler gui.
 * <p>
 * The test fails if any errors occur, or the framegraph does not show up on the
 * detailed profiler gui after importing.
 * <p>
 * The test application closes after exporting.
 * 
 * @author codex
 */
public class TestFrameGraphImportExport extends SimpleApplication {
    
    private final String path = System.getProperty("user.home")+"/myGraph";
    private final boolean export = true;
    private final boolean xml = false;
    
    public static void main(String[] args) {
        TestFrameGraphImportExport app = new TestFrameGraphImportExport();
        AppSettings as = new AppSettings(true);
        as.setWidth(768);
        as.setHeight(768);
        app.setSettings(as);
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        
        final File file = new File(path+"."+(xml ? "xml" : "j3g"));
        
        if (export) {
            FrameGraph graph = FrameGraphFactory.forward(assetManager);
            try {
                if (xml) {
                    XMLExporter.getInstance().save(graph.createModuleData(), file);
                } else {
                    BinaryExporter.getInstance().save(graph.createModuleData(), file);
                }
            } catch (IOException ex) {
                Logger.getLogger(TestFrameGraphImportExport.class.getName()).log(Level.SEVERE, null, ex);
            }
            stop();
        } else {
            stateManager.attach(new DetailedProfilerState());
            flyCam.setDragToRotate(true);
            try {
                FrameGraph graph = new FrameGraph(assetManager);
                if (xml) {
                    graph.applyData(XMLImporter.getInstance().load(file));
                } else {
                    graph.applyData(BinaryImporter.getInstance().load(file));
                }
                viewPort.setFrameGraph(graph);
            } catch (IOException ex) {
                Logger.getLogger(TestFrameGraphImportExport.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
}
