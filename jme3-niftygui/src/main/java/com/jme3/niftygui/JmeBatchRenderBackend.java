/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.niftygui;

import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import de.lessvoid.nifty.render.BlendMode;
import de.lessvoid.nifty.render.batch.spi.BatchRenderBackend;
import de.lessvoid.nifty.spi.render.MouseCursor;
import de.lessvoid.nifty.tools.Color;
import de.lessvoid.nifty.tools.Factory;
import de.lessvoid.nifty.tools.ObjectPool;
import de.lessvoid.nifty.tools.resourceloader.NiftyResourceLoader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Nifty GUI BatchRenderBackend Implementation for jMonkeyEngine.
 *
 * @author void
 */
public class JmeBatchRenderBackend implements BatchRenderBackend {

    private static final Logger log = Logger.getLogger(JmeBatchRenderBackend.class.getName());

    private final ObjectPool<Batch> batchPool;
    private final List<Batch> batches = new ArrayList<>();

    // A modify texture call needs a jme Renderer to execute. If we're called to modify a texture but don't
    // have a Renderer yet - since it was not initialized on the jme side - we'll cache the modify texture calls
    // in here and execute them later (at the next beginFrame() call).
    private final List<ModifyTexture> modifyTextureCalls = new ArrayList<>();

    private RenderManager renderManager;
    private NiftyJmeDisplay display;
    private Map<Integer, Texture2D> textures = new HashMap<>();
    private int textureAtlasId = 1;
    private Batch currentBatch;
    private Matrix4f tempMat = new Matrix4f();
    private ByteBuffer initialData = null;

    // This is only used for debugging purposes and will fill the removed textures with a color.
    // Please note: the old way to init this via a system property has been
    // removed, since it's now possible to configure it using the
    // BatchRenderConfiguration class when you create the NiftyJmeDisplay instance.
    private boolean fillRemovedTexture = false;

    public JmeBatchRenderBackend(final NiftyJmeDisplay display) {
        this.display = display;
        this.batchPool = new ObjectPool<>(new Factory<Batch>() {

            @Override
            public Batch createNew() {
                return new Batch();
            }
        });
    }

    public void setRenderManager(final RenderManager rm) {
        this.renderManager = rm;
    }

    @Override
    public void setResourceLoader(final NiftyResourceLoader resourceLoader) {
    }

    @Override
    public int getWidth() {
        return display.getWidth();
    }

    @Override
    public int getHeight() {
        return display.getHeight();
    }

    @Override
    public void beginFrame() {
        log.fine("beginFrame()");

        for (int i = 0; i < batches.size(); i++) {
            batchPool.free(batches.get(i));
        }
        batches.clear();

        // in case we have pending modifyTexture calls we'll need to execute them now
        if (!modifyTextureCalls.isEmpty()) {
            Renderer renderer = display.getRenderer();
            for (int i = 0; i < modifyTextureCalls.size(); i++) {
                modifyTextureCalls.get(i).execute(renderer);
            }
            modifyTextureCalls.clear();
        }
    }

    @Override
    public void endFrame() {
        log.fine("endFrame");
    }

    @Override
    public void clear() {
    }

    // TODO: Cursor support
    @Override
    public MouseCursor createMouseCursor(final String filename, final int hotspotX, final int hotspotY) throws IOException {
        return new MouseCursor() {
            @Override
            public void dispose() {
            }

            @Override
            public void enable() {
            }

            @Override
            public void disable() {
            }
        };
    }

    @Override
    public void enableMouseCursor(final MouseCursor mouseCursor) {
    }

    @Override
    public void disableMouseCursor() {
    }

    @Override
    public int createTextureAtlas(final int width, final int height) {
        try {
            // we initialize a buffer here that will be used as base for all texture atlas images
            if (initialData == null) {
                initialData = BufferUtils.createByteBuffer(width * height * 4);
                for (int i = 0; i < width * height; i++) {
                    initialData.put((byte) 0x00);
                    initialData.put((byte) 0x00);
                    initialData.put((byte) 0x00);
                    initialData.put((byte) 0xff);
                }
            }

            int atlasId = addTexture(createAtlasTextureInternal(width, height));

            return atlasId;
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
            return 0; // TODO Nifty always expects this call to be successful
            // there currently is no way to return failure or something :/
        }
    }

    @Override
    public void clearTextureAtlas(final int atlasId) {
        initialData.rewind();
        getTextureAtlas(atlasId).getImage().setData(initialData);
    }

    @Override
    public Image loadImage(final String filename) {
        TextureKey key = new TextureKey(filename, false);
        key.setAnisotropy(0);
        key.setGenerateMips(false);

        Texture2D texture = (Texture2D) display.getAssetManager().loadTexture(key);
        // Fix GLES format incompatibility issue with glTexSubImage
        Renderer renderer = display.getRenderer();
        if (renderer == null || renderer.getCaps().contains(Caps.OpenGLES20)) {
            if (texture.getImage().getFormat() != Format.RGBA8) {
                com.jme3.texture.Image sourceImage = texture.getImage();
                int size = sourceImage.getWidth() * sourceImage.getHeight() * 4;
                ByteBuffer buffer = BufferUtils.createByteBuffer(size);
                com.jme3.texture.Image rgba8Image = new com.jme3.texture.Image(Format.RGBA8,
                        sourceImage.getWidth(),
                        sourceImage.getHeight(),
                        buffer,
                        sourceImage.getColorSpace());

                ImageRaster input = ImageRaster.create(sourceImage, 0, 0, false);
                ImageRaster output = ImageRaster.create(rgba8Image, 0, 0, false);
                ColorRGBA color = new ColorRGBA();

                for (int y = 0; y < sourceImage.getHeight(); y++) {
                    for (int x = 0; x < sourceImage.getWidth(); x++) {
                        output.setPixel(x, y, input.getPixel(x, y, color));
                    }
                }
                return new ImageImpl(rgba8Image);
            }
        }
        return new ImageImpl(texture.getImage());
    }

    @Override
    public Image loadImage(final ByteBuffer imageData, final int imageWidth, final int imageHeight) {
        return new ImageImpl(new com.jme3.texture.Image(Format.RGBA8, imageWidth, imageHeight, imageData, ColorSpace.Linear));
    }

    @Override
    public void addImageToAtlas(final Image image, final int x, final int y, final int atlasTextureId) {
        ImageImpl imageImpl = (ImageImpl) image;
        imageImpl.modifyTexture(this, getTextureAtlas(atlasTextureId), x, y);
    }

    @Override
    public int createNonAtlasTexture(final Image image) {
        ImageImpl imageImpl = (ImageImpl) image;

        Texture2D texture = new Texture2D(imageImpl.image);
        texture.setMinFilter(MinFilter.NearestNoMipMaps);
        texture.setMagFilter(MagFilter.Nearest);
        return addTexture(texture);
    }

    @Override
    public void deleteNonAtlasTexture(final int textureId) {
        textures.remove(textureId);
    }

    @Override
    public boolean existsNonAtlasTexture(final int textureId) {
        return textures.containsKey(textureId);
    }

    @Override
    public void beginBatch(final BlendMode blendMode, final int textureId) {
        batches.add(batchPool.allocate());
        currentBatch = batches.get(batches.size() - 1);
        currentBatch.begin(blendMode, getTextureAtlas(textureId));
    }

    @Override
    public void addQuad(
            final float x,
            final float y,
            final float width,
            final float height,
            final Color color1,
            final Color color2,
            final Color color3,
            final Color color4,
            final float textureX,
            final float textureY,
            final float textureWidth,
            final float textureHeight,
            final int textureId) {
        if (!currentBatch.canAddQuad()) {
            beginBatch(currentBatch.getBlendMode(), textureId);
        }
        currentBatch.addQuadInternal(x, y, width, height, color1, color2, color3, color4, textureX, textureY, textureWidth, textureHeight);
    }

    @Override
    public int render() {
        for (int i = 0; i < batches.size(); i++) {
            Batch batch = batches.get(i);
            batch.render();
        }
        return batches.size();
    }

    @Override
    public void removeImageFromAtlas(final Image image, final int x, final int y, final int w, final int h, final int atlasTextureId) {
        // Since we clear the whole texture when we switch screens it's not really necessary to remove data from the
        // texture atlas when individual textures are removed. If necessary this can be enabled with a system property.
        if (!fillRemovedTexture) {
            return;
        }

        ByteBuffer initialData = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
        for (int i = 0; i < image.getWidth() * image.getHeight(); i++) {
            initialData.put((byte) 0xff);
            initialData.put((byte) 0x00);
            initialData.put((byte) 0x00);
            initialData.put((byte) 0xff);
        }
        initialData.rewind();
        modifyTexture(
                getTextureAtlas(atlasTextureId),
                new com.jme3.texture.Image(Format.RGBA8, image.getWidth(), image.getHeight(), initialData, ColorSpace.sRGB),
                x,
                y);
    }

    /**
     * Whether or not to render textures with high quality settings. Usually,
     * setting to true will result in slower performance, but nicer looking
     * textures, and vice versa. How high quality textures are rendered versus
     * low quality textures will vary depending on the
     * {@link de.lessvoid.nifty.render.batch.spi.BatchRenderBackend}
     * implementation
     *
     * @param shouldUseHighQualityTextures true&rarr;higher quality,
     * false&rarr;lower quality
     */
    @Override
    public void useHighQualityTextures(final boolean shouldUseHighQualityTextures) {
        // TODO when true this should use something like linear filtering
        // not sure right now how to tell jme about that ... might not be
        // necessary to be set?
    }

    /**
     * Whether or not to overwrite previously used atlas space with blank data.
     * Setting to true will result in slower performance, but may be useful in
     * debugging when visually inspecting the atlas, since there will not be
     * portions of old images visible in currently unused atlas space.
     *
     * @param shouldFill true&rarr;overwrite with blank data, false&rarr;don't
     * overwrite
     */
    @Override
    public void fillRemovedImagesInAtlas(final boolean shouldFill) {
        fillRemovedTexture = shouldFill;
    }

    // internal implementations
    private Texture2D createAtlasTextureInternal(final int width, final int height) throws Exception {
        // re-use pre-defined initial data instead of creating a new buffer
        initialData.rewind();

        Texture2D texture = new Texture2D(new com.jme3.texture.Image(Format.RGBA8, width, height, initialData, ColorSpace.sRGB));
        texture.setMinFilter(MinFilter.NearestNoMipMaps);
        texture.setMagFilter(MagFilter.Nearest);
        return texture;
    }

    private void modifyTexture(
            final Texture2D textureAtlas,
            final com.jme3.texture.Image image,
            final int x,
            final int y) {
        Renderer renderer = display.getRenderer();
        if (renderer == null) {
            // we have no renderer (yet) so we'll need to cache this call to the next beginFrame() call
            modifyTextureCalls.add(new ModifyTexture(textureAtlas, image, x, y));
            return;
        }

        // All is well. We can execute the modify right away.
        renderer.modifyTexture(textureAtlas, image, x, y);
    }

    private Texture2D getTextureAtlas(final int atlasId) {
        return textures.get(atlasId);
    }

    private int addTexture(final Texture2D texture) {
        final int atlasId = textureAtlasId++;
        textures.put(atlasId, texture);
        return atlasId;
    }

    /**
     * Simple BatchRenderBackend.Image implementation that will transport the
     * dimensions of an image as well as the actual bytes from the loadImage()
     * to the addImageToTexture() method.
     *
     * @author void
     */
    private static class ImageImpl implements BatchRenderBackend.Image {

        private final com.jme3.texture.Image image;

        public ImageImpl(final com.jme3.texture.Image image) {
            this.image = image;
        }

        public void modifyTexture(
                final JmeBatchRenderBackend backend,
                final Texture2D textureAtlas,
                final int x,
                final int y) {
            backend.modifyTexture(textureAtlas, image, x, y);
        }

        @Override
        public int getWidth() {
            return image.getWidth();
        }

        @Override
        public int getHeight() {
            return image.getHeight();
        }
    }

    /**
     * Used to delay ModifyTexture calls in case we don't have a JME3 Renderer
     * yet.
     *
     * @author void
     */
    private static class ModifyTexture {

        private final Texture2D atlas;
        private final com.jme3.texture.Image image;
        private final int x;
        private final int y;

        private ModifyTexture(final Texture2D atlas, final com.jme3.texture.Image image, final int x, final int y) {
            this.atlas = atlas;
            this.image = image;
            this.x = x;
            this.y = y;
        }

        public void execute(final Renderer renderer) {
            renderer.modifyTexture(atlas, image, x, y);
        }
    }

    /**
     * This class helps us to manage the batch data. We'll keep a bunch of
     * instances of this class around that will be reused when needed. Each
     * Batch instance provides room for a certain number of vertices. We'll
     * use a new Batch when we exceed that limit.
     *
     * @author void
     */
    private class Batch {
        // 4 vertices per quad and 8 vertex attributes for each vertex:
        // - 2 x pos
        // - 2 x texture
        // - 4 x color
        //
        // stored into 3 different buffers: position, texture coords, vertex color
        // and an additional buffer for indexes
        //
        // there is a fixed amount of primitives per batch. if we run out of vertices we'll start a new batch.

        private final static int BATCH_MAX_QUADS = 2000;
        private final static int BATCH_MAX_VERTICES = BATCH_MAX_QUADS * 4;

        // individual buffers for all the vertex attributes
        private final VertexBuffer vertexPos = new VertexBuffer(Type.Position);
        private final VertexBuffer vertexTexCoord = new VertexBuffer(Type.TexCoord);
        private final VertexBuffer vertexColor = new VertexBuffer(Type.Color);
        private final VertexBuffer indexBuffer = new VertexBuffer(Type.Index);

        private final Mesh mesh = new Mesh();
        private final Geometry meshGeometry = new Geometry("nifty-quad", mesh);
        private final RenderState renderState = new RenderState();

        private final FloatBuffer vertexPosBuffer;
        private final FloatBuffer vertexTexCoordBuffer;
        private final FloatBuffer vertexColorBuffer;
        private final ShortBuffer indexBufferBuffer;

        // number of quads already added to this batch.
        private int quadCount;
        private short globalVertexIndex;

        // current blend mode
        private BlendMode blendMode = BlendMode.BLEND;
        private Texture2D texture;
        private final Material material;

        public Batch() {
            // set up mesh
            vertexPos.setupData(Usage.Stream, 2, VertexBuffer.Format.Float, BufferUtils.createFloatBuffer(BATCH_MAX_VERTICES * 2));
            vertexPosBuffer = (FloatBuffer) vertexPos.getData();
            mesh.setBuffer(vertexPos);

            vertexTexCoord.setupData(Usage.Stream, 2, VertexBuffer.Format.Float, BufferUtils.createFloatBuffer(BATCH_MAX_VERTICES * 2));
            vertexTexCoordBuffer = (FloatBuffer) vertexTexCoord.getData();
            mesh.setBuffer(vertexTexCoord);

            vertexColor.setupData(Usage.Stream, 4, VertexBuffer.Format.Float, BufferUtils.createFloatBuffer(BATCH_MAX_VERTICES * 4));
            vertexColorBuffer = (FloatBuffer) vertexColor.getData();
            mesh.setBuffer(vertexColor);

            indexBuffer.setupData(Usage.Stream, 3, VertexBuffer.Format.UnsignedShort, BufferUtils.createShortBuffer(BATCH_MAX_QUADS * 2 * 3));
            indexBufferBuffer = (ShortBuffer) indexBuffer.getData();
            mesh.setBuffer(indexBuffer);

            material = new Material(display.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            material.setBoolean("VertexColor", true);

            renderState.setDepthTest(false);
            renderState.setDepthWrite(false);
        }

        public void begin(final BlendMode blendMode, final Texture2D texture) {
            this.blendMode = blendMode;
            this.texture = texture;

            quadCount = 0;
            globalVertexIndex = 0;
            vertexPosBuffer.clear();
            vertexTexCoordBuffer.clear();
            vertexColorBuffer.clear();
            indexBufferBuffer.clear();
        }

        public BlendMode getBlendMode() {
            return blendMode;
        }

        public void render() {
            renderState.setBlendMode(convertBlend(blendMode));

            vertexPosBuffer.flip();
            vertexPos.updateData(vertexPosBuffer);

            vertexTexCoordBuffer.flip();
            vertexTexCoord.updateData(vertexTexCoordBuffer);

            vertexColorBuffer.flip();
            vertexColor.updateData(vertexColorBuffer);

            indexBufferBuffer.flip();
            indexBuffer.updateData(indexBufferBuffer);

            tempMat.loadIdentity();
            renderManager.setWorldMatrix(tempMat);
            renderManager.setForcedRenderState(renderState);

            material.setTexture("ColorMap", texture);
            mesh.updateCounts();
            material.render(meshGeometry, renderManager);
            renderManager.setForcedRenderState(null);
        }

        private RenderState.BlendMode convertBlend(final BlendMode blendMode) {
            if (blendMode == null) {
                return RenderState.BlendMode.Off;
            } else {
                switch (blendMode) {
                    case BLEND:
                        return RenderState.BlendMode.Alpha;
                    case MULIPLY:
                        return RenderState.BlendMode.Alpha;
                    default:
                        throw new UnsupportedOperationException();
                }
            }
        }

        public boolean canAddQuad() {
            return (quadCount + 1) < BATCH_MAX_QUADS;
        }

        private void addQuadInternal(
                final float x,
                final float y,
                final float width,
                final float height,
                final Color color1,
                final Color color2,
                final Color color3,
                final Color color4,
                final float textureX,
                final float textureY,
                final float textureWidth,
                final float textureHeight) {
            indexBufferBuffer.put((short) (globalVertexIndex + 0));
            indexBufferBuffer.put((short) (globalVertexIndex + 3));
            indexBufferBuffer.put((short) (globalVertexIndex + 2));

            indexBufferBuffer.put((short) (globalVertexIndex + 0));
            indexBufferBuffer.put((short) (globalVertexIndex + 2));
            indexBufferBuffer.put((short) (globalVertexIndex + 1));

            addVertex(x, y, textureX, textureY, color1);
            addVertex(x + width, y, textureX + textureWidth, textureY, color2);
            addVertex(x + width, y + height, textureX + textureWidth, textureY + textureHeight, color4);
            addVertex(x, y + height, textureX, textureY + textureHeight, color3);

            quadCount++;
            globalVertexIndex += 4;
        }

        private void addVertex(final float x, final float y, final float tx, final float ty, final Color c) {
            vertexPosBuffer.put(x);
            vertexPosBuffer.put(getHeight() - y);
            vertexTexCoordBuffer.put(tx);
            vertexTexCoordBuffer.put(ty);
            vertexColorBuffer.put(c.getRed());
            vertexColorBuffer.put(c.getGreen());
            vertexColorBuffer.put(c.getBlue());
            vertexColorBuffer.put(c.getAlpha());
        }
    }
}
