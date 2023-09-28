/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.scene.plugins.gltf.ext.JME_speaker;


import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData.DataType;
import com.jme3.plugins.json.JsonElement;
import com.jme3.plugins.json.JsonObject;
import com.jme3.audio.AudioNode;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.gltf.ExtensionLoader;
import com.jme3.scene.plugins.gltf.GltfLoader;

/**
 * An extension for the GLTF loader that loads speaker nodes.
 * (This extension requires the jme-extras addon for blender)
 */
public class SpeakerExtensionLoader implements ExtensionLoader {

    public SpeakerExtensionLoader(){}

    @Override
    public Object handleExtension(GltfLoader loader, String parentName, JsonElement parent, JsonElement extension, Object input) {
        if (input instanceof Node) {
            JsonObject jsonObject = extension.getAsJsonObject();

            final float volume = jsonObject.getAsJsonPrimitive("volume").getAsFloat();
            final float pitch = jsonObject.getAsJsonPrimitive("pitch").getAsFloat();
            final float attenuation = jsonObject.getAsJsonPrimitive("attenuation").getAsFloat(); // unused
            final float distanceMax = jsonObject.getAsJsonPrimitive("distance_max").getAsFloat();
            final float distanceReference = jsonObject.getAsJsonPrimitive("distance_reference").getAsFloat();
            final float volume_min = jsonObject.getAsJsonPrimitive("volume_min").getAsFloat(); // unused
            final float volume_max = jsonObject.getAsJsonPrimitive("volume_max").getAsFloat(); // unused
            final float angleOuterCone = jsonObject.getAsJsonPrimitive("angle_outer_cone").getAsFloat();
            final float angleInnerCone = jsonObject.getAsJsonPrimitive("angle_inner_cone").getAsFloat();
            final float outerConeVolume = jsonObject.getAsJsonPrimitive("outer_cone_volume").getAsFloat(); // unused
            final String soundPath = jsonObject.getAsJsonPrimitive("sound_path").getAsString();

            String absSoundPath = loader.getInfo().getKey().getFolder() + soundPath;
            

            AssetManager am = loader.getInfo().getManager();
            AudioNode audioSource = new AudioNode(am, absSoundPath, DataType.Buffer);
            audioSource.setVolume(volume);
            audioSource.setPitch(pitch);
            audioSource.setRefDistance(distanceReference);
            audioSource.setMaxDistance(distanceMax);
            audioSource.setInnerAngle(angleInnerCone);
            audioSource.setOuterAngle(angleOuterCone);
            audioSource.setPositional(true);

            if (angleOuterCone == 360 && angleInnerCone == 360) {
                audioSource.setDirectional(false);
            } else {
                audioSource.setDirectional(true);
            }

            audioSource.setLooping(true);
            audioSource.setReverbEnabled(true);

            ((Node) input).attachChild(audioSource);

            audioSource.play();

        }

        return input;

    }

}
