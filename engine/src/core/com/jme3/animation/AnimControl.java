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

package com.jme3.animation;

import com.jme3.bullet.control.RagdollControl;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * <code>AnimControl</code> is a Spatial control that allows manipulation
 * of skeletal animation.
 *
 * The control currently supports:
 * 1) Animation blending/transitions
 * 2) Multiple animation channels
 * 3) Multiple skins
 * 4) Animation event listeners
 * 5) Animated model cloning
 * 6) Animated model binary import/export
 *
 * Planned:
 * 1) Hardware skinning
 * 2) Morph/Pose animation
 * 3) Attachments
 * 4) Add/remove skins
 *
 * @author Kirill Vainer
 */
public final class AnimControl extends AbstractControl implements Savable, Cloneable {

    /**
     * List of targets which this controller effects.
     */
    Mesh[] targets;

    /**
     * Skeleton object must contain corresponding data for the targets' weight buffers.
     */
    Skeleton skeleton;

    /**
     * List of animations
     */
    HashMap<String, BoneAnimation> animationMap;

    /**
     * Animation channels
     */
    transient ArrayList<AnimChannel> channels
            = new ArrayList<AnimChannel>();

    transient ArrayList<AnimEventListener> listeners
            = new ArrayList<AnimEventListener>();

    /**
     * Create a new <code>AnimControl</code> that will animate the given skins
     * using the skeleton and provided animations.
     *
     * @param model The root node of all the skins specified in
     * <code>meshes</code> argument.
     * @param meshes The skins, or meshes, to animate. Should have
     * properly set BoneIndex and BoneWeight buffers.
     * @param skeleton The skeleton structure represents a bone hierarchy
     * to be animated.
     */
    public AnimControl(Node model, Mesh[] meshes, Skeleton skeleton){
        super(model);

        this.skeleton = skeleton;
        this.targets = meshes;
        reset();
    }

    /**
     * Used only for Saving/Loading models (all parameters of the non-default
     * constructor are restored from the saved model, but the object must be
     * constructed beforehand)
     */
    public AnimControl() {
    }

    public Control cloneForSpatial(Spatial spatial){
        try {
            Node clonedNode = (Node) spatial;
            AnimControl clone = (AnimControl) super.clone();
            clone.spatial  = spatial;
            clone.skeleton = new Skeleton(skeleton);
            Mesh[] meshes = new Mesh[targets.length];
            for (int i = 0; i < meshes.length; i++) {
                meshes[i] = ((Geometry) clonedNode.getChild(i)).getMesh();
            }
            for (int i = meshes.length; i < clonedNode.getQuantity(); i++){
                // go through attachment nodes, apply them to correct bone
                Spatial child = clonedNode.getChild(i);
                if (child instanceof Node){
                    Node clonedAttachNode = (Node) child;
                    Bone originalBone     = (Bone) clonedAttachNode.getUserData("AttachedBone");

                    if (originalBone != null){
                        Bone clonedBone       = clone.skeleton.getBone(originalBone.getName());

                        clonedAttachNode.setUserData("AttachedBone", clonedBone);
                        clonedBone.setAttachmentsNode(clonedAttachNode);
                    }
                }
            }
            clone.targets = meshes;
            clone.channels = new ArrayList<AnimChannel>();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     * @param animations Set the animations that this <code>AnimControl</code>
     * will be capable of playing. The animations should be compatible
     * with the skeleton given in the constructor.
     */
    public void setAnimations(HashMap<String, BoneAnimation> animations){
        animationMap = animations;
    }

    /**
     * Retrieve an animation from the list of animations.
     * @param name The name of the animation to retrieve.
     * @return The animation corresponding to the given name, or null, if no
     * such named animation exists.
     */
    public BoneAnimation getAnim(String name){
        return animationMap.get(name);
    }

    /**
     * Adds an animation to be available for playing to this
     * <code>AnimControl</code>.
     * @param anim The animation to add.
     */
    public void addAnim(BoneAnimation anim){
        animationMap.put(anim.getName(), anim);
    }

    /**
     * Remove an animation so that it is no longer available for playing.
     * @param anim The animation to remove.
     */
    public void removeAnim(BoneAnimation anim){
        if (!animationMap.containsKey(anim.getName()))
            throw new IllegalArgumentException("Given animation does not exist " +
                                               "in this AnimControl");

        animationMap.remove(anim.getName());
    }

    public Node getAttachmentsNode(String boneName) {
        Bone b = skeleton.getBone(boneName);
        if (b == null)
            throw new IllegalArgumentException("Given bone name does not exist " +
                                               "in the skeleton.");

        Node n = b.getAttachmentsNode();
        Node model = (Node) spatial;
        model.attachChild(n);
        return n;
    }

    /**
     * Create a new animation channel, by default assigned to all bones
     * in the skeleton.
     * 
     * @return A new animation channel for this <code>AnimControl</code>.
     */
    public AnimChannel createChannel(){
        AnimChannel channel = new AnimChannel(this);
        channels.add(channel);
        return channel;
    }

    /**
     * Return the animation channel at the given index.
     * @param index The index, starting at 0, to retrieve the <code>AnimChannel</code>.
     * @return The animation channel at the given index, or throws an exception
     * if the index is out of bounds.
     *
     * @throws IndexOutOfBoundsException If no channel exists at the given index.
     */
    public AnimChannel getChannel(int index){
        return channels.get(index);
    }

    /**
     * @return The number of channels that are controlled by this
     * <code>AnimControl</code>.
     *
     * @see AnimControl#createChannel()
     */
    public int getNumChannels(){
        return channels.size();
    }

    /**
     * Clears all the channels that were created.
     *
     * @see AnimControl#createChannel()
     */
    public void clearChannels(){
        channels.clear();
    }

    /**
     * @return The skeleton of this <code>AnimControl</code>.
     */
    public Skeleton getSkeleton() {
        return skeleton;
    }

    /**
     * @return The targets, or skins, being influenced by this
     * <code>AnimControl</code>.
     */
    public Mesh[] getTargets() {
        return targets;
    }

    /**
     * Adds a new listener to receive animation related events.
     * @param listener The listener to add.
     */
    public void addListener(AnimEventListener listener){
        if (listeners.contains(listener))
            throw new IllegalArgumentException("The given listener is already " +
                                               "registed at this AnimControl");

        listeners.add(listener);
    }

    /**
     * Removes the given listener from listening to events.
     * @param listener
     * @see AnimControl#addListener(com.jme3.animation.AnimEventListener)
     */
    public void removeListener(AnimEventListener listener){
        if (!listeners.remove(listener))
            throw new IllegalArgumentException("The given listener is not " +
                                               "registed at this AnimControl");
    }

    /**
     * Clears all the listeners added to this <code>AnimControl</code>
     *
     * @see AnimControl#addListener(com.jme3.animation.AnimEventListener)
     */
    public void clearListeners(){
        listeners.clear();
    }

    void notifyAnimChange(AnimChannel channel, String name){
        for (int i = 0; i < listeners.size(); i++){
            listeners.get(i).onAnimChange(this, channel, name);
        }
    }

    void notifyAnimCycleDone(AnimChannel channel, String name){
        for (int i = 0; i < listeners.size(); i++){
            listeners.get(i).onAnimCycleDone(this, channel, name);
        }
    }

    final void reset(){
        resetToBind();
        if (skeleton != null){
            skeleton.resetAndUpdate();
        }
    }

    void resetToBind(){
        for (int i = 0; i < targets.length; i++){
            Mesh mesh = targets[i];
            if (targets[i].getBuffer(Type.BindPosePosition) != null){
                VertexBuffer bi = mesh.getBuffer(Type.BoneIndex);
                ByteBuffer bib = (ByteBuffer) bi.getData();
                if (!bib.hasArray())
                    mesh.prepareForAnim(true); // prepare for software animation
                    
                VertexBuffer bindPos = mesh.getBuffer(Type.BindPosePosition);
                VertexBuffer bindNorm = mesh.getBuffer(Type.BindPoseNormal);
                VertexBuffer pos = mesh.getBuffer(Type.Position);
                VertexBuffer norm = mesh.getBuffer(Type.Normal);
                FloatBuffer pb = (FloatBuffer) pos.getData();
                FloatBuffer nb = (FloatBuffer) norm.getData();
                FloatBuffer bpb = (FloatBuffer) bindPos.getData();
                FloatBuffer bnb = (FloatBuffer) bindNorm.getData();
                pb.clear();
                nb.clear();
                bpb.clear();
                bnb.clear();
                pb.put(bpb).clear();
                nb.put(bnb).clear();
            }
        }
    }

    /**
     * @return The names of all animations that this <code>AnimControl</code>
     * can play.
     */
    public Collection<String> getAnimationNames(){
        return animationMap.keySet();
    }

    /**
     * Returns the length of the given named animation.
     * @param name The name of the animation
     * @return The length of time, in seconds, of the named animation.
     */
    public float getAnimationLength(String name){
        BoneAnimation a = animationMap.get(name);
        if (a == null)
            throw new IllegalArgumentException("The animation " + name +
                                               " does not exist in this AnimControl");

        return a.getLength();
    }
    
    private RagdollControl ragdoll=null;

    public void setRagdoll(RagdollControl ragdoll) {
        this.ragdoll = ragdoll;
    }
    

    @Override
    protected void controlUpdate(float tpf) {
        resetToBind(); // reset morph meshes to bind pose
        skeleton.reset(); // reset skeleton to bind pose

        for (int i = 0; i < channels.size(); i++){
            channels.get(i).update(tpf);
        }

        skeleton.updateWorldVectors();
        // here update the targets vertices if no hardware skinning supported

        if(ragdoll!=null){
            ragdoll.update(tpf);
        }
        
        Matrix4f[] offsetMatrices = skeleton.computeSkinningMatrices();

        // if hardware skinning is supported, the matrices and weight buffer
        // will be sent by the SkinningShaderLogic object assigned to the shader
        for (int i = 0; i < targets.length; i++){
            // only update targets with bone-vertex assignments
            if (targets[i].getBuffer(Type.BoneIndex) != null)
                softwareSkinUpdate(targets[i], offsetMatrices);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    private void softwareSkinUpdate(Mesh mesh, Matrix4f[] offsetMatrices){
        int maxWeightsPerVert = mesh.getMaxNumWeights();
        int fourMinusMaxWeights = 4 - maxWeightsPerVert;

        // NOTE: This code assumes the vertex buffer is in bind pose
        // resetToBind() has been called this frame
        VertexBuffer vb = mesh.getBuffer(Type.Position);
        FloatBuffer fvb = (FloatBuffer) vb.getData();
        fvb.rewind();

        VertexBuffer nb = mesh.getBuffer(Type.Normal);
        FloatBuffer fnb = (FloatBuffer) nb.getData();
        fnb.rewind();

        // get boneIndexes and weights for mesh
        ByteBuffer ib = (ByteBuffer) mesh.getBuffer(Type.BoneIndex).getData();
        FloatBuffer wb = (FloatBuffer) mesh.getBuffer(Type.BoneWeight).getData();

        ib.rewind();
        wb.rewind();

        float[] weights = wb.array();
        byte[] indices = ib.array();
        int idxWeights = 0;

        TempVars vars = TempVars.get();
        float[] posBuf = vars.skinPositions;
        float[] normBuf = vars.skinNormals;

        int iterations = (int) FastMath.ceil(fvb.capacity() / ((float)posBuf.length));
        int bufLength = posBuf.length * 3;
        for (int i = iterations-1; i >= 0; i--){
            // read next set of positions and normals from native buffer
            bufLength = Math.min(posBuf.length, fvb.remaining());
            fvb.get(posBuf, 0, bufLength);
            fnb.get(normBuf, 0, bufLength);
            int verts = bufLength / 3;
            int idxPositions = 0;

            // iterate vertices and apply skinning transform for each effecting bone
            for (int vert = verts - 1; vert >= 0; vert--){
                float nmx = normBuf[idxPositions];
                float vtx = posBuf[idxPositions++];
                float nmy = normBuf[idxPositions];
                float vty = posBuf[idxPositions++];
                float nmz = normBuf[idxPositions];
                float vtz = posBuf[idxPositions++];

                float rx=0, ry=0, rz=0, rnx=0, rny=0, rnz=0;

                for (int w = maxWeightsPerVert - 1; w >= 0; w--){
                    float weight = weights[idxWeights];
                    Matrix4f mat = offsetMatrices[indices[idxWeights++]];

                    rx += (mat.m00 * vtx + mat.m01 * vty + mat.m02 * vtz + mat.m03) * weight;
                    ry += (mat.m10 * vtx + mat.m11 * vty + mat.m12 * vtz + mat.m13) * weight;
                    rz += (mat.m20 * vtx + mat.m21 * vty + mat.m22 * vtz + mat.m23) * weight;

                    rnx += (nmx * mat.m00 + nmy * mat.m01 + nmz * mat.m02) * weight;
                    rny += (nmx * mat.m10 + nmy * mat.m11 + nmz * mat.m12) * weight;
                    rnz += (nmx * mat.m20 + nmy * mat.m21 + nmz * mat.m22) * weight;
                }

                idxWeights += fourMinusMaxWeights;

                idxPositions -= 3;
                normBuf[idxPositions] = rnx;
                posBuf[idxPositions++] = rx;
                normBuf[idxPositions] = rny;
                posBuf[idxPositions++] = ry;
                normBuf[idxPositions] = rnz;
                posBuf[idxPositions++] = rz;
            }


            fvb.position(fvb.position()-bufLength);
            fvb.put(posBuf, 0, bufLength);
            fnb.position(fnb.position()-bufLength);
            fnb.put(normBuf, 0, bufLength);
        }

        vb.updateData(fvb);
        nb.updateData(fnb);

//        mesh.updateBound();
    }

    @Override
    public void write(JmeExporter ex) throws IOException{
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(targets, "targets", null);
        oc.write(skeleton, "skeleton", null);
        oc.writeStringSavableMap(animationMap, "animations", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException{
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        Savable[] sav = in.readSavableArray("targets", null);
        if (sav != null){
            targets = new Mesh[sav.length];
            System.arraycopy(sav, 0, targets, 0, sav.length);
        }
        skeleton = (Skeleton) in.readSavable("skeleton", null);
        animationMap = (HashMap<String, BoneAnimation>) in.readStringSavableMap("animations", null);
    }

}
