package com.jme3.renderer.opengl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class GLDebugDesktop extends GLDebugES implements GL2, GL3, GL4 {

    private final GL2 gl2;
    private final GL3 gl3;
    private final GL4 gl4;
    
    public GLDebugDesktop(GL gl, GLExt glext, GLFbo glfbo) {
        super(gl, glext, glfbo);
        this.gl2 = gl instanceof GL2 ? (GL2) gl : null;
        this.gl3 = gl instanceof GL3 ? (GL3) gl : null;
        this.gl4 = gl instanceof GL4 ? (GL4) gl : null;
    }
    
    public void glAlphaFunc(int func, float ref) {
        gl2.glAlphaFunc(func, ref);
        checkError();
    }

    public void glPointSize(float size) {
        gl2.glPointSize(size);
        checkError();
    }

    public void glPolygonMode(int face, int mode) {
        gl2.glPolygonMode(face, mode);
        checkError();
    }

    public void glDrawBuffer(int mode) {
        gl2.glDrawBuffer(mode);
        checkError();
    }

    public void glReadBuffer(int mode) {
        gl2.glReadBuffer(mode);
        checkError();
    }

    public void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, ByteBuffer data) {
        gl2.glCompressedTexImage3D(target, level, internalformat, width, height, depth, border, data);
        checkError();
    }

    public void glCompressedTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, ByteBuffer data) {
        gl2.glCompressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, data);
        checkError();
    }

    public void glTexImage3D(int target, int level, int internalFormat, int width, int height, int depth, int border, int format, int type, ByteBuffer data) {
        gl2.glTexImage3D(target, level, internalFormat, width, height, depth, border, format, type, data);
        checkError();
    }

    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, ByteBuffer data) {
        gl2.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
        checkError();
    }

    public void glBindFragDataLocation(int param1, int param2, String param3) {
        gl3.glBindFragDataLocation(param1, param2, param3);
        checkError();
    }

    public void glBindVertexArray(int param1) {
        gl3.glBindVertexArray(param1);
        checkError();
    }

    public void glGenVertexArrays(IntBuffer param1) {
        gl3.glGenVertexArrays(param1);
        checkError();
    }
    
    @Override
    public String glGetString(int param1, int param2) {
        String result = gl3.glGetString(param1, param2);
        checkError();
        return result;
    }

    @Override
    public void glDeleteVertexArrays(IntBuffer arrays) {
        gl3.glDeleteVertexArrays(arrays);
        checkError();
    }

    @Override
    public void glPatchParameter(int count) {
        gl4.glPatchParameter(count);
        checkError();
    }

    @Override
    public void glFramebufferTextureLayer(int param1, int param2, int param3, int param4, int param5) {
        gl3.glFramebufferTextureLayer(param1, param2, param3, param4, param5);
        checkError();
    }

    public void glBlendEquationSeparate(int colorMode, int alphaMode) {
        gl.glBlendEquationSeparate(colorMode, alphaMode);
        checkError();
    }
}
