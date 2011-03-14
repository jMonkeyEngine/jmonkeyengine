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
package com.jme3.bullet.nodes;

import com.jme3.scene.Spatial;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.List;

/**
 * <i>From Bullet manual:</i><br>
 * GhostObject can keep track of all objects that are overlapping.
 * By default, this overlap is based on the AABB.
 * This is useful for creating a character controller,
 * collision sensors/triggers, explosions etc.<br>
 * @author normenhansen
 * @deprecated in favor of physics Controls
 */
@Deprecated
public class PhysicsGhostNode extends PhysicsBaseNode {

//    protected PhysicsGhostControl gObject;

    public PhysicsGhostNode() {
    }

    public PhysicsGhostNode(CollisionShape shape) {
        collisionObject=new GhostControl(shape);
        addControl(((GhostControl)collisionObject));
    }

    public PhysicsGhostNode(Spatial child, CollisionShape shape) {
        collisionObject=new GhostControl(shape);
        addControl(((GhostControl)collisionObject));
        attachChild(child);
    }

    @Override
    public void setCollisionShape(CollisionShape collisionShape) {
        ((GhostControl)collisionObject).setCollisionShape(collisionShape);
    }

    @Override
    public void setLocalTransform(Transform t) {
        super.setLocalTransform(t);
        ((PhysicsGhostObject)collisionObject).setPhysicsLocation(getWorldTranslation());
        ((PhysicsGhostObject)collisionObject).setPhysicsRotation(getWorldRotation().toRotationMatrix());
    }

    @Override
    public void setLocalTranslation(Vector3f localTranslation) {
        super.setLocalTranslation(localTranslation);
        ((PhysicsGhostObject)collisionObject).setPhysicsLocation(getWorldTranslation());
    }

    @Override
    public void setLocalTranslation(float x, float y, float z) {
        super.setLocalTranslation(x, y, z);
        ((PhysicsGhostObject)collisionObject).setPhysicsLocation(getWorldTranslation());
    }

    @Override
    public void setLocalRotation(Matrix3f rotation) {
        super.setLocalRotation(rotation);
        ((PhysicsGhostObject)collisionObject).setPhysicsRotation(getWorldRotation().toRotationMatrix());
    }

    @Override
    public void setLocalRotation(Quaternion quaternion) {
        super.setLocalRotation(quaternion);
        ((PhysicsGhostObject)collisionObject).setPhysicsRotation(getWorldRotation().toRotationMatrix());
    }

    /**
     * used internally
     */
    public PhysicsGhostObject getGhostObject() {
        return ((GhostControl)collisionObject);
    }

    /**
     * destroys this PhysicsGhostNode and removes it from memory
     */
    public void destroy() {
        ((GhostControl)collisionObject).destroy();
    }

    /**
     * Another Object is overlapping with this GhostNode,
     * if and if only there CollisionShapes overlaps.
     * They could be both regular PhysicsNodes or PhysicsGhostNode.
     * @return All CollisionObjects overlapping with this GhostNode.
     */
    public List<PhysicsCollisionObject> getOverlappingObjects() {
        return ((GhostControl)collisionObject).getOverlappingObjects();
    }

    /**
     *
     * @return With how many other CollisionObjects this GhostNode is currently overlapping.
     */
    public int getOverlappingCount() {
        return ((GhostControl)collisionObject).getOverlappingCount();
    }

    /**
     *
     * @param index The index of the overlapping Node to retrieve.
     * @return The Overlapping CollisionObject at the given index.
     */
    public PhysicsCollisionObject getOverlapping(int index) {
        return ((GhostControl)collisionObject).getOverlapping(index);
    }

    public void setCcdSweptSphereRadius(float radius) {
        ((GhostControl)collisionObject).setCcdSweptSphereRadius(radius);
    }

    public void setCcdMotionThreshold(float threshold) {
        ((GhostControl)collisionObject).setCcdMotionThreshold(threshold);
    }

    public float getCcdSweptSphereRadius() {
        return ((GhostControl)collisionObject).getCcdSweptSphereRadius();
    }

    public float getCcdMotionThreshold() {
        return ((GhostControl)collisionObject).getCcdMotionThreshold();
    }

    public float getCcdSquareMotionThreshold() {
        return ((GhostControl)collisionObject).getCcdSquareMotionThreshold();
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
    }
}
