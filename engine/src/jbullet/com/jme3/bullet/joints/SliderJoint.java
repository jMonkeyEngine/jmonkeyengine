/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.bullet.joints;

import com.bulletphysics.dynamics.constraintsolver.SliderConstraint;
import com.bulletphysics.linearmath.Transform;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.util.Converter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * <i>From bullet manual:</i><br>
 * The slider constraint allows the body to rotate around one axis and translate along this axis.
 * @author normenhansen
 */
public class SliderJoint extends PhysicsJoint {
    protected Matrix3f rotA, rotB;
    protected boolean useLinearReferenceFrameA;

    public SliderJoint() {
    }

    /**
     * @param pivotA local translation of the joint connection point in node A
     * @param pivotB local translation of the joint connection point in node B
     */
    public SliderJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB, Matrix3f rotA, Matrix3f rotB, boolean useLinearReferenceFrameA) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.rotA=rotA;
        this.rotB=rotB;
        this.useLinearReferenceFrameA=useLinearReferenceFrameA;
        createJoint();
    }

    /**
     * @param pivotA local translation of the joint connection point in node A
     * @param pivotB local translation of the joint connection point in node B
     */
    public SliderJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB, boolean useLinearReferenceFrameA) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.rotA=new Matrix3f();
        this.rotB=new Matrix3f();
        this.useLinearReferenceFrameA=useLinearReferenceFrameA;
        createJoint();
    }

    public float getLowerLinLimit() {
        return ((SliderConstraint) constraint).getLowerLinLimit();
    }

    public void setLowerLinLimit(float lowerLinLimit) {
        ((SliderConstraint) constraint).setLowerLinLimit(lowerLinLimit);
    }

    public float getUpperLinLimit() {
        return ((SliderConstraint) constraint).getUpperLinLimit();
    }

    public void setUpperLinLimit(float upperLinLimit) {
        ((SliderConstraint) constraint).setUpperLinLimit(upperLinLimit);
    }

    public float getLowerAngLimit() {
        return ((SliderConstraint) constraint).getLowerAngLimit();
    }

    public void setLowerAngLimit(float lowerAngLimit) {
        ((SliderConstraint) constraint).setLowerAngLimit(lowerAngLimit);
    }

    public float getUpperAngLimit() {
        return ((SliderConstraint) constraint).getUpperAngLimit();
    }

    public void setUpperAngLimit(float upperAngLimit) {
        ((SliderConstraint) constraint).setUpperAngLimit(upperAngLimit);
    }

    public float getSoftnessDirLin() {
        return ((SliderConstraint) constraint).getSoftnessDirLin();
    }

    public void setSoftnessDirLin(float softnessDirLin) {
        ((SliderConstraint) constraint).setSoftnessDirLin(softnessDirLin);
    }

    public float getRestitutionDirLin() {
        return ((SliderConstraint) constraint).getRestitutionDirLin();
    }

    public void setRestitutionDirLin(float restitutionDirLin) {
        ((SliderConstraint) constraint).setRestitutionDirLin(restitutionDirLin);
    }

    public float getDampingDirLin() {
        return ((SliderConstraint) constraint).getDampingDirLin();
    }

    public void setDampingDirLin(float dampingDirLin) {
        ((SliderConstraint) constraint).setDampingDirLin(dampingDirLin);
    }

    public float getSoftnessDirAng() {
        return ((SliderConstraint) constraint).getSoftnessDirAng();
    }

    public void setSoftnessDirAng(float softnessDirAng) {
        ((SliderConstraint) constraint).setSoftnessDirAng(softnessDirAng);
    }

    public float getRestitutionDirAng() {
        return ((SliderConstraint) constraint).getRestitutionDirAng();
    }

    public void setRestitutionDirAng(float restitutionDirAng) {
        ((SliderConstraint) constraint).setRestitutionDirAng(restitutionDirAng);
    }

    public float getDampingDirAng() {
        return ((SliderConstraint) constraint).getDampingDirAng();
    }

    public void setDampingDirAng(float dampingDirAng) {
        ((SliderConstraint) constraint).setDampingDirAng(dampingDirAng);
    }

    public float getSoftnessLimLin() {
        return ((SliderConstraint) constraint).getSoftnessLimLin();
    }

    public void setSoftnessLimLin(float softnessLimLin) {
        ((SliderConstraint) constraint).setSoftnessLimLin(softnessLimLin);
    }

    public float getRestitutionLimLin() {
        return ((SliderConstraint) constraint).getRestitutionLimLin();
    }

    public void setRestitutionLimLin(float restitutionLimLin) {
        ((SliderConstraint) constraint).setRestitutionLimLin(restitutionLimLin);
    }

    public float getDampingLimLin() {
        return ((SliderConstraint) constraint).getDampingLimLin();
    }

    public void setDampingLimLin(float dampingLimLin) {
        ((SliderConstraint) constraint).setDampingLimLin(dampingLimLin);
    }

    public float getSoftnessLimAng() {
        return ((SliderConstraint) constraint).getSoftnessLimAng();
    }

    public void setSoftnessLimAng(float softnessLimAng) {
        ((SliderConstraint) constraint).setSoftnessLimAng(softnessLimAng);
    }

    public float getRestitutionLimAng() {
        return ((SliderConstraint) constraint).getRestitutionLimAng();
    }

    public void setRestitutionLimAng(float restitutionLimAng) {
        ((SliderConstraint) constraint).setRestitutionLimAng(restitutionLimAng);
    }

    public float getDampingLimAng() {
        return ((SliderConstraint) constraint).getDampingLimAng();
    }

    public void setDampingLimAng(float dampingLimAng) {
        ((SliderConstraint) constraint).setDampingLimAng(dampingLimAng);
    }

    public float getSoftnessOrthoLin() {
        return ((SliderConstraint) constraint).getSoftnessOrthoLin();
    }

    public void setSoftnessOrthoLin(float softnessOrthoLin) {
        ((SliderConstraint) constraint).setSoftnessOrthoLin(softnessOrthoLin);
    }

    public float getRestitutionOrthoLin() {
        return ((SliderConstraint) constraint).getRestitutionOrthoLin();
    }

    public void setRestitutionOrthoLin(float restitutionOrthoLin) {
        ((SliderConstraint) constraint).setRestitutionOrthoLin(restitutionOrthoLin);
    }

    public float getDampingOrthoLin() {
        return ((SliderConstraint) constraint).getDampingOrthoLin();
    }

    public void setDampingOrthoLin(float dampingOrthoLin) {
        ((SliderConstraint) constraint).setDampingOrthoLin(dampingOrthoLin);
    }

    public float getSoftnessOrthoAng() {
        return ((SliderConstraint) constraint).getSoftnessOrthoAng();
    }

    public void setSoftnessOrthoAng(float softnessOrthoAng) {
        ((SliderConstraint) constraint).setSoftnessOrthoAng(softnessOrthoAng);
    }

    public float getRestitutionOrthoAng() {
        return ((SliderConstraint) constraint).getRestitutionOrthoAng();
    }

    public void setRestitutionOrthoAng(float restitutionOrthoAng) {
        ((SliderConstraint) constraint).setRestitutionOrthoAng(restitutionOrthoAng);
    }

    public float getDampingOrthoAng() {
        return ((SliderConstraint) constraint).getDampingOrthoAng();
    }

    public void setDampingOrthoAng(float dampingOrthoAng) {
        ((SliderConstraint) constraint).setDampingOrthoAng(dampingOrthoAng);
    }

    public boolean isPoweredLinMotor() {
        return ((SliderConstraint) constraint).getPoweredLinMotor();
    }

    public void setPoweredLinMotor(boolean poweredLinMotor) {
        ((SliderConstraint) constraint).setPoweredLinMotor(poweredLinMotor);
    }

    public float getTargetLinMotorVelocity() {
        return ((SliderConstraint) constraint).getTargetLinMotorVelocity();
    }

    public void setTargetLinMotorVelocity(float targetLinMotorVelocity) {
        ((SliderConstraint) constraint).setTargetLinMotorVelocity(targetLinMotorVelocity);
    }

    public float getMaxLinMotorForce() {
        return ((SliderConstraint) constraint).getMaxLinMotorForce();
    }

    public void setMaxLinMotorForce(float maxLinMotorForce) {
        ((SliderConstraint) constraint).setMaxLinMotorForce(maxLinMotorForce);
    }

    public boolean isPoweredAngMotor() {
        return ((SliderConstraint) constraint).getPoweredAngMotor();
    }

    public void setPoweredAngMotor(boolean poweredAngMotor) {
        ((SliderConstraint) constraint).setPoweredAngMotor(poweredAngMotor);
    }

    public float getTargetAngMotorVelocity() {
        return ((SliderConstraint) constraint).getTargetAngMotorVelocity();
    }

    public void setTargetAngMotorVelocity(float targetAngMotorVelocity) {
        ((SliderConstraint) constraint).setTargetAngMotorVelocity(targetAngMotorVelocity);
    }

    public float getMaxAngMotorForce() {
        return ((SliderConstraint) constraint).getMaxAngMotorForce();
    }

    public void setMaxAngMotorForce(float maxAngMotorForce) {
        ((SliderConstraint) constraint).setMaxAngMotorForce(maxAngMotorForce);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        //TODO: standard values..
        capsule.write(((SliderConstraint) constraint).getDampingDirAng(), "dampingDirAng", 0f);
        capsule.write(((SliderConstraint) constraint).getDampingDirLin(), "dampingDirLin", 0f);
        capsule.write(((SliderConstraint) constraint).getDampingLimAng(), "dampingLimAng", 0f);
        capsule.write(((SliderConstraint) constraint).getDampingLimLin(), "dampingLimLin", 0f);
        capsule.write(((SliderConstraint) constraint).getDampingOrthoAng(), "dampingOrthoAng", 0f);
        capsule.write(((SliderConstraint) constraint).getDampingOrthoLin(), "dampingOrthoLin", 0f);
        capsule.write(((SliderConstraint) constraint).getLowerAngLimit(), "lowerAngLimit", 0f);
        capsule.write(((SliderConstraint) constraint).getLowerLinLimit(), "lowerLinLimit", 0f);
        capsule.write(((SliderConstraint) constraint).getMaxAngMotorForce(), "maxAngMotorForce", 0f);
        capsule.write(((SliderConstraint) constraint).getMaxLinMotorForce(), "maxLinMotorForce", 0f);
        capsule.write(((SliderConstraint) constraint).getPoweredAngMotor(), "poweredAngMotor", false);
        capsule.write(((SliderConstraint) constraint).getPoweredLinMotor(), "poweredLinMotor", false);
        capsule.write(((SliderConstraint) constraint).getRestitutionDirAng(), "restitutionDirAng", 0f);
        capsule.write(((SliderConstraint) constraint).getRestitutionDirLin(), "restitutionDirLin", 0f);
        capsule.write(((SliderConstraint) constraint).getRestitutionLimAng(), "restitutionLimAng", 0f);
        capsule.write(((SliderConstraint) constraint).getRestitutionLimLin(), "restitutionLimLin", 0f);
        capsule.write(((SliderConstraint) constraint).getRestitutionOrthoAng(), "restitutionOrthoAng", 0f);
        capsule.write(((SliderConstraint) constraint).getRestitutionOrthoLin(), "restitutionOrthoLin", 0f);

        capsule.write(((SliderConstraint) constraint).getSoftnessDirAng(), "softnessDirAng", 0f);
        capsule.write(((SliderConstraint) constraint).getSoftnessDirLin(), "softnessDirLin", 0f);
        capsule.write(((SliderConstraint) constraint).getSoftnessLimAng(), "softnessLimAng", 0f);
        capsule.write(((SliderConstraint) constraint).getSoftnessLimLin(), "softnessLimLin", 0f);
        capsule.write(((SliderConstraint) constraint).getSoftnessOrthoAng(), "softnessOrthoAng", 0f);
        capsule.write(((SliderConstraint) constraint).getSoftnessOrthoLin(), "softnessOrthoLin", 0f);

        capsule.write(((SliderConstraint) constraint).getTargetAngMotorVelocity(), "targetAngMotorVelicoty", 0f);
        capsule.write(((SliderConstraint) constraint).getTargetLinMotorVelocity(), "targetLinMotorVelicoty", 0f);

        capsule.write(((SliderConstraint) constraint).getUpperAngLimit(), "upperAngLimit", 0f);
        capsule.write(((SliderConstraint) constraint).getUpperLinLimit(), "upperLinLimit", 0f);

        capsule.write(useLinearReferenceFrameA, "useLinearReferenceFrameA", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        float dampingDirAng = capsule.readFloat("dampingDirAng", 0f);
        float dampingDirLin = capsule.readFloat("dampingDirLin", 0f);
        float dampingLimAng = capsule.readFloat("dampingLimAng", 0f);
        float dampingLimLin = capsule.readFloat("dampingLimLin", 0f);
        float dampingOrthoAng = capsule.readFloat("dampingOrthoAng", 0f);
        float dampingOrthoLin = capsule.readFloat("dampingOrthoLin", 0f);
        float lowerAngLimit = capsule.readFloat("lowerAngLimit", 0f);
        float lowerLinLimit = capsule.readFloat("lowerLinLimit", 0f);
        float maxAngMotorForce = capsule.readFloat("maxAngMotorForce", 0f);
        float maxLinMotorForce = capsule.readFloat("maxLinMotorForce", 0f);
        boolean poweredAngMotor = capsule.readBoolean("poweredAngMotor", false);
        boolean poweredLinMotor = capsule.readBoolean("poweredLinMotor", false);
        float restitutionDirAng = capsule.readFloat("restitutionDirAng", 0f);
        float restitutionDirLin = capsule.readFloat("restitutionDirLin", 0f);
        float restitutionLimAng = capsule.readFloat("restitutionLimAng", 0f);
        float restitutionLimLin = capsule.readFloat("restitutionLimLin", 0f);
        float restitutionOrthoAng = capsule.readFloat("restitutionOrthoAng", 0f);
        float restitutionOrthoLin = capsule.readFloat("restitutionOrthoLin", 0f);

        float softnessDirAng = capsule.readFloat("softnessDirAng", 0f);
        float softnessDirLin = capsule.readFloat("softnessDirLin", 0f);
        float softnessLimAng = capsule.readFloat("softnessLimAng", 0f);
        float softnessLimLin = capsule.readFloat("softnessLimLin", 0f);
        float softnessOrthoAng = capsule.readFloat("softnessOrthoAng", 0f);
        float softnessOrthoLin = capsule.readFloat("softnessOrthoLin", 0f);

        float targetAngMotorVelicoty = capsule.readFloat("targetAngMotorVelicoty", 0f);
        float targetLinMotorVelicoty = capsule.readFloat("targetLinMotorVelicoty", 0f);

        float upperAngLimit = capsule.readFloat("upperAngLimit", 0f);
        float upperLinLimit = capsule.readFloat("upperLinLimit", 0f);

        useLinearReferenceFrameA = capsule.readBoolean("useLinearReferenceFrameA", false);

        createJoint();

        ((SliderConstraint)constraint).setDampingDirAng(dampingDirAng);
        ((SliderConstraint)constraint).setDampingDirLin(dampingDirLin);
        ((SliderConstraint)constraint).setDampingLimAng(dampingLimAng);
        ((SliderConstraint)constraint).setDampingLimLin(dampingLimLin);
        ((SliderConstraint)constraint).setDampingOrthoAng(dampingOrthoAng);
        ((SliderConstraint)constraint).setDampingOrthoLin(dampingOrthoLin);
        ((SliderConstraint)constraint).setLowerAngLimit(lowerAngLimit);
        ((SliderConstraint)constraint).setLowerLinLimit(lowerLinLimit);
        ((SliderConstraint)constraint).setMaxAngMotorForce(maxAngMotorForce);
        ((SliderConstraint)constraint).setMaxLinMotorForce(maxLinMotorForce);
        ((SliderConstraint)constraint).setPoweredAngMotor(poweredAngMotor);
        ((SliderConstraint)constraint).setPoweredLinMotor(poweredLinMotor);
        ((SliderConstraint)constraint).setRestitutionDirAng(restitutionDirAng);
        ((SliderConstraint)constraint).setRestitutionDirLin(restitutionDirLin);
        ((SliderConstraint)constraint).setRestitutionLimAng(restitutionLimAng);
        ((SliderConstraint)constraint).setRestitutionLimLin(restitutionLimLin);
        ((SliderConstraint)constraint).setRestitutionOrthoAng(restitutionOrthoAng);
        ((SliderConstraint)constraint).setRestitutionOrthoLin(restitutionOrthoLin);

        ((SliderConstraint)constraint).setSoftnessDirAng(softnessDirAng);
        ((SliderConstraint)constraint).setSoftnessDirLin(softnessDirLin);
        ((SliderConstraint)constraint).setSoftnessLimAng(softnessLimAng);
        ((SliderConstraint)constraint).setSoftnessLimLin(softnessLimLin);
        ((SliderConstraint)constraint).setSoftnessOrthoAng(softnessOrthoAng);
        ((SliderConstraint)constraint).setSoftnessOrthoLin(softnessOrthoLin);

        ((SliderConstraint)constraint).setTargetAngMotorVelocity(targetAngMotorVelicoty);
        ((SliderConstraint)constraint).setTargetLinMotorVelocity(targetLinMotorVelicoty);

        ((SliderConstraint)constraint).setUpperAngLimit(upperAngLimit);
        ((SliderConstraint)constraint).setUpperLinLimit(upperLinLimit);
    }

    protected void createJoint(){
        Transform transA = new Transform(Converter.convert(rotA));
        Converter.convert(pivotA, transA.origin);
        Converter.convert(rotA, transA.basis);

        Transform transB = new Transform(Converter.convert(rotB));
        Converter.convert(pivotB, transB.origin);
        Converter.convert(rotB, transB.basis);

        constraint = new SliderConstraint(nodeA.getObjectId(), nodeB.getObjectId(), transA, transB, useLinearReferenceFrameA);
    }
}
