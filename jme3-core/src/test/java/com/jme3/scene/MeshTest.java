package com.jme3.scene;

import com.jme3.scene.shape.Box;
import org.junit.Test;

/**
 * Created by arkkadhiratara on 3/22/16.
 */
public class MeshTest {
    @Test
    public void extractVertexDataTest() {
        Box m = new Box(10,10,10);
        Box m2 = new Box(20,20,20);

        // Extract Vertex Data m into m2
        m2.extractVertexData(m);

        // Check does the new extracted m2 have the same value with m1.

        // Have the same mode
        assert m.getMode() == m2.getMode();

        // Have the same Instance Count
        assert m.getInstanceCount() == m2.getInstanceCount();

        // Have the same point size
        assert m.getPointSize() == m2.getPointSize();

        // Have the same buffer in general
        assert m.getBufferList().size() == m2.getBufferList().size();
        assert m.getIndexBuffer().size() == m2.getIndexBuffer().size();
        assert m.getBuffer(VertexBuffer.Type.Index).getUniqueId() == m2.getBuffer(VertexBuffer.Type.Index).getUniqueId();
        assert m.getBuffer(VertexBuffer.Type.Index).getNumElements() == m2.getBuffer(VertexBuffer.Type.Index).getNumElements();

        assert m.getBuffer(VertexBuffer.Type.Position).getUniqueId() == m2.getBuffer(VertexBuffer.Type.Position).getUniqueId();
        assert m.getBuffer(VertexBuffer.Type.Position).getNumElements() == m2.getBuffer(VertexBuffer.Type.Position).getNumElements();

        assert m.getBuffer(VertexBuffer.Type.Normal).getUniqueId() == m2.getBuffer(VertexBuffer.Type.Normal).getUniqueId();
        assert m.getBuffer(VertexBuffer.Type.Normal).getNumElements() == m2.getBuffer(VertexBuffer.Type.Normal).getNumElements();

        assert m.getBuffer(VertexBuffer.Type.TexCoord).getUniqueId() == m2.getBuffer(VertexBuffer.Type.TexCoord).getUniqueId();
        assert m.getBuffer(VertexBuffer.Type.TexCoord).getNumElements() == m2.getBuffer(VertexBuffer.Type.TexCoord).getNumElements();
    }
}
