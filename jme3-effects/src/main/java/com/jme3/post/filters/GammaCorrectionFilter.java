/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.post.filters;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;

/**
 * 
 * @author Phate666
 * @version 1.0 initial version
 * @version 1.1 added luma
 */
public class GammaCorrectionFilter extends Filter
{
	private float gamma = 2.0f;
	private boolean computeLuma = false;

	public GammaCorrectionFilter()
	{
		super("GammaCorrectionFilter");
	}

	public GammaCorrectionFilter(float gamma)
	{
		this();
		this.setGamma(gamma);
	}

	@Override
	protected Material getMaterial()
	{
		return material;
	}

	@Override
	protected void initFilter(AssetManager manager,
			RenderManager renderManager, ViewPort vp, int w, int h)
	{
		material = new Material(manager,
				"Common/MatDefs/Post/GammaCorrection.j3md");
		material.setFloat("gamma", gamma);
		material.setBoolean("computeLuma", computeLuma);
	}

	public float getGamma()
	{
		return gamma;
	}

	/**
	 * set to 0.0 to disable gamma correction
	 * @param gamma
	 */
	public void setGamma(float gamma)
	{
		if (material != null)
		{
			material.setFloat("gamma", gamma);
		}
		this.gamma = gamma;
	}

	public boolean isComputeLuma()
	{
		return computeLuma;
	}

	public void setComputeLuma(boolean computeLuma)
	{
		if (material != null)
		{
			material.setBoolean("computeLuma", computeLuma);
		}
		this.computeLuma = computeLuma;
	}
}
