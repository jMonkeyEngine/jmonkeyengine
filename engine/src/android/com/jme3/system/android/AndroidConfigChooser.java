package com.jme3.system.android;

import java.util.logging.Logger;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView.EGLConfigChooser;

/**
 * AndroidConfigChooser is used to determine the best suited EGL Config
 * @author larynx
 *
 */
public class AndroidConfigChooser implements EGLConfigChooser 
{
    private static final Logger logger = Logger.getLogger(AndroidConfigChooser.class.getName());
    
    protected int clientOpenGLESVersion = 0;
    protected EGLConfig bestConfig = null;
    protected EGLConfig fastestConfig = null;
    protected EGLConfig choosenConfig = null;
    protected ConfigType type;
    protected int pixelFormat;
    
    protected boolean verbose = false;
    
    private final static int EGL_OPENGL_ES2_BIT = 4;

    public enum ConfigType 
    {
        /**
         * RGB565, 0 alpha, 16 depth, 0 stencil
         */
        FASTEST,
        /**
         * RGB???, 0 alpha, 16 depth, 0 stencil
         */
        BEST
    }
    
    public AndroidConfigChooser(ConfigType type, boolean verbose)
    {
        this.type = type;
        this.verbose = verbose;
    }
        
    /**
     * Gets called by the GLSurfaceView class to return the best config
     */    
    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display)
    {
        return choosenConfig;
    }
    
    /**
     * findConfig is used to locate the best config and init the chooser with
     * @param egl
     * @param display
     * @return true if successfull, false if no config was found
     */
    public boolean findConfig(EGL10 egl, EGLDisplay display)
    {        
        //Querying number of configurations
        int[] num_conf = new int[1];
        egl.eglGetConfigs(display, null, 0, num_conf);  //if configuration array is null it still returns the number of configurations
        int configurations = num_conf[0];

        //Querying actual configurations
        EGLConfig[] conf = new EGLConfig[configurations];
        egl.eglGetConfigs(display, conf, configurations, num_conf);
          
        int[] value = new int[1];        
   
        
        if (configurations <= 0)
        {
            logger.severe("###ERROR### ZERO EGL Configurations found, This Is a Problem"); 
        }
        
        // Loop over all configs to get the best
        for(int i = 0; i < configurations; i++)
        {                    
            if (conf[i] != null)
            {                
                egl.eglGetConfigAttrib(display, conf[i], EGL10.EGL_SURFACE_TYPE, value);
                // check if conf is a valid gl window
                if ((value[0] & EGL10.EGL_WINDOW_BIT) != 0)
                {                    
                    egl.eglGetConfigAttrib(display, conf[i], EGL10.EGL_DEPTH_SIZE, value);
                    // check if conf has a minimum depth of 16
                    if (value[0] >= 16)
                    {
                        egl.eglGetConfigAttrib(display, conf[i], EGL10.EGL_RENDERABLE_TYPE, value);
                        // Check if conf is OpenGL ES 2.0
                        if ((value[0] & EGL_OPENGL_ES2_BIT) != 0)
                        {
                            clientOpenGLESVersion = 2;  // OpenGL ES 2.0 detected
                            bestConfig = better(bestConfig, conf[i], egl, display);
                            fastestConfig = faster(fastestConfig, conf[i], egl, display);
                            
                            if (verbose)
                            {
                                logger.info("** Supported EGL Configuration #" + i );                            
                                logEGLConfig(conf[i], display, egl);
                            }
                        }
                        else
                        {
                            if (verbose)
                            {
                                logger.info("NOT Supported EGL Configuration #" + i + " EGL_OPENGL_ES2_BIT not set");                            
                                logEGLConfig(conf[i], display, egl); 
                            }
                        }  
                    }
                    else
                    {
                        if (verbose)
                        {
                            logger.info("NOT Supported EGL Configuration #" + i + " EGL_DEPTH_SIZE < 16");                            
                            logEGLConfig(conf[i], display, egl);
                        }
                    }
                }
                else
                {
                    if (verbose)
                    {
                        logger.info("NOT Supported EGL Configuration #" + i + " EGL_WINDOW_BIT not set");                            
                        logEGLConfig(conf[i], display, egl);
                    }
                }
            }
            else
            {
                logger.severe("###ERROR### EGL Configuration #" + i + " is NULL");
            }
        }
    
        
        if ((type == ConfigType.BEST) && (bestConfig != null))
        {
            logger.info("JME3 using best EGL configuration available here: ");
            choosenConfig = bestConfig;
        }
        else
        {
            if (fastestConfig != null)
            {
                logger.info("JME3 using fastest EGL configuration available here: ");
            }
            choosenConfig = fastestConfig;            
        }
        
        if (choosenConfig != null)
        {
            logEGLConfig(choosenConfig, display, egl);               
            pixelFormat = getPixelFormat(choosenConfig, display, egl);
            clientOpenGLESVersion = getOpenGLVersion(choosenConfig, display, egl);
            return true;
        }
        else
        {
            logger.severe("###ERROR### Unable to get a valid OpenGL ES 2.0 config, nether Fastest nor Best found! Bug. Please report this.");
            clientOpenGLESVersion = 1;
            pixelFormat = PixelFormat.UNKNOWN;
            return false;
        }        
    }

    /**
     * Returns the best of the two EGLConfig passed according to depth and colours
     * @param a The first candidate
     * @param b The second candidate
     * @return The chosen candidate
     */
    private EGLConfig better(EGLConfig a, EGLConfig b, EGL10 egl, EGLDisplay display)
    {
        if(a == null) return b;
    
        EGLConfig result = null;
    
        int[] value = new int[1];
    
        // Choose highest color size
        egl.eglGetConfigAttrib(display, a, EGL10.EGL_RED_SIZE, value);
        int redA = value[0];
    
        egl.eglGetConfigAttrib(display, b, EGL10.EGL_RED_SIZE, value);
        int redB = value[0];
    
        if (redA > redB)
            result = a;
        else if (redA < redB)
            result = b;
        else // red size is equal
        {
            // Choose highest depth size
            egl.eglGetConfigAttrib(display, a, EGL10.EGL_DEPTH_SIZE, value);
            int depthA = value[0];
    
            egl.eglGetConfigAttrib(display, b, EGL10.EGL_DEPTH_SIZE, value);
            int depthB = value[0];
    
            if (depthA > depthB)
                result = a;
            else if (depthA < depthB)
                result = b;
            else // depth is equal
            {       
                
                // Choose lowest alpha size
                egl.eglGetConfigAttrib(display, a, EGL10.EGL_ALPHA_SIZE, value);
                int alphaA = value[0];
        
                egl.eglGetConfigAttrib(display, b, EGL10.EGL_ALPHA_SIZE, value);
                int alphaB = value[0];
        
                if (alphaA < alphaB)
                    result = a;
                else if (alphaA > alphaB)
                    result = b;
                else // alpha is equal
                {
                    // Choose lowest stencil size
                    egl.eglGetConfigAttrib(display, a, EGL10.EGL_STENCIL_SIZE, value);
                    int stencilA = value[0];
            
                    egl.eglGetConfigAttrib(display, b, EGL10.EGL_STENCIL_SIZE, value);
                    int stencilB = value[0];
            
                    if (stencilA < stencilB)
                        result = a;
                    else
                        result = b;
                }
            }
        }
        return result;
    }
    
    /**
     * Returns the fastest of the two EGLConfig passed according to depth and colours
     * @param a The first candidate
     * @param b The second candidate
     * @return The chosen candidate
     */
    private EGLConfig faster(EGLConfig a, EGLConfig b, EGL10 egl, EGLDisplay display)
    {
        if(a == null) return b;
    
        EGLConfig result = null;
    
        int[] value = new int[1];
    
        // Choose 565 color size
        egl.eglGetConfigAttrib(display, a, EGL10.EGL_GREEN_SIZE, value);
        int greenA = value[0];
    
        egl.eglGetConfigAttrib(display, b, EGL10.EGL_GREEN_SIZE, value);
        int greenB = value[0];
    
        if ((greenA == 6) && (greenB != 6))
            result = a;
        else if ((greenA != 6) && (greenB == 6))
            result = b;
        else // green size is equal
        {
            // Choose lowest depth size
            egl.eglGetConfigAttrib(display, a, EGL10.EGL_DEPTH_SIZE, value);
            int depthA = value[0];
    
            egl.eglGetConfigAttrib(display, b, EGL10.EGL_DEPTH_SIZE, value);
            int depthB = value[0];
    
            if (depthA < depthB)
                result = a;
            else if (depthA > depthB)
                result = b;
            else // depth is equal
            {                              
                // Choose lowest alpha size
                egl.eglGetConfigAttrib(display, a, EGL10.EGL_ALPHA_SIZE, value);
                int alphaA = value[0];
        
                egl.eglGetConfigAttrib(display, b, EGL10.EGL_ALPHA_SIZE, value);
                int alphaB = value[0];
        
                if (alphaA < alphaB)
                    result = a;
                else if (alphaA > alphaB)
                    result = b;
                else // alpha is equal
                {
                    // Choose lowest stencil size
                    egl.eglGetConfigAttrib(display, a, EGL10.EGL_STENCIL_SIZE, value);
                    int stencilA = value[0];
            
                    egl.eglGetConfigAttrib(display, b, EGL10.EGL_STENCIL_SIZE, value);
                    int stencilB = value[0];
            
                    if (stencilA < stencilB)
                        result = a;
                    else
                        result = b;
                }
            }
        }
        return result;
    }
    
    private int getPixelFormat(EGLConfig conf, EGLDisplay display, EGL10 egl)
    {
        int[] value = new int[1];
        int result = PixelFormat.RGB_565;

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_RED_SIZE, value);
        if (value[0] == 8)
        {
            result = PixelFormat.RGBA_8888;
            /*
            egl.eglGetConfigAttrib(display, conf, EGL10.EGL_ALPHA_SIZE, value);
            if (value[0] == 8)
            {
                result = PixelFormat.RGBA_8888;
            }
            else
            {
                result = PixelFormat.RGB_888;
            }*/
        }
        
        if (verbose)
        {
            logger.info("Using PixelFormat " + result);                            
        }
    
        return result;                    
    }
    
    private int getOpenGLVersion(EGLConfig conf, EGLDisplay display, EGL10 egl)
    {
        int[] value = new int[1];
        int result = 1;
                        
        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_RENDERABLE_TYPE, value);
        // Check if conf is OpenGL ES 2.0
        if ((value[0] & EGL_OPENGL_ES2_BIT) != 0)
        {
            result = 2;
        }

        return result;                    
    }
    
    /**
     * log output with egl config details
     * @param conf
     * @param display
     * @param egl
     */
    private void logEGLConfig(EGLConfig conf, EGLDisplay display, EGL10 egl)
    {
        int[] value = new int[1];

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_RED_SIZE, value);
        logger.info(String.format("EGL_RED_SIZE  = %d", value[0] ) );
        
        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_GREEN_SIZE, value);
        logger.info(String.format("EGL_GREEN_SIZE  = %d", value[0] ) );
        
        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_BLUE_SIZE, value);
        logger.info(String.format("EGL_BLUE_SIZE  = %d", value[0] ) );
        
        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_ALPHA_SIZE, value);
        logger.info(String.format("EGL_ALPHA_SIZE  = %d", value[0] ) );
        
        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_DEPTH_SIZE, value);
        logger.info(String.format("EGL_DEPTH_SIZE  = %d", value[0] ) );
                
        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_STENCIL_SIZE, value);
        logger.info(String.format("EGL_STENCIL_SIZE  = %d", value[0] ) );

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_RENDERABLE_TYPE, value);
        logger.info(String.format("EGL_RENDERABLE_TYPE  = %d", value[0] ) );
        
        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_SURFACE_TYPE, value);
        logger.info(String.format("EGL_SURFACE_TYPE  = %d", value[0] ) );               
    }
    

    
    
    // Getter - Setter
    
    public EGLConfig getBestConfig() 
    {
        return bestConfig;
    }

    public void setBestConfig(EGLConfig bestConfig) 
    {
        this.bestConfig = bestConfig;
    }
    public EGLConfig getFastestConfig() 
    {
        return fastestConfig;
    }

    public void setFastestConfig(EGLConfig fastestConfig) 
    {
        this.fastestConfig = fastestConfig;
    }
        
    public int getClientOpenGLESVersion() 
    {
        return clientOpenGLESVersion;
    }

    public void setClientOpenGLESVersion(int clientOpenGLESVersion) 
    {
        this.clientOpenGLESVersion = clientOpenGLESVersion;
    }
    
    public int getPixelFormat() 
    {
        return pixelFormat;
    }
    
}
