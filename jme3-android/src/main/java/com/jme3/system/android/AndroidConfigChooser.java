package com.jme3.system.android;

import android.opengl.GLSurfaceView;
import com.jme3.renderer.android.RendererUtil;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * AndroidConfigChooser is used to determine the best suited EGL Config.
 *
 * @author iwgeric
 */
public final class AndroidConfigChooser implements GLSurfaceView.EGLConfigChooser {

    private static final Logger logger = Logger.getLogger(AndroidConfigChooser.class.getName());

    private static final int EGL_OPENGL_ES3_BIT = 0x40;
    private static final int EGL_WINDOW_BIT = 0x0004;

    private static final int REJECTED = Integer.MIN_VALUE / 2;

    private final AppSettings settings;

    public AndroidConfigChooser(AppSettings settings) {
        this.settings = settings;
    }

    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        RequestedConfig requested = getRequestedConfig();
        EGLConfig[] configs = getConfigs(egl, display);
        EGLConfig chosenConfig = chooseBestConfig(egl, display, configs, requested);

        if (chosenConfig == null && requested.samples > 0) {
            logger.log(Level.INFO, "EGL configuration not found with requested samples, disabling MSAA");
            requested = requested.withSamples(0);
            chosenConfig = chooseBestConfig(egl, display, configs, requested);
        }

        if (chosenConfig == null && requested.alpha > 0) {
            logger.log(Level.INFO, "EGL configuration not found with requested alpha, allowing opaque config");
            requested = requested.withAlpha(0);
            chosenConfig = chooseBestConfig(egl, display, configs, requested);
        }

        if (chosenConfig == null && requested.depth > 16) {
            logger.log(Level.INFO, "EGL configuration not found with requested depth, reducing depth to 16");
            requested = requested.withDepth(16);
            chosenConfig = chooseBestConfig(egl, display, configs, requested);
        }

        if (chosenConfig == null) {
            logger.log(Level.INFO, "EGL configuration not found, using minimal GLES3 window config");
            requested = new RequestedConfig(0, 0, 0, 0, 16, 0, 0, false);
            chosenConfig = chooseBestConfig(egl, display, configs, requested);
        }

        if (chosenConfig == null) {
            throw new IllegalStateException("No suitable GLES3 EGLConfig found");
        }

        storeSelectedConfig(egl, display, chosenConfig);
        if (logger.isLoggable(Level.INFO)) {
            logEGLConfig(chosenConfig, display, egl, Level.INFO);
        }
        return chosenConfig;
    }

    private RequestedConfig getRequestedConfig() {
        int bitsPerPixel = settings.getBitsPerPixel();
        boolean gamma = settings.isGammaCorrection();
        int red;
        int green;
        int blue;

        if (gamma || bitsPerPixel >= 24) {
            red = 8;
            green = 8;
            blue = 8;
            if (bitsPerPixel < 24) {
                settings.setBitsPerPixel(24);
            }
        } else {
            red = 5;
            green = 6;
            blue = 5;
            if (bitsPerPixel != 16) {
                logger.log(Level.INFO, "Invalid bitsPerPixel setting: {0}, using RGB565", bitsPerPixel);
                settings.setBitsPerPixel(16);
            }
        }

        return new RequestedConfig(
                red,
                green,
                blue,
                Math.max(0, settings.getAlphaBits()),
                Math.max(0, settings.getDepthBits()),
                Math.max(0, settings.getStencilBits()),
                Math.max(0, settings.getSamples()),
                gamma);
    }

    private EGLConfig[] getConfigs(EGL10 egl, EGLDisplay display) {
        int[] numConfig = new int[1];
        int[] configSpec = {
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
                EGL10.EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
                EGL10.EGL_NONE
        };

        if (!egl.eglChooseConfig(display, configSpec, null, 0, numConfig)) {
            RendererUtil.checkEGLError(egl);
            throw new AssertionError("Unable to query GLES3 EGL configs");
        }

        int numConfigs = numConfig[0];
        if (numConfigs == 0) {
            throw new IllegalStateException("No GLES3 window EGL configs found");
        }

        EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!egl.eglChooseConfig(display, configSpec, configs, numConfigs, numConfig)) {
            RendererUtil.checkEGLError(egl);
            throw new AssertionError("Unable to enumerate GLES3 EGL configs");
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("--------------Display Configurations---------------");
            for (EGLConfig config : configs) {
                logEGLConfig(config, display, egl, Level.FINE);
                logger.fine("----------------------------------------");
            }
        }
        return configs;
    }

    private EGLConfig chooseBestConfig(EGL10 egl, EGLDisplay display,
                                       EGLConfig[] configs, RequestedConfig requested) {
        EGLConfig bestConfig = null;
        int bestScore = REJECTED;

        for (EGLConfig config : configs) {
            int red = getAttrib(egl, display, config, EGL10.EGL_RED_SIZE);
            int green = getAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE);
            int blue = getAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE);
            int alpha = getAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE);
            int depth = getAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE);
            int stencil = getAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE);
            int sampleBuffers = getAttrib(egl, display, config, EGL10.EGL_SAMPLE_BUFFERS);
            int samples = getAttrib(egl, display, config, EGL10.EGL_SAMPLES);

            int score = scoreConfig(requested, red, green, blue, alpha, depth,
                    stencil, sampleBuffers, samples);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                        "Checking EGLConfig R{0} G{1} B{2} A{3} D{4} S{5} MSAA[{6},{7}] score {8}",
                        new Object[]{red, green, blue, alpha, depth, stencil, sampleBuffers, samples, score});
            }

            if (score > bestScore) {
                bestScore = score;
                bestConfig = config;
            }
        }

        return bestScore == REJECTED ? null : bestConfig;
    }

    private int scoreConfig(RequestedConfig requested, int red, int green, int blue,
                            int alpha, int depth, int stencil, int sampleBuffers, int samples) {
        if (requested.gamma && (red < 8 || green < 8 || blue < 8)) {
            return REJECTED;
        }
        if (red < requested.red || green < requested.green || blue < requested.blue) {
            return REJECTED;
        }
        if (alpha < requested.alpha || depth < requested.depth || stencil < requested.stencil) {
            return REJECTED;
        }
        if (requested.samples > 0 && (sampleBuffers == 0 || samples < requested.samples)) {
            return REJECTED;
        }

        int score = 0;
        score += closenessScore(red, requested.red, 8);
        score += closenessScore(green, requested.green, 8);
        score += closenessScore(blue, requested.blue, 8);
        score += requested.alpha > 0 ? closenessScore(alpha, requested.alpha, 8) : opaqueBonus(alpha);
        score += closenessScore(depth, requested.depth, 32);
        score += closenessScore(stencil, requested.stencil, 8);

        if (requested.samples > 0) {
            score += 100 + closenessScore(samples, requested.samples, 16);
        } else {
            score += samples == 0 ? 10 : -samples;
        }

        return score;
    }

    private int closenessScore(int actual, int requested, int maxUseful) {
        if (requested <= 0) {
            return 0;
        }
        return maxUseful - Math.min(maxUseful, actual - requested);
    }

    private int opaqueBonus(int alpha) {
        return alpha == 0 ? 10 : -Math.min(alpha, 8);
    }

    private int getAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute) {
        int[] value = new int[1];
        if (!egl.eglGetConfigAttrib(display, config, attribute, value)) {
            RendererUtil.checkEGLError(egl);
            throw new AssertionError("Unable to query EGL attribute " + attribute);
        }
        return value[0];
    }

    private void storeSelectedConfig(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
        int red = getAttrib(egl, display, eglConfig, EGL10.EGL_RED_SIZE);
        int green = getAttrib(egl, display, eglConfig, EGL10.EGL_GREEN_SIZE);
        int blue = getAttrib(egl, display, eglConfig, EGL10.EGL_BLUE_SIZE);
        int samples = getAttrib(egl, display, eglConfig, EGL10.EGL_SAMPLE_BUFFERS) > 0
                ? getAttrib(egl, display, eglConfig, EGL10.EGL_SAMPLES)
                : 0;

        settings.setBitsPerPixel(red + green + blue);
        settings.setAlphaBits(getAttrib(egl, display, eglConfig, EGL10.EGL_ALPHA_SIZE));
        settings.setDepthBits(getAttrib(egl, display, eglConfig, EGL10.EGL_DEPTH_SIZE));
        settings.setStencilBits(getAttrib(egl, display, eglConfig, EGL10.EGL_STENCIL_SIZE));
        settings.setSamples(samples);
    }

    private void logEGLConfig(EGLConfig config, EGLDisplay display, EGL10 egl, Level level) {
        logger.log(level,
                "EGLConfig chosen: R{0} G{1} B{2} A{3} D{4} S{5} MSAA[{6} buffers, {7} samples]",
                new Object[]{
                        getAttrib(egl, display, config, EGL10.EGL_RED_SIZE),
                        getAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE),
                        getAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE),
                        getAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE),
                        getAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE),
                        getAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE),
                        getAttrib(egl, display, config, EGL10.EGL_SAMPLE_BUFFERS),
                        getAttrib(egl, display, config, EGL10.EGL_SAMPLES)
                });
    }

    private static final class RequestedConfig {
        private final int red;
        private final int green;
        private final int blue;
        private final int alpha;
        private final int depth;
        private final int stencil;
        private final int samples;
        private final boolean gamma;

        private RequestedConfig(int red, int green, int blue, int alpha,
                                int depth, int stencil, int samples, boolean gamma) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            this.depth = depth;
            this.stencil = stencil;
            this.samples = samples;
            this.gamma = gamma;
        }

        private RequestedConfig withSamples(int samples) {
            return new RequestedConfig(red, green, blue, alpha, depth, stencil, samples, gamma);
        }

        private RequestedConfig withAlpha(int alpha) {
            return new RequestedConfig(red, green, blue, alpha, depth, stencil, samples, gamma);
        }

        private RequestedConfig withDepth(int depth) {
            return new RequestedConfig(red, green, blue, alpha, depth, stencil, samples, gamma);
        }
    }
}
