/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author codex
 * @param <T>
 */
public interface RenderParameter <T> {
    
    /**
     * When returned as the parameter name, other classes will be unable to access this parameter.
     */
    public static final String PRIVATE = null;
    
    /**
     * Returns the name this parameter is identified by.
     * 
     * @return 
     */
    public String getParameterName();
    
    /**
     * Applies the given value to this parameter.
     * 
     * @param value 
     */
    public void accept(T value);
    
    /**
     * Returns the value held by this parameter.
     * 
     * @return 
     */
    public T produce();
    
    /**
     * Deletes the value held by this parameter.
     */
    public default void erase() {
        accept(null);
    }
    
    /**
     * Returns the next value 
     * 
     * @return 
     */
    public default boolean validate() {
        return produce() != null;
    }
    
    /**
     * Returns the value returned by {@link #produce()} if {@link #validate()}
     * returns true, otherwise returns the given value.
     * 
     * @param value
     * @return 
     */
    public default T produceOrElse(T value) {
        if (validate()) {
            return produce();
        } else {
            return value;
        }
    }
    
    /**
     * Returns true if this parameter is publicly accessible.
     * 
     * @return 
     */
    public default boolean isPublic() {
        return getParameterName() != null;
    }
    
    /**
     * Returns true if this parameter is public and named the given name.
     * 
     * @param name
     * @return 
     */
    public default boolean isPubliclyNamed(String name) {
        return isPublic() && getParameterName().equals(name);
    }
    
}
