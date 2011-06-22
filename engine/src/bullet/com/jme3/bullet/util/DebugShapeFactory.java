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
package com.jme3.bullet.util;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.math.Matrix3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.TempVars;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author CJ Hare, normenhansen
 */
public class DebugShapeFactory {

    /** The maximum corner for the aabb used for triangles to include in ConcaveShape processing.*/
//    private static final Vector3f aabbMax = new Vector3f(1e30f, 1e30f, 1e30f);
    /** The minimum corner for the aabb used for triangles to include in ConcaveShape processing.*/
//    private static final Vector3f aabbMin = new Vector3f(-1e30f, -1e30f, -1e30f);

    /**
     * Creates a debug shape from the given collision shape. This is mostly used internally.<br>
     * To attach a debug shape to a physics object, call <code>attachDebugShape(AssetManager manager);</code> on it.
     * @param collisionShape
     * @return
     */
    public static Spatial getDebugShape(CollisionShape collisionShape) {
        if (collisionShape == null) {
            return null;
        }
        Spatial debugShape;
        if (collisionShape instanceof CompoundCollisionShape) {
            CompoundCollisionShape shape = (CompoundCollisionShape) collisionShape;
            List<ChildCollisionShape> children = shape.getChildren();
            Node node = new Node("DebugShapeNode");
            for (Iterator<ChildCollisionShape> it = children.iterator(); it.hasNext();) {
                ChildCollisionShape childCollisionShape = it.next();
                CollisionShape ccollisionShape = childCollisionShape.shape;
                Geometry geometry = createDebugShape(ccollisionShape);

                // apply translation
                geometry.setLocalTranslation(childCollisionShape.location);

                // apply rotation
                TempVars vars = TempVars.get();                
                Matrix3f tempRot = vars.tempMat3;

                tempRot.set(geometry.getLocalRotation());
                childCollisionShape.rotation.mult(tempRot, tempRot);
                geometry.setLocalRotation(tempRot);

                vars.release();

                node.attachChild(geometry);
            }
            debugShape = node;
        } else {
            debugShape = createDebugShape(collisionShape);
        }
        if (debugShape == null) {
            return null;
        }
        debugShape.updateGeometricState();
        return debugShape;
    }

    private static Geometry createDebugShape(CollisionShape shape) {
        Geometry geom = new Geometry();
        geom.setMesh(DebugShapeFactory.getDebugMesh(shape));
//        geom.setLocalScale(shape.getScale());
        geom.updateModelBound();
        return geom;
    }

    public static Mesh getDebugMesh(CollisionShape shape) {
        Mesh mesh = new Mesh();
        mesh = new Mesh();
        DebugMeshCallback callback = new DebugMeshCallback();
        getVertices(shape.getObjectId(), callback);
        mesh.setBuffer(Type.Position, 3, callback.getVertices());
        mesh.getFloatBuffer(Type.Position).clear();
        return mesh;
    }

    private static native void getVertices(long shapeId, DebugMeshCallback buffer);
}
