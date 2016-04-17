/*
 * Copyright (c) 2009-2016 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.opencl;

import java.util.Arrays;

/**
 * The work size (global and local) for executing a kernel
 * @author Sebastian Weiss
 */
public final class WorkSize {
	private int dimension;
	private int[] sizes;
	
	public WorkSize(int dimension, int... sizes)
	{
		set(dimension, sizes);
	}
	
	public WorkSize() {
		this(1, 1, 1, 1);
	}
	public WorkSize(int size) {
		this(1, size, 1, 1);
	}
	public WorkSize(int width, int height) {
		this(2, width, height, 1);
	}
	public WorkSize(int width, int height, int depth) {
		this(3, width, height, depth);
	}

	public int getDimension() {
		return dimension;
	}
	public int[] getSizes() {
		return sizes;
	}
	
	public void set(int dimension, int... sizes) {
		if (sizes==null || sizes.length!=3) {
			throw new IllegalArgumentException("sizes must be an array of length 3");
		}
		if (dimension<=0 || dimension>3) {
			throw new IllegalArgumentException("dimension must be between 1 and 3");
		}
		this.dimension = dimension;
		this.sizes = sizes;
	}
	public void set(WorkSize ws) {
		this.dimension = ws.dimension;
		this.sizes = ws.sizes;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 47 * hash + this.dimension;
		hash = 47 * hash + Arrays.hashCode(this.sizes);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final WorkSize other = (WorkSize) obj;
		if (this.dimension != other.dimension) {
			return false;
		}
		if (!Arrays.equals(this.sizes, other.sizes)) {
			return false;
		}
		return true;
	}
	
	
}
