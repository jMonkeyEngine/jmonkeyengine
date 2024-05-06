/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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

package jme3test.scene;

import com.jme3.app.SimpleApplication;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestUserData extends SimpleApplication {    
    public static final String HOME_PATH = System.getProperty("user.home");
    
    public static enum TestEnum {
        OPTION1, OPTION2, OPTION3;
    }

    public static void main(String[] args) {
        TestUserData app = new TestUserData();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        Node scene = (Node) assetManager.loadModel("Scenes/DotScene/DotScene.scene");
        System.out.println("Scene: " + scene);

        Spatial testNode = scene.getChild("TestNode");
        debugDataUser(testNode);
       
        otherUserData();
    }
    
    private void debugDataUser(Spatial spatial) {
        System.out.println("[ UserData ] :Object user data >> " + spatial);
        for (String key : spatial.getUserDataKeys()){
            System.out.println("  * Property " + key + " = " + spatial.getUserData(key));
        }
    }
    
    private void otherUserData() {
        Node emptyNode = new Node("Empty");
        
        emptyNode.setUserData("Options", TestEnum.OPTION1);
        
        List<Object> data = new ArrayList<>();
        data.add(TestEnum.OPTION2);
        data.add(TestEnum.OPTION3);
        
        emptyNode.setUserData("List<?>", data);
        exportAndImport(emptyNode);
    }
    
    public void exportAndImport(Spatial spatial) {
        try {
            BinaryExporter exporter = BinaryExporter.getInstance();
            exporter.save(spatial, new File(HOME_PATH, "UserData.j3o"));
            
            System.out.println("\n");
            System.out.println("[ ok ] Export '" + spatial + "' > " + HOME_PATH + File.separator + "UserData.j3o");
            debugDataUser(spatial);
            
            BinaryImporter importer = BinaryImporter.getInstance();
            Spatial s = (Spatial) importer.load(new File(HOME_PATH, "UserData.j3o"));
            
            System.out.println();
            System.out.println("[ ok ] Import '" + spatial + "' > " + HOME_PATH + File.separatorChar + "UserData.j3o");
            debugDataUser(s);
        } catch (IOException e) {
            System.err.println("[ err ] " + e.getMessage());
        }
    }
}
