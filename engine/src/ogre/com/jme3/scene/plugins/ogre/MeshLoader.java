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

package com.jme3.scene.plugins.ogre;

import com.jme3.animation.AnimControl;
import com.jme3.animation.BoneAnimation;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.material.Material;
import com.jme3.material.MaterialList;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import static com.jme3.util.xml.SAXUtil.*;

/**
 * Loads Ogre3D mesh.xml files.
 */
public class MeshLoader extends DefaultHandler implements AssetLoader {

    private static final Logger logger = Logger.getLogger(MeshLoader.class.getName());

    public static boolean AUTO_INTERLEAVE = true;
    public static boolean HARDWARE_SKINNING = false;

    private String meshName;
    private String folderName;
    private AssetManager assetManager;
    private MaterialList materialList;

    private ShortBuffer sb;
    private IntBuffer ib;
    private FloatBuffer fb;
    private VertexBuffer vb;
    private Mesh mesh;
    private Geometry geom;
    private Mesh sharedmesh;
    private Geometry sharedgeom;
    private int geomIdx = 0;
    private int texCoordIdx = 0;
    private static volatile int nodeIdx = 0;
    private String ignoreUntilEnd = null;
    private boolean bigindices = false;
    private int vertCount;
    private int triCount;

    private List<Geometry> geoms = new ArrayList<Geometry>();
    private List<Boolean> usesSharedGeom = new ArrayList<Boolean>();
    private IntMap<List<VertexBuffer>> lodLevels = new IntMap<List<VertexBuffer>>();
    private AnimData animData;

    private ByteBuffer indicesData;
    private FloatBuffer weightsFloatData;

    public MeshLoader(){
        super();
    }

    @Override
    public void startDocument() {
        geoms.clear();
        usesSharedGeom.clear();
        lodLevels.clear();

        sb = null;
        ib = null;
        fb = null;
        vb = null;
        mesh = null;
        geom = null;
        sharedgeom = null;
        sharedmesh = null;

        vertCount = 0;
        triCount = 0;
        geomIdx = 0;
        texCoordIdx = 0;
        nodeIdx = 0;
        ignoreUntilEnd = null;

        animData = null;

        indicesData = null;
        weightsFloatData = null;
    }

    @Override
    public void endDocument() {
    }

    private void pushFace(String v1, String v2, String v3) throws SAXException{
        int i1 = parseInt(v1);

        // TODO: fan/strip support
        int i2 = parseInt(v2);
        int i3 = parseInt(v3);
        if (ib != null){
            ib.put(i1).put(i2).put(i3);
        }else{
            sb.put((short)i1).put((short)i2).put((short)i3);
        }
    }

    private void startFaces(String count) throws SAXException{
        int numFaces = parseInt(count);
        int numIndices;

        if (mesh.getMode() == Mesh.Mode.Triangles){
            //mesh.setTriangleCount(numFaces);
            numIndices = numFaces * 3;
        }else{
            throw new SAXException("Triangle strip or fan not supported!");
        }

        int numVerts;
        if (usesSharedGeom.size() > 0 && usesSharedGeom.get(geoms.size()-1)){
//            sharedgeom.getMesh().updateCounts();
            numVerts = sharedmesh.getVertexCount();
        }else{
//            mesh.updateCounts();
            numVerts = mesh.getVertexCount();
        }
        vb = new VertexBuffer(VertexBuffer.Type.Index);
        if (!bigindices){
            sb = BufferUtils.createShortBuffer(numIndices);
            ib = null;
            vb.setupData(Usage.Static, 3, Format.UnsignedShort, sb);
        }else{
            ib = BufferUtils.createIntBuffer(numIndices);
            sb = null;
            vb.setupData(Usage.Static, 3, Format.UnsignedInt, ib);
        }
        mesh.setBuffer(vb);
    }

    private void applyMaterial(Geometry geom, String matName){
        Material mat = null;
        if (matName.endsWith(".j3m")){
            // load as native jme3 material instance
            mat = assetManager.loadMaterial(matName);
        }else{
            if (materialList != null){
                mat = materialList.get(matName);
            }
            if (mat == null){
                logger.log(Level.WARNING, "Material {0} not found. Applying default material", matName);
                mat = (Material) assetManager.loadAsset(new AssetKey("Common/Materials/RedColor.j3m"));
            }
        }
        
        if (mat == null)
            throw new RuntimeException("Cannot locate material named " + matName);

        if (mat.isTransparent())
            geom.setQueueBucket(Bucket.Transparent);
//        else
//            geom.setShadowMode(ShadowMode.CastAndReceive);
        
//        if (mat.isReceivesShadows())
            
            
        geom.setMaterial(mat);
    }

    private void startMesh(String matName, String usesharedvertices, String use32bitIndices, String opType) throws SAXException{
        mesh = new Mesh();
        if (opType == null || opType.equals("triangle_list")){
            mesh.setMode(Mesh.Mode.Triangles);
        }else if (opType.equals("triangle_strip")){
            mesh.setMode(Mesh.Mode.TriangleStrip);
        }else if (opType.equals("triangle_fan")){
            mesh.setMode(Mesh.Mode.TriangleFan);
        }

        bigindices = parseBool(use32bitIndices, false);
        boolean sharedverts = parseBool(usesharedvertices, false);
        if (sharedverts){
            usesSharedGeom.add(true);
            // import vertexbuffers from shared geom
            IntMap<VertexBuffer> sharedBufs = sharedmesh.getBuffers();
            for (Entry<VertexBuffer> entry : sharedBufs){
                    mesh.setBuffer(entry.getValue());
            }
            // this mesh is shared!
        }else{
            usesSharedGeom.add(false);
        }

        if (meshName == null)
            geom = new Geometry("OgreSubmesh-"+(++geomIdx), mesh);
        else
            geom = new Geometry(meshName+"-geom-"+(++geomIdx), mesh);

        applyMaterial(geom, matName);
        geoms.add(geom);
    }

    private void startSharedGeom(String vertexcount) throws SAXException{
        sharedmesh = new Mesh();
        vertCount = parseInt(vertexcount);
//        sharedmesh.setVertexCount(vertCount);

        if (meshName == null)
            sharedgeom = new Geometry("Ogre-SharedGeom", sharedmesh);
        else
            sharedgeom = new Geometry(meshName+"-sharedgeom", sharedmesh);

        sharedgeom.setCullHint(CullHint.Always);
        geoms.add(sharedgeom);
        usesSharedGeom.add(false); // shared geometry doesnt use shared geometry (?)

        geom = sharedgeom;
        mesh = sharedmesh;
    }

    private void startGeometry(String vertexcount) throws SAXException{
        vertCount = parseInt(vertexcount);
//        mesh.setVertexCount(vertCount);
    }

    /**
     * Normalizes weights if needed and finds largest amount of weights used
     * for all vertices in the buffer.
     */
    private void endBoneAssigns(){
        if (mesh != sharedmesh && usesSharedGeom.get(geoms.size() - 1)){
            return;
        }

        //int vertCount = mesh.getVertexCount();
        int maxWeightsPerVert = 0;
        weightsFloatData.rewind();
        for (int v = 0; v < vertCount; v++){
            float w0 = weightsFloatData.get(),
                  w1 = weightsFloatData.get(),
                  w2 = weightsFloatData.get(),
                  w3 = weightsFloatData.get();

            if (w3 != 0){
                maxWeightsPerVert = Math.max(maxWeightsPerVert, 4);
            }else if (w2 != 0){
                maxWeightsPerVert = Math.max(maxWeightsPerVert, 3);
            }else if (w1 != 0){
                maxWeightsPerVert = Math.max(maxWeightsPerVert, 2);
            }else if (w0 != 0){
                maxWeightsPerVert = Math.max(maxWeightsPerVert, 1);
            }

            float sum = w0 + w1 + w2 + w3;
            if (sum != 1f){
                weightsFloatData.position(weightsFloatData.position()-4);
                // compute new vals based on sum
                float sumToB = 1f / sum;
                weightsFloatData.put( w0 * sumToB );
                weightsFloatData.put( w1 * sumToB );
                weightsFloatData.put( w2 * sumToB );
                weightsFloatData.put( w3 * sumToB );
            }
        }
        weightsFloatData.rewind();

        weightsFloatData = null;
        indicesData = null;
        
        mesh.setMaxNumWeights(maxWeightsPerVert);
    }

    private void startBoneAssigns(){
        if (mesh != sharedmesh && usesSharedGeom.get(geoms.size() - 1)){
            // will use bone assignments from shared mesh (?)
            return;
        }

        // current mesh will have bone assigns
        //int vertCount = mesh.getVertexCount();
        // each vertex has
        // - 4 bone weights
        // - 4 bone indices
        if (HARDWARE_SKINNING){
            weightsFloatData = BufferUtils.createFloatBuffer(vertCount * 4);
            indicesData = BufferUtils.createByteBuffer(vertCount * 4);
        }else{
            // create array-backed buffers if software skinning for access speed
            weightsFloatData = FloatBuffer.allocate(vertCount * 4);
            indicesData = ByteBuffer.allocate(vertCount * 4);
        }
        
        VertexBuffer weights = new VertexBuffer(Type.BoneWeight);
        VertexBuffer indices = new VertexBuffer(Type.BoneIndex);

        Usage usage = HARDWARE_SKINNING ? Usage.Static : Usage.CpuOnly;
        weights.setupData(usage, 4, Format.Float, weightsFloatData);
        indices.setupData(usage, 4, Format.UnsignedByte, indicesData);
        
        mesh.setBuffer(weights);
        mesh.setBuffer(indices);
    }

    private void startVertexBuffer(Attributes attribs) throws SAXException{
        if (parseBool(attribs.getValue("positions"), false)){
            vb = new VertexBuffer(Type.Position);
            fb = BufferUtils.createFloatBuffer(vertCount * 3);
            vb.setupData(Usage.Static, 3, Format.Float, fb);
            mesh.setBuffer(vb);
        }
        if (parseBool(attribs.getValue("normals"), false)){
            vb = new VertexBuffer(Type.Normal);
            fb = BufferUtils.createFloatBuffer(vertCount * 3);
            vb.setupData(Usage.Static, 3, Format.Float, fb);
            mesh.setBuffer(vb);
        }
        if (parseBool(attribs.getValue("colours_diffuse"), false)){
            vb = new VertexBuffer(Type.Color);
            fb = BufferUtils.createFloatBuffer(vertCount * 4);
            vb.setupData(Usage.Static, 4, Format.Float, fb);
            mesh.setBuffer(vb);
        }
        if (parseBool(attribs.getValue("tangents"), false)){
            int dimensions = parseInt(attribs.getValue("tangent_dimensions"), 3);
            vb = new VertexBuffer(Type.Tangent);
            fb = BufferUtils.createFloatBuffer(vertCount * dimensions);
            vb.setupData(Usage.Static, dimensions, Format.Float, fb);
            mesh.setBuffer(vb);
        }
        if (parseBool(attribs.getValue("binormals"), false)){
            vb = new VertexBuffer(Type.Binormal);
            fb = BufferUtils.createFloatBuffer(vertCount * 3);
            vb.setupData(Usage.Static, 3, Format.Float, fb);
            mesh.setBuffer(vb);
        }

        int texCoords = parseInt(attribs.getValue("texture_coords"), 0);
        for (int i = 0; i < texCoords; i++){
            int dims = parseInt(attribs.getValue("texture_coord_dimensions_" + i), 2);
            if (dims < 1 || dims > 4)
                throw new SAXException("Texture coord dimensions must be 1 <= dims <= 4");

            if (i >= 2)
                throw new SAXException("More than 2 texture coordinates not supported");

            if (i == 0){
                vb = new VertexBuffer(Type.TexCoord);
            }else{
                vb = new VertexBuffer(Type.TexCoord2);
            }
            fb = BufferUtils.createFloatBuffer(vertCount * dims);
            vb.setupData(Usage.Static, dims, Format.Float, fb);
            mesh.setBuffer(vb);
        }
    }

    private void startVertex(){
        texCoordIdx = 0;
    }

    private void pushAttrib(Type type, Attributes attribs) throws SAXException{
        try {
            FloatBuffer buf = (FloatBuffer) mesh.getBuffer(type).getData();
            buf.put(parseFloat(attribs.getValue("x")))
           .put(parseFloat(attribs.getValue("y")))
           .put(parseFloat(attribs.getValue("z")));
        } catch (Exception ex){
           throw new SAXException("Failed to push attrib", ex);
        }
    }

    private void pushTangent(Attributes attribs) throws SAXException{
        try {
            VertexBuffer tangentBuf = mesh.getBuffer(Type.Tangent);
            FloatBuffer buf = (FloatBuffer) tangentBuf.getData();
            buf.put(parseFloat(attribs.getValue("x")))
           .put(parseFloat(attribs.getValue("y")))
           .put(parseFloat(attribs.getValue("z")));
            if (tangentBuf.getNumComponents() == 4){
                buf.put(parseFloat(attribs.getValue("w")));
            }
        } catch (Exception ex){
           throw new SAXException("Failed to push attrib", ex);
        }
    }

    private void pushTexCoord(Attributes attribs) throws SAXException{
        if (texCoordIdx >= 1)
            return; // TODO: Support multi-texcoords
        
        VertexBuffer tcvb = mesh.getBuffer(Type.TexCoord);
        FloatBuffer buf = (FloatBuffer) tcvb.getData();

        buf.put(parseFloat(attribs.getValue("u")));
        if (tcvb.getNumComponents() >= 2){
            buf.put(parseFloat(attribs.getValue("v")));
            if (tcvb.getNumComponents() >= 3){
                buf.put(parseFloat(attribs.getValue("w")));
                if (tcvb.getNumComponents() == 4){
                    buf.put(parseFloat(attribs.getValue("x")));
                }
            }
        }

        texCoordIdx++;
    }

    private void pushColor(Attributes attribs) throws SAXException{
        FloatBuffer buf = (FloatBuffer) mesh.getBuffer(Type.Color).getData();
        String value = parseString(attribs.getValue("value"));
        String[] vals = value.split(" ");
        if (vals.length != 3 && vals.length != 4)
            throw new SAXException("Color value must contain 3 or 4 components");

        ColorRGBA color = new ColorRGBA();
        color.r = parseFloat(vals[0]);
        color.g = parseFloat(vals[1]);
        color.b = parseFloat(vals[2]);
        if (vals.length == 3)
            color.a = 1f;
        else
            color.a = parseFloat(vals[3]);
        
        buf.put(color.r).put(color.g).put(color.b).put(color.a);
    }

    private void startLodFaceList(String submeshindex, String numfaces){
        int index = Integer.parseInt(submeshindex);
        int faceCount = Integer.parseInt(numfaces);
        
        vb = new VertexBuffer(VertexBuffer.Type.Index);
        sb = BufferUtils.createShortBuffer(faceCount * 3);
        ib = null;
        vb.setupData(Usage.Static, 3, Format.UnsignedShort, sb);

        List<VertexBuffer> levels = lodLevels.get(index);
        if (levels == null){
            levels = new ArrayList<VertexBuffer>();
            Mesh submesh = geoms.get(index).getMesh();
            levels.add(submesh.getBuffer(Type.Index));
            lodLevels.put(index, levels);
        }

        levels.add(vb);
    }

    private void startLevelOfDetail(String numlevels){
//        numLevels = Integer.parseInt(numlevels);
    }

    private void endLevelOfDetail(){
        // set the lod data for each mesh
        for (Entry<List<VertexBuffer>> entry : lodLevels){
            Mesh m = geoms.get(entry.getKey()).getMesh();
            List<VertexBuffer> levels = entry.getValue();
            VertexBuffer[] levelArray = new VertexBuffer[levels.size()];
            levels.toArray(levelArray);
            m.setLodLevels(levelArray);
        }
    }

    private void startLodGenerated(String depthsqr){
//        dist = Float.parseFloat(depthsqr);
    }

    private void pushBoneAssign(String vertIndex, String boneIndex, String weight) throws SAXException{
        int vert = parseInt(vertIndex);
        float w = parseFloat(weight);
        byte bone = (byte) parseInt(boneIndex);

        assert bone >= 0;
        assert vert >= 0 && vert < mesh.getVertexCount();

        int i;
        // see which weights are unused for a given bone
        for (i = vert * 4; i < vert * 4 + 4; i++){
            float v = weightsFloatData.get(i);
            if (v == 0)
                break;
        }

        weightsFloatData.put(i, w);
        indicesData.put(i, bone);
    }

    private void startSkeleton(String name){
        animData = (AnimData) assetManager.loadAsset(folderName + name + ".xml");
        //TODO:workaround for meshxml / mesh.xml
        if(animData==null)
            animData = (AnimData) assetManager.loadAsset(folderName + name + "xml");
    }

    private void startSubmeshName(String indexStr, String nameStr){
        int index = Integer.parseInt(indexStr);
        geoms.get(index).setName(nameStr);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attribs) throws SAXException{
        if (ignoreUntilEnd != null)
            return;

        if (qName.equals("texcoord")){
            pushTexCoord(attribs);
        }else if (qName.equals("vertexboneassignment")){
            pushBoneAssign(attribs.getValue("vertexindex"),
                           attribs.getValue("boneindex"),
                           attribs.getValue("weight"));
        }else if (qName.equals("face")){
            pushFace(attribs.getValue("v1"),
                     attribs.getValue("v2"),
                     attribs.getValue("v3"));
        }else if (qName.equals("position")){
            pushAttrib(Type.Position, attribs);
        }else if (qName.equals("normal")){
            pushAttrib(Type.Normal, attribs);
        }else if (qName.equals("tangent")){
            pushTangent(attribs);
        }else if (qName.equals("binormal")){
            pushAttrib(Type.Binormal, attribs);
        }else if (qName.equals("colour_diffuse")){
            pushColor(attribs);
        }else if (qName.equals("vertex")){
            startVertex(); 
        }else if (qName.equals("faces")){
            startFaces(attribs.getValue("count"));
        }else if (qName.equals("geometry")){
            String count = attribs.getValue("vertexcount");
            if (count == null)
                count = attribs.getValue("count");

            startGeometry(count);
        }else if (qName.equals("vertexbuffer")){
            startVertexBuffer(attribs);
        }else if (qName.equals("lodfacelist")){
            startLodFaceList(attribs.getValue("submeshindex"),
                             attribs.getValue("numfaces"));
        }else if (qName.equals("lodgenerated")){
            startLodGenerated(attribs.getValue("fromdepthsquared"));
        }else if (qName.equals("levelofdetail")){
            startLevelOfDetail(attribs.getValue("numlevels"));
        }else if (qName.equals("boneassignments")){
            startBoneAssigns();
        }else if (qName.equals("submesh")){
            startMesh(attribs.getValue("material"),
                      attribs.getValue("usesharedvertices"),
                      attribs.getValue("use32bitindexes"),
                      attribs.getValue("operationtype"));
        }else if (qName.equals("sharedgeometry")){
            String count = attribs.getValue("vertexcount");
            if (count == null)
                count = attribs.getValue("count");

            if (count != null && !count.equals("0"))
                startSharedGeom(count);
        }else if (qName.equals("submeshes")){
            // ok
        }else if (qName.equals("skeletonlink")){
            startSkeleton(attribs.getValue("name"));
        }else if (qName.equals("submeshname")){
            startSubmeshName(attribs.getValue("index"), attribs.getValue("name"));
        }else if (qName.equals("mesh")){
            // ok
        }else{
            logger.log(Level.WARNING, "Unknown tag: {0}. Ignoring.", qName);
            ignoreUntilEnd = qName;
        }
    }

    @Override
    public void endElement(String uri, String name, String qName) {
        if (ignoreUntilEnd != null){
            if (ignoreUntilEnd.equals(qName))
                ignoreUntilEnd = null;

            return;
        }

        if (qName.equals("submesh")){
            bigindices = false;
            geom = null;
            mesh = null;
        }else if (qName.equals("submeshes")){
            // IMPORTANT: restore sharedgeoemtry, for use with shared boneweights
            geom = sharedgeom;
            mesh = sharedmesh;
        }else if (qName.equals("faces")){
            if (ib != null)
                ib.flip();
            else
                sb.flip();
            
            vb = null;
            ib = null;
            sb = null;
        }else if (qName.equals("vertexbuffer")){
            fb = null;
            vb = null;
        }else if (qName.equals("geometry")
               || qName.equals("sharedgeometry")){
            // finish writing to buffers
            IntMap<VertexBuffer> bufs = mesh.getBuffers();
            for (Entry<VertexBuffer> entry : bufs){
                Buffer data = entry.getValue().getData();
                if (data.position() != 0)
                    data.flip();
            }
            mesh.updateBound();
            mesh.setStatic();

            if (qName.equals("sharedgeometry")){
                geom = null;
                mesh = null;
            }
        }else if (qName.equals("lodfacelist")){
            sb.flip();
            vb = null;
            sb = null;
        }else if (qName.equals("levelofdetail")){
            endLevelOfDetail();
        }else if (qName.equals("boneassignments")){
            endBoneAssigns();
        }
    }

    @Override
    public void characters(char ch[], int start, int length) {
    }

    private void createBindPose(Mesh mesh){
        VertexBuffer pos = mesh.getBuffer(Type.Position);
        if (pos == null || mesh.getBuffer(Type.BoneIndex) == null){
            // ignore, this mesh doesn't have positional data
            // or it doesn't have bone-vertex assignments, so its not animated
            return;
        }

        VertexBuffer bindPos = new VertexBuffer(Type.BindPosePosition);
        bindPos.setupData(Usage.CpuOnly,
                          3,
                          Format.Float,
                          BufferUtils.clone(pos.getData()));
        mesh.setBuffer(bindPos);

        // XXX: note that this method also sets stream mode
        // so that animation is faster. this is not needed for hardware skinning
        pos.setUsage(Usage.Stream);

        VertexBuffer norm = mesh.getBuffer(Type.Normal);
        if (norm != null){
            VertexBuffer bindNorm = new VertexBuffer(Type.BindPoseNormal);
            bindNorm.setupData(Usage.CpuOnly,
                              3,
                              Format.Float,
                              BufferUtils.clone(norm.getData()));
            mesh.setBuffer(bindNorm);
            norm.setUsage(Usage.Stream);
        }
    }

    private Node compileModel(){
        String nodeName;
        if (meshName == null)
            nodeName = "OgreMesh"+(++nodeIdx);
        else
            nodeName = meshName+"-ogremesh";

        Node model = new Node(nodeName);
        if (animData != null){
            ArrayList<Mesh> newMeshes = new ArrayList<Mesh>(geoms.size());

            // generate bind pose for mesh and add to skin-list
            // ONLY if not using shared geometry
            // This includes the shared geoemtry itself actually
            for (int i = 0; i < geoms.size(); i++){
                Geometry g = geoms.get(i);
                Mesh m = geoms.get(i).getMesh();
                boolean useShared = usesSharedGeom.get(i);
                // create bind pose
                if (!useShared){
                    createBindPose(m);
                    newMeshes.add(m);
                }else{
                    VertexBuffer bindPos  = sharedmesh.getBuffer(Type.BindPosePosition);
                    VertexBuffer bindNorm = sharedmesh.getBuffer(Type.BindPoseNormal);
                    VertexBuffer boneIndex = sharedmesh.getBuffer(Type.BoneIndex);
                    VertexBuffer boneWeight = sharedmesh.getBuffer(Type.BoneWeight);
                    
                    if (bindPos != null)
                        m.setBuffer(bindPos);

                    if (bindNorm != null)
                        m.setBuffer(bindNorm);

                    if (boneIndex != null)
                        m.setBuffer(boneIndex);

                    if (boneWeight != null)
                        m.setBuffer(boneWeight);
                }
            }
            Mesh[] meshes = new Mesh[newMeshes.size()];
            for (int i = 0; i < meshes.length; i++)
                meshes[i] = newMeshes.get(i);

            HashMap<String, BoneAnimation> anims = new HashMap<String, BoneAnimation>();
            ArrayList<BoneAnimation> animList = animData.anims;
            for (int i = 0; i < animList.size(); i++){
                BoneAnimation anim = animList.get(i);
                anims.put(anim.getName(), anim);
            }

            AnimControl ctrl = new AnimControl(model,
                                               meshes,
                                               animData.skeleton);
            ctrl.setAnimations(anims);
            model.addControl(ctrl);
        }

        for (int i = 0; i < geoms.size(); i++){
            Geometry g = geoms.get(i);
            Mesh m = g.getMesh();
            if (sharedmesh != null && usesSharedGeom.get(i)){
                m.setBound(sharedmesh.getBound().clone());
            }
            model.attachChild(geoms.get(i));
        }
        
        return model;
    }

    public Object load(AssetInfo info) throws IOException {
        try{
            AssetKey key = info.getKey();
            meshName = key.getName();
            folderName = key.getFolder();
            String ext = key.getExtension();
            meshName = meshName.substring(0, meshName.length() - ext.length() - 1);
            if (folderName != null && folderName.length() > 0){
                meshName = meshName.substring(folderName.length());
            }
            assetManager = info.getManager();

            OgreMeshKey meshKey = null;       
            if (key instanceof OgreMeshKey){
                meshKey = (OgreMeshKey) key;
                materialList = meshKey.getMaterialList();
            }else{             
                try {
                    materialList = (MaterialList) assetManager.loadAsset(folderName + meshName + ".material");
                } catch (AssetNotFoundException e) {
                    logger.log(Level.WARNING, "Cannot locate {0}{1}.material for model {2}{3}.{4}", new Object[]{folderName, meshName, folderName, meshName, ext});
                }
                
            }

            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(this);
            xr.setErrorHandler(this);
            InputStreamReader r = new InputStreamReader(info.openStream());
            xr.parse(new InputSource(r));
            r.close();

            return compileModel();
        }catch (SAXException ex){
            IOException ioEx = new IOException("Error while parsing Ogre3D mesh.xml");
            ioEx.initCause(ex);
            throw ioEx;
        }

    }

}
