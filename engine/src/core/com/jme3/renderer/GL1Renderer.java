package com.jme3.renderer;

import com.jme3.material.FixedFuncBinding;

/**
 * Renderer sub-interface that is used for non-shader based renderers.
 * <p>
 * The <code>GL1Renderer</code> provides a single call, 
 * {@link #setFixedFuncBinding(com.jme3.material.FixedFuncBinding, java.lang.Object) }
 * which allows to set fixed functionality state.
 * 
 * @author Kirill Vainer
 */
public interface GL1Renderer extends Renderer {
    
    /**
     * Set the fixed functionality state.
     * <p>
     * See {@link FixedFuncBinding} for various values that
     * can be set.
     * 
     * @param ffBinding The binding to set
     * @param val The value
     */
    public void setFixedFuncBinding(FixedFuncBinding ffBinding, Object val);
}
