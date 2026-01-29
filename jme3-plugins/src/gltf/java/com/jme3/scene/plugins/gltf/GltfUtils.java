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
package com.jme3.scene.plugins.gltf;

import com.jme3.plugins.json.JsonArray;
import com.jme3.plugins.json.JsonElement;
import com.jme3.plugins.json.JsonObject;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoadException;
import com.jme3.math.*;
import com.jme3.plugins.json.Json;
import com.jme3.plugins.json.JsonParser;
import com.jme3.renderer.opengl.GL;
import com.jme3.scene.*;
import com.jme3.texture.Texture;
import com.jme3.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Nehon on 07/08/2017.
 */
public class GltfUtils {

    private static final Logger logger = Logger.getLogger(GltfUtils.class.getName());

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private GltfUtils() {
    }

    public static ByteBuffer asReadableByteBuffer(ByteBuffer bbf){
        return bbf.slice().order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Parse a json input stream and returns a {@link JsonObject}
     * @param stream the stream to parse
     * @return the JsonObject
     */
    public static JsonObject parse(InputStream stream) {
        JsonParser parser = Json.create();
        return parser.parse(stream);
    }

    public static Mesh.Mode getMeshMode(Integer mode) {
        if (mode == null) {
            return Mesh.Mode.Triangles;
        }
        //too bad, we could have returned the enum value from the ordinal
        //but LineLoop and LineStrip are inverted in the Mesh.Mode Enum declaration.
        switch (mode) {
            case 0:
                return Mesh.Mode.Points;
            case 1:
                return Mesh.Mode.Lines;
            case 2:
                return Mesh.Mode.LineLoop;
            case 3:
                return Mesh.Mode.LineStrip;
            case 4:
                return Mesh.Mode.Triangles;
            case 5:
                return Mesh.Mode.TriangleStrip;
            case 6:
                return Mesh.Mode.TriangleFan;
        }
        return Mesh.Mode.Triangles;
    }

    public static VertexBuffer.Format getVertexBufferFormat(int componentType) {
        switch (componentType) {
            case GL.GL_BYTE:
                return VertexBuffer.Format.Byte;
            case GL.GL_UNSIGNED_BYTE:
                return VertexBuffer.Format.UnsignedByte;
            case GL.GL_SHORT:
                return VertexBuffer.Format.Short;
            case GL.GL_UNSIGNED_SHORT:
                return VertexBuffer.Format.UnsignedShort;
            case GL.GL_UNSIGNED_INT:
                return VertexBuffer.Format.UnsignedInt;
            case GL.GL_FLOAT:
                return VertexBuffer.Format.Float;
            default:
                throw new AssetLoadException("Illegal component type: " + componentType);
        }
    }

    public static int getNumberOfComponents(String type) {
        switch (type) {
            case "SCALAR":
                return 1;
            case "VEC2":
                return 2;
            case "VEC3":
                return 3;
            case "VEC4":
                return 4;
            case "MAT2":
                return 4;
            case "MAT3":
                return 9;
            case "MAT4":
                return 16;
            default:
                throw new AssetLoadException("Illegal type: " + type);
        }
    }

    public static VertexBuffer.Type getVertexBufferType(String attribute) {
        switch (attribute) {
            case "POSITION":
                return VertexBuffer.Type.Position;
            case "NORMAL":
                return VertexBuffer.Type.Normal;
            case "TANGENT":
                return VertexBuffer.Type.Tangent;
            case "TEXCOORD_0":
                return VertexBuffer.Type.TexCoord;
            case "TEXCOORD_1":
                return VertexBuffer.Type.TexCoord2;
            case "TEXCOORD_2":
                return VertexBuffer.Type.TexCoord3;
            case "TEXCOORD_3":
                return VertexBuffer.Type.TexCoord4;
            case "TEXCOORD_4":
                return VertexBuffer.Type.TexCoord5;
            case "TEXCOORD_5":
                return VertexBuffer.Type.TexCoord6;
            case "TEXCOORD_6":
                return VertexBuffer.Type.TexCoord7;
            case "TEXCOORD_7":
                return VertexBuffer.Type.TexCoord8;
            case "COLOR_0":
                return VertexBuffer.Type.Color;
            case "JOINTS_0":
                return VertexBuffer.Type.BoneIndex;
            case "WEIGHTS_0":
                return VertexBuffer.Type.BoneWeight;
            default:
                logger.log(Level.WARNING, "Unsupported Vertex Buffer type " + attribute);
                return null;

        }
    }

    public static int getIndex(String name) {
        String num = name.substring(name.lastIndexOf("_") + 1);
        return Integer.parseInt(num);
    }

    public static Texture.MagFilter getMagFilter(Integer value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case 9728:
                return Texture.MagFilter.Nearest;
            case 9729:
                return Texture.MagFilter.Bilinear;
        }
        return null;
    }

    public static Texture.MinFilter getMinFilter(Integer value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case 9728:
                return Texture.MinFilter.NearestNoMipMaps;
            case 9729:
                return Texture.MinFilter.BilinearNoMipMaps;
            case 9984:
                return Texture.MinFilter.NearestNearestMipMap;
            case 9985:
                return Texture.MinFilter.BilinearNearestMipMap;
            case 9986:
                return Texture.MinFilter.NearestLinearMipMap;
            case 9987:
                return Texture.MinFilter.Trilinear;

        }
        return null;
    }

    public static Texture.WrapMode getWrapMode(Integer value) {
        if (value == null) {
            return Texture.WrapMode.Repeat;
        }
        switch (value) {
            case 33071:
                return Texture.WrapMode.EdgeClamp;
            case 33648:
                return Texture.WrapMode.MirroredRepeat;
            default:
                return Texture.WrapMode.Repeat;
        }
    }

    public static void padBuffer(Buffer buffer, int bufferSize) {
        buffer.clear();
        if (buffer instanceof IntBuffer) {
            IntBuffer ib = (IntBuffer) buffer;
            for (int i = 0; i < bufferSize; i++) {
                ib.put(0);
            }
        } else if (buffer instanceof FloatBuffer) {
            FloatBuffer fb = (FloatBuffer) buffer;
            for (int i = 0; i < bufferSize; i++) {
                fb.put(0);
            }
        } else if (buffer instanceof ShortBuffer) {
            ShortBuffer sb = (ShortBuffer) buffer;
            for (int i = 0; i < bufferSize; i++) {
                sb.put((short) 0);
            }
        } else if (buffer instanceof ByteBuffer) {
            ByteBuffer bb = (ByteBuffer) buffer;
            for (int i = 0; i < bufferSize; i++) {
                bb.put((byte) 0);
            }
        }
        buffer.rewind();
    }

    public static void padBuffer(float[] array, int bufferSize) {
        for (int i = 0; i < bufferSize; i++) {
            array[i] = 0;
        }
    }

    public static void padBuffer(short[] array, int bufferSize) {
        for (int i = 0; i < bufferSize; i++) {
            array[i] = 0;
        }
    }
    
    public static void padBuffer(Vector3f[] array, int bufferSize) {
        for (int i = 0; i < bufferSize; i++) {
            array[i] = new Vector3f();
        }
    }

    public static void padBuffer(Quaternion[] array, int bufferSize) {
        for (int i = 0; i < bufferSize; i++) {
            array[i] = new Quaternion();
        }      
    }

    public static void padBuffer(Matrix4f[] array, int bufferSize) {
        for (int i = 0; i < bufferSize; i++) {
            array[i] = new Matrix4f();
        }        
    }





    public static void populateBuffer(Object store, ByteBuffer source, int count, int byteOffset, int byteStride, int numComponents, VertexBuffer.Format format) throws IOException {
        source = asReadableByteBuffer(source);

        if (store instanceof Buffer) {
            Buffer buffer = (Buffer) store;
            buffer.clear();
            if (buffer instanceof ByteBuffer) {
                populateByteBuffer((ByteBuffer) buffer, source, count, byteOffset, byteStride, numComponents, format);
                return;
            }
            if (buffer instanceof ShortBuffer) {
                populateShortBuffer((ShortBuffer) buffer, source, count, byteOffset, byteStride, numComponents, format);
            } else if (buffer instanceof IntBuffer) {
                populateIntBuffer((IntBuffer) buffer, source, count, byteOffset, byteStride, numComponents, format);
            } else if (buffer instanceof FloatBuffer) {
                populateFloatBuffer((FloatBuffer) buffer, source, count, byteOffset, byteStride, numComponents, format);
            }
            buffer.rewind();
            return;
        }

        if (store instanceof byte[]) {
            populateByteArray((byte[]) store, source, count, byteOffset, byteStride, numComponents, format);
        } else if (store instanceof short[]) {
            populateShortArray((short[]) store, source, count, byteOffset, byteStride, numComponents, format);
        } else if (store instanceof float[]) {
            populateFloatArray((float[]) store, source, count, byteOffset, byteStride, numComponents, format);
        } else if (store instanceof Vector3f[]) {
            populateVector3fArray((Vector3f[]) store, source, count, byteOffset, byteStride, numComponents, format);
        } else if (store instanceof Quaternion[]) {
            populateQuaternionArray((Quaternion[]) store, source, count, byteOffset, byteStride, numComponents, format);
        } else if (store instanceof Matrix4f[]) {
            populateMatrix4fArray((Matrix4f[]) store, source, count, byteOffset, byteStride, numComponents, format);
        }
    }

    private static void skip(ByteBuffer buff, int n) {
        buff.position(Math.min(buff.position() + n, buff.limit()));
    }

    private static void populateByteBuffer(ByteBuffer buffer, ByteBuffer source, int count, int byteOffset, int byteStride, int numComponents, VertexBuffer.Format format) {
        int componentSize = format.getComponentSize();
        int index = byteOffset;
        int dataLength = componentSize * numComponents;
        int stride = Math.max(dataLength, byteStride);
        int end = count * stride + byteOffset;
        while (index < end) {
            for (int i = 0; i < numComponents; i++) {
                buffer.put(source.get(index + i));
            }
            index += stride;
        }
    }

    private static void populateShortBuffer(ShortBuffer buffer, ByteBuffer source, int count, int byteOffset, int byteStride, int numComponents, VertexBuffer.Format format) throws IOException {
        int componentSize = format.getComponentSize();
        int index = byteOffset;
        int dataLength = componentSize * numComponents;
        int stride = Math.max(dataLength, byteStride);
        int end = count * stride + byteOffset;        
        source.position(source.position() + byteOffset);
        while (index < end) {
            for (int i = 0; i < numComponents; i++) {
                buffer.put(source.getShort());
            }

            if (dataLength < stride) {
                skip(source, stride - dataLength);
            }
            index += stride;
        }
    }


    private static void populateIntBuffer(IntBuffer buffer, ByteBuffer source, int count, int byteOffset, int byteStride, int numComponents, VertexBuffer.Format format) throws IOException {
        int componentSize = format.getComponentSize();
        int index = byteOffset;
        int dataLength = componentSize * numComponents;
        int stride = Math.max(dataLength, byteStride);
        int end = count * stride + byteOffset;
        source.position(source.position() + byteOffset);
        while (index < end) {
            for (int i = 0; i < numComponents; i++) {
                buffer.put(source.getInt());
            }
            if (dataLength < stride) {
                skip(source, stride - dataLength);
            }
            index += stride;
        }
    }

    private static void populateFloatBuffer(FloatBuffer buffer, ByteBuffer source, int count, int byteOffset, int byteStride, int numComponents, VertexBuffer.Format format) throws IOException {
        int componentSize = format.getComponentSize();
        int index = byteOffset;
        int dataLength = componentSize * numComponents;
        int stride = Math.max(dataLength, byteStride);
        int end = count * stride + byteOffset;
        source.position(source.position() + byteOffset);
        while (index < end) {
            for (int i = 0; i < numComponents; i++) {
                buffer.put(readAsFloat(source, format));
            }
            if (dataLength < stride) {
                skip(source, stride - dataLength);
            }
            index += stride;
        }
    }

    public static float readAsFloat(ByteBuffer source, VertexBuffer.Format format) throws IOException {
        //We may have packed data so depending on the format, we need to read data differently and unpack it
        // Implementations must use following equations to get corresponding floating-point value f from a normalized integer c and vise-versa:
        // accessor.componentType    int-to-float                float-to-int
        // 5120 (BYTE)               f = max(c / 127.0, -1.0)    c = round(f * 127.0)
        // 5121 (UNSIGNED_BYTE)      f = c / 255.0               c = round(f * 255.0)
        // 5122 (SHORT)              f = max(c / 32767.0, -1.0)  c = round(f * 32767.0)
        // 5123 (UNSIGNED_SHORT)     f = c / 65535.0             c = round(f * 65535.0)
        int c;
        switch (format) {
            case Byte:
                c = source.get();
                return Math.max(c / 127f, -1f);
            case UnsignedByte:
                c = source.get() & 0xFF;
                return c / 255f;
            case Short:
                c = source.getShort();
                return Math.max(c / 32767f, -1f);
            case UnsignedShort:               
                c = source.get() & 0xff | (source.get() & 0xff) << 8;
                return c / 65535f;
            default:
                //we have a regular float
                return source.getFloat();
        }

    }

    private static void populateByteArray(byte[] array, ByteBuffer source, int count, int byteOffset, int byteStride, int numComponents, VertexBuffer.Format format) throws IOException {
        int componentSize = format.getComponentSize();
        int index = byteOffset;
        int dataLength = componentSize * numComponents;
        int stride = Math.max(dataLength, byteStride);
        int end = count * stride + byteOffset;
        source.position(source.position() + byteOffset);

        if (dataLength == stride) {
            read(source, array, end - index);

            return;
        }

        int arrayIndex = 0;
        byte[] buffer = new byte[numComponents];
        while (index < end) {
            read(source, buffer, numComponents);
            System.arraycopy(buffer, 0, array, arrayIndex, numComponents);
            arrayIndex += numComponents;
            if (dataLength < stride) {
                skip(source, stride - dataLength);
            }
            index += stride;
        }
    }

    private static void read(ByteBuffer source, byte[] buffer, int length) throws IOException {
        int n = 0;
        while (n < length) {
            int cnt = Math.min(source.remaining(), length - n);
            source.get(buffer, n, cnt);
            if (cnt < 0) {
                throw new AssetLoadException("Data ended prematurely");
            }
            n += cnt;
        }
    }

    private static void populateShortArray(short[] array, ByteBuffer source, int count, int byteOffset, int byteStride, int numComponents, VertexBuffer.Format format) throws IOException {
        int componentSize = format.getComponentSize();
        int index = byteOffset;
        int dataLength = componentSize * numComponents;
        int stride = Math.max(dataLength, byteStride);
        int end = count * stride + byteOffset;
        source.position(source.position() + byteOffset);
        int arrayIndex = 0;
        while (index < end) {
            for (int i = 0; i < numComponents; i++) {
                if (componentSize == 2) {
                    array[arrayIndex] = source.getShort();
                } else {
                    array[arrayIndex] = source.get();
                }
                arrayIndex++;
            }
            if (dataLength < stride) {
                skip(source, stride - dataLength);
            }
            index += stride;
        }
    }

    public static byte[] toByteArray(short[] shortArray) {
        byte[] bytes = new byte[shortArray.length];
        for (int i = 0; i < shortArray.length; i++) {
            bytes[i] = (byte) shortArray[i];
        }
        return bytes;
    }


    public static void handleSkinningBuffers(Mesh mesh, IntMap<GltfLoader.SkinBuffers> skinBuffers) {
        if (skinBuffers.size() > 0) {
            int length = skinBuffers.get(0).joints.length;
            short[] jointsArray = new short[length];
            float[] weightsArray = new float[length];
            List<GltfLoader.WeightData> weightData = new ArrayList<>();
            int componentSize = 1;

            for (int i = 0; i < weightsArray.length; i += 4) {
                weightData.clear();
                for (int j = 0; j < skinBuffers.size(); j++) {
                    GltfLoader.SkinBuffers buffs = skinBuffers.get(j);
                    for (int k = 0; k < 4; k++) {
                        weightData.add(new GltfLoader.WeightData(buffs.weights[i + k], buffs.joints[i + k], buffs.componentSize));
                    }

                }
                Collections.sort(weightData, new Comparator<GltfLoader.WeightData>() {
                    @Override
                    public int compare(GltfLoader.WeightData o1, GltfLoader.WeightData o2) {
                        if (o1.value > o2.value) {
                            return -1;
                        } else if (o1.value < o2.value) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });

                float sum = 0;
                for (int j = 0; j < 4; j++) {
                    GltfLoader.WeightData data = weightData.get(j);
                    jointsArray[i + j] = data.index;
                    weightsArray[i + j] = data.value;
                    sum += data.value;
                    if (data.value > 0 && (j + 1) > mesh.getMaxNumWeights()) {
                        mesh.setMaxNumWeights(j + 1);
                    }
                    if (data.componentSize > componentSize) {
                        componentSize = data.componentSize;
                    }
                }

                if (sum != 1f) {
                    // compute new values based on sum
                    float sumToB = sum == 0 ? 0 : 1f / sum;
                    weightsArray[i] *= sumToB;
                    weightsArray[i + 1] *= sumToB;
                    weightsArray[i + 2] *= sumToB;
                    weightsArray[i + 3] *= sumToB;
                }
            }
            setSkinBuffers(mesh, jointsArray, weightsArray, componentSize);
        }
    }


    public static void setSkinBuffers(Mesh mesh, short[] jointsArray, float[] weightsArray, int componentSize) {
        if (componentSize == 1) {
            mesh.setBuffer(VertexBuffer.Type.BoneIndex, 4, BufferUtils.createByteBuffer(toByteArray(jointsArray)));
        } else {
            mesh.setBuffer(VertexBuffer.Type.BoneIndex, 4, BufferUtils.createShortBuffer(jointsArray));
        }
        mesh.setBuffer(VertexBuffer.Type.BoneWeight, 4, BufferUtils.createFloatBuffer(weightsArray));
        mesh.getBuffer(VertexBuffer.Type.BoneIndex).setUsage(VertexBuffer.Usage.CpuOnly);
        mesh.getBuffer(VertexBuffer.Type.BoneWeight).setUsage(VertexBuffer.Usage.CpuOnly);
    }

    private static void populateFloatArray(float[] array, ByteBuffer source, int count, int byteOffset, int byteStride, int numComponents, VertexBuffer.Format format) throws IOException {
        int componentSize = format.getComponentSize();
        int index = byteOffset;
        int dataLength = componentSize * numComponents;
        int stride = Math.max(dataLength, byteStride);
        int end = count * stride + byteOffset;
        source.position(source.position() + byteOffset);
        int arrayIndex = 0;
        while (index < end) {
            for (int i = 0; i < numComponents; i++) {
                array[arrayIndex] = readAsFloat(source, format);
                arrayIndex++;
            }
            if (dataLength < stride) {
                skip(source, stride - dataLength);
            }
            index += stride;
        }
    }

    private static void populateVector3fArray(Vector3f[] array, ByteBuffer source, int count, int byteOffset, int byteStride, int numComponents, VertexBuffer.Format format) throws IOException {
        int componentSize = format.getComponentSize();
        int index = byteOffset;
        int dataLength = componentSize * numComponents;
        int stride = Math.max(dataLength, byteStride);
        int end = count * stride + byteOffset;
        source.position(source.position() + byteOffset);
        int arrayIndex = 0;
        while (index < end) {
            array[arrayIndex] = new Vector3f(
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format)
            );

            arrayIndex++;
            if (dataLength < stride) {
                skip(source, stride - dataLength);
            }
            index += stride;
        }
    }

    private static void populateQuaternionArray(Quaternion[] array, ByteBuffer source, int count, int byteOffset, int byteStride, int numComponents, VertexBuffer.Format format) throws IOException {
        int componentSize = format.getComponentSize();
        int index = byteOffset;
        int dataLength = componentSize * numComponents;
        int stride = Math.max(dataLength, byteStride);
        int end = count * stride + byteOffset;
        source.position(source.position() + byteOffset);
        int arrayIndex = 0;
        while (index < end) {
            array[arrayIndex] = new Quaternion(
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format)
            );

            arrayIndex++;
            if (dataLength < stride) {
                skip(source, stride - dataLength);
            }
            index += stride;
        }
    }

    private static void populateMatrix4fArray(Matrix4f[] array, ByteBuffer source, int count, int byteOffset, int byteStride, int numComponents, VertexBuffer.Format format) throws IOException {
        int componentSize = format.getComponentSize();
        int index = byteOffset;
        int dataLength = componentSize * numComponents;
        int stride = Math.max(dataLength, byteStride);
        int end = count * stride + byteOffset;
        source.position(source.position() + byteOffset);
        int arrayIndex = 0;
        while (index < end) {

            array[arrayIndex] = toRowMajor(
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format),
                    readAsFloat(source, format)
            );
            //gltf matrix are column major, JME ones are row major.

            arrayIndex++;
            if (dataLength < stride) {
                skip(source, stride - dataLength);
            }

            index += stride;
        }
    }

    public static Matrix4f toRowMajor(float m00, float m01, float m02, float m03,
                                      float m10, float m11, float m12, float m13,
                                      float m20, float m21, float m22, float m23,
                                      float m30, float m31, float m32, float m33) {
        return new Matrix4f(m00, m10, m20, m30, m01, m11, m21, m31, m02, m12, m22, m32, m03, m13, m23, m33);
    }

    public static GltfModelKey getKey(AssetInfo info) {
        if (info.getKey() instanceof GltfModelKey) {
            return (GltfModelKey) info.getKey();
        }
        return null;
    }

    public static MaterialAdapter getAdapterForMaterial(AssetInfo info, String defName) {
        GltfModelKey key = getKey(info);
        if (key == null) {
            return null;
        }
        return key.getAdapterForMaterial(defName);
    }

    public static boolean isKeepSkeletonPose(AssetInfo info) {
        GltfModelKey key = getKey(info);
        if (key == null) {
            return false;
        }
        return key.isKeepSkeletonPose();
    }

    public static LittleEndien getStream(byte[] buffer) {
        return new LittleEndien(new DataInputStream(new ByteArrayInputStream(buffer)));
    }

    public static String getAsString(JsonObject parent, String name) {
        JsonElement el = parent.get(name);
        return el == null ? null : el.getAsString();
    }

    public static Integer getAsInteger(JsonObject parent, String name) {
        JsonElement el = parent.get(name);
        return el == null ? null : el.getAsInt();
    }

    /**
     * Returns the specified element from the given parent as an <code>int</code>,
     * throwing an exception if it is not present.
     * 
     * @param parent The parent element
     * @param parentName The parent name
     * @param name The name of the element
     * @return The value, as an <code>int</code>
     * @throws AssetLoadException If the element is not present
     */
	public static int getAsInt(JsonObject parent, String parentName, String name) {
		JsonElement el = parent.get(name);
		if (el == null) {
			throw new AssetLoadException("No " + name + " defined for " + parentName);
		}
		return el.getAsInt();
	}
    
    public static Integer getAsInteger(JsonObject parent, String name, int defaultValue) {
        JsonElement el = parent.get(name);
        return el == null ? defaultValue : el.getAsInt();
    }

    public static Float getAsFloat(JsonObject parent, String name) {
        JsonElement el = parent.get(name);
        return el == null ? null : el.getAsFloat();
    }

    public static Float getAsFloat(JsonObject parent, String name, float defaultValue) {
        JsonElement el = parent.get(name);
        return el == null ? defaultValue : el.getAsFloat();
    }

    public static Boolean getAsBoolean(JsonObject parent, String name) {
        JsonElement el = parent.get(name);
        return el == null ? null : el.getAsBoolean();
    }

    public static Boolean getAsBoolean(JsonObject parent, String name, boolean defaultValue) {
        JsonElement el = parent.get(name);
        return el == null ? defaultValue : el.getAsBoolean();
    }

    public static ColorRGBA getAsColor(JsonObject parent, String name) {
        JsonElement el = parent.get(name);
        if (el == null) {
            return null;
        }
        JsonArray color = el.getAsJsonArray();
        return new ColorRGBA(color.get(0).getAsFloat(), color.get(1).getAsFloat(), color.get(2).getAsFloat(), color.size() > 3 ? color.get(3).getAsFloat() : 1f);
    }

    public static ColorRGBA getAsColor(JsonObject parent, String name, ColorRGBA defaultValue) {
        ColorRGBA color = getAsColor(parent, name);
        return color == null ? defaultValue : color;
    }


    public static void assertNotNull(Object o, String errorMessage) {
        if (o == null) {
            throw new AssetLoadException(errorMessage);
        }
    }

    private static final float EPSILON = 0.0001f;

    public static boolean equalsEpsilon(Vector3f v1, Vector3f v2) {
        return FastMath.abs(v1.x - v2.x) < EPSILON
                && FastMath.abs(v1.y - v2.y) < EPSILON
                && FastMath.abs(v1.z - v2.z) < EPSILON;
    }

    public static boolean equalsEpsilon(Quaternion q1, Quaternion q2) {
        return (FastMath.abs(q1.getX() - q2.getX()) < EPSILON
                && FastMath.abs(q1.getY() - q2.getY()) < EPSILON
                && FastMath.abs(q1.getZ() - q2.getZ()) < EPSILON
                && FastMath.abs(q1.getW() - q2.getW()) < EPSILON)
                ||
 (FastMath.abs(q1.getX() + q2.getX()) < EPSILON
                && FastMath.abs(q1.getY() + q2.getY()) < EPSILON
                && FastMath.abs(q1.getZ() + q2.getZ()) < EPSILON
                && FastMath.abs(q1.getW() + q2.getW()) < EPSILON);
    }


    public static void dumpArray(Object[] array) {
        if (array == null) {
            System.err.println("null");
            return;
        }
        for (int i = 0; i < array.length; i++) {
            Object o = array[i];
            System.err.print(i + ": ");
            if (o instanceof Quaternion) {
                Quaternion q = (Quaternion) o;
                System.err.print("(");
                if (q.getX() > 0.00001) System.err.print(q.getX() + ", ");
                else System.err.print("0.0, ");
                if (q.getY() > 0.00001) System.err.print(q.getY() + ", ");
                else System.err.print("0.0, ");
                if (q.getZ() > 0.00001) System.err.print(q.getZ() + ", ");
                else System.err.print("0.0, ");
                if (q.getW() > 0.00001) System.err.print(q.getW() + ", ");
                else System.err.print("0.0, ");
                System.err.println(")");
            } else {
                System.err.println(o.toString() + ", ");
            }
        }
        System.err.println("");
    }

    public static void dumpArray(float[] array) {
        if (array == null) {
            System.err.println("null");
            return;
        }

        for (int i = 0; i < array.length; i++) {
            float o = array[i];
            System.err.println(i + ": " + o);
        }

        System.err.println("");
    }

    public static Spatial findCommonAncestor(List<Spatial> spatials) {
        Map<Spatial, List<Spatial>> flatParents = new HashMap<>();

        for (Spatial spatial : spatials) {
            List<Spatial> parents = new ArrayList<>();
            Spatial parent = spatial.getParent();
            while (parent != null) {
                parents.add(0, parent);
                parent = parent.getParent();
            }
            flatParents.put(spatial, parents);
        }

        int index = 0;
        Spatial lastCommonParent = null;
        Spatial parent = null;
        while (true) {
            for (Spatial spatial : flatParents.keySet()) {
                List<Spatial> parents = flatParents.get(spatial);
                if (parents.isEmpty()) {
                    continue;
                }
                if (index == parents.size()) {
                    //we reached the end of a spatial hierarchy let's return;
                    return lastCommonParent;
                }
                Spatial p = parents.get(index);
                if (parent == null) {
                    parent = p;
                } else if (p != parent) {
                    return lastCommonParent;
                }
            }
            lastCommonParent = parent;
            parent = null;
            index++;
        }



    }

    public static void dumpMesh(Mesh m) {
        for (VertexBuffer vertexBuffer : m.getBufferList().getArray()) {
            System.err.println(vertexBuffer.getBufferType());
            System.err.println(vertexBuffer.getFormat());
            if (vertexBuffer.getData() instanceof FloatBuffer) {
                FloatBuffer b = (FloatBuffer) vertexBuffer.getData();
                float[] arr = new float[b.capacity()];
                b.rewind();
                b.get(arr);
                b.rewind();
                for (float v : arr) {
                    System.err.print(v + ",");
                }
            }
            if (vertexBuffer.getData() instanceof ShortBuffer) {
                ShortBuffer b = (ShortBuffer) vertexBuffer.getData();
                short[] arr = new short[b.capacity()];
                b.rewind();
                b.get(arr);
                b.rewind();
                for (short v : arr) {
                    System.err.print(v + ",");
                }
            }
            if (vertexBuffer.getData() instanceof IntBuffer) {
                IntBuffer b = (IntBuffer) vertexBuffer.getData();
                int[] arr = new int[b.capacity()];
                b.rewind();
                b.get(arr);
                b.rewind();
                for (int v : arr) {
                    System.err.print(v + ",");
                }
            }
            System.err.println("\n---------------------------");
        }
    }

    public static void readToByteBuffer(InputStream input, ByteBuffer dst, int bytesToRead) throws IOException {
        if (bytesToRead <= 0) throw new IOException("bytesToRead must be > 0");

        int startPos = dst.position();
        int remaining = dst.limit() - startPos;
        if (remaining < bytesToRead) {
            throw new IOException("Destination ByteBuffer too small: remaining=" + remaining + " < bytesToRead=" + bytesToRead);
        }
    
        ReadableByteChannel ch = Channels.newChannel(input); 
        int total = 0;
        while (total < bytesToRead) {
            int n = ch.read(dst);
            if (n == -1) break;
            total += n;
        }

        if (total < bytesToRead) {
            throw new IOException("Data ended prematurely " + total + " < " + bytesToRead);
        }

        dst.flip();
    }

    public static void readToByteArray(InputStream input, byte[] dst, int bytesToRead) throws IOException {
        if (bytesToRead < 0) throw new IllegalArgumentException("bytesToRead < 0");
        if (bytesToRead > dst.length) {
            throw new IOException("Destination array too small: length=" + dst.length + " < bytesToRead=" + bytesToRead);
        }

        int totalRead = 0;
        while (totalRead < bytesToRead) {
            int n = input.read(dst, totalRead, bytesToRead - totalRead);
            if (n == -1) break;
            totalRead += n;
        }

        if (totalRead < bytesToRead) {
            throw new IOException("Data ended prematurely " + totalRead + " < " + bytesToRead);
        }
    }


    /**
     * Try to expose a glTF buffer region as a typed NIO view without copying.
     * Falls back to allocating a destination buffer and populating it when
     * interleaving, normalization, or format mismatch prevents a pure view.
     *
     * @param source         the original ByteBuffer (direct or heap)
     * @param count          number of elements
     * @param byteOffset     start offset within source (relative to beginning)
     * @param byteStride     stride in bytes (0 means tightly packed = element size)
     * @param numComponents  components per element (e.g. 3 for VEC3)
     * @param originalFormat the source component type  
     * @param targetFormat   the desired buffer view type to return
     */
    public static Buffer getBufferView(ByteBuffer source, int byteOffset,  int count, int byteStride,
                                       int numComponents, VertexBuffer.Format originalFormat,
                                       VertexBuffer.Format targetFormat) throws IOException {
        // Work in little-endian as per glTF spec
        source = asReadableByteBuffer(source);  

        // Layout from source format
        int srcCompSize = originalFormat.getComponentSize();
        int elemSize = srcCompSize * numComponents;
        int stride = Math.max(elemSize, byteStride);
        int start = byteOffset;
        int bytes = stride * count;


        boolean tightlyPacked = (stride == elemSize);

        if (tightlyPacked) {
            ByteBuffer view = source.duplicate();
            view.position(start).limit(start + bytes);
            view = view.slice().order(ByteOrder.LITTLE_ENDIAN);

            // Zero-copy returns only when source/target formats are compatible and aligned
            switch (targetFormat) {
                case Byte:
                case UnsignedByte:
                    if (srcCompSize == 1 &&
                        (originalFormat == VertexBuffer.Format.Byte ||
                         originalFormat == VertexBuffer.Format.UnsignedByte)) {
                        return view;
                    }
                    break;

                case Short:
                case UnsignedShort:
                    if (srcCompSize == 2 &&
                        (originalFormat == VertexBuffer.Format.Short ||
                         originalFormat == VertexBuffer.Format.UnsignedShort) &&
                        (start & 1) == 0) {
                        return view.asShortBuffer();
                    }
                    break;

                case Int:
                case UnsignedInt:
                    if (srcCompSize == 4 &&
                        (originalFormat == VertexBuffer.Format.Int ||
                         originalFormat == VertexBuffer.Format.UnsignedInt) &&
                        (start & 3) == 0) {
                        return view.asIntBuffer();
                    }
                    break;

                case Float:
                    if (srcCompSize == 4 &&
                        originalFormat == VertexBuffer.Format.Float &&
                        (start & 3) == 0) {
                        return view.asFloatBuffer();
                    }
                    break;

                case Double:
                    if (srcCompSize == 8 &&
                        originalFormat == VertexBuffer.Format.Double &&
                        (start & 7) == 0) {
                        return view.asDoubleBuffer();
                    }
                    break;
            }
        }

        // Fallback: allocate destination buffer by desired targetFormat and populate from source
        int elements = count * numComponents;
        switch (targetFormat) {
            case Byte:
            case UnsignedByte: {
                ByteBuffer out = BufferUtils.createByteBuffer(elements);
                populateBuffer(out, source, count, byteOffset, byteStride, numComponents, originalFormat);
                return out;
            }
            case Short:
            case UnsignedShort: {
                ShortBuffer out = BufferUtils.createShortBuffer(elements);
                populateBuffer(out, source, count, byteOffset, byteStride, numComponents, originalFormat);
                return out;
            }
            case Int:
            case UnsignedInt: {
                IntBuffer out = BufferUtils.createIntBuffer(elements);
                populateBuffer(out, source, count, byteOffset, byteStride, numComponents, originalFormat);
                return out;
            }
            case Float: {
                FloatBuffer out = BufferUtils.createFloatBuffer(elements);
                populateBuffer(out, source, count, byteOffset, byteStride, numComponents, originalFormat);
                return out;
            }
            case Double:
                throw new IllegalArgumentException("Double conversion fallback not supported");
            default:
                throw new IllegalArgumentException("Unsupported format " + targetFormat);
        }
    }


}
