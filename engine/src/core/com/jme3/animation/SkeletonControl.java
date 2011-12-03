/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.animation;

import com.jme3.export.*;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.*;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * The Skeleton control deforms a model according to a skeleton, 
 * It handles the computation of the deformation matrices and performs 
 * the transformations on the mesh
 * 
 * @author Rémy Bouquet Based on AnimControl by Kirill Vainer
 */
public class SkeletonControl extends AbstractControl implements Cloneable {

    /**
     * The skeleton of the model
     */
    private Skeleton skeleton;
    /**
     * List of targets which this controller effects.
     */
    private Mesh[] targets;
    /**
     * Used to track when a mesh was updated. Meshes are only updated
     * if they are visible in at least one camera.
     */
    private boolean wasMeshUpdated = false;

    /**
     * Serialization only. Do not use.
     */
    public SkeletonControl() {
    }

    /**
     * Creates a skeleton control.
     * The list of targets will be acquired automatically when
     * the control is attached to a node.
     * 
     * @param skeleton the skeleton
     */
    public SkeletonControl(Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    /**
     * Creates a skeleton control.
     * 
     * @param targets the meshes controlled by the skeleton
     * @param skeleton the skeleton
     */
    @Deprecated
    SkeletonControl(Mesh[] targets, Skeleton skeleton) {
        this.skeleton = skeleton;
        this.targets = targets;
    }

    private boolean isMeshAnimated(Mesh mesh) {
        return mesh.getBuffer(Type.BindPosePosition) != null;
    }

    private Mesh[] findTargets(Node node) {
        Mesh sharedMesh = null;
        ArrayList<Mesh> animatedMeshes = new ArrayList<Mesh>();

        for (Spatial child : node.getChildren()) {
            if (!(child instanceof Geometry)) {
                continue; // could be an attachment node, ignore.
            }

            Geometry geom = (Geometry) child;

            // is this geometry using a shared mesh?
            Mesh childSharedMesh = geom.getUserData(UserData.JME_SHAREDMESH);

            if (childSharedMesh != null) {
                // Don't bother with non-animated shared meshes
                if (isMeshAnimated(childSharedMesh)) {
                    // child is using shared mesh,
                    // so animate the shared mesh but ignore child
                    if (sharedMesh == null) {
                        sharedMesh = childSharedMesh;
                    } else if (sharedMesh != childSharedMesh) {
                        throw new IllegalStateException("Two conflicting shared meshes for " + node);
                    }
                }
            } else {
                Mesh mesh = geom.getMesh();
                if (isMeshAnimated(mesh)) {
                    animatedMeshes.add(mesh);
                }
            }
        }

        if (sharedMesh != null) {
            animatedMeshes.add(sharedMesh);
        }

        return animatedMeshes.toArray(new Mesh[animatedMeshes.size()]);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        if (spatial != null) {
            Node node = (Node) spatial;
            targets = findTargets(node);
        } else {
            targets = null;
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        if (!wasMeshUpdated) {
            resetToBind(); // reset morph meshes to bind pose

            Matrix4f[] offsetMatrices = skeleton.computeSkinningMatrices();

            // if hardware skinning is supported, the matrices and weight buffer
            // will be sent by the SkinningShaderLogic object assigned to the shader
            for (int i = 0; i < targets.length; i++) {
                // NOTE: This assumes that code higher up
                // Already ensured those targets are animated
                // otherwise a crash will happen in skin update
                //if (isMeshAnimated(targets[i])) {
                softwareSkinUpdate(targets[i], offsetMatrices);
                //}
            }

            wasMeshUpdated = true;
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        wasMeshUpdated = false;
    }

    void resetToBind() {
        for (Mesh mesh : targets) {
            if (isMeshAnimated(mesh)) {
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

                //reseting bind tangents if there is a bind tangent buffer
                VertexBuffer bindTangents = mesh.getBuffer(Type.BindPoseTangent);
                if (bindTangents != null) {
                    VertexBuffer tangents = mesh.getBuffer(Type.Tangent);
                    FloatBuffer tb = (FloatBuffer) tangents.getData();
                    FloatBuffer btb = (FloatBuffer) bindTangents.getData();
                    tb.clear();
                    btb.clear();
                    tb.put(btb).clear();
                }


                pb.put(bpb).clear();
                nb.put(bnb).clear();
            }
        }
    }

    public Control cloneForSpatial(Spatial spatial) {
        Node clonedNode = (Node) spatial;
        AnimControl ctrl = spatial.getControl(AnimControl.class);
        SkeletonControl clone = new SkeletonControl();
        clone.setSpatial(clonedNode);

        clone.skeleton = ctrl.getSkeleton();
        // Fix animated targets for the cloned node
        clone.targets = findTargets(clonedNode);

        // Fix attachments for the cloned node
        for (int i = 0; i < clonedNode.getQuantity(); i++) {
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

    /**
     * returns the skeleton of this control
     * @return 
     */
    public Skeleton getSkeleton() {
        return skeleton;
    }

    /**
     * sets the skeleton for this control
     * @param skeleton 
     */
//    public void setSkeleton(Skeleton skeleton) {
//        this.skeleton = skeleton;
//    }
    /**
     * returns the targets meshes of this control
     * @return 
     */
    public Mesh[] getTargets() {
        return targets;
    }

    /**
     * sets the target  meshes of this control
     * @param targets 
     */
//    public void setTargets(Mesh[] targets) {
//        this.targets = targets;
//    }
    /**
     * Update the mesh according to the given transformation matrices
     * @param mesh then mesh
     * @param offsetMatrices the transformation matrices to apply 
     */
    private void softwareSkinUpdate(Mesh mesh, Matrix4f[] offsetMatrices) {

        VertexBuffer tb = mesh.getBuffer(Type.Tangent);
        if (tb == null) {
            //if there are no tangents use the classic skinning
            applySkinning(mesh, offsetMatrices);
        } else {
            //if there are tangents use the skinning with tangents
            applySkinningTangents(mesh, offsetMatrices, tb);
        }


    }

    /**
     * Method to apply skinning transforms to a mesh's buffers    
     * @param mesh the mesh
     * @param offsetMatrices the offset matices to apply
     */
    private void applySkinning(Mesh mesh, Matrix4f[] offsetMatrices) {
        int maxWeightsPerVert = mesh.getMaxNumWeights();
        if (maxWeightsPerVert <= 0) {
            throw new IllegalStateException("Max weights per vert is incorrectly set!");
        }

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

        int iterations = (int) FastMath.ceil(fvb.capacity() / ((float) posBuf.length));
        int bufLength = posBuf.length;
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

        vars.release();

        vb.updateData(fvb);
        nb.updateData(fnb);

    }

    /**
     * Specific method for skinning with tangents to avoid cluttering the classic skinning calculation with
     * null checks that would slow down the process even if tangents don't have to be computed.
     * Also the iteration has additional indexes since tangent has 4 components instead of 3 for pos and norm
     * @param maxWeightsPerVert maximum number of weights per vertex
     * @param mesh the mesh
     * @param offsetMatrices the offsetMaytrices to apply
     * @param tb the tangent vertexBuffer
     */
    private void applySkinningTangents(Mesh mesh, Matrix4f[] offsetMatrices, VertexBuffer tb) {
        int maxWeightsPerVert = mesh.getMaxNumWeights();

        if (maxWeightsPerVert <= 0) {
            throw new IllegalStateException("Max weights per vert is incorrectly set!");
        }

        int fourMinusMaxWeights = 4 - maxWeightsPerVert;

        // NOTE: This code assumes the vertex buffer is in bind pose
        // resetToBind() has been called this frame
        VertexBuffer vb = mesh.getBuffer(Type.Position);
        FloatBuffer fvb = (FloatBuffer) vb.getData();
        fvb.rewind();

        VertexBuffer nb = mesh.getBuffer(Type.Normal);

        FloatBuffer fnb = (FloatBuffer) nb.getData();
        fnb.rewind();


        FloatBuffer ftb = (FloatBuffer) tb.getData();
        ftb.rewind();

   
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
        float[] tanBuf = vars.skinTangents;

        int iterations = (int) FastMath.ceil(fvb.capacity() / ((float) posBuf.length));
        int bufLength = 0;       
        int tanLength = 0;
        for (int i = iterations - 1; i >= 0; i--) {
            // read next set of positions and normals from native buffer
            bufLength = Math.min(posBuf.length, fvb.remaining());
            tanLength = Math.min(tanBuf.length, ftb.remaining());
            fvb.get(posBuf, 0, bufLength);
            fnb.get(normBuf, 0, bufLength);
            ftb.get(tanBuf, 0, tanLength);
            int verts = bufLength / 3;
            int idxPositions = 0;
            //tangents has their own index because of the 4 components
            int idxTangents = 0;

            // iterate vertices and apply skinning transform for each effecting bone
            for (int vert = verts - 1; vert >= 0; vert--) {
                float nmx = normBuf[idxPositions];
                float vtx = posBuf[idxPositions++];
                float nmy = normBuf[idxPositions];
                float vty = posBuf[idxPositions++];
                float nmz = normBuf[idxPositions];
                float vtz = posBuf[idxPositions++];

                float tnx = tanBuf[idxTangents++];
                float tny = tanBuf[idxTangents++];
                float tnz = tanBuf[idxTangents++];

                //skipping the 4th component of the tangent since it doesn't have to be transformed
                idxTangents++;

                float rx = 0, ry = 0, rz = 0, rnx = 0, rny = 0, rnz = 0, rtx = 0, rty = 0, rtz = 0;

                for (int w = maxWeightsPerVert - 1; w >= 0; w--) {
                    float weight = weights[idxWeights];
                    Matrix4f mat = offsetMatrices[indices[idxWeights++]];

                    rx += (mat.m00 * vtx + mat.m01 * vty + mat.m02 * vtz + mat.m03) * weight;
                    ry += (mat.m10 * vtx + mat.m11 * vty + mat.m12 * vtz + mat.m13) * weight;
                    rz += (mat.m20 * vtx + mat.m21 * vty + mat.m22 * vtz + mat.m23) * weight;

                    rnx += (nmx * mat.m00 + nmy * mat.m01 + nmz * mat.m02) * weight;
                    rny += (nmx * mat.m10 + nmy * mat.m11 + nmz * mat.m12) * weight;
                    rnz += (nmx * mat.m20 + nmy * mat.m21 + nmz * mat.m22) * weight;

                    rtx += (tnx * mat.m00 + tny * mat.m01 + tnz * mat.m02) * weight;
                    rty += (tnx * mat.m10 + tny * mat.m11 + tnz * mat.m12) * weight;
                    rtz += (tnx * mat.m20 + tny * mat.m21 + tnz * mat.m22) * weight;
                }

                idxWeights += fourMinusMaxWeights;

                idxPositions -= 3;

                normBuf[idxPositions] = rnx;
                posBuf[idxPositions++] = rx;
                normBuf[idxPositions] = rny;
                posBuf[idxPositions++] = ry;
                normBuf[idxPositions] = rnz;
                posBuf[idxPositions++] = rz;

                idxTangents -= 4;

                tanBuf[idxTangents++] = rtx;
                tanBuf[idxTangents++] = rty;
                tanBuf[idxTangents++] = rtz;

                //once again skipping the 4th component of the tangent
                idxTangents++;
            }

            fvb.position(fvb.position() - bufLength);
            fvb.put(posBuf, 0, bufLength);
            fnb.position(fnb.position() - bufLength);
            fnb.put(normBuf, 0, bufLength);
            ftb.position(ftb.position() - tanLength);
            ftb.put(tanBuf, 0, tanLength);
        }

        vars.release();

        vb.updateData(fvb);
        nb.updateData(fnb);
        tb.updateData(ftb);


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
