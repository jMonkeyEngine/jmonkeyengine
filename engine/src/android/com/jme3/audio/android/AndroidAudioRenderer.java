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
package com.jme3.audio.android;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;

import com.jme3.audio.ListenerParam;
import com.jme3.audio.AudioParam;
import com.jme3.audio.AudioBuffer;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioNode.Status;
import com.jme3.audio.AudioStream;
import com.jme3.audio.Environment;
import com.jme3.audio.Filter;
import com.jme3.audio.Listener;
import com.jme3.audio.LowPassFilter;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class is the android implementation for {@link AudioRenderer}
 * @author larynx
 *
 */
public class AndroidAudioRenderer implements AudioRenderer, SoundPool.OnLoadCompleteListener
{

    private static final Logger logger = Logger.getLogger(AndroidAudioRenderer.class.getName());
    private final static int MAX_NUM_CHANNELS = 16;
    
    private SoundPool soundPool = null;
    private final AudioManager manager;
    private final Context context;
    private final AssetManager am;
    
    private HashMap<Integer, AudioNode> mapLoadingAudioNodes = new HashMap<Integer, AudioNode>();
    
    private final AtomicBoolean lastLoadCompleted = new AtomicBoolean();
     

    private Listener listener;
    private boolean audioDisabled = false;

   

    public AndroidAudioRenderer(Activity context)
    {
        this.context = context;
        manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        context.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        am = context.getAssets();
    }
    
    @Override
    public void initialize()
    {
        soundPool = new SoundPool(MAX_NUM_CHANNELS, AudioManager.STREAM_MUSIC, 0);  
        soundPool.setOnLoadCompleteListener(this);
    }
    

    private void updateFilter(Filter f)
    {
        throw new UnsupportedOperationException("Filter type unsupported: " + f.getClass().getName());
    }

    @Override
    public void updateSourceParam(AudioNode src, AudioParam param)
    {
            if (audioDisabled)
                return;
 
            if (src.getChannel() < 0) 
                return;
           
            assert src.getChannel() >= 0;

            
            switch (param){
                case Position:
                    if (!src.isPositional())
                        return;

                    Vector3f pos = src.getWorldTranslation();
                    break;
                case Velocity:
                    if (!src.isPositional())
                        return;
                    
                    Vector3f vel = src.getVelocity();
                    break;
                case MaxDistance:
                    if (!src.isPositional())
                        return;
                    break;
                case RefDistance:
                    if (!src.isPositional())
                        return;
                    break;
                case ReverbFilter:
                    if (!src.isPositional() || !src.isReverbEnabled())
                        return;
                    break;
                case ReverbEnabled:
                    if (!src.isPositional())
                        return;

                    if (src.isReverbEnabled()){
                        updateSourceParam(src, AudioParam.ReverbFilter);
                    }
                    break;
                case IsPositional:
                    break;
                case Direction:
                    if (!src.isDirectional())
                        return;

                    Vector3f dir = src.getDirection();                   
                    break;
                case InnerAngle:
                    if (!src.isDirectional())
                        return;
                    break;
                case OuterAngle:
                    if (!src.isDirectional())
                        return;
                    break;
                case IsDirectional:
                    if (src.isDirectional()){
                        updateSourceParam(src, AudioParam.Direction);
                        updateSourceParam(src, AudioParam.InnerAngle);
                        updateSourceParam(src, AudioParam.OuterAngle);
                    }else{
                    }
                    break;
                case DryFilter:
                    if (src.getDryFilter() != null){
                        Filter f = src.getDryFilter();
                        if (f.isUpdateNeeded()){
                            updateFilter(f);

                        }
                    }
                    break;
                case Looping:
                    if (src.isLooping()){
                    }
                    break;
                case Volume:
                    
                    soundPool.setVolume(src.getChannel(), src.getVolume(), src.getVolume());

                    break;
                case Pitch:

                    break;
            }
        
    }
    
    @Override
    public void updateListenerParam(Listener listener, ListenerParam param)
    {
            if (audioDisabled)
                return;
            
            switch (param){
                case Position:
                    Vector3f pos = listener.getLocation();

                    break;
                case Rotation:
                    Vector3f dir = listener.getDirection();
                    Vector3f up  = listener.getUp();

                    break;
                case Velocity:
                    Vector3f vel = listener.getVelocity();

                    break;
                case Volume:
                    //alListenerf(AL_GAIN, listener.getVolume());
                    break;
            }

    }


    public void update(float tpf)
    {
        // does nothing
    }

    public void updateInThread(float tpf)
    {
        if (audioDisabled)
            return;
        if (!audioDisabled)
            return;

    }

    public void setListener(Listener listener) 
    {
            if (audioDisabled)
                return;

            if (this.listener != null){
                // previous listener no longer associated with current
                // renderer
                this.listener.setRenderer(null);
            }
            
            this.listener = listener;
            this.listener.setRenderer(this);            

    }
    
    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status)
    {
        //lastLoadCompleted.set(true);
        
        if (status == 0)
        {
            AudioNode src = mapLoadingAudioNodes.get(sampleId);
            if (src.getAudioData() instanceof AndroidAudioData)
            {
                AndroidAudioData audioData = (AndroidAudioData)src.getAudioData();
                
                int channelIndex;
                channelIndex = soundPool.play(audioData.getSoundId(), 1f, 1f, 1, -1, 1f);
                src.setChannel(channelIndex);
                // Playing started ?
                if (src.getChannel() > 0)
                {
                    src.setStatus(Status.Playing);
                }
            }
            else
            {
                throw new IllegalArgumentException("AudioData is not of type AndroidAudioData for AudioNode " + src.toString());
            }
        }
    }
    
    @Override
    public void cleanup()
    {
        if (soundPool != null)
        {
            for (AudioNode src: mapLoadingAudioNodes.values())
            {
                if ((src.getStatus() == Status.Playing) && (src.getChannel() > 0))
                {
                    soundPool.stop(src.getChannel());
                }
                
                if (src.getAudioData() instanceof AndroidAudioData)
                {
                    AndroidAudioData audioData = (AndroidAudioData)src.getAudioData();
                    if (audioData.getSoundId() > 0)
                    {
                        soundPool.unload(audioData.getSoundId());
                    }                
                }                        
            }
        
            soundPool.release();
            soundPool = null;
        }
    }

    public void playSourceInstance(AudioNode src)
    {
            if (audioDisabled)
                return;
            
            AndroidAudioData audioData;
            int soundId = 0;
            
            if (src.getAudioData() instanceof AndroidAudioData)
            {
                audioData = (AndroidAudioData)src.getAudioData();
                if (audioData.isUpdateNeeded() || (audioData.getSoundId() == 0))
                {
                    if (audioData.getSoundId() > 0)
                    {
                        if (src.getChannel() > 0)
                        {
                            soundPool.stop(src.getChannel());
                            src.setChannel(-1);
                        }
                        soundPool.unload(audioData.getSoundId());
                    }
                                                          
                    try 
                    {                                           
                        soundId = soundPool.load(am.openFd(audioData.getAssetKey().getName()), 1);   
                    } 
                    catch (IOException e) 
                    {
                        logger.log(Level.SEVERE, "Failed to load sound " + audioData.getAssetKey().getName(), e);
                        soundId = -1;
                    }
                    audioData.setSoundId(soundId);                    
                }
            }
            else
            {
                throw new IllegalArgumentException("AudioData is not of type AndroidAudioData for AudioNode " + src.toString());
            }
            
            // Sound failed to load ?
            if (audioData.getSoundId() <= 0)
            {
                throw new IllegalArgumentException("Failed to load: " + audioData.getAssetKey().getName());
            }
            else
            {
                int channelIndex;
                channelIndex = soundPool.play(audioData.getSoundId(), 1f, 1f, 1, -1, 1f);
                if (channelIndex == 0)
                {
                    // Loading is not finished
                    // Store the soundId and the AudioNode for async loading and later play start
                    mapLoadingAudioNodes.put(audioData.getSoundId(), src);
                }                
                src.setChannel(channelIndex);
            }
            
            // Playing started ?
            if (src.getChannel() > 0)
            {
                src.setStatus(Status.Playing);
            }
 
    }

    
    public void playSource(AudioNode src) 
    {
            if (audioDisabled)
                return;

            //assert src.getStatus() == Status.Stopped || src.getChannel() == -1;

            if (src.getStatus() == Status.Playing)
            {
                return;
            }
            else if (src.getStatus() == Status.Stopped)
            {
                playSourceInstance(src);                
            }
            
        
    }

    
    public void pauseSource(AudioNode src) 
    {
            if (audioDisabled)
                return;
            
            if (src.getStatus() == Status.Playing)
            {
                assert src.getChannel() != -1;

                if (src.getChannel() > 0)
                {
                    soundPool.pause(src.getChannel());
                }
                src.setStatus(Status.Paused);
            }

    }

    
    public void stopSource(AudioNode src) 
    {
            if (audioDisabled)
                return;
            
            if (src.getStatus() != Status.Stopped){
                int chan = src.getChannel();
                assert chan != -1; // if it's not stopped, must have id
                
                if (src.getChannel() > 0)
                {
                    soundPool.stop(src.getChannel());
                    src.setChannel(-1);
                }

                src.setStatus(Status.Stopped);                                
            }
            
            AndroidAudioData audioData;                       
            if (src.getAudioData() instanceof AndroidAudioData)
            {
                audioData = (AndroidAudioData)src.getAudioData();
                if (audioData.getSoundId() > 0)
                {
                    soundPool.unload(audioData.getSoundId());
                }
                audioData.setSoundId(0);
                
            }
            else
            {
                throw new IllegalArgumentException("AudioData is not of type AndroidAudioData for AudioNode " + src.toString());
            }
            

    }

    private int convertFormat(AudioData ad)
    {
        /*
        switch (ad.getBitsPerSample()){
            case 8:
                if (ad.getChannels() == 1)
                    return AL_FORMAT_MONO8;
                else if (ad.getChannels() == 2)
                    return AL_FORMAT_STEREO8;

                break;
            case 16:
                if (ad.getChannels() == 1)
                    return AL_FORMAT_MONO16;
                else
                    return AL_FORMAT_STEREO16;
        }
        */
        throw new UnsupportedOperationException("Unsupported channels/bits combination: "+
                                                "bits="+ad.getBitsPerSample()+", channels="+ad.getChannels());
    }

    public void updateAudioData(AndroidAudioData data)
    {
        throw new UnsupportedOperationException("updateAudioData");
    }

    @Override
    public void deleteAudioData(AudioData ad) 
    {                     
        if (ad instanceof AndroidAudioData)
        {
            if (((AndroidAudioData)ad).getSoundId() > 0)
            {
                soundPool.unload(((AndroidAudioData)ad).getSoundId());
            }
            ((AndroidAudioData)ad).setSoundId(0);
            
        }
        else
        {
            throw new IllegalArgumentException("AudioData is not of type AndroidAudioData in deleteAudioData");
        }        
    }

    @Override
    public void setEnvironment(Environment env) {
        // TODO Auto-generated method stub
        
    }




}
