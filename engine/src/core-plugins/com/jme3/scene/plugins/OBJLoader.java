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

package com.jme3.scene.plugins;

import com.jme3.asset.*;
import com.jme3.material.Material;
import com.jme3.material.MaterialList;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.*;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.mesh.IndexIntBuffer;
import com.jme3.scene.mesh.IndexShortBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Map.Entry;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads OBJ format models.
 */
public final class OBJLoader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(OBJLoader.class.getName());

    protected final ArrayList<Vector3f> verts = new ArrayList<Vector3f>();
    protected final ArrayList<Vector2f> texCoords = new ArrayList<Vector2f>();
    protected final ArrayList<Vector3f> norms = new ArrayList<Vector3f>();
    
    protected final ArrayList<Face> faces = new ArrayList<Face>();
    protected final HashMap<String, ArrayList<Face>> matFaces = new HashMap<String, ArrayList<Face>>();
    
    protected String currentMatName;
    protected String currentObjectName;

    protected final HashMap<Vertex, Integer> vertIndexMap = new HashMap<Vertex, Integer>(100);
    protected final IntMap<Vertex> indexVertMap = new IntMap<Vertex>(100);
    protected int curIndex    = 0;
    protected int objectIndex = 0;
    protected int geomIndex   = 0;

    protected Scanner scan;
    protected ModelKey key;
    protected AssetManager assetManager;
    protected MaterialList matList;

    protected String objName;
    protected Node objNode;

    protected static class Vertex {

        Vector3f v;
        Vector2f vt;
        Vector3f vn;
        int index;

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Vertex other = (Vertex) obj;
            if (this.v != other.v && (this.v == null || !this.v.equals(other.v))) {
                return false;
            }
            if (this.vt != other.vt && (this.vt == null || !this.vt.equals(other.vt))) {
                return false;
            }
            if (this.vn != other.vn && (this.vn == null || !this.vn.equals(other.vn))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 53 * hash + (this.v != null ? this.v.hashCode() : 0);
            hash = 53 * hash + (this.vt != null ? this.vt.hashCode() : 0);
            hash = 53 * hash + (this.vn != null ? this.vn.hashCode() : 0);
            return hash;
        }
    }
    
    protected static class Face {
        Vertex[] verticies;
    }
    
    protected class ObjectGroup {
        
        final String objectName;
        
        public ObjectGroup(String objectName){
            this.objectName = objectName;
        }
        
        public Spatial createGeometry(){
            Node groupNode = new Node(objectName);
            
//            if (matFaces.size() > 0){
//                for (Entry<String, ArrayList<Face>> entry : matFaces.entrySet()){
//                    ArrayList<Face> materialFaces = entry.getValue();
//                    if (materialFaces.size() > 0){
//                        Geometry geom = createGeometry(materialFaces, entry.getKey());
//                        objNode.attachChild(geom);
//                    }
//                }
//            }else if (faces.size() > 0){
//                // generate final geometry
//                Geometry geom = createGeometry(faces, null);
//                objNode.attachChild(geom);
//            }
            
            return groupNode;
        }
    }

    public void reset(){
        verts.clear();
        texCoords.clear();
        norms.clear();
        faces.clear();
        matFaces.clear();

        vertIndexMap.clear();
        indexVertMap.clear();

        currentMatName = null;
        matList = null;
        curIndex = 0;
        geomIndex = 0;
        scan = null;
    }

    protected void findVertexIndex(Vertex vert){
        Integer index = vertIndexMap.get(vert);
        if (index != null){
            vert.index = index.intValue();
        }else{
            vert.index = curIndex++;
            vertIndexMap.put(vert, vert.index);
            indexVertMap.put(vert.index, vert);
        }
    }

    protected Face[] quadToTriangle(Face f){
        assert f.verticies.length == 4;

        Face[] t = new Face[]{ new Face(), new Face() };
        t[0].verticies = new Vertex[3];
        t[1].verticies = new Vertex[3];

        Vertex v0 = f.verticies[0];
        Vertex v1 = f.verticies[1];
        Vertex v2 = f.verticies[2];
        Vertex v3 = f.verticies[3];

        // find the pair of verticies that is closest to each over
        // v0 and v2
        // OR
        // v1 and v3
        float d1 = v0.v.distanceSquared(v2.v);
        float d2 = v1.v.distanceSquared(v3.v);
        if (d1 < d2){
            // put an edge in v0, v2
            t[0].verticies[0] = v0;
            t[0].verticies[1] = v1;
            t[0].verticies[2] = v3;

            t[1].verticies[0] = v1;
            t[1].verticies[1] = v2;
            t[1].verticies[2] = v3;
        }else{
            // put an edge in v1, v3
            t[0].verticies[0] = v0;
            t[0].verticies[1] = v1;
            t[0].verticies[2] = v2;

            t[1].verticies[0] = v0;
            t[1].verticies[1] = v2;
            t[1].verticies[2] = v3;
        }

        return t;
    }

    private ArrayList<Vertex> vertList = new ArrayList<Vertex>();

    protected void readFace(){
        Face f = new Face();
        vertList.clear();

        String line = scan.nextLine().trim();
        String[] verticies = line.split("\\s");
        for (String vertex : verticies){
            int v = 0;
            int vt = 0;
            int vn = 0;

            String[] split = vertex.split("/");
            if (split.length == 1){
                v = Integer.parseInt(split[0].trim());
            }else if (split.length == 2){
                v = Integer.parseInt(split[0].trim());
                vt = Integer.parseInt(split[1].trim());
            }else if (split.length == 3 && !split[1].equals("")){
                v = Integer.parseInt(split[0].trim());
                vt = Integer.parseInt(split[1].trim());
                vn = Integer.parseInt(split[2].trim());
            }else if (split.length == 3){
                v = Integer.parseInt(split[0].trim());
                vn = Integer.parseInt(split[2].trim());
            }

            Vertex vx = new Vertex();
            vx.v = verts.get(v - 1);

            if (vt > 0)
                vx.vt = texCoords.get(vt - 1);

            if (vn > 0)
                vx.vn = norms.get(vn - 1);

            vertList.add(vx);
        }

        if (vertList.size() > 4 || vertList.size() <= 2)
            logger.warning("Edge or polygon detected in OBJ. Ignored.");

        f.verticies = new Vertex[vertList.size()];
        for (int i = 0; i < vertList.size(); i++){
            f.verticies[i] = vertList.get(i);
        }

        if (matList != null && matFaces.containsKey(currentMatName)){
            matFaces.get(currentMatName).add(f);
        }else{
            faces.add(f); // faces that belong to the default material
        }
    }

    protected Vector3f readVector3(){
        Vector3f v = new Vector3f();

        v.set(Float.parseFloat(scan.next()),
              Float.parseFloat(scan.next()),
              Float.parseFloat(scan.next()));

        return v;
    }

    protected Vector2f readVector2(){
        Vector2f v = new Vector2f();

        String line = scan.nextLine().trim();
        String[] split = line.split("\\s");
        v.setX( Float.parseFloat(split[0].trim()) );
        v.setY( Float.parseFloat(split[1].trim()) );

//        v.setX(scan.nextFloat());
//        if (scan.hasNextFloat()){
//            v.setY(scan.nextFloat());
//            if (scan.hasNextFloat()){
//                scan.nextFloat(); // ignore
//            }
//        }

        return v;
    }

    protected void loadMtlLib(String name) throws IOException{
        if (!name.toLowerCase().endsWith(".mtl"))
            throw new IOException("Expected .mtl file! Got: " + name);

        // NOTE: Cut off any relative/absolute paths
        name = new File(name).getName();
        AssetKey mtlKey = new AssetKey(key.getFolder() + name);
        try {
            matList = (MaterialList) assetManager.loadAsset(mtlKey);
        } catch (AssetNotFoundException ex){
            logger.log(Level.WARNING, "Cannot locate {0} for model {1}", new Object[]{name, key});
        }

        if (matList != null){
            // create face lists for every material
            for (String matName : matList.keySet()){
                matFaces.put(matName, new ArrayList<Face>());
            }
        }
    }

    protected boolean nextStatement(){
        try {
            scan.skip(".*\r{0,1}\n");
            return true;
        } catch (NoSuchElementException ex){
            // EOF
            return false;
        }
    }

    protected boolean readLine() throws IOException{
        if (!scan.hasNext()){
            return false;
        }

        String cmd = scan.next();
        if (cmd.startsWith("#")){
            // skip entire comment until next line
            return nextStatement();
        }else if (cmd.equals("v")){
            // vertex position
            verts.add(readVector3());
        }else if (cmd.equals("vn")){
            // vertex normal
            norms.add(readVector3());
        }else if (cmd.equals("vt")){
            // texture coordinate
            texCoords.add(readVector2());
        }else if (cmd.equals("f")){
            // face, can be triangle, quad, or polygon (unsupported)
            readFace();
        }else if (cmd.equals("usemtl")){
            // use material from MTL lib for the following faces
            currentMatName = scan.next();
//            if (!matList.containsKey(currentMatName))
//                throw new IOException("Cannot locate material " + currentMatName + " in MTL file!");
            
        }else if (cmd.equals("mtllib")){
            // specify MTL lib to use for this OBJ file
            String mtllib = scan.nextLine().trim();
            loadMtlLib(mtllib);
        }else if (cmd.equals("s") || cmd.equals("g")){
            return nextStatement();
        }else{
            // skip entire command until next line
            logger.log(Level.WARNING, "Unknown statement in OBJ! {0}", cmd);
            return nextStatement();
        }     

        return true;
    }

    protected Geometry createGeometry(ArrayList<Face> faceList, String matName) throws IOException{
        if (faceList.isEmpty())
            throw new IOException("No geometry data to generate mesh");

        // Create mesh from the faces
        Mesh mesh = constructMesh(faceList);
        
        Geometry geom = new Geometry(objName + "-geom-" + (geomIndex++), mesh);
        
        Material material = null;
        if (matName != null && matList != null){
            // Get material from material list
            material = matList.get(matName);
        }
        if (material == null){
            // create default material
            material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            material.setFloat("Shininess", 64);
        }
        geom.setMaterial(material);
        if (material.isTransparent())
            geom.setQueueBucket(Bucket.Transparent);
        else
            geom.setQueueBucket(Bucket.Opaque);
        
        if (material.getMaterialDef().getName().contains("Lighting")
          && mesh.getFloatBuffer(Type.Normal) == null){
            logger.log(Level.WARNING, "OBJ mesh {0} doesn't contain normals! "
                                    + "It might not display correctly", geom.getName());
        }
        
        return geom;
    }

    protected Mesh constructMesh(ArrayList<Face> faceList){
        Mesh m = new Mesh();
        m.setMode(Mode.Triangles);

        boolean hasTexCoord = false;
        boolean hasNormals  = false;

        ArrayList<Face> newFaces = new ArrayList<Face>(faceList.size());
        for (int i = 0; i < faceList.size(); i++){
            Face f = faceList.get(i);

            for (Vertex v : f.verticies){
                findVertexIndex(v);

                if (!hasTexCoord && v.vt != null)
                    hasTexCoord = true;
                if (!hasNormals && v.vn != null)
                    hasNormals = true;
            }

            if (f.verticies.length == 4){
                Face[] t = quadToTriangle(f);
                newFaces.add(t[0]);
                newFaces.add(t[1]);
            }else{
                newFaces.add(f);
            }
        }

        FloatBuffer posBuf  = BufferUtils.createFloatBuffer(vertIndexMap.size() * 3);
        FloatBuffer normBuf = null;
        FloatBuffer tcBuf   = null;

        if (hasNormals){
            normBuf = BufferUtils.createFloatBuffer(vertIndexMap.size() * 3);
            m.setBuffer(VertexBuffer.Type.Normal, 3, normBuf);
        }
        if (hasTexCoord){
            tcBuf = BufferUtils.createFloatBuffer(vertIndexMap.size() * 2);
            m.setBuffer(VertexBuffer.Type.TexCoord, 2, tcBuf);
        }

        IndexBuffer indexBuf = null;
        if (vertIndexMap.size() >= 65536){
            // too many verticies: use intbuffer instead of shortbuffer
            IntBuffer ib = BufferUtils.createIntBuffer(newFaces.size() * 3);
            m.setBuffer(VertexBuffer.Type.Index, 3, ib);
            indexBuf = new IndexIntBuffer(ib);
        }else{
            ShortBuffer sb = BufferUtils.createShortBuffer(newFaces.size() * 3);
            m.setBuffer(VertexBuffer.Type.Index, 3, sb);
            indexBuf = new IndexShortBuffer(sb);
        }

        int numFaces = newFaces.size();
        for (int i = 0; i < numFaces; i++){
            Face f = newFaces.get(i);
            if (f.verticies.length != 3)
                continue;

            Vertex v0 = f.verticies[0];
            Vertex v1 = f.verticies[1];
            Vertex v2 = f.verticies[2];

            posBuf.position(v0.index * 3);
            posBuf.put(v0.v.x).put(v0.v.y).put(v0.v.z);
            posBuf.position(v1.index * 3);
            posBuf.put(v1.v.x).put(v1.v.y).put(v1.v.z);
            posBuf.position(v2.index * 3);
            posBuf.put(v2.v.x).put(v2.v.y).put(v2.v.z);

            if (normBuf != null){
                if (v0.vn != null){
                    normBuf.position(v0.index * 3);
                    normBuf.put(v0.vn.x).put(v0.vn.y).put(v0.vn.z);
                    normBuf.position(v1.index * 3);
                    normBuf.put(v1.vn.x).put(v1.vn.y).put(v1.vn.z);
                    normBuf.position(v2.index * 3);
                    normBuf.put(v2.vn.x).put(v2.vn.y).put(v2.vn.z);
                }
            }

            if (tcBuf != null){
                if (v0.vt != null){
                    tcBuf.position(v0.index * 2);
                    tcBuf.put(v0.vt.x).put(v0.vt.y);
                    tcBuf.position(v1.index * 2);
                    tcBuf.put(v1.vt.x).put(v1.vt.y);
                    tcBuf.position(v2.index * 2);
                    tcBuf.put(v2.vt.x).put(v2.vt.y);
                }
            }

            int index = i * 3; // current face * 3 = current index
            indexBuf.put(index,   v0.index);
            indexBuf.put(index+1, v1.index);
            indexBuf.put(index+2, v2.index);
        }

        m.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        // index buffer and others were set on creation

        m.setStatic();
        m.updateBound();
        m.updateCounts();
        //m.setInterleaved();

        // clear data generated face statements
        // to prepare for next mesh
        vertIndexMap.clear();
        indexVertMap.clear();
        curIndex = 0;

        return m;
    }

    @SuppressWarnings("empty-statement")
    public Object load(AssetInfo info) throws IOException{
        reset();
        
        key = (ModelKey) info.getKey();
        assetManager = info.getManager();
        objName    = key.getName();
        
        String folderName = key.getFolder();
        String ext        = key.getExtension();
        objName = objName.substring(0, objName.length() - ext.length() - 1);
        if (folderName != null && folderName.length() > 0){
            objName = objName.substring(folderName.length());
        }

        objNode = new Node(objName + "-objnode");

        if (!(info.getKey() instanceof ModelKey))
            throw new IllegalArgumentException("Model assets must be loaded using a ModelKey");

        InputStream in = null; 
        try {
            in = info.openStream();
            
            scan = new Scanner(in);
            scan.useLocale(Locale.US);

            while (readLine());
        } finally {
            if (in != null){
                in.close();
            }
        }
        
        if (matFaces.size() > 0){
            for (Entry<String, ArrayList<Face>> entry : matFaces.entrySet()){
                ArrayList<Face> materialFaces = entry.getValue();
                if (materialFaces.size() > 0){
                    Geometry geom = createGeometry(materialFaces, entry.getKey());
                    objNode.attachChild(geom);
                }
            }
        }else if (faces.size() > 0){
            // generate final geometry
            Geometry geom = createGeometry(faces, null);
            objNode.attachChild(geom);
        }

        if (objNode.getQuantity() == 1)
            // only 1 geometry, so no need to send node
            return objNode.getChild(0); 
        else
            return objNode;
    }
 
}
