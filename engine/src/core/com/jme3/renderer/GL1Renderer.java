package com.jme3.renderer;

import com.jme3.material.FixedFuncBinding;

public interface GL1Renderer extends Renderer {
    public void setFixedFuncBinding(FixedFuncBinding ffBinding, Object val);
}
