/*
 * Copyright (c) 2009-2017 jMonkeyEngine
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
package jme3test.collision;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import java.util.logging.Logger;

/**
 * Test for mesh collision with Line-mode meshes. Press the R key to cast a ray.
 * The only collision result should be from the red square. If a collision with
 * the white lines is reported, that's issue #710.
 *
 * @author Stephen Gold
 */
public class CollideWithLineModeMesh extends SimpleApplication {

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            CollideWithLineModeMesh.class.getName());

    /**
     * ray in the -Z direction, starting from (0.1, 0.2, 10)
     */
    final private Ray ray = new Ray(/* origin */new Vector3f(0.1f, 0.2f, 10f),
            /* direction */ new Vector3f(0f, 0f, -1f));

    public static void main(String[] args) {
        CollideWithLineModeMesh app = new CollideWithLineModeMesh();
        app.setPauseOnLostFocus(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        createRedSquare();
        createWhiteLines();
        /*
         * Invoke castRay() each time the user presses the R.
         */
        inputManager.addMapping("cast ray", new KeyTrigger(KeyInput.KEY_R));
        ActionListener listen = new ActionListener() {
            @Override
            public void onAction(String action, boolean keyPressed, float tpf) {
                if (action.equals("cast ray") && keyPressed) {
                    castRay();
                }
            }
        };
        inputManager.addListener(listen, "cast ray");
        /*
         * Faster movement of flyCam.
         */
        flyCam.setMoveSpeed(9f);
    }

    /**
     * Cast a ray at the geometries and report all collisions.
     */
    void castRay() {
        CollisionResults results = new CollisionResults();
        rootNode.collideWith(ray, results);
        int numResults = results.size();
        System.out.printf("%n%d collision result%s:%n", numResults,
                numResults == 1 ? "" : "s");
        for (int resultI = 0; resultI < numResults; resultI++) {
            CollisionResult result = results.getCollision(resultI);
            Geometry geometry = result.getGeometry();
            Vector3f location = result.getContactPoint();
            String name = geometry.getName();
            System.out.printf(" [%d] with %s at %s%n", resultI,
                    name, location.toString());
        }
    }

    /**
     * Attach a red square with its lower left corner at (0, 0, 0). It is
     * composed of 2 triangles.
     */
    void createRedSquare() {
        Mesh quadMesh = new Quad(1f, 1f);
        Geometry redSquare = new Geometry("red square", quadMesh);
        Material red = assetManager.loadMaterial("Common/Materials/RedColor.j3m");
        redSquare.setMaterial(red);
        rootNode.attachChild(redSquare);
    }

    /**
     * Attach a pair of parallel white lines in the z=1 plane.
     */
    void createWhiteLines() {
        Mesh lineMesh = new Mesh();
        lineMesh.setMode(Mesh.Mode.Lines);
        float[] corners = new float[]{
            -1f, -1f, 0f,
            -1f, 1f, 0f,
            1f, 1f, 0f,
            1f, -1f, 0f
        };
        lineMesh.setBuffer(VertexBuffer.Type.Position, 3, corners);
        short[] indices = new short[]{0, 1, 2, 3};
        lineMesh.setBuffer(VertexBuffer.Type.Index, 2, indices);
        lineMesh.updateBound();
        Geometry whiteLines = new Geometry("white lines", lineMesh);
        Material white = assetManager.loadMaterial("Common/Materials/WhiteColor.j3m");
        whiteLines.setMaterial(white);
        whiteLines.move(0f, 0f, 1f);
        rootNode.attachChild(whiteLines);
    }
}
