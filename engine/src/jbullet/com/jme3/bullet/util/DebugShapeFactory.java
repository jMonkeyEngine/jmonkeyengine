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

import com.bulletphysics.collision.shapes.ConcaveShape;
import com.bulletphysics.collision.shapes.ConvexShape;
import com.bulletphysics.collision.shapes.ShapeHull;
import com.bulletphysics.collision.shapes.TriangleCallback;
import com.bulletphysics.util.IntArrayList;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.math.Matrix3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.vecmath.Vector3f;

/**
 *
 * @author CJ Hare, normenhansen
 */
public class DebugShapeFactory {

    /** The maximum corner for the aabb used for triangles to include in ConcaveShape processing.*/
    private static final Vector3f aabbMax = new Vector3f(1e30f, 1e30f, 1e30f);
    /** The minimum corner for the aabb used for triangles to include in ConcaveShape processing.*/
    private static final Vector3f aabbMin = new Vector3f(-1e30f, -1e30f, -1e30f);

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
        Mesh mesh = null;
        if (shape.getCShape() instanceof ConvexShape) {
            mesh = new Mesh();
            mesh.setBuffer(Type.Position, 3, getVertices((ConvexShape) shape.getCShape()));
            mesh.getFloatBuffer(Type.Position).clear();
        } else if (shape.getCShape() instanceof ConcaveShape) {
            mesh = new Mesh();
            mesh.setBuffer(Type.Position, 3, getVertices((ConcaveShape) shape.getCShape()));
            mesh.getFloatBuffer(Type.Position).clear();
        }
        return mesh;
    }

    /**
     *  Constructs the buffer for the vertices of the concave shape.
     *
     * @param concaveShape the shape to get the vertices for / from.
     * @return the shape as stored by the given broadphase rigid body.
     */
    private static FloatBuffer getVertices(ConcaveShape concaveShape) {
        // Create the call back that'll create the vertex buffer
        BufferedTriangleCallback triangleProcessor = new BufferedTriangleCallback();
        concaveShape.processAllTriangles(triangleProcessor, aabbMin, aabbMax);

        // Retrieve the vextex and index buffers
        return triangleProcessor.getVertices();
    }

    /**
     *  Processes the given convex shape to retrieve a correctly ordered FloatBuffer to
     *  construct the shape from with a TriMesh.
     *
     * @param convexShape the shape to retreieve the vertices from.
     * @return the vertices as a FloatBuffer, ordered as Triangles.
     */
    private static FloatBuffer getVertices(ConvexShape convexShape) {
        // Check there is a hull shape to render
        if (convexShape.getUserPointer() == null) {
            // create a hull approximation
            ShapeHull hull = new ShapeHull(convexShape);
            float margin = convexShape.getMargin();
            hull.buildHull(margin);
            convexShape.setUserPointer(hull);
        }

        // Assert state - should have a pointer to a hull (shape) that'll be drawn
        assert convexShape.getUserPointer() != null : "Should have a shape for the userPointer, instead got null";
        ShapeHull hull = (ShapeHull) convexShape.getUserPointer();

        // Assert we actually have a shape to render
        assert hull.numTriangles() > 0 : "Expecting the Hull shape to have triangles";
        int numberOfTriangles = hull.numTriangles();

        // The number of bytes needed is: (floats in a vertex) * (vertices in a triangle) * (# of triangles) * (size of float in bytes)
        final int numberOfFloats = 3 * 3 * numberOfTriangles;
        FloatBuffer vertices = BufferUtils.createFloatBuffer(numberOfFloats); 

        // Force the limit, set the cap - most number of floats we will use the buffer for
        vertices.limit(numberOfFloats);

        // Loop variables
        final IntArrayList hullIndicies = hull.getIndexPointer();
        final List<Vector3f> hullVertices = hull.getVertexPointer();
        Vector3f vertexA, vertexB, vertexC;
        int index = 0;

        for (int i = 0; i < numberOfTriangles; i++) {
            // Grab the data for this triangle from the hull
            vertexA = hullVertices.get(hullIndicies.get(index++));
            vertexB = hullVertices.get(hullIndicies.get(index++));
            vertexC = hullVertices.get(hullIndicies.get(index++));

            // Put the verticies into the vertex buffer
            vertices.put(vertexA.x).put(vertexA.y).put(vertexA.z);
            vertices.put(vertexB.x).put(vertexB.y).put(vertexB.z);
            vertices.put(vertexC.x).put(vertexC.y).put(vertexC.z);
        }

        vertices.clear();
        return vertices;
    }
}

/**
 *  A callback is used to process the triangles of the shape as there is no direct access to a concave shapes, shape.
 *  <p/>
 *  The triangles are simply put into a list (which in extreme condition will cause memory problems) then put into a direct buffer.
 *
 * @author CJ Hare
 */
class BufferedTriangleCallback extends TriangleCallback {

    private ArrayList<Vector3f> vertices;

    public BufferedTriangleCallback() {
        vertices = new ArrayList<Vector3f>();
    }

    @Override
    public void processTriangle(Vector3f[] triangle, int partId, int triangleIndex) {
        // Three sets of individual lines
        // The new Vector is needed as the given triangle reference is from a pool
        vertices.add(new Vector3f(triangle[0]));
        vertices.add(new Vector3f(triangle[1]));
        vertices.add(new Vector3f(triangle[2]));
    }

    /**
     *  Retrieves the vertices from the Triangle buffer.
     */
    public FloatBuffer getVertices() {
        // There are 3 floats needed for each vertex (x,y,z)
        final int numberOfFloats = vertices.size() * 3;
        FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(numberOfFloats); 

        // Force the limit, set the cap - most number of floats we will use the buffer for
        verticesBuffer.limit(numberOfFloats);

        // Copy the values from the list to the direct float buffer
        for (Vector3f v : vertices) {
            verticesBuffer.put(v.x).put(v.y).put(v.z);
        }

        vertices.clear();
        return verticesBuffer;
    }
}
