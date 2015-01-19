package com.jme3.renderer.opengl;

import com.jme3.renderer.RendererException;

public abstract class GLDebug {

    protected GL gl;
    
    protected String decodeError(int err) {
        String errMsg;
        switch (err) {
            case GL.GL_NO_ERROR:
                errMsg = "No Error";
                break;
            case GL.GL_INVALID_ENUM:
                errMsg = "Invalid enum argument";
                break;
            case GL.GL_INVALID_OPERATION:
                errMsg = "Invalid operation";
                break;
            case GL.GL_INVALID_VALUE:
                errMsg = "Invalid numeric argument";
                break;
            case GL.GL_OUT_OF_MEMORY:
                errMsg = "Out of memory";
                break;
            case GLFbo.GL_INVALID_FRAMEBUFFER_OPERATION_EXT:
                errMsg = "Framebuffer is not complete";
                break;
            case GL2.GL_STACK_OVERFLOW:
                errMsg = "Internal stack overflow";
                break;
            case GL2.GL_STACK_UNDERFLOW:
                errMsg = "Internal stack underflow";
                break;
            default:
                errMsg = "Unknown";
                break;
        }
        return errMsg + " (Error Code: " + err + ")";
    }

    protected void checkError() {
        int err = gl.glGetError();
        if (err != 0) {
            throw new RendererException("An OpenGL error occurred - " + decodeError(err));
        }
    }
}
