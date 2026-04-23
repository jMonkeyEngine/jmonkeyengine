package com.jme3.scene.plugins.fbx;

import java.util.logging.Level;
import java.util.logging.Logger;

final class FbxDeprecationWarnings {

    private static volatile boolean logged;

    private FbxDeprecationWarnings() {
    }

    static void log(Logger logger) {
        if (!logged) {
            synchronized (FbxDeprecationWarnings.class) {
                if (!logged) {
                    logger.log(Level.WARNING,
                            "FBX support is deprecated and will be removed in a future release. "
                            + "Prefer glTF assets instead.");
                    logged = true;
                }
            }
        }
    }
}
