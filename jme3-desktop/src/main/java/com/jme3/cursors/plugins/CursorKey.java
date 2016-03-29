package com.jme3.cursors.plugins;

import com.jme3.asset.AssetKey;

/**
 * The implementation of asset key for loading cursor.
 *
 * @author JavaSaBr
 */
public class CursorKey extends AssetKey<JmeCursor> {

    /**
     * Need to flip the image.
     */
    private boolean flipY;

    public CursorKey(final String name, final boolean flipY) {
        super(name);
        this.flipY = flipY;
    }

    public CursorKey(final String name) {
        super(name);
        this.flipY = true;
    }

    public CursorKey() {
        this.flipY = true;
    }

    /**
     * @param flipY need to flip the image. Default true.
     */
    public void setFlipY(boolean flipY) {
        this.flipY = flipY;
    }

    /**
     * @return need to flip the image. Default true.
     */
    public boolean isFlipY() {
        return flipY;
    }
}
