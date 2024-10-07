/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;

import jme3tools.optimize.LodGenerator;

public class TestLodGeneration extends SimpleApplication {

    public static void main(String[] args) {
        TestLodGeneration app = new TestLodGeneration();
        app.start();
    }

    private boolean wireFrame = false;
    private float reductionValue = 0.0f;
    private int lodLevel = 0;
    private BitmapText hudText;
    final private List<Geometry> listGeoms = new ArrayList<>();
    final private ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(5);

    @Override
    public void simpleInitApp() {

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(dl);

        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.6f));
        rootNode.addLight(al);

        // model = (Node) assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
        Node model = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        BoundingBox b = ((BoundingBox) model.getWorldBound());
        model.setLocalScale(1.2f / (b.getYExtent() * 2));
        // model.setLocalTranslation(0,-(b.getCenter().y - b.getYExtent())* model.getLocalScale().y, 0);
        for (Spatial spatial : model.getChildren()) {
            if (spatial instanceof Geometry) {
                listGeoms.add((Geometry) spatial);
            }
        }

        ChaseCamera chaseCam = new ChaseCamera(cam, inputManager);
        model.addControl(chaseCam);
        chaseCam.setLookAtOffset(b.getCenter());
        chaseCam.setDefaultDistance(5);
        chaseCam.setMinVerticalRotation(-FastMath.HALF_PI + 0.01f);
        chaseCam.setZoomSensitivity(0.5f);

        SkinningControl skControl = model.getControl(SkinningControl.class);
        if (skControl != null) {
            skControl.setEnabled(false);
        }

        reductionValue = 0.80f;
        lodLevel = 1;
        for (final Geometry geom : listGeoms) {
            LodGenerator lodGenerator = new LodGenerator(geom);
            lodGenerator.bakeLods(LodGenerator.TriangleReductionMethod.PROPORTIONAL, reductionValue);
            geom.setLodLevel(lodLevel);
        }

        rootNode.attachChild(model);
        flyCam.setEnabled(false);

        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        hudText = new BitmapText(guiFont);
        hudText.setSize(guiFont.getCharSet().getRenderedSize());
        hudText.setText(computeNbTri() + " tris");
        hudText.setLocalTranslation(cam.getWidth() / 2, hudText.getLineHeight(), 0);
        guiNode.attachChild(hudText);

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed) {
                    if (name.equals("plus")) {
                        reductionValue += 0.05f;
                        updateLod();
                    }
                    if (name.equals("minus")) {
                        reductionValue -= 0.05f;
                        updateLod();
                    }
                    if (name.equals("wireFrame")) {
                        wireFrame = !wireFrame;
                        for (Geometry geom : listGeoms) {
                            Material mat = geom.getMaterial();
                            mat.getAdditionalRenderState().setWireframe(wireFrame);
                        }
                    }
                }
            }
        }, "plus", "minus", "wireFrame");

        inputManager.addMapping("plus", new KeyTrigger(KeyInput.KEY_ADD));
        inputManager.addMapping("minus", new KeyTrigger(KeyInput.KEY_SUBTRACT));
        inputManager.addMapping("wireFrame", new KeyTrigger(KeyInput.KEY_SPACE));
    }

    @Override
    public void simpleUpdate(float tpf) {
    }

    @Override
    public void destroy() {
        super.destroy();
        exec.shutdown();
    }

    private void updateLod() {
        reductionValue = FastMath.clamp(reductionValue, 0.0f, 1.0f);
        makeLod(LodGenerator.TriangleReductionMethod.PROPORTIONAL, reductionValue, 1);
    }

    private int computeNbTri() {
        int nbTri = 0;
        for (Geometry geom : listGeoms) {
            Mesh mesh = geom.getMesh();
            if (mesh.getNumLodLevels() > 0) {
                nbTri += mesh.getLodLevel(lodLevel).getNumElements();
            } else {
                nbTri += mesh.getTriangleCount();
            }
        }
        return nbTri;
    }

    private void makeLod(final LodGenerator.TriangleReductionMethod method, final float value, final int ll) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                for (final Geometry geom : listGeoms) {
                    LodGenerator lodGenerator = new LodGenerator(geom);
                    final VertexBuffer[] lods = lodGenerator.computeLods(method, value);

                    enqueue(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            geom.getMesh().setLodLevels(lods);
                            lodLevel = 0;
                            if (geom.getMesh().getNumLodLevels() > ll) {
                                lodLevel = ll;
                            }
                            geom.setLodLevel(lodLevel);
                            hudText.setText(computeNbTri() + " tris");
                            return null;
                        }
                    });
                }
            }
        });
    }
}
