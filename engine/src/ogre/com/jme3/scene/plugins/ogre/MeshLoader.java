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
import com.jme3.animation.Animation;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.*;
import com.jme3.material.Material;
import com.jme3.material.MaterialList;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.*;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.plugins.ogre.matext.OgreMaterialKey;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import com.jme3.util.PlaceholderAssets;
import static com.jme3.util.xml.SAXUtil.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Loads Ogre3D mesh.xml files.
 */
public class MeshLoader extends DefaultHandler implements AssetLoader {

    private static final Logger logger = Logger.getLogger(MeshLoader.class.getName());
    public static boolean AUTO_INTERLEAVE = true;
    public static boolean HARDWARE_SKINNING = false;
    private static final Type[] TEXCOORD_TYPES =
            new Type[]{
        Type.TexCoord,
        Type.TexCoord2,
        Type.TexCoord3,
        Type.TexCoord4,
        Type.TexCoord5,
        Type.TexCoord6,
        Type.TexCoord7,
        Type.TexCoord8,};
    private AssetKey key;
    private String meshName;
    private String folderName;
    private AssetManager assetManager;
    private MaterialList materialList;
    // Data per submesh/sharedgeom
    private ShortBuffer sb;
    private IntBuffer ib;
    private FloatBuffer fb;
    private VertexBuffer vb;
    private Mesh mesh;
    private Geometry geom;
    private ByteBuffer indicesData;
    private FloatBuffer weightsFloatData;
    private boolean actuallyHasWeights = false;
    private int vertCount;
    private boolean usesSharedVerts;
    private boolean usesBigIndices;
    // Global data
    private Mesh sharedMesh;
    private int meshIndex = 0;
    private int texCoordIndex = 0;
    private String ignoreUntilEnd = null;
    private List<Geometry> geoms = new ArrayList<Geometry>();
    private ArrayList<Boolean> usesSharedMesh = new ArrayList<Boolean>();
    private IntMap<List<VertexBuffer>> lodLevels = new IntMap<List<VertexBuffer>>();
    private AnimData animData;

    public MeshLoader() {
        super();
    }

    @Override
    public void startDocument() {
        geoms.clear();
        lodLevels.clear();

        sb = null;
        ib = null;
        fb = null;
        vb = null;
        mesh = null;
        geom = null;
        sharedMesh = null;

        usesSharedMesh.clear();
        usesSharedVerts = false;
        vertCount = 0;
        meshIndex = 0;
        texCoordIndex = 0;
        ignoreUntilEnd = null;

        animData = null;

        actuallyHasWeights = false;
        indicesData = null;
        weightsFloatData = null;
    }

    @Override
    public void endDocument() {
    }

    private void pushIndex(int index){
        if (ib != null){
            ib.put(index);
        }else{
            sb.put((short)index);
        }
    }
    
    private void pushFace(String v1, String v2, String v3) throws SAXException {
        // TODO: fan/strip support
        switch (mesh.getMode()){
            case Triangles:
                pushIndex(parseInt(v1));
                pushIndex(parseInt(v2));
                pushIndex(parseInt(v3));
                break;
            case Lines:
                pushIndex(parseInt(v1));
                pushIndex(parseInt(v2));
                break;
            case Points:
                pushIndex(parseInt(v1));
                break;
        }
    }

//    private boolean isUsingSharedVerts(Geometry geom) {
        // Old code for buffer sharer
        //return geom.getUserData(UserData.JME_SHAREDMESH) != null;
//    }

    private void startFaces(String count) throws SAXException {
        int numFaces = parseInt(count);
        int indicesPerFace = 0;

        switch (mesh.getMode()){
            case Triangles:
                indicesPerFace = 3;
                break;
            case Lines:
                indicesPerFace = 2;
                break;
            case Points:
                indicesPerFace = 1;
                break;
            default:
                throw new SAXException("Strips or fans not supported!");
        }

        int numIndices = indicesPerFace * numFaces;
        
        vb = new VertexBuffer(VertexBuffer.Type.Index);
        if (!usesBigIndices) {
            sb = BufferUtils.createShortBuffer(numIndices);
            ib = null;
            vb.setupData(Usage.Static, indicesPerFace, Format.UnsignedShort, sb);
        } else {
            ib = BufferUtils.createIntBuffer(numIndices);
            sb = null;
            vb.setupData(Usage.Static, indicesPerFace, Format.UnsignedInt, ib);
        }
        mesh.setBuffer(vb);
    }

    private void applyMaterial(Geometry geom, String matName) {
        Material mat = null;
        if (matName.endsWith(".j3m")) {
            // load as native jme3 material instance
            try {
                mat = assetManager.loadMaterial(matName);
            } catch (AssetNotFoundException ex){
                // Warning will be raised (see below)
                if (!ex.getMessage().equals(matName)){
                    throw ex;
                }
            }
        } else {
            if (materialList != null) {
                mat = materialList.get(matName);
            }
        }
        
        if (mat == null) {
            logger.log(Level.WARNING, "Cannot locate {0} for model {1}", new Object[]{matName, key});
            mat = PlaceholderAssets.getPlaceholderMaterial(assetManager);
        }

        if (mat.isTransparent()) {
            geom.setQueueBucket(Bucket.Transparent);
        }

        geom.setMaterial(mat);
    }

    private void startSubMesh(String matName, String usesharedvertices, String use32bitIndices, String opType) throws SAXException {
        mesh = new Mesh();
        if (opType == null || opType.equals("triangle_list")) {
            mesh.setMode(Mesh.Mode.Triangles);
        //} else if (opType.equals("triangle_strip")) {
        //    mesh.setMode(Mesh.Mode.TriangleStrip);
        //} else if (opType.equals("triangle_fan")) {
        //    mesh.setMode(Mesh.Mode.TriangleFan);
        } else if (opType.equals("line_list")) {
            mesh.setMode(Mesh.Mode.Lines);
        } else {
            throw new SAXException("Unsupported operation type: " + opType);
        }

        usesBigIndices = parseBool(use32bitIndices, false);
        usesSharedVerts = parseBool(usesharedvertices, false);
        if (usesSharedVerts) {
            usesSharedMesh.add(true);
            
            // Old code for buffer sharer
            // import vertexbuffers from shared geom
//            IntMap<VertexBuffer> sharedBufs = sharedMesh.getBuffers();
//            for (Entry<VertexBuffer> entry : sharedBufs) {
//                mesh.setBuffer(entry.getValue());
//            }
        }else{
            usesSharedMesh.add(false);
        }

        if (meshName == null) {
            geom = new Geometry("OgreSubmesh-" + (++meshIndex), mesh);
        } else {
            geom = new Geometry(meshName + "-geom-" + (++meshIndex), mesh);
        }

        if (usesSharedVerts) {
            // Old code for buffer sharer
            // this mesh is shared!
            //geom.setUserData(UserData.JME_SHAREDMESH, sharedMesh);
        }

        applyMaterial(geom, matName);
        geoms.add(geom);
    }

    private void startSharedGeom(String vertexcount) throws SAXException {
        sharedMesh = new Mesh();
        vertCount = parseInt(vertexcount);
        usesSharedVerts = false;

        geom = null;
        mesh = sharedMesh;
    }

    private void startGeometry(String vertexcount) throws SAXException {
        vertCount = parseInt(vertexcount);
    }

    /**
     * Normalizes weights if needed and finds largest amount of weights used
     * for all vertices in the buffer.
     */
    private void endBoneAssigns() {
//        if (mesh != sharedMesh && isUsingSharedVerts(geom)) {
//            return;
//        }

        if (!actuallyHasWeights){
            // No weights were actually written (the tag didn't have any entries)
            // remove those buffers
            mesh.clearBuffer(Type.BoneIndex);
            mesh.clearBuffer(Type.BoneWeight);
            
            weightsFloatData = null;
            indicesData = null;
            
            return;
        }
        
        //int vertCount = mesh.getVertexCount();
        int maxWeightsPerVert = 0;
        weightsFloatData.rewind();
        for (int v = 0; v < vertCount; v++) {
            float w0 = weightsFloatData.get(),
                    w1 = weightsFloatData.get(),
                    w2 = weightsFloatData.get(),
                    w3 = weightsFloatData.get();

            if (w3 != 0) {
                maxWeightsPerVert = Math.max(maxWeightsPerVert, 4);
            } else if (w2 != 0) {
                maxWeightsPerVert = Math.max(maxWeightsPerVert, 3);
            } else if (w1 != 0) {
                maxWeightsPerVert = Math.max(maxWeightsPerVert, 2);
            } else if (w0 != 0) {
                maxWeightsPerVert = Math.max(maxWeightsPerVert, 1);
            }

            float sum = w0 + w1 + w2 + w3;
            if (sum != 1f) {
                weightsFloatData.position(weightsFloatData.position() - 4);
                // compute new vals based on sum
                float sumToB = 1f / sum;
                weightsFloatData.put(w0 * sumToB);
                weightsFloatData.put(w1 * sumToB);
                weightsFloatData.put(w2 * sumToB);
                weightsFloatData.put(w3 * sumToB);
            }
        }
        weightsFloatData.rewind();

        actuallyHasWeights = false;
        weightsFloatData = null;
        indicesData = null;

        mesh.setMaxNumWeights(maxWeightsPerVert);
    }

    private void startBoneAssigns() {
        if (mesh != sharedMesh && usesSharedVerts) {
            // will use bone assignments from shared mesh (?)
            return;
        }

        // current mesh will have bone assigns
        //int vertCount = mesh.getVertexCount();
        // each vertex has
        // - 4 bone weights
        // - 4 bone indices
        if (HARDWARE_SKINNING) {
            weightsFloatData = BufferUtils.createFloatBuffer(vertCount * 4);
            indicesData = BufferUtils.createByteBuffer(vertCount * 4);
        } else {
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

    private void startVertexBuffer(Attributes attribs) throws SAXException {
        if (parseBool(attribs.getValue("positions"), false)) {
            vb = new VertexBuffer(Type.Position);
            fb = BufferUtils.createFloatBuffer(vertCount * 3);
            vb.setupData(Usage.Static, 3, Format.Float, fb);
            mesh.setBuffer(vb);
        }
        if (parseBool(attribs.getValue("normals"), false)) {
            vb = new VertexBuffer(Type.Normal);
            fb = BufferUtils.createFloatBuffer(vertCount * 3);
            vb.setupData(Usage.Static, 3, Format.Float, fb);
            mesh.setBuffer(vb);
        }
        if (parseBool(attribs.getValue("colours_diffuse"), false)) {
            vb = new VertexBuffer(Type.Color);
            fb = BufferUtils.createFloatBuffer(vertCount * 4);
            vb.setupData(Usage.Static, 4, Format.Float, fb);
            mesh.setBuffer(vb);
        }
        if (parseBool(attribs.getValue("tangents"), false)) {
            int dimensions = parseInt(attribs.getValue("tangent_dimensions"), 3);
            vb = new VertexBuffer(Type.Tangent);
            fb = BufferUtils.createFloatBuffer(vertCount * dimensions);
            vb.setupData(Usage.Static, dimensions, Format.Float, fb);
            mesh.setBuffer(vb);
        }
        if (parseBool(attribs.getValue("binormals"), false)) {
            vb = new VertexBuffer(Type.Binormal);
            fb = BufferUtils.createFloatBuffer(vertCount * 3);
            vb.setupData(Usage.Static, 3, Format.Float, fb);
            mesh.setBuffer(vb);
        }

        int texCoords = parseInt(attribs.getValue("texture_coords"), 0);
        for (int i = 0; i < texCoords; i++) {
            int dims = parseInt(attribs.getValue("texture_coord_dimensions_" + i), 2);
            if (dims < 1 || dims > 4) {
                throw new SAXException("Texture coord dimensions must be 1 <= dims <= 4");
            }

            if (i <= 7) {
                vb = new VertexBuffer(TEXCOORD_TYPES[i]);
            } else {
                // more than 8 texture coordinates are not supported by ogre.
                throw new SAXException("More than 8 texture coordinates not supported");
            }
            fb = BufferUtils.createFloatBuffer(vertCount * dims);
            vb.setupData(Usage.Static, dims, Format.Float, fb);
            mesh.setBuffer(vb);
        }
    }

    private void startVertex() {
        texCoordIndex = 0;
    }

    private void pushAttrib(Type type, Attributes attribs) throws SAXException {
        try {
            FloatBuffer buf = (FloatBuffer) mesh.getBuffer(type).getData();
            buf.put(parseFloat(attribs.getValue("x"))).put(parseFloat(attribs.getValue("y"))).put(parseFloat(attribs.getValue("z")));
        } catch (Exception ex) {
            throw new SAXException("Failed to push attrib", ex);
        }
    }

    private void pushTangent(Attributes attribs) throws SAXException {
        try {
            VertexBuffer tangentBuf = mesh.getBuffer(Type.Tangent);
            FloatBuffer buf = (FloatBuffer) tangentBuf.getData();
            buf.put(parseFloat(attribs.getValue("x"))).put(parseFloat(attribs.getValue("y"))).put(parseFloat(attribs.getValue("z")));
            if (tangentBuf.getNumComponents() == 4) {
                buf.put(parseFloat(attribs.getValue("w")));
            }
        } catch (Exception ex) {
            throw new SAXException("Failed to push attrib", ex);
        }
    }

    private void pushTexCoord(Attributes attribs) throws SAXException {
        if (texCoordIndex >= 8) {
            return; // More than 8 not supported by ogre.
        }
        Type type = TEXCOORD_TYPES[texCoordIndex];

        VertexBuffer tcvb = mesh.getBuffer(type);
        FloatBuffer buf = (FloatBuffer) tcvb.getData();

        buf.put(parseFloat(attribs.getValue("u")));
        if (tcvb.getNumComponents() >= 2) {
            buf.put(parseFloat(attribs.getValue("v")));
            if (tcvb.getNumComponents() >= 3) {
                buf.put(parseFloat(attribs.getValue("w")));
                if (tcvb.getNumComponents() == 4) {
                    buf.put(parseFloat(attribs.getValue("x")));
                }
            }
        }

        texCoordIndex++;
    }

    private void pushColor(Attributes attribs) throws SAXException {
        FloatBuffer buf = (FloatBuffer) mesh.getBuffer(Type.Color).getData();
        String value = parseString(attribs.getValue("value"));
        String[] vals = value.split("\\s");
        if (vals.length != 3 && vals.length != 4) {
            throw new SAXException("Color value must contain 3 or 4 components");
        }

        ColorRGBA color = new ColorRGBA();
        color.r = parseFloat(vals[0]);
        color.g = parseFloat(vals[1]);
        color.b = parseFloat(vals[2]);
        if (vals.length == 3) {
            color.a = 1f;
        } else {
            color.a = parseFloat(vals[3]);
        }

        buf.put(color.r).put(color.g).put(color.b).put(color.a);
    }

    private void startLodFaceList(String submeshindex, String numfaces) {
        int index = Integer.parseInt(submeshindex);
        mesh = geoms.get(index).getMesh();
        int faceCount = Integer.parseInt(numfaces);
        
        VertexBuffer originalIndexBuffer = mesh.getBuffer(Type.Index);
        vb = new VertexBuffer(VertexBuffer.Type.Index);
        if (originalIndexBuffer.getFormat() == Format.UnsignedInt){
            // LOD buffer should also be integer
            ib = BufferUtils.createIntBuffer(faceCount * 3);
            sb = null;
            vb.setupData(Usage.Static, 3, Format.UnsignedInt, ib);
        }else{
            sb = BufferUtils.createShortBuffer(faceCount * 3);
            ib = null;
            vb.setupData(Usage.Static, 3, Format.UnsignedShort, sb);
        }

        List<VertexBuffer> levels = lodLevels.get(index);
        if (levels == null) {
            // Create the LOD levels list
            levels = new ArrayList<VertexBuffer>();
            
            // Add the first LOD level (always the original index buffer)
            levels.add(originalIndexBuffer);
            lodLevels.put(index, levels);
        }
        levels.add(vb);
    }

    private void startLevelOfDetail(String numlevels) {
//        numLevels = Integer.parseInt(numlevels);
    }

    private void endLevelOfDetail() {
        // set the lod data for each mesh
        for (Entry<List<VertexBuffer>> entry : lodLevels) {
            Mesh m = geoms.get(entry.getKey()).getMesh();
            List<VertexBuffer> levels = entry.getValue();
            VertexBuffer[] levelArray = new VertexBuffer[levels.size()];
            levels.toArray(levelArray);
            m.setLodLevels(levelArray);
        }
    }

    private void startLodGenerated(String depthsqr) {
    }

    private void pushBoneAssign(String vertIndex, String boneIndex, String weight) throws SAXException {
        int vert = parseInt(vertIndex);
        float w = parseFloat(weight);
        byte bone = (byte) parseInt(boneIndex);

        assert bone >= 0;
        assert vert >= 0 && vert < mesh.getVertexCount();

        int i;
        float v = 0;
        // see which weights are unused for a given bone
        for (i = vert * 4; i < vert * 4 + 4; i++) {
            v = weightsFloatData.get(i);
            if (v == 0) {
                break;
            }
        }
        if (v != 0) {
            logger.log(Level.WARNING, "Vertex {0} has more than 4 weights per vertex! Ignoring..", vert);
            return;
        }

        weightsFloatData.put(i, w);
        indicesData.put(i, bone);
        actuallyHasWeights = true;
    }

    private void startSkeleton(String name) {
        AssetKey assetKey = new AssetKey(folderName + name + ".xml");
        try {
            animData = (AnimData) assetManager.loadAsset(assetKey);
        } catch (AssetNotFoundException ex){
            logger.log(Level.WARNING, "Cannot locate {0} for model {1}", new Object[]{assetKey, key});
            animData = null;
        }
    }

    private void startSubmeshName(String indexStr, String nameStr) {
        int index = Integer.parseInt(indexStr);
        geoms.get(index).setName(nameStr);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attribs) throws SAXException {
        if (ignoreUntilEnd != null) {
            return;
        }

        if (qName.equals("texcoord")) {
            pushTexCoord(attribs);
        } else if (qName.equals("vertexboneassignment")) {
            pushBoneAssign(attribs.getValue("vertexindex"),
                    attribs.getValue("boneindex"),
                    attribs.getValue("weight"));
        } else if (qName.equals("face")) {
            pushFace(attribs.getValue("v1"),
                    attribs.getValue("v2"),
                    attribs.getValue("v3"));
        } else if (qName.equals("position")) {
            pushAttrib(Type.Position, attribs);
        } else if (qName.equals("normal")) {
            pushAttrib(Type.Normal, attribs);
        } else if (qName.equals("tangent")) {
            pushTangent(attribs);
        } else if (qName.equals("binormal")) {
            pushAttrib(Type.Binormal, attribs);
        } else if (qName.equals("colour_diffuse")) {
            pushColor(attribs);
        } else if (qName.equals("vertex")) {
            startVertex();
        } else if (qName.equals("faces")) {
            startFaces(attribs.getValue("count"));
        } else if (qName.equals("geometry")) {
            String count = attribs.getValue("vertexcount");
            if (count == null) {
                count = attribs.getValue("count");
            }
            startGeometry(count);
        } else if (qName.equals("vertexbuffer")) {
            startVertexBuffer(attribs);
        } else if (qName.equals("lodfacelist")) {
            startLodFaceList(attribs.getValue("submeshindex"),
                    attribs.getValue("numfaces"));
        } else if (qName.equals("lodgenerated")) {
            startLodGenerated(attribs.getValue("fromdepthsquared"));
        } else if (qName.equals("levelofdetail")) {
            startLevelOfDetail(attribs.getValue("numlevels"));
        } else if (qName.equals("boneassignments")) {
            startBoneAssigns();
        } else if (qName.equals("submesh")) {
            startSubMesh(attribs.getValue("material"),
                    attribs.getValue("usesharedvertices"),
                    attribs.getValue("use32bitindexes"),
                    attribs.getValue("operationtype"));
        } else if (qName.equals("sharedgeometry")) {
            String count = attribs.getValue("vertexcount");
            if (count == null) {
                count = attribs.getValue("count");
            }

            if (count != null && !count.equals("0")) {
                startSharedGeom(count);
            }
        } else if (qName.equals("submeshes")) {
            // ok
        } else if (qName.equals("skeletonlink")) {
            startSkeleton(attribs.getValue("name"));
        } else if (qName.equals("submeshnames")) {
            // ok
        } else if (qName.equals("submeshname")) {
            startSubmeshName(attribs.getValue("index"), attribs.getValue("name"));
        } else if (qName.equals("mesh")) {
            // ok
        } else {
            logger.log(Level.WARNING, "Unknown tag: {0}. Ignoring.", qName);
            ignoreUntilEnd = qName;
        }
    }

    @Override
    public void endElement(String uri, String name, String qName) {
        if (ignoreUntilEnd != null) {
            if (ignoreUntilEnd.equals(qName)) {
                ignoreUntilEnd = null;
            }
            return;
        }

        if (qName.equals("submesh")) {
            usesBigIndices = false;
            geom = null;
            mesh = null;
        } else if (qName.equals("submeshes")) {
            // IMPORTANT: restore sharedmesh, for use with shared boneweights
            geom = null;
            mesh = sharedMesh;
            usesSharedVerts = false;
        } else if (qName.equals("faces")) {
            if (ib != null) {
                ib.flip();
            } else {
                sb.flip();
            }

            vb = null;
            ib = null;
            sb = null;
        } else if (qName.equals("vertexbuffer")) {
            fb = null;
            vb = null;
        } else if (qName.equals("geometry")
                || qName.equals("sharedgeometry")) {
            // finish writing to buffers
            for (VertexBuffer buf : mesh.getBufferList().getArray()) {
                Buffer data = buf.getData();
                if (data.position() != 0) {
                    data.flip();
                }
            }
            mesh.updateBound();
            mesh.setStatic();

            if (qName.equals("sharedgeometry")) {
                geom = null;
                mesh = null;
            }
        } else if (qName.equals("lodfacelist")) {
            sb.flip();
            vb = null;
            sb = null;
        } else if (qName.equals("levelofdetail")) {
            endLevelOfDetail();
        } else if (qName.equals("boneassignments")) {
            endBoneAssigns();
        }
    }

    @Override
    public void characters(char ch[], int start, int length) {
    }

    private Node compileModel() {
        Node model = new Node(meshName + "-ogremesh");

        for (int i = 0; i < geoms.size(); i++) {
            Geometry g = geoms.get(i);
            Mesh m = g.getMesh();
            
            // New code for buffer extract
            if (sharedMesh != null && usesSharedMesh.get(i)) {
                m.extractVertexData(sharedMesh);
            }
            
            // Old code for buffer sharer
            //if (sharedMesh != null && isUsingSharedVerts(g)) {
            //    m.setBound(sharedMesh.getBound().clone());
            //}
            model.attachChild(geoms.get(i));
        }

        // Do not attach shared geometry to the node!

        if (animData != null) {
            // This model uses animation

            // Old code for buffer sharer
            // generate bind pose for mesh
            // ONLY if not using shared geometry
            // This includes the shared geoemtry itself actually
            //if (sharedMesh != null) {
            //    sharedMesh.generateBindPose(!HARDWARE_SKINNING);
            //}

            for (int i = 0; i < geoms.size(); i++) {
                Geometry g = geoms.get(i);
                Mesh m = geoms.get(i).getMesh();
                
                m.generateBindPose(!HARDWARE_SKINNING);
                
                // Old code for buffer sharer
                //boolean useShared = isUsingSharedVerts(g);
                //if (!useShared) {
                    // create bind pose
                    //m.generateBindPose(!HARDWARE_SKINNING);
                //}
            }

            // Put the animations in the AnimControl
            HashMap<String, Animation> anims = new HashMap<String, Animation>();
            ArrayList<Animation> animList = animData.anims;
            for (int i = 0; i < animList.size(); i++) {
                Animation anim = animList.get(i);
                anims.put(anim.getName(), anim);
            }

            AnimControl ctrl = new AnimControl(animData.skeleton);
            ctrl.setAnimations(anims);
            model.addControl(ctrl);

            // Put the skeleton in the skeleton control
            SkeletonControl skeletonControl = new SkeletonControl(animData.skeleton);

            // This will acquire the targets from the node
            model.addControl(skeletonControl);
        }

        return model;
    }

    public Object load(AssetInfo info) throws IOException {
        try {
            key = info.getKey();
            meshName = key.getName();
            folderName = key.getFolder();
            String ext = key.getExtension();
            meshName = meshName.substring(0, meshName.length() - ext.length() - 1);
            if (folderName != null && folderName.length() > 0) {
                meshName = meshName.substring(folderName.length());
            }
            assetManager = info.getManager();

            if (key instanceof OgreMeshKey) {
                // OgreMeshKey is being used, try getting the material list
                // from it
                OgreMeshKey meshKey = (OgreMeshKey) key;
                materialList = meshKey.getMaterialList();
                String materialName = meshKey.getMaterialName();
                
                // Material list not set but material name is available
                if (materialList == null && materialName != null) {
                    OgreMaterialKey materialKey = new OgreMaterialKey(folderName + materialName + ".material");
                    try {
                        materialList = (MaterialList) assetManager.loadAsset(materialKey);
                    } catch (AssetNotFoundException e) {
                        logger.log(Level.WARNING, "Cannot locate {0} for model {1}", new Object[]{materialKey, key});
                    }
                }
            }else{
                // Make sure to reset it to null so that previous state
                // doesn't leak onto this one
                materialList = null;
            }

            // If for some reason material list could not be found through
            // OgreMeshKey, or if regular ModelKey specified, load using 
            // default method.
            if (materialList == null){
                OgreMaterialKey materialKey = new OgreMaterialKey(folderName + meshName + ".material");
                try {
                    materialList = (MaterialList) assetManager.loadAsset(materialKey);
                } catch (AssetNotFoundException e) {
                    logger.log(Level.WARNING, "Cannot locate {0} for model {1}", new Object[]{ materialKey, key });
                }
            }
            
            // Added by larynx 25.06.2011
            // Android needs the namespace aware flag set to true                 
            // Kirill 30.06.2011
            // Now, hack is applied for both desktop and android to avoid
            // checking with JmeSystem.
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            
            XMLReader xr = factory.newSAXParser().getXMLReader();
            xr.setContentHandler(this);
            xr.setErrorHandler(this);
            
            InputStreamReader r = null;
            try {
                r = new InputStreamReader(info.openStream());
                xr.parse(new InputSource(r));
            } finally {
                if (r != null){
                    r.close();
                }
            }
            
            return compileModel();
        } catch (SAXException ex) {
            IOException ioEx = new IOException("Error while parsing Ogre3D mesh.xml");
            ioEx.initCause(ex);
            throw ioEx;
        } catch (ParserConfigurationException ex) {
            IOException ioEx = new IOException("Error while parsing Ogre3D mesh.xml");
            ioEx.initCause(ex);
            throw ioEx;
        }

    }
}
