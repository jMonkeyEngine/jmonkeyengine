/*
 * Copyright (c) 2009-2014 jMonkeyEngine
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
package com.jme3.scene.plugins.fbx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.plugins.fbx.AnimationList.AnimInverval;
import com.jme3.scene.plugins.fbx.file.FBXElement;
import com.jme3.scene.plugins.fbx.file.FBXFile;
import com.jme3.scene.plugins.fbx.file.FBXReader;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;

/**
 * FBX file format loader
 * <p> Loads scene meshes, materials, textures, skeleton and skeletal animation.
 * Multiple animations can be defined with {@link AnimationList} passing into {@link SceneKey}.</p>
 * 
 * @author Aleksandra Menshchikova
 */
public class SceneLoader implements AssetLoader {
	
	private static final Logger logger = Logger.getLogger(SceneLoader.class.getName());
	
	private AssetManager assetManager;
	private AnimationList animList;
	
	private String sceneName;
	private String sceneFilename;
	private String sceneFolderName;
	private float unitSize;
	private float animFrameRate;
	private final double secondsPerUnit = 1 / 46186158000d; // Animation speed factor
	
	// Loaded objects data
	private Map<Long, MeshData> meshDataMap = new HashMap<Long, MeshData>();
	private Map<Long, MaterialData> matDataMap = new HashMap<Long, MaterialData>();
	private Map<Long, TextureData> texDataMap = new HashMap<Long, TextureData>();
	private Map<Long, ImageData> imgDataMap = new HashMap<Long, ImageData>(); // Video clips
	private Map<Long, ModelData> modelDataMap = new HashMap<Long, ModelData>(); // Mesh nodes and limb nodes
	private Map<Long, BindPoseData> poseDataMap = new HashMap<Long, BindPoseData>(); // Node bind poses
	private Map<Long, SkinData> skinMap = new HashMap<Long, SkinData>(); // Skin for bone clusters
	private Map<Long, ClusterData> clusterMap = new HashMap<Long, ClusterData>(); // Bone skinning cluster
	private Map<Long, AnimCurveData> acurveMap = new HashMap<Long, AnimCurveData>(); // Animation curves
	private Map<Long, AnimNode> anodeMap = new HashMap<Long, AnimNode>(); // Animation nodes
	private Map<Long, AnimLayer> alayerMap = new HashMap<Long, AnimLayer>(); // Amination layers
	private Map<Long, List<Long>> refMap = new HashMap<Long, List<Long>>(); // Object links
	private Map<Long, List<PropertyLink>> propMap = new HashMap<Long, List<PropertyLink>>(); // Property links
	
	// Scene objects
	private Map<Long, Node> modelMap = new HashMap<Long, Node>(); // Mesh nodes
	private Map<Long, Limb> limbMap = new HashMap<Long, Limb>(); // Bones
	private Map<Long, BindPose> bindMap = new HashMap<Long, BindPose>(); // Node bind poses
	private Map<Long, Geometry> geomMap = new HashMap<Long, Geometry>(); // Mesh geometries
	private Map<Long, Material> matMap = new HashMap<Long, Material>();
	private Map<Long, Texture> texMap = new HashMap<Long, Texture>();
	private Map<Long, Image> imgMap = new HashMap<Long, Image>();
	private Skeleton skeleton;
	private AnimControl animControl;
	
	@Override
	public Object load(AssetInfo assetInfo) throws IOException {
		this.assetManager = assetInfo.getManager();
		AssetKey<?> assetKey = assetInfo.getKey();
		if(assetKey instanceof SceneKey)
			animList = ((SceneKey) assetKey).getAnimations();
		else if(!(assetKey instanceof ModelKey))
			throw new AssetLoadException("Invalid asset key");
		InputStream stream = assetInfo.openStream();
		Node sceneNode = null;
		try {
			sceneFilename = assetKey.getName();
			sceneFolderName = assetKey.getFolder();
			String ext = assetKey.getExtension();
			sceneName = sceneFilename.substring(0, sceneFilename.length() - ext.length() - 1);
			if(sceneFolderName != null && sceneFolderName.length() > 0)
				sceneName = sceneName.substring(sceneFolderName.length());
			reset();
			loadScene(stream);
			sceneNode = linkScene();
		} finally {
			releaseObjects();
			if(stream != null) {
				stream.close();
			}
		}
		return sceneNode;
	}
	
	private void reset() {
		unitSize = 1;
		animFrameRate = 30;
	}
	
	private void loadScene(InputStream stream) throws IOException {
		logger.log(Level.FINE, "Loading scene {0}", sceneFilename);
		long startTime = System.currentTimeMillis();
		FBXFile scene = FBXReader.readFBX(stream);
		for(FBXElement e : scene.rootElements) {
			if(e.id.equals("GlobalSettings"))
				loadGlobalSettings(e);
			else if(e.id.equals("Objects"))
				loadObjects(e);
			else if(e.id.equals("Connections"))
				loadConnections(e);
		}
		long estimatedTime = System.currentTimeMillis() - startTime;
		logger.log(Level.FINE, "Loading done in {0} ms", estimatedTime);
	}
	
	private void loadGlobalSettings(FBXElement element) {
		for(FBXElement e : element.children) {
			if(e.id.equals("Properties70")) {
				for(FBXElement e2 : e.children) {
					if(e2.id.equals("P")) {
						String propName = (String) e2.properties.get(0);
						if(propName.equals("UnitScaleFactor"))
							this.unitSize = ((Double) e2.properties.get(4)).floatValue();
						else if(propName.equals("CustomFrameRate")) {
							float framerate = ((Double) e2.properties.get(4)).floatValue();
							if(framerate != -1)
								this.animFrameRate = framerate;
						}
					}
				}
			}
		}
	}
	
	private void loadObjects(FBXElement element) {
		for(FBXElement e : element.children) {
			if(e.id.equals("Geometry"))
				loadGeometry(e);
			else if(e.id.equals("Material"))
				loadMaterial(e);
			else if(e.id.equals("Model"))
				loadModel(e);
			else if(e.id.equals("Pose"))
				loadPose(e);
			else if(e.id.equals("Texture"))
				loadTexture(e);
			else if(e.id.equals("Video"))
				loadImage(e);
			else if(e.id.equals("Deformer"))
				loadDeformer(e);
			else if(e.id.equals("AnimationLayer"))
				loadAnimLayer(e);
			else if(e.id.equals("AnimationCurve"))
				loadAnimCurve(e);
			else if(e.id.equals("AnimationCurveNode"))
				loadAnimNode(e);
		}
	}
	
	private void loadGeometry(FBXElement element) {
		long id = (Long) element.properties.get(0);
		String type = (String) element.properties.get(2);
		if(type.equals("Mesh")) {
			MeshData data = new MeshData();
			for(FBXElement e : element.children) {
				if(e.id.equals("Vertices"))
					data.vertices = (double[]) e.properties.get(0);
				else if(e.id.equals("PolygonVertexIndex"))
					data.indices = (int[]) e.properties.get(0);
				// TODO edges are not used now
				//else if(e.id.equals("Edges"))
				//	data.edges = (int[]) e.properties.get(0);
				else if(e.id.equals("LayerElementNormal"))
					for(FBXElement e2 : e.children) {
						if(e2.id.equals("MappingInformationType")) {
							data.normalsMapping = (String) e2.properties.get(0);
							if(!data.normalsMapping.equals("ByVertice") && !data.normalsMapping.equals("ByPolygonVertex"))
								throw new AssetLoadException("Not supported LayerElementNormal.MappingInformationType = " + data.normalsMapping);
						} else if(e2.id.equals("ReferenceInformationType")) {
							data.normalsReference = (String) e2.properties.get(0);
							if(!data.normalsReference.equals("Direct"))
								throw new AssetLoadException("Not supported LayerElementNormal.ReferenceInformationType = " + data.normalsReference);
						} else if(e2.id.equals("Normals"))
							data.normals = (double[]) e2.properties.get(0);
					}
				else if(e.id.equals("LayerElementTangent"))
					for(FBXElement e2 : e.children) {
						if(e2.id.equals("MappingInformationType")) {
							data.tangentsMapping = (String) e2.properties.get(0);
							if(!data.tangentsMapping.equals("ByVertice") && !data.tangentsMapping.equals("ByPolygonVertex"))
								throw new AssetLoadException("Not supported LayerElementTangent.MappingInformationType = " + data.tangentsMapping);
						} else if(e2.id.equals("ReferenceInformationType")) {
							data.tangentsReference = (String) e2.properties.get(0);
							if(!data.tangentsReference.equals("Direct"))
								throw new AssetLoadException("Not supported LayerElementTangent.ReferenceInformationType = " + data.tangentsReference);
						} else if(e2.id.equals("Tangents"))
							data.tangents = (double[]) e2.properties.get(0);
					}
				else if(e.id.equals("LayerElementBinormal"))
					for(FBXElement e2 : e.children) {
						if(e2.id.equals("MappingInformationType")) {
							data.binormalsMapping = (String) e2.properties.get(0);
							if(!data.binormalsMapping.equals("ByVertice") && !data.binormalsMapping.equals("ByPolygonVertex"))
								throw new AssetLoadException("Not supported LayerElementBinormal.MappingInformationType = " + data.binormalsMapping);
						} else if(e2.id.equals("ReferenceInformationType")) {
							data.binormalsReference = (String) e2.properties.get(0);
							if(!data.binormalsReference.equals("Direct"))
								throw new AssetLoadException("Not supported LayerElementBinormal.ReferenceInformationType = " + data.binormalsReference);
						} else if(e2.id.equals("Tangents"))
							data.binormals = (double[]) e2.properties.get(0);
					}
				else if(e.id.equals("LayerElementUV"))
					for(FBXElement e2 : e.children) {
						if(e2.id.equals("MappingInformationType")) {
							data.uvMapping = (String) e2.properties.get(0);
							if(!data.uvMapping.equals("ByPolygonVertex"))
								throw new AssetLoadException("Not supported LayerElementUV.MappingInformationType = " + data.uvMapping);
						} else if(e2.id.equals("ReferenceInformationType")) {
							data.uvReference = (String) e2.properties.get(0);
							if(!data.uvReference.equals("IndexToDirect"))
								throw new AssetLoadException("Not supported LayerElementUV.ReferenceInformationType = " + data.uvReference);
						} else if(e2.id.equals("UV"))
							data.uv = (double[]) e2.properties.get(0);
						else if(e2.id.equals("UVIndex"))
							data.uvIndex = (int[]) e2.properties.get(0);
					}
				// TODO smoothing is not used now
				//else if(e.id.equals("LayerElementSmoothing"))
				//	for(FBXElement e2 : e.children) {
				//		if(e2.id.equals("MappingInformationType")) {
				//			data.smoothingMapping = (String) e2.properties.get(0);
				//			if(!data.smoothingMapping.equals("ByEdge"))
				//				throw new AssetLoadException("Not supported LayerElementSmoothing.MappingInformationType = " + data.smoothingMapping);
				//		} else if(e2.id.equals("ReferenceInformationType")) {
				//			data.smoothingReference = (String) e2.properties.get(0);
				//			if(!data.smoothingReference.equals("Direct"))
				//				throw new AssetLoadException("Not supported LayerElementSmoothing.ReferenceInformationType = " + data.smoothingReference);
				//		} else if(e2.id.equals("Smoothing"))
				//			data.smoothing = (int[]) e2.properties.get(0);
				//	}
				else if(e.id.equals("LayerElementMaterial"))
					for(FBXElement e2 : e.children) {
						if(e2.id.equals("MappingInformationType")) {
							data.materialsMapping = (String) e2.properties.get(0);
							if(!data.materialsMapping.equals("AllSame"))
								throw new AssetLoadException("Not supported LayerElementMaterial.MappingInformationType = " + data.materialsMapping);
						} else if(e2.id.equals("ReferenceInformationType")) {
							data.materialsReference = (String) e2.properties.get(0);
							if(!data.materialsReference.equals("IndexToDirect"))
								throw new AssetLoadException("Not supported LayerElementMaterial.ReferenceInformationType = " + data.materialsReference);
						} else if(e2.id.equals("Materials"))
							data.materials = (int[]) e2.properties.get(0);
					}
			}
			meshDataMap.put(id, data);
		}
	}
	
	private void loadMaterial(FBXElement element) {
		long id = (Long) element.properties.get(0);
		String path = (String) element.properties.get(1);
		String type = (String) element.properties.get(2);
		if(type.equals("")) {
			MaterialData data = new MaterialData();
			data.name = path.substring(0, path.indexOf(0));
			for(FBXElement e : element.children) {
				if(e.id.equals("ShadingModel")) {
					data.shadingModel = (String) e.properties.get(0);
				} else if(e.id.equals("Properties70")) {
					for(FBXElement e2 : e.children) {
						if(e2.id.equals("P")) {
							String propName = (String) e2.properties.get(0);
							if(propName.equals("AmbientColor")) {
								double x = (Double) e2.properties.get(4);
								double y = (Double) e2.properties.get(5);
								double z = (Double) e2.properties.get(6);
								data.ambientColor.set((float) x, (float) y, (float) z);
							} else if(propName.equals("AmbientFactor")) {
								double x = (Double) e2.properties.get(4);
								data.ambientFactor = (float) x;
							} else if(propName.equals("DiffuseColor")) {
								double x = (Double) e2.properties.get(4);
								double y = (Double) e2.properties.get(5);
								double z = (Double) e2.properties.get(6);
								data.diffuseColor.set((float) x, (float) y, (float) z);
							} else if(propName.equals("DiffuseFactor")) {
								double x = (Double) e2.properties.get(4);
								data.diffuseFactor = (float) x;
							} else if(propName.equals("SpecularColor")) {
								double x = (Double) e2.properties.get(4);
								double y = (Double) e2.properties.get(5);
								double z = (Double) e2.properties.get(6);
								data.specularColor.set((float) x, (float) y, (float) z);
							} else if(propName.equals("Shininess") || propName.equals("ShininessExponent")) {
								double x = (Double) e2.properties.get(4);
								data.shininessExponent = (float) x;
							}
						}
					}
				}
			}
			matDataMap.put(id, data);
		}
	}
	
	private void loadModel(FBXElement element) {
		long id = (Long) element.properties.get(0);
		String path = (String) element.properties.get(1);
		String type = (String) element.properties.get(2);
		ModelData data = new ModelData();
		data.name = path.substring(0, path.indexOf(0));
		data.type = type;
		for(FBXElement e : element.children) {
			if(e.id.equals("Properties70")) {
				for(FBXElement e2 : e.children) {
					if(e2.id.equals("P")) {
						String propName = (String) e2.properties.get(0);
						if(propName.equals("Lcl Translation")) {
							double x = (Double) e2.properties.get(4);
							double y = (Double) e2.properties.get(5);
							double z = (Double) e2.properties.get(6);
							data.localTranslation.set((float) x, (float) y, (float) z).divideLocal(unitSize);
						} else if(propName.equals("Lcl Rotation")) {
							double x = (Double) e2.properties.get(4);
							double y = (Double) e2.properties.get(5);
							double z = (Double) e2.properties.get(6);
							data.localRotation.fromAngles((float) x * FastMath.DEG_TO_RAD, (float) y * FastMath.DEG_TO_RAD, (float) z * FastMath.DEG_TO_RAD);
						} else if(propName.equals("Lcl Scaling")) {
							double x = (Double) e2.properties.get(4);
							double y = (Double) e2.properties.get(5);
							double z = (Double) e2.properties.get(6);
							data.localScale.set((float) x, (float) y, (float) z).multLocal(unitSize);
						} else if(propName.equals("PreRotation")) {
							double x = (Double) e2.properties.get(4);
							double y = (Double) e2.properties.get(5);
							double z = (Double) e2.properties.get(6);
							data.preRotation = quatFromBoneAngles((float) x * FastMath.DEG_TO_RAD, (float) y * FastMath.DEG_TO_RAD, (float) z * FastMath.DEG_TO_RAD);
						}
					}
				}
			}
		}
		modelDataMap.put(id, data);
	}
	
	private void loadPose(FBXElement element) {
		long id = (Long) element.properties.get(0);
		String path = (String) element.properties.get(1);
		String type = (String) element.properties.get(2);
		if(type.equals("BindPose")) {
			BindPoseData data = new BindPoseData();
			data.name = path.substring(0, path.indexOf(0));
			for(FBXElement e : element.children) {
				if(e.id.equals("PoseNode")) {
					NodeTransformData item = new NodeTransformData();
					for(FBXElement e2 : e.children) {
						if(e2.id.equals("Node"))
							item.nodeId = (Long) e2.properties.get(0);
						else if(e2.id.equals("Matrix"))
							item.transform = (double[]) e2.properties.get(0);
					}
					data.list.add(item);
				}
			}
			poseDataMap.put(id, data);
		}
	}
	
	private void loadTexture(FBXElement element) {
		long id = (Long) element.properties.get(0);
		String path = (String) element.properties.get(1);
		String type = (String) element.properties.get(2);
		if(type.equals("")) {
			TextureData data = new TextureData();
			data.name = path.substring(0, path.indexOf(0));
			for(FBXElement e : element.children) {
				if(e.id.equals("Type"))
					data.bindType = (String) e.properties.get(0);
				else if(e.id.equals("FileName"))
					data.filename = (String) e.properties.get(0);
			}
			texDataMap.put(id, data);
		}
	}
	
	private void loadImage(FBXElement element) {
		long id = (Long) element.properties.get(0);
		String path = (String) element.properties.get(1);
		String type = (String) element.properties.get(2);
		if(type.equals("Clip")) {
			ImageData data = new ImageData();
			data.name = path.substring(0, path.indexOf(0));
			for(FBXElement e : element.children) {
				if(e.id.equals("Type"))
					data.type = (String) e.properties.get(0);
				else if(e.id.equals("Filename"))
					data.filename = (String) e.properties.get(0);
				else if(e.id.equals("RelativeFilename"))
					data.relativeFilename = (String) e.properties.get(0);
				else if(e.id.equals("Content")) {
					if(e.properties.size() > 0)
						data.content = (byte[]) e.properties.get(0);
				}
			}
			imgDataMap.put(id, data);
		}
	}
	
	private void loadDeformer(FBXElement element) {
		long id = (Long) element.properties.get(0);
		String type = (String) element.properties.get(2);
		if(type.equals("Skin")) {
			SkinData skinData = new SkinData();
			for(FBXElement e : element.children) {
				if(e.id.equals("SkinningType"))
					skinData.type = (String) e.properties.get(0);
			}
			skinMap.put(id, skinData);
		} else if(type.equals("Cluster")) {
			ClusterData clusterData = new ClusterData();
			for(FBXElement e : element.children) {
				if(e.id.equals("Indexes"))
					clusterData.indexes = (int[]) e.properties.get(0);
				else if(e.id.equals("Weights"))
					clusterData.weights = (double[]) e.properties.get(0);
				else if(e.id.equals("Transform"))
					clusterData.transform = (double[]) e.properties.get(0);
				else if(e.id.equals("TransformLink"))
					clusterData.transformLink = (double[]) e.properties.get(0);
			}
			clusterMap.put(id, clusterData);
		}
	}
	
	private void loadAnimLayer(FBXElement element) {
		long id = (Long) element.properties.get(0);
		String path = (String) element.properties.get(1);
		String type = (String) element.properties.get(2);
		if(type.equals("")) {
			AnimLayer layer = new AnimLayer();
			layer.name = path.substring(0, path.indexOf(0));
			alayerMap.put(id, layer);
		}
	}
	
	private void loadAnimCurve(FBXElement element) {
		long id = (Long) element.properties.get(0);
		String type = (String) element.properties.get(2);
		if(type.equals("")) {
			AnimCurveData data = new AnimCurveData();
			for(FBXElement e : element.children) {
				if(e.id.equals("KeyTime"))
					data.keyTimes = (long[]) e.properties.get(0);
				else if(e.id.equals("KeyValueFloat"))
					data.keyValues = (float[]) e.properties.get(0);
			}
			acurveMap.put(id, data);
		}
	}
	
	private void loadAnimNode(FBXElement element) {
		long id = (Long) element.properties.get(0);
		String path = (String) element.properties.get(1);
		String type = (String) element.properties.get(2);
		if(type.equals("")) {
			Double x = null, y = null, z = null;
			for(FBXElement e : element.children) {
				if(e.id.equals("Properties70")) {
					for(FBXElement e2 : e.children) {
						if(e2.id.equals("P")) {
							String propName = (String) e2.properties.get(0);
							if(propName.equals("d|X"))
								x = (Double) e2.properties.get(4);
							else if(propName.equals("d|Y"))
								y = (Double) e2.properties.get(4);
							else if(propName.equals("d|Z"))
								z = (Double) e2.properties.get(4);
						}
					}
				}
			}
			// Load only T R S curve nodes
			if(x != null && y != null && z != null) {
				AnimNode node = new AnimNode();
				node.value = new Vector3f(x.floatValue(), y.floatValue(), z.floatValue());
				node.name = path.substring(0, path.indexOf(0));
				anodeMap.put(id, node);
			}
		}
	}
	
	private void loadConnections(FBXElement element) {
		for(FBXElement e : element.children) {
			if(e.id.equals("C")) {
				String type = (String) e.properties.get(0);
				long objId, refId;
				if(type.equals("OO")) {
					objId = (Long) e.properties.get(1);
					refId = (Long) e.properties.get(2);
					List<Long> links = refMap.get(objId);
					if(links == null) {
						links = new ArrayList<Long>();
						refMap.put(objId, links);
					}
					links.add(refId);
				} else if(type.equals("OP")) {
					objId = (Long) e.properties.get(1);
					refId = (Long) e.properties.get(2);
					String propName = (String) e.properties.get(3);
					List<PropertyLink> props = propMap.get(objId);
					if(props == null) {
						props = new ArrayList<PropertyLink>();
						propMap.put(objId, props);
					}
					props.add(new PropertyLink(refId, propName));
				}
			}
		}
	}
	
	private Geometry createGeomerty(MeshData data) {
		Mesh mesh = new Mesh();
		mesh.setMode(Mode.Triangles);
		// Since each vertex should contain unique texcoord and normal we should unroll vertex indexing
		// So we don't use VertexBuffer.Type.Index for elements drawing
		// Moreover quads should be triangulated (this increases number of vertices)
		boolean isQuads = false;
		if(data.indices != null) {
			data.iCount = data.indices.length;
			data.srcVertexCount = data.vertices.length / 3;
			// Indices contains negative numbers to define polygon last index
			// Check indices strides to be sure we have triangles or quads
			boolean allTriangles = true;
			boolean allQads = true;
			for(int i = 0; i < data.indices.length; ++i) {
				if(i % 3 == 2) { // Triangle stride
					if(data.indices[i] >= 0)
						allTriangles = false;
				} else {
					if(data.indices[i] < 0)
						allTriangles = false;
				}
				if(i % 4 == 3) { // Quad stride
					if(data.indices[i] >= 0)
						allQads = false;
				} else {
					if(data.indices[i] < 0)
						allQads = false;
				}
			}
			if(allTriangles) {
				isQuads = false;
				data.vCount = data.iCount;
			} else if(allQads) {
				isQuads = true;
				data.vCount = 6 * (data.iCount / 4); // Each quad will be splited into two triangles
			} else
				throw new AssetLoadException("Unsupported PolygonVertexIndex stride");
			data.vertexMap = new int[data.vCount];
			data.indexMap = new int[data.vCount];
			// Unroll index array into vertex mapping
			int n = 0;
			for(int i = 0; i < data.iCount; ++i) {
				int index = data.indices[i];
				if(index < 0) {
					int lastIndex = -(index + 1);
					if(isQuads) {
						data.vertexMap[n + 0] = data.indices[i - 3];
						data.vertexMap[n + 1] = data.indices[i - 2];
						data.vertexMap[n + 2] = data.indices[i - 1];
						data.vertexMap[n + 3] = data.indices[i - 3];
						data.vertexMap[n + 4] = data.indices[i - 1];
						data.vertexMap[n + 5] = lastIndex;
						data.indexMap[n + 0] = (i - 3);
						data.indexMap[n + 1] = (i - 2);
						data.indexMap[n + 2] = (i - 1);
						data.indexMap[n + 3] = (i - 3);
						data.indexMap[n + 4] = (i - 1);
						data.indexMap[n + 5] = (i - 0);
						n += 6;
					} else {
						data.vertexMap[n + 0] = data.indices[i - 2];
						data.vertexMap[n + 1] = data.indices[i - 1];
						data.vertexMap[n + 2] = lastIndex;
						data.indexMap[n + 0] = (i - 2);
						data.indexMap[n + 1] = (i - 1);
						data.indexMap[n + 2] = (i - 0);
						n += 3;
					}
				}
			}
			// Build reverse vertex mapping
			data.reverseVertexMap = new ArrayList<List<Integer>>(data.srcVertexCount);
			for(int i = 0; i < data.srcVertexCount; ++i)
				data.reverseVertexMap.add(new ArrayList<Integer>());
			for(int i = 0; i < data.vCount; ++i) {
				int index = data.vertexMap[i];
				data.reverseVertexMap.get(index).add(i);
			}
		} else {
			// Stub for no vertex indexing (direct mapping)
			data.iCount = data.vCount = data.srcVertexCount;
			data.vertexMap = new int[data.vCount];
			data.indexMap = new int[data.vCount];
			data.reverseVertexMap = new ArrayList<List<Integer>>(data.vCount);
			for(int i = 0; i < data.vCount; ++i) {
				data.vertexMap[i] = i;
				data.indexMap[i] = i;
				List<Integer> reverseIndices = new ArrayList<Integer>(1);
				reverseIndices.add(i);
				data.reverseVertexMap.add(reverseIndices);
			}
		}
		if(data.vertices != null) {
			// Unroll vertices data array
			FloatBuffer posBuf = BufferUtils.createFloatBuffer(data.vCount * 3);
			mesh.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
			int srcCount = data.vertices.length / 3;
			for(int i = 0; i < data.vCount; ++i) {
				int index = data.vertexMap[i];
				if(index > srcCount)
					throw new AssetLoadException("Invalid vertex mapping. Unexpected lookup vertex " + index + " from " + srcCount);
				float x = (float) data.vertices[3 * index + 0] / unitSize;
				float y = (float) data.vertices[3 * index + 1] / unitSize;
				float z = (float) data.vertices[3 * index + 2] / unitSize;
				posBuf.put(x).put(y).put(z);
			}
		}
		if(data.normals != null) {
			// Unroll normals data array
			FloatBuffer normBuf = BufferUtils.createFloatBuffer(data.vCount * 3);
			mesh.setBuffer(VertexBuffer.Type.Normal, 3, normBuf);
			int[] mapping = null;
			if(data.normalsMapping.equals("ByVertice"))
				mapping = data.vertexMap;
			else if(data.normalsMapping.equals("ByPolygonVertex"))
				mapping = data.indexMap;
			int srcCount = data.normals.length / 3;
			for(int i = 0; i < data.vCount; ++i) {
				int index = mapping[i];
				if(index > srcCount)
					throw new AssetLoadException("Invalid normal mapping. Unexpected lookup normal " + index + " from " + srcCount);
				float x = (float) data.normals[3 * index + 0];
				float y = (float) data.normals[3 * index + 1];
				float z = (float) data.normals[3 * index + 2];
				normBuf.put(x).put(y).put(z);
			}
		}
		if(data.tangents != null) {
			// Unroll normals data array
			FloatBuffer tanBuf = BufferUtils.createFloatBuffer(data.vCount * 4);
			mesh.setBuffer(VertexBuffer.Type.Tangent, 4, tanBuf);
			int[] mapping = null;
			if(data.tangentsMapping.equals("ByVertice"))
				mapping = data.vertexMap;
			else if(data.tangentsMapping.equals("ByPolygonVertex"))
				mapping = data.indexMap;
			int srcCount = data.tangents.length / 3;
			for(int i = 0; i < data.vCount; ++i) {
				int index = mapping[i];
				if(index > srcCount)
					throw new AssetLoadException("Invalid tangent mapping. Unexpected lookup tangent " + index + " from " + srcCount);
				float x = (float) data.tangents[3 * index + 0];
				float y = (float) data.tangents[3 * index + 1];
				float z = (float) data.tangents[3 * index + 2];
				tanBuf.put(x).put(y).put(z).put(-1.0f);
			}
		}
		if(data.binormals != null) {
			// Unroll normals data array
			FloatBuffer binormBuf = BufferUtils.createFloatBuffer(data.vCount * 3);
			mesh.setBuffer(VertexBuffer.Type.Binormal, 3, binormBuf);
			int[] mapping = null;
			if(data.binormalsMapping.equals("ByVertice"))
				mapping = data.vertexMap;
			else if(data.binormalsMapping.equals("ByPolygonVertex"))
				mapping = data.indexMap;
			int srcCount = data.binormals.length / 3;
			for(int i = 0; i < data.vCount; ++i) {
				int index = mapping[i];
				if(index > srcCount)
					throw new AssetLoadException("Invalid binormal mapping. Unexpected lookup binormal " + index + " from " + srcCount);
				float x = (float) data.binormals[3 * index + 0];
				float y = (float) data.binormals[3 * index + 1];
				float z = (float) data.binormals[3 * index + 2];
				binormBuf.put(x).put(y).put(z);
			}
		}
		if(data.uv != null) {
			int[] unIndexMap = data.vertexMap;
			if(data.uvIndex != null) {
				int uvIndexSrcCount = data.uvIndex.length;
				if(uvIndexSrcCount != data.iCount)
					throw new AssetLoadException("Invalid number of texcoord index data " + uvIndexSrcCount + " expected " + data.iCount);
				// Unroll UV index array
				unIndexMap = new int[data.vCount];
				int n = 0; 
				for(int i = 0; i < data.iCount; ++i) {
					int index = data.uvIndex[i];
					if(isQuads && (i % 4) == 3) {
						unIndexMap[n + 0] = data.uvIndex[i - 3];
						unIndexMap[n + 1] = data.uvIndex[i - 1];
						unIndexMap[n + 2] = index;
						n += 3;
					} else {
						unIndexMap[i] = index;
					}
				}
			}
			// Unroll UV data array
			FloatBuffer tcBuf = BufferUtils.createFloatBuffer(data.vCount * 2);
			mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, tcBuf);
			int srcCount = data.uv.length / 2;
			for(int i = 0; i < data.vCount; ++i) {
				int index = unIndexMap[i];
				if(index > srcCount)
					throw new AssetLoadException("Invalid texcoord mapping. Unexpected lookup texcoord " + index + " from " + srcCount);
				float u = (float) data.uv[2 * index + 0];
				float v = (float) data.uv[2 * index + 1];
				tcBuf.put(u).put(v);
			}
		}
		mesh.setStatic();
		mesh.updateBound();
		mesh.updateCounts();
		Geometry geom = new Geometry();
		geom.setMesh(mesh);
		return geom;
	}
	
	private Material createMaterial(MaterialData data) {
		Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		m.setName(data.name);
		data.ambientColor.multLocal(data.ambientFactor);
		data.diffuseColor.multLocal(data.diffuseFactor);
		data.specularColor.multLocal(data.specularFactor);
		m.setColor("Ambient", new ColorRGBA(data.ambientColor.x, data.ambientColor.y, data.ambientColor.z, 1));
		m.setColor("Diffuse", new ColorRGBA(data.diffuseColor.x, data.diffuseColor.y, data.diffuseColor.z, 1));
		m.setColor("Specular", new ColorRGBA(data.specularColor.x, data.specularColor.y, data.specularColor.z, 1));
		m.setFloat("Shininess", data.shininessExponent);
		m.setBoolean("UseMaterialColors", true);
		m.getAdditionalRenderState().setAlphaTest(true);
		m.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		return m;
	}
	
	private Node createNode(ModelData data) {
		Node model = new Node(data.name);
		model.setLocalTranslation(data.localTranslation);
		model.setLocalRotation(data.localRotation);
		model.setLocalScale(data.localScale);
		return model;
	}
	
	private Limb createLimb(ModelData data) {
		Limb limb = new Limb();
		limb.name = data.name;
		Quaternion rotation = data.preRotation.mult(data.localRotation);
		limb.bindTransform = new Transform(data.localTranslation, rotation, data.localScale);
		return limb;
	}
	
	private BindPose createPose(BindPoseData data) {
		BindPose pose = new BindPose();
		pose.name = data.name;
		for(NodeTransformData item : data.list) {
			Transform t = buildTransform(item.transform);
			//t.getTranslation().divideLocal(unitSize);
			t.getScale().multLocal(unitSize);
			pose.nodeTransforms.put(item.nodeId, t);
		}
		return pose;
	}
	
	private Texture createTexture(TextureData data) {
		Texture tex = new Texture2D();
		tex.setName(data.name);
		return tex;
	}
	
	private Image createImage(ImageData data) {
		Image image = null;
		if(data.filename != null) {
			// Try load by absolute path
			File file = new File(data.filename);
			if(file.exists() && file.isFile()) {
				File dir = new File(file.getParent());
				String locatorPath = dir.getAbsolutePath();
				Texture tex = null;
				try {
					assetManager.registerLocator(locatorPath, com.jme3.asset.plugins.FileLocator.class);
					tex = assetManager.loadTexture(file.getName());
				} catch(Exception e) {
				} finally {
					assetManager.unregisterLocator(locatorPath, com.jme3.asset.plugins.FileLocator.class);
				}
				if(tex != null)
					image = tex.getImage();
			}
		}
		if(image == null && data.relativeFilename != null) {
			// Try load by relative path
			File dir = new File(sceneFolderName);
			String locatorPath = dir.getAbsolutePath();
			Texture tex = null;
			try {
				assetManager.registerLocator(locatorPath, com.jme3.asset.plugins.FileLocator.class);
				tex = assetManager.loadTexture(data.relativeFilename);
			} catch(Exception e) {
			} finally {
				assetManager.unregisterLocator(locatorPath, com.jme3.asset.plugins.FileLocator.class);
			}
			if(tex != null)
				image = tex.getImage();
		}
		if(image == null && data.content != null) {
			// Try load from content
			String filename = null;
			if(data.filename != null)
				filename = new File(data.filename).getName();
			if(filename != null && data.relativeFilename != null)
				filename = data.relativeFilename;
			// Filename is required to aquire asset loader by extension
			if(filename != null) {
				String locatorPath = sceneFilename;
				filename = sceneFilename + File.separatorChar + filename; // Unique path
				Texture tex = null;
				try {
					assetManager.registerLocator(locatorPath, com.jme3.scene.plugins.fbx.ContentTextureLocator.class);
					tex = assetManager.loadTexture(new ContentTextureKey(filename, data.content));
				} catch(Exception e) {
				} finally {
					assetManager.unregisterLocator(locatorPath, com.jme3.scene.plugins.fbx.ContentTextureLocator.class);
				}
				if(tex != null)
					image = tex.getImage();
			}
		}
		if(image == null)
			throw new AssetLoadException("Content not loaded for image " + data.name);
		return image;
	}
	
	private Transform buildTransform(double[] transform) {
		float[] m = new float[transform.length];
		for(int i = 0; i < transform.length; ++i)
			m[i] = (float) transform[i];
		Matrix4f matrix = new Matrix4f(m);
		Vector3f pos = matrix.toTranslationVector();
		Quaternion rot = matrix.toRotationQuat();
		Vector3f scale = matrix.toScaleVector();
		return new Transform(pos, rot, scale);
	}
	
	private Quaternion quatFromBoneAngles(float xAngle, float yAngle, float zAngle) {
		float angle;
		float sinY, sinZ, sinX, cosY, cosZ, cosX;
		angle = zAngle * 0.5f;
		sinZ = FastMath.sin(angle);
		cosZ = FastMath.cos(angle);
		angle = yAngle * 0.5f;
		sinY = FastMath.sin(angle);
		cosY = FastMath.cos(angle);
		angle = xAngle * 0.5f;
		sinX = FastMath.sin(angle);
		cosX = FastMath.cos(angle);
		float cosYXcosZ = cosY * cosZ;
		float sinYXsinZ = sinY * sinZ;
		float cosYXsinZ = cosY * sinZ;
		float sinYXcosZ = sinY * cosZ;
		// For some reason bone space is differ, this is modified formulas
		float w = (cosYXcosZ * cosX + sinYXsinZ * sinX);
		float x = (cosYXcosZ * sinX - sinYXsinZ * cosX);
		float y = (sinYXcosZ * cosX + cosYXsinZ * sinX);
		float z = (cosYXsinZ * cosX - sinYXcosZ * sinX);
		return new Quaternion(x, y, z, w).normalizeLocal();
	}
	
	private Node linkScene() {
		logger.log(Level.FINE, "Linking scene objects");
		long startTime = System.currentTimeMillis();
		Node sceneNode = linkSceneNodes();
		linkMaterials();
		linkMeshes(sceneNode);
		linkSkins(sceneNode);
		linkAnimations();
		long estimatedTime = System.currentTimeMillis() - startTime;
		logger.log(Level.FINE, "Linking done in {0} ms", estimatedTime);
		return sceneNode;
	}
	
	private Node linkSceneNodes() {
		Node sceneNode = new Node(sceneName + "-scene");
		// Build mesh nodes
		for(long nodeId : modelDataMap.keySet()) {
			ModelData data = modelDataMap.get(nodeId);
			if(data.type.equals("Mesh")) {
				Node node = createNode(data);
				modelMap.put(nodeId, node);
			}
		}
		// Link model nodes into scene
		for(long modelId : modelMap.keySet()) {
			List<Long> refs = refMap.get(modelId);
			if(refs == null)
				continue;
			Node model = modelMap.get(modelId);
			for(long refId : refs) {
				Node rootNode = (refId != 0) ? modelMap.get(refId) : sceneNode;
				if(rootNode != null)
					rootNode.attachChild(model);
			}
		}
		// Build bind poses
		for(long poseId : poseDataMap.keySet()) {
			BindPoseData data = poseDataMap.get(poseId);
			BindPose pose = createPose(data);
			if(pose != null)
				bindMap.put(poseId, pose);
		}
		// Apply bind poses
		for(BindPose pose : bindMap.values()) {
			for(long nodeId : pose.nodeTransforms.keySet()) {
				Node model = modelMap.get(nodeId);
				if(model != null) {
					Transform t = pose.nodeTransforms.get(nodeId);
					model.setLocalTransform(t);
				}
			}
		}
		return sceneNode;
	}
	
	private void linkMaterials() {
		// Build materials
		for(long matId : matDataMap.keySet()) {
			MaterialData data = matDataMap.get(matId);
			Material material = createMaterial(data);
			if(material != null)
				matMap.put(matId, material);
		}
		// Build textures
		for(long texId : texDataMap.keySet()) {
			TextureData data = texDataMap.get(texId);
			Texture tex = createTexture(data);
			if(tex != null)
				texMap.put(texId, tex);
		}
		// Build Images
		for(long imgId : imgDataMap.keySet()) {
			ImageData data = imgDataMap.get(imgId);
			Image img = createImage(data);
			if(img != null)
				imgMap.put(imgId, img);
		}
		// Link images to textures
		for(long imgId : imgMap.keySet()) {
			List<Long> refs = refMap.get(imgId);
			if(refs == null)
				continue;
			Image img = imgMap.get(imgId);
			for(long refId : refs) {
				Texture tex = texMap.get(refId);
				if(tex != null)
					tex.setImage(img);
			}
		}
		// Link textures to material maps
		for(long texId : texMap.keySet()) {
			List<PropertyLink> props = propMap.get(texId);
			if(props == null)
				continue;
			Texture tex = texMap.get(texId);
			for(PropertyLink prop : props) {
				Material mat = matMap.get(prop.ref);
				if(mat != null) {
					if(prop.propName.equals("DiffuseColor")) {
						mat.setTexture("DiffuseMap", tex);
						mat.setColor("Diffuse", ColorRGBA.White);
					} else if(prop.propName.equals("SpecularColor")) {
						mat.setTexture("SpecularMap", tex);
						mat.setColor("Specular", ColorRGBA.White);
					} else if(prop.propName.equals("NormalMap"))
						mat.setTexture("NormalMap", tex);
				}
			}
		}
	}
	
	private void linkMeshes(Node sceneNode) {
		// Build meshes
		for(long meshId : meshDataMap.keySet()) {
			MeshData data = meshDataMap.get(meshId);
			Geometry geom = createGeomerty(data);
			if(geom != null)
				geomMap.put(meshId, geom);
		}
		// Link meshes to models
		for(long geomId : geomMap.keySet()) {
			List<Long> refs = refMap.get(geomId);
			if(refs == null)
				continue;
			Geometry geom = geomMap.get(geomId);
			for(long refId : refs) {
				Node rootNode = (refId != 0) ? modelMap.get(refId) : sceneNode;
				if(rootNode != null) {
					geom.setName(rootNode.getName() + "-mesh");
					geom.updateModelBound();
					rootNode.attachChild(geom);
					break;
				}
			}
		}
		// Link materials to meshes
		for(long matId : matMap.keySet()) {
			List<Long> refs = refMap.get(matId);
			if(refs == null)
				continue;
			Material mat = matMap.get(matId);
			for(long refId : refs) {
				Node rootNode = modelMap.get(refId);
				if(rootNode != null) {
					for(Spatial child : rootNode.getChildren())
						child.setMaterial(mat);
				}
			}
		}
	}
	
	private void linkSkins(Node sceneNode) {
		// Build skeleton limbs
		for(long nodeId : modelDataMap.keySet()) {
			ModelData data = modelDataMap.get(nodeId);
			if(data.type.equals("LimbNode")) {
				Limb limb = createLimb(data);
				limbMap.put(nodeId, limb);
			}
		}
		if(limbMap.size() == 0)
			return;
		// Build skeleton bones
		Map<Long, Bone> bones = new HashMap<Long, Bone>();
		for(long limbId : limbMap.keySet()) {
			Limb limb = limbMap.get(limbId);
			Bone bone = new Bone(limb.name);
			Transform t = limb.bindTransform;
			bone.setBindTransforms(t.getTranslation(), t.getRotation(), t.getScale());
			bones.put(limbId, bone);
		}
		// Attach bones to roots
		for(long limbId : limbMap.keySet()) {
			List<Long> refs = refMap.get(limbId);
			if(refs == null)
				continue;
			// Find root limb
			long rootLimbId = 0L;
			for(long refId : refs) {
				if(limbMap.containsKey(refId)) {
					rootLimbId = refId;
					break;
				}
			}
			if(rootLimbId != 0L) {
				Bone bone = bones.get(limbId);
				Bone root = bones.get(rootLimbId);
				root.addChild(bone);
			}
		}
		// Link bone clusters to skin
		for(long clusterId : clusterMap.keySet()) {
			List<Long> refs = refMap.get(clusterId);
			if(refs == null)
				continue;
			for(long skinId : refs) {
				if(skinMap.containsKey(skinId)) {
					ClusterData data = clusterMap.get(clusterId);
					data.skinId = skinId;
					break;
				}
			}
		}
		// Build the skeleton
		this.skeleton = new Skeleton(bones.values().toArray(new Bone[0]));
		skeleton.setBindingPose();
		for(long limbId : bones.keySet()) {
			Bone bone = bones.get(limbId);
			Limb limb = limbMap.get(limbId);
			limb.boneIndex = skeleton.getBoneIndex(bone);
		}
		// Assign bones skinning to meshes
		for(long skinId : skinMap.keySet()) {
			// Find mesh by skin
			Mesh mesh = null;
			MeshData meshData = null;
			for(long meshId : refMap.get(skinId)) {
				Geometry g = geomMap.get(meshId);
				if(g != null) {
					meshData = meshDataMap.get(meshId);
					mesh = g.getMesh();
					break;
				}
			}
			// Bind skinning indexes and weight
			if(mesh != null && meshData != null) {
				// Create bone buffers
				FloatBuffer boneWeightData = BufferUtils.createFloatBuffer(meshData.vCount * 4);
				ByteBuffer boneIndicesData = BufferUtils.createByteBuffer(meshData.vCount * 4);
				mesh.setBuffer(VertexBuffer.Type.BoneWeight, 4, boneWeightData);
				mesh.setBuffer(VertexBuffer.Type.BoneIndex, 4, boneIndicesData);
				mesh.getBuffer(VertexBuffer.Type.BoneWeight).setUsage(Usage.CpuOnly);
				mesh.getBuffer(VertexBuffer.Type.BoneIndex).setUsage(Usage.CpuOnly);
				VertexBuffer weightsHW = new VertexBuffer(Type.HWBoneWeight);
				VertexBuffer indicesHW = new VertexBuffer(Type.HWBoneIndex);
				indicesHW.setUsage(Usage.CpuOnly); // Setting usage to CpuOnly so that the buffer is not send empty to the GPU
				weightsHW.setUsage(Usage.CpuOnly);
				mesh.setBuffer(weightsHW);
				mesh.setBuffer(indicesHW);
				// Accumulate skin bones influence into mesh buffers
				boolean bonesLimitExceeded = false;
				for(long limbId : bones.keySet()) {
					// Search bone cluster for the given limb and skin
					ClusterData cluster = null;
					for(long clusterId : refMap.get(limbId)) {
						ClusterData data = clusterMap.get(clusterId);
						if(data != null && data.skinId == skinId) {
							cluster = data;
							break;
						}
					}
					if(cluster == null || cluster.indexes == null || cluster.weights == null || cluster.indexes.length != cluster.weights.length)
						continue;
					Limb limb = limbMap.get(limbId);
					if(limb.boneIndex > 255)
						throw new AssetLoadException("Bone index can't be packed into byte");
					for(int i = 0; i < cluster.indexes.length; ++i) {
						int vertexIndex = cluster.indexes[i];
						if(vertexIndex >= meshData.reverseVertexMap.size())
							throw new AssetLoadException("Invalid skinning vertex index. Unexpected index lookup " + vertexIndex + " from " + meshData.reverseVertexMap.size());
						List<Integer> dstVertices = meshData.reverseVertexMap.get(vertexIndex);
						for(int v : dstVertices) {
							// Append bone index and weight to vertex
							int offset;
							float w = 0;
							for(offset = v * 4; offset < v * 4 + 4; ++offset) {
								w = boneWeightData.get(offset);
								if(w == 0)
									break;
							}
							if(w == 0) {
								boneWeightData.put(offset, (float) cluster.weights[i]);
								boneIndicesData.put(offset, (byte) limb.boneIndex);
							} else {
								// TODO It would be nice to discard small weight to accumulate more heavy influence
								bonesLimitExceeded = true;
							}
						}
					}
				}
				if(bonesLimitExceeded)
					logger.log(Level.WARNING, "Skinning support max 4 bone per vertex. Exceeding data will be discarded.");
				// Postprocess bones weights
				int maxWeightsPerVert = 0;
				boneWeightData.rewind();
				for(int v = 0; v < meshData.vCount; v++) {
					float w0 = boneWeightData.get();
					float w1 = boneWeightData.get();
					float w2 = boneWeightData.get();
					float w3 = boneWeightData.get();
					if(w3 != 0) {
						maxWeightsPerVert = Math.max(maxWeightsPerVert, 4);
					} else if(w2 != 0) {
						maxWeightsPerVert = Math.max(maxWeightsPerVert, 3);
					} else if(w1 != 0) {
						maxWeightsPerVert = Math.max(maxWeightsPerVert, 2);
					} else if(w0 != 0) {
						maxWeightsPerVert = Math.max(maxWeightsPerVert, 1);
					}
					float sum = w0 + w1 + w2 + w3;
					if(sum != 1f) {
						// normalize weights
						float mult = (sum != 0) ? (1f / sum) : 0;
						boneWeightData.position(v * 4);
						boneWeightData.put(w0 * mult);
						boneWeightData.put(w1 * mult);
						boneWeightData.put(w2 * mult);
						boneWeightData.put(w3 * mult);
					}
				}
				mesh.setMaxNumWeights(maxWeightsPerVert);
				mesh.generateBindPose(true);
			}
		}
		// Attach controls
		animControl = new AnimControl(skeleton);
		sceneNode.addControl(animControl);
		SkeletonControl control = new SkeletonControl(skeleton);
		sceneNode.addControl(control);
	}
	
	private void linkAnimations() {
		if(skeleton == null)
			return;
		if(animList == null || animList.list.size() == 0)
			return;
		// Link curves to nodes
		for(long curveId : acurveMap.keySet()) {
			List<PropertyLink> props = propMap.get(curveId);
			if(props == null)
				continue;
			AnimCurveData acurve = acurveMap.get(curveId);
			for(PropertyLink prop : props) {
				AnimNode anode = anodeMap.get(prop.ref);
				if(anode != null) {
					if(prop.propName.equals("d|X"))
						anode.xCurve = acurve;
					else if(prop.propName.equals("d|Y"))
						anode.yCurve = acurve;
					else if(prop.propName.equals("d|Z"))
						anode.zCurve = acurve;
				}
			}
		}
		// Link nodes to layers
		for(long nodeId : anodeMap.keySet()) {
			List<Long> refs = refMap.get(nodeId);
			if(refs == null)
				continue;
			for(long layerId : refs) {
				if(alayerMap.containsKey(layerId)) {
					AnimNode anode = anodeMap.get(nodeId);
					anode.layerId = layerId;
					break;
				}
			}
		}
		// Extract aminations
		HashMap<String, Animation> anims = new HashMap<String, Animation>();
		for(AnimInverval animInfo : animList.list) {
			float length = (animInfo.lastFrame - animInfo.firstFrame) / this.animFrameRate;
			float animStart = animInfo.firstFrame / this.animFrameRate;
			float animStop = animInfo.lastFrame / this.animFrameRate;
			Animation anim = new Animation(animInfo.name, length);
			// Search source layer for animation nodes
			long sourceLayerId = 0L;
			for(long layerId : alayerMap.keySet()) {
				AnimLayer layer = alayerMap.get(layerId);
				if(layer.name.equals(animInfo.layerName)) {
					sourceLayerId = layerId;
					break;
				}
			}
			// Assign animation nodes to limbs
			for(Limb limb : limbMap.values()) {
				limb.animTranslation = null;
				limb.animRotation = null;
				limb.animScale = null;
			}
			for(long nodeId : anodeMap.keySet()) {
				List<PropertyLink> props = propMap.get(nodeId);
				if(props == null)
					continue;
				AnimNode anode = anodeMap.get(nodeId);
				if(sourceLayerId != 0L && anode.layerId != sourceLayerId)
					continue; // filter node
				for(PropertyLink prop : props) {
					Limb limb = limbMap.get(prop.ref);
					if(limb != null) {
						if(prop.propName.equals("Lcl Translation"))
							limb.animTranslation = anode;
						else if(prop.propName.equals("Lcl Rotation"))
							limb.animRotation = anode;
						else if(prop.propName.equals("Lcl Scaling"))
							limb.animScale = anode;
					}
				}
			}
			// Build bone tracks
			for(Limb limb : limbMap.values()) {
				long[] keyTimes = null;
				boolean haveTranslation = (limb.animTranslation != null && limb.animTranslation.xCurve != null && limb.animTranslation.yCurve != null && limb.animTranslation.zCurve != null);
				boolean haveRotation = (limb.animRotation != null && limb.animRotation.xCurve != null && limb.animRotation.yCurve != null && limb.animRotation.zCurve != null);
				boolean haveScale = (limb.animScale != null && limb.animScale.xCurve != null && limb.animScale.yCurve != null && limb.animScale.zCurve != null);
				// Search key time array
				if(haveTranslation)
					keyTimes = limb.animTranslation.xCurve.keyTimes;
				else if(haveRotation)
					keyTimes = limb.animRotation.xCurve.keyTimes;
				else if(haveScale)
					keyTimes = limb.animScale.xCurve.keyTimes;
				if(keyTimes == null)
					continue;
				// Calculate keys interval by animation time interval
				int firstKey = 0;
				int lastKey = keyTimes.length - 1;
				for(int i = 0; i < keyTimes.length; ++i) {
					float time = (float) (((double) keyTimes[i]) * secondsPerUnit); // Translate into seconds
					if(time <= animStart)
						firstKey = i;
					if(time >= animStop) {
						lastKey = i;
						break;
					}
				}
				int keysCount = lastKey - firstKey + 1;
				if(keysCount <= 0)
					continue;
				float[] times = new float[keysCount];
				Vector3f[] translations = new Vector3f[keysCount];
				Quaternion[] rotations = new Quaternion[keysCount];
				Vector3f[] scales = null;
				// Calculate keyframes times
				for(int i = 0; i < keysCount; ++i) {
					int keyIndex = firstKey + i;
					float time = (float) (((double) keyTimes[keyIndex]) * secondsPerUnit); // Translate into seconds
					times[i] = time - animStart;
				}
				// Load keyframes from animation curves
				if(haveTranslation) {
					for(int i = 0; i < keysCount; ++i) {
						int keyIndex = firstKey + i;
						float x = limb.animTranslation.xCurve.keyValues[keyIndex] - limb.animTranslation.value.x;
						float y = limb.animTranslation.yCurve.keyValues[keyIndex] - limb.animTranslation.value.y;
						float z = limb.animTranslation.zCurve.keyValues[keyIndex] - limb.animTranslation.value.z;
						translations[i] = new Vector3f(x, y, z).divideLocal(unitSize);
					}
				} else {
					for(int i = 0; i < keysCount; ++i)
						translations[i] = new Vector3f();
				}
				if(haveRotation) {
					for(int i = 0; i < keysCount; ++i) {
						int keyIndex = firstKey + i;
						float x = limb.animRotation.xCurve.keyValues[keyIndex];
						float y = limb.animRotation.yCurve.keyValues[keyIndex];
						float z = limb.animRotation.zCurve.keyValues[keyIndex];
						rotations[i] = new Quaternion().fromAngles(FastMath.DEG_TO_RAD * x, FastMath.DEG_TO_RAD * y, FastMath.DEG_TO_RAD * z);
					}
				} else {
					for(int i = 0; i < keysCount; ++i)
						rotations[i] = new Quaternion();
				}
				if(haveScale) {
					scales = new Vector3f[keysCount];
					for(int i = 0; i < keysCount; ++i) {
						int keyIndex = firstKey + i;
						float x = limb.animScale.xCurve.keyValues[keyIndex];
						float y = limb.animScale.yCurve.keyValues[keyIndex];
						float z = limb.animScale.zCurve.keyValues[keyIndex];
						scales[i] = new Vector3f(x, y, z);
					}
				}
				BoneTrack track = null;
				if(haveScale)
					track = new BoneTrack(limb.boneIndex, times, translations, rotations, scales);
				else
					track = new BoneTrack(limb.boneIndex, times, translations, rotations);
				anim.addTrack(track);
			}
			anims.put(anim.getName(), anim);
		}
		animControl.setAnimations(anims);
	}
	
	private void releaseObjects() {
		meshDataMap.clear();
		matDataMap.clear();
		texDataMap.clear();
		imgDataMap.clear();
		modelDataMap.clear();
		poseDataMap.clear();
		skinMap.clear();
		clusterMap.clear();
		acurveMap.clear();
		anodeMap.clear();
		alayerMap.clear();
		refMap.clear();
		propMap.clear();
		modelMap.clear();
		limbMap.clear();
		bindMap.clear();
		geomMap.clear();
		matMap.clear();
		texMap.clear();
		imgMap.clear();
		skeleton = null;
		animControl = null;
		animList = null;
	}
	
	private class MeshData {
		double[] vertices;
		int[] indices;
		int[] edges;
		String normalsMapping;
		String normalsReference;
		double[] normals;
		String tangentsMapping;
		String tangentsReference;
		double[] tangents;
		String binormalsMapping;
		String binormalsReference;
		double[] binormals;
		String uvMapping;
		String uvReference;
		double[] uv;
		int[] uvIndex;
		String smoothingMapping;
		String smoothingReference;
		int[] smoothing;
		String materialsMapping;
		String materialsReference;
		int[] materials;
		// Build helping data
		int iCount;
		int vCount;
		int srcVertexCount;
		int[] vertexMap; // Target vertex -> source vertex
		List<List<Integer>> reverseVertexMap; // source vertex -> list of target vertices
		int[] indexMap; // Target vertex -> source index
	}
	
	private class ModelData {
		String name;
		String type;
		Vector3f localTranslation = new Vector3f();
		Quaternion localRotation = new Quaternion();
		Vector3f localScale = new Vector3f(Vector3f.UNIT_XYZ);
		Quaternion preRotation = new Quaternion();
	}
	
	private class NodeTransformData {
		long nodeId;
		double[] transform;
	}
	
	private class BindPoseData {
		String name;
		List<NodeTransformData> list = new LinkedList<NodeTransformData>();
	}
	
	private class BindPose {
		String name;
		Map<Long, Transform> nodeTransforms = new HashMap<Long, Transform>();
	}
	
	private class MaterialData {
		String name;
		String shadingModel = "phong";
		Vector3f ambientColor = new Vector3f(0.2f, 0.2f, 0.2f);
		float ambientFactor = 1.0f;
		Vector3f diffuseColor = new Vector3f(0.8f, 0.8f, 0.8f);
		float diffuseFactor = 1.0f;
		Vector3f specularColor = new Vector3f(0.2f, 0.2f, 0.2f);
		float specularFactor = 1.0f;
		float shininessExponent = 20.0f;
	}
	
	private class TextureData {
		String name;
		String bindType;
		String filename;
	}
	
	private class ImageData {
		String name;
		String type;
		String filename;
		String relativeFilename;
		byte[] content;
	}
	
	private class Limb {
		String name;
		Transform bindTransform;
		int boneIndex;
		AnimNode animTranslation;
		AnimNode animRotation;
		AnimNode animScale;
	}
	
	private class ClusterData {
		int[] indexes;
		double[] weights;
		double[] transform;
		double[] transformLink;
		long skinId;
	}
	
	private class SkinData {
		String type;
	}
	
	private class AnimCurveData {
		long[] keyTimes;
		float[] keyValues;
	}
	
	private class AnimLayer {
		String name;
	}
	
	private class AnimNode {
		String name;
		Vector3f value;
		AnimCurveData xCurve;
		AnimCurveData yCurve;
		AnimCurveData zCurve;
		long layerId;
	}
	
	private class PropertyLink {
		long ref;
		String propName;
		
		public PropertyLink(long id, String prop) {
			this.ref = id;
			this.propName = prop;
		}
	}
	
}
