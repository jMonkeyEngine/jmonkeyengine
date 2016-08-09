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
package com.jme3.audio.openal;

import com.jme3.audio.*;
import com.jme3.audio.AudioSource.Status;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObjectManager;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jme3.audio.openal.AL.*;

public class ALAudioRenderer implements AudioRenderer, Runnable {

    private static final Logger logger = Logger.getLogger(ALAudioRenderer.class.getName());
    
    private static final String THREAD_NAME = "jME3 Audio Decoder";
    
    private final NativeObjectManager objManager = new NativeObjectManager();
    // When multiplied by STREAMING_BUFFER_COUNT, will equal 44100 * 2 * 2
    // which is exactly 1 second of audio.
    private static final int BUFFER_SIZE = 35280;
    private static final int STREAMING_BUFFER_COUNT = 5;
    private final static int MAX_NUM_CHANNELS = 64;
    private IntBuffer ib = BufferUtils.createIntBuffer(1);
    private final FloatBuffer fb = BufferUtils.createVector3Buffer(2);
    private final ByteBuffer nativeBuf = BufferUtils.createByteBuffer(BUFFER_SIZE);
    private final byte[] arrayBuf = new byte[BUFFER_SIZE];
    private int[] channels;
    private AudioSource[] chanSrcs;
    private int nextChan = 0;
    private final ArrayList<Integer> freeChans = new ArrayList<Integer>();
    private Listener listener;
    private boolean audioDisabled = false;
    private boolean supportEfx = false;
    private boolean supportPauseDevice = false;
    private int auxSends = 0;
    private int reverbFx = -1;
    private int reverbFxSlot = -1;
    
    // Fill streaming sources every 50 ms
    private static final float UPDATE_RATE = 0.05f;
    private final Thread decoderThread = new Thread(this, THREAD_NAME);
    private final Object threadLock = new Object();

    private final AL al;
    private final ALC alc;
    private final EFX efx;
    
    public ALAudioRenderer(AL al, ALC alc, EFX efx) {
        this.al = al;
        this.alc = alc;
        this.efx = efx;
    }
    
    private void initOpenAL() {
        try {
            if (!alc.isCreated()) {
                alc.createALC();
            }
        } catch (UnsatisfiedLinkError ex) {
            logger.log(Level.SEVERE, "Failed to load audio library", ex);
            audioDisabled = true;
            return;
        }

        // Find maximum # of sources supported by this implementation
        ArrayList<Integer> channelList = new ArrayList<Integer>();
        for (int i = 0; i < MAX_NUM_CHANNELS; i++) {
            int chan = al.alGenSources();
            if (al.alGetError() != 0) {
                break;
            } else {
                channelList.add(chan);
            }
        }

        channels = new int[channelList.size()];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = channelList.get(i);
        }

        ib = BufferUtils.createIntBuffer(channels.length);
        chanSrcs = new AudioSource[channels.length];

        final String deviceName = alc.alcGetString(ALC.ALC_DEVICE_SPECIFIER);

        logger.log(Level.INFO, "Audio Renderer Information\n" +
                        " * Device: {0}\n" +
                        " * Vendor: {1}\n" +
                        " * Renderer: {2}\n" +
                        " * Version: {3}\n" +
                        " * Supported channels: {4}\n" +
                        " * ALC extensions: {5}\n" +
                        " * AL extensions: {6}",
                new Object[]{
                        deviceName,
                        al.alGetString(AL_VENDOR),
                        al.alGetString(AL_RENDERER),
                        al.alGetString(AL_VERSION),
                        channels.length,
                        alc.alcGetString(ALC.ALC_EXTENSIONS),
                        al.alGetString(AL_EXTENSIONS)
                });

        // Pause device is a feature used specifically on Android
        // where the application could be closed but still running,
        // thus the audio context remains open but no audio should be playing.
        supportPauseDevice = alc.alcIsExtensionPresent("ALC_SOFT_pause_device");
        if (!supportPauseDevice) {
            logger.log(Level.WARNING, "Pausing audio device not supported.");
        }
        
        supportEfx = alc.alcIsExtensionPresent("ALC_EXT_EFX");
        if (supportEfx) {
            ib.position(0).limit(1);
            alc.alcGetInteger(EFX.ALC_EFX_MAJOR_VERSION, ib, 1);
            int major = ib.get(0);
            ib.position(0).limit(1);
            alc.alcGetInteger(EFX.ALC_EFX_MINOR_VERSION, ib, 1);
            int minor = ib.get(0);
            logger.log(Level.INFO, "Audio effect extension version: {0}.{1}", new Object[]{major, minor});

            alc.alcGetInteger(EFX.ALC_MAX_AUXILIARY_SENDS, ib, 1);
            auxSends = ib.get(0);
            logger.log(Level.INFO, "Audio max auxiliary sends: {0}", auxSends);

            // create slot
            ib.position(0).limit(1);
            efx.alGenAuxiliaryEffectSlots(1, ib);
            reverbFxSlot = ib.get(0);

            // create effect
            ib.position(0).limit(1);
            efx.alGenEffects(1, ib);
            reverbFx = ib.get(0);
            efx.alEffecti(reverbFx, EFX.AL_EFFECT_TYPE, EFX.AL_EFFECT_REVERB);

            // attach reverb effect to effect slot
            efx.alAuxiliaryEffectSloti(reverbFxSlot, EFX.AL_EFFECTSLOT_EFFECT, reverbFx);
        } else {
            logger.log(Level.WARNING, "OpenAL EFX not available! Audio effects won't work.");
        }
    }
    
    private void destroyOpenAL() {
        if (audioDisabled) {
            alc.destroyALC();
            return;
        }

        // stop any playing channels
        for (int i = 0; i < chanSrcs.length; i++) {
            if (chanSrcs[i] != null) {
                clearChannel(i);
            }
        }

        // delete channel-based sources
        ib.clear();
        ib.put(channels);
        ib.flip();
        al.alDeleteSources(channels.length, ib);

        // delete audio buffers and filters
        objManager.deleteAllObjects(this);

        if (supportEfx) {
            ib.position(0).limit(1);
            ib.put(0, reverbFx);
            efx.alDeleteEffects(1, ib);

            // If this is not allocated, why is it deleted?
            // Commented out to fix native crash in OpenAL.
            ib.position(0).limit(1);
            ib.put(0, reverbFxSlot);
            efx.alDeleteAuxiliaryEffectSlots(1, ib);
        }

        alc.destroyALC();
    }

    public void initialize() {
        if (decoderThread.isAlive()) {
            throw new IllegalStateException("Initialize already called");
        }
        
        // Initialize OpenAL context.
        initOpenAL();

        // Initialize decoder thread.
        // Set high priority to avoid buffer starvation.
        decoderThread.setDaemon(true);
        decoderThread.setPriority(Thread.NORM_PRIORITY + 1);
        decoderThread.start();
    }

    private void checkDead() {
        if (decoderThread.getState() == Thread.State.TERMINATED) {
            throw new IllegalStateException("Decoding thread is terminated");
        }
    }

    public void run() {
        long updateRateNanos = (long) (UPDATE_RATE * 1000000000);
        mainloop:
        while (true) {
            long startTime = System.nanoTime();

            if (Thread.interrupted()) {
                break;
            }

            synchronized (threadLock) {
                updateInDecoderThread(UPDATE_RATE);
            }

            long endTime = System.nanoTime();
            long diffTime = endTime - startTime;

            if (diffTime < updateRateNanos) {
                long desiredEndTime = startTime + updateRateNanos;
                while (System.nanoTime() < desiredEndTime) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        break mainloop;
                    }
                }
            }
        }
    }

    public void cleanup() {
        // kill audio thread
        if (!decoderThread.isAlive()) {
            return;
        }
        
        decoderThread.interrupt();
        try {
            decoderThread.join();
        } catch (InterruptedException ex) {
        }
        
        // destroy OpenAL context
        destroyOpenAL();
    }

    private void updateFilter(Filter f) {
        int id = f.getId();
        if (id == -1) {
            ib.position(0).limit(1);
            efx.alGenFilters(1, ib);
            id = ib.get(0);
            f.setId(id);

            objManager.registerObject(f);
        }

        if (f instanceof LowPassFilter) {
            LowPassFilter lpf = (LowPassFilter) f;
            efx.alFilteri(id, EFX.AL_FILTER_TYPE, EFX.AL_FILTER_LOWPASS);
            efx.alFilterf(id, EFX.AL_LOWPASS_GAIN, lpf.getVolume());
            efx.alFilterf(id, EFX.AL_LOWPASS_GAINHF, lpf.getHighFreqVolume());
        } else {
            throw new UnsupportedOperationException("Filter type unsupported: "
                    + f.getClass().getName());
        }

        f.clearUpdateNeeded();
    }

    @Override
    public float getSourcePlaybackTime(AudioSource src) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return 0;
            }
            
            // See comment in updateSourceParam().
            if (src.getChannel() < 0) {
                return 0;
            }
            
            int id = channels[src.getChannel()];
            AudioData data = src.getAudioData();
            int playbackOffsetBytes = 0;
            
            if (data instanceof AudioStream) {
                // Because audio streams are processed in buffer chunks, 
                // we have to compute the amount of time the stream was already
                // been playing based on the number of buffers that were processed.
                AudioStream stream = (AudioStream) data;
                
                // NOTE: the assumption is that all enqueued buffers are the same size.
                //       this is currently enforced by fillBuffer().
                
                // The number of unenqueued bytes that the decoder thread
                // keeps track of.
                int unqueuedBytes = stream.getUnqueuedBufferBytes();
                
                // Additional processed buffers that the decoder thread
                // did not unenqueue yet (it only updates 20 times per second).
                int unqueuedBytesExtra = al.alGetSourcei(id, AL_BUFFERS_PROCESSED) * BUFFER_SIZE;
                
                // Total additional bytes that need to be considered.
                playbackOffsetBytes = unqueuedBytes; // + unqueuedBytesExtra;
            }
            
            // Add byte offset from source (for both streams and buffers)
            playbackOffsetBytes += al.alGetSourcei(id, AL_BYTE_OFFSET);
            
            // Compute time value from bytes
            // E.g. for 44100 source with 2 channels and 16 bits per sample:
            //    (44100 * 2 * 16 / 8) = 176400
            int bytesPerSecond = (data.getSampleRate() * 
                                  data.getChannels() * 
                                  data.getBitsPerSample() / 8);
            
            return (float)playbackOffsetBytes / bytesPerSecond;
        }
    }
    
    public void updateSourceParam(AudioSource src, AudioParam param) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            // There is a race condition in AudioSource that can
            // cause this to be called for a node that has been
            // detached from its channel.  For example, setVolume()
            // called from the render thread may see that that AudioSource
            // still has a channel value but the audio thread may
            // clear that channel before setVolume() gets to call
            // updateSourceParam() (because the audio stopped playing
            // on its own right as the volume was set).  In this case, 
            // it should be safe to just ignore the update
            if (src.getChannel() < 0) {
                return;
            }

            assert src.getChannel() >= 0;

            int id = channels[src.getChannel()];
            switch (param) {
                case Position:
                    if (!src.isPositional()) {
                        return;
                    }

                    Vector3f pos = src.getPosition();
                    al.alSource3f(id, AL_POSITION, pos.x, pos.y, pos.z);
                    break;
                case Velocity:
                    if (!src.isPositional()) {
                        return;
                    }

                    Vector3f vel = src.getVelocity();
                    al.alSource3f(id, AL_VELOCITY, vel.x, vel.y, vel.z);
                    break;
                case MaxDistance:
                    if (!src.isPositional()) {
                        return;
                    }

                    al.alSourcef(id, AL_MAX_DISTANCE, src.getMaxDistance());
                    break;
                case RefDistance:
                    if (!src.isPositional()) {
                        return;
                    }

                    al.alSourcef(id, AL_REFERENCE_DISTANCE, src.getRefDistance());
                    break;
                case ReverbFilter:
                    if (!supportEfx || !src.isPositional() || !src.isReverbEnabled()) {
                        return;
                    }

                    int filter = EFX.AL_FILTER_NULL;
                    if (src.getReverbFilter() != null) {
                        Filter f = src.getReverbFilter();
                        if (f.isUpdateNeeded()) {
                            updateFilter(f);
                        }
                        filter = f.getId();
                    }
                    al.alSource3i(id, EFX.AL_AUXILIARY_SEND_FILTER, reverbFxSlot, 0, filter);
                    break;
                case ReverbEnabled:
                    if (!supportEfx || !src.isPositional()) {
                        return;
                    }

                    if (src.isReverbEnabled()) {
                        updateSourceParam(src, AudioParam.ReverbFilter);
                    } else {
                        al.alSource3i(id, EFX.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX.AL_FILTER_NULL);
                    }
                    break;
                case IsPositional:
                    if (!src.isPositional()) {
                        // Play in headspace
                        al.alSourcei(id, AL_SOURCE_RELATIVE, AL_TRUE);
                        al.alSource3f(id, AL_POSITION, 0, 0, 0);
                        al.alSource3f(id, AL_VELOCITY, 0, 0, 0);
                        
                        // Disable reverb
                        al.alSource3i(id, EFX.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX.AL_FILTER_NULL);
                    } else {
                        al.alSourcei(id, AL_SOURCE_RELATIVE, AL_FALSE);
                        updateSourceParam(src, AudioParam.Position);
                        updateSourceParam(src, AudioParam.Velocity);
                        updateSourceParam(src, AudioParam.MaxDistance);
                        updateSourceParam(src, AudioParam.RefDistance);
                        updateSourceParam(src, AudioParam.ReverbEnabled);
                    }
                    break;
                case Direction:
                    if (!src.isDirectional()) {
                        return;
                    }

                    Vector3f dir = src.getDirection();
                    al.alSource3f(id, AL_DIRECTION, dir.x, dir.y, dir.z);
                    break;
                case InnerAngle:
                    if (!src.isDirectional()) {
                        return;
                    }

                    al.alSourcef(id, AL_CONE_INNER_ANGLE, src.getInnerAngle());
                    break;
                case OuterAngle:
                    if (!src.isDirectional()) {
                        return;
                    }

                    al.alSourcef(id, AL_CONE_OUTER_ANGLE, src.getOuterAngle());
                    break;
                case IsDirectional:
                    if (src.isDirectional()) {
                        updateSourceParam(src, AudioParam.Direction);
                        updateSourceParam(src, AudioParam.InnerAngle);
                        updateSourceParam(src, AudioParam.OuterAngle);
                        al.alSourcef(id, AL_CONE_OUTER_GAIN, 0);
                    } else {
                        al.alSourcef(id, AL_CONE_INNER_ANGLE, 360);
                        al.alSourcef(id, AL_CONE_OUTER_ANGLE, 360);
                        al.alSourcef(id, AL_CONE_OUTER_GAIN, 1f);
                    }
                    break;
                case DryFilter:
                    if (!supportEfx) {
                        return;
                    }

                    if (src.getDryFilter() != null) {
                        Filter f = src.getDryFilter();
                        if (f.isUpdateNeeded()) {
                            updateFilter(f);

                            // NOTE: must re-attach filter for changes to apply.
                            al.alSourcei(id, EFX.AL_DIRECT_FILTER, f.getId());
                        }
                    } else {
                        al.alSourcei(id, EFX.AL_DIRECT_FILTER, EFX.AL_FILTER_NULL);
                    }
                    break;
                case Looping:
                    if (src.isLooping() && !(src.getAudioData() instanceof AudioStream)) {
                        al.alSourcei(id, AL_LOOPING, AL_TRUE);
                    } else {
                        al.alSourcei(id, AL_LOOPING, AL_FALSE);
                    }
                    break;
                case Volume:
                    al.alSourcef(id, AL_GAIN, src.getVolume());
                    break;
                case Pitch:
                    al.alSourcef(id, AL_PITCH, src.getPitch());
                    break;
            }
        }
    }

    private void setSourceParams(int id, AudioSource src, boolean forceNonLoop) {
        if (src.isPositional()) {
            Vector3f pos = src.getPosition();
            Vector3f vel = src.getVelocity();
            al.alSource3f(id, AL_POSITION, pos.x, pos.y, pos.z);
            al.alSource3f(id, AL_VELOCITY, vel.x, vel.y, vel.z);
            al.alSourcef(id, AL_MAX_DISTANCE, src.getMaxDistance());
            al.alSourcef(id, AL_REFERENCE_DISTANCE, src.getRefDistance());
            al.alSourcei(id, AL_SOURCE_RELATIVE, AL_FALSE);

            if (src.isReverbEnabled() && supportEfx) {
                int filter = EFX.AL_FILTER_NULL;
                if (src.getReverbFilter() != null) {
                    Filter f = src.getReverbFilter();
                    if (f.isUpdateNeeded()) {
                        updateFilter(f);
                    }
                    filter = f.getId();
                }
                al.alSource3i(id, EFX.AL_AUXILIARY_SEND_FILTER, reverbFxSlot, 0, filter);
            }
        } else {
            // play in headspace
            al.alSourcei(id, AL_SOURCE_RELATIVE, AL_TRUE);
            al.alSource3f(id, AL_POSITION, 0, 0, 0);
            al.alSource3f(id, AL_VELOCITY, 0, 0, 0);
        }

        if (src.getDryFilter() != null && supportEfx) {
            Filter f = src.getDryFilter();
            if (f.isUpdateNeeded()) {
                updateFilter(f);

                // NOTE: must re-attach filter for changes to apply.
                al.alSourcei(id, EFX.AL_DIRECT_FILTER, f.getId());
            }
        }

        if (forceNonLoop || src.getAudioData() instanceof AudioStream) {
            al.alSourcei(id, AL_LOOPING, AL_FALSE);
        } else {
            al.alSourcei(id, AL_LOOPING, src.isLooping() ? AL_TRUE : AL_FALSE);
        }
        al.alSourcef(id, AL_GAIN, src.getVolume());
        al.alSourcef(id, AL_PITCH, src.getPitch());
        al.alSourcef(id, AL_SEC_OFFSET, src.getTimeOffset());

        if (src.isDirectional()) {
            Vector3f dir = src.getDirection();
            al.alSource3f(id, AL_DIRECTION, dir.x, dir.y, dir.z);
            al.alSourcef(id, AL_CONE_INNER_ANGLE, src.getInnerAngle());
            al.alSourcef(id, AL_CONE_OUTER_ANGLE, src.getOuterAngle());
            al.alSourcef(id, AL_CONE_OUTER_GAIN, 0);
        } else {
            al.alSourcef(id, AL_CONE_INNER_ANGLE, 360);
            al.alSourcef(id, AL_CONE_OUTER_ANGLE, 360);
            al.alSourcef(id, AL_CONE_OUTER_GAIN, 1f);
        }
    }

    public void updateListenerParam(Listener listener, ListenerParam param) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            switch (param) {
                case Position:
                    Vector3f pos = listener.getLocation();
                    al.alListener3f(AL_POSITION, pos.x, pos.y, pos.z);
                    break;
                case Rotation:
                    Vector3f dir = listener.getDirection();
                    Vector3f up = listener.getUp();
                    fb.rewind();
                    fb.put(dir.x).put(dir.y).put(dir.z);
                    fb.put(up.x).put(up.y).put(up.z);
                    fb.flip();
                    al.alListener(AL_ORIENTATION, fb);
                    break;
                case Velocity:
                    Vector3f vel = listener.getVelocity();
                    al.alListener3f(AL_VELOCITY, vel.x, vel.y, vel.z);
                    break;
                case Volume:
                    al.alListenerf(AL_GAIN, listener.getVolume());
                    break;
            }
        }
    }

    private void setListenerParams(Listener listener) {
        Vector3f pos = listener.getLocation();
        Vector3f vel = listener.getVelocity();
        Vector3f dir = listener.getDirection();
        Vector3f up = listener.getUp();

        al.alListener3f(AL_POSITION, pos.x, pos.y, pos.z);
        al.alListener3f(AL_VELOCITY, vel.x, vel.y, vel.z);
        fb.rewind();
        fb.put(dir.x).put(dir.y).put(dir.z);
        fb.put(up.x).put(up.y).put(up.z);
        fb.flip();
        al.alListener(AL_ORIENTATION, fb);
        al.alListenerf(AL_GAIN, listener.getVolume());
    }

    private int newChannel() {
        if (freeChans.size() > 0) {
            return freeChans.remove(0);
        } else if (nextChan < channels.length) {
            return nextChan++;
        } else {
            return -1;
        }
    }

    private void freeChannel(int index) {
        if (index == nextChan - 1) {
            nextChan--;
        } else {
            freeChans.add(index);
        }
    }

    public void setEnvironment(Environment env) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled || !supportEfx) {
                return;
            }

            efx.alEffectf(reverbFx, EFX.AL_REVERB_DENSITY, env.getDensity());
            efx.alEffectf(reverbFx, EFX.AL_REVERB_DIFFUSION, env.getDiffusion());
            efx.alEffectf(reverbFx, EFX.AL_REVERB_GAIN, env.getGain());
            efx.alEffectf(reverbFx, EFX.AL_REVERB_GAINHF, env.getGainHf());
            efx.alEffectf(reverbFx, EFX.AL_REVERB_DECAY_TIME, env.getDecayTime());
            efx.alEffectf(reverbFx, EFX.AL_REVERB_DECAY_HFRATIO, env.getDecayHFRatio());
            efx.alEffectf(reverbFx, EFX.AL_REVERB_REFLECTIONS_GAIN, env.getReflectGain());
            efx.alEffectf(reverbFx, EFX.AL_REVERB_REFLECTIONS_DELAY, env.getReflectDelay());
            efx.alEffectf(reverbFx, EFX.AL_REVERB_LATE_REVERB_GAIN, env.getLateReverbGain());
            efx.alEffectf(reverbFx, EFX.AL_REVERB_LATE_REVERB_DELAY, env.getLateReverbDelay());
            efx.alEffectf(reverbFx, EFX.AL_REVERB_AIR_ABSORPTION_GAINHF, env.getAirAbsorbGainHf());
            efx.alEffectf(reverbFx, EFX.AL_REVERB_ROOM_ROLLOFF_FACTOR, env.getRoomRolloffFactor());

            // attach effect to slot
            efx.alAuxiliaryEffectSloti(reverbFxSlot, EFX.AL_EFFECTSLOT_EFFECT, reverbFx);
        }
    }

    private boolean fillBuffer(AudioStream stream, int id) {
        int size = 0;
        int result;

        while (size < arrayBuf.length) {
            result = stream.readSamples(arrayBuf, size, arrayBuf.length - size);

            if (result > 0) {
                size += result;
            } else {
                break;
            }
        }

        if (size == 0) {
            return false;
        }

        nativeBuf.clear();
        nativeBuf.put(arrayBuf, 0, size);
        nativeBuf.flip();

        al.alBufferData(id, convertFormat(stream), nativeBuf, size, stream.getSampleRate());

        return true;
    }

    private boolean fillStreamingSource(int sourceId, AudioStream stream, boolean looping) {
        boolean success = false;
        int processed = al.alGetSourcei(sourceId, AL_BUFFERS_PROCESSED);
        int unqueuedBufferBytes = 0;
        
        for (int i = 0; i < processed; i++) {
            int buffer;

            ib.position(0).limit(1);
            al.alSourceUnqueueBuffers(sourceId, 1, ib);
            buffer = ib.get(0);
            
            // XXX: assume that reading from AudioStream always 
            // gives BUFFER_SIZE amount of bytes! This might not always
            // be the case...
            unqueuedBufferBytes += BUFFER_SIZE;
            
            boolean active = fillBuffer(stream, buffer);
            
            if (!active && !stream.isEOF()) {
                throw new AssertionError();
            }
            
            if (!active && looping) {
                stream.setTime(0);
                active = fillBuffer(stream, buffer);
                if (!active) {
                    throw new IllegalStateException("Looping streaming source " +
                            "was rewinded but could not be filled");
                }
            }
            
            if (active) {
                ib.position(0).limit(1);
                ib.put(0, buffer);
                al.alSourceQueueBuffers(sourceId, 1, ib);
                // At least one buffer enqueued = success.
                success = true;
            } else {
                // No more data left to process.
                break;
            }
        }
        
        stream.setUnqueuedBufferBytes(stream.getUnqueuedBufferBytes() + unqueuedBufferBytes);

        return success;
    }

    private void attachStreamToSource(int sourceId, AudioStream stream, boolean looping) {
        boolean success = false;
        
        // Reset the stream. Typically happens if it finished playing on 
        // its own and got reclaimed. 
        // Note that AudioNode.stop() already resets the stream
        // since it might not be in EOF when stopped.
        if (stream.isEOF()) {
            stream.setTime(0);
        }
        
        for (int id : stream.getIds()) {
            boolean active = fillBuffer(stream, id);
            if (!active && !stream.isEOF()) {
                throw new AssertionError();
            }
            if (!active && looping) {
                stream.setTime(0);
                active = fillBuffer(stream, id);
                if (!active) {
                    throw new IllegalStateException("Looping streaming source " +
                            "was rewinded but could not be filled");
                }
            }
            if (active) {
                ib.position(0).limit(1);
                ib.put(id).flip();
                al.alSourceQueueBuffers(sourceId, 1, ib);
                success = true;
            }
        }
        
        if (!success) {
            // should never happen
            throw new IllegalStateException("No valid data could be read from stream");
        }
    }

    private boolean attachBufferToSource(int sourceId, AudioBuffer buffer) {
        al.alSourcei(sourceId, AL_BUFFER, buffer.getId());
        return true;
    }

    private void attachAudioToSource(int sourceId, AudioData data, boolean looping) {
        if (data instanceof AudioBuffer) {
            attachBufferToSource(sourceId, (AudioBuffer) data);
        } else if (data instanceof AudioStream) {
            attachStreamToSource(sourceId, (AudioStream) data, looping);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void clearChannel(int index) {
        // make room at this channel
        if (chanSrcs[index] != null) {
            AudioSource src = chanSrcs[index];

            int sourceId = channels[index];
            al.alSourceStop(sourceId);
            
            // For streaming sources, this will clear all queued buffers.
            al.alSourcei(sourceId, AL_BUFFER, 0);

            if (src.getDryFilter() != null && supportEfx) {
                // detach filter
                al.alSourcei(sourceId, EFX.AL_DIRECT_FILTER, EFX.AL_FILTER_NULL);
            }
            if (src.isPositional()) {
                AudioSource pas = (AudioSource) src;
                if (pas.isReverbEnabled() && supportEfx) {
                    al.alSource3i(sourceId, EFX.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX.AL_FILTER_NULL);
                }
            }

            chanSrcs[index] = null;
        }
    }
    
    private AudioSource.Status convertStatus(int oalStatus) {
        switch (oalStatus) {
            case AL_INITIAL:
            case AL_STOPPED:
                return Status.Stopped;
            case AL_PAUSED:
                return Status.Paused;
            case AL_PLAYING:
                return Status.Playing;
            default:
                throw new UnsupportedOperationException("Unrecognized OAL state: " + oalStatus);
        }
    }

    public void update(float tpf) {
        synchronized (threadLock) {
            updateInRenderThread(tpf);
        }
    }

    public void updateInRenderThread(float tpf) {
        if (audioDisabled) {
            return;
        }
        
        for (int i = 0; i < channels.length; i++) {
            AudioSource src = chanSrcs[i];
            
            if (src == null) {
                continue;
            }

            int sourceId = channels[i];
            boolean boundSource = i == src.getChannel();
            boolean reclaimChannel = false;
            
            Status oalStatus = convertStatus(al.alGetSourcei(sourceId, AL_SOURCE_STATE));
            
            if (!boundSource) {
                // Rules for instanced playback vary significantly. 
                // Handle it here.
                if (oalStatus == Status.Stopped) {
                    // Instanced audio stopped playing. Reclaim channel.
                    clearChannel(i);
                    freeChannel(i);
                } else if (oalStatus == Status.Paused) {
                    throw new AssertionError("Instanced audio cannot be paused");
                }
                
                continue;
            }
            
            Status jmeStatus = src.getStatus();
            
            // Check if we need to sync JME status with OAL status.
            if (oalStatus != jmeStatus) {
                if (oalStatus == Status.Stopped && jmeStatus == Status.Playing) {
                    // Maybe we need to reclaim the channel.
                    if (src.getAudioData() instanceof AudioStream) {
                        AudioStream stream = (AudioStream) src.getAudioData();
                        
                        if (stream.isEOF() && !src.isLooping()) {
                            // Stream finished playing
                            reclaimChannel = true;
                        } else {
                            // Stream still has data. 
                            // Buffer starvation occured.
                            // Audio decoder thread will fill the data 
                            // and start the channel again.
                        }
                    } else {
                        // Buffer finished playing.
                        if (src.isLooping()) {
                            // When a device is disconnected, all sources
                            // will enter the "stopped" state.
                            logger.warning("A looping sound has stopped playing");
                        }

                        reclaimChannel = true;
                    }
                    
                    if (reclaimChannel) {
                        src.setStatus(Status.Stopped);
                        src.setChannel(-1);
                        clearChannel(i);
                        freeChannel(i);
                    }
                } else {
                    // jME3 state does not match OAL state.
                    // This is only relevant for bound sources.
                    throw new AssertionError("Unexpected sound status. "
                                            + "OAL: " + oalStatus 
                                            + ", JME: " + jmeStatus);
                }
            } else {
                // Stopped channel was not cleared correctly.
                if (oalStatus == Status.Stopped) {
                    throw new AssertionError("Channel " + i + " was not reclaimed");
                }
            }
        }
    }
    
    public void updateInDecoderThread(float tpf) {
        if (audioDisabled) {
            return;
        }

        for (int i = 0; i < channels.length; i++) {
            AudioSource src = chanSrcs[i];
            
            if (src == null || !(src.getAudioData() instanceof AudioStream)) {
                continue;
            }

            int sourceId = channels[i];
            AudioStream stream = (AudioStream) src.getAudioData();

            Status oalStatus = convertStatus(al.alGetSourcei(sourceId, AL_SOURCE_STATE));
            Status jmeStatus = src.getStatus();

            // Keep filling data (even if we are stopped / paused)
            boolean buffersWereFilled = fillStreamingSource(sourceId, stream, src.isLooping());

            if (buffersWereFilled) {
                if (oalStatus == Status.Stopped && jmeStatus == Status.Playing) {
                    // The source got stopped due to buffer starvation.
                    // Start it again.
                    logger.log(Level.WARNING, "Buffer starvation "
                                            + "occurred while playing stream");
                    al.alSourcePlay(sourceId);
                } else {
                    // Buffers were filled, stream continues to play.
                    if (oalStatus == Status.Playing && jmeStatus == Status.Playing) {
                        // Nothing to do.
                    } else {
                        throw new AssertionError();
                    }
                }
            }
        }

        // Delete any unused objects.
        objManager.deleteUnused(this);
    }

    public void setListener(Listener listener) {
        checkDead();
        synchronized (threadLock) {
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
            setListenerParams(listener);
        }
    }
    
    public void pauseAll() {
        if (!supportPauseDevice) {
            throw new UnsupportedOperationException("Pause device is NOT supported!");
        }
        
        alc.alcDevicePauseSOFT();
    }

    public void resumeAll() {
        if (!supportPauseDevice) {
            throw new UnsupportedOperationException("Pause device is NOT supported!");
        }
        
        alc.alcDeviceResumeSOFT();
    }

    public void playSourceInstance(AudioSource src) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            if (src.getAudioData() instanceof AudioStream) {
                throw new UnsupportedOperationException(
                        "Cannot play instances "
                        + "of audio streams. Use play() instead.");
            }

            if (src.getAudioData().isUpdateNeeded()) {
                updateAudioData(src.getAudioData());
            }

            // create a new index for an audio-channel
            int index = newChannel();
            if (index == -1) {
                return;
            }

            int sourceId = channels[index];

            clearChannel(index);

            // set parameters, like position and max distance
            setSourceParams(sourceId, src, true);
            attachAudioToSource(sourceId, src.getAudioData(), false);
            chanSrcs[index] = src;

            // play the channel
            al.alSourcePlay(sourceId);
        }
    }

    public void playSource(AudioSource src) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            if (src.getStatus() == Status.Playing) {
                return;
            } else if (src.getStatus() == Status.Stopped) {
                //Assertion removed as it seems it's not possible to have 
                //something different than =1 when first playing an AudioNode
                // assert src.getChannel() != -1;
                
                // allocate channel to this source
                int index = newChannel();
                if (index == -1) {
                    logger.log(Level.WARNING, "No channel available to play {0}", src);
                    return;
                }
                clearChannel(index);
                src.setChannel(index);

                AudioData data = src.getAudioData();
                if (data.isUpdateNeeded()) {
                    updateAudioData(data);
                }

                chanSrcs[index] = src;
                setSourceParams(channels[index], src, false);
                attachAudioToSource(channels[index], data, src.isLooping());
            }

            al.alSourcePlay(channels[src.getChannel()]);
            src.setStatus(Status.Playing);
        }
    }

    public void pauseSource(AudioSource src) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            if (src.getStatus() == Status.Playing) {
                assert src.getChannel() != -1;

                al.alSourcePause(channels[src.getChannel()]);
                src.setStatus(Status.Paused);
            }
        }
    }

    public void stopSource(AudioSource src) {
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            if (src.getStatus() != Status.Stopped) {
                int chan = src.getChannel();
                assert chan != -1; // if it's not stopped, must have id

                src.setStatus(Status.Stopped);
                src.setChannel(-1);
                clearChannel(chan);
                freeChannel(chan);
                
                if (src.getAudioData() instanceof AudioStream) {
                    // If the stream is seekable, then rewind it.
                    // Otherwise, close it, as it is no longer valid.
                    AudioStream stream = (AudioStream)src.getAudioData();
                    if (stream.isSeekable()) {
                        stream.setTime(0);
                    } else {
                        stream.close();
                    }
                }
            }
        }
    }

    private int convertFormat(AudioData ad) {
        switch (ad.getBitsPerSample()) {
            case 8:
                if (ad.getChannels() == 1) {
                    return AL_FORMAT_MONO8;
                } else if (ad.getChannels() == 2) {
                    return AL_FORMAT_STEREO8;
                }

                break;
            case 16:
                if (ad.getChannels() == 1) {
                    return AL_FORMAT_MONO16;
                } else {
                    return AL_FORMAT_STEREO16;
                }
        }
        throw new UnsupportedOperationException("Unsupported channels/bits combination: "
                + "bits=" + ad.getBitsPerSample() + ", channels=" + ad.getChannels());
    }

    private void updateAudioBuffer(AudioBuffer ab) {
        int id = ab.getId();
        if (ab.getId() == -1) {
            ib.position(0).limit(1);
            al.alGenBuffers(1, ib);
            id = ib.get(0);
            ab.setId(id);

            objManager.registerObject(ab);
        }

        ab.getData().clear();
        al.alBufferData(id, convertFormat(ab), ab.getData(), ab.getData().capacity(), ab.getSampleRate());
        ab.clearUpdateNeeded();
    }

    private void updateAudioStream(AudioStream as) {
        if (as.getIds() != null) {
            deleteAudioData(as);
        }

        int[] ids = new int[STREAMING_BUFFER_COUNT];
        ib.position(0).limit(STREAMING_BUFFER_COUNT);
        al.alGenBuffers(STREAMING_BUFFER_COUNT, ib);
        ib.position(0).limit(STREAMING_BUFFER_COUNT);
        ib.get(ids);

        // Not registered with object manager.
        // AudioStreams can be handled without object manager
        // since their lifecycle is known to the audio renderer.

        as.setIds(ids);
        as.clearUpdateNeeded();
    }

    private void updateAudioData(AudioData ad) {
        if (ad instanceof AudioBuffer) {
            updateAudioBuffer((AudioBuffer) ad);
        } else if (ad instanceof AudioStream) {
            updateAudioStream((AudioStream) ad);
        }
    }

    public void deleteFilter(Filter filter) {
        int id = filter.getId();
        if (id != -1) {
            ib.position(0).limit(1);
            ib.put(id).flip();
            efx.alDeleteFilters(1, ib);
            filter.resetObject();
        }
    }

    public void deleteAudioData(AudioData ad) {
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            if (ad instanceof AudioBuffer) {
                AudioBuffer ab = (AudioBuffer) ad;
                int id = ab.getId();
                if (id != -1) {
                    ib.put(0, id);
                    ib.position(0).limit(1);
                    al.alDeleteBuffers(1, ib);
                    ab.resetObject();
                }
            } else if (ad instanceof AudioStream) {
                AudioStream as = (AudioStream) ad;
                int[] ids = as.getIds();
                if (ids != null) {
                    ib.clear();
                    ib.put(ids).flip();
                    al.alDeleteBuffers(ids.length, ib);
                    as.resetObject();
                }
            }
        }
    }
}
