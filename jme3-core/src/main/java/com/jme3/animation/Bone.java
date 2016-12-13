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
package com.jme3.animation;

import com.jme3.export.*;
import com.jme3.math.*;
import com.jme3.scene.Node;
import com.jme3.util.TempVars;
import com.jme3.util.clone.JmeCloneable;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.ArrayList;

/**
 * <code>Bone</code> describes a bone in the bone-weight skeletal animation
 * system. A bone contains a name and an index, as well as relevant
 * transformation data.
 * 
 * A bone has 3 sets of transforms :
 * 1. The bind transforms, that are the transforms of the bone when the skeleton
 * is in its rest pose (also called bind pose or T pose in the literature). 
 * The bind transforms are expressed in Local space meaning relatively to the 
 * parent bone.
 * 
 * 2. The Local transforms, that are the transforms of the bone once animation
 * or user transforms has been applied to the bind pose. The local transforms are
 * expressed in Local space meaning relatively to the parent bone.
 * 
 * 3. The Model transforms, that are the transforms of the bone relatives to the 
 * rootBone of the skeleton. Those transforms are what is needed to apply skinning 
 * to the mesh the skeleton controls.
 * Note that there can be several rootBones in a skeleton. The one considered for 
 * these transforms is the one that is an ancestor of this bone.
 *
 * @author Kirill Vainer
 * @author Rémy Bouquet
 */
public final class Bone implements Savable, JmeCloneable {

    // Version #2: Changed naming of transforms as they were misleading
    public static final int SAVABLE_VERSION = 2;
    private String name;
    private Bone parent;
    private ArrayList<Bone> children = new ArrayList<Bone>();
    /**
     * If enabled, user can control bone transform with setUserTransforms.
     * Animation transforms are not applied to this bone when enabled.
     */
    private boolean userControl = false;
    /**
     * The attachment node.
     */
    private Node attachNode;
    
    /**
     * Bind transform is the local bind transform of this bone. (local space)
     */
    private Vector3f bindPos;
    private Quaternion bindRot;
    private Vector3f bindScale;
    
    /**
     * The inverse bind transforms of this bone expressed in model space     
     */
    private Vector3f modelBindInversePos;
    private Quaternion modelBindInverseRot;
    private Vector3f modelBindInverseScale;
    
    /**
     * The local animated or user transform combined with the local bind transform
     */
    private Vector3f localPos = new Vector3f();
    private Quaternion localRot = new Quaternion();
    private Vector3f localScale = new Vector3f(1.0f, 1.0f, 1.0f);
    /**
     * The model transforms of this bone     
     */
    private Vector3f modelPos = new Vector3f();
    private Quaternion modelRot = new Quaternion();
    private Vector3f modelScale = new Vector3f();
    
    // Used for getCombinedTransform
    private Transform tmpTransform;
    
    /**
     * Used to handle blending from one animation to another.
     * See {@link #blendAnimTransforms(com.jme3.math.Vector3f, com.jme3.math.Quaternion, com.jme3.math.Vector3f, float)}
     * on how this variable is used.
     */
    private transient float currentWeightSum = -1;

    /**
     * Creates a new bone with the given name.
     * 
     * @param name Name to give to this bone
     */
    public Bone(String name) {
        if (name == null)
            throw new IllegalArgumentException("Name cannot be null");
        
        this.name = name;

        bindPos = new Vector3f();
        bindRot = new Quaternion();
        bindScale = new Vector3f(1, 1, 1);

        modelBindInversePos = new Vector3f();
        modelBindInverseRot = new Quaternion();
        modelBindInverseScale = new Vector3f();
    }

    /**
     * Special-purpose copy constructor. 
     * <p>
     * Only copies the name, user control state and bind pose transforms from the original.
     * <p>
     * The rest of the data is <em>NOT</em> copied, as it will be
     * generated automatically when the bone is animated.
     * 
     * @param source The bone from which to copy the data.
     */
    Bone(Bone source) {
        this.name = source.name;

        userControl = source.userControl;

        bindPos = source.bindPos.clone();
        bindRot = source.bindRot.clone();
        bindScale = source.bindScale.clone();

        modelBindInversePos = source.modelBindInversePos.clone();
        modelBindInverseRot = source.modelBindInverseRot.clone();
        modelBindInverseScale = source.modelBindInverseScale.clone();

        // parent and children will be assigned manually..
    }

    /**
     * Serialization only. Do not use.
     */
    public Bone() {
    }
    
    @Override   
    public Object jmeClone() {
        try {
            Bone clone = (Bone)super.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }     

    @Override   
    public void cloneFields( Cloner cloner, Object original ) {
    
        this.parent = cloner.clone(parent);
        this.children = cloner.clone(children);    
        
        this.attachNode = cloner.clone(attachNode);
        
        this.bindPos = cloner.clone(bindPos);
        this.bindRot = cloner.clone(bindRot);
        this.bindScale = cloner.clone(bindScale);
        
        this.modelBindInversePos = cloner.clone(modelBindInversePos);
        this.modelBindInverseRot = cloner.clone(modelBindInverseRot);
        this.modelBindInverseScale = cloner.clone(modelBindInverseScale);
    
        this.localPos = cloner.clone(localPos);
        this.localRot = cloner.clone(localRot);
        this.localScale = cloner.clone(localScale);
        
        this.modelPos = cloner.clone(modelPos);
        this.modelRot = cloner.clone(modelRot);
        this.modelScale = cloner.clone(modelScale);
    
        this.tmpTransform = cloner.clone(tmpTransform);
    }

    /**
     * Returns the name of the bone, set in the constructor.
     * 
     * @return The name of the bone, set in the constructor.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns parent bone of this bone, or null if it is a root bone.
     * @return The parent bone of this bone, or null if it is a root bone.
     */
    public Bone getParent() {
        return parent;
    }

    /**
     * Returns all the children bones of this bone.
     * 
     * @return All the children bones of this bone.
     */
    public ArrayList<Bone> getChildren() {
        return children;
    }

    /**
     * Returns the local position of the bone, relative to the parent bone.
     * 
     * @return The local position of the bone, relative to the parent bone.
     */
    public Vector3f getLocalPosition() {
        return localPos;
    }

    /**
     * Returns the local rotation of the bone, relative to the parent bone.
     * 
     * @return The local rotation of the bone, relative to the parent bone.
     */
    public Quaternion getLocalRotation() {
        return localRot;
    }

    /**
     * Returns the local scale of the bone, relative to the parent bone.
     * 
     * @return The local scale of the bone, relative to the parent bone.
     */
    public Vector3f getLocalScale() {
        return localScale;
    }

    /**
     * Returns the position of the bone in model space.
     * 
     * @return The position of the bone in model space.
     */
    public Vector3f getModelSpacePosition() {
        return modelPos;
    }

    /**
     * Returns the rotation of the bone in model space.
     * 
     * @return The rotation of the bone in model space.
     */
    public Quaternion getModelSpaceRotation() {
        return modelRot;
    }

    /**
     * Returns the scale of the bone in model space.
     * 
     * @return The scale of the bone in model space.
     */
    public Vector3f getModelSpaceScale() {
        return modelScale;
    }

    /**     
     * @deprecated use {@link #getModelBindInversePosition()}
     */
    @Deprecated 
    public Vector3f getWorldBindInversePosition() {
        return modelBindInversePos;
    }
    
      /**
     * Returns the inverse Bind position of this bone expressed in model space.
     * <p>
     * The inverse bind pose transform of the bone in model space is its "default"
     * transform with no animation applied.
     * 
     * @return the inverse bind position of this bone expressed in model space.
     */   
    public Vector3f getModelBindInversePosition() {
        return modelBindInversePos;
    }

    /**     
     * @deprecated use {@link #getModelBindInverseRotation()}
     */
    @Deprecated
    public Quaternion getWorldBindInverseRotation() {
        return modelBindInverseRot;
    }
    
      /**
     * Returns the inverse bind rotation of this bone expressed in model space.
     * <p>
     * The inverse bind pose transform of the bone in model space is its "default"
     * transform with no animation applied.
     * 
     * @return the inverse bind rotation of this bone expressed in model space.
     */
    public Quaternion getModelBindInverseRotation() {
        return modelBindInverseRot;
    }


    /**     
     * @deprecated use {@link #getModelBindInverseScale()}
     */
    @Deprecated
    public Vector3f getWorldBindInverseScale() {
        return modelBindInverseScale;
    }
    
    /**
     * Returns the inverse world bind pose scale.
     * <p>
     * The inverse bind pose transform of the bone in model space is its "default"
     * transform with no animation applied.
     * 
     * @return the inverse world bind pose scale.
     */
    public Vector3f getModelBindInverseScale() {
        return modelBindInverseScale;
    }

    public Transform getModelBindInverseTransform() {
        Transform t = new Transform();
        t.setTranslation(modelBindInversePos);
        t.setRotation(modelBindInverseRot);
        if (modelBindInverseScale != null) {
            t.setScale(modelBindInverseScale);
        }
        return t;
    }
    
    public Transform getBindInverseTransform() {
        Transform t = new Transform();
        t.setTranslation(bindPos);
        t.setRotation(bindRot);
        if (bindScale != null) {
            t.setScale(bindScale);
        }
        return t.invert();
    }
    
    /**    
     * @deprecated use {@link #getBindPosition()}
     */
    @Deprecated
    public Vector3f getWorldBindPosition() {
        return bindPos;
    }
    
     /**
     * Returns the bind position expressed in local space (relative to the parent bone).
     * <p>
     * The bind pose transform of the bone in local space is its "default"
     * transform with no animation applied.
     * 
     * @return the bind position in local space.
     */
    public Vector3f getBindPosition() {
        return bindPos;
    }

    /**  
     * @deprecated use {@link #getBindRotation() }
     */    
    @Deprecated
    public Quaternion getWorldBindRotation() {
        return bindRot;
    }

    /**
     * Returns the bind rotation expressed in local space (relative to the parent bone).
     * <p>
     * The bind pose transform of the bone in local space is its "default"
     * transform with no animation applied.
     * 
     * @return the bind rotation in local space.
     */    
    public Quaternion getBindRotation() {
        return bindRot;
    }  
    
    /**
     * @deprecated use {@link #getBindScale() }
     */
    @Deprecated
    public Vector3f getWorldBindScale() {
        return bindScale;
    }
    
    /**
     * Returns the  bind scale expressed in local space (relative to the parent bone).
     * <p>
     * The bind pose transform of the bone in local space is its "default"
     * transform with no animation applied.
     * 
     * @return the bind scale in local space.
     */
    public Vector3f getBindScale() {
        return bindScale;
    }

    /**
     * If enabled, user can control bone transform with setUserTransforms.
     * Animation transforms are not applied to this bone when enabled.
     */
    public void setUserControl(boolean enable) {
        userControl = enable;
    }

    /**
     * Add a new child to this bone. Shouldn't be used by user code.
     * Can corrupt skeleton.
     * 
     * @param bone The bone to add
     */
    public void addChild(Bone bone) {
        children.add(bone);
        bone.parent = this;
    }

    /**
     * 
     * @deprecated use {@link #updateModelTransforms() }
     */
    @Deprecated
    public final void updateWorldVectors(){
        updateModelTransforms();
    }
            
            
    /**
     * Updates the model transforms for this bone, and, possibly the attach node
     * if not null.
     * <p>
     * The model transform of this bone is computed by combining the parent's
     * model transform with this bones' local transform.
     */
    public final void updateModelTransforms() {
        if (currentWeightSum == 1f) {
            currentWeightSum = -1;
        } else if (currentWeightSum != -1f) {
            // Apply the weight to the local transform
            if (currentWeightSum == 0) {
                localRot.set(bindRot);
                localPos.set(bindPos);
                localScale.set(bindScale);
            } else {
                float invWeightSum = 1f - currentWeightSum;
                localRot.nlerp(bindRot, invWeightSum);
                localPos.interpolateLocal(bindPos, invWeightSum);
                localScale.interpolateLocal(bindScale, invWeightSum);
            }
            
            // Future invocations of transform blend will start over.
            currentWeightSum = -1;
        }
        
        if (parent != null) {
            //rotation
            parent.modelRot.mult(localRot, modelRot);

            //scale
            //For scale parent scale is not taken into account!
            // worldScale.set(localScale);
            parent.modelScale.mult(localScale, modelScale);

            //translation
            //scale and rotation of parent affect bone position            
            parent.modelRot.mult(localPos, modelPos);
            modelPos.multLocal(parent.modelScale);
            modelPos.addLocal(parent.modelPos);
        } else {
            modelRot.set(localRot);
            modelPos.set(localPos);
            modelScale.set(localScale);
        }

        if (attachNode != null) {
            attachNode.setLocalTranslation(modelPos);
            attachNode.setLocalRotation(modelRot);
            attachNode.setLocalScale(modelScale);
        }
    }

    /**
     * Updates world transforms for this bone and it's children.
     */
    public final void update() {
        this.updateModelTransforms();

        for (int i = children.size() - 1; i >= 0; i--) {
            children.get(i).update();
        }
    }

    /**
     * Saves the current bone state as its binding pose, including its children.
     */
    void setBindingPose() {
        bindPos.set(localPos);
        bindRot.set(localRot);
        bindScale.set(localScale);

        if (modelBindInversePos == null) {
            modelBindInversePos = new Vector3f();
            modelBindInverseRot = new Quaternion();
            modelBindInverseScale = new Vector3f();
        }

        // Save inverse derived position/scale/orientation, used for calculate offset transform later
        modelBindInversePos.set(modelPos);
        modelBindInversePos.negateLocal();

        modelBindInverseRot.set(modelRot);
        modelBindInverseRot.inverseLocal();

        modelBindInverseScale.set(Vector3f.UNIT_XYZ);
        modelBindInverseScale.divideLocal(modelScale);

        for (Bone b : children) {
            b.setBindingPose();
        }
    }

    /**
     * Reset the bone and it's children to bind pose.
     */
    final void reset() {
        if (!userControl) {
            localPos.set(bindPos);
            localRot.set(bindRot);
            localScale.set(bindScale);
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            children.get(i).reset();
        }
    }

     /**
     * Stores the skinning transform in the specified Matrix4f.
     * The skinning transform applies the animation of the bone to a vertex.
     * 
     * This assumes that the world transforms for the entire bone hierarchy
     * have already been computed, otherwise this method will return undefined
     * results.
     * 
     * @param outTransform
     */
    void getOffsetTransform(Matrix4f outTransform, Quaternion tmp1, Vector3f tmp2, Vector3f tmp3, Matrix3f tmp4) {
        // Computing scale
        Vector3f scale = modelScale.mult(modelBindInverseScale, tmp3);

        // Computing rotation
        Quaternion rotate = modelRot.mult(modelBindInverseRot, tmp1);

        // Computing translation
        // Translation depend on rotation and scale
        Vector3f translate = modelPos.add(rotate.mult(scale.mult(modelBindInversePos, tmp2), tmp2), tmp2);

        // Populating the matrix
        outTransform.setTransform(translate, scale, rotate.toRotationMatrix(tmp4));
    }

    /**
     * 
     * Sets the transforms of this bone in local space (relative to the parent bone)
     *
     * @param translation the translation in local space
     * @param rotation the rotation in local space
     * @param scale the scale in local space
     */
    public void setUserTransforms(Vector3f translation, Quaternion rotation, Vector3f scale) {
        if (!userControl) {
            throw new IllegalStateException("You must call setUserControl(true) in order to setUserTransform to work");
        }

        localPos.set(bindPos);
        localRot.set(bindRot);
        localScale.set(bindScale);

        localPos.addLocal(translation);
        localRot.multLocal(rotation);
        localScale.multLocal(scale);
    }

    /**
     * 
     * @param translation -
     * @param rotation -
     * @deprecated use {@link #setUserTransformsInModelSpace(com.jme3.math.Vector3f, com.jme3.math.Quaternion) }
     */
    @Deprecated
    public void setUserTransformsWorld(Vector3f translation, Quaternion rotation) {
        
    }
    /**
     * Sets the transforms of this bone in model space (relative to the root bone)
     * 
     * Must update all bones in skeleton for this to work.
     * @param translation translation in model space
     * @param rotation rotation in model space
     */
    public void setUserTransformsInModelSpace(Vector3f translation, Quaternion rotation) {
        if (!userControl) {
            throw new IllegalStateException("You must call setUserControl(true) in order to setUserTransformsInModelSpace to work");
        }

        // TODO: add scale here ???
        modelPos.set(translation);
        modelRot.set(rotation);
        
        //if there is an attached Node we need to set it's local transforms too.
        if(attachNode != null){
            attachNode.setLocalTranslation(translation);
            attachNode.setLocalRotation(rotation);
        }
    }

    /**
     * Returns the local transform of this bone combined with the given position and rotation
     * @param position a position
     * @param rotation a rotation
     */
    public Transform getCombinedTransform(Vector3f position, Quaternion rotation) {
        if(tmpTransform == null){
            tmpTransform = new Transform();
        }
        rotation.mult(localPos, tmpTransform.getTranslation()).addLocal(position);
        tmpTransform.setRotation(rotation).getRotation().multLocal(localRot);
        return tmpTransform;
    }

    /**
     * Returns the attachment node.
     * Attach models and effects to this node to make
     * them follow this bone's motions.
     */
    Node getAttachmentsNode() {
        if (attachNode == null) {
            attachNode = new Node(name + "_attachnode");
            attachNode.setUserData("AttachedBone", this);
        }
        return attachNode;
    }

    /**
     * Used internally after model cloning.
     * @param attachNode
     */
    void setAttachmentsNode(Node attachNode) {
        this.attachNode = attachNode;
    }

    /**
     * Sets the local animation transform of this bone.
     * Bone is assumed to be in bind pose when this is called.
     */
    void setAnimTransforms(Vector3f translation, Quaternion rotation, Vector3f scale) {
        if (userControl) {
            return;
        }

//        localPos.addLocal(translation);
//        localRot.multLocal(rotation);
        //localRot = localRot.mult(rotation);

        localPos.set(bindPos).addLocal(translation);
        localRot.set(bindRot).multLocal(rotation);

        if (scale != null) {
            localScale.set(bindScale).multLocal(scale);
        }
    }

    /**
     * Blends the given animation transform onto the bone's local transform.
     * <p>
     * Subsequent calls of this method stack up, with the final transformation
     * of the bone computed at {@link #updateModelTransforms() } which resets
     * the stack.
     * <p>
     * E.g. a single transform blend with weight = 0.5 followed by an
     * updateModelTransforms() call will result in final transform = transform * 0.5.
     * Two transform blends with weight = 0.5 each will result in the two
     * transforms blended together (nlerp) with blend = 0.5.
     * 
     * @param translation The translation to blend in
     * @param rotation The rotation to blend in
     * @param scale The scale to blend in
     * @param weight The weight of the transform to apply. Set to 1.0 to prevent
     * any other transform from being applied until updateModelTransforms().
     */
    void blendAnimTransforms(Vector3f translation, Quaternion rotation, Vector3f scale, float weight) {
        if (userControl) {
            return;
        }
        
        if (weight == 0) {
            // Do not apply this transform at all.
            return;
        }

        if (currentWeightSum == 1){
            return; // More than 2 transforms are being blended
        } else if (currentWeightSum == -1 || currentWeightSum == 0) {
            // Set the transform fully
            localPos.set(bindPos).addLocal(translation);
            localRot.set(bindRot).multLocal(rotation);
            if (scale != null) {
                localScale.set(bindScale).multLocal(scale);
            }
            // Set the weight. It will be applied in updateModelTransforms().
            currentWeightSum = weight;
        } else {
            // The weight is already set. 
            // Blend in the new transform.
            TempVars vars = TempVars.get();

            Vector3f tmpV = vars.vect1;
            Vector3f tmpV2 = vars.vect2;
            Quaternion tmpQ = vars.quat1;
            
            tmpV.set(bindPos).addLocal(translation);
            localPos.interpolateLocal(tmpV, weight);

            tmpQ.set(bindRot).multLocal(rotation);
            localRot.nlerp(tmpQ, weight);

            if (scale != null) {
                tmpV2.set(bindScale).multLocal(scale);
                localScale.interpolateLocal(tmpV2, weight);
            }
        
            // Ensures no new weights will be blended in the future.
            currentWeightSum = 1;
            
            vars.release();
        }
    }

    /**
     * Sets local bind transform for bone.
     * Call setBindingPose() after all of the skeleton bones' bind transforms are set to save them.
     */
    public void setBindTransforms(Vector3f translation, Quaternion rotation, Vector3f scale) {
        bindPos.set(translation);
        bindRot.set(rotation);
        //ogre.xml can have null scale values breaking this if the check is removed
        if (scale != null) {
            bindScale.set(scale);
        }

        localPos.set(translation);
        localRot.set(rotation);
        if (scale != null) {
            localScale.set(scale);
        }
    }

    private String toString(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append('-');
        }

        sb.append(name).append(" bone\n");
        for (Bone child : children) {
            sb.append(child.toString(depth + 1));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.toString(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule input = im.getCapsule(this);

        name = input.readString("name", null);
        int ver = input.getSavableVersion(Bone.class);
        if(ver < 2){
            bindPos = (Vector3f) input.readSavable("initialPos", null);
            bindRot = (Quaternion) input.readSavable("initialRot", null);
            bindScale = (Vector3f) input.readSavable("initialScale", new Vector3f(1.0f, 1.0f, 1.0f));
        }else{
            bindPos = (Vector3f) input.readSavable("bindPos", null);
            bindRot = (Quaternion) input.readSavable("bindRot", null);
            bindScale = (Vector3f) input.readSavable("bindScale", new Vector3f(1.0f, 1.0f, 1.0f));
        }
        
        attachNode = (Node) input.readSavable("attachNode", null);

        localPos.set(bindPos);
        localRot.set(bindRot);
        localScale.set(bindScale);

        ArrayList<Bone> childList = input.readSavableArrayList("children", null);
        for (int i = childList.size() - 1; i >= 0; i--) {
            this.addChild(childList.get(i));
        }

        // NOTE: Parent skeleton will call update() then setBindingPose()
        // after Skeleton has been de-serialized.
        // Therefore, worldBindInversePos and worldBindInverseRot
        // will be reconstructed based on that information.
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule output = ex.getCapsule(this);

        output.write(name, "name", null);
        output.write(attachNode, "attachNode", null);
        output.write(bindPos, "bindPos", null);
        output.write(bindRot, "bindRot", null);
        output.write(bindScale, "bindScale", new Vector3f(1.0f, 1.0f, 1.0f));
        output.writeSavableArrayList(children, "children", null);
    }

    /**
     * Sets the rotation of the bone in object space.
     * Warning: you need to call {@link #setUserControl(boolean)} with true to be able to do that operation
     * @param rot
     */
    public void setLocalRotation(Quaternion rot){
        if (!userControl) {
            throw new IllegalStateException("User control must be on bone to allow user transforms");
        }
        this.localRot.set(rot);
    }

    /**
     * Sets the position of the bone in object space.
     * Warning: you need to call {@link #setUserControl(boolean)} with true to be able to do that operation
     * @param pos
     */
    public void setLocalTranslation(Vector3f pos){
        if (!userControl) {
            throw new IllegalStateException("User control must be on bone to allow user transforms");
        }
        this.localPos.set(pos);
    }

    /**
     * Sets the scale of the bone in object space.
     * Warning: you need to call {@link #setUserControl(boolean)} with true to be able to do that operation
     * @param scale the scale to apply
     */
    public void setLocalScale(Vector3f scale){
        if (!userControl) {
            throw new IllegalStateException("User control must be on bone to allow user transforms");
        }
        this.localScale.set(scale);
    }

    /**
     * returns true if this bone can be directly manipulated by the user.
     * @see #setUserControl(boolean)
     * @return
     */
    public boolean hasUserControl(){
        return userControl;
    }
}
