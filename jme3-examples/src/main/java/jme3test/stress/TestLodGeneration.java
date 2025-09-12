/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package jme3test.stress;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.jme3.anim.SkinningControl;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;

import jme3tools.optimize.LodGenerator;

public class TestLodGeneration extends SimpleApplication implements ActionListener {

    public static void main(String[] args) {
        TestLodGeneration app = new TestLodGeneration();
        app.start();
    }

    private boolean wireframe = false;
    // Current reduction value for LOD generation (0.0 to 1.0)
    private float reductionValue = 0.0f;
    private int lodLevel = 0;
    private BitmapText hudText;
    private final List<Geometry> listGeoms = new ArrayList<>();
    private final ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(5);

    @Override
    public void simpleInitApp() {

        // --- Lighting Setup ---
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(dl);

        AmbientLight al = new AmbientLight();
        rootNode.addLight(al);

        // --- Model Loading and Setup ---
        // model = (Node) assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
        Node model = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        BoundingBox b = ((BoundingBox) model.getWorldBound());
        model.setLocalScale(1.2f / (b.getYExtent() * 2));
        // model.setLocalTranslation(0,-(b.getCenter().y - b.getYExtent())* model.getLocalScale().y, 0);

        // Iterate through the model's children and collect all Geometry objects
        for (Spatial spatial : model.getChildren()) {
            if (spatial instanceof Geometry) {
                listGeoms.add((Geometry) spatial);
            }
        }

        // --- Camera Setup ---
        ChaseCamera chaseCam = new ChaseCamera(cam, model, inputManager);
        chaseCam.setLookAtOffset(b.getCenter());
        chaseCam.setDefaultDistance(5);
        chaseCam.setMinVerticalRotation(-FastMath.HALF_PI + 0.01f);
        chaseCam.setZoomSensitivity(0.5f);

        SkinningControl skControl = model.getControl(SkinningControl.class);
        if (skControl != null) {
            // Disable skinning control if found. This is an optimization for static LOD generation
            // as skinning computation is not needed when generating LODs.
            skControl.setEnabled(false);
        }

        // --- Initial LOD Generation ---
        // Set initial reduction value and LOD level
        reductionValue = 0.80f;
        lodLevel = 1;

        // Generate LODs for each geometry in the model
        for (final Geometry geom : listGeoms) {
            LodGenerator lodGenerator = new LodGenerator(geom);
            lodGenerator.bakeLods(LodGenerator.TriangleReductionMethod.PROPORTIONAL, reductionValue);
            geom.setLodLevel(lodLevel);
        }

        rootNode.attachChild(model);
        // Disable the default fly camera as we are using a chase camera
        flyCam.setEnabled(false);

        // --- HUD Setup ---
        hudText = new BitmapText(guiFont);
        hudText.setText(computeNbTri() + " tris");
        hudText.setLocalTranslation(cam.getWidth() / 2f, hudText.getLineHeight(), 0);
        guiNode.attachChild(hudText);

        // Register input mappings for user interaction
        registerInputMappings();
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) return;

        if (name.equals("plus")) {
            reductionValue += 0.05f;
            updateLod();

        } else if (name.equals("minus")) {
            reductionValue -= 0.05f;
            updateLod();

        } else if (name.equals("wireframe")) {
            wireframe = !wireframe;
            for (Geometry geom : listGeoms) {
                Material mat = geom.getMaterial();
                mat.getAdditionalRenderState().setWireframe(wireframe);
            }
        }
    }

    private void registerInputMappings() {
        addMapping("plus", new KeyTrigger(KeyInput.KEY_P));
        addMapping("minus", new KeyTrigger(KeyInput.KEY_L));
        addMapping("wireframe", new KeyTrigger(KeyInput.KEY_SPACE));
    }

    private void addMapping(String mappingName, Trigger... triggers) {
        inputManager.addMapping(mappingName, triggers);
        inputManager.addListener(this, mappingName);
    }

    @Override
    public void destroy() {
        super.destroy();
        exec.shutdown();
    }

    private void updateLod() {
        // Clamp the reduction value between 0.0 and 1.0 to ensure it's within valid range
        reductionValue = FastMath.clamp(reductionValue, 0.0f, 1.0f);
        makeLod(LodGenerator.TriangleReductionMethod.PROPORTIONAL, reductionValue, 1);
    }

    /**
     * Computes the total number of triangles currently displayed by all geometries.
     * @return The total number of triangles.
     */
    private int computeNbTri() {
        int nbTri = 0;
        for (Geometry geom : listGeoms) {
            Mesh mesh = geom.getMesh();
            // Check if the mesh has LOD levels
            if (mesh.getNumLodLevels() > 0) {
                nbTri += mesh.getLodLevel(lodLevel).getNumElements();
            } else {
                nbTri += mesh.getTriangleCount();
            }
        }
        return nbTri;
    }

    /**
     * Generates and applies LOD levels to the geometries in a background thread.
     *
     * @param reductionMethod     The triangle reduction method to use (e.g., PROPORTIONAL).
     * @param reductionPercentage The percentage of triangles to reduce (0.0 to 1.0).
     * @param targetLodLevel      The index of the LOD level to set active after generation.
     */
    private void makeLod(final LodGenerator.TriangleReductionMethod reductionMethod,
                         final float reductionPercentage, final int targetLodLevel) {

        // --- Asynchronous LOD Generation ---
        // Execute the LOD generation process in the background thread pool.
        exec.execute(new Runnable() {
            @Override
            public void run() {
                for (final Geometry geom : listGeoms) {
                    LodGenerator lodGenerator = new LodGenerator(geom);
                    final VertexBuffer[] lods = lodGenerator.computeLods(reductionMethod, reductionPercentage);

                    // --- JME Thread Synchronization ---
                    // Mesh modifications and scene graph updates must be done on the main thread.
                    enqueue(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            geom.getMesh().setLodLevels(lods);

                            // Reset lodLevel to 0 initially
                            lodLevel = 0;
                            // If the generated LOD levels are more than the target, set to target LOD
                            if (geom.getMesh().getNumLodLevels() > targetLodLevel) {
                                lodLevel = targetLodLevel;
                            }
                            geom.setLodLevel(lodLevel);

                            int nbTri = computeNbTri();
                            hudText.setText(nbTri + " tris");

                            // Print debug information to the console
                            System.out.println(geom + " lodLevel: " + lodLevel + ", numLodLevels: " + geom.getMesh().getNumLodLevels()
                                    + ", reductionValue: " + reductionValue + ", triangles: " + nbTri);
                            return null;
                        }
                    });
                }
            }
        });
    }
}
