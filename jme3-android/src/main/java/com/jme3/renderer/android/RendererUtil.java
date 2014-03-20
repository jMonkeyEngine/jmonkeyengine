package com.jme3.renderer.android;

import android.opengl.GLES20;
import android.opengl.GLU;
import com.jme3.renderer.RendererException;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;

/**
 * Utility class used by the {@link OGLESShaderRenderer renderer} and sister
 * classes.
 *
 * @author Kirill Vainer
 */
public class RendererUtil {

    /**
     * When set to true, every OpenGL call will check for errors and throw an
     * exception if there is one, if false, no error checking is performed.
     */
    public static boolean ENABLE_ERROR_CHECKING = true;

    /**
     * Checks for an OpenGL error and throws a {@link RendererException} if
     * there is one. Ignores the value of
     * {@link RendererUtil#ENABLE_ERROR_CHECKING}.
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
     * Checks for an EGL error and throws a {@link RendererException} if there
     * is one. Ignores the value of {@link RendererUtil#ENABLE_ERROR_CHECKING}.
     */
    public static void checkEGLError(EGL10 egl) {
        int error = egl.eglGetError();
        if (error != EGL10.EGL_SUCCESS) {
            String errorMessage;
            switch (error) {
                case EGL10.EGL_SUCCESS:
                    return;
                case EGL10.EGL_NOT_INITIALIZED:
                    errorMessage = "EGL is not initialized, or could not be "
                            + "initialized, for the specified EGL display connection. ";
                    break;
                case EGL10.EGL_BAD_ACCESS:
                    errorMessage = "EGL cannot access a requested resource "
                            + "(for example a context is bound in another thread). ";
                    break;
                case EGL10.EGL_BAD_ALLOC:
                    errorMessage = "EGL failed to allocate resources for the requested operation.";
                    break;
                case EGL10.EGL_BAD_ATTRIBUTE:
                    errorMessage = "An unrecognized attribute or attribute "
                            + "value was passed in the attribute list. ";
                    break;
                case EGL10.EGL_BAD_CONTEXT:
                    errorMessage = "An EGLContext argument does not name a valid EGL rendering context. ";
                    break;
                case EGL10.EGL_BAD_CONFIG:
                    errorMessage = "An EGLConfig argument does not name a valid EGL frame buffer configuration. ";
                    break;
                case EGL10.EGL_BAD_CURRENT_SURFACE:
                    errorMessage = "The current surface of the calling thread "
                            + "is a window, pixel buffer or pixmap that is no longer valid. ";
                    break;
                case EGL10.EGL_BAD_DISPLAY:
                    errorMessage = "An EGLDisplay argument does not name a valid EGL display connection. ";
                    break;
                case EGL10.EGL_BAD_SURFACE:
                    errorMessage = "An EGLSurface argument does not name a "
                            + "valid surface (window, pixel buffer or pixmap) configured for GL rendering. ";
                    break;
                case EGL10.EGL_BAD_MATCH:
                    errorMessage = "Arguments are inconsistent (for example, a "
                            + "valid context requires buffers not supplied by a valid surface). ";
                    break;
                case EGL10.EGL_BAD_PARAMETER:
                    errorMessage = "One or more argument values are invalid.";
                    break;
                case EGL10.EGL_BAD_NATIVE_PIXMAP:
                    errorMessage = "A NativePixmapType argument does not refer to a valid native pixmap. ";
                    break;
                case EGL10.EGL_BAD_NATIVE_WINDOW:
                    errorMessage = "A NativeWindowType argument does not refer to a valid native window. ";
                    break;
                case EGL11.EGL_CONTEXT_LOST:
                    errorMessage = "A power management event has occurred. "
                            + "The application must destroy all contexts and reinitialise "
                            + "OpenGL ES state and objects to continue rendering. ";
                    break;
                default:
                    errorMessage = "Unknown";
            }
            
            throw new RendererException("EGL error 0x" + Integer.toHexString(error) + ": " + errorMessage);
        }
    }

    /**
     * Checks for an OpenGL error and throws a {@link RendererException} if
     * there is one. Does nothing if {@link RendererUtil#ENABLE_ERROR_CHECKING}
     * is set to
     * <code>false</code>.
     */
    public static void checkGLError() {
        if (!ENABLE_ERROR_CHECKING) {
            return;
        }
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
