package com.jme3.renderer.android;

import android.opengl.GLES20;
import android.opengl.GLU;
import com.jme3.renderer.RendererException;

/**
 * Utility class used by the {@link OGLESShaderRenderer renderer} and sister classes.
 * 
 * @author Kirill Vainer
 */
public class RendererUtil {
    
    /**
     * When set to true, every OpenGL call will check for errors and throw
     * an exception if there is one, if false, no error checking is performed.
     */
    public static boolean ENABLE_ERROR_CHECKING = true;
    
    /**
     * Checks for an OpenGL error and throws a {@link RendererException}
     * if there is one. Ignores the value of {@link RendererUtil#ENABLE_ERROR_CHECKING}.
     */
    public static void checkGLErrorForced() {
        int error = GLES20.glGetError();
        if (error != 0) {
            String message = GLU.gluErrorString(error);
            if (message == null) {
                throw new RendererException("An unknown OpenGL error has occurred.");
            } else {
                throw new RendererException("An OpenGL error has occurred: " + message);
            }
        }
    }
    
    /**
     * Checks for an OpenGL error and throws a {@link RendererException}
     * if there is one. Does nothing if {@link RendererUtil#ENABLE_ERROR_CHECKING}
     * is set to <code>false</code>.
     */
    public static void checkGLError() {
        if (!ENABLE_ERROR_CHECKING) return;
        int error = GLES20.glGetError();
        if (error != 0) {
            String message = GLU.gluErrorString(error);
            if (message == null) {
                throw new RendererException("An unknown OpenGL error has occurred.");
            } else {
                throw new RendererException("An OpenGL error has occurred: " + message);
            }
        }
    }
}
