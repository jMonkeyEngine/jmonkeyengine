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
package com.jme3.scene.plugins.ogre;

import com.jme3.asset.*;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.MaterialList;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.plugins.ogre.matext.OgreMaterialKey;
import com.jme3.util.PlaceholderAssets;
import com.jme3.util.xml.SAXUtil;
import static com.jme3.util.xml.SAXUtil.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class SceneLoader extends DefaultHandler implements AssetLoader {

    private static final int DEFAULT_CAM_WIDTH = 640;
    private static final int DEFAULT_CAM_HEIGHT = 480;
    
    private static final Logger logger = Logger.getLogger(SceneLoader.class.getName());
    private SceneMaterialLoader materialLoader = new SceneMaterialLoader();
    private SceneMeshLoader meshLoader=new SceneMeshLoader();
    private Stack<String> elementStack = new Stack<String>();
    private AssetKey key;
    private String sceneName;
    private String folderName;
    private AssetManager assetManager;
    private MaterialList materialList;
    private com.jme3.scene.Node root;
    private com.jme3.scene.Node node;
    private com.jme3.scene.Node entityNode;
    private Light light;
    private Camera camera;
    private CameraNode cameraNode;
    private int nodeIdx = 0;
    private static volatile int sceneIdx = 0;

    public SceneLoader() {
        super();
    }

    @Override
    public void startDocument() {
    }

    @Override
    public void endDocument() {
    }

    private void reset() {
    	meshLoader.reset();
        elementStack.clear();
        nodeIdx = 0;

        // NOTE: Setting some of those to null is only needed
        // if the parsed file had an error e.g. startElement was called
        // but not endElement
        root = null;
        node = null;
        entityNode = null;
        light = null;
        camera = null;
        cameraNode = null;
    }

    private void checkTopNode(String topNode) throws SAXException {
        if (!elementStack.peek().equals(topNode)) {
            throw new SAXException("dotScene parse error: Expected parent node to be " + topNode);
        }
    }

    private Quaternion parseQuat(Attributes attribs) throws SAXException {
        if (attribs.getValue("x") != null) {
            // defined as quaternion
            float x = parseFloat(attribs.getValue("x"));
            float y = parseFloat(attribs.getValue("y"));
            float z = parseFloat(attribs.getValue("z"));
            float w = parseFloat(attribs.getValue("w"));
            return new Quaternion(x, y, z, w);
        } else if (attribs.getValue("qx") != null) {
            // defined as quaternion with prefix "q"
            float x = parseFloat(attribs.getValue("qx"));
            float y = parseFloat(attribs.getValue("qy"));
            float z = parseFloat(attribs.getValue("qz"));
            float w = parseFloat(attribs.getValue("qw"));
            return new Quaternion(x, y, z, w);
        } else if (attribs.getValue("angle") != null) {
            // defined as angle + axis
            float angle = parseFloat(attribs.getValue("angle"));
            float axisX = parseFloat(attribs.getValue("axisX"));
            float axisY = parseFloat(attribs.getValue("axisY"));
            float axisZ = parseFloat(attribs.getValue("axisZ"));
            Quaternion q = new Quaternion();
            q.fromAngleAxis(angle, new Vector3f(axisX, axisY, axisZ));
            return q;
        } else {
            // defines as 3 angles along XYZ axes
            float angleX = parseFloat(attribs.getValue("angleX"));
            float angleY = parseFloat(attribs.getValue("angleY"));
            float angleZ = parseFloat(attribs.getValue("angleZ"));
            Quaternion q = new Quaternion();
            q.fromAngles(angleX, angleY, angleZ);
            return q;
        }
    }

    private void parseLightNormal(Attributes attribs) throws SAXException {
        checkTopNode("light");

        // SpotLight will be supporting a direction-normal, too.
        if (light instanceof DirectionalLight) {
            ((DirectionalLight) light).setDirection(parseVector3(attribs));
        } else if (light instanceof SpotLight) {
            ((SpotLight) light).setDirection(parseVector3(attribs));
        }
    }

    private void parseLightAttenuation(Attributes attribs) throws SAXException {
        // NOTE: Derives range based on "linear" if it is used solely
        // for the attenuation. Otherwise derives it from "range"
        checkTopNode("light");

        if (light instanceof PointLight || light instanceof SpotLight) {
            float range = parseFloat(attribs.getValue("range"));
            float constant = parseFloat(attribs.getValue("constant"));
            float linear = parseFloat(attribs.getValue("linear"));

            String quadraticStr = attribs.getValue("quadratic");
            if (quadraticStr == null) {
                quadraticStr = attribs.getValue("quadric");
            }

            float quadratic = parseFloat(quadraticStr);

            if (constant == 1 && quadratic == 0 && linear > 0) {
                range = 1f / linear;
            }

            if (light instanceof PointLight) {
                ((PointLight) light).setRadius(range);
            } else {
                ((SpotLight) light).setSpotRange(range);
            }
        }
    }

    private void parseLightSpotLightRange(Attributes attribs) throws SAXException {
        checkTopNode("light");

        float outer = SAXUtil.parseFloat(attribs.getValue("outer"));
        float inner = SAXUtil.parseFloat(attribs.getValue("inner"));

        if (!(light instanceof SpotLight)) {
            throw new SAXException("dotScene parse error: spotLightRange "
                    + "can only appear under 'spot' light elements");
        }

        SpotLight sl = (SpotLight) light;
        sl.setSpotInnerAngle(inner * 0.5f);
        sl.setSpotOuterAngle(outer * 0.5f);
    }

    private void parseLight(Attributes attribs) throws SAXException {
        if (node == null || node.getParent() == null) {
            throw new SAXException("dotScene parse error: light can only appear under a node");
        }

        checkTopNode("node");

        String lightType = parseString(attribs.getValue("type"), "point");
        if (lightType.equals("point")) {
            light = new PointLight();
        } else if (lightType.equals("directional") || lightType.equals("sun")) {
            light = new DirectionalLight();
            // Assuming "normal" property is not provided
            ((DirectionalLight) light).setDirection(Vector3f.UNIT_Z);
        } else if (lightType.equals("spotLight") || lightType.equals("spot")) {
            light = new SpotLight();
        } else if (lightType.equals("omni")) {
            // XXX: It doesn't seem any exporters actually emit this type?
            light = new AmbientLight();
        } else {
            logger.log(Level.WARNING, "No matching jME3 LightType found for OGRE LightType: {0}", lightType);
        }
        logger.log(Level.FINEST, "{0} created.", light);

        if (!parseBool(attribs.getValue("visible"), true)) {
            // set to disabled
        }

        // "attach" it to the parent of this node
        if (light != null) {
            node.getParent().addLight(light);
        }
    }
    
    private void parseCameraClipping(Attributes attribs) throws SAXException {
        if (attribs.getValue("near") != null) {
            camera.setFrustumNear(SAXUtil.parseFloat(attribs.getValue("near")));
            camera.setFrustumFar(SAXUtil.parseFloat(attribs.getValue("far")));
        } else {
            camera.setFrustumNear(SAXUtil.parseFloat(attribs.getValue("nearPlaneDist")));
            camera.setFrustumFar(SAXUtil.parseFloat(attribs.getValue("farPlaneDist")));
        }
    }
    
    private void parseCamera(Attributes attribs) throws SAXException {
        camera = new Camera(DEFAULT_CAM_WIDTH, DEFAULT_CAM_HEIGHT);
        if (SAXUtil.parseString(attribs.getValue("projectionType"), "perspective").equals("parallel")){
            camera.setParallelProjection(true);
        }
        float fov = SAXUtil.parseFloat(attribs.getValue("fov"), 45f);
        if (fov < FastMath.PI) { 
            // XXX: Most likely, it is in radians..
            fov = fov * FastMath.RAD_TO_DEG;
        }
        camera.setFrustumPerspective(fov, (float)DEFAULT_CAM_WIDTH / DEFAULT_CAM_HEIGHT, 1, 1000);
        
        cameraNode = new CameraNode(attribs.getValue("name"), camera);
        cameraNode.setControlDir(ControlDirection.SpatialToCamera);
        
        node.attachChild(cameraNode);
        node = null;
    }
    
    private void parseEntity(Attributes attribs) throws SAXException {
        String name = attribs.getValue("name");
        if (name == null) {
            name = "OgreEntity-" + (++nodeIdx);
        } else {
            name += "-entity";
        }

        String meshFile = attribs.getValue("meshFile");
        if (meshFile == null) {
            throw new SAXException("Required attribute 'meshFile' missing for 'entity' node");
        }

        // TODO: Not currently used
        String materialName = attribs.getValue("materialName");

        if (folderName != null) {
            meshFile = folderName + meshFile;
        }

        // NOTE: append "xml" since its assumed mesh files are binary in dotScene
        meshFile += ".xml";

        entityNode = new com.jme3.scene.Node(name);
        OgreMeshKey meshKey = new OgreMeshKey(meshFile, materialList);
        try {
			try{
				Spatial ogreMesh=(Spatial)meshLoader.load(assetManager.locateAsset(meshKey));
				entityNode.attachChild(ogreMesh);
			}catch(IOException e){
				throw new AssetNotFoundException(meshKey.toString());
			}
        } catch (AssetNotFoundException ex) {
            if (ex.getMessage().equals(meshFile)) {
                logger.log(Level.WARNING, "Cannot locate {0} for scene {1}", new Object[]{meshKey, key});
                // Attach placeholder asset.
                Spatial model = PlaceholderAssets.getPlaceholderModel(assetManager);
                model.setKey(key);
                entityNode.attachChild(model);
            } else {
                throw ex;
            }
        }

        node.attachChild(entityNode);
        node = null;
    }
    
    private void parseNode(Attributes attribs) throws SAXException {
        String name = attribs.getValue("name");
        if (name == null) {
            name = "OgreNode-" + (++nodeIdx);
        }

        com.jme3.scene.Node newNode = new com.jme3.scene.Node(name);
        if (node != null) {
            node.attachChild(newNode);
        }
        node = newNode;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attribs) throws SAXException {
        if (qName.equals("scene")) {
            if (elementStack.size() != 0) {
                throw new SAXException("dotScene parse error: 'scene' element must be the root XML element");
            }

            String version = attribs.getValue("formatVersion");
            if (version == null || (!version.equals("1.0.0") && !version.equals("1.0.1"))) {
                logger.log(Level.WARNING, "Unrecognized version number"
                        + " in dotScene file: {0}", version);
            }
        } else if (qName.equals("nodes")) {
            if (root != null) {
                throw new SAXException("dotScene parse error: nodes element was specified twice");
            }
            if (sceneName == null) {
                root = new com.jme3.scene.Node("OgreDotScene" + (++sceneIdx));
            } else {
                root = new com.jme3.scene.Node(sceneName + "-scene_node");
            }

            node = root;
        } else if (qName.equals("externals")) {
            checkTopNode("scene");
        } else if (qName.equals("item")) {
            checkTopNode("externals");
        } else if (qName.equals("file")) {
            checkTopNode("item");

            // NOTE: This part of the file is ignored, it is parsed
            // by SceneMaterialLoader in the first pass.
        } else if (qName.equals("node")) {
            String curElement = elementStack.peek();
            if (!curElement.equals("node") && !curElement.equals("nodes")) {
                throw new SAXException("dotScene parse error: "
                        + "node element can only appear under 'node' or 'nodes'");
            }

            parseNode(attribs);
        } else if (qName.equals("property")) {
            if (node != null) {
                String type = attribs.getValue("type");
                String name = attribs.getValue("name");
                String data = attribs.getValue("data");
                if (type.equals("BOOL")) {
                    node.setUserData(name, Boolean.parseBoolean(data) || data.equals("1"));
                } else if (type.equals("FLOAT")) {
                    node.setUserData(name, Float.parseFloat(data));
                } else if (type.equals("STRING")) {
                    node.setUserData(name, data);
                } else if (type.equals("INT")) {
                    node.setUserData(name, Integer.parseInt(data));
                }
            }
        } else if (qName.equals("entity")) {
            checkTopNode("node");
            parseEntity(attribs);
        } else if (qName.equals("camera")) {
            checkTopNode("node");
            parseCamera(attribs);
        } else if (qName.equals("clipping")) {
            checkTopNode("camera");
            parseCameraClipping(attribs);
        } else if (qName.equals("position")) {
            if (elementStack.peek().equals("node")) {
                node.setLocalTranslation(SAXUtil.parseVector3(attribs));
            } else if (elementStack.peek().equals("camera")) {
                cameraNode.setLocalTranslation(SAXUtil.parseVector3(attribs));
            }
        } else if (qName.equals("quaternion") || qName.equals("rotation")) {
            node.setLocalRotation(parseQuat(attribs));
        } else if (qName.equals("scale")) {
            node.setLocalScale(SAXUtil.parseVector3(attribs));
        } else if (qName.equals("light")) {
            parseLight(attribs);
        } else if (qName.equals("colourDiffuse") || qName.equals("colorDiffuse")) {
            if (elementStack.peek().equals("light")) {
                if (light != null) {
                    light.setColor(parseColor(attribs));
                }
            } else {
                checkTopNode("environment");
            }
        } else if (qName.equals("colourAmbient") || qName.equals("colorAmbient")) {
            if (elementStack.peek().equals("environment")) {
                ColorRGBA color = parseColor(attribs);
                if (!color.equals(ColorRGBA.Black) && !color.equals(ColorRGBA.BlackNoAlpha)) {
                    // Lets add an ambient light to the scene.
                    AmbientLight al = new AmbientLight();
                    al.setColor(color);
                    root.addLight(al);
                }
            }
        } else if (qName.equals("normal") || qName.equals("direction")) {
            checkTopNode("light");
            parseLightNormal(attribs);
        } else if (qName.equals("lightAttenuation")) {
            parseLightAttenuation(attribs);
        } else if (qName.equals("spotLightRange") || qName.equals("lightRange")) {
            parseLightSpotLightRange(attribs);
        }

        elementStack.push(qName);
    }

    @Override
    public void endElement(String uri, String name, String qName) throws SAXException {
        if (qName.equals("node")) {
            node = node.getParent();
        } else if (qName.equals("nodes")) {
            node = null;
        } else if (qName.equals("entity")) {
            node = entityNode.getParent();
            entityNode = null;
        } else if (qName.equals("camera")) {
            node = cameraNode.getParent();
            cameraNode = null;
        } else if (qName.equals("light")) {
            // apply the node's world transform on the light..
            root.updateGeometricState();
            if (light != null) {
                if (light instanceof DirectionalLight) {
                    DirectionalLight dl = (DirectionalLight) light;
                    Quaternion q = node.getWorldRotation();
                    Vector3f dir = dl.getDirection();
                    q.multLocal(dir);
                    dl.setDirection(dir);
                } else if (light instanceof PointLight) {
                    PointLight pl = (PointLight) light;
                    Vector3f pos = node.getWorldTranslation();
                    pl.setPosition(pos);
                } else if (light instanceof SpotLight) {
                    SpotLight sl = (SpotLight) light;

                    Vector3f pos = node.getWorldTranslation();
                    sl.setPosition(pos);

                    Quaternion q = node.getWorldRotation();
                    Vector3f dir = sl.getDirection();
                    q.multLocal(dir);
                    sl.setDirection(dir);
                }
            }
            light = null;
        }
        checkTopNode(qName);
        elementStack.pop();
    }

    @Override
    public void characters(char ch[], int start, int length) {
    }

    public Object load(AssetInfo info) throws IOException {
        try {
            key = info.getKey();
            assetManager = info.getManager();
            sceneName = key.getName();
            String ext = key.getExtension();
            folderName = key.getFolder();
            sceneName = sceneName.substring(0, sceneName.length() - ext.length() - 1);

            reset();

            // == Run 1st pass over XML file to determine material list ==
            materialList = materialLoader.load(assetManager, folderName, info.openStream());

            if (materialList == null || materialList.isEmpty()) {
                // NOTE: No materials were found by searching the externals section.
                // Try finding a similarly named material file in the same folder.
                // (Backward compatibility only!)
                OgreMaterialKey materialKey = new OgreMaterialKey(sceneName + ".material");
                try {
                    materialList = (MaterialList) assetManager.loadAsset(materialKey);
                } catch (AssetNotFoundException ex) {
                    logger.log(Level.WARNING, "Cannot locate {0} for scene {1}", new Object[]{materialKey, key});
                    materialList = null;
                }
            }

            // == Run 2nd pass to load entities and other objects ==

            // Added by larynx 25.06.2011
            // Android needs the namespace aware flag set to true 
            // Kirill 30.06.2011
            // Now, hack is applied for both desktop and android to avoid
            // checking with JmeSystem.
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XMLReader xr = factory.newSAXParser().getXMLReader();

            xr.setContentHandler(this);
            xr.setErrorHandler(this);

            InputStreamReader r = null;

            try {
                r = new InputStreamReader(info.openStream());
                xr.parse(new InputSource(r));
            } finally {
                if (r != null) {
                    r.close();
                }
            }

            return root;
        } catch (SAXException ex) {
            IOException ioEx = new IOException("Error while parsing Ogre3D dotScene");
            ioEx.initCause(ex);
            throw ioEx;
        } catch (ParserConfigurationException ex) {
            IOException ioEx = new IOException("Error while parsing Ogre3D dotScene");
            ioEx.initCause(ex);
            throw ioEx;
        }
    }
}
