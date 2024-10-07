package com.jme3.system.android;

import android.opengl.GLSurfaceView.EGLConfigChooser;
import com.jme3.renderer.android.RendererUtil;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * AndroidConfigChooser is used to determine the best suited EGL Config
 *
 * @author iwgeric
 */
public class AndroidConfigChooser implements EGLConfigChooser {

    private static final Logger logger = Logger.getLogger(AndroidConfigChooser.class.getName());
    protected AppSettings settings;
    private final static int EGL_OPENGL_ES2_BIT = 4;
    private final static int EGL_OPENGL_ES3_BIT = 0x40;

    public AndroidConfigChooser(AppSettings settings) {
        this.settings = settings;
    }

    /**
     * Gets called by the GLSurfaceView class to return the best config
     */
    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        logger.fine("GLSurfaceView asking for egl config");
        Config requestedConfig = getRequestedConfig();
        EGLConfig[] configs = getConfigs(egl, display);

        // First try to find an exact match, but allowing a higher stencil
        EGLConfig chosenConfig = chooseConfig(egl, display, configs, requestedConfig, false, false, false, true);
        if (chosenConfig == null && requestedConfig.d > 16) {
            logger.log(Level.INFO, "EGL configuration not found, reducing depth");
            requestedConfig.d = 16;
            chosenConfig = chooseConfig(egl, display, configs, requestedConfig, false, false, false, true);
        }

        if (chosenConfig == null) {
            logger.log(Level.INFO, "EGL configuration not found, allowing higher RGB");
            chosenConfig = chooseConfig(egl, display, configs, requestedConfig, true, false, false, true);
        }

        if (chosenConfig == null && requestedConfig.a > 0) {
            logger.log(Level.INFO, "EGL configuration not found, allowing higher alpha");
            chosenConfig = chooseConfig(egl, display, configs, requestedConfig, true, true, false, true);
        }

        if (chosenConfig == null && requestedConfig.s > 0) {
            logger.log(Level.INFO, "EGL configuration not found, allowing higher samples");
            chosenConfig = chooseConfig(egl, display, configs, requestedConfig, true, true, true, true);
        }

        if (chosenConfig == null && requestedConfig.a > 0) {
            logger.log(Level.INFO, "EGL configuration not found, reducing alpha");
            requestedConfig.a = 1;
            chosenConfig = chooseConfig(egl, display, configs, requestedConfig, true, true, false, true);
        }

        if (chosenConfig == null && requestedConfig.s > 0) {
            logger.log(Level.INFO, "EGL configuration not found, reducing samples");
            requestedConfig.s = 1;
            if (requestedConfig.a > 0) {
                chosenConfig = chooseConfig(egl, display, configs, requestedConfig, true, true, true, true);
            } else {
                chosenConfig = chooseConfig(egl, display, configs, requestedConfig, true, false, true, true);
            }
        }

        if (chosenConfig == null && requestedConfig.getBitsPerPixel() > 16) {
            logger.log(Level.INFO, "EGL configuration not found, setting to RGB565");
            requestedConfig.r = 5;
            requestedConfig.g = 6;
            requestedConfig.b = 5;
            chosenConfig = chooseConfig(egl, display, configs, requestedConfig, true, false, false, true);

            if (chosenConfig == null) {
                logger.log(Level.INFO, "EGL configuration not found, allowing higher alpha");
                chosenConfig = chooseConfig(egl, display, configs, requestedConfig, true, true, false, true);
            }
        }

        if (chosenConfig == null) {
            logger.log(Level.INFO, "EGL configuration not found, looking for best config with >= 16 bit Depth");
            // failsafe: pick the best config with depth >= 16
            requestedConfig = new Config(0, 0, 0, 0, 16, 0, 0);
            chosenConfig = chooseConfig(egl, display, configs, requestedConfig, true, false, false, true);
        }

        if (chosenConfig != null) {
            logger.fine("GLSurfaceView asks for egl config, returning: ");
            logEGLConfig(chosenConfig, display, egl, Level.FINE);

            storeSelectedConfig(egl, display, chosenConfig);
            return chosenConfig;
        } else {
            logger.severe("No EGL Config found");
            return null;
        }
    }

    private Config getRequestedConfig() {
        int r, g, b;
        if (settings.getBitsPerPixel() == 24) {
            r = g = b = 8;
        } else {
            if (settings.getBitsPerPixel() != 16) {
                logger.log(Level.SEVERE, "Invalid bitsPerPixel setting: {0}, setting to RGB565 (16)", settings.getBitsPerPixel());
                settings.setBitsPerPixel(16);
            }
            r = 5;
            g = 6;
            b = 5;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Requested Display Config:");
            logger.log(Level.FINE, "RGB: {0}, alpha: {1}, depth: {2}, samples: {3}, stencil: {4}",
                    new Object[]{settings.getBitsPerPixel(),
                            settings.getAlphaBits(), settings.getDepthBits(),
                            settings.getSamples(), settings.getStencilBits()});
        }

        return new Config(
                r, g, b,
                settings.getAlphaBits(),
                settings.getDepthBits(),
                settings.getSamples(),
                settings.getStencilBits());
    }

    /**
     * Query egl for the available configs
     * @param egl
     * @param display
     * @return
     */
    private EGLConfig[] getConfigs(EGL10 egl, EGLDisplay display) {

        int[] num_config = new int[1];
        int[] configSpec = new int[]{
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
            EGL10.EGL_NONE};
        boolean gles3=true;

        // Try openGL ES 3
        try {
            if (!egl.eglChooseConfig(display, configSpec, null, 0, num_config)) {
                RendererUtil.checkEGLError(egl);
                gles3=false;
            }
        } catch (com.jme3.renderer.RendererException re) { 
            // it's just the device not supporting GLES3. Fallback to GLES2
            gles3=false;
        } 

        if(!gles3)
        {
            // Get back to openGL ES 2
            configSpec[1]=EGL_OPENGL_ES2_BIT;
            if (!egl.eglChooseConfig(display, configSpec, null, 0, num_config)) {
                RendererUtil.checkEGLError(egl);
                throw new AssertionError();
            }
        }

        int numConfigs = num_config[0];
        EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!egl.eglChooseConfig(display, configSpec, configs, numConfigs, num_config)) {
            RendererUtil.checkEGLError(egl);
            throw new AssertionError();
        }

        logger.fine("--------------Display Configurations---------------");
        for (EGLConfig eGLConfig : configs) {
            logEGLConfig(eGLConfig, display, egl, Level.FINE);
            logger.fine("----------------------------------------");
        }

        return configs;
    }

    private EGLConfig chooseConfig(
            EGL10 egl, EGLDisplay display, EGLConfig[] configs, Config requestedConfig,
            boolean higherRGB, boolean higherAlpha,
            boolean higherSamples, boolean higherStencil) {

        EGLConfig keptConfig = null;
        int kr = 0;
        int kg = 0;
        int kb = 0;
        int ka = 0;
        int kd = 0;
        int ks = 0;
        int kst = 0;


        // first pass through config list.  Try to find an exact match.
        for (EGLConfig config : configs) {
            int r = eglGetConfigAttribSafe(egl, display, config,
                    EGL10.EGL_RED_SIZE);
            int g = eglGetConfigAttribSafe(egl, display, config,
                    EGL10.EGL_GREEN_SIZE);
            int b = eglGetConfigAttribSafe(egl, display, config,
                    EGL10.EGL_BLUE_SIZE);
            int a = eglGetConfigAttribSafe(egl, display, config,
                    EGL10.EGL_ALPHA_SIZE);
            int d = eglGetConfigAttribSafe(egl, display, config,
                    EGL10.EGL_DEPTH_SIZE);
            int s = eglGetConfigAttribSafe(egl, display, config,
                    EGL10.EGL_SAMPLES);
            int st = eglGetConfigAttribSafe(egl, display, config,
                    EGL10.EGL_STENCIL_SIZE);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Checking Config r: {0}, g: {1}, b: {2}, alpha: {3}, depth: {4}, samples: {5}, stencil: {6}",
                        new Object[]{r, g, b, a, d, s, st});
            }

            if (higherRGB && r < requestedConfig.r) { continue; }
            if (!higherRGB && r != requestedConfig.r) { continue; }

            if (higherRGB && g < requestedConfig.g) { continue; }
            if (!higherRGB && g != requestedConfig.g) { continue; }

            if (higherRGB && b < requestedConfig.b) { continue; }
            if (!higherRGB && b != requestedConfig.b) { continue; }

            if (higherAlpha && a < requestedConfig.a) { continue; }
            if (!higherAlpha && a != requestedConfig.a) { continue; }

            if (d < requestedConfig.d) { continue; } // always allow higher depth

            if (higherSamples && s < requestedConfig.s) { continue; }
            if (!higherSamples && s != requestedConfig.s) { continue; }

            if (higherStencil && st < requestedConfig.st) { continue; }
            if (!higherStencil && !inRange(st, 0, requestedConfig.st)) { continue; }

            //we keep the config if it is better
            if (    r >= kr || g >= kg || b >= kb || a >= ka ||
                    d >= kd || s >= ks || st >= kst ) {
                kr = r; kg = g; kb = b; ka = a;
                kd = d; ks = s; kst = st;
                keptConfig = config;
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Keeping Config r: {0}, g: {1}, b: {2}, alpha: {3}, depth: {4}, samples: {5}, stencil: {6}",
                            new Object[]{r, g, b, a, d, s, st});
                }
            }

        }

        if (keptConfig != null) {
            return keptConfig;
        }

        //no match found
        logger.log(Level.SEVERE, "No egl config match found");
        return null;
    }

    private static int eglGetConfigAttribSafe(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute) {
        int[] value = new int[1];
        if (!egl.eglGetConfigAttrib(display, config, attribute, value)) {
            RendererUtil.checkEGLError(egl);
            throw new AssertionError();
        }
        return value[0];
    }

    private void storeSelectedConfig(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
        int r = eglGetConfigAttribSafe(egl, display, eglConfig, EGL10.EGL_RED_SIZE);
        int g = eglGetConfigAttribSafe(egl, display, eglConfig, EGL10.EGL_GREEN_SIZE);
        int b = eglGetConfigAttribSafe(egl, display, eglConfig, EGL10.EGL_BLUE_SIZE);
        settings.setBitsPerPixel(r+g+b);

        settings.setAlphaBits(
                eglGetConfigAttribSafe(egl, display, eglConfig, EGL10.EGL_ALPHA_SIZE));
        settings.setDepthBits(
                eglGetConfigAttribSafe(egl, display, eglConfig, EGL10.EGL_DEPTH_SIZE));
        settings.setSamples(
                eglGetConfigAttribSafe(egl, display, eglConfig, EGL10.EGL_SAMPLES));
        settings.setStencilBits(
                eglGetConfigAttribSafe(egl, display, eglConfig, EGL10.EGL_STENCIL_SIZE));
    }

    /**
     * log output with egl config details
     *
     * @param conf
     * @param display
     * @param egl
     */
    private void logEGLConfig(EGLConfig conf, EGLDisplay display, EGL10 egl, Level level) {

        logger.log(level, "EGL_RED_SIZE = {0}",
                eglGetConfigAttribSafe(egl, display, conf, EGL10.EGL_RED_SIZE));

        logger.log(level, "EGL_GREEN_SIZE = {0}",
                eglGetConfigAttribSafe(egl, display, conf, EGL10.EGL_GREEN_SIZE));

        logger.log(level, "EGL_BLUE_SIZE = {0}",
                eglGetConfigAttribSafe(egl, display, conf, EGL10.EGL_BLUE_SIZE));

        logger.log(level, "EGL_ALPHA_SIZE = {0}",
                eglGetConfigAttribSafe(egl, display, conf, EGL10.EGL_ALPHA_SIZE));

        logger.log(level, "EGL_DEPTH_SIZE = {0}",
                eglGetConfigAttribSafe(egl, display, conf, EGL10.EGL_DEPTH_SIZE));

        logger.log(level, "EGL_STENCIL_SIZE = {0}",
                eglGetConfigAttribSafe(egl, display, conf, EGL10.EGL_STENCIL_SIZE));

        logger.log(level, "EGL_RENDERABLE_TYPE = {0}",
                eglGetConfigAttribSafe(egl, display, conf, EGL10.EGL_RENDERABLE_TYPE));

        logger.log(level, "EGL_SURFACE_TYPE = {0}",
                eglGetConfigAttribSafe(egl, display, conf, EGL10.EGL_SURFACE_TYPE));

        logger.log(level, "EGL_SAMPLE_BUFFERS = {0}",
                eglGetConfigAttribSafe(egl, display, conf, EGL10.EGL_SAMPLE_BUFFERS));

        logger.log(level, "EGL_SAMPLES = {0}",
                eglGetConfigAttribSafe(egl, display, conf, EGL10.EGL_SAMPLES));
    }

    private boolean inRange(int val, int min, int max) {
        return min <= val && val <= max;
    }

    private class Config {
        /**
         * red, green, blue, alpha, depth, samples, stencil
         */
        int r, g, b, a, d, s, st;

        private Config(int r, int g, int b, int a, int d, int s, int st) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.d = d;
            this.s = s;
            this.st = st;
        }

        private int getBitsPerPixel() {
            return r+g+b;
        }
    }

//DON'T REMOVE THIS, USED FOR UNIT TESTING FAILING CONFIGURATION LISTS.
//    private static class Config {
//
//        int r, g, b, a, d, s, ms, ns;
//
//        public Config(int r, int g, int b, int a, int d, int s, int ms, int ns) {
//            this.r = r;
//            this.g = g;
//            this.b = b;
//            this.a = a;
//            this.d = d;
//            this.s = s;
//            this.ms = ms;
//            this.ns = ns;
//        }
//
//        @Override
//        public String toString() {
//            return "Config{" + "r=" + r + ", g=" + g + ", b=" + b + ", a=" + a + ", d=" + d + ", s=" + s + ", ms=" + ms + ", ns=" + ns + '}';
//        }
//    }
//
//    public static Config chooseConfig(List<Config> configs, ConfigType configType, int mSamples) {
//
//        Config keptConfig = null;
//        int kd = 0;
//        int knbMs = 0;
//
//
//        // first pass through config list.  Try to find an exact match.
//        for (Config config : configs) {
////                logEGLConfig(config, display, egl);
//            int r = config.r;
//            int g = config.g;
//            int b = config.b;
//            int a = config.a;
//            int d = config.d;
//            int s = config.s;
//            int isMs = config.ms;
//            int nbMs = config.ns;
//
//            if (inRange(r, configType.mr, configType.r)
//                    && inRange(g, configType.mg, configType.g)
//                    && inRange(b, configType.mb, configType.b)
//                    && inRange(a, configType.ma, configType.a)
//                    && inRange(d, configType.md, configType.d)
//                    && inRange(s, configType.ms, configType.s)) {
//                if (mSamples == 0 && isMs != 0) {
//                    continue;
//                }
//                boolean keep = false;
//                //we keep the config if the depth is better or if the AA setting is better
//                if (d >= kd) {
//                    kd = d;
//                    keep = true;
//                } else {
//                    keep = false;
//                }
//
//                if (mSamples != 0) {
//                    if (nbMs >= knbMs && nbMs <= mSamples) {
//                        knbMs = nbMs;
//                        keep = true;
//                    } else {
//                        keep = false;
//                    }
//                }
//
//                if (keep) {
//                    keptConfig = config;
//                }
//            }
//        }
//
//        if (keptConfig != null) {
//            return keptConfig;
//        }
//
//        if (configType == ConfigType.BEST) {
//            keptConfig = chooseConfig(configs, ConfigType.BEST_TRANSLUCENT, mSamples);
//
//            if (keptConfig != null) {
//                return keptConfig;
//            }
//        }
//
//        if (configType == ConfigType.BEST_TRANSLUCENT) {
//            keptConfig = chooseConfig(configs, ConfigType.FASTEST, mSamples);
//
//            if (keptConfig != null) {
//                return keptConfig;
//            }
//        }
//        // failsafe. pick the 1st config.
//
//        for (Config config : configs) {
//            if (config.d >= 16) {
//                return config;
//            }
//        }
//
//        return null;
//    }
//
//    private static boolean inRange(int val, int min, int max) {
//        return min <= val && val <= max;
//    }
//
//    public static void main(String... argv) {
//        List<Config> confs = new ArrayList<Config>();
//        confs.add(new Config(5, 6, 5, 0, 0, 0, 0, 0));
//        confs.add(new Config(5, 6, 5, 0, 16, 0, 0, 0));
//        confs.add(new Config(5, 6, 5, 0, 24, 8, 0, 0));
//        confs.add(new Config(8, 8, 8, 8, 0, 0, 0, 0));
////            confs.add(new Config(8, 8, 8, 8, 16, 0, 0, 0));
////            confs.add(new Config(8, 8, 8, 8, 24, 8, 0, 0));
//
//        confs.add(new Config(5, 6, 5, 0, 0, 0, 1, 2));
//        confs.add(new Config(5, 6, 5, 0, 16, 0, 1, 2));
//        confs.add(new Config(5, 6, 5, 0, 24, 8, 1, 2));
//        confs.add(new Config(8, 8, 8, 8, 0, 0, 1, 2));
////            confs.add(new Config(8, 8, 8, 8, 16, 0, 1, 2));
////            confs.add(new Config(8, 8, 8, 8, 24, 8, 1, 2));
//
//        confs.add(new Config(5, 6, 5, 0, 0, 0, 1, 4));
//        confs.add(new Config(5, 6, 5, 0, 16, 0, 1, 4));
//        confs.add(new Config(5, 6, 5, 0, 24, 8, 1, 4));
//        confs.add(new Config(8, 8, 8, 8, 0, 0, 1, 4));
////            confs.add(new Config(8, 8, 8, 8, 16, 0, 1, 4));
////            confs.add(new Config(8, 8, 8, 8, 24, 8, 1, 4));
//
//        Config chosen = chooseConfig(confs, ConfigType.BEST, 0);
//
//        System.err.println(chosen);
//
//    }
}
