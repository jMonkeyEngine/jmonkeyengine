package com.jme3.scene.plugins.blender.curves;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.jme3.util.BufferUtils;

/**
 * A simple helping class to create index buffer. Depending on the size of the
 * buffer either ShorBuffer or IntBuffer is used to save memory.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class IndexBuffer {
	/** The buffer used for larger objects. */
	private IntBuffer intBuffer;
	/** The buffer used for smaller objects. */
	private ShortBuffer shortBuffer;

	/**
	 * Creates the buffer depending on the given size.
	 * 
	 * @param bufferSize
	 *            the size of the buffer
	 */
	public IndexBuffer(int bufferSize) {
		if (bufferSize < Short.MAX_VALUE) {
			shortBuffer = BufferUtils.createShortBuffer(bufferSize);
		} else {
			intBuffer = BufferUtils.createIntBuffer(bufferSize);
		}
	}

	/**
	 * Puts a value to the created buffer.
	 * 
	 * @param value
	 *            the value to be put to the buffer
	 */
	public void put(int value) {
		if (intBuffer != null) {
			intBuffer.put(value);
		} else {
			shortBuffer.put((short) value);
		}
	}

	/**
	 * Returns the value on the given index. Take in mind that onlye <b>int</b>
	 * is returned, no matter if short or int buffer is used.
	 * 
	 * @param index
	 *            the index of the value
	 * @return the value from the buffer
	 */
	public int get(int index) {
		if (intBuffer != null) {
			return intBuffer.get(index);
		}
		return shortBuffer.get(index);
	}

	/**
	 * @return the limit of the buffer
	 */
	public int limit() {
		if (intBuffer != null) {
			return intBuffer.limit();
		}
		return shortBuffer.limit();
	}

	/**
	 * @return integer buffer
	 */
	public IntBuffer getIntBuffer() {
		return intBuffer;
	}

	/**
	 * @return short buffer
	 */
	public ShortBuffer getShortBuffer() {
		return shortBuffer;
	}
}
