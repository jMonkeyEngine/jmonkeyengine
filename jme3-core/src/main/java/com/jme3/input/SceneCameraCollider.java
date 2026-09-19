/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
package com.jme3.input;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link CameraCollider} that prevents the {@link ChaseCamera} from passing
 * through scene geometry by casting a ray from the target to the desired camera
 * position and adjusting the camera to sit just in front of the nearest hit.
 *
 * <p>Multiple scene nodes can be registered via
 * {@link #addNode(Node)} / {@link #setNodes(List)}, which is useful for paged
 * or otherwise partitioned worlds.</p>
 *
 * <p>Individual geometries or entire sub-trees can be excluded from camera
 * collision by setting a non-null userData value with the key returned by
 * {@link #getExcludeTag()} on the spatial or any of its ancestors. By default
 * the exclude tag is {@code null}, meaning no filtering is performed.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * SceneCameraCollider collider = new SceneCameraCollider(rootNode);
 * collider.setExcludeTag("ignoreCamera");
 * chaseCamera.setCameraCollider(collider);
 * </pre>
 *
 * @see CameraCollider
 * @see ChaseCamera#setCameraCollider(CameraCollider)
 */
public class SceneCameraCollider implements CameraCollider {

    private final List<Node> nodes = new ArrayList<>();
    private String excludeTag = null;
    private float minDistance = 0.1f;

    private final CollisionResults collisionResults = new CollisionResults();
    private final Ray ray = new Ray();

    /**
     * Creates a {@code SceneCameraCollider} with no scene nodes registered.
     * Add nodes later with {@link #addNode(Node)}.
     */
    public SceneCameraCollider() {
    }

    /**
     * Creates a {@code SceneCameraCollider} with the given nodes.
     *
     * @param nodes one or more nodes to test for camera collisions
     */
    public SceneCameraCollider(Node... nodes) {
        for (Node node : nodes) {
            this.nodes.add(node);
        }
    }

    /**
     * Adds a node to the set of nodes checked for camera collisions.
     *
     * @param node the node to add (not null)
     */
    public void addNode(Node node) {
        nodes.add(node);
    }

    /**
     * Removes a previously added node from the collision check.
     *
     * @param node the node to remove
     */
    public void removeNode(Node node) {
        nodes.remove(node);
    }

    /**
     * Returns a live view of the node list used for collision checks.
     *
     * @return the list of nodes (not null)
     */
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * Replaces the entire node list with the provided list.
     *
     * @param nodes the new list of nodes (not null)
     */
    public void setNodes(List<Node> nodes) {
        this.nodes.clear();
        this.nodes.addAll(nodes);
    }

    /**
     * Returns the userData key used to exclude geometries from camera
     * collision. If a geometry or any of its ancestors has a non-null value
     * for this key, it is skipped. Returns {@code null} if no filtering is
     * active (the default).
     *
     * @return the exclude tag, or {@code null}
     */
    public String getExcludeTag() {
        return excludeTag;
    }

    /**
     * Sets the userData key used to exclude geometries from camera collision.
     * Set to {@code null} to disable filtering (the default).
     *
     * <p>Any geometry (or any of its parent nodes) that has a non-null userData
     * value for this key is ignored during the collision check. This allows
     * invisible barriers or other objects to block player movement without
     * also affecting camera movement.</p>
     *
     * @param excludeTag the userData key to check, or {@code null} to disable
     */
    public void setExcludeTag(String excludeTag) {
        this.excludeTag = excludeTag;
    }

    /**
     * Returns the minimum distance the camera will maintain from a detected
     * collision point.
     *
     * @return the minimum distance (in world units, default=0.1)
     */
    public float getMinDistance() {
        return minDistance;
    }

    /**
     * Sets the minimum distance the camera will maintain from a detected
     * collision point. The camera is placed this far in front of the hit.
     *
     * @param minDistance the desired minimum distance (in world units,
     *                    default=0.1)
     */
    public void setMinDistance(float minDistance) {
        this.minDistance = minDistance;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Casts a ray from {@code targetPosition} toward {@code camPosition}.
     * Iterates over all registered nodes and finds the closest collision that
     * is not excluded. If any such collision is closer than the desired camera
     * distance, the camera is moved to
     * {@code max(collisionDistance - minDistance, 0)} units from the target.</p>
     */
    @Override
    public void collide(Vector3f targetPosition, Vector3f camPosition) {
        Vector3f camDir = camPosition.subtract(targetPosition);
        float maxDist = camDir.length();
        if (maxDist < FastMath.ZERO_TOLERANCE) {
            return;
        }
        camDir.normalizeLocal();
        ray.setOrigin(targetPosition);
        ray.setDirection(camDir);
        ray.setLimit(maxDist);

        float closestDist = maxDist;
        for (Node node : nodes) {
            collisionResults.clear();
            node.collideWith(ray, collisionResults);
            if (collisionResults.size() == 0) {
                continue;
            }
            // CollisionResults are sorted by distance (closest first)
            for (CollisionResult result : collisionResults) {
                float dist = result.getDistance();
                if (dist >= closestDist) {
                    // All remaining results are further away
                    break;
                }
                if (excludeTag != null && isExcluded(result.getGeometry())) {
                    continue;
                }
                closestDist = dist;
                break;
            }
        }

        if (closestDist < maxDist) {
            float adjustedDist = Math.max(closestDist - minDistance, 0);
            // camDir is normalized and not modified by mult(float), which returns a new vector
            camPosition.set(targetPosition).addLocal(camDir.mult(adjustedDist));
        }
    }

    /**
     * Returns {@code true} if the given spatial or any of its ancestors has a
     * non-null value for the exclude tag.
     *
     * @param spatial the spatial to test
     * @return true if the spatial should be excluded from collision
     */
    private boolean isExcluded(Spatial spatial) {
        Spatial current = spatial;
        while (current != null) {
            if (current.getUserData(excludeTag) != null) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
