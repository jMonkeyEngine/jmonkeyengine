package com.jme3.util;

import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;

public interface BufferAllocator {

	void destroyDirectBuffer(Buffer toBeDestroyed);

	ByteBuffer allocate(int size);

}
