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

package com.jme3.audio;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.util.PlaceholderAssets;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An <code>AudioNode</code> is used in jME3 for playing audio files.
 * <br/>
 * First, an {@link AudioNode} is loaded from file, and then assigned
 * to an audio node for playback. Once the audio node is attached to the 
 * scene, its location will influence the position it is playing from relative
 * to the {@link Listener}.
 * <br/>
 * An audio node can also play in "headspace", meaning its location
 * or velocity does not influence how it is played. 
 * The "positional" property of an AudioNode can be set via 
 * {@link AudioNode#setPositional(boolean) }.
 * 
 * @author normenhansen
 * @author Kirill Vainer
 */
public class AudioNode extends Node {

    protected boolean loop = false;
    protected float volume = 1;
    protected float pitch = 1;
    protected float timeOffset = 0;
    protected Filter dryFilter;
    protected AudioKey audioKey;
    protected transient AudioData data = null;
    protected transient volatile Status status = Status.Stopped;
    protected transient volatile int channel = -1;
    protected Vector3f velocity = new Vector3f();
    protected boolean reverbEnabled = true;
    protected float maxDistance = 200; // 200 meters
    protected float refDistance = 10; // 10 meters
    protected Filter reverbFilter;
    private boolean directional = false;
    protected Vector3f direction = new Vector3f(0, 0, 1);
    protected float innerAngle = 360;
    protected float outerAngle = 360;
    protected boolean positional = true;

    /**
     * <code>Status</code> indicates the current status of the audio node.
     */
    public enum Status {
        /**
         * The audio node is currently playing. This will be set if
         * {@link AudioNode#play() } is called.
         */
        Playing,
        
        /**
         * The audio node is currently paused.
         */
        Paused,
        
        /**
         * The audio node is currently stopped.
         * This will be set if {@link AudioNode#stop() } is called 
         * or the audio has reached the end of the file.
         */
        Stopped,
    }

    /**
     * Creates a new <code>AudioNode</code> without any audio data set.
     */
    public AudioNode() {
    }

    /**
     * Creates a new <code>AudioNode</code> without any audio data set.
     * 
     * @param audioRenderer The audio renderer to use for playing. Cannot be null.
     *
     * @deprecated AudioRenderer parameter is ignored.
     */
    public AudioNode(AudioRenderer audioRenderer) {
    }

    /**
     * Creates a new <code>AudioNode</code> with the given data and key.
     * 
     * @param audioRenderer The audio renderer to use for playing. Cannot be null.
     * @param audioData The audio data contains the audio track to play.
     * @param audioKey The audio key that was used to load the AudioData
     *
     * @deprecated AudioRenderer parameter is ignored.
     */
    public AudioNode(AudioRenderer audioRenderer, AudioData audioData, AudioKey audioKey) {
        setAudioData(audioData, audioKey);
    }

    /**
     * Creates a new <code>AudioNode</code> with the given data and key.
     * 
     * @param audioData The audio data contains the audio track to play.
     * @param audioKey The audio key that was used to load the AudioData
     */
    public AudioNode(AudioData audioData, AudioKey audioKey) {
        setAudioData(audioData, audioKey);
    }

    /**
     * Creates a new <code>AudioNode</code> with the given audio file.
     * 
     * @param audioRenderer The audio renderer to use for playing. Cannot be null.
     * @param assetManager The asset manager to use to load the audio file
     * @param name The filename of the audio file
     * @param stream If true, the audio will be streamed gradually from disk, 
     *               otherwise, it will be buffered.
     * @param streamCache If stream is also true, then this specifies if
     * the stream cache is used. When enabled, the audio stream will
     * be read entirely but not decoded, allowing features such as 
     * seeking, looping and determining duration.
     *
     * @deprecated AudioRenderer parameter is ignored.
     */
    public AudioNode(AudioRenderer audioRenderer, AssetManager assetManager, String name, boolean stream, boolean streamCache) {
        this.audioKey = new AudioKey(name, stream, streamCache);
        this.data = (AudioData) assetManager.loadAsset(audioKey);
    }

    /**
     * Creates a new <code>AudioNode</code> with the given audio file.
     * 
     * @param assetManager The asset manager to use to load the audio file
     * @param name The filename of the audio file
     * @param stream If true, the audio will be streamed gradually from disk, 
     *               otherwise, it will be buffered.
     * @param streamCache If stream is also true, then this specifies if
     * the stream cache is used. When enabled, the audio stream will
     * be read entirely but not decoded, allowing features such as 
     * seeking, looping and determining duration.
     */
    public AudioNode(AssetManager assetManager, String name, boolean stream, boolean streamCache) {
        this.audioKey = new AudioKey(name, stream, streamCache);
        this.data = (AudioData) assetManager.loadAsset(audioKey);
    }
    
    /**
     * Creates a new <code>AudioNode</code> with the given audio file.
     * 
     * @param audioRenderer The audio renderer to use for playing. Cannot be null.
     * @param assetManager The asset manager to use to load the audio file
     * @param name The filename of the audio file
     * @param stream If true, the audio will be streamed gradually from disk, 
     *               otherwise, it will be buffered.
     *
     * @deprecated AudioRenderer parameter is ignored.
     */
    public AudioNode(AudioRenderer audioRenderer, AssetManager assetManager, String name, boolean stream) {
        this(audioRenderer, assetManager, name, stream, false);
    }

    /**
     * Creates a new <code>AudioNode</code> with the given audio file.
     * 
     * @param assetManager The asset manager to use to load the audio file
     * @param name The filename of the audio file
     * @param stream If true, the audio will be streamed gradually from disk, 
     *               otherwise, it will be buffered.
     */
    public AudioNode(AssetManager assetManager, String name, boolean stream) {
        this(assetManager, name, stream, false);
    }

    /**
     * Creates a new <code>AudioNode</code> with the given audio file.
     * 
     * @param audioRenderer The audio renderer to use for playing. Cannot be null.
     * @param assetManager The asset manager to use to load the audio file
     * @param name The filename of the audio file
     * 
     * @deprecated AudioRenderer parameter is ignored.
     */
    public AudioNode(AudioRenderer audioRenderer, AssetManager assetManager, String name) {
        this(assetManager, name, false);
    }
    
    /**
     * Creates a new <code>AudioNode</code> with the given audio file.
     * 
     * @param assetManager The asset manager to use to load the audio file
     * @param name The filename of the audio file
     */
    public AudioNode(AssetManager assetManager, String name) {
        this(assetManager, name, false);
    }
    
    protected AudioRenderer getRenderer() {
        AudioRenderer result = AudioContext.getAudioRenderer();
        if( result == null )
            throw new IllegalStateException( "No audio renderer available, make sure call is being performed on render thread." );
        return result;            
    }
    
    /**
     * Start playing the audio.
     */
    public void play(){
        getRenderer().playSource(this);
    }

    /**
     * Start playing an instance of this audio. This method can be used
     * to play the same <code>AudioNode</code> multiple times. Note
     * that changes to the parameters of this AudioNode will not effect the 
     * instances already playing.
     */
    public void playInstance(){
        getRenderer().playSourceInstance(this);
    }
    
    /**
     * Stop playing the audio that was started with {@link AudioNode#play() }.
     */
    public void stop(){
        getRenderer().stopSource(this);
    }
    
    /**
     * Pause the audio that was started with {@link AudioNode#play() }.
     */
    public void pause(){
        getRenderer().pauseSource(this);
    }
    
    /**
     * Do not use.
     */
    public final void setChannel(int channel) {
        if (status != Status.Stopped) {
            throw new IllegalStateException("Can only set source id when stopped");
        }

        this.channel = channel;
    }

    /**
     * Do not use.
     */
    public int getChannel() {
        return channel;
    }

    /**
     * @return The {#link Filter dry filter} that is set.
     * @see AudioNode#setDryFilter(com.jme3.audio.Filter) 
     */
    public Filter getDryFilter() {
        return dryFilter;
    }

    /**
     * Set the dry filter to use for this audio node.
     * 
     * When {@link AudioNode#setReverbEnabled(boolean) reverb} is used, 
     * the dry filter will only influence the "dry" portion of the audio, 
     * e.g. not the reverberated parts of the AudioNode playing.
     * 
     * See the relevent documentation for the {@link Filter} to determine
     * the effect.
     * 
     * @param dryFilter The filter to set, or null to disable dry filter.
     */
    public void setDryFilter(Filter dryFilter) {
        this.dryFilter = dryFilter;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.DryFilter);
    }

    /**
     * Set the audio data to use for the audio. Note that this method
     * can only be called once, if for example the audio node was initialized
     * without an {@link AudioData}.
     * 
     * @param audioData The audio data contains the audio track to play.
     * @param audioKey The audio key that was used to load the AudioData
     */
    public void setAudioData(AudioData audioData, AudioKey audioKey) {
        if (data != null) {
            throw new IllegalStateException("Cannot change data once its set");
        }

        data = audioData;
        this.audioKey = audioKey;
    }

    /**
     * @return The {@link AudioData} set previously with 
     * {@link AudioNode#setAudioData(com.jme3.audio.AudioData, com.jme3.audio.AudioKey) }
     * or any of the constructors that initialize the audio data.
     */
    public AudioData getAudioData() {
        return data;
    }

    /**
     * @return The {@link Status} of the audio node. 
     * The status will be changed when either the {@link AudioNode#play() }
     * or {@link AudioNode#stop() } methods are called.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Do not use.
     */
    public final void setStatus(Status status) {
        this.status = status;
    }

    /**
     * @return True if the audio will keep looping after it is done playing,
     * otherwise, false.
     * @see AudioNode#setLooping(boolean)
     */
    public boolean isLooping() {
        return loop;
    }

    /**
     * Set the looping mode for the audio node. The default is false.
     * 
     * @param loop True if the audio should keep looping after it is done playing.
     */
    public void setLooping(boolean loop) {
        this.loop = loop;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.Looping);
    }

    /**
     * @return The pitch of the audio, also the speed of playback.
     * 
     * @see AudioNode#setPitch(float) 
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Set the pitch of the audio, also the speed of playback.
     * The value must be between 0.5 and 2.0.
     * 
     * @param pitch The pitch to set.
     * @throws IllegalArgumentException If pitch is not between 0.5 and 2.0.
     */
    public void setPitch(float pitch) {
        if (pitch < 0.5f || pitch > 2.0f) {
            throw new IllegalArgumentException("Pitch must be between 0.5 and 2.0");
        }

        this.pitch = pitch;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.Pitch);
    }

    /**
     * @return The volume of this audio node.
     * 
     * @see AudioNode#setVolume(float)
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Set the volume of this audio node.
     * 
     * The volume is specified as gain. 1.0 is the default.
     * 
     * @param volume The volume to set.
     * @throws IllegalArgumentException If volume is negative
     */
    public void setVolume(float volume) {
        if (volume < 0f) {
            throw new IllegalArgumentException("Volume cannot be negative");
        }

        this.volume = volume;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.Volume);
    }

    /**
     * @return The time offset in seconds when the sound will start playing.
     */
    public float getTimeOffset() {
        return timeOffset;
    }

    /**
     * Set the time offset in seconds when the sound will start playing.
     * 
     * @param timeOffset The time offset
     * @throws IllegalArgumentException If timeOffset is negative
     */
    public void setTimeOffset(float timeOffset) {
        if (timeOffset < 0f) {
            throw new IllegalArgumentException("Time offset cannot be negative");
        }

        this.timeOffset = timeOffset;
        if (data instanceof AudioStream) {
            System.out.println("request setTime");
            ((AudioStream) data).setTime(timeOffset);
        }else if(status == Status.Playing){
            stop();
            play();
        }
    }

    /**
     * @return The velocity of the audio node.
     * 
     * @see AudioNode#setVelocity(com.jme3.math.Vector3f)
     */
    public Vector3f getVelocity() {
        return velocity;
    }

    /**
     * Set the velocity of the audio node. The velocity is expected
     * to be in meters. Does nothing if the audio node is not positional.
     * 
     * @param velocity The velocity to set.
     * @see AudioNode#setPositional(boolean)
     */
    public void setVelocity(Vector3f velocity) {
        this.velocity.set(velocity);
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.Velocity);
    }

    /**
     * @return True if reverb is enabled, otherwise false.
     * 
     * @see AudioNode#setReverbEnabled(boolean)
     */
    public boolean isReverbEnabled() {
        return reverbEnabled;
    }

    /**
     * Set to true to enable reverberation effects for this audio node.
     * Does nothing if the audio node is not positional.
     * <br/>
     * When enabled, the audio environment set with 
     * {@link AudioRenderer#setEnvironment(com.jme3.audio.Environment) }
     * will apply a reverb effect to the audio playing from this audio node.
     * 
     * @param reverbEnabled True to enable reverb.
     */
    public void setReverbEnabled(boolean reverbEnabled) {
        this.reverbEnabled = reverbEnabled;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.ReverbEnabled);
    }

    /**
     * @return Filter for the reverberations of this audio node.
     * 
     * @see AudioNode#setReverbFilter(com.jme3.audio.Filter) 
     */
    public Filter getReverbFilter() {
        return reverbFilter;
    }

    /**
     * Set the reverb filter for this audio node.
     * <br/>
     * The reverb filter will influence the reverberations
     * of the audio node playing. This only has an effect if
     * reverb is enabled.
     * 
     * @param reverbFilter The reverb filter to set.
     * @see AudioNode#setDryFilter(com.jme3.audio.Filter)
     */
    public void setReverbFilter(Filter reverbFilter) {
        this.reverbFilter = reverbFilter;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.ReverbFilter);
    }

    /**
     * @return Max distance for this audio node.
     * 
     * @see AudioNode#setMaxDistance(float)
     */
    public float getMaxDistance() {
        return maxDistance;
    }

    /**
     * Set the maximum distance for the attenuation of the audio node.
     * Does nothing if the audio node is not positional.
     * <br/>
     * The maximum distance is the distance beyond which the audio
     * node will no longer be attenuated.  Normal attenuation is logarithmic
     * from refDistance (it reduces by half when the distance doubles).
     * Max distance sets where this fall-off stops and the sound will never
     * get any quieter than at that distance.  If you want a sound to fall-off
     * very quickly then set ref distance very short and leave this distance
     * very long.
     * 
     * @param maxDistance The maximum playing distance.
     * @throws IllegalArgumentException If maxDistance is negative
     */
    public void setMaxDistance(float maxDistance) {
        if (maxDistance < 0) {
            throw new IllegalArgumentException("Max distance cannot be negative");
        }

        this.maxDistance = maxDistance;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.MaxDistance);
    }

    /**
     * @return The reference playing distance for the audio node.
     * 
     * @see AudioNode#setRefDistance(float) 
     */
    public float getRefDistance() {
        return refDistance;
    }

    /**
     * Set the reference playing distance for the audio node.
     * Does nothing if the audio node is not positional.
     * <br/>
     * The reference playing distance is the distance at which the
     * audio node will be exactly half of its volume.
     * 
     * @param refDistance The reference playing distance.
     * @throws  IllegalArgumentException If refDistance is negative
     */
    public void setRefDistance(float refDistance) {
        if (refDistance < 0) {
            throw new IllegalArgumentException("Reference distance cannot be negative");
        }

        this.refDistance = refDistance;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.RefDistance);
    }

    /**
     * @return True if the audio node is directional
     * 
     * @see AudioNode#setDirectional(boolean) 
     */
    public boolean isDirectional() {
        return directional;
    }

    /**
     * Set the audio node to be directional.
     * Does nothing if the audio node is not positional.
     * <br/>
     * After setting directional, you should call 
     * {@link AudioNode#setDirection(com.jme3.math.Vector3f) }
     * to set the audio node's direction.
     * 
     * @param directional If the audio node is directional
     */
    public void setDirectional(boolean directional) {
        this.directional = directional;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.IsDirectional);
    }

    /**
     * @return The direction of this audio node.
     * 
     * @see AudioNode#setDirection(com.jme3.math.Vector3f)
     */
    public Vector3f getDirection() {
        return direction;
    }

    /**
     * Set the direction of this audio node.
     * Does nothing if the audio node is not directional.
     * 
     * @param direction 
     * @see AudioNode#setDirectional(boolean) 
     */
    public void setDirection(Vector3f direction) {
        this.direction = direction;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.Direction);
    }

    /**
     * @return The directional audio node, cone inner angle.
     * 
     * @see AudioNode#setInnerAngle(float) 
     */
    public float getInnerAngle() {
        return innerAngle;
    }

    /**
     * Set the directional audio node cone inner angle.
     * Does nothing if the audio node is not directional.
     * 
     * @param innerAngle The cone inner angle.
     */
    public void setInnerAngle(float innerAngle) {
        this.innerAngle = innerAngle;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.InnerAngle);
    }

    /**
     * @return The directional audio node, cone outer angle.
     * 
     * @see AudioNode#setOuterAngle(float) 
     */
    public float getOuterAngle() {
        return outerAngle;
    }

    /**
     * Set the directional audio node cone outer angle.
     * Does nothing if the audio node is not directional.
     * 
     * @param outerAngle The cone outer angle.
     */
    public void setOuterAngle(float outerAngle) {
        this.outerAngle = outerAngle;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.OuterAngle);
    }

    /**
     * @return True if the audio node is positional.
     * 
     * @see AudioNode#setPositional(boolean) 
     */
    public boolean isPositional() {
        return positional;
    }

    /**
     * Set the audio node as positional.
     * The position, velocity, and distance parameters effect positional
     * audio nodes. Set to false if the audio node should play in "headspace".
     * 
     * @param positional True if the audio node should be positional, otherwise
     * false if it should be headspace.
     */
    public void setPositional(boolean positional) {
        this.positional = positional;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.IsPositional);
    }

    @Override
    public void updateGeometricState(){
        boolean updatePos = false;
        if ((refreshFlags & RF_TRANSFORM) != 0){
            updatePos = true;
        }
        
        super.updateGeometricState();

        if (updatePos && channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.Position);
    }

    @Override
    public AudioNode clone(){
        AudioNode clone = (AudioNode) super.clone();
        
        clone.direction = direction.clone();
        clone.velocity  = velocity.clone();
        
        return clone;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(audioKey, "audio_key", null);
        oc.write(loop, "looping", false);
        oc.write(volume, "volume", 1);
        oc.write(pitch, "pitch", 1);
        oc.write(timeOffset, "time_offset", 0);
        oc.write(dryFilter, "dry_filter", null);

        oc.write(velocity, "velocity", null);
        oc.write(reverbEnabled, "reverb_enabled", false);
        oc.write(reverbFilter, "reverb_filter", null);
        oc.write(maxDistance, "max_distance", 20);
        oc.write(refDistance, "ref_distance", 10);

        oc.write(directional, "directional", false);
        oc.write(direction, "direction", null);
        oc.write(innerAngle, "inner_angle", 360);
        oc.write(outerAngle, "outer_angle", 360);
        
        oc.write(positional, "positional", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        
        // NOTE: In previous versions of jME3, audioKey was actually
        // written with the name "key". This has been changed
        // to "audio_key" in case Spatial's key will be written as "key".
        if (ic.getSavableVersion(AudioNode.class) == 0){
            audioKey = (AudioKey) ic.readSavable("key", null);
        }else{
            audioKey = (AudioKey) ic.readSavable("audio_key", null);
        }
        
        loop = ic.readBoolean("looping", false);
        volume = ic.readFloat("volume", 1);
        pitch = ic.readFloat("pitch", 1);
        timeOffset = ic.readFloat("time_offset", 0);
        dryFilter = (Filter) ic.readSavable("dry_filter", null);

        velocity = (Vector3f) ic.readSavable("velocity", null);
        reverbEnabled = ic.readBoolean("reverb_enabled", false);
        reverbFilter = (Filter) ic.readSavable("reverb_filter", null);
        maxDistance = ic.readFloat("max_distance", 20);
        refDistance = ic.readFloat("ref_distance", 10);

        directional = ic.readBoolean("directional", false);
        direction = (Vector3f) ic.readSavable("direction", null);
        innerAngle = ic.readFloat("inner_angle", 360);
        outerAngle = ic.readFloat("outer_angle", 360);
        
        positional = ic.readBoolean("positional", false);
        
        if (audioKey != null) {
            try {
                data = im.getAssetManager().loadAudio(audioKey);
            } catch (AssetNotFoundException ex){
                Logger.getLogger(AudioNode.class.getName()).log(Level.FINE, "Cannot locate {0} for audio node {1}", new Object[]{audioKey, key});
                data = PlaceholderAssets.getPlaceholderAudio();
            }
        }
    }

    @Override
    public String toString() {
        String ret = getClass().getSimpleName()
                + "[status=" + status;
        if (volume != 1f) {
            ret += ", vol=" + volume;
        }
        if (pitch != 1f) {
            ret += ", pitch=" + pitch;
        }
        return ret + "]";
    }
}
