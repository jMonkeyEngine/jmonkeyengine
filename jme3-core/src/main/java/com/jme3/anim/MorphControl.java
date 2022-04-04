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
package com.jme3.anim;

import com.jme3.export.*;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.renderer.*;
import com.jme3.scene.*;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.mesh.MorphTarget;
import com.jme3.shader.VarType;
import com.jme3.util.BufferUtils;
import com.jme3.util.SafeArrayList;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A control that handle morph animation for Position, Normal and Tangent buffers.
 * All stock shaders only support morphing these 3 buffers, but note that MorphTargets can have any type of buffers.
 * If you want to use other types of buffers you will need a custom MorphControl and a custom shader.
 *
 * Note that if morphed children are attached to or detached from the sub graph after the MorphControl is added to
 * spatial, you must detach and attach the control again for the changes to get reflected.
 *
 * @author Rémy Bouquet
 */
public class MorphControl extends AbstractControl implements Savable {

    private static final Logger logger = Logger.getLogger(MorphControl.class.getName());

    private static final int MAX_MORPH_BUFFERS = 14;
    private final static float MIN_WEIGHT = 0.005f;

    private static final String TAG_APPROXIMATE = "approximateTangents";
    private static final String TAG_TARGETS = "targets";

    private SafeArrayList<Geometry> targets = new SafeArrayList<>(Geometry.class);
    private TargetLocator targetLocator = new TargetLocator();

    private boolean approximateTangents = true;
    private MatParamOverride nullNumberOfBones = new MatParamOverride(VarType.Int, "NumberOfBones", null);

    private float[] tmpPosArray;
    private float[] tmpNormArray;
    private float[] tmpTanArray;

    private static final VertexBuffer.Type bufferTypes[] = VertexBuffer.Type.values();

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        // Remove matparam override from the old targets
        for (Geometry target : targets.getArray()) {
            target.removeMatParamOverride(nullNumberOfBones);
        }

        // gathering geometries in the sub graph.
        // This must not be done in the render phase as the gathering might add a matparam override
        // which then will throw an IllegalStateException if done in the render phase.
        targets.clear();
        if (spatial != null) {
            spatial.depthFirstTraversal(targetLocator);
        }
    }

    @Override
    protected void controlUpdate(float tpf) {

    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        for (Geometry geom : targets.getArray()) {
            Mesh mesh = geom.getMesh();
            if (!geom.isDirtyMorph()) {
                continue;
            }

            Material m = geom.getMaterial();
            float weights[] = geom.getMorphState();
            MorphTarget morphTargets[] = mesh.getMorphTargets();
            float matWeights[];
            //Number of buffer to handle for each morph target
            int targetNumBuffers = getTargetNumBuffers(morphTargets[0]);

            int maxGPUTargets = getMaxGPUTargets(rm, geom, m, targetNumBuffers);

            MatParam param2 = m.getParam("MorphWeights");
            matWeights = (float[]) param2.getValue();

            int nbGPUTargets = 0;
            int lastGpuTargetIndex = 0;
            int boundBufferIdx = 0;
            float cpuWeightSum = 0;
            // binding the morphTargets buffer to the mesh morph buffers
            for (int i = 0; i < morphTargets.length; i++) {
                // discard weights below the threshold
                if (weights[i] < MIN_WEIGHT) {
                    continue;
                }
                if (nbGPUTargets >= maxGPUTargets) {
                    // we already bound all the available gpu slots we need to merge the remaining morph targets.
                    cpuWeightSum += weights[i];
                    continue;
                }
                lastGpuTargetIndex = i;
                // binding the morph target's buffers to the mesh morph buffers.
                MorphTarget t = morphTargets[i];
                boundBufferIdx = bindMorphTargetBuffer(mesh, targetNumBuffers, boundBufferIdx, t);
                // setting the weight in the mat param array
                matWeights[nbGPUTargets] = weights[i];
                nbGPUTargets++;
            }

            if (nbGPUTargets < matWeights.length) {
                // if we have less simultaneous GPU targets than the length of the weight array, the array is padded with 0
                for (int i = nbGPUTargets; i < matWeights.length; i++) {
                    matWeights[i] = 0;
                }
            } else if (cpuWeightSum > 0) {
                // we have more simultaneous morph targets than available gpu slots,
                // we merge the additional morph targets and bind them to the last gpu slot
                MorphTarget mt = geom.getFallbackMorphTarget();
                if (mt == null) {
                    mt = initCpuMorphTarget(geom);
                    geom.setFallbackMorphTarget(mt);
                }
                // adding the last Gpu target weight
                cpuWeightSum += matWeights[nbGPUTargets - 1];
                ensureTmpArraysCapacity(geom.getVertexCount() * 3, targetNumBuffers);

                // merging all remaining targets in tmp arrays
                for (int i = lastGpuTargetIndex; i < morphTargets.length; i++) {
                    if (weights[i] < MIN_WEIGHT) {
                        continue;
                    }
                    float weight = weights[i] / cpuWeightSum;
                    MorphTarget t = geom.getMesh().getMorphTargets()[i];
                    mergeMorphTargets(targetNumBuffers, weight, t, i == lastGpuTargetIndex);
                }

                // writing the tmp arrays to the float buffer
                writeCpuBuffer(targetNumBuffers, mt);

                // binding the merged morph target
                bindMorphTargetBuffer(mesh, targetNumBuffers, (nbGPUTargets - 1) * targetNumBuffers, mt);

                // setting the eight of the merged targets
                matWeights[nbGPUTargets - 1] = cpuWeightSum;
            }
            geom.setDirtyMorph(false);
        }
    }

    private int getMaxGPUTargets(RenderManager rm, Geometry geom, Material mat, int targetNumBuffers) {
        if (geom.getNbSimultaneousGPUMorph() > -1) {
            return geom.getNbSimultaneousGPUMorph();
        }

        // Evaluate the number of CPU slots remaining for morph buffers.
        int nbMaxBuffers = getRemainingBuffers(geom.getMesh(), rm.getRenderer());

        int realNumTargetsBuffers = geom.getMesh().getMorphTargets().length * targetNumBuffers;

        // compute the max number of targets to send to the GPU
        int maxGPUTargets = Math.min(realNumTargetsBuffers, Math.min(nbMaxBuffers, MAX_MORPH_BUFFERS)) / targetNumBuffers;

        MatParam param = mat.getParam("MorphWeights");
        if (param == null) {
            // init the mat param if it doesn't exist.
            float[] wts = new float[maxGPUTargets];
            mat.setParam("MorphWeights", VarType.FloatArray, wts);
        }

        mat.setInt("NumberOfTargetsBuffers", targetNumBuffers);

        // test compile the shader to find the accurate number of remaining attributes slots
        boolean compilationOk = false;
        // Note that if ever the shader has an unrelated issue we want to break at some point, hence the maxGPUTargets > 0
        while (!compilationOk && maxGPUTargets > 0) {
            // setting the maximum number as the real number may change every frame and trigger a shader recompilation since it's bound to a define.
            mat.setInt("NumberOfMorphTargets", maxGPUTargets);
            try {
                // preload the spatial. this will trigger a shader compilation that will fail if the number of attributes is over the limit.
                rm.preloadScene(spatial);
                compilationOk = true;
            } catch (RendererException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "{0}: failed at {1}", new Object[]{geom.getName(), maxGPUTargets});
                }
                // the compilation failed let's decrement the number of targets and try again.
                maxGPUTargets--;
            }
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "{0}: {1}", new Object[]{geom.getName(), maxGPUTargets});
        }
        // set the number of GPU morph on the geom to not have to recompute it next frame.
        geom.setNbSimultaneousGPUMorph(maxGPUTargets);
        return maxGPUTargets;
    }

    private int bindMorphTargetBuffer(Mesh mesh, int targetNumBuffers, int boundBufferIdx, MorphTarget t) {
        int start = VertexBuffer.Type.MorphTarget0.ordinal();
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
        return boundBufferIdx;
    }

    private void writeCpuBuffer(int targetNumBuffers, MorphTarget mt) {
        if (targetNumBuffers >= 1) {
            FloatBuffer dest = mt.getBuffer(VertexBuffer.Type.Position);
            dest.rewind();
            dest.put(tmpPosArray, 0, dest.capacity());
        }
        if (targetNumBuffers >= 2) {
            FloatBuffer dest = mt.getBuffer(VertexBuffer.Type.Normal);
            dest.rewind();
            dest.put(tmpNormArray, 0, dest.capacity());
        }
        if (!approximateTangents && targetNumBuffers == 3) {
            FloatBuffer dest = mt.getBuffer(VertexBuffer.Type.Tangent);
            dest.rewind();
            dest.put(tmpTanArray, 0, dest.capacity());
        }
    }

    private void mergeMorphTargets(int targetNumBuffers, float weight, MorphTarget t, boolean init) {
        if (targetNumBuffers >= 1) {
            mergeTargetBuffer(tmpPosArray, weight, t.getBuffer(VertexBuffer.Type.Position), init);
        }
        if (targetNumBuffers >= 2) {
            mergeTargetBuffer(tmpNormArray, weight, t.getBuffer(VertexBuffer.Type.Normal), init);
        }
        if (!approximateTangents && targetNumBuffers == 3) {
            mergeTargetBuffer(tmpTanArray, weight, t.getBuffer(VertexBuffer.Type.Tangent), init);
        }
    }

    private void ensureTmpArraysCapacity(int capacity, int targetNumBuffers) {
        if (targetNumBuffers >= 1) {
            tmpPosArray = ensureCapacity(tmpPosArray, capacity);
        }
        if (targetNumBuffers >= 2) {
            tmpNormArray = ensureCapacity(tmpNormArray, capacity);
        }
        if (!approximateTangents && targetNumBuffers == 3) {
            tmpTanArray = ensureCapacity(tmpTanArray, capacity);
        }
    }

    private void mergeTargetBuffer(float[] array, float weight, FloatBuffer src, boolean init) {
        src.rewind();
        for (int j = 0; j < src.capacity(); j++) {
            if (init) {
                array[j] = 0;
            }
            array[j] += weight * src.get();
        }
    }

    private void activateBuffer(Mesh mesh, int idx, int start, FloatBuffer b) {
        VertexBuffer.Type t = bufferTypes[start + idx];
        VertexBuffer vb = mesh.getBuffer(t);
        // only set the buffer if it's different
        if (vb == null || vb.getData() != b) {
            mesh.setBuffer(t, 3, b);
        }
    }

    private float[] ensureCapacity(float[] tmpArray, int size) {
        if (tmpArray == null || tmpArray.length < size) {
            return new float[size];
        }
        return tmpArray;
    }

    private MorphTarget initCpuMorphTarget(Geometry geom) {
        MorphTarget res = new MorphTarget();
        MorphTarget mt = geom.getMesh().getMorphTargets()[0];
        FloatBuffer b = mt.getBuffer(VertexBuffer.Type.Position);
        if (b != null) {
            res.setBuffer(VertexBuffer.Type.Position, BufferUtils.createFloatBuffer(b.capacity()));
        }
        b = mt.getBuffer(VertexBuffer.Type.Normal);
        if (b != null) {
            res.setBuffer(VertexBuffer.Type.Normal, BufferUtils.createFloatBuffer(b.capacity()));
        }
        if (!approximateTangents) {
            b = mt.getBuffer(VertexBuffer.Type.Tangent);
            if (b != null) {
                res.setBuffer(VertexBuffer.Type.Tangent, BufferUtils.createFloatBuffer(b.capacity()));
            }
        }
        return res;
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

    /**
     * Computes the number of remaining buffers on this mesh.
     * This is supposed to give a hint on how many attributes will be used in the material and computes the remaining available slots for the morph attributes.
     * However, the shader can declare attributes that are not used and not bound to a real buffer.
     * That's why we attempt to compile the shader later on to avoid any compilation crash.
     * This method is here to avoid too much render test iteration.
     *
     * @param mesh
     * @param renderer
     * @return
     */
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

    /**
     * Alter whether this Control approximates tangents.
     *
     * @param approximateTangents true to approximate tangents, false to get
     * them from a VertexBuffer
     */
    public void setApproximateTangents(boolean approximateTangents) {
        this.approximateTangents = approximateTangents;
    }

    /**
     * Test whether this Control approximates tangents.
     *
     * @return true if approximating tangents, false if getting them from a
     * VertexBuffer
     */
    public boolean isApproximateTangents() {
        return approximateTangents;
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned Control into a deep-cloned one, using the specified Cloner
     * and original to resolve copied fields.
     *
     * @param cloner the Cloner that's cloning this Control (not null, modified)
     * @param original the instance from which this Control was shallow-cloned
     * (not null, unaffected)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        targets = cloner.clone(targets);
        targetLocator = new TargetLocator();
        nullNumberOfBones = cloner.clone(nullNumberOfBones);
        tmpPosArray = null;
        tmpNormArray = null;
        tmpTanArray = null;
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public MorphControl jmeClone() {
        try {
            MorphControl clone = (MorphControl) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * De-serialize this Control from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from the importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);
        approximateTangents = capsule.readBoolean(TAG_APPROXIMATE, true);
        targets.addAll(capsule.readSavableArrayList(TAG_TARGETS, null));
    }

    /**
     * Serialize this Control to the specified exporter, for example when saving
     * to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from the exporter
     */
    @Override
    @SuppressWarnings("unchecked")
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule capsule = exporter.getCapsule(this);
        capsule.write(approximateTangents, TAG_APPROXIMATE, true);
        capsule.writeSavableArrayList(new ArrayList(targets), TAG_TARGETS, null);
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
