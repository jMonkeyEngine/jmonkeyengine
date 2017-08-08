package com.jme3.scene.plugins.gltf;

import com.google.gson.*;
import com.jme3.asset.AssetLoadException;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.LittleEndien;

import java.io.*;
import java.nio.*;

/**
 * Created by Nehon on 07/08/2017.
 */
public class GltfUtils {

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
            case 5120:
                return VertexBuffer.Format.Byte;
            case 5121:
                return VertexBuffer.Format.UnsignedByte;
            case 5122:
                return VertexBuffer.Format.Short;
            case 5123:
                return VertexBuffer.Format.UnsignedShort;
            case 5125:
                return VertexBuffer.Format.UnsignedInt;
            case 5126:
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
            case "WEIGHT_0":
                return VertexBuffer.Type.BoneWeight;
            default:
                throw new AssetLoadException("Unsupported buffer attribute: " + attribute);

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

    public static void populateBuffer(Buffer buffer, byte[] source, int length, int byteOffset, int byteStride, int numComponents) throws IOException {
        buffer.clear();

        if (buffer instanceof ByteBuffer) {
            populateByteBuffer((ByteBuffer) buffer, source, length, byteOffset, byteStride, numComponents);
            return;
        }
        LittleEndien stream = getStream(source);
        if (buffer instanceof ShortBuffer) {
            populateShortBuffer((ShortBuffer) buffer, stream, length, byteOffset, byteStride, numComponents);
        } else if (buffer instanceof IntBuffer) {
            populateIntBuffer((IntBuffer) buffer, stream, length, byteOffset, byteStride, numComponents);
        } else if (buffer instanceof FloatBuffer) {
            populateFloatBuffer((FloatBuffer) buffer, stream, length, byteOffset, byteStride, numComponents);
        }
        buffer.rewind();
    }

    private static void populateByteBuffer(ByteBuffer buffer, byte[] source, int length, int byteOffset, int byteStride, int numComponents) {
        int index = byteOffset;
        int componentSize = 1;
        while (index < length + byteOffset) {
            for (int i = 0; i < numComponents; i++) {
                buffer.put(source[index + i]);
            }
            index += Math.max(componentSize * numComponents, byteStride);
        }
    }

    private static void populateShortBuffer(ShortBuffer buffer, LittleEndien stream, int length, int byteOffset, int byteStride, int numComponents) throws IOException {
        int index = byteOffset;
        int componentSize = 2;
        int end = length * componentSize + byteOffset;
        stream.skipBytes(byteOffset);
        while (index < end) {
            for (int i = 0; i < numComponents; i++) {
                buffer.put(stream.readShort());
            }
            index += Math.max(componentSize * numComponents, byteStride);
        }
    }

    private static void populateIntBuffer(IntBuffer buffer, LittleEndien stream, int length, int byteOffset, int byteStride, int numComponents) throws IOException {
        int index = byteOffset;
        int componentSize = 4;
        int end = length * componentSize + byteOffset;
        stream.skipBytes(byteOffset);
        while (index < end) {
            for (int i = 0; i < numComponents; i++) {
                buffer.put(stream.readInt());
            }
            index += Math.max(componentSize * numComponents, byteStride);
        }
    }

    private static void populateFloatBuffer(FloatBuffer buffer, LittleEndien stream, int length, int byteOffset, int byteStride, int numComponents) throws IOException {
        int index = byteOffset;
        int componentSize = 4;
        int end = length * componentSize + byteOffset;
        stream.skipBytes(byteOffset);
        while (index < end) {
            for (int i = 0; i < numComponents; i++) {
                buffer.put(stream.readFloat());
            }
            index += Math.max(componentSize * numComponents, byteStride);
        }
    }

    private static LittleEndien getStream(byte[] buffer) {
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
        return new ColorRGBA(color.get(0).getAsFloat(), color.get(1).getAsFloat(), color.get(2).getAsFloat(), color.get(3).getAsFloat());
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
}
