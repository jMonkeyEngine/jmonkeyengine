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
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;


import com.jme3.audio.AudioKey;
import com.jme3.audio.ListenerParam;
import com.jme3.audio.AudioParam;

import com.jme3.audio.AudioData;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioNode.Status;

import com.jme3.audio.Environment;
import com.jme3.audio.Filter;
import com.jme3.audio.Listener;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;


import java.io.IOException;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class is the android implementation for {@link AudioRenderer}
 * @author larynx
 *
 */
public class AndroidAudioRenderer implements AudioRenderer, SoundPool.OnLoadCompleteListener, MediaPlayer.OnCompletionListener
{

    private static final Logger logger = Logger.getLogger(AndroidAudioRenderer.class.getName());
    private final static int MAX_NUM_CHANNELS = 16;
    
    private SoundPool soundPool = null;
    private HashMap<AudioNode, MediaPlayer> musicPlaying = new HashMap<AudioNode, MediaPlayer>();   

    private final Vector3f listenerPosition = new Vector3f();
    // For temp use
    private final Vector3f distanceVector = new Vector3f();
    
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

    @Override
    public void updateSourceParam(AudioNode src, AudioParam param)
    {
        logger.log(Level.INFO, "updateSourceParam " + param);
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
                            //updateFilter(f);

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
        logger.log(Level.INFO, "updateListenerParam " + param);
        if (audioDisabled)
            return;
        
        switch (param){
            case Position:
                listenerPosition.set(listener.getLocation());

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

    @Override
    public void update(float tpf)
    {
        float distance;
        float volume;
        
        // Loop over all mediaplayers
        for (AudioNode src : musicPlaying.keySet())
        {
            MediaPlayer mp = musicPlaying.get(src);
            {
                // Calc the distance to the listener
                distanceVector.set(listenerPosition);
                distanceVector.subtractLocal(src.getLocalTranslation());
                distance = FastMath.abs(distanceVector.length());
                
                if (distance < src.getRefDistance())
                    distance = src.getRefDistance();
                if (distance > src.getMaxDistance())
                    distance = src.getMaxDistance();
                volume = src.getRefDistance() / distance;
                
                // Left / Right channel get the same volume by now, only positional
                mp.setVolume(volume, volume);
            }
        }
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
        AudioNode src = mapLoadingAudioNodes.get(sampleId);
        if (src.getAudioData() instanceof AndroidAudioData)
        {
            AndroidAudioData audioData = (AndroidAudioData)src.getAudioData();
            
            if (status == 0)    // load was successfull
            {
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
                src.setChannel(-1);                    
            }
        }
        else
        {
            throw new IllegalArgumentException("AudioData is not of type AndroidAudioData for AudioNode " + src.toString());
        }
    }
    
    @Override
    public void cleanup()
    {
        // Cleanup sound pool
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
        
        // Cleanup media player
        for (AudioNode src : musicPlaying.keySet())
        {
            MediaPlayer mp = musicPlaying.get(src);
            {                             
                mp.stop();
                mp.release();
                src.setStatus(Status.Stopped);
            }
        }
        musicPlaying.clear();                
    }

    @Override
    public void onCompletion(MediaPlayer mp) 
    {
        for (AudioNode src : musicPlaying.keySet())
        {
            if (musicPlaying.get(src) == mp)
            {                     
                mp.seekTo(0);        
                mp.stop();
                src.setStatus(Status.Stopped);
                break;
            }
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
                
                if (audioData.getAssetKey() instanceof AudioKey)
                {                
                    AudioKey assetKey = (AudioKey) audioData.getAssetKey();    
                    
                    // streaming audionodes get played using android mediaplayer, non streaming uses SoundPool
                    if (assetKey.isStream())
                    {
                        MediaPlayer mp;
                        if (musicPlaying.containsKey(src))
                        {
                            mp = musicPlaying.get(src);
                        }
                        else
                        {
                            mp = new MediaPlayer();
                            mp.setOnCompletionListener(this);
                            //mp = MediaPlayer.create(context, new Ur );
                            musicPlaying.put(src, mp);                                
                        }
                        if (!mp.isPlaying())
                        {
                            try {                                                               
                                AssetFileDescriptor afd = am.openFd(assetKey.getName());                                
                                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());

                                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                mp.prepare();
                                mp.setLooping(src.isLooping());
                                mp.start();
                                src.setChannel(1);
                                src.setStatus(Status.Playing);                                
                            } catch (IllegalArgumentException e) 
                            {
                                logger.log(Level.SEVERE, "Failed to play " + assetKey.getName(), e); 
                            } catch (IllegalStateException e) {
                                // TODO Auto-generated catch block
                                logger.log(Level.SEVERE, "Failed to play " + assetKey.getName(), e); 
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                logger.log(Level.SEVERE, "Failed to play " + assetKey.getName(), e); 
                            }
                            
                        }
                        
                    }
                    else
                    {
                        // Low latency Sound effect using SoundPool
                        if (audioData.isUpdateNeeded() || (audioData.getSoundId() <= 0))
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
                                soundId = soundPool.load(am.openFd(assetKey.getName()), 1);   
                            } 
                            catch (IOException e) 
                            {
                                logger.log(Level.SEVERE, "Failed to load sound " + assetKey.getName(), e);
                                soundId = -1;
                            }
                            audioData.setSoundId(soundId);                                                           
                        }
                        
                        // Sound failed to load ?
                        if (audioData.getSoundId() <= 0)
                        {
                            throw new IllegalArgumentException("Failed to load: " + assetKey.getName());
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
                
                }
            }
            else
            {
                throw new IllegalArgumentException("AudioData is not of type AndroidAudioData for AudioNode " + src.toString());
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
                if (src.getAudioData() instanceof AndroidAudioData)
                {
                    AndroidAudioData audioData = (AndroidAudioData)src.getAudioData();                    
                    if (audioData.getAssetKey() instanceof AudioKey)
                    {                
                        AudioKey assetKey = (AudioKey) audioData.getAssetKey();    
                        
                        if (assetKey.isStream())
                        {
                            MediaPlayer mp;
                            if (musicPlaying.containsKey(src))
                            {
                                mp = musicPlaying.get(src);
                                mp.pause();
                                src.setStatus(Status.Paused);
                            }
                        }
                        else
                        {                                                       
                            assert src.getChannel() != -1;
            
                            if (src.getChannel() > 0)
                            {
                                soundPool.pause(src.getChannel());
                                src.setStatus(Status.Paused);
                            }
                        }
                    }
                }
                
            }

    }

    
    public void stopSource(AudioNode src) 
    {
            if (audioDisabled)
                return;
            
            
            if (src.getStatus() != Status.Stopped)
            {                
                if (src.getAudioData() instanceof AndroidAudioData)
                {
                    AndroidAudioData audioData = (AndroidAudioData)src.getAudioData();                    
                    if (audioData.getAssetKey() instanceof AudioKey)
                    {                
                        AudioKey assetKey = (AudioKey) audioData.getAssetKey();                     
                        if (assetKey.isStream())
                        {
                            MediaPlayer mp;
                            if (musicPlaying.containsKey(src))
                            {
                                mp = musicPlaying.get(src);
                                mp.stop();
                                src.setStatus(Status.Stopped);
                                src.setChannel(-1);
                            }
                        }
                        else
                        {                                                       
                            int chan = src.getChannel();
                            assert chan != -1; // if it's not stopped, must have id
                            
                            if (src.getChannel() > 0)
                            {
                                soundPool.stop(src.getChannel());
                                src.setChannel(-1);
                            }
                            
                            src.setStatus(Status.Stopped);
                            
                            if (audioData.getSoundId() > 0)
                            {
                                soundPool.unload(audioData.getSoundId());
                            }
                            audioData.setSoundId(-1);
                            
                            
                            
                        }
                    }
                }
                
            } 
            
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
            AndroidAudioData audioData = (AndroidAudioData)ad;                  
            if (audioData.getAssetKey() instanceof AudioKey)
            {                
                AudioKey assetKey = (AudioKey) audioData.getAssetKey();                     
                if (assetKey.isStream())
                {                    
                    for (AudioNode src : musicPlaying.keySet())
                    {                        
                        if (src.getAudioData() == ad)
                        {
                            MediaPlayer mp = musicPlaying.get(src);
                            mp.stop();
                            mp.release();
                            musicPlaying.remove(src);
                            src.setStatus(Status.Stopped);
                            src.setChannel(-1);
                            break;                           
                        }
                    }
                }
                else
                {
                    if (audioData.getSoundId() > 0)
                    {
                        soundPool.unload(audioData.getSoundId());
                    }
                    audioData.setSoundId(0);   
                }
                
            }
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
