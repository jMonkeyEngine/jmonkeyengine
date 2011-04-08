/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.animation;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
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

/**
 *
 * @author Nehon
 */
public class SkeletonControl extends AbstractControl implements Savable, Cloneable {

    /**
     * The skelrton of the model
     */
    private Skeleton skeleton;
    /**
     * List of targets which this controller effects.
     */
    Mesh[] targets;

    public SkeletonControl() {
    }

    public SkeletonControl(Node model, Mesh[] targets, Skeleton skeleton) {
        super(model);
        this.skeleton = skeleton;
        this.targets = targets;
    }

    @Override
    protected void controlUpdate(float tpf) {
        resetToBind(); // reset morph meshes to bind pose 

        Matrix4f[] offsetMatrices = skeleton.computeSkinningMatrices();

        // if hardware skinning is supported, the matrices and weight buffer
        // will be sent by the SkinningShaderLogic object assigned to the shader
        for (int i = 0; i < targets.length; i++) {
            // only update targets with bone-vertex assignments
            if (targets[i].getBuffer(Type.BoneIndex) != null) {
                softwareSkinUpdate(targets[i], offsetMatrices);
            }
        }
    }

    void resetToBind() {
        for (int i = 0; i < targets.length; i++) {
            Mesh mesh = targets[i];
            if (targets[i].getBuffer(Type.BindPosePosition) != null) {
                VertexBuffer bi = mesh.getBuffer(Type.BoneIndex);
                ByteBuffer bib = (ByteBuffer) bi.getData();
                if (!bib.hasArray()) {
                    mesh.prepareForAnim(true); // prepare for software animation
                }
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

    private void softwareSkinUpdate(Mesh mesh, Matrix4f[] offsetMatrices) {
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
        assert vars.lock();

        float[] posBuf = vars.skinPositions;
        float[] normBuf = vars.skinNormals;

        int iterations = (int) FastMath.ceil(fvb.capacity() / ((float) posBuf.length));
        int bufLength = posBuf.length * 3;
        for (int i = iterations - 1; i >= 0; i--) {
            // read next set of positions and normals from native buffer
            bufLength = Math.min(posBuf.length, fvb.remaining());
            fvb.get(posBuf, 0, bufLength);
            fnb.get(normBuf, 0, bufLength);
            int verts = bufLength / 3;
            int idxPositions = 0;

            // iterate vertices and apply skinning transform for each effecting bone
            for (int vert = verts - 1; vert >= 0; vert--) {
                float nmx = normBuf[idxPositions];
                float vtx = posBuf[idxPositions++];
                float nmy = normBuf[idxPositions];
                float vty = posBuf[idxPositions++];
                float nmz = normBuf[idxPositions];
                float vtz = posBuf[idxPositions++];

                float rx = 0, ry = 0, rz = 0, rnx = 0, rny = 0, rnz = 0;

                for (int w = maxWeightsPerVert - 1; w >= 0; w--) {
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


            fvb.position(fvb.position() - bufLength);
            fvb.put(posBuf, 0, bufLength);
            fnb.position(fnb.position() - bufLength);
            fnb.put(normBuf, 0, bufLength);
        }

        assert vars.unlock();

        vb.updateData(fvb);
        nb.updateData(fnb);

//        mesh.updateBound();
    }

    final void reset() {
        resetToBind();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    public Control cloneForSpatial(Spatial spatial) {
        Node clonedNode = (Node) spatial;
        AnimControl ctrl = spatial.getControl(AnimControl.class);
        SkeletonControl clone = new SkeletonControl();
        clone.setSpatial(clonedNode);

        clone.skeleton = ctrl.getSkeleton();
        Mesh[] meshes = new Mesh[targets.length];
        for (int i = 0; i < meshes.length; i++) {
            meshes[i] = ((Geometry) clonedNode.getChild(i)).getMesh();
        }
        for (int i = meshes.length; i < clonedNode.getQuantity(); i++) {
            // go through attachment nodes, apply them to correct bone
            Spatial child = clonedNode.getChild(i);
            if (child instanceof Node) {
                Node clonedAttachNode = (Node) child;
                Bone originalBone = (Bone) clonedAttachNode.getUserData("AttachedBone");

                if (originalBone != null) {
                    Bone clonedBone = clone.skeleton.getBone(originalBone.getName());

                    clonedAttachNode.setUserData("AttachedBone", clonedBone);
                    clonedBone.setAttachmentsNode(clonedAttachNode);
                }
            }
        }
        clone.targets = meshes;
        return clone;

    }

    /**
     * 
     * @param boneName the name of the bone
     * @return the node attached to this bone    
     */
    public Node getAttachmentsNode(String boneName) {
        Bone b = skeleton.getBone(boneName);
        if (b == null) {
            throw new IllegalArgumentException("Given bone name does not exist "
                    + "in the skeleton.");
        }

        Node n = b.getAttachmentsNode();
        Node model = (Node) spatial;
        model.attachChild(n);
        return n;
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public void setSkeleton(Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    public Mesh[] getTargets() {
        return targets;
    }

    public void setTargets(Mesh[] targets) {
        this.targets = targets;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(targets, "targets", null);
        oc.write(skeleton, "skeleton", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        Savable[] sav = in.readSavableArray("targets", null);
        if (sav != null) {
            targets = new Mesh[sav.length];
            System.arraycopy(sav, 0, targets, 0, sav.length);
        }
        skeleton = (Skeleton) in.readSavable("skeleton", null);
    }
}
