package com.jme3.vulkan.slang;

import java.nio.ByteBuffer;

public interface ComponentType {

    ProgramLayout getLayout();

    ComponentType link();

    ByteBuffer getEntryPointCode(int entryPointIndex, int targetIndex);

}
