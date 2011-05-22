package com.jme3.scene.mesh;

import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * <code>WrappedIndexBuffer</code> converts from one representation of mesh
 * data to another. For example it can be used to read TriangleStrip data
 * as if it was in Triangle format.
 * 
 * @author Kirill Vainer
 */
public class WrappedIndexBuffer extends VirtualIndexBuffer {

    private final IndexBuffer ib;

    public WrappedIndexBuffer(Mesh mesh){
        super(mesh.getVertexCount(), mesh.getMode());
        this.ib = mesh.getIndexBuffer();
        switch (meshMode){
            case Points:
                numIndices = mesh.getTriangleCount();
                break;
            case Lines:
            case LineLoop:
            case LineStrip:
                numIndices = mesh.getTriangleCount() * 2;
                break;
            case Triangles:
            case TriangleStrip:
            case TriangleFan:
                numIndices = mesh.getTriangleCount() * 3;
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public int get(int i) {
        int superIdx = super.get(i);
        return ib.get(superIdx);
    }

    @Override
    public Buffer getBuffer() {
        return ib.getBuffer();
    }

    public static void convertToList(Mesh mesh){
        IndexBuffer inBuf = mesh.getIndexBuffer();
        if (inBuf == null){
            inBuf = new VirtualIndexBuffer(mesh.getVertexCount(), mesh.getMode());
        }else{
            inBuf = new WrappedIndexBuffer(mesh);
        }

        IndexBuffer outBuf;
        if (inBuf.size() > Short.MAX_VALUE * 2){
            outBuf = new IndexIntBuffer(BufferUtils.createIntBuffer(inBuf.size()));
        }else{
            outBuf = new IndexShortBuffer(BufferUtils.createShortBuffer(inBuf.size()));
        }

        for (int i = 0; i < inBuf.size(); i++){
            outBuf.put(i, inBuf.get(i));
        }

        mesh.clearBuffer(Type.Index);
        mesh.setMode(Mode.Triangles);
        if (outBuf instanceof IndexIntBuffer){
            mesh.setBuffer(Type.Index, 3, (IntBuffer)outBuf.getBuffer());
        }else{
            mesh.setBuffer(Type.Index, 3, (ShortBuffer)outBuf.getBuffer());
        }
    }
    
}
