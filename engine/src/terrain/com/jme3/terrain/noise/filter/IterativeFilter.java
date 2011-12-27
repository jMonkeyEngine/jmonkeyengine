/**
 * Copyright (c) 2011, Novyon Events
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * @author Anthyon
 */
package com.jme3.terrain.noise.filter;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme3.terrain.noise.Filter;

public class IterativeFilter extends AbstractFilter {

	private int iterations;

	private List<Filter> preIterateFilters = new ArrayList<Filter>();
	private List<Filter> postIterateFilters = new ArrayList<Filter>();
	private Filter filter;

	@Override
	public int getMargin(int size, int margin) {
		if (!this.isEnabled()) {
			return margin;
		}
		for (Filter f : this.preIterateFilters) {
			margin = f.getMargin(size, margin);
		}
		margin = this.filter.getMargin(size, margin);
		for (Filter f : this.postIterateFilters) {
			margin = f.getMargin(size, margin);
		}
		return this.iterations * margin + super.getMargin(size, margin);
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public int getIterations() {
		return this.iterations;
	}

	public IterativeFilter addPostIterateFilter(Filter filter) {
		this.postIterateFilters.add(filter);
		return this;
	}

	public IterativeFilter addPreIterateFilter(Filter filter) {
		this.preIterateFilters.add(filter);
		return this;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	@Override
	public FloatBuffer filter(float sx, float sy, float base, FloatBuffer data, int size) {
		if (!this.isEnabled()) {
			return data;
		}
		FloatBuffer retval = data;

		for (int i = 0; i < this.iterations; i++) {
			for (Filter f : this.preIterateFilters) {
				retval = f.doFilter(sx, sy, base, retval, size);
			}
			retval = this.filter.doFilter(sx, sy, base, retval, size);
			for (Filter f : this.postIterateFilters) {
				retval = f.doFilter(sx, sy, base, retval, size);
			}
		}

		return retval;
	}
}
