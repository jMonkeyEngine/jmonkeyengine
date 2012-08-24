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

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapText;
import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.terraineditor.ExtraToolParams;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import org.openide.loaders.DataObject;

/**
 *
 * @author Shirkit
 */
public class SlopeTerrainTool extends TerrainTool {

    private Vector3f point1, point2;
    private Geometry markerThird, line;
    private Node parent;
    private SlopeExtraToolParams toolParams;
    private BitmapText angleText;
    private boolean leftCtrl = false;

    public SlopeTerrainTool() {
        toolHintTextKey = "TerrainEditorTopComponent.toolHint.slope";
    }

    @Override
    protected boolean useStraightLine() {
        return true;
    }
    
    @Override
    public void activate(AssetManager manager, Node parent) {
        super.activate(manager, parent);
        addMarkerSecondary(parent);
        addMarkerThird(parent);
        addLineAndText();
        this.parent = parent;
    }

    @Override
    public void hideMarkers() {
        super.hideMarkers();
        if (markerThird != null)
            markerThird.removeFromParent();

        line.removeFromParent();
        angleText.removeFromParent();
    }

    private void addMarkerThird(Node parent) {
        if (markerThird == null) {
            markerThird = new Geometry("edit marker secondary");
            Mesh m2 = new Sphere(8, 8, 0.5f);
            markerThird.setMesh(m2);
            Material mat2 = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat2.getAdditionalRenderState().setWireframe(false);
            markerThird.setMaterial(mat2);
            markerThird.setLocalTranslation(0, 0, 0);
            mat2.setColor("Color", ColorRGBA.Blue);
        }
        parent.attachChild(markerThird);
    }

    private void addLineAndText() {
        line = new Geometry("line", new Line(Vector3f.ZERO, Vector3f.ZERO));
        Material m = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", ColorRGBA.White);
        line.setMaterial(m);

        angleText = new BitmapText(manager.loadFont("Interface/Fonts/Default.fnt"));
        BillboardControl control = new BillboardControl();
        angleText.addControl(control);
        angleText.setSize(0.5f);
        angleText.setCullHint(Spatial.CullHint.Never);
    }

    @Override
    public void actionPrimary(Vector3f point, int textureIndex, AbstractSceneExplorerNode rootNode, DataObject dataObject) {
        if (point1 != null && point2 != null && point1.distance(point2) > 0.01f) { // Preventing unexpected behavior, like destroying the terrain
            SlopeTerrainToolAction action = new SlopeTerrainToolAction(point, point1, point2, radius, weight, toolParams.precision, toolParams.lock, getMesh());
            action.actionPerformed(rootNode, dataObject);
        }
    }

    @Override
    public void keyPressed(KeyInputEvent kie) {
        super.keyPressed(kie);
        switch (kie.getKeyCode()) {
            case KeyInput.KEY_LCONTROL:
                leftCtrl = kie.isPressed();
                break;
            case KeyInput.KEY_C:
                point1 = null;
                point2 = null;
                markerSecondary.removeFromParent();
                markerThird.removeFromParent();
                line.removeFromParent();
                angleText.removeFromParent();
                break;
            case KeyInput.KEY_UP:
                markerThird.move(0f, 0.1f, 0f);
                point2.set(markerThird.getLocalTranslation());
                updateAngle();
                break;
            case KeyInput.KEY_DOWN:
                markerThird.move(0f, -0.1f, 0f);
                point2.set(markerThird.getLocalTranslation());
                updateAngle();
                break;
        }
    }

    private void updateAngle() {
        Vector3f temp, higher, lower;
        if (point2.y > point1.y) {
            temp = point2;
            higher = point2;
            lower = point1;
        } else {
            temp = point1;
            higher = point1;
            lower = point2;
        }
        temp = temp.clone().setY(lower.y);

        float angle = ((FastMath.asin(temp.distance(higher) / lower.distance(higher))) * FastMath.RAD_TO_DEG);

        angleText.setText(angle + " degrees");
        angleText.setLocalTranslation(new Vector3f().interpolate(point1, point2, 0.5f));

        if (line.getParent() == null) {
            parent.attachChild(line);
            parent.attachChild(angleText);
        }
        ((Line) line.getMesh()).updatePoints(point1, point2);
    }

    @Override
    public void actionSecondary(Vector3f point, int textureIndex, AbstractSceneExplorerNode rootNode, DataObject dataObject) {
        if (leftCtrl) {
            point2 = point;
            if (markerThird.getParent() == null)
                parent.attachChild(markerThird);

            markerThird.setLocalTranslation(point);
        } else {
            point1 = point;
            if (markerSecondary.getParent() == null)
                parent.attachChild(markerSecondary);

            markerSecondary.setLocalTranslation(point);
        }
        if (point1 != null && point2 != null)
            updateAngle();
        else
            if (line != null)
                line.removeFromParent();
    }

    @Override
    public void setExtraParams(ExtraToolParams params) {
        if (params instanceof SlopeExtraToolParams)
            this.toolParams = (SlopeExtraToolParams) params;
    }
}
