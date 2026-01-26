package com.jme3.scene.shape;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;


/**
 * The FullscreenTriangle class defines a mesh representing a single
 * triangle that spans the entire screen. It is typically used in rendering
 * techniques where a fullscreen quad or triangle is needed, such as in
 * post-processing effects or screen-space operations.
 */
public class FullscreenTriangle extends Mesh {
    private static final float[] POSITIONS = {
        -1, -1, 0,
        3, -1, 0,
        -1, 3, 0
    };

    private static final float[] TEXCOORDS = {
            0,0,
            2,0,
            0,2
    };


    public FullscreenTriangle() {
        super();
        setBuffer(VertexBuffer.Type.Position, 3, POSITIONS);
        setBuffer(VertexBuffer.Type.TexCoord, 2, TEXCOORDS);
        setBuffer(VertexBuffer.Type.Index, 3, new short[]{0, 1, 2});
        updateBound();
    }
}
