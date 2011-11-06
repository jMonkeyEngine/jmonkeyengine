package com.jme3.asset;

/**
 * <code>AssetNotFoundException</code> is thrown when the {@link AssetManager}
 * is unable to locate the requested asset using any of the registered
 * {@link AssetLocator}s.
 *
 * @author Kirill Vainer
 */
public class AssetNotFoundException extends RuntimeException {
    public AssetNotFoundException(String message){
        super(message);
    }
    public AssetNotFoundException(String message, Exception ex){
        super(message, ex);
    }
}
