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
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Ray;
import com.jme3.math.Triangle;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Octree {

    private final ArrayList<OCTTriangle> allTris = new ArrayList<OCTTriangle>();
    private final Geometry[] geoms;
    private final BoundingBox bbox;
    private final int minTrisPerNode;
    private Octnode root;

    private CollisionResults boundResults = new CollisionResults();

    private static List<Geometry> getGeometries(Spatial scene){
        if (scene instanceof Geometry){
            List<Geometry> geomList = new ArrayList<Geometry>(1);
            geomList.add((Geometry) scene);
            return geomList;
        }else if (scene instanceof Node){
            Node n = (Node) scene;
            List<Geometry> geoms = new ArrayList<Geometry>();
            for (Spatial child : n.getChildren()){
                geoms.addAll(getGeometries(child));
            }
            return geoms;
        }else{
            throw new UnsupportedOperationException("Unsupported scene element class");
        }
    }

    public Octree(Spatial scene, int minTrisPerNode){
        scene.updateGeometricState();

        List<Geometry> geomsList = getGeometries(scene);
        geoms = new Geometry[geomsList.size()];
        geomsList.toArray(geoms);
        // generate bound box for all geom
        bbox = new BoundingBox();
        for (Geometry geom : geoms){
            BoundingVolume bv = geom.getWorldBound();
            bbox.mergeLocal(bv);
        }

        // set largest extent
        float extent = Math.max(bbox.getXExtent(), Math.max(bbox.getYExtent(), bbox.getZExtent()));
        bbox.setXExtent(extent);
        bbox.setYExtent(extent);
        bbox.setZExtent(extent);

        this.minTrisPerNode = minTrisPerNode;

        Triangle t = new Triangle();
        for (int g = 0; g < geoms.length; g++){
            Mesh m = geoms[g].getMesh();
            for (int i = 0; i < m.getTriangleCount(); i++){
                m.getTriangle(i, t);
                OCTTriangle ot = new OCTTriangle(t.get1(), t.get2(), t.get3(), i, g);
                allTris.add(ot);
                // convert triangle to world space
//                geom.getWorldTransform().transformVector(t.get1(), t.get1());
//                geom.getWorldTransform().transformVector(t.get2(), t.get2());
//                geom.getWorldTransform().transformVector(t.get3(), t.get3());
            }
        }
    }

    public Octree(Spatial scene){
        this(scene,11);
    }

    public void construct(){
        root = new Octnode(bbox, allTris);
        root.subdivide(minTrisPerNode);
        root.collectTriangles(geoms);
    }

    public void createFastOctnodes(List<Geometry> globalGeomList){
        root.createFastOctnode(globalGeomList);
    }

    public BoundingBox getBound(){
        return bbox;
    }

    public FastOctnode getFastRoot(){
        return root.fastNode;
    }

    public void generateFastOctnodeLinks(){
        root.generateFastOctnodeLinks(null, null, 0);
    }
    
    public void generateRenderSet(Set<Geometry> renderSet, Camera cam){
        root.generateRenderSet(renderSet, cam);
    }

    public void renderBounds(RenderQueue rq, Matrix4f transform, WireBox box, Material mat){
        root.renderBounds(rq, transform, box, mat);
    }

    public void intersect(Ray r, float farPlane, Geometry[] geoms, CollisionResults results){
        boundResults.clear();
        bbox.collideWith(r, boundResults);
        if (boundResults.size() > 0){
            float tMin = boundResults.getClosestCollision().getDistance();
            float tMax = boundResults.getFarthestCollision().getDistance();

            tMin = Math.max(tMin, 0);
            tMax = Math.min(tMax, farPlane);

            root.intersectWhere(r, geoms, tMin, tMax, results);
        }
    }
}
