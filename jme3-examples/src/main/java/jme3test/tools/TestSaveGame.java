/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package jme3test.tools;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import jme3tools.savegame.SaveGame;

public class TestSaveGame extends SimpleApplication {

    public static void main(String[] args) {

        TestSaveGame app = new TestSaveGame();
        app.start();
    }

    @Override
    public void simpleUpdate(float tpf) {
    }

    @Override
    public void simpleInitApp() {

        // Create a Node to store player data.
        Node myPlayer = new Node();
        myPlayer.setName("PlayerNode");
        myPlayer.setUserData("name", "Mario");
        myPlayer.setUserData("health", 100.0f);
        myPlayer.setUserData("points", 0);

        // Attach the model to the Node.
        Spatial model = assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        myPlayer.attachChild(model);

        // Before saving the game, detach the model since it doesn't need to be saved.
        myPlayer.detachAllChildren();
        SaveGame.saveGame("mycompany/mygame", "savegame_001", myPlayer);

        // Later, the game is loaded again ...
        Node player = (Node) SaveGame.loadGame("mycompany/mygame", "savegame_001");
        player.attachChild(model);
        rootNode.attachChild(player);

        // and the player data are available.
        System.out.println("Name: " + player.getUserData("name"));
        System.out.println("Health: " + player.getUserData("health"));
        System.out.println("Points: " + player.getUserData("points"));

        AmbientLight al = new AmbientLight();
        rootNode.addLight(al);
        
        // Note that your custom classes can also implement the Savable interface.
    }
}
