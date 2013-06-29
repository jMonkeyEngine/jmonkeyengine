/*
 * Copyright (c) 2003-2012 jMonkeyEngine
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
package com.jme3.gde.core.util;

import com.jme3.animation.AnimControl;
import com.jme3.animation.SkeletonControl;
import com.jme3.gde.core.scene.ApplicationLogHandler.LogLevel;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Various utilities, mostly for operating on Spatials recursively and to copy
 * data over from "original" spatials (meshes, animations etc.). Mainly used by
 * the "external changes" scanner for models.
 *
 * @author normenhansen
 */
public class SpatialUtil {

    private static final Logger logger = Logger.getLogger(SpatialUtil.class.getName());
    //TODO: use these variables
    public static final String ORIGINAL_NAME = "ORIGINAL_NAME";
    public static final String ORIGINAL_PATH = "ORIGINAL_PATH";

    /**
     * Gets a "pathname" for the given Spatial, combines the Spatials and
     * parents names to make a long name. This "path" is stored in geometry
     * after the first import for example.
     *
     * @param spat
     * @return
     */
    public static String getSpatialPath(Spatial spat) {
        StringBuilder geometryIdentifier = new StringBuilder();
        while (spat != null) {
            String name = spat.getName();
            if (name == null) {
                logger.log(Level.WARNING, "Null spatial name!");
                name = "null";
            }
            geometryIdentifier.insert(0, name);
            geometryIdentifier.insert(0, '/');
            spat = spat.getParent();
        }
        String id = geometryIdentifier.toString();
        return id;
    }

    /**
     * Stores ORIGINAL_NAME and ORIGINAL_PATH UserData to given Spatial and all
     * sub-Spatials.
     *
     * @param spat
     */
    public static void storeOriginalPathUserData(Spatial spat) {
        //TODO: only stores for geometry atm
        final ArrayList<String> geomMap = new ArrayList<String>();
        if (spat != null) {
            spat.depthFirstTraversal(new SceneGraphVisitor() {
                @Override
                public void visit(Spatial geom) {
                    Spatial curSpat = geom;
                    String geomName = curSpat.getName();
                    if (geomName == null) {
                        logger.log(Level.WARNING, "Null Spatial name!");
                        geomName = "null";
                    }
                    geom.setUserData("ORIGINAL_NAME", geomName);
                    logger.log(Level.FINE, "Set ORIGINAL_NAME for {0}", geomName);
                    String id = SpatialUtil.getSpatialPath(curSpat);
                    if (geomMap.contains(id)) {
                        logger.log(Level.WARNING, "Cannot create unique name for Spatial {0}: {1}", new Object[]{geom, id});
                    }
                    geomMap.add(id);
                    geom.setUserData("ORIGINAL_PATH", id);
                    logger.log(Level.FINE, "Set ORIGINAL_PATH for {0}", id);
                }
            });
        } else {
            logger.log(Level.SEVERE, "No Spatial available when trying to add Spatial paths.");
        }
    }

    /**
     * Finds a previously marked spatial in the supplied root Spatial, creates
     * the name and path to be looked for from the given needle Spatial.
     *
     * @param root
     * @param needle
     * @return
     */
    public static Spatial findTaggedSpatial(final Spatial root, final Spatial needle) {
        if (needle == null) {
            logger.log(Level.WARNING, "Trying to find null needle for {0}", root);
            return null;
        }
        final String name = needle.getName();
        final String path = getSpatialPath(needle);
        if (name == null || path == null) {
            logger.log(Level.INFO, "Trying to find tagged Spatial with null name spatial for {0}.", root);
        }
        final Class clazz = needle.getClass();
        String rootName = root.getUserData("ORIGINAL_NAME");
        String rootPath = root.getUserData("ORIGINAL_PATH");
        if (name.equals(rootName) && path.equals(rootPath)) {
            return root;
        }
        final SpatialHolder holder = new SpatialHolder();
        root.depthFirstTraversal(new SceneGraphVisitor() {
            public void visit(Spatial spatial) {
                String spName = spatial.getUserData("ORIGINAL_NAME");
                String spPath = spatial.getUserData("ORIGINAL_PATH");
                if (name.equals(spName) && path.equals(spPath) && clazz.isInstance(spatial)) {
                    if (holder.spatial == null) {
                        holder.spatial = spatial;
                    } else {
                        logger.log(Level.WARNING, "Found Spatial {0} twice in {1}", new Object[]{path, root});
                    }
                }
            }
        });
        return holder.spatial;
    }

    /**
     * Finds a spatial in the given Spatial tree with the specified name and
     * path, the path and name are constructed from the given (sub-)spatial(s)
     * and is not read from the UserData of the objects. This is mainly used to
     * check if the original spatial still exists in the original file.
     *
     * @param root
     * @param name
     * @param path
     */
    public static Spatial findSpatial(final Spatial root, final String name, final String path) {
        if (name == null || path == null) {
            logger.log(Level.INFO, "Trying to find Spatial with null name spatial for {0}.", root);
        }
        if (name.equals(root.getName()) && path.equals(getSpatialPath(root))) {
            return root;
        }
        final SpatialHolder holder = new SpatialHolder();
        root.depthFirstTraversal(new SceneGraphVisitor() {
            public void visit(Spatial spatial) {
                if (name.equals(spatial.getName()) && path.equals(getSpatialPath(spatial))) {
                    if (holder.spatial == null) {
                        holder.spatial = spatial;
                    } else {
                        logger.log(Level.WARNING, "Found Spatial {0} twice in {1}", new Object[]{path, root});
                    }
                }
            }
        });
        return holder.spatial;
    }

    /**
     * Updates the mesh data of existing objects from an original file, adds new
     * nonexisting geometry objects to the root, including their parents if they
     * don't exist.
     *
     * @param root
     * @param original
     */
    public static void updateMeshDataFromOriginal(final Spatial root, final Spatial original) {
        //loop through original to also find new geometry
        original.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry geom) {
                //will always return same class type as 2nd param, so casting is safe
                Geometry spat = (Geometry) findTaggedSpatial(root, geom);
                if (spat != null) {
                    spat.setMesh(geom.getMesh());
                    logger.log(LogLevel.USERINFO, "Updated mesh for Geometry {0}", geom.getName());
                } else {
                    addLeafWithNonExistingParents(root, geom);
                }
            }
        });
    }

    /**
     * Adds a leaf to a spatial, including all nonexisting parents.
     *
     * @param root
     * @param original
     */
    private static void addLeafWithNonExistingParents(Spatial root, Spatial leaf) {
        if (!(root instanceof Node)) {
            logger.log(Level.WARNING, "Cannot add new Leaf {0} to {1}, is not a Node!", new Object[]{leaf.getName(), root.getName()});
            return;
        }
        for (Spatial s = leaf; s.getParent() != null; s = s.getParent()) {
            Spatial parent = s.getParent();
            Spatial other = findTaggedSpatial(root, parent);
            if (other == null) {
                continue;
            }
            if (other instanceof Node) {
                logger.log(Level.INFO, "Attaching {0} to {1} in root {2} to add leaf {3}", new Object[]{s, other, root, leaf});
                //set original path data to leaf and new parents
                for (Spatial spt = leaf; spt != parent; spt = spt.getParent()) {
                    spt.setUserData(ORIGINAL_NAME, spt.getName());
                    spt.setUserData(ORIGINAL_PATH, getSpatialPath(spt));
                    spt = spt.getParent();
                }
                //attach to new node in own root
                Node otherNode = (Node) other;
                otherNode.attachChild(s);
                logger.log(LogLevel.USERINFO, "Attached Node {0} with leaf {0}", new Object[]{other.getName(), leaf.getName()});
                return;
            } else {
                logger.log(Level.WARNING, "Cannot attach leaf {0} to found spatial {1} in root {2}, not a node.", new Object[]{leaf, other, root});
            }
        }
        logger.log(Level.WARNING, "Could not attach new Leaf {0}, no root node found.", leaf.getName());
    }

    public static void updateAnimControlDataFromOriginal(final Spatial root, final Spatial original) {
        //loop through original to also find new AnimControls, we expect all nodes etc. to exist
        //TODO: can (blender) AnimControls end up in other nodes that are not a parent of the geometry they modify?
        original.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spat) {
                AnimControl animControl = spat.getControl(AnimControl.class);
                if (animControl != null) {
                    Spatial mySpatial = findTaggedSpatial(root, spat);
                    if (mySpatial != null) {
                        //TODO: move attachments: have to scan through all nodes and find the ones
                        //where UserData "AttachedBone" == Bone and move it to new Bone
                        AnimControl myAnimControl = mySpatial.getControl(AnimControl.class);
                        SkeletonControl mySkeletonControl = spat.getControl(SkeletonControl.class);
                        if (mySkeletonControl != null) {
                            mySpatial.removeControl(mySkeletonControl);
                        }
                        if (myAnimControl != null) {
                            mySpatial.removeControl(myAnimControl);
                        }
                        AnimControl newControl = (AnimControl)animControl.cloneForSpatial(mySpatial);
                        if (mySpatial.getControl(SkeletonControl.class) == null) {
                            logger.log(Level.INFO, "Adding control for {0}", mySpatial.getName());
                            mySpatial.addControl(newControl);
                        }else{
                            logger.log(Level.INFO, "Control for {0} was added automatically", mySpatial.getName());
                        }
                        if (mySpatial.getControl(SkeletonControl.class) == null) {
                            mySpatial.addControl(new SkeletonControl(animControl.getSkeleton()));
                        } else {
                            logger.log(Level.INFO, "SkeletonControl for {0} was added by AnimControl already", mySpatial.getName());
                        }
                        logger.log(LogLevel.USERINFO, "Updated animation for {0}", mySpatial.getName());
                    } else {
                        logger.log(Level.WARNING, "Could not find sibling for {0} in root {1} when trying to apply AnimControl data", new Object[]{spat, root});
                    }
                }
            }
        });
        //TODO: remove old AnimControls?
    }

    public static void clearRemovedOriginals(final Spatial root, final Spatial original) {
        //TODO: Clear old stuff at all?
        return;
    }

    /**
     * Finds out if a spatial has animations.
     *
     * @param root
     */
    public static boolean hasAnimations(final Spatial root) {
        final AtomicBoolean animFound = new AtomicBoolean(false);
        root.depthFirstTraversal(new SceneGraphVisitor() {
            public void visit(Spatial spatial) {
                if (spatial.getControl(AnimControl.class) != null) {
                    animFound.set(true);
                }
            }
        });
        return animFound.get();
    }

    private static class SpatialHolder {

        Spatial spatial;
    }
}
