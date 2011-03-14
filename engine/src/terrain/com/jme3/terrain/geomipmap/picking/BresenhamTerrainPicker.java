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

package com.jme3.terrain.geomipmap.picking;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Triangle;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainPatch;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.picking.BresenhamYUpGridTracer.Direction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * It basically works by casting a pick ray
 * against the bounding volumes of the TerrainQuad and its children, gathering
 * all of the TerrainPatches hit (in distance order.) The triangles of each patch
 * are then tested using the BresenhamYUpGridTracer to determine which triangles
 * to test and in what order. When a hit is found, it is guaranteed to be the
 * first such hit and can immediately be returned.
 * 
 * @author Joshua Slack
 * @author Brent Owens
 */
public class BresenhamTerrainPicker implements TerrainPicker {

    private final Triangle gridTriA = new Triangle(new Vector3f(), new Vector3f(), new Vector3f());
    private final Triangle gridTriB = new Triangle(new Vector3f(), new Vector3f(), new Vector3f());

    private final Vector3f calcVec1 = new Vector3f();
    private final Ray workRay = new Ray();
    private final Ray worldPickRay = new Ray();

    private final TerrainQuad root;
    private final BresenhamYUpGridTracer tracer = new BresenhamYUpGridTracer();


    public BresenhamTerrainPicker(TerrainQuad root) {
        this.root = root;
    }

    public Vector3f getTerrainIntersection(Ray worldPick, CollisionResults results) {

        worldPickRay.set(worldPick);
        List<TerrainPickData> pickData = new ArrayList<TerrainPickData>();
        root.findPick(worldPick.clone(), pickData);
        Collections.sort(pickData);

        if (pickData.isEmpty())
            return null;

        workRay.set(worldPick);

        for (TerrainPickData pd : pickData) {
            TerrainPatch patch = pd.targetPatch;


            tracer.getGridSpacing().set(patch.getWorldScale());
            tracer.setGridOrigin(patch.getWorldTranslation());

            workRay.getOrigin().set(worldPick.getDirection()).multLocal(pd.cr.getDistance()-.1f).addLocal(worldPick.getOrigin());

            tracer.startWalk(workRay);

            final Vector3f intersection = new Vector3f();
            final Vector2f loc = tracer.getGridLocation();

            if (tracer.isRayPerpendicularToGrid()) {
                Triangle hit = new Triangle();
                checkTriangles(loc.x, loc.y, workRay, intersection, patch, hit);
                float distance = worldPickRay.origin.distance(intersection);
                CollisionResult cr = new CollisionResult(intersection, distance);
                cr.setGeometry(patch);
                cr.setContactNormal(hit.getNormal());
                results.addCollision(cr);
                return intersection;
            }
            
            

            while (loc.x >= -1 && loc.x <= patch.getSize() && 
                   loc.y >= -1 && loc.y <= patch.getSize()) {

                //System.out.print(loc.x+","+loc.y+" : ");
                // check the triangles of main square for intersection.
                Triangle hit = new Triangle();
                if (checkTriangles(loc.x, loc.y, workRay, intersection, patch, hit)) {
                    // we found an intersection, so return that!
                    float distance = worldPickRay.origin.distance(intersection);
                    CollisionResult cr = new CollisionResult(intersection, distance);
                    cr.setGeometry(patch);
                    results.addCollision(cr);
                    cr.setContactNormal(hit.getNormal());
                    return intersection;
                }

                // because of how we get our height coords, we will
                // sometimes be off by a grid spot, so we check the next
                // grid space up.
                int dx = 0, dz = 0;
                Direction d = tracer.getLastStepDirection();
                switch (d) {
                case PositiveX:
                case NegativeX:
                    dx = 0;
                    dz = 1;
                    break;
                case PositiveZ:
                case NegativeZ:
                    dx = 1;
                    dz = 0;
                    break;
                }

                if (checkTriangles(loc.x + dx, loc.y + dz, workRay, intersection, patch, hit)) {
                    // we found an intersection, so return that!
                    float distance = worldPickRay.origin.distance(intersection);
                    CollisionResult cr = new CollisionResult(intersection, distance);
                    results.addCollision(cr);
                    cr.setGeometry(patch);
                    cr.setContactNormal(hit.getNormal());
                    return intersection;
                }

                tracer.next();
            }
        }

        return null;
    }

    protected boolean checkTriangles(float gridX, float gridY, Ray pick, Vector3f intersection, TerrainPatch patch, Triangle store) {
        if (!getTriangles(gridX, gridY, patch))
            return false;

        if (pick.intersectWhere(gridTriA, intersection)) {
            store.set(gridTriA.get1(), gridTriA.get2(), gridTriA.get3());
            return true;
        } else {
            if (pick.intersectWhere(gridTriB, intersection)) {
                store.set(gridTriB.get1(), gridTriB.get2(), gridTriB.get3());
                return true;
            }
        }

        return false;
    }

    /**
     * Request the triangles (in world coord space) of a TerrainBlock that
     * correspond to the given grid location. The triangles are stored in the
     * class fields _gridTriA and _gridTriB.
     *
     * @param gridX
     *            grid row
     * @param gridY
     *            grid column
     * @param block
     *            the TerrainBlock we are working with
     * @return true if the grid point is valid for the given block, false if it
     *         is off the block.
     */
    protected boolean getTriangles(float gridX, float gridY, TerrainPatch patch) {
        calcVec1.set(gridX, 0, gridY);
        int index = findClosestHeightIndex(calcVec1, patch);

        if (index == -1)
            return false;
        
        Triangle[] t = patch.getGridTriangles(gridX, gridY);
        if (t == null || t.length == 0)
            return false;
        
        gridTriA.set1(t[0].get1());
        gridTriA.set2(t[0].get2());
        gridTriA.set3(t[0].get3());

        gridTriB.set1(t[1].get1());
        gridTriB.set2(t[1].get2());
        gridTriB.set3(t[1].get3());

        return true;
    }

    /**
     * Finds the closest height point to a position. Will always be left/above
     * that position.
     *
     * @param position
     *            the position to check at
     * @param block
     *            the block to get height values from
     * @return an index to the height position of the given block.
     */
    protected int findClosestHeightIndex(Vector3f position, TerrainPatch patch) {

        int x = (int) position.x;
        int z = (int) position.z;

        if (x < 0 || x >= patch.getSize() - 1) {
            return -1;
        }
        if (z < 0 || z >= patch.getSize() - 1) {
            return -1;
        }

        return z * patch.getSize() + x;
    }
}
