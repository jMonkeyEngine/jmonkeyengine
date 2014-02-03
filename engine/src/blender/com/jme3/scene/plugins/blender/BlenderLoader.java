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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.BlenderKey;
import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.asset.BlenderKey.LoadingResults;
import com.jme3.asset.ModelKey;
import com.jme3.light.Light;
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
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.landscape.LandscapeHelper;
import com.jme3.scene.plugins.blender.lights.LightHelper;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.scene.plugins.blender.meshes.MeshHelper;
import com.jme3.scene.plugins.blender.modifiers.ModifierHelper;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;
import com.jme3.scene.plugins.blender.particles.ParticlesHelper;
import com.jme3.scene.plugins.blender.textures.TextureHelper;

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

    public Spatial load(AssetInfo assetInfo) throws IOException {
        try {
            this.setup(assetInfo);

            List<FileBlockHeader> sceneBlocks = new ArrayList<FileBlockHeader>();
            BlenderKey blenderKey = blenderContext.getBlenderKey();
            LoadingResults loadingResults = blenderKey.prepareLoadingResults();
            
            AnimationHelper animationHelper = blenderContext.getHelper(AnimationHelper.class);
            animationHelper.loadAnimations();
            
            for (FileBlockHeader block : blocks) {
                switch (block.getCode()) {
                    case FileBlockHeader.BLOCK_OB00:// Object
                        ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
                        Object object = objectHelper.toObject(block.getStructure(blenderContext), blenderContext);
                        if (object instanceof LightNode) {
                            loadingResults.addLight((LightNode) object);
                        } else if (object instanceof CameraNode) {
                            loadingResults.addCamera((CameraNode) object);
                        } else if (object instanceof Node) {
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.log(Level.FINE, "{0}: {1}--> {2}", new Object[] { ((Node) object).getName(), ((Node) object).getLocalTranslation().toString(), ((Node) object).getParent() == null ? "null" : ((Node) object).getParent().getName() });
                            }
                            if (this.isRootObject(loadingResults, (Node) object)) {
                                loadingResults.addObject((Node) object);
                            }
                        }
                        break;
//                    case FileBlockHeader.BLOCK_MA00:// Material
//                        MaterialHelper materialHelper = blenderContext.getHelper(MaterialHelper.class);
//                        MaterialContext materialContext = materialHelper.toMaterialContext(block.getStructure(blenderContext), blenderContext);
//                        if (blenderKey.isLoadUnlinkedAssets() && blenderKey.shouldLoad(FeaturesToLoad.MATERIALS)) {
//                            loadingResults.addMaterial(this.toMaterial(block.getStructure(blenderContext)));
//                        }
//                        break;
                    case FileBlockHeader.BLOCK_SC00:// Scene
                        if (blenderKey.shouldLoad(FeaturesToLoad.SCENES)) {
                            sceneBlocks.add(block);
                        }
                        break;
                    case FileBlockHeader.BLOCK_WO00:// World
                        if (blenderKey.shouldLoad(FeaturesToLoad.WORLD)) {
                            Structure worldStructure = block.getStructure(blenderContext);
                            String worldName = worldStructure.getName();
                            if (blenderKey.getUsedWorld() == null || blenderKey.getUsedWorld().equals(worldName)) {
                                LandscapeHelper landscapeHelper = blenderContext.getHelper(LandscapeHelper.class);
                                Light ambientLight = landscapeHelper.toAmbientLight(worldStructure);
                                loadingResults.addLight(new LightNode(null, ambientLight));
                                loadingResults.setSky(landscapeHelper.toSky(worldStructure));
                                loadingResults.setBackgroundColor(landscapeHelper.toBackgroundColor(worldStructure));
                            }
                        }
                        break;
                }
            }

            // bake constraints after everything is loaded
            ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
            constraintHelper.bakeConstraints(blenderContext);

            // load the scene at the very end so that the root nodes have no parent during loading or constraints applying
            for (FileBlockHeader sceneBlock : sceneBlocks) {
                loadingResults.addScene(this.toScene(sceneBlock.getStructure(blenderContext)));
            }

            return loadingResults;
        } catch (BlenderFileException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        } catch (Exception e) {
            throw new IOException("Unexpected importer exception occured: " + e.getLocalizedMessage(), e);
        } finally {
            this.clear();
        }
    }

    /**
     * This method indicates if the given spatial is a root object. It means it
     * has no parent or is directly attached to one of the already loaded scene
     * nodes.
     * 
     * @param loadingResults
     *            loading results containing the scene nodes
     * @param spatial
     *            spatial object
     * @return <b>true</b> if the given spatial is a root object and
     *         <b>false</b> otherwise
     */
    protected boolean isRootObject(LoadingResults loadingResults, Spatial spatial) {
        if (spatial.getParent() == null) {
            return true;
        }
        for (Node scene : loadingResults.getScenes()) {
            if (spatial.getParent().equals(scene)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method converts the given structure to a scene node.
     * @param structure
     *            structure of a scene
     * @return scene's node
     */
    private Node toScene(Structure structure) {
        ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
        Node result = new Node(structure.getName());
        try {
            List<Structure> base = ((Structure) structure.getFieldValue("base")).evaluateListBase();
            for (Structure b : base) {
                Pointer pObject = (Pointer) b.getFieldValue("object");
                if (pObject.isNotNull()) {
                    Structure objectStructure = pObject.fetchData().get(0);

                    Object object = objectHelper.toObject(objectStructure, blenderContext);
                    if (object instanceof LightNode) {
                        result.addLight(((LightNode) object).getLight());
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
        } catch (BlenderFileException e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
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
                if (fileBlock.getCode() == FileBlockHeader.BLOCK_SC00) {
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
}
