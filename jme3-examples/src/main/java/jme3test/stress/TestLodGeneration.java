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
package jme3test.stress;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import jme3tools.optimize.LodGenerator;

public class TestLodGeneration extends SimpleApplication {

    public static void main(String[] args) {
        TestLodGeneration app = new TestLodGeneration();
        app.start();
    }
    boolean wireFrame = false;
    float reductionvalue = 0.0f;
    private int lodLevel = 0;
    private Node model;
    private BitmapText hudText;
    private List<Geometry> listGeoms = new ArrayList<Geometry>();
    private ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(5);
    private AnimChannel ch;

    public void simpleInitApp() {

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(dl);
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.6f));
        rootNode.addLight(al);

       // model = (Node) assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
       model = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        BoundingBox b = ((BoundingBox) model.getWorldBound());
        model.setLocalScale(1.2f / (b.getYExtent() * 2));
        //  model.setLocalTranslation(0,-(b.getCenter().y - b.getYExtent())* model.getLocalScale().y, 0);
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



//           ch = model.getControl(AnimControl.class).createChannel();
//          ch.setAnim("Wave");
        SkeletonControl c = model.getControl(SkeletonControl.class);
        if (c != null) {
            c.setEnabled(false);
        }


        reductionvalue = 0.80f;
        lodLevel = 1;
        for (final Geometry geometry : listGeoms) {
            LodGenerator lodGenerator = new LodGenerator(geometry);          
            lodGenerator.bakeLods(LodGenerator.TriangleReductionMethod.PROPORTIONAL, reductionvalue);
            geometry.setLodLevel(lodLevel);

        }

        rootNode.attachChild(model);
        flyCam.setEnabled(false);



        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        hudText = new BitmapText(guiFont, false);
        hudText.setSize(guiFont.getCharSet().getRenderedSize());
        hudText.setText(computeNbTri() + " tris");
        hudText.setLocalTranslation(cam.getWidth() / 2, hudText.getLineHeight(), 0);
        guiNode.attachChild(hudText);

        inputManager.addListener(new ActionListener() {
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed) {
                    if (name.equals("plus")) {
//                        lodLevel++;
//                        for (Geometry geometry : listGeoms) {
//                            if (geometry.getMesh().getNumLodLevels() <= lodLevel) {
//                                lodLevel = 0;
//                            }
//                            geometry.setLodLevel(lodLevel);
//                        }
//                        jaimeText.setText(computeNbTri() + " tris");



                        reductionvalue += 0.05f;
                        updateLod();



                    }
                    if (name.equals("minus")) {
//                        lodLevel--;
//                        for (Geometry geometry : listGeoms) {
//                            if (lodLevel < 0) {
//                                lodLevel = geometry.getMesh().getNumLodLevels() - 1;
//                            }
//                            geometry.setLodLevel(lodLevel);
//                        }
//                        jaimeText.setText(computeNbTri() + " tris");



                        reductionvalue -= 0.05f;
                        updateLod();


                    }
                    if (name.equals("wireFrame")) {
                        wireFrame = !wireFrame;
                        for (Geometry geometry : listGeoms) {
                            geometry.getMaterial().getAdditionalRenderState().setWireframe(wireFrame);
                        }
                    }

                }

            }

            private void updateLod() {
                reductionvalue = FastMath.clamp(reductionvalue, 0.0f, 1.0f);
                makeLod(LodGenerator.TriangleReductionMethod.PROPORTIONAL, reductionvalue, 1);
            }
        }, "plus", "minus", "wireFrame");

        inputManager.addMapping("plus", new KeyTrigger(KeyInput.KEY_ADD));
        inputManager.addMapping("minus", new KeyTrigger(KeyInput.KEY_SUBTRACT));
        inputManager.addMapping("wireFrame", new KeyTrigger(KeyInput.KEY_SPACE));



    }

    @Override
    public void simpleUpdate(float tpf) {
        //    model.rotate(0, tpf, 0);        
    }

    private int computeNbTri() {
        int nbTri = 0;
        for (Geometry geometry : listGeoms) {
            if (geometry.getMesh().getNumLodLevels() > 0) {
                nbTri += geometry.getMesh().getLodLevel(lodLevel).getNumElements();
            } else {
                nbTri += geometry.getMesh().getTriangleCount();
            }
        }
        return nbTri;
    }

    @Override
    public void destroy() {
        super.destroy();
        exec.shutdown();
    }

    private void makeLod(final LodGenerator.TriangleReductionMethod method, final float value, final int ll) {
        exec.execute(new Runnable() {
            public void run() {
                for (final Geometry geometry : listGeoms) {
                    LodGenerator lODGenerator = new LodGenerator(geometry);
                    final VertexBuffer[] lods = lODGenerator.computeLods(method, value);

                    enqueue(new Callable<Void>() {
                        public Void call() throws Exception {
                            geometry.getMesh().setLodLevels(lods);
                            lodLevel = 0;
                            if (geometry.getMesh().getNumLodLevels() > ll) {
                                lodLevel = ll;
                            }
                            geometry.setLodLevel(lodLevel);
                            hudText.setText(computeNbTri() + " tris");
                            return null;
                        }
                    });
                }
            }
        });

    }
}
