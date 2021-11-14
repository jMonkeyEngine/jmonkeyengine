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
 * The test increments the angle (starting from angle 180 degrees) and the radius (starting from 0.5) on each run based on the time per frames and
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

        rootNode.attachChild(bambooNode);
        //do the blending between 2 actions.
        startBlendAction();
    }
    private void startBlendAction(){
        //key frames timings
        final float[] times = new float[]{2, 4, 8, 16};

        //first action -- vertical traction.
        final Quaternion[] verticalTraction = new Quaternion[] {
                new Quaternion().fromAngleAxis(0, Vector3f.UNIT_Z),
                bambooNode.getLocalRotation().fromAngleAxis((float)Math.toRadians(120), Vector3f.UNIT_Z),
                bambooNode.getLocalRotation().fromAngleAxis((float)Math.toRadians(120), Vector3f.UNIT_Z),
                bambooNode.getLocalRotation().fromAngleAxis((float)Math.toRadians(120), Vector3f.UNIT_Z)
        };
        final TransformTrack bambooVerticalTraction = new TransformTrack(bambooNode, times, null, verticalTraction, null);
        final AnimClip verticalTractionClip = new AnimClip("Bamboo Rotation");
        verticalTractionClip.setTracks(new AnimTrack[]{ bambooVerticalTraction });
        final ClipAction verticalTractionAction = new ClipAction(verticalTractionClip);
        verticalTractionAction.setSpeed(8f);
        verticalTractionAction.setLength(10f);

        //second action -- horizontal traction.
        final Quaternion[] traction = new Quaternion[] {
                new Quaternion().fromAngleAxis(0, Vector3f.UNIT_X),
                bambooNode.getLocalRotation().fromAngleAxis((float)Math.toRadians(30), Vector3f.UNIT_X),
                bambooNode.getLocalRotation().fromAngleAxis((float)Math.toRadians(30), Vector3f.UNIT_X),
                bambooNode.getLocalRotation().fromAngleAxis((float)Math.toRadians(30), Vector3f.UNIT_X)
        };
        final TransformTrack bambooHorizontalTraction = new TransformTrack(bambooNode, times, null, traction, null);
        final AnimClip tractionClip = new AnimClip("Bamboo Traction");
        tractionClip.setTracks(new AnimTrack[]{ bambooHorizontalTraction });
        final ClipAction horizontalTractionAction = new ClipAction(tractionClip);
        horizontalTractionAction.setSpeed(5f);
        horizontalTractionAction.setLength(10f);

        //apply the pie chart blend space
        pieChartSpace = new PieChartSpace(0.5f, 180f);
        blendAction = new BlendAction(pieChartSpace, verticalTractionAction, horizontalTractionAction);
        composer.addAction("Bamboo Clip", blendAction);
        composer.setCurrentAction("Bamboo Clip", AnimComposer.DEFAULT_LAYER);
    }
    @Override
    public void simpleUpdate(float tpf) {
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
