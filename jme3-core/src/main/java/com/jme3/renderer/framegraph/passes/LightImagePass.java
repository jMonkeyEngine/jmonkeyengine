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
import com.jme3.renderer.framegraph.light.TiledRenderGrid;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
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
    
    private final LightImagePacker packer = new LightImagePacker();
    private ResourceTicket<LightList> lights;
    private ResourceTicket<TiledRenderGrid> tileInfo;
    private ResourceTicket<Texture2D>[] textures;
    private ResourceTicket<Texture2D>[] tileTextures;
    private ResourceTicket<Integer> numLights;
    private ResourceTicket<ColorRGBA> ambientColor;
    private ResourceTicket<List<LightProbe>> probes;
    private final LightTextureDef lightTexDef = new LightTextureDef(512);
    private final TileTextureDef tileDef = new TileTextureDef();
    private final TileTextureDef indexDef = new TileTextureDef();
    private final ColorRGBA ambient = new ColorRGBA();
    private final LinkedList<LightProbe> probeList = new LinkedList<>();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        lights = addInput("Lights");
        tileInfo = addInput("TileInfo");
        textures = addOutputGroup("Textures", 3);
        tileTextures = addOutputGroup("TileTextures", 2);
        numLights = addOutput("NumLights");
        ambientColor = addOutput("Ambient");
        probes = addOutput("Probes");
        lightTexDef.setFormat(Image.Format.RGBA32F);
        lightTexDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        lightTexDef.setMagFilter(Texture.MagFilter.Nearest);
        lightTexDef.setWrap(Texture.WrapMode.EdgeClamp);
        tileDef.setFormat(Image.Format.RGBA32F);
        tileDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        tileDef.setMagFilter(Texture.MagFilter.Nearest);
        tileDef.setWrap(Texture.WrapMode.EdgeClamp);
        indexDef.setFormat(Image.Format.RGBA32F);
        indexDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        indexDef.setMagFilter(Texture.MagFilter.Nearest);
        indexDef.setWrap(Texture.WrapMode.EdgeClamp);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        for (ResourceTicket<Texture2D> t : textures) {
            declare(lightTexDef, t);
            reserve(t);
        }
        declare(tileDef, tileTextures[0]);
        declare(indexDef, tileTextures[1]);
        declare(null, numLights);
        declare(null, ambientColor);
        declare(null, probes);
        reference(lights);
        referenceOptional(tileInfo);
    }
    @Override
    protected void execute(FGRenderContext context) {
        LightList lightList = resources.acquire(lights);
        TiledRenderGrid grid = resources.acquireOrElse(tileInfo, null);
        Camera cam = context.getViewPort().getCamera();
        Texture2D tiles = null;
        Texture2D indices = null;
        if (grid != null) {
            grid.update(cam);
            tileDef.setSize(grid.getGridWidth(), grid.getGridHeight());
            // four indices are stored per pixel
            int reqPixels = lightTexDef.getWidth()*grid.getNumTiles()/4;
            if (indexDef.getNumPixels() < reqPixels) {
                indexDef.setNumPixels(reqPixels, true, true, false);
            }
            tiles = resources.acquire(tileTextures[0]);
            indices = resources.acquire(tileTextures[1]);
        } else {
            resources.setUndefined(tileTextures[0]);
            resources.setUndefined(tileTextures[1]);
        }
        packer.setTextures(resources.acquire(textures[0]),
                           resources.acquire(textures[1]),
                           resources.acquire(textures[2]),
                           tiles, indices);
        int n = packer.packLights(lightList, ambient, probeList, cam, grid);
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
        out.write(lightTexDef.getWidth(), "maxLights", 512);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        lightTexDef.setWidth(in.readInt("maxLights", 512));
    }
    
    public int getMaxLights() {
        return lightTexDef.getWidth();
    }
    
    public void setMaxLights(int maxLights) {
        lightTexDef.setWidth(maxLights);
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
            ByteBuffer data = BufferUtils.createByteBuffer((int)(format.getBitsPerPixel()/8)*width);
            Image img = new Image(format, width, 1, data, null, getColorSpace());
            return createTexture(img);
        }
        @Override
        public void setHeight(int height) {}
        @Override
        public void setDepth(int depth) {}
        
    }
    private static class TileTextureDef extends TextureDef<Texture2D> {
        
        public TileTextureDef() {
            super(Texture2D.class, img -> new Texture2D(img));
        }
        
        @Override
        public Texture2D createResource() {
            Image.Format format = getFormat();
            int width = getWidth();
            int height = getHeight();
            ByteBuffer data = BufferUtils.createByteBuffer((int)(format.getBitsPerPixel()/8)*width*height);
            Image img = new Image(format, width, height, data, null, getColorSpace());
            return createTexture(img);
        }
        
    }
    
}
