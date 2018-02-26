package com.jme3.anim;

import com.jme3.material.*;
import com.jme3.renderer.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.mesh.MorphTarget;
import com.jme3.shader.VarType;
import com.jme3.util.SafeArrayList;

import java.nio.FloatBuffer;

/**
 * A control that handle morph animation for Position, Normal and Tangent buffers.
 * All stock shaders only support morphing these 3 buffers, but note that MorphTargets can have any type of buffers.
 * If you want to use other types of buffers you will need a custom MorphControl and a custom shader.
 * @author Rémy Bouquet
 */
public class MorphControl extends AbstractControl {

    private static final int MAX_MORPH_BUFFERS = 14;
    private final static float MIN_WEIGHT = 0.005f;

    private SafeArrayList<Geometry> targets = new SafeArrayList<>(Geometry.class);
    private TargetLocator targetLocator = new TargetLocator();

    private boolean approximateTangents = true;
    private MatParamOverride nullNumberOfBones = new MatParamOverride(VarType.Int, "NumberOfBones", null);

    @Override
    protected void controlUpdate(float tpf) {
        // gathering geometries in the sub graph.
        // This must be done in the update phase as the gathering might add a matparam override
        targets.clear();
        this.spatial.depthFirstTraversal(targetLocator);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        for (Geometry target : targets) {
            Mesh mesh = target.getMesh();
            if (!mesh.isDirtyMorph()) {
                continue;
            }
            int nbMaxBuffers = getRemainingBuffers(mesh, rm.getRenderer());
            Material m = target.getMaterial();

            float weights[] = mesh.getMorphState();
            MorphTarget morphTargets[] = mesh.getMorphTargets();
            float matWeights[];
            MatParam param = m.getParam("MorphWeights");

            //Number of buffer to handle for each morph target
            int targetNumBuffers = getTargetNumBuffers(morphTargets[0]);
            // compute the max number of targets to send to the GPU
            int maxGPUTargets = Math.min(nbMaxBuffers, MAX_MORPH_BUFFERS) / targetNumBuffers;
            if (param == null) {
                matWeights = new float[maxGPUTargets];
                m.setParam("MorphWeights", VarType.FloatArray, matWeights);
            } else {
                matWeights = (float[]) param.getValue();
            }

            // setting the maximum number as the real number may change every frame and trigger a shader recompilation since it's bound to a define.
            m.setInt("NumberOfMorphTargets", maxGPUTargets);
            m.setInt("NumberOfTargetsBuffers", targetNumBuffers);

            int nbGPUTargets = 0;
            int nbCPUBuffers = 0;
            int boundBufferIdx = 0;
            for (int i = 0; i < morphTargets.length; i++) {
                if (weights[i] < MIN_WEIGHT) {
                    continue;
                }
                if (nbGPUTargets >= maxGPUTargets) {
                    //TODO we should fallback to CPU there.
                    nbCPUBuffers++;
                    continue;
                }
                int start = VertexBuffer.Type.MorphTarget0.ordinal();
                MorphTarget t = morphTargets[i];
                if (targetNumBuffers >= 1) {
                    activateBuffer(mesh, boundBufferIdx, start, t.getBuffer(VertexBuffer.Type.Position));
                    boundBufferIdx++;
                }
                if (targetNumBuffers >= 2) {
                    activateBuffer(mesh, boundBufferIdx, start, t.getBuffer(VertexBuffer.Type.Normal));
                    boundBufferIdx++;
                }
                if (!approximateTangents && targetNumBuffers == 3) {
                    activateBuffer(mesh, boundBufferIdx, start, t.getBuffer(VertexBuffer.Type.Tangent));
                    boundBufferIdx++;
                }
                matWeights[nbGPUTargets] = weights[i];
                nbGPUTargets++;

            }
            if (nbGPUTargets < matWeights.length) {
                for (int i = nbGPUTargets; i < matWeights.length; i++) {
                    matWeights[i] = 0;
                }
            }
        }
    }

    private void activateBuffer(Mesh mesh, int idx, int start, FloatBuffer b) {
        mesh.setBuffer(VertexBuffer.Type.values()[start + idx], 3, b);
    }

    private int getTargetNumBuffers(MorphTarget morphTarget) {
        int num = 0;
        if (morphTarget.getBuffer(VertexBuffer.Type.Position) != null) num++;
        if (morphTarget.getBuffer(VertexBuffer.Type.Normal) != null) num++;

        // if tangents are not needed we don't count the tangent buffer
        if (!approximateTangents && morphTarget.getBuffer(VertexBuffer.Type.Tangent) != null) {
            num++;
        }
        return num;
    }

    private int getRemainingBuffers(Mesh mesh, Renderer renderer) {
        int nbUsedBuffers = 0;
        for (VertexBuffer vb : mesh.getBufferList().getArray()) {
            boolean isMorphBuffer = vb.getBufferType().ordinal() >= VertexBuffer.Type.MorphTarget0.ordinal() && vb.getBufferType().ordinal() <= VertexBuffer.Type.MorphTarget9.ordinal();
            if (vb.getBufferType() == VertexBuffer.Type.Index || isMorphBuffer) continue;
            if (vb.getUsage() != VertexBuffer.Usage.CpuOnly) {
                nbUsedBuffers++;
            }
        }
        return renderer.getLimits().get(Limits.VertexAttributes) - nbUsedBuffers;
    }

    public void setApproximateTangents(boolean approximateTangents) {
        this.approximateTangents = approximateTangents;
    }

    public boolean isApproximateTangents() {
        return approximateTangents;
    }

    private class TargetLocator extends SceneGraphVisitorAdapter {
        @Override
        public void visit(Geometry geom) {
            MatParam p = geom.getMaterial().getMaterialDef().getMaterialParam("MorphWeights");
            if (p == null) {
                return;
            }
            Mesh mesh = geom.getMesh();
            if (mesh != null && mesh.hasMorphTargets()) {
                targets.add(geom);
                // If the mesh is in a subgraph of a node with a SkinningControl it might have hardware skinning activated through mat param override even if it's not skinned.
                // this code makes sure that if the mesh has no hardware skinning buffers hardware skinning won't be activated.
                // this is important, because if HW skinning is activated the shader will declare 2 additional useless attributes,
                // and we desperately need all the attributes we can find for Morph animation.
                if (mesh.getBuffer(VertexBuffer.Type.HWBoneIndex) == null && !geom.getLocalMatParamOverrides().contains(nullNumberOfBones)) {
                    geom.addMatParamOverride(nullNumberOfBones);
                }
            }
        }
    }
}
