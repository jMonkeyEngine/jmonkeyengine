package com.jme3.renderer.android;

import android.opengl.GLES20;

public class Android22Workaround {
    public static void glVertexAttribPointer(int location, int components, int format, boolean normalize, int stride, int offset){
        GLES20.glVertexAttribPointer(location,
                                     components,
                                     format,
                                     normalize,
                                     stride,
                                     offset);
    }
}
