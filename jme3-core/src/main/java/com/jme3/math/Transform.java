/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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
package com.jme3.math;

import com.jme3.export.*;
import com.jme3.util.TempVars;

import java.io.IOException;

/**
 * Started Date: Jul 16, 2004<br><br>
 * Represents a translation, rotation and scale in one object.
 * 
 * @author Jack Lindamood
 * @author Joshua Slack
 */
public final class Transform implements Savable, Cloneable, java.io.Serializable {

    static final long serialVersionUID = 1;

    public static final Transform IDENTITY = new Transform();

    private Quaternion rot = new Quaternion();
    private Vector3f translation = new Vector3f();
    private Vector3f scale = new Vector3f(1, 1, 1);

    public Transform(Vector3f translation, Quaternion rot){
        this.translation.set(translation);
        this.rot.set(rot);
    }
    
    public Transform(Vector3f translation, Quaternion rot, Vector3f scale){
        this(translation, rot);
        this.scale.set(scale);
    }

    public Transform(Vector3f translation){
        this(translation, Quaternion.IDENTITY);
    }

    public Transform(Quaternion rot){
        this(Vector3f.ZERO, rot);
    }

    public Transform(){
        this(Vector3f.ZERO, Quaternion.IDENTITY);
    }

    /**
     * Sets this rotation to the given Quaternion value.
     * @param rot The new rotation for this matrix.
     * @return this
     */
    public Transform setRotation(Quaternion rot) {
        this.rot.set(rot);
        return this;
    }

    /**
     * Sets this translation to the given value.
     * @param trans The new translation for this matrix.
     * @return this
     */
    public Transform setTranslation(Vector3f trans) {
        this.translation.set(trans);
        return this;
    }

    /**
     * Return the translation vector in this matrix.
     * @return translation vector.
     */
    public Vector3f getTranslation() {
        return translation;
    }

    /**
     * Sets this scale to the given value.
     * @param scale The new scale for this matrix.
     * @return this
     */
    public Transform setScale(Vector3f scale) {
        this.scale.set(scale);
        return this;
    }

    /**
     * Sets this scale to the given value.
     * @param scale The new scale for this matrix.
     * @return this
     */
    public Transform setScale(float scale) {
        this.scale.set(scale, scale, scale);
        return this;
    }

    /**
     * Return the scale vector in this matrix.
     * @return scale vector.
     */
    public Vector3f getScale() {
        return scale;
    }

    /**
     * Stores this translation value into the given vector3f.  If trans is null, a new vector3f is created to
     * hold the value.  The value, once stored, is returned.
     * @param trans The store location for this matrix's translation.
     * @return The value of this matrix's translation.
     */
    public Vector3f getTranslation(Vector3f trans) {
        if (trans==null) trans=new Vector3f();
        trans.set(this.translation);
        return trans;
    }

    /**
     * Stores this rotation value into the given Quaternion.  If quat is null, a new Quaternion is created to
     * hold the value.  The value, once stored, is returned.
     * @param quat The store location for this matrix's rotation.
     * @return The value of this matrix's rotation.
     */
    public Quaternion getRotation(Quaternion quat) {
        if (quat==null) quat=new Quaternion();
        quat.set(rot);
        return quat;
    }
    
    /**
     * Return the rotation quaternion in this matrix.
     * @return rotation quaternion.
     */
    public Quaternion getRotation() {
        return rot;
    } 
    
    /**
     * Stores this scale value into the given vector3f.  If scale is null, a new vector3f is created to
     * hold the value.  The value, once stored, is returned.
     * @param scale The store location for this matrix's scale.
     * @return The value of this matrix's scale.
     */
    public Vector3f getScale(Vector3f scale) {
        if (scale==null) scale=new Vector3f();
        scale.set(this.scale);
        return scale;
    }

    /**
     * Sets this transform to the interpolation between the first transform and the second by delta amount.
     * @param t1 The beginning transform.
     * @param t2 The ending transform.
     * @param delta An amount between 0 and 1 representing how far to interpolate from t1 to t2.
     */
    public void interpolateTransforms(Transform t1, Transform t2, float delta) {
        t1.rot.nlerp(t2.rot, delta);
        this.rot.set(t1.rot);
        this.translation.interpolateLocal(t1.translation,t2.translation,delta);
        this.scale.interpolateLocal(t1.scale,t2.scale,delta);
    }

    /**
     * Changes the values of this matrix according to its parent.  Very similar to the concept of Node/Spatial transforms.
     * @param parent The parent matrix.
     * @return This matrix, after combining.
     */
    public Transform combineWithParent(Transform parent) {
        //applying parent scale to local scale
        scale.multLocal(parent.scale);
        //applying parent rotation to local rotation.
        parent.rot.mult(rot, rot);
        //applying parent scale to local translation.
        translation.multLocal(parent.scale);
        //applying parent rotation to local translation, then applying parent translation to local translation.
        //Note that parent.rot.multLocal(translation) doesn't modify "parent.rot" but "translation"
        parent
            .rot
            .multLocal(translation)
            .addLocal(parent.translation);

        return this;
    }

    /**
     * Sets this matrix's translation to the given x,y,z values.
     * @param x This matrix's new x translation.
     * @param y This matrix's new y translation.
     * @param z This matrix's new z translation.
     * @return this
     */
    public Transform setTranslation(float x,float y, float z) {
        translation.set(x,y,z);
        return this;
    }

    /**
     * Sets this matrix's scale to the given x,y,z values.
     * @param x This matrix's new x scale.
     * @param y This matrix's new y scale.
     * @param z This matrix's new z scale.
     * @return this
     */
    public Transform setScale(float x, float y, float z) {
        scale.set(x,y,z);
        return this;
    }

    public Vector3f transformVector(final Vector3f in, Vector3f store){
        if (store == null)
            store = new Vector3f();

        // multiply with scale first, then rotate, finally translate (cf.
        // Eberly)
        return rot.mult(store.set(in).multLocal(scale), store).addLocal(translation);
    }

    public Vector3f transformInverseVector(final Vector3f in, Vector3f store){
        if (store == null)
            store = new Vector3f();

        // The author of this code should look above and take the inverse of that
        // But for some reason, they didn't ..
//        in.subtract(translation, store).divideLocal(scale);
//        rot.inverse().mult(store, store);

        in.subtract(translation, store);
        rot.inverse().mult(store, store);
        store.divideLocal(scale);

        return store;
    }

    public Matrix4f toTransformMatrix() {
        return toTransformMatrix(null);
    }

    public Matrix4f toTransformMatrix(Matrix4f store) {
        if (store == null) {
            store = new Matrix4f();
        }
        store.setTranslation(translation);
        rot.toTransformMatrix(store);
        store.setScale(scale);
        return store;
    }
    
    public void fromTransformMatrix(Matrix4f mat) {
        TempVars vars = TempVars.get();
        translation.set(mat.toTranslationVector(vars.vect1));
        rot.set(mat.toRotationQuat(vars.quat1));
        scale.set(mat.toScaleVector(vars.vect2));
        vars.release();
    }
    
    public Transform invert() {
        Transform t = new Transform();
        t.fromTransformMatrix(toTransformMatrix().invertLocal());
        return t;
    }
    
    /**
     * Loads the identity.  Equal to translation=0,0,0 scale=1,1,1 rot=0,0,0,1.
     */
    public void loadIdentity() {
        translation.set(0, 0, 0);
        scale.set(1, 1, 1);
        rot.set(0, 0, 0, 1);
    }

    /**
     * Test for exact identity.
     *
     * @return true if exactly equal to {@link #IDENTITY}, otherwise false
     */
    public boolean isIdentity() {
        return translation.x == 0f && translation.y == 0f && translation.z == 0f
                && scale.x == 1f && scale.y == 1f && scale.z == 1f
                && rot.w == 1f && rot.x == 0f && rot.y == 0f && rot.z == 0f;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + rot.hashCode();
        hash = 89 * hash + translation.hashCode();
        hash = 89 * hash + scale.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Transform other = (Transform) obj;
        return this.translation.equals(other.translation)
                && this.scale.equals(other.scale)
                && this.rot.equals(other.rot);
    }

    @Override
    public String toString(){
        return getClass().getSimpleName() + "[ " + translation.x + ", " + translation.y + ", " + translation.z + "]\n"
                                          + "[ " + rot.x + ", " + rot.y + ", " + rot.z + ", " + rot.w + "]\n"
                                          + "[ " + scale.x + " , " + scale.y + ", " + scale.z + "]";
    }

    /**
     * Sets this matrix to be equal to the given matrix.
     * @param matrixQuat The matrix to be equal to.
     * @return this
     */
    public Transform set(Transform matrixQuat) {
        this.translation.set(matrixQuat.translation);
        this.rot.set(matrixQuat.rot);
        this.scale.set(matrixQuat.scale);
        return this;
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(rot, "rot", Quaternion.IDENTITY);
        capsule.write(translation, "translation", Vector3f.ZERO);
        capsule.write(scale, "scale", Vector3f.UNIT_XYZ);
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        InputCapsule capsule = e.getCapsule(this);
        
        rot.set((Quaternion)capsule.readSavable("rot", Quaternion.IDENTITY));
        translation.set((Vector3f)capsule.readSavable("translation", Vector3f.ZERO));
        scale.set((Vector3f)capsule.readSavable("scale", Vector3f.UNIT_XYZ));
    }
    
    @Override
    public Transform clone() {
        try {
            Transform tq = (Transform) super.clone();
            tq.rot = rot.clone();
            tq.scale = scale.clone();
            tq.translation = translation.clone();
            return tq;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
