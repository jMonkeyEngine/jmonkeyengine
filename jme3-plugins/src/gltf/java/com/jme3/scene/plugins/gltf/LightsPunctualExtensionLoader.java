/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package com.jme3.scene.plugins.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.control.LightControl;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import static com.jme3.scene.plugins.gltf.GltfUtils.getAsColor;
import static com.jme3.scene.plugins.gltf.GltfUtils.getAsFloat;

/**
 * Extension loader for KHR_lights_punctual extension which allows
 * for lights to be added to the node from the gltf model
 * Created by Trevor Flynn - 12/12/2020
 */
public class LightsPunctualExtensionLoader implements ExtensionLoader {

    private HashSet<NodeNeedingLight> pendingNodes = new HashSet<>();
    private HashMap<Integer, Light> lightDefinitions = new HashMap<>();

    @Override
    public Object handleExtension(GltfLoader loader, String parentName, JsonElement parent, JsonElement extension, Object input) throws IOException {
        if (input instanceof Node) { //We are processing a node
            JsonObject jsonObject = extension.getAsJsonObject();
            if (jsonObject.has("light")) { //These will get run first when loading the gltf file
                //Add node to queue
                JsonElement indexElement = jsonObject.get("light");
                int index = indexElement.getAsInt();
                if (!lightDefinitions.containsKey(index)) {
                    pendingNodes.add(new NodeNeedingLight((Node) input, index));
                } else {
                    addLight((Node) input, (Node) input, index);
                }
            } else if (jsonObject.has("lights")) { //This will get run last
                //Process the light definitions
                JsonArray lights = jsonObject.getAsJsonArray("lights");

                for (int i = 0; i < lights.size(); i++) {
                    //Create light definition
                    JsonObject light = lights.get(i).getAsJsonObject();
                    String type = light.get("type").getAsString();

                    Light lightNode;
                    switch (type) {
                        case "point":
                            lightNode = buildPointLight(light);
                            break;
                        case "directional":
                            lightNode = buildDirectionalLight(light);
                            break;
                        case "spot":
                            lightNode = buildSpotLight(light);
                            break;
                        default:
                            throw new AssetLoadException("KHR_lights_punctual unsupported light type: " + type);
                    }

                    lightDefinitions.put(i, lightNode);
                }

                //Build any lights that are pending now that we have definitions
                for (NodeNeedingLight nodeInNeed : pendingNodes) {
                    addLight((Node) input, nodeInNeed.getNode(), nodeInNeed.getLightIndex());
                }
                pendingNodes.clear();
            } else {
                throw new AssetLoadException("KHR_lights_punctual extension malformed json");
            }

            return input;
        } else {
            throw new AssetLoadException("KHR_lights_punctual extension added on unsupported element");
        }
    }

    private SpotLight buildSpotLight(JsonObject obj) {
        //Get properties
        String name = obj.has("name") ? obj.get("name").getAsString() : "";

        float intensity = obj.has("intensity") ? obj.get("intensity").getAsFloat() : 1.0f;
        ColorRGBA color = obj.has("color") ? GltfUtils.getAsColor(obj, "color") : new ColorRGBA(ColorRGBA.White);
        color = lumensToColor(color, intensity);
        float range = obj.has("range") ? obj.get("range").getAsFloat() : Float.POSITIVE_INFINITY;

        //Spot specific
        JsonObject spot = obj.getAsJsonObject("spot");
        float innerConeAngle = spot != null && spot.has("innerConeAngle") ? spot.get("innerConeAngle").getAsFloat() : 0f;
        float outerConeAngle = spot != null && spot.has("outerConeAngle") ? spot.get("outerConeAngle").getAsFloat() : ((float) Math.PI) / 4f;

        /*
        Correct floating point error on half PI, GLTF spec says that the outerConeAngle
        can be less or equal to PI/2, but JME requires less than PI/2.
        We will being the angle within PI/2 if it is equal or larger than PI/2
         */
        if (outerConeAngle >= FastMath.HALF_PI) {
            outerConeAngle = FastMath.HALF_PI - 0.000001f;
        }

        SpotLight spotLight = new SpotLight();
        spotLight.setName(name);
        spotLight.setColor(color);
        spotLight.setSpotRange(range);
        spotLight.setSpotInnerAngle(innerConeAngle);
        spotLight.setSpotOuterAngle(outerConeAngle);
        spotLight.setDirection(Vector3f.UNIT_Z.negate());

        return spotLight;
    }

    private DirectionalLight buildDirectionalLight(JsonObject obj) {
        //Get properties
        String name = obj.has("name") ? obj.get("name").getAsString() : "";

        float intensity = obj.has("intensity") ? obj.get("intensity").getAsFloat() : 1.0f;
        ColorRGBA color = obj.has("color") ? GltfUtils.getAsColor(obj, "color") : new ColorRGBA(ColorRGBA.White);
        color = lumensToColor(color, intensity);

        DirectionalLight directionalLight = new DirectionalLight();
        directionalLight.setName(name);
        directionalLight.setColor(color);
        directionalLight.setDirection(Vector3f.UNIT_Z.negate());

        return directionalLight;
    }

    private PointLight buildPointLight(JsonObject obj) {
        //Get properties
        String name = obj.has("name") ? obj.get("name").getAsString() : "";

        float intensity = obj.has("intensity") ? obj.get("intensity").getAsFloat() : 1.0f;
        ColorRGBA color = obj.has("color") ? GltfUtils.getAsColor(obj, "color") : new ColorRGBA(ColorRGBA.White);
        color = lumensToColor(color, intensity);
        float range = obj.has("range") ? obj.get("range").getAsFloat() : Float.POSITIVE_INFINITY;

        PointLight pointLight = new PointLight();
        pointLight.setName(name);
        pointLight.setColor(color);
        pointLight.setRadius(range);

        return pointLight;
    }

    private void addLight(Node parent, Node node, int lightIndex) {
        if (lightDefinitions.containsKey(lightIndex)) {
            Light light = lightDefinitions.get(lightIndex);
            parent.addLight(light);
            LightControl control = new LightControl(light);
            node.addControl(control);
        } else {
            throw new AssetLoadException("KHR_lights_punctual extension accessed undefined light at index " + lightIndex);
        }
    }

    private ColorRGBA lumensToColor(ColorRGBA color, float lumens) {
        ColorRGBA brightnessModifier = lumensToColor(lumens);
        return color.mult(brightnessModifier);
    }

    private ColorRGBA lumensToColor(float lumens) {
        /*
        Taken from /Common/ShaderLib/Hdr.glsllib
        vec4 HDR_EncodeLum(in float lum){
        float Le = 2.0 * log2(lum + epsilon) + 127.0;
        vec4 result = vec4(0.0);
        result.a = fract(Le);
        result.rgb = vec3((Le - (floor(result.a * 255.0)) / 255.0) / 255.0);
        return result;
         */
        float epsilon = 0.0001f;

        double Le = 2f * Math.log(lumens * epsilon) / Math.log(2);
        ColorRGBA color = new ColorRGBA();
        color.a = (float) (Le - Math.floor(Le)); //Get fractional part
        float val = (float) ((Le - (Math.floor(color.a * 255.0)) / 255.0) / 255.0);
        color.r = val;
        color.g = val;
        color.b = val;

        return color;
    }

    private class NodeNeedingLight {
        private Node node;
        private int lightIndex;

        public NodeNeedingLight(Node node, int lightIndex) {
            this.node = node;
            this.lightIndex = lightIndex;
        }

        public Node getNode() {
            return node;
        }

        public void setNode(Node node) {
            this.node = node;
        }

        public int getLightIndex() {
            return lightIndex;
        }

        public void setLightIndex(int lightIndex) {
            this.lightIndex = lightIndex;
        }
    }
}
