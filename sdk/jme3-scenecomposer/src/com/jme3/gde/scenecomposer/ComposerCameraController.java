/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.scenecomposer;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.controller.AbstractCameraController;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

/**
 *
 * @author normenhansen
 */
public class ComposerCameraController extends AbstractCameraController {

    private Node rootNode;
    private JmeNode jmeRootNode;

    public ComposerCameraController(Camera cam, JmeNode rootNode) {
        super(cam, SceneApplication.getApplication().getInputManager());
        this.jmeRootNode = rootNode;
        this.rootNode = rootNode.getLookup().lookup(Node.class);
    }

    public void checkClick(int button) {
        if (button == 0) {
            CollisionResults results = new CollisionResults();
            Ray ray = new Ray();
            Vector3f pos = cam.getWorldCoordinates(new Vector2f(mouseX, mouseY), 0).clone();
            Vector3f dir = cam.getWorldCoordinates(new Vector2f(mouseX, mouseY), 0.3f).clone();
            dir.subtractLocal(pos).normalizeLocal();
            ray.setOrigin(pos);
            ray.setDirection(dir);
            rootNode.collideWith(ray, results);
            if (results == null) {
                return;
            }
            final CollisionResult result = results.getClosestCollision();
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    if (result != null && result.getGeometry() != null) {
                        SceneApplication.getApplication().setSelectedNode(jmeRootNode.getChild(result.getGeometry()));
                    } else {
                        SceneApplication.getApplication().setSelectedNode(jmeRootNode);
                    }
                }
            });
            checkClick = false;
        }
        if (button == 1) {
            CollisionResults results = new CollisionResults();
            Ray ray = new Ray();
            Vector3f pos = cam.getWorldCoordinates(new Vector2f(mouseX, mouseY), 0).clone();
            Vector3f dir = cam.getWorldCoordinates(new Vector2f(mouseX, mouseY), 0.3f).clone();
            dir.subtractLocal(pos).normalizeLocal();
            ray.setOrigin(pos);
            ray.setDirection(dir);
            rootNode.collideWith(ray, results);
            if (results == null) {
                return;
            }
            CollisionResult result = results.getClosestCollision();
            if (result != null) {
                ((SceneComposerTopComponent) master).doMoveCursor(result.getContactPoint());//getGeometry().getWorldTranslation().add(result.getGeometry().getWorldRotation().mult(result.getContactPoint())));
            }
            checkClickR = false;
        }
    }
}
