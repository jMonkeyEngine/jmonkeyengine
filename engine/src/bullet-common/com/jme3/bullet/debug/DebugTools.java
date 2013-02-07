/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.bullet.debug;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.Arrow;

/**
 *
 * @author normenhansen
 */
public class DebugTools {

    protected final AssetManager manager;
    public Material DEBUG_BLUE;
    public Material DEBUG_RED;
    public Material DEBUG_GREEN;
    public Material DEBUG_YELLOW;
    public Material DEBUG_MAGENTA;
    public Material DEBUG_PINK;
    public Node debugNode = new Node("Debug Node");
    public Arrow arrowBlue = new Arrow(Vector3f.ZERO);
    public Geometry arrowBlueGeom = new Geometry("Blue Arrow", arrowBlue);
    public Arrow arrowGreen = new Arrow(Vector3f.ZERO);
    public Geometry arrowGreenGeom = new Geometry("Green Arrow", arrowGreen);
    public Arrow arrowRed = new Arrow(Vector3f.ZERO);
    public Geometry arrowRedGeom = new Geometry("Red Arrow", arrowRed);
    public Arrow arrowMagenta = new Arrow(Vector3f.ZERO);
    public Geometry arrowMagentaGeom = new Geometry("Magenta Arrow", arrowMagenta);
    public Arrow arrowYellow = new Arrow(Vector3f.ZERO);
    public Geometry arrowYellowGeom = new Geometry("Yellow Arrow", arrowYellow);
    public Arrow arrowPink = new Arrow(Vector3f.ZERO);
    public Geometry arrowPinkGeom = new Geometry("Pink Arrow", arrowPink);
    protected static final Vector3f UNIT_X_CHECK = new Vector3f(1, 0, 0);
    protected static final Vector3f UNIT_Y_CHECK = new Vector3f(0, 1, 0);
    protected static final Vector3f UNIT_Z_CHECK = new Vector3f(0, 0, 1);
    protected static final Vector3f UNIT_XYZ_CHECK = new Vector3f(1, 1, 1);
    protected static final Vector3f ZERO_CHECK = new Vector3f(0, 0, 0);

    public DebugTools(AssetManager manager) {
        this.manager = manager;
        setupMaterials();
        setupDebugNode();
    }

    public void show(RenderManager rm, ViewPort vp) {
        if (!Vector3f.UNIT_X.equals(UNIT_X_CHECK) || !Vector3f.UNIT_Y.equals(UNIT_Y_CHECK) || !Vector3f.UNIT_Z.equals(UNIT_Z_CHECK)
                || !Vector3f.UNIT_XYZ.equals(UNIT_XYZ_CHECK) || !Vector3f.ZERO.equals(ZERO_CHECK)) {
            throw new IllegalStateException("Unit vectors compromised!"
                    + "\nX: " + Vector3f.UNIT_X
                    + "\nY: " + Vector3f.UNIT_Y
                    + "\nZ: " + Vector3f.UNIT_Z
                    + "\nXYZ: " + Vector3f.UNIT_XYZ
                    + "\nZERO: " + Vector3f.ZERO);
        }
        debugNode.updateLogicalState(0);
        debugNode.updateGeometricState();
        rm.renderScene(debugNode, vp);
    }

    public void setBlueArrow(Vector3f location, Vector3f extent) {
        arrowBlueGeom.setLocalTranslation(location);
        arrowBlue.setArrowExtent(extent);
    }

    public void setGreenArrow(Vector3f location, Vector3f extent) {
        arrowGreenGeom.setLocalTranslation(location);
        arrowGreen.setArrowExtent(extent);
    }

    public void setRedArrow(Vector3f location, Vector3f extent) {
        arrowRedGeom.setLocalTranslation(location);
        arrowRed.setArrowExtent(extent);
    }

    public void setMagentaArrow(Vector3f location, Vector3f extent) {
        arrowMagentaGeom.setLocalTranslation(location);
        arrowMagenta.setArrowExtent(extent);
    }

    public void setYellowArrow(Vector3f location, Vector3f extent) {
        arrowYellowGeom.setLocalTranslation(location);
        arrowYellow.setArrowExtent(extent);
    }

    public void setPinkArrow(Vector3f location, Vector3f extent) {
        arrowPinkGeom.setLocalTranslation(location);
        arrowPink.setArrowExtent(extent);
    }

    protected void setupDebugNode() {
        arrowBlueGeom.setMaterial(DEBUG_BLUE);
        arrowGreenGeom.setMaterial(DEBUG_GREEN);
        arrowRedGeom.setMaterial(DEBUG_RED);
        arrowMagentaGeom.setMaterial(DEBUG_MAGENTA);
        arrowYellowGeom.setMaterial(DEBUG_YELLOW);
        arrowPinkGeom.setMaterial(DEBUG_PINK);
        debugNode.attachChild(arrowBlueGeom);
        debugNode.attachChild(arrowGreenGeom);
        debugNode.attachChild(arrowRedGeom);
        debugNode.attachChild(arrowMagentaGeom);
        debugNode.attachChild(arrowYellowGeom);
        debugNode.attachChild(arrowPinkGeom);
    }

    protected void setupMaterials() {
        DEBUG_BLUE = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        DEBUG_BLUE.getAdditionalRenderState().setWireframe(true);
        DEBUG_BLUE.setColor("Color", ColorRGBA.Blue);
        DEBUG_GREEN = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        DEBUG_GREEN.getAdditionalRenderState().setWireframe(true);
        DEBUG_GREEN.setColor("Color", ColorRGBA.Green);
        DEBUG_RED = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        DEBUG_RED.getAdditionalRenderState().setWireframe(true);
        DEBUG_RED.setColor("Color", ColorRGBA.Red);
        DEBUG_YELLOW = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        DEBUG_YELLOW.getAdditionalRenderState().setWireframe(true);
        DEBUG_YELLOW.setColor("Color", ColorRGBA.Yellow);
        DEBUG_MAGENTA = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        DEBUG_MAGENTA.getAdditionalRenderState().setWireframe(true);
        DEBUG_MAGENTA.setColor("Color", ColorRGBA.Magenta);
        DEBUG_PINK = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        DEBUG_PINK.getAdditionalRenderState().setWireframe(true);
        DEBUG_PINK.setColor("Color", ColorRGBA.Pink);
    }
}
