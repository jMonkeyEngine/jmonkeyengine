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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A filter that draws a highlighting shape around objects in the scene that have
 * the material parameter {@code Highlighted} set to true.
 * @author Sebastian Weiss
 */
public class HighlightFilter extends Filter {
	
	public static final int MIN_BRUSH_SIZE = 1;
	public static final int MAX_BRUSH_SIZE = 3;
	
	private Material drawHighlightMaterial;
	private Pass preHighlightPass;
	
	private int size = MIN_BRUSH_SIZE;
	private boolean debug = false;
	private ColorRGBA highlightColor = ColorRGBA.BlackNoAlpha;
	
	private int screenWidth;
    private int screenHeight;    
    private RenderManager renderManager;
    private ViewPort viewPort;
    private AssetManager assetManager;

	public HighlightFilter() {
		super("HighlightFilter");
	}
	
	@Override
	protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
		this.renderManager = renderManager;
        this.viewPort = vp;

        this.assetManager = manager;             
        screenWidth = w;
        screenHeight = h;
		
		preHighlightPass = new Pass();
        preHighlightPass.init(renderManager.getRenderer(), screenWidth, screenHeight, Image.Format.RGB8, Image.Format.Depth);
		
		drawHighlightMaterial = new Material(manager, "Common/MatDefs/Post/HighlightFinal.j3md");
		drawHighlightMaterial.setTexture("HighlightTex", preHighlightPass.getRenderedTexture());
		drawHighlightMaterial.setFloat("StepX", 1f/w);
		drawHighlightMaterial.setFloat("StepY", 1f/h);	
		
		debugEnabled(debug);
		setBrushSize(size);
		setHighlightColor(highlightColor);
	}

	@Override
	protected Material getMaterial() {
		return drawHighlightMaterial;
	}
	
	@Override
    protected void postQueue(RenderQueue queue) {
		renderManager.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);            
		renderManager.getRenderer().setFrameBuffer(preHighlightPass.getRenderFrameBuffer());
		renderManager.getRenderer().clearBuffers(true, true, true);
		renderManager.setForcedTechnique("Highlight");
		renderManager.renderViewPortQueues(viewPort, false);         
		renderManager.setForcedTechnique(null);
		renderManager.getRenderer().setFrameBuffer(viewPort.getOutputFrameBuffer());
    }

	@Override
	protected void cleanUpFilter(Renderer r) {
		preHighlightPass.cleanup(r);
	}
	
	public void debugEnabled(boolean debug) {
		this.debug = debug;
		if (drawHighlightMaterial!=null) {
			drawHighlightMaterial.setBoolean("Debug", debug);
		}
	}
	
	public void setBrushSize(int size) {
		if (size<MIN_BRUSH_SIZE || size>MAX_BRUSH_SIZE) {
			throw new IllegalArgumentException("brush size must be greater equals than "+MIN_BRUSH_SIZE
				+" and smaller equals than "+MAX_BRUSH_SIZE);
		}
		this.size = size;
		if (drawHighlightMaterial!=null) {
			drawHighlightMaterial.setInt("Size", size);
		}
	}
	
	public int getBrushSize() {
		return size;
	}
	
	public void setHighlightColor(ColorRGBA col) {
		if (col==null) {
			throw new NullPointerException("color is null");
		}
		this.highlightColor = col;
		if (drawHighlightMaterial!=null) {
			drawHighlightMaterial.setColor("HighlightColor", col);
		}
	}
	
	public ColorRGBA getHighlightColor() {
		return highlightColor;
	}
	
	@Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
		oc.write(size, "brushSize", MIN_BRUSH_SIZE);
		oc.write(highlightColor, "highlightColor", ColorRGBA.BlackNoAlpha);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
		size = ic.readInt("brushSize", MIN_BRUSH_SIZE);
		highlightColor = (ColorRGBA) ic.readSavable("highlightColor", ColorRGBA.BlackNoAlpha);
    }
}
