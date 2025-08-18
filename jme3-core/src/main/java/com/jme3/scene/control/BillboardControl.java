/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.scene.control;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;

import java.io.IOException;

/**
 * <code>BillboardControl</code> is a special control that makes a spatial always
 * face the camera. This is useful for health bars, or other 2D elements
 * that should always be oriented towards the viewer in a 3D scene.
 * <p>
 * The alignment can be customized to different modes:
 * <ul>
 * <li>Screen: The billboard always faces the screen, keeping its 'up' vector aligned with camera's 'up'.</li>
 * <li>Camera: The billboard always faces the camera position directly.</li>
 * <li>AxialY: The billboard faces the camera but keeps its local Y-axis fixed.</li>
 * <li>AxialZ: The billboard faces the camera but keeps its local Z-axis fixed.</li>
 * </ul>
 */
public class BillboardControl extends AbstractControl {

    // Member variables for calculations, reused to avoid constant object allocation.
    private final Matrix3f tempMat3 = new Matrix3f();
    private final Vector3f tempDir = new Vector3f();
    private final Vector3f tempLeft = new Vector3f();

    /**
     * The current alignment mode for the billboard.
     */
    private Alignment alignment = Alignment.Screen;

    /**
     * Determines how the billboard is aligned to the screen/camera.
     */
    public enum Alignment {
        /**
         * Aligns this Billboard to the screen.
         */
        Screen,
        /**
         * Aligns this Billboard to the camera position.
         */
        Camera,
        /**
         * Aligns this Billboard to the screen, but keeps the Y axis fixed.
         */
        AxialY,
        /**
         * Aligns this Billboard to the screen, but keeps the Z axis fixed.
         */
        AxialZ;
    }

    /**
     * Constructs a new `BillboardControl` with the default alignment set to
     * {@link Alignment#Screen}.
     */
    public BillboardControl() {
    }

    /**
     * Constructs a new `BillboardControl` with the specified alignment.
     *
     * @param alignment The desired alignment type for the billboard.
     * See {@link Alignment} for available options.
     */
    public BillboardControl(Alignment alignment) {
        this.alignment = alignment;
    }

    @Override
    protected void controlUpdate(float tpf) {
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        Camera cam = vp.getCamera();
        rotateBillboard(cam);
        fixRefreshFlags();
    }

    private void fixRefreshFlags() {
        // force transforms to update below this node
        spatial.updateGeometricState();

        // force world bound to update
        Spatial rootNode = spatial;
        while (rootNode.getParent() != null) {
            rootNode = rootNode.getParent();
        }
        rootNode.getWorldBound();
    }

    /**
     * Rotates the billboard based on the alignment type set.
     * This method is called every frame during the render phase.
     *
     * @param cam The current Camera used for rendering.
     */
    private void rotateBillboard(Camera cam) {
        switch (alignment) {
            case AxialY:
                rotateAxial(cam, Vector3f.UNIT_Y);
                break;
            case AxialZ:
                rotateAxial(cam, Vector3f.UNIT_Z);
                break;
            case Screen:
                rotateScreenAligned(cam);
                break;
            case Camera:
                rotateCameraAligned(cam);
                break;
        }
    }

    /**
     * Aligns this Billboard so that it points directly to the camera position.
     * The billboard's local rotation is set to ensure its positive Z-axis
     * points towards the camera's location.
     *
     * @param camera The current Camera.
     */
    private void rotateCameraAligned(Camera camera) {
        tempDir.set(camera.getLocation()).subtractLocal(
                spatial.getWorldTranslation());
        // co-opt left for our own purposes.
        Vector3f xzp = tempLeft;
        // The xzp vector is the projection of the look vector on the xz plane
        xzp.set(tempDir.x, 0, tempDir.z);

        // check for undefined rotation...
        if (xzp.equals(Vector3f.ZERO)) {
            return;
        }

        tempDir.normalizeLocal();
        xzp.normalizeLocal();
        float cosp = tempDir.dot(xzp);

        // compute the local orientation matrix for the billboard
        tempMat3.set(0, 0, xzp.z);
        tempMat3.set(0, 1, xzp.x * -tempDir.y);
        tempMat3.set(0, 2, xzp.x * cosp);
        tempMat3.set(1, 0, 0);
        tempMat3.set(1, 1, cosp);
        tempMat3.set(1, 2, tempDir.y);
        tempMat3.set(2, 0, -xzp.x);
        tempMat3.set(2, 1, xzp.z * -tempDir.y);
        tempMat3.set(2, 2, xzp.z * cosp);

        // Set the billboard's local rotation based on the computed orientation matrix.
        spatial.setLocalRotation(tempMat3);
    }

    /**
     * Rotates the billboard so it points directly opposite the direction the
     * camera is facing (screen-aligned). This means the billboard will always
     * be flat against the screen, regardless of its position in 3D space.
     * Its Z-axis will point against the camera's direction, and its Y-axis
     * will align with the camera's Y-axis.
     *
     * @param camera The current Camera.
     */
    private void rotateScreenAligned(Camera camera) {
        TempVars vars = TempVars.get();

        Vector3f up = camera.getUp(vars.vect1);
        // co-opt diff for our in direction:
        Vector3f dir = camera.getDirection(vars.vect2).negateLocal();
        // co-opt loc for our left direction:
        Vector3f left = camera.getLeft(vars.vect3).negateLocal();

        Matrix3f orient = vars.tempMat3;
        orient.fromAxes(left, up, dir);

        Node parent = spatial.getParent();
        Quaternion rot = vars.quat1.fromRotationMatrix(orient);

        if (parent != null) {
            Quaternion invRot = vars.quat2.set(parent.getWorldRotation()).inverseLocal();
            rot = invRot.multLocal(rot);
            rot.normalizeLocal();
        }

        // Apply the calculated local rotation to the spatial.
        spatial.setLocalRotation(rot);

        vars.release();
    }

    /**
     * Rotates the billboard towards the camera, but keeps a given axis fixed.
     * This is used for {@link Alignment#AxialY} (fixed Y-axis) or
     * {@link Alignment#AxialZ} (fixed Z-axis) alignments. The billboard will
     * only rotate around the specified axis.
     *
     * @param camera The current Camera.
     * @param axis The fixed axis (e.g., {@link Vector3f#UNIT_Y} for AxialY).
     */
    private void rotateAxial(Camera camera, Vector3f axis) {
        // Compute the additional rotation required for the billboard to face
        // the camera. To do this, the camera must be inverse-transformed into
        // the model space of the billboard.
        tempDir.set(camera.getLocation()).subtractLocal(spatial.getWorldTranslation());
        spatial.getParent().getWorldRotation().mult(tempDir, tempLeft); // co-opt left for our own purposes.
        tempLeft.x *= 1.0f / spatial.getWorldScale().x;
        tempLeft.y *= 1.0f / spatial.getWorldScale().y;
        tempLeft.z *= 1.0f / spatial.getWorldScale().z;

        // squared length of the camera projection in the xz-plane
//        float lengthSquared = left.x * left.x + left.z * left.z;

        // Calculate squared length of the camera projection on the plane perpendicular
        // to the fixed axis. This determines the magnitude of the projection used
        // for axial rotation.
        float lengthSquared;
        if (axis.y == 1) { // AxialY: projection on XZ plane
            lengthSquared = tempLeft.x * tempLeft.x + tempLeft.z * tempLeft.z;
        } else if (axis.z == 1) { // AxialZ: projection on XY plane
            lengthSquared = tempLeft.x * tempLeft.x + tempLeft.y * tempLeft.y;
        } else {
            // This case should ideally not be reached with the current Alignment enum,
            // but provides robustness for unexpected 'axis' values.
            return;
        }

        // Check for edge case: camera is directly on the fixed axis relative to the billboard.
        // If the projection length is too small, the rotation is undefined.
        if (lengthSquared < FastMath.FLT_EPSILON) {
            // Rotation is undefined, so no rotation is applied.
            return;
        }

        // Unitize the projection to get a normalized direction vector in the plane.
        float invLength = FastMath.invSqrt(lengthSquared);
        if (axis.y == 1) {
            tempLeft.x *= invLength;
            tempLeft.y = 0.0f; // Fix Y-component to 0 as it's axial, forcing rotation only around Y.
            tempLeft.z *= invLength;

            // compute the local orientation matrix for the billboard
            tempMat3.set(0, 0, tempLeft.z);
            tempMat3.set(0, 1, 0);
            tempMat3.set(0, 2, tempLeft.x);
            tempMat3.set(1, 0, 0);
            tempMat3.set(1, 1, 1); // Y-axis remains fixed (no rotation along Y).
            tempMat3.set(1, 2, 0);
            tempMat3.set(2, 0, -tempLeft.x);
            tempMat3.set(2, 1, 0);
            tempMat3.set(2, 2, tempLeft.z);

        } else if (axis.z == 1) {
            tempLeft.x *= invLength;
            tempLeft.y *= invLength;
            tempLeft.z = 0.0f; // Fix Z-component to 0 as it's axial, forcing rotation only around Z.

            // compute the local orientation matrix for the billboard
            tempMat3.set(0, 0, tempLeft.y);
            tempMat3.set(0, 1, tempLeft.x);
            tempMat3.set(0, 2, 0);
            tempMat3.set(1, 0, -tempLeft.y);
            tempMat3.set(1, 1, tempLeft.x);
            tempMat3.set(1, 2, 0);
            tempMat3.set(2, 0, 0);
            tempMat3.set(2, 1, 0);
            tempMat3.set(2, 2, 1); // Z-axis remains fixed (no rotation along Z).
        }

        // Apply the calculated local rotation matrix to the spatial.
        spatial.setLocalRotation(tempMat3);
    }

    /**
     * Returns the alignment this Billboard is set too.
     *
     * @return The alignment of rotation, AxialY, AxialZ, Camera or Screen.
     */
    public Alignment getAlignment() {
        return alignment;
    }

    /**
     * Sets the type of rotation this Billboard will have. The alignment can
     * be {@link Alignment#Camera}, {@link Alignment#Screen},
     * {@link Alignment#AxialY}, or {@link Alignment#AxialZ}.
     *
     * @param alignment The desired {@link Alignment} for the billboard's rotation behavior.
     */
    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(alignment, "alignment", Alignment.Screen);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        alignment = ic.readEnum("alignment", Alignment.class, Alignment.Screen);
    }
}
