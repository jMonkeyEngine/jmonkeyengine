/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.BlenderKey;
import com.jme3.asset.BlenderKey.LoadingResults;
import com.jme3.asset.ModelKey;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.data.FileBlockHeader;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.helpers.ArmatureHelper;
import com.jme3.scene.plugins.blender.helpers.CameraHelper;
import com.jme3.scene.plugins.blender.helpers.ConstraintHelper;
import com.jme3.scene.plugins.blender.helpers.CurvesHelper;
import com.jme3.scene.plugins.blender.helpers.IpoHelper;
import com.jme3.scene.plugins.blender.helpers.LightHelper;
import com.jme3.scene.plugins.blender.helpers.MaterialHelper;
import com.jme3.scene.plugins.blender.helpers.MeshHelper;
import com.jme3.scene.plugins.blender.helpers.ModifierHelper;
import com.jme3.scene.plugins.blender.helpers.NoiseHelper;
import com.jme3.scene.plugins.blender.helpers.ObjectHelper;
import com.jme3.scene.plugins.blender.helpers.ParticlesHelper;
import com.jme3.scene.plugins.blender.helpers.TextureHelper;
import com.jme3.scene.plugins.blender.utils.BlenderInputStream;
import com.jme3.scene.plugins.blender.utils.DataRepository;
import com.jme3.scene.plugins.blender.utils.JmeConverter;

/**
 * This is the main loading class. Have in notice that asset manager needs to have loaders for resources like textures.
 * @author Marcin Roguski
 */
public class BlenderModelLoader implements AssetLoader {
	private static final Logger	LOGGER	= Logger.getLogger(BlenderModelLoader.class.getName());

	@Override
	public Spatial load(AssetInfo assetInfo) throws IOException {
		try {
			//registering loaders
			ModelKey modelKey = (ModelKey)assetInfo.getKey();
			BlenderKey blenderKey;
			if(modelKey instanceof BlenderKey) {
				blenderKey = (BlenderKey)modelKey;
			} else {
				blenderKey = new BlenderKey(modelKey.getName());
				blenderKey.setAssetRootPath(modelKey.getFolder());
			}

			//opening stream
			BlenderInputStream inputStream = new BlenderInputStream(assetInfo.openStream(), assetInfo.getManager());
			List<FileBlockHeader> blocks = new ArrayList<FileBlockHeader>();
			FileBlockHeader fileBlock;
			DataRepository dataRepository = new DataRepository();
			dataRepository.setAssetManager(assetInfo.getManager());
			dataRepository.setInputStream(inputStream);
			dataRepository.setBlenderKey(blenderKey);
			dataRepository.putHelper(ArmatureHelper.class, new ArmatureHelper(inputStream.getVersionNumber()));
			dataRepository.putHelper(TextureHelper.class, new TextureHelper(inputStream.getVersionNumber()));
			dataRepository.putHelper(MeshHelper.class, new MeshHelper(inputStream.getVersionNumber()));
			dataRepository.putHelper(ObjectHelper.class, new ObjectHelper(inputStream.getVersionNumber()));
			dataRepository.putHelper(CurvesHelper.class, new CurvesHelper(inputStream.getVersionNumber()));
			dataRepository.putHelper(LightHelper.class, new LightHelper(inputStream.getVersionNumber()));
			dataRepository.putHelper(CameraHelper.class, new CameraHelper(inputStream.getVersionNumber()));
			dataRepository.putHelper(ModifierHelper.class, new ModifierHelper(inputStream.getVersionNumber()));
			dataRepository.putHelper(MaterialHelper.class, new MaterialHelper(inputStream.getVersionNumber()));
			dataRepository.putHelper(ConstraintHelper.class, new ConstraintHelper(inputStream.getVersionNumber(), dataRepository));
			dataRepository.putHelper(IpoHelper.class, new IpoHelper(inputStream.getVersionNumber()));
			dataRepository.putHelper(NoiseHelper.class, new NoiseHelper(inputStream.getVersionNumber()));
			dataRepository.putHelper(ParticlesHelper.class, new ParticlesHelper(inputStream.getVersionNumber()));
			
			//setting additional data to helpers
			if(blenderKey.isFixUpAxis()) {
				ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
				objectHelper.setyIsUpAxis(true);
				CurvesHelper curvesHelper = dataRepository.getHelper(CurvesHelper.class);
				curvesHelper.setyIsUpAxis(true);
			}
			MaterialHelper materialHelper = dataRepository.getHelper(MaterialHelper.class);
			materialHelper.setFaceCullMode(blenderKey.getFaceCullMode());

			//reading the blocks (dna block is automatically saved in the data repository when found)//TODO: zmienić to
			do {
				fileBlock = new FileBlockHeader(inputStream, dataRepository);
				if(!fileBlock.isDnaBlock()) {
					blocks.add(fileBlock);
				}
			} while(!fileBlock.isLastBlock());

			JmeConverter converter = new JmeConverter(dataRepository);
			LoadingResults loadingResults = blenderKey.prepareLoadingResults();
			for(FileBlockHeader block : blocks) {
				if(block.getCode() == FileBlockHeader.BLOCK_OB00) {
					Object object = converter.toObject(block.getStructure(dataRepository));
					if(object instanceof Node) {
						LOGGER.log(Level.INFO, ((Node)object).getName() + ": " + ((Node)object).getLocalTranslation().toString() + "--> " + (((Node)object).getParent() == null ? "null" : ((Node)object).getParent().getName()));
						if(((Node)object).getParent() == null) {
							loadingResults.addObject((Node)object);
						}
					}
				}
			}
			inputStream.close();
			List<Node> objects = loadingResults.getObjects();
			if(objects.size() > 0) {
				Node modelNode = new Node(blenderKey.getName());
				for(Iterator<Node> it = objects.iterator(); it.hasNext();) {
					Node node = it.next();
					modelNode.attachChild(node);
				}
				return modelNode;
			} else if(objects.size() == 1) {
				return objects.get(0);
			}
		} catch(BlenderFileException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}
}
