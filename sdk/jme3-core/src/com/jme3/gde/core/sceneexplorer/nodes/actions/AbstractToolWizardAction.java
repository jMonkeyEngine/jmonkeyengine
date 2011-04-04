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

import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.core.undoredo.SceneUndoRedoManager;
import java.awt.event.ActionEvent;
import java.util.concurrent.Callable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 *
 * @author normenhansen
 */
public abstract class AbstractToolWizardAction implements ToolAction {

    protected String name = "*";

    protected Action makeAction(final AbstractSceneExplorerNode rootNode, final DataObject dataObject) {
        return new AbstractAction(name) {

            public void actionPerformed(ActionEvent e) {
                final Object settings = showWizard(rootNode);
                SceneApplication.getApplication().enqueue(new Callable<Void>() {

                    public Void call() throws Exception {
                        final Object object = doApplyTool(rootNode, settings);
                        if (object != null) {
                            Lookup.getDefault().lookup(SceneUndoRedoManager.class).addEdit(this, new AbstractUndoableSceneEdit() {

                                @Override
                                public void sceneUndo() throws CannotUndoException {
                                    doUndoTool(rootNode, object);
                                    setModified();
                                }

                                @Override
                                public void sceneRedo() throws CannotRedoException {
                                    doApplyTool(rootNode, settings);
                                    setModified();
                                }

                                @Override
                                public void awtRedo() {
                                    dataObject.setModified(true);
                                    rootNode.refresh(true);
                                }

                                @Override
                                public void awtUndo() {
                                    dataObject.setModified(true);
                                    rootNode.refresh(true);
                                }
                            });
                            setModified();
                        }
                        return null;
                    }
                });
            }

            protected void setModified() {
                java.awt.EventQueue.invokeLater(new Runnable() {

                    public void run() {
                        dataObject.setModified(true);
                        rootNode.refresh(true);
                    }
                });
            }
        };
    }

    protected abstract Object showWizard(org.openide.nodes.Node node);

    protected abstract Object doApplyTool(AbstractSceneExplorerNode rootNode, Object settings);

    protected abstract void doUndoTool(AbstractSceneExplorerNode rootNode, Object undoObject);

//    protected abstract void doAwtUndo();
//    protected abstract void doAwtRedo();
    public Action getAction(AbstractSceneExplorerNode rootNode, DataObject dataObject) {
        return makeAction(rootNode, dataObject);
    }
}
