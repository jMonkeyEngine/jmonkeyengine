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
package com.jme3.scene.plugins.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.HashMap;
import java.util.HashSet;

/**
 * Extension loader for KHR_lights_punctual extension which allows
 * for lights to be added to the node from the gltf model.
 *
 * Supports directional, point, and spot lights.
 *
 * Created by Trevor Flynn - 3/23/2021
 */
public class LightsPunctualExtensionLoader implements ExtensionLoader {

    private final HashSet<NodeNeedingLight> pendingNodes = new HashSet<>();
    private final HashMap<Integer, Light> lightDefinitions = new HashMap<>();

    @Override
    public Object handleExtension(GltfLoader loader, String parentName, JsonElement parent, JsonElement extension, Object input) {
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

    /**
     * Build a spot light from gltf json.
     * @param obj The gltf json object for the spot light
     * @return A spot light representation of the gltf object
     */
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
        We will bring the angle within PI/2 if it is equal or larger than PI/2
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

    /**
     * Build a directional light from gltf json.
     * @param obj The gltf json object for the directional light
     * @return A directional light representation of the gltf object
     */
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

    /**
     * Build a point light from gltf json.
     * @param obj The gltf json object for the point light
     * @return A point light representation of the gltf object
     */
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

    /**
     * Attach a light at the given index to the given parent node,
     * and the control for the light to the given node.
     * @param parent The node to attach the light to
     * @param node The node to attach the light control to
     * @param lightIndex The index of the light
     */
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

    /**
     * Convert a floating point lumens value into a color that
     * represents both color and brightness of the light.
     *
     * @param color The base color of the light
     * @param lumens The lumens value to convert to a color
     * @return A color representing the intensity of the given lumens encoded into the given color
     */
    private ColorRGBA lumensToColor(ColorRGBA color, float lumens) {
        ColorRGBA brightnessModifier = lumensToColor(lumens);
        return color.mult(brightnessModifier);
    }

    /**
     * Convert a floating point lumens value into a grayscale color that
     * represents a brightness.
     *
     * @param lumens The lumens value to convert to a color
     * @return A color representing the intensity of the given lumens
     */
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

        double Le = 2f * Math.log(lumens * epsilon) / Math.log(2) + 127.0;
        ColorRGBA color = new ColorRGBA();
        color.a = (float) (Le - Math.floor(Le)); //Get fractional part
        float val = (float) ((Le - (Math.floor(color.a * 255.0)) / 255.0) / 255.0);
        color.r = val;
        color.g = val;
        color.b = val;

        return color;
    }

    /**
     * A bean to contain the relation between a node and a light index
     */
    private static class NodeNeedingLight {
        private Node node;
        private int lightIndex;

        private NodeNeedingLight(Node node, int lightIndex) {
            this.node = node;
            this.lightIndex = lightIndex;
        }

        private Node getNode() {
            return node;
        }

        private void setNode(Node node) {
            this.node = node;
        }

        private int getLightIndex() {
            return lightIndex;
        }

        private void setLightIndex(int lightIndex) {
            this.lightIndex = lightIndex;
        }
    }
}
