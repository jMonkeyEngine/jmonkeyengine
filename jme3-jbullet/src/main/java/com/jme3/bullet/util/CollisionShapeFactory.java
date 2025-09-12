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
package com.jme3.bullet.util;

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.collision.shapes.*;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.math.Matrix3f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import com.jme3.terrain.geomipmap.TerrainPatch;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A utility class for generating collision shapes from Spatials.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author normenhansen, tim8dev
 */
public class CollisionShapeFactory {
    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private CollisionShapeFactory() {
    }

    /**
     * Calculate the correct transform for a collision shape relative to the
     * ancestor for which the shape was generated.
     *
     * @param spat
     * @param parent
     * @return a new instance (not null)
     */
    private static Transform getTransform(Spatial spat, Spatial parent) {
        Transform shapeTransform = new Transform();
        Spatial parentNode = spat.getParent() != null ? spat.getParent() : spat;
        Spatial currentSpatial = spat;
        //if we have parents combine their transforms
        while (parentNode != null) {
            if (parent == currentSpatial) {
                //real parent -> only apply scale, not transform
                Transform trans = new Transform();
                trans.setScale(currentSpatial.getLocalScale());
                shapeTransform.combineWithParent(trans);
                parentNode = null;
            } else {
                shapeTransform.combineWithParent(currentSpatial.getLocalTransform());
                parentNode = currentSpatial.getParent();
                currentSpatial = parentNode;
            }
        }
        return shapeTransform;
    }

    private static CompoundCollisionShape createCompoundShape(Node realRootNode,
            Node rootNode, CompoundCollisionShape shape, boolean meshAccurate, boolean dynamic) {
        for (Spatial spatial : rootNode.getChildren()) {
            if (spatial instanceof TerrainQuad) {
                Boolean bool = spatial.getUserData(UserData.JME_PHYSICSIGNORE);
                if (bool != null && bool.booleanValue()) {
                    continue; // go to the next child in the loop
                }
                TerrainQuad terrain = (TerrainQuad) spatial;
                Transform trans = getTransform(spatial, realRootNode);
                shape.addChildShape(new HeightfieldCollisionShape(terrain.getHeightMap(), trans.getScale()),
                        trans.getTranslation(),
                        trans.getRotation().toRotationMatrix());
            } else if (spatial instanceof Node) {
                createCompoundShape(realRootNode, (Node) spatial, shape, meshAccurate, dynamic);
            } else if (spatial instanceof TerrainPatch) {
                Boolean bool = spatial.getUserData(UserData.JME_PHYSICSIGNORE);
                if (bool != null && bool.booleanValue()) {
                    continue; // go to the next child in the loop
                }
                TerrainPatch terrain = (TerrainPatch) spatial;
                Transform trans = getTransform(spatial, realRootNode);
                shape.addChildShape(new HeightfieldCollisionShape(terrain.getHeightMap(), terrain.getLocalScale()),
                        trans.getTranslation(),
                        trans.getRotation().toRotationMatrix());
            } else if (spatial instanceof Geometry) {
                Boolean bool = spatial.getUserData(UserData.JME_PHYSICSIGNORE);
                if (bool != null && bool.booleanValue()) {
                    continue; // go to the next child in the loop
                }

                if (meshAccurate) {
                    CollisionShape childShape = dynamic
                            ? createSingleDynamicMeshShape((Geometry) spatial, realRootNode)
                            : createSingleMeshShape((Geometry) spatial, realRootNode);
                    if (childShape != null) {
                        Transform trans = getTransform(spatial, realRootNode);
                        shape.addChildShape(childShape,
                                trans.getTranslation(),
                                trans.getRotation().toRotationMatrix());
                    }
                } else {
                    Transform trans = getTransform(spatial, realRootNode);
                    shape.addChildShape(createSingleBoxShape(spatial, realRootNode),
                            trans.getTranslation(),
                            trans.getRotation().toRotationMatrix());
                }
            }
        }
        return shape;
    }

    private static CompoundCollisionShape createCompoundShape(
            Node rootNode, CompoundCollisionShape shape, boolean meshAccurate) {
        return createCompoundShape(rootNode, rootNode, shape, meshAccurate, false);
    }

    /**
     * This type of collision shape is mesh-accurate and meant for immovable
     * "world objects". Examples include terrain, houses or whole shooter
     * levels.
     * <p>
     * Objects with "mesh" type collision shape will not collide with each
     * other.
     *
     * @param rootNode the node on which to base the shape (not null)
     * @return a new shape (not null)
     */
    private static CompoundCollisionShape createMeshCompoundShape(Node rootNode) {
        return createCompoundShape(rootNode, new CompoundCollisionShape(), true);
    }

    /**
     * This type of collision shape creates a CompoundShape made out of boxes
     * that are based on the bounds of the Geometries in the tree.
     *
     * @param rootNode the node on which to base the shape (not null)
     * @return a new shape (not null)
     */
    private static CompoundCollisionShape createBoxCompoundShape(Node rootNode) {
        return createCompoundShape(rootNode, new CompoundCollisionShape(), false);
    }

    /**
     * Create a mesh shape for the given Spatial.
     * <p>
     * This type of collision shape is mesh-accurate and meant for immovable
     * "world objects". Examples include terrain, houses or whole shooter
     * levels.
     * <p>
     * Objects with "mesh" type collision shape will not collide with each
     * other.
     * <p>
     * Creates a HeightfieldCollisionShape if the supplied spatial is a
     * TerrainQuad.
     *
     * @param spatial the spatial on which to base the shape (not null)
     * @return A MeshCollisionShape or a CompoundCollisionShape with
     * MeshCollisionShapes as children if the supplied spatial is a Node. A
     * HeightfieldCollisionShape if a TerrainQuad was supplied.
     */
    public static CollisionShape createMeshShape(Spatial spatial) {
        if (spatial instanceof TerrainQuad) {
            TerrainQuad terrain = (TerrainQuad) spatial;
            return new HeightfieldCollisionShape(terrain.getHeightMap(), terrain.getLocalScale());
        } else if (spatial instanceof TerrainPatch) {
            TerrainPatch terrain = (TerrainPatch) spatial;
            return new HeightfieldCollisionShape(terrain.getHeightMap(), terrain.getLocalScale());
        } else if (spatial instanceof Geometry) {
            return createSingleMeshShape((Geometry) spatial, spatial);
        } else if (spatial instanceof Node) {
            return createMeshCompoundShape((Node) spatial);
        } else {
            throw new IllegalArgumentException("Supplied spatial must either be Node or Geometry!");
        }
    }

    /**
     * Create a hull shape for the given Spatial.
     * <p>
     * For mesh-accurate animated meshes (CPU intense!) use GImpact shapes.
     *
     * @param spatial the spatial on which to base the shape (not null)
     * @return a HullCollisionShape (if spatial is a Geometry) or a
     * CompoundCollisionShape with HullCollisionShapes as children (if spatial
     * is a Node)
     */
    public static CollisionShape createDynamicMeshShape(Spatial spatial) {
        if (spatial instanceof Geometry) {
            return createSingleDynamicMeshShape((Geometry) spatial, spatial);
        } else if (spatial instanceof Node) {
            return createCompoundShape((Node) spatial, (Node) spatial, new CompoundCollisionShape(), true, true);
        } else {
            throw new IllegalArgumentException("Supplied spatial must either be Node or Geometry!");
        }

    }

    /**
     * Create a box shape for the given Spatial.
     *
     * @param spatial the spatial on which to base the shape (not null)
     * @return a BoxCollisionShape (if spatial is a Geometry) or a
     * CompoundCollisionShape with BoxCollisionShapes as children (if spatial is
     * a Node)
     */
    public static CollisionShape createBoxShape(Spatial spatial) {
        if (spatial instanceof Geometry) {
            return createSingleBoxShape(spatial, spatial);
        } else if (spatial instanceof Node) {
            return createBoxCompoundShape((Node) spatial);
        } else {
            throw new IllegalArgumentException("Supplied spatial must either be Node or Geometry!");
        }
    }

    /**
     * This type of collision shape is mesh-accurate and meant for immovable
     * "world objects". Examples include terrain, houses or whole shooter
     * levels.
     * <p>
     * Objects with "mesh" type collision shape will not collide with each
     * other.
     */
    private static MeshCollisionShape createSingleMeshShape(Geometry geom, Spatial parent) {
        Mesh mesh = geom.getMesh();
        Transform trans = getTransform(geom, parent);
        if (mesh != null && mesh.getMode() == Mesh.Mode.Triangles) {
            MeshCollisionShape mColl = new MeshCollisionShape(mesh);
            mColl.setScale(trans.getScale());
            return mColl;
        } else {
            return null;
        }
    }

    /**
     * Use the bounding box of the supplied spatial to create a
     * BoxCollisionShape.
     *
     * @param spatial the spatial on which to base the shape (not null)
     * @param parent unused
     * @return a new shape with the dimensions of the spatial's bounding box
     * (not null)
     */
    private static BoxCollisionShape createSingleBoxShape(Spatial spatial, Spatial parent) {
        //TODO: using world bound here instead of "local world" bound...
        BoxCollisionShape shape = new BoxCollisionShape(
                ((BoundingBox) spatial.getWorldBound()).getExtent(new Vector3f()));
        return shape;
    }

    /**
     * Create a hull collision shape for the specified geometry.
     *
     * @param geom the geometry on which to base the shape (not null)
     * @param parent
     */
    private static HullCollisionShape createSingleDynamicMeshShape(Geometry geom, Spatial parent) {
        Mesh mesh = geom.getMesh();
        Transform trans = getTransform(geom, parent);
        if (mesh != null) {
            HullCollisionShape dynamicShape = new HullCollisionShape(mesh);
            dynamicShape.setScale(trans.getScale());
            return dynamicShape;
        } else {
            return null;
        }
    }

    /**
     * This method moves each child shape of a compound shape by the given vector
     *
     * @param compoundShape the shape to modify (not null)
     * @param vector the offset vector (not null, unaffected)
     */
    public static void shiftCompoundShapeContents(CompoundCollisionShape compoundShape, Vector3f vector) {
        for (Iterator<ChildCollisionShape> it = new LinkedList<>(compoundShape.getChildren()).iterator(); it.hasNext();) {
            ChildCollisionShape childCollisionShape = it.next();
            CollisionShape child = childCollisionShape.shape;
            Vector3f location = childCollisionShape.location;
            Matrix3f rotation = childCollisionShape.rotation;
            compoundShape.removeChildShape(child);
            compoundShape.addChildShape(child, location.add(vector), rotation);
        }
    }
}
