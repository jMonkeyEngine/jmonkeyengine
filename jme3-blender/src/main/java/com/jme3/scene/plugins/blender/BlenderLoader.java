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
package com.jme3.scene.plugins.blender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.Animation;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.BlenderKey;
import com.jme3.asset.ModelKey;
import com.jme3.light.Light;
import com.jme3.math.ColorRGBA;
import com.jme3.post.Filter;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.LightNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.animations.AnimationHelper;
import com.jme3.scene.plugins.blender.cameras.CameraHelper;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper;
import com.jme3.scene.plugins.blender.curves.CurvesHelper;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.BlenderInputStream;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.FileBlockHeader.BlockCode;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.landscape.LandscapeHelper;
import com.jme3.scene.plugins.blender.lights.LightHelper;
import com.jme3.scene.plugins.blender.materials.MaterialContext;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.scene.plugins.blender.meshes.MeshHelper;
import com.jme3.scene.plugins.blender.meshes.TemporalMesh;
import com.jme3.scene.plugins.blender.modifiers.ModifierHelper;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;
import com.jme3.scene.plugins.blender.particles.ParticlesHelper;
import com.jme3.scene.plugins.blender.textures.TextureHelper;
import com.jme3.texture.Texture;

/**
 * This is the main loading class. Have in notice that asset manager needs to have loaders for resources like textures.
 * @author Marcin Roguski (Kaelthas)
 */
public class BlenderLoader implements AssetLoader {
    private static final Logger     LOGGER = Logger.getLogger(BlenderLoader.class.getName());

    /** The blocks read from the file. */
    protected List<FileBlockHeader> blocks;
    /** The blender context. */
    protected BlenderContext        blenderContext;

    @Override
    public Spatial load(AssetInfo assetInfo) throws IOException {
        try {
            this.setup(assetInfo);

            AnimationHelper animationHelper = blenderContext.getHelper(AnimationHelper.class);
            animationHelper.loadAnimations();

            BlenderKey blenderKey = blenderContext.getBlenderKey();
            LoadedFeatures loadedFeatures = new LoadedFeatures();
            for (FileBlockHeader block : blocks) {
                switch (block.getCode()) {
                    case BLOCK_OB00:
                        ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
                        Node object = (Node) objectHelper.toObject(block.getStructure(blenderContext), blenderContext);
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, "{0}: {1}--> {2}", new Object[] { object.getName(), object.getLocalTranslation().toString(), object.getParent() == null ? "null" : object.getParent().getName() });
                        }
                        if (object.getParent() == null) {
                            loadedFeatures.objects.add(object);
                        }
                        if (object instanceof LightNode && ((LightNode) object).getLight() != null) {
                            loadedFeatures.lights.add(((LightNode) object).getLight());
                        } else if (object instanceof CameraNode && ((CameraNode) object).getCamera() != null) {
                            loadedFeatures.cameras.add(((CameraNode) object).getCamera());
                        }
                        break;
                    case BLOCK_SC00:// Scene
                        loadedFeatures.sceneBlocks.add(block);
                        break;
                    case BLOCK_MA00:// Material
                        MaterialHelper materialHelper = blenderContext.getHelper(MaterialHelper.class);
                        MaterialContext materialContext = materialHelper.toMaterialContext(block.getStructure(blenderContext), blenderContext);
                        loadedFeatures.materials.add(materialContext);
                        break;
                    case BLOCK_ME00:// Mesh
                        MeshHelper meshHelper = blenderContext.getHelper(MeshHelper.class);
                        TemporalMesh temporalMesh = meshHelper.toTemporalMesh(block.getStructure(blenderContext), blenderContext);
                        loadedFeatures.meshes.add(temporalMesh);
                        break;
                    case BLOCK_IM00:// Image
                        TextureHelper textureHelper = blenderContext.getHelper(TextureHelper.class);
                        Texture image = textureHelper.loadImageAsTexture(block.getStructure(blenderContext), 0, blenderContext);
                        if (image != null && image.getImage() != null) {// render results are stored as images but are not being loaded
                            loadedFeatures.images.add(image);
                        }
                        break;
                    case BLOCK_TE00:
                        Structure textureStructure = block.getStructure(blenderContext);
                        int type = ((Number) textureStructure.getFieldValue("type")).intValue();
                        if (type == TextureHelper.TEX_IMAGE) {
                            TextureHelper texHelper = blenderContext.getHelper(TextureHelper.class);
                            Texture texture = texHelper.getTexture(textureStructure, null, blenderContext);
                            if (texture != null) {// null is returned when texture has no image
                                loadedFeatures.textures.add(texture);
                            }
                        } else {
                            LOGGER.fine("Only image textures can be loaded as unlinked assets. Generated textures will be applied to an existing object.");
                        }
                        break;
                    case BLOCK_WO00:// World
                        LandscapeHelper landscapeHelper = blenderContext.getHelper(LandscapeHelper.class);
                        Structure worldStructure = block.getStructure(blenderContext);

                        String worldName = worldStructure.getName();
                        if (blenderKey.getUsedWorld() == null || blenderKey.getUsedWorld().equals(worldName)) {

                            Light ambientLight = landscapeHelper.toAmbientLight(worldStructure);
                            if (ambientLight != null) {
                                loadedFeatures.objects.add(new LightNode(null, ambientLight));
                                loadedFeatures.lights.add(ambientLight);
                            }
                            loadedFeatures.sky = landscapeHelper.toSky(worldStructure);
                            loadedFeatures.backgroundColor = landscapeHelper.toBackgroundColor(worldStructure);

                            Filter fogFilter = landscapeHelper.toFog(worldStructure);
                            if (fogFilter != null) {
                                loadedFeatures.filters.add(landscapeHelper.toFog(worldStructure));
                            }
                        }
                        break;
                    case BLOCK_AC00:
                        LOGGER.fine("Loading unlinked animations is not yet supported!");
                        break;
                    default:
                        LOGGER.log(Level.FINEST, "Ommiting the block: {0}.", block.getCode());
                }
            }

            LOGGER.fine("Baking constraints after every feature is loaded.");
            ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
            constraintHelper.bakeConstraints(blenderContext);

            LOGGER.fine("Loading scenes and attaching them to the root object.");
            for (FileBlockHeader sceneBlock : loadedFeatures.sceneBlocks) {
                loadedFeatures.scenes.add(this.toScene(sceneBlock.getStructure(blenderContext)));
            }

            LOGGER.fine("Creating the root node of the model and applying loaded nodes of the scene and loaded features to it.");
            Node modelRoot = new Node(blenderKey.getName());
            for (Node scene : loadedFeatures.scenes) {
                modelRoot.attachChild(scene);
            }

            if (blenderKey.isLoadUnlinkedAssets()) {
                LOGGER.fine("Setting loaded content as user data in resulting sptaial.");
                Map<String, Map<String, Object>> linkedData = new HashMap<String, Map<String, Object>>();

                Map<String, Object> thisFileData = new HashMap<String, Object>();
                thisFileData.put("scenes", loadedFeatures.scenes == null ? new ArrayList<Object>() : loadedFeatures.scenes);
                thisFileData.put("objects", loadedFeatures.objects == null ? new ArrayList<Object>() : loadedFeatures.objects);
                thisFileData.put("meshes", loadedFeatures.meshes == null ? new ArrayList<Object>() : loadedFeatures.meshes);
                thisFileData.put("materials", loadedFeatures.materials == null ? new ArrayList<Object>() : loadedFeatures.materials);
                thisFileData.put("textures", loadedFeatures.textures == null ? new ArrayList<Object>() : loadedFeatures.textures);
                thisFileData.put("images", loadedFeatures.images == null ? new ArrayList<Object>() : loadedFeatures.images);
                thisFileData.put("animations", loadedFeatures.animations == null ? new ArrayList<Object>() : loadedFeatures.animations);
                thisFileData.put("cameras", loadedFeatures.cameras == null ? new ArrayList<Object>() : loadedFeatures.cameras);
                thisFileData.put("lights", loadedFeatures.lights == null ? new ArrayList<Object>() : loadedFeatures.lights);
                thisFileData.put("filters", loadedFeatures.filters == null ? new ArrayList<Object>() : loadedFeatures.filters);
                thisFileData.put("backgroundColor", loadedFeatures.backgroundColor);
                thisFileData.put("sky", loadedFeatures.sky);

                linkedData.put("this", thisFileData);
                linkedData.putAll(blenderContext.getLinkedFeatures());

                modelRoot.setUserData("linkedData", linkedData);
            }

            return modelRoot;
        } catch (BlenderFileException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        } catch (Exception e) {
            throw new IOException("Unexpected importer exception occured: " + e.getLocalizedMessage(), e);
        } finally {
            this.clear();
        }
    }

    /**
     * This method converts the given structure to a scene node.
     * @param structure
     *            structure of a scene
     * @return scene's node
     * @throws BlenderFileException
     *             an exception throw when problems with blender file occur
     */
    private Node toScene(Structure structure) throws BlenderFileException {
        ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
        Node result = new Node(structure.getName());
        List<Structure> base = ((Structure) structure.getFieldValue("base")).evaluateListBase();
        for (Structure b : base) {
            Pointer pObject = (Pointer) b.getFieldValue("object");
            if (pObject.isNotNull()) {
                Structure objectStructure = pObject.fetchData().get(0);

                Object object = objectHelper.toObject(objectStructure, blenderContext);
                if (object instanceof LightNode) {
                    result.addLight(((LightNode) object).getLight());// FIXME: check if this is needed !!!
                    result.attachChild((LightNode) object);
                } else if (object instanceof Node) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "{0}: {1}--> {2}", new Object[] { ((Node) object).getName(), ((Node) object).getLocalTranslation().toString(), ((Node) object).getParent() == null ? "null" : ((Node) object).getParent().getName() });
                    }
                    if (((Node) object).getParent() == null) {
                        result.attachChild((Spatial) object);
                    }
                }
            }
        }
        return result;
    }

    /**
     * This method sets up the loader.
     * @param assetInfo
     *            the asset info
     * @throws BlenderFileException
     *             an exception is throw when something wrong happens with blender file
     */
    protected void setup(AssetInfo assetInfo) throws BlenderFileException {
        // registering loaders
        ModelKey modelKey = (ModelKey) assetInfo.getKey();
        BlenderKey blenderKey;
        if (modelKey instanceof BlenderKey) {
            blenderKey = (BlenderKey) modelKey;
        } else {
            blenderKey = new BlenderKey(modelKey.getName());
            blenderKey.setAssetRootPath(modelKey.getFolder());
        }

        // opening stream
        BlenderInputStream inputStream = new BlenderInputStream(assetInfo.openStream());

        // reading blocks
        blocks = new ArrayList<FileBlockHeader>();
        FileBlockHeader fileBlock;
        blenderContext = new BlenderContext();
        blenderContext.setBlenderVersion(inputStream.getVersionNumber());
        blenderContext.setAssetManager(assetInfo.getManager());
        blenderContext.setInputStream(inputStream);
        blenderContext.setBlenderKey(blenderKey);

        // creating helpers
        blenderContext.putHelper(AnimationHelper.class, new AnimationHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(TextureHelper.class, new TextureHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(MeshHelper.class, new MeshHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(ObjectHelper.class, new ObjectHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(CurvesHelper.class, new CurvesHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(LightHelper.class, new LightHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(CameraHelper.class, new CameraHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(ModifierHelper.class, new ModifierHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(MaterialHelper.class, new MaterialHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(ConstraintHelper.class, new ConstraintHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(ParticlesHelper.class, new ParticlesHelper(inputStream.getVersionNumber(), blenderContext));
        blenderContext.putHelper(LandscapeHelper.class, new LandscapeHelper(inputStream.getVersionNumber(), blenderContext));

        // reading the blocks (dna block is automatically saved in the blender context when found)
        FileBlockHeader sceneFileBlock = null;
        do {
            fileBlock = new FileBlockHeader(inputStream, blenderContext);
            if (!fileBlock.isDnaBlock()) {
                blocks.add(fileBlock);
                // save the scene's file block
                if (fileBlock.getCode() == BlockCode.BLOCK_SC00) {
                    sceneFileBlock = fileBlock;
                }
            }
        } while (!fileBlock.isLastBlock());
        if (sceneFileBlock != null) {
            blenderContext.setSceneStructure(sceneFileBlock.getStructure(blenderContext));
        }
    }

    /**
     * The internal data is only needed during loading so make it unreachable so that the GC can release
     * that memory (which can be quite large amount).
     */
    protected void clear() {
        blenderContext = null;
        blocks = null;
    }

    /**
     * This class holds the loading results according to the given loading flag.
     * @author Marcin Roguski (Kaelthas)
     */
    private static class LoadedFeatures {
        private List<FileBlockHeader> sceneBlocks     = new ArrayList<FileBlockHeader>();
        /** The scenes from the file. */
        private List<Node>            scenes          = new ArrayList<Node>();
        /** Objects from all scenes. */
        private List<Node>            objects         = new ArrayList<Node>();
        /** All meshes. */
        private List<TemporalMesh>    meshes          = new ArrayList<TemporalMesh>();
        /** Materials from all objects. */
        private List<MaterialContext> materials       = new ArrayList<MaterialContext>();
        /** Textures from all objects. */
        private List<Texture>         textures        = new ArrayList<Texture>();
        /** The images stored in the blender file. */
        private List<Texture>         images          = new ArrayList<Texture>();
        /** Animations of all objects. */
        private List<Animation>       animations      = new ArrayList<Animation>();
        /** All cameras from the file. */
        private List<Camera>          cameras         = new ArrayList<Camera>();
        /** All lights from the file. */
        private List<Light>           lights          = new ArrayList<Light>();
        /** Loaded sky. */
        private Spatial               sky;
        /** Scene filters (ie. FOG). */
        private List<Filter>          filters         = new ArrayList<Filter>();
        /**
         * The background color of the render loaded from the horizon color of the world. If no world is used than the gray color
         * is set to default (as in blender editor.
         */
        private ColorRGBA             backgroundColor = ColorRGBA.Gray;
    }
}
