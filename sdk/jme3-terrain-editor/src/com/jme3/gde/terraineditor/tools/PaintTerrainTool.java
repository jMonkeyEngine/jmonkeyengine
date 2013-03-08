/*
 * Copyright (c) 2009-2011 jMonkeyEngine
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
package com.jme3.gde.terraineditor.tools;

import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AbstractStatefulGLToolAction;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.core.undoredo.SceneUndoRedoManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.terrain.Terrain;
import java.util.ArrayList;
import java.util.List;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 * Paint or erase the textures on the terrain.
 * 
 * @author Brent Owens
 */
public class PaintTerrainTool extends TerrainTool {

    private boolean painting = false; // to check when undo actions need to be set
    List<PaintTerrainToolAction> actions = new ArrayList<PaintTerrainToolAction>();

    
    @Override
    public void actionPrimary(Vector3f point, int textureIndex, AbstractSceneExplorerNode rootNode, DataObject dataObject) {
        setPrimary(true);
        action(point, textureIndex, rootNode, dataObject);
    }

    @Override
    public void actionSecondary(Vector3f point, int textureIndex, AbstractSceneExplorerNode rootNode, DataObject dataObject) {
        setPrimary(false);
        action(point, textureIndex, rootNode, dataObject);
    }
    
    private void action(Vector3f point, int textureIndex, AbstractSceneExplorerNode rootNode, DataObject dataObject) {
        if (radius == 0 || weight == 0)
            return;
        
        if (!painting)
            painting = true;
        
        PaintTerrainToolAction action;
        if (isPrimary())
            action = new PaintTerrainToolAction(point, radius, weight, textureIndex);
        else
            action = new PaintTerrainToolAction(point, radius, -weight, textureIndex);
        action.doActionPerformed(rootNode, dataObject, false);
        actions.add(action);
    }
    
    @Override
    public void actionEnded(AbstractSceneExplorerNode rootNode, DataObject dataObject) {
        if (painting) {
            painting = false;
            
            if (actions.isEmpty())
                return;
            
            // record undo action
            List<PaintTerrainToolAction> cloned = new ArrayList<PaintTerrainToolAction>();
            cloned.addAll(actions);
            recordUndo(cloned, rootNode, dataObject);
            actions.clear();
        }
    }

    /**
     * Is it already painting?
     * If the user releases the mouse outside the window, this can be used to check
     * if they were painting, and if so, call actionEnded()
     */
    public boolean isPainting() {
        return painting;
    }
    
    @Override
    public void addMarkerPrimary(Node parent) {
        super.addMarkerPrimary(parent);
        markerPrimary.getMaterial().setColor("Color", ColorRGBA.Cyan);
    }

    private void recordUndo(final List<PaintTerrainToolAction> actions, final AbstractSceneExplorerNode rootNode, final DataObject dataObject) {
        Lookup lookup = Lookup.getDefault() ;
        SceneUndoRedoManager manager = lookup.lookup(SceneUndoRedoManager.class);

        AbstractUndoableSceneEdit undoer = new AbstractUndoableSceneEdit() {

            @Override
            public void sceneUndo() throws CannotUndoException {
                Terrain terrain = null;
                for (int i=actions.size()-1; i>=0; i--) {
                    PaintTerrainToolAction a = actions.get(i);
                    if (terrain == null) 
                        terrain = a.getTerrain(rootNode.getLookup().lookup(Node.class));
                    a.doUndoTool(rootNode, terrain);
                }
                setModified(rootNode, dataObject);
            }

            @Override
            public void sceneRedo() throws CannotRedoException {
                for (int i=0; i<actions.size(); i++) {
                    PaintTerrainToolAction a = actions.get(i);
                    a.applyTool(rootNode);
                }
                setModified(rootNode, dataObject);
            }

        };
        if (manager != null) // this is a temporary check, it should never be null but occasionally is
            manager.addEdit(this, undoer);
    }
    
    protected void setModified(final AbstractSceneExplorerNode rootNode, final DataObject dataObject) {
        if (dataObject.isModified())
            return;
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                dataObject.setModified(true);
                rootNode.refresh(true);
            }
        });
    }
}
