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
package com.jme3.gde.core.sceneexplorer.nodes.actions;

import com.jme3.audio.AudioNode;
import com.jme3.effect.EmitterSphereShape;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.core.undoredo.SceneUndoRedoManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.event.ActionEvent;
import java.util.concurrent.Callable;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;

/**
 *
 * @author normenhansen
 */
public class AddSpatialAction extends AbstractAction implements Presenter.Popup {

    protected JmeNode jmeNode;
    protected Node node;
    protected DataObject dataObject;

    public AddSpatialAction(JmeNode node) {
        this.jmeNode = node;
        this.node = node.getLookup().lookup(Node.class);
        this.dataObject = node.getLookup().lookup(DataObject.class);
    }

    public void actionPerformed(ActionEvent e) {
    }

    public JMenuItem getPopupPresenter() {
        JMenu result = new JMenu("Add Spatial..");
        result.add(new JMenuItem(new AddNodeAction()));
        result.add(new JMenuItem(new AddEmitterAction()));
        result.add(new JMenuItem(new AddAudioNodeAction()));
        for (NewSpatialAction di : Lookup.getDefault().lookupAll(NewSpatialAction.class)) {
            result.add(new JMenuItem(di.getAction(jmeNode, dataObject)));
        }
        return result;
    }

    private class AddNodeAction extends AbstractAction {

        public AddNodeAction() {
            putValue(NAME, "Node");
        }

        public void actionPerformed(ActionEvent e) {
            SceneApplication.getApplication().enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    Node newItem = new Node("Node");
                    node.attachChild(newItem);
                    addSpatialUndo(node, newItem);
                    setModified();
                    return null;
                }
            });
        }
    }

    private class AddEmitterAction extends AbstractAction {

        public AddEmitterAction() {
            putValue(NAME, "Particle Emitter");
        }

        public void actionPerformed(ActionEvent e) {
            SceneApplication.getApplication().enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    ParticleEmitter emit = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 200);
                    emit.setShape(new EmitterSphereShape(Vector3f.ZERO, 1f));
                    emit.setGravity(0);
                    emit.setLowLife(5);
                    emit.setHighLife(10);
                    emit.setInitialVelocity(new Vector3f(0, 0, 0));
                    emit.setImagesX(15);
                    Material mat = new Material(SceneApplication.getApplication().getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");
                    emit.setMaterial(mat);
                    node.attachChild(emit);
                    addSpatialUndo(node, emit);
                    setModified();
                    return null;
                }
            });
        }
    }

    private class AddAudioNodeAction extends AbstractAction {

        public AddAudioNodeAction() {
            putValue(NAME, "Audio Node");
        }

        public void actionPerformed(ActionEvent e) {
            SceneApplication.getApplication().enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    AudioNode newItem = new AudioNode();
                    newItem.setName("AudioNode");
                    node.attachChild(newItem);
                    addSpatialUndo(node, newItem);
                    setModified();
                    return null;
                }
            });
        }
    }

    private void addSpatialUndo(final Node undoParent, final Spatial undoSpatial) {
        //add undo
        if (undoParent != null && undoSpatial != null) {
            Lookup.getDefault().lookup(SceneUndoRedoManager.class).addEdit(this, new AbstractUndoableSceneEdit() {

                @Override
                public void sceneUndo() throws CannotUndoException {
                    undoSpatial.removeFromParent();
                }

                @Override
                public void sceneRedo() throws CannotRedoException {
                    undoParent.attachChild(undoSpatial);
                }

                @Override
                public void awtRedo() {
                    dataObject.setModified(true);
                    jmeNode.refresh(true);
                }

                @Override
                public void awtUndo() {
                    dataObject.setModified(true);
                    jmeNode.refresh(true);
                }
            });
        }
    }

    private void setModified() {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                dataObject.setModified(true);
                jmeNode.refresh(true);
            }
        });
    }
}
