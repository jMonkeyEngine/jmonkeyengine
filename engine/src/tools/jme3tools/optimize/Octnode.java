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

package jme3tools.optimize;

import com.jme3.bounding.BoundingBox;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Ray;
import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.debug.WireBox;
import java.util.*;

public class Octnode {

    static final Vector3f[] extentMult = new Vector3f[]
    {
        new Vector3f( 1, 1, 1), // right top forw
        new Vector3f(-1, 1, 1), // left top forw
        new Vector3f( 1,-1, 1), // right bot forw
        new Vector3f(-1,-1, 1), // left bot forw
        new Vector3f( 1, 1,-1), // right top back
        new Vector3f(-1, 1,-1), // left top back
        new Vector3f( 1,-1,-1), // right bot back
        new Vector3f(-1,-1,-1)  // left bot back
    };

    final BoundingBox bbox;
    final ArrayList<OCTTriangle> tris;
    Geometry[] geoms;
    final Octnode[] children = new Octnode[8];
    boolean leaf = false;
    FastOctnode fastNode;

    public Octnode(BoundingBox bbox, ArrayList<OCTTriangle> tris){
        this.bbox = bbox;
        this.tris = tris;
    }
    
    private BoundingBox getChildBound(int side){
        float extent = bbox.getXExtent() * 0.5f;
        Vector3f center = new Vector3f(bbox.getCenter().x + extent * extentMult[side].x,
                                       bbox.getCenter().y + extent * extentMult[side].y,
                                       bbox.getCenter().z + extent * extentMult[side].z);
        return new BoundingBox(center, extent, extent, extent);
    }

    private float getAdditionCost(BoundingBox bbox, OCTTriangle t){
        if (bbox.intersects(t.get1(), t.get2(), t.get3())){
            float d1 = bbox.distanceToEdge(t.get1());
            float d2 = bbox.distanceToEdge(t.get2());
            float d3 = bbox.distanceToEdge(t.get3());
            return d1 + d2 + d3;
        }
        return Float.POSITIVE_INFINITY;
    }

    private void expandBoxToContainTri(BoundingBox bbox, OCTTriangle t){
        Vector3f min = bbox.getMin(null);
        Vector3f max = bbox.getMax(null);
        BoundingBox.checkMinMax(min, max, t.get1());
        BoundingBox.checkMinMax(min, max, t.get2());
        BoundingBox.checkMinMax(min, max, t.get3());
        bbox.setMinMax(min, max);
    }

    private boolean contains(BoundingBox bbox, OCTTriangle t){
        if (bbox.contains(t.get1()) &&
            bbox.contains(t.get2()) &&
            bbox.contains(t.get3())){
            return true;
        }
        return false;
    }

    public void subdivide(int depth, int minTrisPerNode){
        if (tris == null || depth > 50 || bbox.getVolume() < 0.01f || tris.size() < minTrisPerNode){
            // no need to subdivide anymore
            leaf = true;
            return;
        }

        ArrayList<OCTTriangle> keepTris = new ArrayList<OCTTriangle>();
        ArrayList[] trisForChild = new ArrayList[8];
        BoundingBox[] boxForChild = new BoundingBox[8];
        // create boxes for children
        for (int i = 0; i < 8; i++){
            boxForChild[i] = getChildBound(i);
            trisForChild[i] = new ArrayList<Triangle>();
        }

        for (OCTTriangle t : tris){
            float lowestCost = Float.POSITIVE_INFINITY;
            int lowestIndex = -1;
            int numIntersecting = 0;
            for (int i = 0; i < 8; i++){
                BoundingBox childBox = boxForChild[i];
                float cost = getAdditionCost(childBox, t);
                if (cost < lowestCost){
                    lowestCost = cost;
                    lowestIndex = i;
                    numIntersecting++;
                }
            }
            if (numIntersecting < 8 && lowestIndex > -1){
                trisForChild[lowestIndex].add(t);
                expandBoxToContainTri(boxForChild[lowestIndex], t);
            }else{
                keepTris.add(t);
            }
//            boolean wasAdded = false;
//            for (int i = 0; i < 8; i++){
//                BoundingBox childBox = boxForChild[i];
//                if (contains(childBox, t)){
//                    trisForChild[i].add(t);
//                    wasAdded = true;
//                    break;
//                }
//            }
//            if (!wasAdded){
//                keepTris.add(t);
//            }
        }
        tris.retainAll(keepTris);
        for (int i = 0; i < 8; i++){
            if (trisForChild[i].size() > 0){
                children[i] = new Octnode(boxForChild[i], trisForChild[i]);
                children[i].subdivide(depth + 1, minTrisPerNode);
            }
        }
    }

    public void subdivide(int minTrisPerNode){
        subdivide(0, minTrisPerNode);
    }

    public void createFastOctnode(List<Geometry> globalGeomList){
        fastNode = new FastOctnode();

        if (geoms != null){
            Collection<Geometry> geomsColl = Arrays.asList(geoms);
            List<Geometry> myOptimizedList = GeometryBatchFactory.makeBatches(geomsColl);

            int startIndex = globalGeomList.size();
            globalGeomList.addAll(myOptimizedList);

            fastNode.setOffset(startIndex);
            fastNode.length = myOptimizedList.size();
        }else{
            fastNode.setOffset(0);
            fastNode.length = 0;
        }

        for (int i = 0; i < 8; i++){
            if (children[i] != null){
                children[i].createFastOctnode(globalGeomList);
            }
        }
    }

    public void generateFastOctnodeLinks(Octnode parent, Octnode nextSibling, int side){
        fastNode.setSide(side);
        fastNode.next = nextSibling != null ? nextSibling.fastNode : null;

        // We set the next sibling property by going in reverse order
        Octnode prev = null;
        for (int i = 7; i >= 0; i--){
            if (children[i] != null){
                children[i].generateFastOctnodeLinks(this, prev, i);
                prev = children[i];
            }
        }
        fastNode.child = prev != null ? prev.fastNode : null;
    }

    private void generateRenderSetNoCheck(Set<Geometry> renderSet, Camera cam){
        if (geoms != null){
            renderSet.addAll(Arrays.asList(geoms));
        }
        for (int i = 0; i < 8; i++){
            if (children[i] != null){
                children[i].generateRenderSetNoCheck(renderSet, cam);
            }
        }
    }

    public void generateRenderSet(Set<Geometry> renderSet, Camera cam){
//        generateRenderSetNoCheck(renderSet, cam);

        bbox.setCheckPlane(0);
        cam.setPlaneState(0);
        Camera.FrustumIntersect result = cam.contains(bbox);
        if (result != Camera.FrustumIntersect.Outside){
            if (geoms != null){
                renderSet.addAll(Arrays.asList(geoms));
            }
            for (int i = 0; i < 8; i++){
                if (children[i] != null){
                    if (result == Camera.FrustumIntersect.Inside){
                        children[i].generateRenderSetNoCheck(renderSet, cam);
                    }else{
                        children[i].generateRenderSet(renderSet, cam);
                    }
                }
            }
        }
    }

    public void collectTriangles(Geometry[] inGeoms){
        if (tris.size() > 0){
            List<Geometry> geomsList = TriangleCollector.gatherTris(inGeoms, tris);
            geoms = new Geometry[geomsList.size()];
            geomsList.toArray(geoms);
        }else{
            geoms = null;
        }
        for (int i = 0; i < 8; i++){
            if (children[i] != null){
                children[i].collectTriangles(inGeoms);
            }
        }
    }

    public void renderBounds(RenderQueue rq, Matrix4f transform, WireBox box, Material mat){
        int numChilds = 0;
        for (int i = 0; i < 8; i++){
            if (children[i] != null){
                numChilds ++;
                break;
            }
        }
        if (geoms != null && numChilds == 0){
            BoundingBox bbox2 = new BoundingBox(bbox);
            bbox.transform(transform, bbox2);
//            WireBox box = new WireBox(bbox2.getXExtent(), bbox2.getYExtent(),
//                                      bbox2.getZExtent());
//            WireBox box = new WireBox(1,1,1);

            Geometry geom = new Geometry("bound", box);
            geom.setLocalTranslation(bbox2.getCenter());
            geom.setLocalScale(bbox2.getXExtent(), bbox2.getYExtent(),
                               bbox2.getZExtent());
            geom.updateGeometricState();
            geom.setMaterial(mat);
            rq.addToQueue(geom, Bucket.Opaque);
            box = null;
            geom = null;
        }
        for (int i = 0; i < 8; i++){
            if (children[i] != null){
                children[i].renderBounds(rq, transform, box, mat);
            }
        }
    }

    public final void intersectWhere(Ray r, Geometry[] geoms, float sceneMin, float sceneMax,
                                            CollisionResults results){
        for (OCTTriangle t : tris){
            float d = r.intersects(t.get1(), t.get2(), t.get3());
            if (Float.isInfinite(d))
                continue;

            Vector3f contactPoint = new Vector3f(r.getDirection()).multLocal(d).addLocal(r.getOrigin());
            CollisionResult result = new CollisionResult(geoms[t.getGeometryIndex()],
                                                         contactPoint,
                                                         d,
                                                         t.getTriangleIndex());
            results.addCollision(result);
        }
        for (int i = 0; i < 8; i++){
            Octnode child = children[i];
            if (child == null)
                continue;

            if (child.bbox.intersects(r)){
                child.intersectWhere(r, geoms, sceneMin, sceneMax, results);
            }
        }
    }

}
