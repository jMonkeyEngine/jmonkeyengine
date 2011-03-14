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

package jme3test.collision;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.MotionAllowedListener;
import com.jme3.collision.SweepSphere;
import com.jme3.math.Plane;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

public class SphereMotionAllowedListener implements MotionAllowedListener {

    private Ray ray = new Ray();
    private SweepSphere ss = new SweepSphere();
    private CollisionResults results = new CollisionResults();
    private Spatial scene;
    private Vector3f dimension = new Vector3f();

    private Vector3f newPos = new Vector3f();
    private Vector3f newVel = new Vector3f();

    private float charHeight;
    private float footHeight;
    private float footStart;
    private float sphHeight;
    private float sphCenter;

    final float unitsPerMeter = 100.0f;
    final float unitScale = unitsPerMeter / 100.0f;
    final float veryCloseDist = 0.005f * unitScale;

    private int depth = 0;

    public SphereMotionAllowedListener(Spatial scene, Vector3f dimension){
        if (scene == null || dimension == null)
            throw new NullPointerException();

        this.scene = scene;
        
        charHeight = dimension.getY();

        footHeight = charHeight / 3f;
        footStart = -(charHeight / 2f) + footHeight;
        sphHeight = charHeight - footHeight;
        sphCenter = (charHeight / 2f) - (sphHeight / 2f);
        this.dimension.set(dimension);
        this.dimension.setY(sphHeight);
    }

    private void collideWithWorld(){
        if (depth > 3){
//            System.out.println("DEPTH LIMIT REACHED!!");
            return;
        }

        if (newVel.length() < veryCloseDist)
            return;

        Vector3f destination = newPos.add(0, sphCenter, 0).add(newVel);

        ss.setCenter(newPos.add(0, sphCenter, 0));
        ss.setVelocity(newVel);
        ss.setDimension(dimension);

        results.clear();
        scene.collideWith(ss, results);

        if (results.size() == 0){
            newPos.addLocal(newVel);
            return;
        }

        for (int i = 0; i < results.size(); i++){
            CollisionResult collision = results.getCollision(i);
            // *** collision occured ***
//            Vector3f destination = newPos.add(newVel);
            Vector3f contactPoint = collision.getContactPoint().clone();
            float dist = collision.getDistance();

            if (dist >= veryCloseDist){
                // P += ||V|| * dist
                Vector3f tmp = new Vector3f(newVel);
                tmp.normalizeLocal().multLocal(dist - veryCloseDist);
                newPos.addLocal(tmp);

                tmp.normalizeLocal();
                tmp.multLocal(veryCloseDist);
                contactPoint.subtractLocal(tmp);
            }

    //        Vector3f normal = newPos.subtract(contactPoint).normalizeLocal();
            Vector3f normal = collision.getContactNormal();

            Plane p = new Plane();
            p.setOriginNormal(contactPoint, normal);

            Vector3f destinationOnPlane = p.getClosestPoint(destination);
            newVel.set(destinationOnPlane).subtractLocal(contactPoint);
    //        normal.multLocal(normal.dot(destination) -  veryCloseDist);
    ////        normal.multLocal(p.pseudoDistance(destination));
    //        Vector3f newDest = destination.add(normal);
    //        newVel.set(newDest).subtractLocal(contactPoint);

            // recurse:
            if (newVel.length() < veryCloseDist){
                return;
            }
        }

        depth = depth + 1;
        collideWithWorld();
    }

    public void checkMotionAllowed(Vector3f position, Vector3f velocity) {
        if (velocity.getX() == 0 && velocity.getZ() == 0)
            return;

        depth = 0;
        newPos.set(position);
        newVel.set(velocity);
        velocity.setY(0);
//        newPos.addLocal(velocity);
        collideWithWorld();

        ray.setOrigin(newPos.add(0, footStart, 0));
        ray.setDirection(new Vector3f(0, -1, 0));
//        ray.setLimit(footHeight);

        results.clear();
        scene.collideWith(ray, results);
        CollisionResult result = results.getClosestCollision();
        if (result != null){
            newPos.y = result.getContactPoint().getY() + charHeight / 2f;
        }
            
        position.set(newPos);
    }


}
