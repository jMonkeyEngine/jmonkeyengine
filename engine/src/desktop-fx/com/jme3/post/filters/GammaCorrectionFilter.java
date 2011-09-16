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
