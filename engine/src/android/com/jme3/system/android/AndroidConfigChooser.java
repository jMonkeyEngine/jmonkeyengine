package com.jme3.system.android;

import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
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
    public final static String SETTINGS_CONFIG_TYPE = "configType";
    protected int clientOpenGLESVersion = 0;
    protected EGLConfig bestConfig = null;
    protected EGLConfig fastestConfig = null;
    protected EGLConfig choosenConfig = null;
    protected AppSettings settings;
    protected int pixelFormat;
    protected boolean verbose = false;
    private final static int EGL_OPENGL_ES2_BIT = 4;

    public enum ConfigType {

        /**
         * RGB565, 0 alpha, 16 depth, 0 stencil
         */
        FASTEST(5, 6, 5, 0, 16, 0, 5, 6, 5, 0, 16, 0),
        /**
         * min RGB888, 0 alpha, 16 depth, 0 stencil max RGB888, 0 alpha, 32
         * depth, 8 stencil
         */
        BEST(8, 8, 8, 0, 32, 8, 8, 8, 8, 0, 16, 0),
        /**
         * Turn off config chooser and use hardcoded
         * setEGLContextClientVersion(2); setEGLConfigChooser(5, 6, 5, 0, 16,
         * 0);
         */
        LEGACY(5, 6, 5, 0, 16, 0, 5, 6, 5, 0, 16, 0),
        /**
         * min RGB888, 8 alpha, 16 depth, 0 stencil max RGB888, 8 alpha, 32
         * depth, 8 stencil
         */
        BEST_TRANSLUCENT(8, 8, 8, 8, 32, 8, 8, 8, 8, 8, 16, 0);
        /**
         * red, green, blue, alpha, depth, stencil (max values)
         */
        int r, g, b, a, d, s;
        /**
         * minimal values
         */
        int mr, mg, mb, ma, md, ms;

        private ConfigType(int r, int g, int b, int a, int d, int s, int fbr, int fbg, int fbb, int fba, int fbd, int fbs) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.d = d;
            this.s = s;
            this.mr = fbr;
            this.mg = fbg;
            this.mb = fbb;
            this.ma = fba;
            this.md = fbd;
            this.ms = fbs;
        }
    }

    /**
     *
     * @param type
     * @deprecated use AndroidConfigChooser(AppSettings settings)
     */
    @Deprecated
    public AndroidConfigChooser(ConfigType type) {
        this.settings = new AppSettings(true);
        settings.put(SETTINGS_CONFIG_TYPE, type);
    }

    public AndroidConfigChooser(AppSettings settings) {
        this.settings = settings;
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
        ConfigType type = (ConfigType) settings.get(SETTINGS_CONFIG_TYPE);

        ComponentSizeChooser compChooser = new ComponentSizeChooser(type, settings.getSamples());
        choosenConfig = compChooser.chooseConfig(egl, display);
        logger.log(Level.FINE, "JME3 using {0} EGL configuration available here: ", type.name());

        if (choosenConfig != null) {
            logger.info("JME3 using choosen config: ");
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
        logger.info(String.format("EGL_RED_SIZE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_GREEN_SIZE, value);
        logger.info(String.format("EGL_GREEN_SIZE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_BLUE_SIZE, value);
        logger.info(String.format("EGL_BLUE_SIZE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_ALPHA_SIZE, value);
        logger.info(String.format("EGL_ALPHA_SIZE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_DEPTH_SIZE, value);
        logger.info(String.format("EGL_DEPTH_SIZE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_STENCIL_SIZE, value);
        logger.info(String.format("EGL_STENCIL_SIZE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_RENDERABLE_TYPE, value);
        logger.info(String.format("EGL_RENDERABLE_TYPE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_SURFACE_TYPE, value);
        logger.info(String.format("EGL_SURFACE_TYPE  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_SAMPLE_BUFFERS, value);
        logger.info(String.format("EGL_SAMPLE_BUFFERS  = %d", value[0]));

        egl.eglGetConfigAttrib(display, conf, EGL10.EGL_SAMPLES, value);
        logger.info(String.format("EGL_SAMPLES  = %d", value[0]));
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

        public BaseConfigChooser() {
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {

            int[] num_config = new int[1];
            int[] configSpec = new int[]{
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL10.EGL_NONE};

            egl.eglChooseConfig(display, configSpec, null, 0, num_config);

            int numConfigs = num_config[0];
            EGLConfig[] configs = new EGLConfig[numConfigs];
            egl.eglChooseConfig(display, configSpec, configs, numConfigs, num_config);

//            System.err.println("-----------------------------");
//            for (EGLConfig eGLConfig : configs) {
//                logEGLConfig(eGLConfig, display, egl);
//            }

            EGLConfig config = chooseConfig(egl, display, configs);
            return config;
        }

        abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                EGLConfig[] configs);
    }

    /**
     * Choose a configuration with exactly the specified r,g,b,a sizes, and at
     * least the specified depth and stencil sizes.
     */
    private class ComponentSizeChooser extends BaseConfigChooser {

        private int[] mValue;
        private ConfigType configType;
        protected int mSamples;

//        public ComponentSizeChooser(int redSize, int greenSize, int blueSize,
//                int alphaSize, int depthSize, int stencilSize, int samples) {
//            super(new int[]{
//                        EGL10.EGL_RED_SIZE, redSize,
//                        EGL10.EGL_GREEN_SIZE, greenSize,
//                        EGL10.EGL_BLUE_SIZE, blueSize,
//                        EGL10.EGL_ALPHA_SIZE, alphaSize,
//                        EGL10.EGL_DEPTH_SIZE, depthSize,
//                        EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
//                        EGL10.EGL_SAMPLE_BUFFERS, TRUE,
//                        EGL10.EGL_SAMPLES, samples,
//                        EGL10.EGL_NONE});
//            mValue = new int[1];
//            mRedSize = redSize;
//            mGreenSize = greenSize;
//            mBlueSize = blueSize;
//            mAlphaSize = alphaSize;
//            mDepthSize = depthSize;
//            mStencilSize = stencilSize;
//            mSamples = samples;
//        }
        public ComponentSizeChooser(ConfigType configType, int samples) {
            mValue = new int[1];
            mSamples = samples;
            this.configType = configType;
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {

            EGLConfig keptConfig = null;
            int kd = 0;
            int knbMs = 0;


            // first pass through config list.  Try to find an exact match.
            for (EGLConfig config : configs) {
//                logEGLConfig(config, display, egl);
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
                int isMs = findConfigAttrib(egl, display, config,
                        EGL10.EGL_SAMPLE_BUFFERS, 0);
                int nbMs = findConfigAttrib(egl, display, config,
                        EGL10.EGL_SAMPLES, 0);

                if (inRange(r, configType.mr, configType.r)
                        && inRange(g, configType.mg, configType.g)
                        && inRange(b, configType.mb, configType.b)
                        && inRange(a, configType.ma, configType.a)
                        && inRange(d, configType.md, configType.d)
                        && inRange(s, configType.ms, configType.s)) {
                    if (mSamples == 0 && isMs != 0) {
                        continue;
                    }
                    boolean keep = false;
                    //we keep the config if the depth is better or if the AA setting is better
                    if (d >= kd) {
                        kd = d;
                        keep = true;
                    } else {
                        keep = false;
                    }

                    if (mSamples != 0) {
                        if (nbMs >= knbMs && nbMs <= mSamples) {
                            knbMs = nbMs;
                            keep = true;
                        } else {
                            keep = false;
                        }
                    }
                    
                    if (keep) {
                        keptConfig = config;
                    }
                }
            }

            if (keptConfig != null) {
                return keptConfig;
            }


            // failsafe. pick the 1st config.
            if (configs.length > 0) {
                return configs[0];
            } else {
                return null;
            }

        }

        private boolean inRange(int val, int min, int max) {
            return min <= val && val <= max;
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
