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
package jme3test.model.anim;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.tween.action.BlendAction;
import com.jme3.anim.tween.action.ClipAction;
import com.jme3.anim.tween.action.PieChartSpace;
import com.jme3.app.SimpleApplication;
import com.jme3.input.ChaseCamera;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Cylinder;

/**
 * Tests {@link com.jme3.anim.tween.action.PieChartSpace} on a primitive geometry.
 *
 * This test blends between 4 successive blendable actions, by changing the firstActionIndex and the secondActionIndex when the track times finishes
 * using the exposed functions of the pie chart blend space {@link PieChartSpace#setFirstAction(int)}, {@link PieChartSpace#setSecondAction(int)}.
 *
 * It increments the angle (starting from angle 180 degrees) and the radius (starting from 0.5) on each run based on the time per frames and
 * the progressive time {@link TestPieChartSpace#progress}.
 *
 * The sector angle is clamped in the range [0, 360] in degrees and the pie chart radius is
 * clamped in the range [0, 1].
 *
 * @author pavl_g.
 */
public class TestPieChartSpace extends SimpleApplication {

    private final AnimComposer composer = new AnimComposer();
    private PieChartSpace pieChartSpace;
    private float progress = 0f;
    private BlendAction blendAction;
    private final Node bambooNode = new Node();
    private float progressToSwitch = 0f;
    private final Vector3f originalScale = new Vector3f();
    private final Vector3f originalTranslation = new Vector3f();
    //key frames timings in seconds
    private final float[] times = new float[]{1, 2, 3, 4};

    public static void main(String[] args) {
        new TestPieChartSpace().start();
    }

    @Override
    public void simpleInitApp() {
        //the mesh.
        final Cylinder cylinder = new Cylinder(100,10,1,5f, true);
        final Geometry bamboo = new Geometry("Bamboo Plant", cylinder);
        final Material material = new Material(assetManager, "Common/MatDefs/Misc/fakeLighting.j3md");
        material.setColor("Color", new ColorRGBA(0f, 0.15f, 0f, 1f));
        bamboo.setMaterial(material);
        bambooNode.attachChild(bamboo);
        bambooNode.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y));

        //the camera.
        final ChaseCamera chaseCamera = new ChaseCamera(cam, bamboo, inputManager);
        chaseCamera.setDefaultDistance(-10f);
        chaseCamera.setDefaultHorizontalRotation(-FastMath.HALF_PI);
        chaseCamera.setDefaultVerticalRotation(FastMath.PI/3f);
        chaseCamera.setEnabled(true);
        chaseCamera.setSmoothMotion(true);

        //add the composer control.
        bamboo.addControl(composer);
        composer.setEnabled(true);
        originalScale.set(bambooNode.getLocalScale());
        originalTranslation.set(bambooNode.getLocalTranslation());

        rootNode.attachChild(bambooNode);
        //do the blending between 2 actions.
        startBlendAction();
    }
    private void startBlendAction(){

        //first action -- vertical traction.
        final Quaternion[] verticalTraction = new Quaternion[] {
                new Quaternion().fromAngleAxis(0, Vector3f.UNIT_Z),
                bambooNode.getLocalRotation().fromAngleAxis(0.6667f * FastMath.PI, Vector3f.UNIT_Z),
                bambooNode.getLocalRotation().fromAngleAxis(0.6667f * FastMath.PI, Vector3f.UNIT_Z),
                bambooNode.getLocalRotation().fromAngleAxis(0.6667f * FastMath.PI, Vector3f.UNIT_Z)
        };
        final TransformTrack bambooVerticalTraction = new TransformTrack(bambooNode, times, null, verticalTraction, null);
        final AnimClip verticalTractionClip = new AnimClip("Bamboo Rotation");
        verticalTractionClip.setTracks(new AnimTrack[]{ bambooVerticalTraction });
        final ClipAction verticalTractionAction = new ClipAction(verticalTractionClip);
        verticalTractionAction.setSpeed(8f);

        //second action -- horizontal traction.
        final Quaternion[] traction = new Quaternion[] {
                new Quaternion().fromAngleAxis(0, Vector3f.UNIT_X),
                bambooNode.getLocalRotation().fromAngleAxis(0.1667f * FastMath.PI, Vector3f.UNIT_X),
                bambooNode.getLocalRotation().fromAngleAxis(0.1667f * FastMath.PI, Vector3f.UNIT_X),
                bambooNode.getLocalRotation().fromAngleAxis(0.1667f * FastMath.PI, Vector3f.UNIT_X)
        };
        final TransformTrack bambooHorizontalTraction = new TransformTrack(bambooNode, times, null, traction, null);
        final AnimClip tractionClip = new AnimClip("Bamboo Traction");
        tractionClip.setTracks(new AnimTrack[]{ bambooHorizontalTraction });
        final ClipAction horizontalTractionAction = new ClipAction(tractionClip);
        horizontalTractionAction.setSpeed(5f);

        //third action -- scales action.
        final Vector3f[] scales = new Vector3f[]{
                originalScale,
                bambooNode.getLocalScale().add(0.5f, 0.5f, 0.5f),
                bambooNode.getLocalScale().add(0.5f, 0.5f, 0.5f),
                bambooNode.getLocalScale().add(0.5f, 0.5f, 0.5f),
        };
        final TransformTrack bambooScales = new TransformTrack(bambooNode, times, null, null, scales);
        final AnimClip bambooScalesClip = new AnimClip("Bamboo Scales");
        bambooScalesClip.setTracks(new AnimTrack[]{ bambooScales });
        final ClipAction scalesAction = new ClipAction(bambooScalesClip);
        scalesAction.setSpeed(2f);

        //forth action -- translation action.
        final Vector3f[] translations = new Vector3f[]{
                originalTranslation,
                bambooNode.getLocalTranslation().add(0,0, -0.1f),
                bambooNode.getLocalTranslation().add(0,0, -0.2f),
                bambooNode.getLocalTranslation().add(0, 0, -0.5f),
        };
        final TransformTrack bambooTranslations = new TransformTrack(bambooNode, times, null, null, translations);
        final AnimClip bambooTranslationsClip = new AnimClip("Bamboo Translations");
        bambooTranslationsClip.setTracks(new AnimTrack[]{ bambooTranslations });
        final ClipAction translationsAction = new ClipAction(bambooTranslationsClip);
        translationsAction.setSpeed(2f);

        //apply the pie chart blend space
        pieChartSpace = new PieChartSpace(0.5f, 180f);
        blendAction = new BlendAction(pieChartSpace, verticalTractionAction, horizontalTractionAction, scalesAction, translationsAction);
        //setup the initial firstActionIndex and the initial secondActionIndex
        pieChartSpace.setFirstAction(0);
        pieChartSpace.setSecondAction(1);
        composer.addAction("Bamboo Clip", blendAction);
        composer.setCurrentAction("Bamboo Clip", AnimComposer.DEFAULT_LAYER);
    }
    @Override
    public void simpleUpdate(float tpf) {
        progressToSwitch += tpf;
        //manipulate actions manually when the time passes the last frame times
        if(pieChartSpace.getFirstActionIndex() == 0 && pieChartSpace.getSecondActionIndex() == 1) {
            // 5 seconds represents more than the times[] of the transform tracks
            if (progressToSwitch >= times[times.length - 1] + 1f) {
                //re-switch them
                pieChartSpace.setFirstAction(2);
                pieChartSpace.setSecondAction(3);
                progressToSwitch = 0f;
            }
        } else if (pieChartSpace.getFirstActionIndex() == 2 && pieChartSpace.getSecondActionIndex() == 3) {
            // 5 seconds represents more than the times[] of the transform tracks
            if (progressToSwitch >= times[times.length - 1] + 1f) {
                //switch the actions
                pieChartSpace.setFirstAction(0);
                pieChartSpace.setSecondAction(1);
                progressToSwitch = 0f;
            }
        }

        progress += tpf;
        if(progress >= blendAction.getLength()){
            //advances the angles and the radius when the blend action finishes.
            //incrementing the angle would increase the sector area of the step.
            //the sector area represents an unscaled blending step.
            //incrementing the radius would increase the scale factor applied to the sector area .
            //the radius approximates the scaleFactor to 1.0.
            final float newAngle = pieChartSpace.getAngle() + (progress * 4);
            final float newRadius = pieChartSpace.getRadius() + (tpf * 2);
            pieChartSpace.setAngle(newAngle);
            pieChartSpace.setRadius(newRadius);
            progress = 0;
        }
    }
}
