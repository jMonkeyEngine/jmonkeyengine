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

public abstract class AbstractFilter implements Filter {

	protected List<Filter> preFilters = new ArrayList<Filter>();
	protected List<Filter> postFilters = new ArrayList<Filter>();

	private boolean enabled = true;

	@Override
	public Filter addPreFilter(Filter filter) {
		this.preFilters.add(filter);
		return this;
	}

	@Override
	public Filter addPostFilter(Filter filter) {
		this.postFilters.add(filter);
		return this;
	}

	@Override
	public FloatBuffer doFilter(float sx, float sy, float base, FloatBuffer data, int size) {
		if (!this.isEnabled()) {
			return data;
		}
		FloatBuffer retval = data;
		for (Filter f : this.preFilters) {
			retval = f.doFilter(sx, sy, base, retval, size);
		}
		retval = this.filter(sx, sy, base, retval, size);
		for (Filter f : this.postFilters) {
			retval = f.doFilter(sx, sy, base, retval, size);
		}
		return retval;
	}

	public abstract FloatBuffer filter(float sx, float sy, float base, FloatBuffer buffer, int size);

	@Override
	public int getMargin(int size, int margin) {
		// TODO sums up all the margins from filters... maybe there's a more
		// efficient algorithm
		if (!this.isEnabled()) {
			return margin;
		}
		for (Filter f : this.preFilters) {
			margin = f.getMargin(size, margin);
		}
		for (Filter f : this.postFilters) {
			margin = f.getMargin(size, margin);
		}
		return margin;
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
