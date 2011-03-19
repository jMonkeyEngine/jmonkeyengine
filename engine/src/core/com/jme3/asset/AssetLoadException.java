package com.jme3.asset;

/**
 * <code>AssetLoadException</code> is thrown when the {@link AssetManager}
 * is able to find the requested asset, but there was a problem while loading
 * it.
 *
 * @author Kirill Vainer
 */
public class AssetLoadException extends RuntimeException {
    public AssetLoadException(String message){
        super(message);
    }
    public AssetLoadException(String message, Throwable cause){
        super(message, cause);
    }
}
