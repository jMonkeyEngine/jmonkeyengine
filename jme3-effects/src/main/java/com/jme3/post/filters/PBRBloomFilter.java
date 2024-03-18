/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.post.filters;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class PBRBloomFilter extends Filter {
    
    private AssetManager assetManager;
    private RenderManager renderManager;
    private ViewPort viewPort;
    private Pass[] downsamplingPasses = new Pass[4];
    private Pass[] upsamplingPasses = new Pass[4];
    private Image.Format format = Image.Format.RGBA16F;
    
    @Override
    protected void initFilter(AssetManager am, RenderManager rm, ViewPort vp, int width, int height) {
        
        assetManager = am;
        renderManager = rm;
        viewPort = vp;
        postRenderPasses = new LinkedList<>();
        Renderer renderer = renderManager.getRenderer();
        
        int w = width;
        int h = height;
        
        // downsampling passes
        Material downsampleMat = new Material(assetManager, "Common/MatDefs/Post/Downsample.j3md");
        Vector2f initTexelSize = new Vector2f(1f/w, 1f/h);
        w /= 2; h /= 2;
        Pass initialPass = new Pass() {
            @Override
            public boolean requiresSceneAsTexture() {
                return true;
            }
            @Override
            public void beforeRender() {
                downsampleMat.setVector2("TexelSize", initTexelSize);
            }
        };
        initialPass.init(renderer, w, h, format, Image.Format.Depth, 1, downsampleMat);
        postRenderPasses.add(initialPass);
        downsamplingPasses[0] = initialPass;
        for (int i = 1; i < downsamplingPasses.length; i++) {
            int wi = w, hi = h;
            Vector2f texelSize = new Vector2f(1f/w, 1f/h);
            w /= 2; h /= 2;
            Pass prev = downsamplingPasses[i-1];
            Pass pass = new Pass() {
                @Override
                public void beforeRender() {
                    //renderManager.getRenderer().setViewPort(0, 0, wi, hi);
                    downsampleMat.setTexture("Texture", prev.getRenderedTexture());
                    downsampleMat.setVector2("TexelSize", texelSize);
                }
            };
            pass.init(renderer, w, h, format, Image.Format.Depth, 1, downsampleMat);
            postRenderPasses.add(pass);
            downsamplingPasses[i] = pass;
        }
        
        // upsampling passes
        Material upsampleMat = new Material(assetManager, "Common/MatDefs/Post/Upsample.j3md");
        for (int i = 0; i < upsamplingPasses.length; i++) {
            int wi = w, hi = h;
            Vector2f texelSize = new Vector2f(1f/w, 1f/h);
            w *= 2; h *= 2;
            Pass prev;
            if (i == 0) {
                prev = downsamplingPasses[downsamplingPasses.length-1];
            } else {
                prev = upsamplingPasses[i-1];
            }
            Pass pass = new Pass() {
                @Override
                public void beforeRender() {
                    //renderManager.getRenderer().setViewPort(0, 0, wi, hi);
                    upsampleMat.setTexture("Texture", prev.getRenderedTexture());
                    upsampleMat.setVector2("TexelSize", texelSize);
                }
            };
            pass.init(renderer, w, h, format, Image.Format.Depth, 1, upsampleMat);
            postRenderPasses.add(pass);
            upsamplingPasses[i] = pass;
        }
        
        material = new Material(assetManager, "Common/MatDefs/Post/PBRBloomFinal.j3md");
        material.setTexture("GlowMap", upsamplingPasses[upsamplingPasses.length-1].getRenderedTexture());
        
    }
    @Override
    protected Material getMaterial() {
        return material;
    }
    @Override
    protected void postFilter(Renderer r, FrameBuffer buffer) {  
        //renderManager.getRenderer().setV
    }
    
}
