package com.jme3.renderer.android;
 
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import java.util.logging.Logger;
 
/**
 * <code>AndroidGLSurfaceView</code> is derived from GLSurfaceView
 * @author iwgeric
 *
 */
public class AndroidGLSurfaceView extends GLSurfaceView {
 
    private final static Logger logger = Logger.getLogger(AndroidGLSurfaceView.class.getName());
 
    public AndroidGLSurfaceView(Context ctx, AttributeSet attribs) {
        super(ctx, attribs);
    }
 
    public AndroidGLSurfaceView(Context ctx) {
        super(ctx);
    }
 
 
}