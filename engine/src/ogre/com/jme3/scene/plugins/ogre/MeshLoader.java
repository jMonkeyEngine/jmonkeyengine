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

import com.jme3.animation.Animation;
import com.jme3.scene.plugins.ogre.matext.OgreMaterialKey;
import com.jme3.animation.AnimControl;
import com.jme3.animation.BoneAnimation;
import com.jme3.animation.SkeletonControl;
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
import com.jme3.scene.UserData;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.system.JmeSystem;
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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

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
    private int vertCount;
    private boolean usesSharedVerts;
    private boolean usesBigIndices;
    // Global data
    private Mesh sharedMesh;
    private int meshIndex = 0;
    private int texCoordIndex = 0;
    private String ignoreUntilEnd = null;
    private List<Geometry> geoms = new ArrayList<Geometry>();
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

        usesSharedVerts = false;
        vertCount = 0;
        meshIndex = 0;
        texCoordIndex = 0;
        ignoreUntilEnd = null;

        animData = null;

        indicesData = null;
        weightsFloatData = null;
    }

    @Override
    public void endDocument() {
    }

    private void pushFace(String v1, String v2, String v3) throws SAXException {
        int i1 = parseInt(v1);

        // TODO: fan/strip support
        int i2 = parseInt(v2);
        int i3 = parseInt(v3);
        if (ib != null) {
            ib.put(i1).put(i2).put(i3);
        } else {
            sb.put((short) i1).put((short) i2).put((short) i3);
        }
    }

    private boolean isUsingSharedVerts(Geometry geom) {
        return geom.getUserData(UserData.JME_SHAREDMESH) != null;
    }

    private void startFaces(String count) throws SAXException {
        int numFaces = parseInt(count);
        int numIndices;

        if (mesh.getMode() == Mesh.Mode.Triangles) {
            numIndices = numFaces * 3;
        } else {
            throw new SAXException("Triangle strip or fan not supported!");
        }

        vb = new VertexBuffer(VertexBuffer.Type.Index);
        if (!usesBigIndices) {
            sb = BufferUtils.createShortBuffer(numIndices);
            ib = null;
            vb.setupData(Usage.Static, 3, Format.UnsignedShort, sb);
        } else {
            ib = BufferUtils.createIntBuffer(numIndices);
            sb = null;
            vb.setupData(Usage.Static, 3, Format.UnsignedInt, ib);
        }
        mesh.setBuffer(vb);
    }

    private void applyMaterial(Geometry geom, String matName) {
        Material mat = null;
        if (matName.endsWith(".j3m")) {
            // load as native jme3 material instance
            mat = assetManager.loadMaterial(matName);
        } else {
            if (materialList != null) {
                mat = materialList.get(matName);
            }
            if (mat == null) {
                logger.log(Level.WARNING, "Material {0} not found. Applying default material", matName);
                mat = (Material) assetManager.loadMaterial("Common/Materials/RedColor.j3m");
            }
        }

        if (mat == null) {
            throw new RuntimeException("Cannot locate material named " + matName);
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
        } else if (opType.equals("triangle_strip")) {
            mesh.setMode(Mesh.Mode.TriangleStrip);
        } else if (opType.equals("triangle_fan")) {
            mesh.setMode(Mesh.Mode.TriangleFan);
        }

        usesBigIndices = parseBool(use32bitIndices, false);
        usesSharedVerts = parseBool(usesharedvertices, false);
        if (usesSharedVerts) {
            // import vertexbuffers from shared geom
            IntMap<VertexBuffer> sharedBufs = sharedMesh.getBuffers();
            for (Entry<VertexBuffer> entry : sharedBufs) {
                mesh.setBuffer(entry.getValue());
            }
        }

        if (meshName == null) {
            geom = new Geometry("OgreSubmesh-" + (++meshIndex), mesh);
        } else {
            geom = new Geometry(meshName + "-geom-" + (++meshIndex), mesh);
        }

        if (usesSharedVerts) {
            // this mesh is shared!
            geom.setUserData(UserData.JME_SHAREDMESH, sharedMesh);
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
        if (mesh != sharedMesh && isUsingSharedVerts(geom)) {
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
        String[] vals = value.split(" ");
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
        int faceCount = Integer.parseInt(numfaces);

        vb = new VertexBuffer(VertexBuffer.Type.Index);
        sb = BufferUtils.createShortBuffer(faceCount * 3);
        ib = null;
        vb.setupData(Usage.Static, 3, Format.UnsignedShort, sb);

        List<VertexBuffer> levels = lodLevels.get(index);
        if (levels == null) {
            levels = new ArrayList<VertexBuffer>();
            Mesh submesh = geoms.get(index).getMesh();
            levels.add(submesh.getBuffer(Type.Index));
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
    }

    private void startSkeleton(String name) {
        animData = (AnimData) assetManager.loadAsset(folderName + name + ".xml");
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
            IntMap<VertexBuffer> bufs = mesh.getBuffers();
            for (Entry<VertexBuffer> entry : bufs) {
                Buffer data = entry.getValue().getData();
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
            if (sharedMesh != null && isUsingSharedVerts(g)) {
                m.setBound(sharedMesh.getBound().clone());
            }
            model.attachChild(geoms.get(i));
        }

        // Do not attach shared geometry to the node!

        if (animData != null) {
            // This model uses animation

            // generate bind pose for mesh
            // ONLY if not using shared geometry
            // This includes the shared geoemtry itself actually
            if (sharedMesh != null) {
                sharedMesh.generateBindPose(!HARDWARE_SKINNING);
            }

            for (int i = 0; i < geoms.size(); i++) {
                Geometry g = geoms.get(i);
                Mesh m = geoms.get(i).getMesh();
                boolean useShared = isUsingSharedVerts(g);


                if (!useShared) {
                    // create bind pose
                    m.generateBindPose(!HARDWARE_SKINNING);
//                } else {
                    // Inherit animation data from shared mesh
//                    VertexBuffer bindPos = sharedMesh.getBuffer(Type.BindPosePosition);
//                    VertexBuffer bindNorm = sharedMesh.getBuffer(Type.BindPoseNormal);
//                    VertexBuffer boneIndex = sharedMesh.getBuffer(Type.BoneIndex);
//                    VertexBuffer boneWeight = sharedMesh.getBuffer(Type.BoneWeight);
//
//                    if (bindPos != null) {
//                        m.setBuffer(bindPos);
//                    }
//
//                    if (bindNorm != null) {
//                        m.setBuffer(bindNorm);
//                    }
//
//                    if (boneIndex != null) {
//                        m.setBuffer(boneIndex);
//                    }
//
//                    if (boneWeight != null) {
//                        m.setBuffer(boneWeight);
//                    }
                }
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
            AssetKey key = info.getKey();
            meshName = key.getName();
            folderName = key.getFolder();
            String ext = key.getExtension();
            meshName = meshName.substring(0, meshName.length() - ext.length() - 1);
            if (folderName != null && folderName.length() > 0) {
                meshName = meshName.substring(folderName.length());
            }
            assetManager = info.getManager();

            OgreMeshKey meshKey = null;
            if (key instanceof OgreMeshKey) {
                meshKey = (OgreMeshKey) key;
                materialList = meshKey.getMaterialList();
                String materialName = meshKey.getMaterialName();
                if (materialList == null && materialName != null) {
                    materialList = (MaterialList) assetManager.loadAsset(new OgreMaterialKey(folderName + materialName + ".material"));
                }
            } else {
                try {
                    materialList = (MaterialList) assetManager.loadAsset(new OgreMaterialKey(folderName + meshName + ".material"));
                } catch (AssetNotFoundException e) {
                    logger.log(Level.WARNING, "Cannot locate {0}{1}.material for model {2}{3}.{4}", new Object[]{folderName, meshName, folderName, meshName, ext});
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
            InputStreamReader r = new InputStreamReader(info.openStream());
            xr.parse(new InputSource(r));
            r.close();

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
