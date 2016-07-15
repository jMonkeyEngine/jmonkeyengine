package com.jme3.util;

import static org.lwjgl.system.jemalloc.JEmalloc.*;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * This class contains a <code>jemalloc</code> allocator which is a general purpose <code>malloc(3)</code> implementation.
 * <p>
 * <b>LWJGL 3 is required</b> because it includes the jemalloc bindings and binaries.
 * </p>
 */
public final class JemallocAllocator implements BufferAllocator
{
	@Override
	public void destroyDirectBuffer(Buffer buffer)
	{
		if (buffer instanceof ByteBuffer)
		{
			je_free((ByteBuffer) buffer);
		}
		else if (buffer instanceof ShortBuffer)
		{
			je_free((ShortBuffer) buffer);
		}
		else if (buffer instanceof IntBuffer)
		{
			je_free((IntBuffer) buffer);
		}
		else if (buffer instanceof LongBuffer)
		{
			je_free((LongBuffer) buffer);
		}
		else if (buffer instanceof FloatBuffer)
		{
			je_free((FloatBuffer) buffer);
		}
		else if (buffer instanceof DoubleBuffer)
		{
			je_free((DoubleBuffer) buffer);
		}
	}

	@Override
	public ByteBuffer allocate(int size)
	{
		return je_malloc(size);
	}
}
