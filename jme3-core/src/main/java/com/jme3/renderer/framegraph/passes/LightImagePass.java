/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.light.LightList;
import com.jme3.light.LightProbe;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.renderer.framegraph.light.LightImagePacker;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author codex
 */
public class LightImagePass extends RenderPass {
    
    private LightImagePacker packer = new LightImagePacker();
    private ResourceTicket<LightList> lights;
    private ResourceTicket<Texture2D>[] textures;
    private ResourceTicket<Integer> numLights;
    private ResourceTicket<ColorRGBA> ambientColor;
    private ResourceTicket<List<LightProbe>> probes;
    private final LightTextureDef texDef = new LightTextureDef(512);
    private final ColorRGBA ambient = new ColorRGBA();
    private final LinkedList<LightProbe> probeList = new LinkedList<>();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        lights = addInput("Lights");
        textures = addOutputGroup("Textures", 3);
        numLights = addOutput("NumLights");
        ambientColor = addOutput("Ambient");
        probes = addOutput("Probes");
        texDef.setFormat(Image.Format.RGBA32F);
        texDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texDef.setMagFilter(Texture.MagFilter.Nearest);
        texDef.setWrap(Texture.WrapMode.EdgeClamp);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        for (ResourceTicket<Texture2D> t : textures) {
            declare(texDef, t);
            reserve(t);
        }
        declare(null, numLights);
        declare(null, ambientColor);
        declare(null, probes);
        reference(lights);
    }
    @Override
    protected void execute(FGRenderContext context) {
        packer.setTextures(
            resources.acquire(textures[0]),
            resources.acquire(textures[1]),
            resources.acquire(textures[2]));
        int n = packer.packLights(resources.acquire(lights), ambient, probeList);
        resources.setPrimitive(numLights, n);
        resources.setPrimitive(ambientColor, ambient);
        resources.setPrimitive(probes, probeList);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(texDef.getWidth(), "maxLights", 512);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        texDef.setWidth(in.readInt("maxLights", 512));
    }

    public int getMaxLights() {
        return texDef.getWidth();
    }
    
    public void setMaxLights(int maxLights) {
        texDef.setWidth(maxLights);
    }
    
    private static class LightTextureDef extends TextureDef<Texture2D> {
        
        public LightTextureDef(int maxLights) {
            super(Texture2D.class, img -> new Texture2D(img));
            setWidth(maxLights);
            super.setHeight(1);
            super.setDepth(0);
        }
        
        @Override
        public Texture2D createResource() {
            Image.Format format = getFormat();
            int width = getWidth();
            ByteBuffer data = BufferUtils.createByteBuffer((int)Math.ceil(format.getBitsPerPixel()/8)*width);
            Image img = new Image(format, width, 1, data, null, getColorSpace());
            //img.setMipmapsGenerated(false);
            return createTexture(img);
        }
        @Override
        public void setHeight(int height) {}
        @Override
        public void setDepth(int depth) {}
        
    }
    
}
