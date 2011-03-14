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

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author normenhansen, tim8dev
 */
public class CollisionShapeFactory {

    private static CompoundCollisionShape createCompoundShape(
            Node rootNode, CompoundCollisionShape shape, boolean meshAccurate, boolean dynamic) {
        for (Spatial spatial : rootNode.getChildren()) {
            if (spatial instanceof Node) {
                createCompoundShape((Node) spatial, shape, meshAccurate, dynamic);
            } else if (spatial instanceof Geometry) {
                if (meshAccurate) {
                    CollisionShape childShape = dynamic
                            ? createSingleDynamicMeshShape((Geometry) spatial)
                            : createSingleMeshShape((Geometry) spatial);
                    if (childShape != null) {
                        shape.addChildShape(childShape,
                                spatial.getWorldTranslation(),
                                spatial.getWorldRotation().toRotationMatrix());
                    }
                } else {
                    shape.addChildShape(createSingleBoxShape(spatial),
                            spatial.getWorldTranslation(),
                            spatial.getWorldRotation().toRotationMatrix());
                }
            }
        }
        return shape;
    }

    private static CompoundCollisionShape createCompoundShape(
            Node rootNode, CompoundCollisionShape shape, boolean meshAccurate) {
        return createCompoundShape(rootNode, shape, meshAccurate, false);
    }

    /**
     * This type of collision shape is mesh-accurate and meant for immovable "world objects".
     * Examples include terrain, houses or whole shooter levels.<br>
     * Objects with "mesh" type collision shape will not collide with each other.
     */
    private static CompoundCollisionShape createMeshCompoundShape(Node rootNode) {
        return createCompoundShape(rootNode, new CompoundCollisionShape(), true);
    }

    /**
     * This type of collision shape creates a CompoundShape made out of boxes that
     * are based on the bounds of the Geometries  in the tree.
     * @param rootNode
     * @return
     */
    private static CompoundCollisionShape createBoxCompoundShape(Node rootNode) {
        return createCompoundShape(rootNode, new CompoundCollisionShape(), false);
    }

    /**
     * This type of collision shape is mesh-accurate and meant for immovable "world objects".
     * Examples include terrain, houses or whole shooter levels.<br>
     * Objects with "mesh" type collision shape will not collide with each other.
     * @return A MeshCollisionShape or a CompoundCollisionShape with MeshCollisionShapes as children if the supplied spatial is a Node.
     */
    public static CollisionShape createMeshShape(Spatial spatial) {
        if (spatial instanceof TerrainQuad) {
            TerrainQuad terrain = (TerrainQuad) spatial;
            return new HeightfieldCollisionShape(terrain.getHeightMap(), terrain.getLocalScale());
            //BELOW: the old way, keeping it here for a little bit as a reference (and so it gets into version control so I can always access it)
		/*Map<TerrainPatch,Vector3f> all = new HashMap<TerrainPatch,Vector3f>();
            terrain.getAllTerrainPatchesWithTranslation(all, terrain.getLocalTranslation());
            
            Node node = new Node();
            
            for (Entry<TerrainPatch,Vector3f> entry : all.entrySet()) {
            TerrainPatch tp = entry.getKey();
            Vector3f trans = entry.getValue();
            PhysicsNode n = new PhysicsNode(new HeightfieldCollisionShape(tp.getHeightmap(), trans, tp.getLocalScale()), 0 );
            n.setLocalTranslation(trans);
            node.attachChild(n);
            }*/

        } else if (spatial instanceof Geometry) {
            return createSingleMeshShape((Geometry) spatial);
        } else if (spatial instanceof Node) {
            return createMeshCompoundShape((Node) spatial);
        } else {
            throw new IllegalArgumentException("Supplied spatial must either be Node or Geometry!");
        }
    }

    /**
     * This method creates a hull shape for the given Spatial.<br>
     * If you want to have mesh-accurate dynamic shapes (CPU intense!!!) use GImpact shapes, its probably best to do so with a low-poly version of your model.
     * @return A HullCollisionShape or a CompoundCollisionShape with HullCollisionShapes as children if the supplied spatial is a Node.
     */
    public static CollisionShape createDynamicMeshShape(Spatial spatial) {
        if (spatial instanceof Geometry) {
            return createSingleDynamicMeshShape((Geometry) spatial);
        } else if (spatial instanceof Node) {
            return createCompoundShape((Node) spatial, new CompoundCollisionShape(), true, true);
        } else {
            throw new IllegalArgumentException("Supplied spatial must either be Node or Geometry!");
        }

    }

    public static CollisionShape createBoxShape(Spatial spatial) {
        if (spatial instanceof Geometry) {
            return createSingleBoxShape((Geometry) spatial);
        } else if (spatial instanceof Node) {
            return createBoxCompoundShape((Node) spatial);
        } else {
            throw new IllegalArgumentException("Supplied spatial must either be Node or Geometry!");
        }
    }

    /**
     * This type of collision shape is mesh-accurate and meant for immovable "world objects".
     * Examples include terrain, houses or whole shooter levels.<br>
     * Objects with "mesh" type collision shape will not collide with each other.
     */
    public static MeshCollisionShape createSingleMeshShape(Geometry geom) {
        Mesh mesh = geom.getMesh();
        if (mesh != null) {
            MeshCollisionShape mColl = new MeshCollisionShape(mesh);
            mColl.setScale(geom.getWorldScale());
            return mColl;
        } else {
            return null;
        }
    }

    /**
     * Uses the bounding box of the supplied spatial to create a BoxCollisionShape
     * @param spatial
     * @return BoxCollisionShape with the size of the spatials BoundingBox
     */
    public static BoxCollisionShape createSingleBoxShape(Spatial spatial) {
        spatial.setModelBound(new BoundingBox());
        BoxCollisionShape shape = new BoxCollisionShape(
                ((BoundingBox) spatial.getWorldBound()).getExtent(new Vector3f()));
        return shape;
    }

    /**
     * This method creates a hull collision shape for the given mesh.<br>
     */
    public static HullCollisionShape createSingleDynamicMeshShape(Geometry geom) {
        Mesh mesh = geom.getMesh();
        if (mesh != null) {
            HullCollisionShape dynamicShape = new HullCollisionShape(mesh);
            dynamicShape.setScale(geom.getWorldScale());
            return dynamicShape;
        } else {
            return null;
        }
    }

    /**
     * This method moves each child shape of a compound shape by the given vector
     * @param vector
     */
    public static void shiftCompoundShapeContents(CompoundCollisionShape compoundShape, Vector3f vector) {
        for (Iterator<ChildCollisionShape> it = new LinkedList(compoundShape.getChildren()).iterator(); it.hasNext();) {
            ChildCollisionShape childCollisionShape = it.next();
            CollisionShape child = childCollisionShape.shape;
            Vector3f location = childCollisionShape.location;
            Matrix3f rotation = childCollisionShape.rotation;
            compoundShape.removeChildShape(child);
            compoundShape.addChildShape(child, location.add(vector), rotation);
        }
    }

}
