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
import android.util.Log;

import com.jme3.asset.AssetKey;
import com.jme3.audio.AudioNode.Status;
import com.jme3.audio.*;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the android implementation for {@link AudioRenderer}
 * 
 * @author larynx
 * @author plan_rich
 */
public class AndroidAudioRenderer implements AudioRenderer,
        SoundPool.OnLoadCompleteListener, MediaPlayer.OnCompletionListener {

    private static final Logger logger = Logger.getLogger(AndroidAudioRenderer.class.getName());
    private final static int MAX_NUM_CHANNELS = 16;
    private final HashMap<AudioNode, MediaPlayer> musicPlaying = new HashMap<AudioNode, MediaPlayer>();
    private SoundPool soundPool = null;
    private final Vector3f listenerPosition = new Vector3f();
    // For temp use
    private final Vector3f distanceVector = new Vector3f();
    private final Context context;
    private final AssetManager assetManager;
    private HashMap<Integer, AudioNode> soundpoolStillLoading = new HashMap<Integer, AudioNode>();
    private Listener listener;
    private boolean audioDisabled = false;
    private final AudioManager manager;

    public AndroidAudioRenderer(Activity context) {
        this.context = context;
        manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        context.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        assetManager = context.getAssets();
    }

    @Override
    public void initialize() {
        soundPool = new SoundPool(MAX_NUM_CHANNELS, AudioManager.STREAM_MUSIC,
                0);
        soundPool.setOnLoadCompleteListener(this);
    }

    @Override
    public void updateSourceParam(AudioNode src, AudioParam param) {
        // logger.log(Level.INFO, "updateSourceParam " + param);

        if (audioDisabled) {
            return;
        }

        if (src.getChannel() < 0) {
            return;
        }

        switch (param) {
            case Position:
                if (!src.isPositional()) {
                    return;
                }

                Vector3f pos = src.getWorldTranslation();
                break;
            case Velocity:
                if (!src.isPositional()) {
                    return;
                }

                Vector3f vel = src.getVelocity();
                break;
            case MaxDistance:
                if (!src.isPositional()) {
                    return;
                }
                break;
            case RefDistance:
                if (!src.isPositional()) {
                    return;
                }
                break;
            case ReverbFilter:
                if (!src.isPositional() || !src.isReverbEnabled()) {
                    return;
                }
                break;
            case ReverbEnabled:
                if (!src.isPositional()) {
                    return;
                }

                if (src.isReverbEnabled()) {
                    updateSourceParam(src, AudioParam.ReverbFilter);
                }
                break;
            case IsPositional:
                break;
            case Direction:
                if (!src.isDirectional()) {
                    return;
                }

                Vector3f dir = src.getDirection();
                break;
            case InnerAngle:
                if (!src.isDirectional()) {
                    return;
                }
                break;
            case OuterAngle:
                if (!src.isDirectional()) {
                    return;
                }
                break;
            case IsDirectional:
                if (src.isDirectional()) {
                    updateSourceParam(src, AudioParam.Direction);
                    updateSourceParam(src, AudioParam.InnerAngle);
                    updateSourceParam(src, AudioParam.OuterAngle);
                } else {
                }
                break;
            case DryFilter:
                if (src.getDryFilter() != null) {
                    Filter f = src.getDryFilter();
                    if (f.isUpdateNeeded()) {
                        // updateFilter(f);
                    }
                }
                break;
            case Looping:
                if (src.isLooping()) {
                }
                break;
            case Volume:

                soundPool.setVolume(src.getChannel(), src.getVolume(),
                        src.getVolume());

                break;
            case Pitch:

                break;
        }

    }

    @Override
    public void updateListenerParam(Listener listener, ListenerParam param) {
        // logger.log(Level.INFO, "updateListenerParam " + param);
        if (audioDisabled) {
            return;
        }

        switch (param) {
            case Position:
                listenerPosition.set(listener.getLocation());

                break;
            case Rotation:
                Vector3f dir = listener.getDirection();
                Vector3f up = listener.getUp();

                break;
            case Velocity:
                Vector3f vel = listener.getVelocity();

                break;
            case Volume:
                // alListenerf(AL_GAIN, listener.getVolume());
                break;
        }

    }

    @Override
    public void update(float tpf) {
        float distance;
        float volume;

        // Loop over all mediaplayers
        for (AudioNode src : musicPlaying.keySet()) {

            MediaPlayer mp = musicPlaying.get(src);
            {
                // Calc the distance to the listener
                distanceVector.set(listenerPosition);
                distanceVector.subtractLocal(src.getLocalTranslation());
                distance = FastMath.abs(distanceVector.length());

                if (distance < src.getRefDistance()) {
                    distance = src.getRefDistance();
                }
                if (distance > src.getMaxDistance()) {
                    distance = src.getMaxDistance();
                }
                volume = src.getRefDistance() / distance;

                AndroidAudioData audioData = (AndroidAudioData) src.getAudioData();

                if (FastMath.abs(audioData.getCurrentVolume() - volume) > FastMath.FLT_EPSILON) {
                    // Left / Right channel get the same volume by now, only
                    // positional
                    mp.setVolume(volume, volume);

                    audioData.setCurrentVolume(volume);
                }
            }
        }
    }

    public void setListener(Listener listener) {
        if (audioDisabled) {
            return;
        }

        if (this.listener != null) {
            // previous listener no longer associated with current
            // renderer
            this.listener.setRenderer(null);
        }

        this.listener = listener;
        this.listener.setRenderer(this);

    }

    @Override
    public void cleanup() {
        // Cleanup sound pool
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }

        // Cleanup media player
        for (AudioNode src : musicPlaying.keySet()) {
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
    public void onCompletion(MediaPlayer mp) {
        mp.seekTo(0);
        mp.stop();
        // XXX: This has bad performance -> maybe change overall structure of
        // mediaplayer in this audiorenderer?
        for (AudioNode src : musicPlaying.keySet()) {
            if (musicPlaying.get(src) == mp) {
                src.setStatus(Status.Stopped);
                break;
            }
        }
    }

    /**
     * Plays using the {@link SoundPool} of Android. Due to hard limitation of
     * the SoundPool: After playing more instances of the sound you only have
     * the channel of the last played instance.
     * 
     * It is not possible to get information about the state of the soundpool of
     * a specific streamid, so removing is not possilbe -> noone knows when
     * sound finished.
     */
    public void playSourceInstance(AudioNode src) {
        if (audioDisabled) {
            return;
        }

        AndroidAudioData audioData = (AndroidAudioData) src.getAudioData();

        if (!(audioData.getAssetKey() instanceof AudioKey)) {
            throw new IllegalArgumentException("Asset is not a AudioKey");
        }

        AudioKey assetKey = (AudioKey) audioData.getAssetKey();

        try {
            if (audioData.getId() < 0) { // found something to load
                int soundId = soundPool.load(
                        assetManager.openFd(assetKey.getName()), 1);
                audioData.setId(soundId);
            }

            int channel = soundPool.play(audioData.getId(), 1f, 1f, 1, 0, 1f);

            if (channel == 0) {
                soundpoolStillLoading.put(audioData.getId(), src);
            } else {
                src.setChannel(channel); // receive a channel at the last
                // playing at least
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE,
                    "Failed to load sound " + assetKey.getName(), e);
            audioData.setId(-1);
        }
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        AudioNode src = soundpoolStillLoading.remove(sampleId);

        if (src == null) {
            logger.warning("Something went terribly wrong! onLoadComplete"
                    + " had sampleId which was not in the HashMap of loading items");
            return;
        }

        AudioData audioData = src.getAudioData();

        if (status == 0) // load was successfull
        {
            int channelIndex;
            channelIndex = soundPool.play(audioData.getId(), 1f, 1f, 1, 0, 1f);
            src.setChannel(channelIndex);
        }
    }

    public void playSource(AudioNode src) {
        if (audioDisabled) {
            return;
        }

        AndroidAudioData audioData = (AndroidAudioData) src.getAudioData();

        MediaPlayer mp = musicPlaying.get(src);
        if (mp == null) {
            mp = new MediaPlayer();
            mp.setOnCompletionListener(this);
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        try {
            AssetKey<?> key = audioData.getAssetKey();

            AssetFileDescriptor afd = assetManager.openFd(key.getName()); // assetKey.getName()
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            mp.prepare();
            mp.setLooping(src.isLooping());
            mp.start();
            src.setChannel(0);
            src.setStatus(Status.Playing);
            musicPlaying.put(src, mp);

        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Pause the current playing sounds. Both from the {@link SoundPool} and the
     * active {@link MediaPlayer}s
     */
    public void pauseAll() {
        if (soundPool != null) {
            soundPool.autoPause();
            for (MediaPlayer mp : musicPlaying.values()) {
                mp.pause();
            }
        }
    }

    /**
     * Resume all paused sounds.
     */
    public void resumeAll() {
        if (soundPool != null) {
            soundPool.autoResume();
            for (MediaPlayer mp : musicPlaying.values()) {
                mp.start(); //no resume -> api says call start to resume
            }
        }
    }

    public void pauseSource(AudioNode src) {
        if (audioDisabled) {
            return;
        }

        MediaPlayer mp = musicPlaying.get(src);
        if (mp != null) {
            mp.pause();
            src.setStatus(Status.Paused);
        } else {
            int channel = src.getChannel();
            if (channel != -1) {
                soundPool.pause(channel); // is not very likley to make
            }											// something useful :)
        }
    }

    public void stopSource(AudioNode src) {
        if (audioDisabled) {
            return;
        }

        // can be stream or buffer -> so try to get mediaplayer
        // if there is non try to stop soundpool
        MediaPlayer mp = musicPlaying.get(src);
        if (mp != null) {
            mp.stop();
            src.setStatus(Status.Paused);
        } else {
            int channel = src.getChannel();
            if (channel != -1) {
                soundPool.pause(channel); // is not very likley to make
                // something useful :)
            }
        }

    }

    @Override
    public void deleteAudioData(AudioData ad) {

        for (AudioNode src : musicPlaying.keySet()) {
            if (src.getAudioData() == ad) {
                MediaPlayer mp = musicPlaying.remove(src);
                mp.stop();
                mp.release();
                src.setStatus(Status.Stopped);
                src.setChannel(-1);
                ad.setId(-1);
                break;
            }
        }

        if (ad.getId() > 0) {
            soundPool.unload(ad.getId());
            ad.setId(-1);
        }
    }

    @Override
    public void setEnvironment(Environment env) {
        // not yet supported
    }

    @Override
    public void deleteFilter(Filter filter) {
    }
}
