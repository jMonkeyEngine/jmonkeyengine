package com.jme3.util.struct;

import com.jme3.shader.bufferobject.BufferRegion;

import java.util.List;

@Deprecated
public interface StructuredBuffer {

    void setRegions(List<BufferRegion> regions);

    BufferRegion getRegion(int position);

}
