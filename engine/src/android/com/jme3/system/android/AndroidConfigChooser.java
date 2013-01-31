package com.jme3.system.android;

import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import java.util.logging.Logger;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * AndroidConfigChooser is used to determine the best suited EGL Config
 *
 * @author larynx
 */
public class AndroidConfigChooser implements EGLConfigChooser {

    private static final Logger logger = Logger.getLogger(AndroidConfigChooser.class.getName());
    protected int clientOpenGLESVersion = 0;
    protected EGLConfig bestConfig = null;
    protected EGLConfig fastestConfig = null;
    protected EGLConfig choosenConfig = null;
    protected ConfigType type;
    protected int pixelFormat;
    protected boolean verbose = false;
    private final static int EGL_OPENGL_ES2_BIT = 4;

    public enum ConfigType {

        /**
         * RGB565, 0 alpha, 16 depth, 0 stencil
         */
        FASTEST,
        /**
         * RGB???, 0 alpha, >=16 depth, 0 stencil
         */
        BEST,
        /**
         * Turn off config chooser and use hardcoded
         * setEGLContextClientVersion(2); setEGLConfigChooser(5, 6, 5, 0, 16,
         * 0);
         */
        LEGACY,
        /**
         * RGB???, 8 alpha, >=16 depth, 0 stencil
         */
        BEST_TRANSLUCENT,}

    public AndroidConfigChooser(ConfigType type) {
        this.type = type;
    }

    /**
     * Gets called by the GLSurfaceView class to return the best config
     */
    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        logger.fine("GLSurfaceView asks for egl config, returning: ");
        logEGLConfig(choosenConfig, display, egl);
        return choosenConfig;
    }

    /**
     * findConfig is used to locate the best config and init the chooser with
     *
     * @param egl
     * @param display
     * @return true if successfull, false if no config was found
     */
    public boolean findConfig(EGL10 egl, EGLDisplay display) {
        ComponentSizeChooser compChooser = null;
        switch (type) {
            case BEST:
                compChooser = new ComponentSizeChooser(8, 8, 8, 0, 32, 0);
                choosenConfig = compChooser.chooseConfig(egl, display);
                if (choosenConfig == null) {
                    compChooser = new ComponentSizeChooser(8, 8, 8, 0, 16, 0);
                    choosenConfig = compChooser.chooseConfig(egl, display);
                }
                logger.fine("JME3 using best EGL configuration available here: ");
                break;
            case BEST_TRANSLUCENT:
                compChooser = new ComponentSizeChooser(8, 8, 8, 8, 32, 0);
                choosenConfig = compChooser.chooseConfig(egl, display);
                if (choosenConfig == null) {
                    compChooser = new ComponentSizeChooser(8, 8, 8, 8, 16, 0);
                    choosenConfig = compChooser.chooseConfig(egl, display);
                }
                logger.fine("JME3 using best EGL configuration available here with translucent pixels: ");
                break;
            case FASTEST:
                compChooser = new ComponentSizeChooser(5, 6, 5, 0, 16, 0);
                choosenConfig = compChooser.chooseConfig(egl, display);
                logger.fine("JME3 using fastest EGL configuration available here: ");
                break;

        }

        if (choosenConfig != null) {
            logger.fine("JME3 using choosen config: ");
            logEGLConfig(choosenConfig, display, egl);
            pixelFormat = getPixelFormat(choosenConfig, display, egl);
            clientOpenGLESVersion = getOpenGLVersion(choosenConfig, display, egl);
            return true;
        } else {
            logger.severe("ERROR: Unable to get a valid OpenGL ES 2.0 config, neither Fastest nor Best found! Bug. Please report this.");
            clientOpenGLESVersion = 1;
            pixelFormat = PixelFormat.UNKNOWN;
            return false;
        }
    }

    private int getPixelFormat(EGLConfig conf, EGLDisplay display, EGL10 egl) {
        int[] value = new int[1];

        //Android Pixel format is not very well documented.
        //From what i gathered, the format is chosen automatically except for the alpha channel
        //if the alpha channel has 8 bit or more, e set the pixel format to Transluscent, as it allow transparent view background
        //if it's 0 bit, the format is OPAQUE otherwise it's TRANSPARENT
        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_ALPHA_SIZE, value);
        if (value[0] >= 8) {
            return PixelFormat.TRANSLUCENT;
        }
        if (value[0] >= 1) {
            return PixelFormat.TRANSPARENT;
        }

        return PixelFormat.OPAQUE;
    }

    private int getOpenGLVersion(EGLConfig conf, EGLDisplay display, EGL10 egl) {
        int[] value = new int[1];
        int result = 1;

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_RENDERABLE_TYPE, value);
        // Check if conf is OpenGL ES 2.0
        if ((value[0] & EGL_OPENGL_ES2_BIT) != 0) {
            result = 2;
        }

        return result;
    }

    /**
     * log output with egl config details
     *
     * @param conf
     * @param display
     * @param egl
     */
    public void logEGLConfig(EGLConfig conf, EGLDisplay display, EGL10 egl) {
        int[] value = new int[1];

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_RED_SIZE, value);
        logger.fine(String.format("EGL_RED_SIZE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_GREEN_SIZE, value);
        logger.fine(String.format("EGL_GREEN_SIZE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_BLUE_SIZE, value);
        logger.fine(String.format("EGL_BLUE_SIZE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_ALPHA_SIZE, value);
        logger.fine(String.format("EGL_ALPHA_SIZE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_DEPTH_SIZE, value);
        logger.fine(String.format("EGL_DEPTH_SIZE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_STENCIL_SIZE, value);
        logger.fine(String.format("EGL_STENCIL_SIZE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_RENDERABLE_TYPE, value);
        logger.fine(String.format("EGL_RENDERABLE_TYPE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_SURFACE_TYPE, value);
        logger.fine(String.format("EGL_SURFACE_TYPE  = %d", value[0]));
    }

    public int getClientOpenGLESVersion() {
        return clientOpenGLESVersion;
    }

    public void setClientOpenGLESVersion(int clientOpenGLESVersion) {
        this.clientOpenGLESVersion = clientOpenGLESVersion;
    }

    public int getPixelFormat() {
        return pixelFormat;
    }

    private abstract class BaseConfigChooser implements EGLConfigChooser {

        private boolean bClientOpenGLESVersionSet;

        public BaseConfigChooser(int[] configSpec) {
            bClientOpenGLESVersionSet = false;
            mConfigSpec = filterConfigSpec(configSpec);
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] num_config = new int[1];
            if (!egl.eglChooseConfig(display, mConfigSpec, null, 0,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig failed");
            }

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                //throw new IllegalArgumentException("No configs match configSpec");

                return null;
            }

            EGLConfig[] configs = new EGLConfig[numConfigs];
            if (!egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
//            logger.log(Level.FINE, "num_config: {0}", num_config[0]);
//
//            logger.log(Level.FINE, "There are {0} configurations that match the configAttrs", num_config[0]);
//            logger.log(Level.FINE, "All Matching Configs:");
//            for (int i=0; i<configs.length; i++) {
//                if (configs[i] != null) {
//                    logger.log(Level.FINE, "configs{0} is not null", i);
//                    logEGLConfig(configs[i], display, egl);
//                } else {
//                    logger.log(Level.FINE, "configs{0} is null", i);
//                }
//            }

            EGLConfig config = chooseConfig(egl, display, configs);
            //if (config == null) {
            //    throw new IllegalArgumentException("No config chosen");
            //}
            return config;
        }

        abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                EGLConfig[] configs);
        protected int[] mConfigSpec;

        private int[] filterConfigSpec(int[] configSpec) {
            if (bClientOpenGLESVersionSet == true) {
                return configSpec;
            }
            /*
             * We know none of the subclasses define EGL_RENDERABLE_TYPE. And we
             * know the configSpec is well formed.
             */
            int len = configSpec.length;
            int[] newConfigSpec = new int[len + 2];
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
            newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE;
            newConfigSpec[len] = 4; /*
             * EGL_OPENGL_ES2_BIT
             */
            newConfigSpec[len + 1] = EGL10.EGL_NONE;

            bClientOpenGLESVersionSet = true;

            return newConfigSpec;
        }
    }

    /**
     * Choose a configuration with exactly the specified r,g,b,a sizes, and at
     * least the specified depth and stencil sizes.
     */
    private class ComponentSizeChooser extends BaseConfigChooser {

        private int[] mValue;
        // Subclasses can adjust these values:
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;

        public ComponentSizeChooser(int redSize, int greenSize, int blueSize,
                int alphaSize, int depthSize, int stencilSize) {
            super(new int[]{
                        EGL10.EGL_RED_SIZE, redSize,
                        EGL10.EGL_GREEN_SIZE, greenSize,
                        EGL10.EGL_BLUE_SIZE, blueSize,
                        EGL10.EGL_ALPHA_SIZE, alphaSize,
                        EGL10.EGL_DEPTH_SIZE, depthSize,
                        EGL10.EGL_STENCIL_SIZE, stencilSize,
                        EGL10.EGL_NONE});
            mValue = new int[1];
            mRedSize = redSize;
            mGreenSize = greenSize;
            mBlueSize = blueSize;
            mAlphaSize = alphaSize;
            mDepthSize = depthSize;
            mStencilSize = stencilSize;
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {

            // first pass through config list.  Try to find an exact match.
            for (EGLConfig config : configs) {
                int r = findConfigAttrib(egl, display, config,
                        EGL10.EGL_RED_SIZE, 0);
                int g = findConfigAttrib(egl, display, config,
                        EGL10.EGL_GREEN_SIZE, 0);
                int b = findConfigAttrib(egl, display, config,
                        EGL10.EGL_BLUE_SIZE, 0);
                int a = findConfigAttrib(egl, display, config,
                        EGL10.EGL_ALPHA_SIZE, 0);
                int d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0);

                if ((
                            r == mRedSize) && (g == mGreenSize)
                        && (b == mBlueSize) && (a == mAlphaSize)
                        && (d == mDepthSize) && (s == mStencilSize)
                        ) {
                    return config;
                }
            }

            // second pass through config list.  Try to find an RGBA match.
            for (EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0);
                if ((d >= mDepthSize) && (s >= mStencilSize)) {
                    int r = findConfigAttrib(egl, display, config,
                            EGL10.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib(egl, display, config,
                            EGL10.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib(egl, display, config,
                            EGL10.EGL_BLUE_SIZE, 0);
                    int a = findConfigAttrib(egl, display, config,
                            EGL10.EGL_ALPHA_SIZE, 0);
                    if ((r == mRedSize) && (g == mGreenSize)
                            && (b == mBlueSize) && (a == mAlphaSize)) {
                        return config;
                    }
                }
            }

            // failsafe. pick the 1st config.
            if (configs.length > 0) {
                return configs[0];
            } else {
                return null;
            }

        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                EGLConfig config, int attribute, int defaultValue) {

            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0];
            }
            return defaultValue;
        }
    }
}
