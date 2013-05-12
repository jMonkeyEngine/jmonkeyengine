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
package com.jme3.audio.android;

import com.jme3.audio.*;
import com.jme3.audio.AudioSource.Status;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObjectManager;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AndroidOpenALSoftAudioRenderer implements AudioRenderer, Runnable {

    private static final Logger logger = Logger.getLogger(AndroidOpenALSoftAudioRenderer.class.getName());
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
    private ArrayList<Integer> freeChans = new ArrayList<Integer>();
    private Listener listener;
    private boolean audioDisabled = false;
    private boolean supportEfx = false;
    private int auxSends = 0;
    private int reverbFx = -1;
    private int reverbFxSlot = -1;
    // Update audio 20 times per second
    private static final float UPDATE_RATE = 0.05f;
    private final Thread audioThread = new Thread(this, "jME3 Audio Thread");
    private final AtomicBoolean threadLock = new AtomicBoolean(false);

    public AndroidOpenALSoftAudioRenderer() {
    }

    public void initialize() {
        if (!audioThread.isAlive()) {
            audioThread.setDaemon(true);
            audioThread.setPriority(Thread.NORM_PRIORITY + 1);
            audioThread.start();
        } else {
            throw new IllegalStateException("Initialize already called");
        }
    }

    private void checkDead() {
        if (audioThread.getState() == Thread.State.TERMINATED) {
            throw new IllegalStateException("Audio thread is terminated");
        }
    }

    public void run() {
        initInThread();
        synchronized (threadLock) {
            threadLock.set(true);
            threadLock.notifyAll();
        }

        long updateRateNanos = (long) (UPDATE_RATE * 1000000000);
        mainloop:
        while (true) {
            long startTime = System.nanoTime();

            if (Thread.interrupted()) {
                break;
            }

            synchronized (threadLock) {
                updateInThread(UPDATE_RATE);
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

        logger.log(Level.INFO, "Exiting audioThread run loop");
        synchronized (threadLock) {
            cleanupInThread();
        }
    }

    public void initInThread() {
        try {
            if (!alIsCreated()) {
                //AL.create();
                logger.log(Level.INFO, "Creating OpenAL Soft Renderer");
                alCreate();
                checkError(false);
            }
//        } catch (OpenALException ex) {
//            logger.log(Level.SEVERE, "Failed to load audio library", ex);
//            audioDisabled = true;
//            return;
//        } catch (LWJGLException ex) {
//            logger.log(Level.SEVERE, "Failed to load audio library", ex);
//            audioDisabled = true;
//            return;
        } catch (UnsatisfiedLinkError ex) {
            logger.log(Level.SEVERE, "Failed to load audio library", ex);
            audioDisabled = true;
            return;
        }

        //ALCdevice device = AL.getDevice(); /* device maintained in jni */
        //String deviceName = ALC10.alcGetString(device, ALC10.ALC_DEVICE_SPECIFIER);
        String deviceName = alcGetString(AL.ALC_DEVICE_SPECIFIER);

        logger.log(Level.INFO, "Audio Device: {0}", deviceName);
        //logger.log(Level.INFO, "Audio Vendor: {0}", alGetString(AL_VENDOR));
        //logger.log(Level.INFO, "Audio Renderer: {0}", alGetString(AL_RENDERER));
        //logger.log(Level.INFO, "Audio Version: {0}", alGetString(AL_VERSION));
        logger.log(Level.INFO, "Audio Vendor: {0}", alGetString(AL.AL_VENDOR));
        logger.log(Level.INFO, "Audio Renderer: {0}", alGetString(AL.AL_RENDERER));
        logger.log(Level.INFO, "Audio Version: {0}", alGetString(AL.AL_VERSION));

        // Find maximum # of sources supported by this implementation
        ArrayList<Integer> channelList = new ArrayList<Integer>();
        for (int i = 0; i < MAX_NUM_CHANNELS; i++) {
            int chan = alGenSources();
            //if (alGetError() != 0) {
            if (checkError(false) != 0) {
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

        logger.log(Level.INFO, "AudioRenderer supports {0} channels", channels.length);

        //supportEfx = alcIsExtensionPresent(device, "ALC_EXT_EFX");
        supportEfx = alcIsExtensionPresent(AL.ALC_EXT_EFX_NAME);
        logger.log(Level.INFO, "{0} found: {1}",
                new Object[]{AL.ALC_EXT_EFX_NAME, supportEfx});

        if (supportEfx) {
            ib.position(0).limit(1);
            //ALC10.alcGetInteger(device, EFX10.ALC_EFX_MAJOR_VERSION, ib);
            alcGetInteger(AL.ALC_EFX_MAJOR_VERSION, ib, 1);
            int major = ib.get(0);
            ib.position(0).limit(1);
            //ALC10.alcGetInteger(device, EFX10.ALC_EFX_MINOR_VERSION, ib);
            alcGetInteger(AL.ALC_EFX_MINOR_VERSION, ib, 1);
            int minor = ib.get(0);
            logger.log(Level.INFO, "Audio effect extension version: {0}.{1}", new Object[]{major, minor});

            //ALC10.alcGetInteger(device, EFX10.ALC_MAX_AUXILIARY_SENDS, ib);
            alcGetInteger(AL.ALC_MAX_AUXILIARY_SENDS, ib, 1);
            auxSends = ib.get(0);
            logger.log(Level.INFO, "Audio max auxilary sends: {0}", auxSends);

            // create slot
            ib.position(0).limit(1);
            //EFX10.alGenAuxiliaryEffectSlots(ib);
            alGenAuxiliaryEffectSlots(1, ib);
            reverbFxSlot = ib.get(0);

            // create effect
            ib.position(0).limit(1);
            //EFX10.alGenEffects(ib);
            alGenEffects(1, ib);
            reverbFx = ib.get(0);
            //EFX10.alEffecti(reverbFx, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_REVERB);
            alEffecti(reverbFx, AL.AL_EFFECT_TYPE, AL.AL_EFFECT_REVERB);

            // attach reverb effect to effect slot
            //EFX10.alAuxiliaryEffectSloti(reverbFxSlot, EFX10.AL_EFFECTSLOT_EFFECT, reverbFx);
            alAuxiliaryEffectSloti(reverbFxSlot, AL.AL_EFFECTSLOT_EFFECT, reverbFx);
        } else {
            logger.log(Level.WARNING, "OpenAL EFX not available! Audio effects won't work.");
        }
    }

    public void cleanupInThread() {
        logger.log(Level.INFO, "cleanupInThread");
        if (audioDisabled) {
            //AL.destroy();
            logger.log(Level.INFO, "Destroying OpenAL Soft Renderer with audioDisabled");
            alDestroy();
            checkError(true);
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
        //alDeleteSources(ib);
        alDeleteSources(channels.length, ib);
        checkError(true);

        // delete audio buffers and filters
        objManager.deleteAllObjects(this);

        if (supportEfx) {
            ib.position(0).limit(1);
            ib.put(0, reverbFx);
            //EFX10.alDeleteEffects(ib);
            alDeleteEffects(1, ib);

            // If this is not allocated, why is it deleted?
            // Commented out to fix native crash in OpenAL.
            ib.position(0).limit(1);
            ib.put(0, reverbFxSlot);
            //EFX10.alDeleteAuxiliaryEffectSlots(ib);
            alDeleteAuxiliaryEffectSlots(1, ib);
        }

        //AL.destroy();
        logger.log(Level.INFO, "Destroying OpenAL Soft Renderer");
        alDestroy();
    }

    public void cleanup() {
        logger.log(Level.INFO, "cleanup");
        // kill audio thread
        if (audioThread.isAlive()) {
            logger.log(Level.INFO, "Interrupting audioThread");
            audioThread.interrupt();
        }
    }

    private void updateFilter(Filter f) {
        int id = f.getId();
        if (id == -1) {
            ib.position(0).limit(1);
            //EFX10.alGenFilters(ib);
            alGenFilters(1, ib);
            id = ib.get(0);
            f.setId(id);

            objManager.registerForCleanup(f);
        }

        if (f instanceof LowPassFilter) {
            LowPassFilter lpf = (LowPassFilter) f;
            //EFX10.alFilteri(id, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS);
            alFilteri(id, AL.AL_FILTER_TYPE, AL.AL_FILTER_LOWPASS);
            //EFX10.alFilterf(id, EFX10.AL_LOWPASS_GAIN, lpf.getVolume());
            alFilterf(id, AL.AL_LOWPASS_GAIN, lpf.getVolume());
            //EFX10.alFilterf(id, EFX10.AL_LOWPASS_GAINHF, lpf.getHighFreqVolume());
            alFilterf(id, AL.AL_LOWPASS_GAINHF, lpf.getHighFreqVolume());
        } else {
            throw new UnsupportedOperationException("Filter type unsupported: "
                    + f.getClass().getName());
        }

        f.clearUpdateNeeded();
    }

    public void updateSourceParam(AudioSource src, AudioParam param) {
        checkDead();
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
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
                    //alSource3f(id, AL_POSITION, pos.x, pos.y, pos.z);
                    alSource3f(id, AL.AL_POSITION, pos.x, pos.y, pos.z);
                    checkError(true);
                    break;
                case Velocity:
                    if (!src.isPositional()) {
                        return;
                    }

                    Vector3f vel = src.getVelocity();
                    //alSource3f(id, AL_VELOCITY, vel.x, vel.y, vel.z);
                    alSource3f(id, AL.AL_VELOCITY, vel.x, vel.y, vel.z);
                    checkError(true);
                    break;
                case MaxDistance:
                    if (!src.isPositional()) {
                        return;
                    }

                    //alSourcef(id, AL_MAX_DISTANCE, src.getMaxDistance());
                    alSourcef(id, AL.AL_MAX_DISTANCE, src.getMaxDistance());
                    checkError(true);
                    break;
                case RefDistance:
                    if (!src.isPositional()) {
                        return;
                    }

                    //alSourcef(id, AL_REFERENCE_DISTANCE, src.getRefDistance());
                    alSourcef(id, AL.AL_REFERENCE_DISTANCE, src.getRefDistance());
                    checkError(true);
                    break;
                case ReverbFilter:
                    if (!supportEfx || !src.isPositional() || !src.isReverbEnabled()) {
                        return;
                    }

                    int filter = AL.AL_FILTER_NULL;
                    if (src.getReverbFilter() != null) {
                        Filter f = src.getReverbFilter();
                        if (f.isUpdateNeeded()) {
                            updateFilter(f);
                        }
                        filter = f.getId();
                    }
                    //AL11.alSource3i(id, EFX10.AL_AUXILIARY_SEND_FILTER, reverbFxSlot, 0, filter);
                    alSource3i(id, AL.AL_AUXILIARY_SEND_FILTER, reverbFxSlot, 0, filter);
                    break;
                case ReverbEnabled:
                    if (!supportEfx || !src.isPositional()) {
                        return;
                    }

                    if (src.isReverbEnabled()) {
                        updateSourceParam(src, AudioParam.ReverbFilter);
                    } else {
                        //AL11.alSource3i(id, EFX10.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX10.AL_FILTER_NULL);
                        alSource3i(id, AL.AL_AUXILIARY_SEND_FILTER, 0, 0, AL.AL_FILTER_NULL);
                    }
                    break;
                case IsPositional:
                    if (!src.isPositional()) {
                        // Play in headspace
                        //alSourcei(id, AL_SOURCE_RELATIVE, AL_TRUE);
                        alSourcei(id, AL.AL_SOURCE_RELATIVE, AL.AL_TRUE);
                        checkError(true);
                        //alSource3f(id, AL_POSITION, 0, 0, 0);
                        alSource3f(id, AL.AL_POSITION, 0, 0, 0);
                        checkError(true);
                        //alSource3f(id, AL_VELOCITY, 0, 0, 0);
                        alSource3f(id, AL.AL_VELOCITY, 0, 0, 0);
                        checkError(true);

                        // Disable reverb
                        //AL11.alSource3i(id, EFX10.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX10.AL_FILTER_NULL);
                        alSource3i(id, AL.AL_AUXILIARY_SEND_FILTER, 0, 0, AL.AL_FILTER_NULL);
                    } else {
                        //alSourcei(id, AL_SOURCE_RELATIVE, AL_FALSE);
                        alSourcei(id, AL.AL_SOURCE_RELATIVE, AL.AL_FALSE);
                        checkError(true);
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
                    //alSource3f(id, AL_DIRECTION, dir.x, dir.y, dir.z);
                    alSource3f(id, AL.AL_DIRECTION, dir.x, dir.y, dir.z);
                    checkError(true);
                    break;
                case InnerAngle:
                    if (!src.isDirectional()) {
                        return;
                    }

                    //alSourcef(id, AL_CONE_INNER_ANGLE, src.getInnerAngle());
                    alSourcef(id, AL.AL_CONE_INNER_ANGLE, src.getInnerAngle());
                    checkError(true);
                    break;
                case OuterAngle:
                    if (!src.isDirectional()) {
                        return;
                    }

                    //alSourcef(id, AL_CONE_OUTER_ANGLE, src.getOuterAngle());
                    alSourcef(id, AL.AL_CONE_OUTER_ANGLE, src.getOuterAngle());
                    checkError(true);
                    break;
                case IsDirectional:
                    if (src.isDirectional()) {
                        updateSourceParam(src, AudioParam.Direction);
                        updateSourceParam(src, AudioParam.InnerAngle);
                        updateSourceParam(src, AudioParam.OuterAngle);
                        //alSourcef(id, AL_CONE_OUTER_GAIN, 0);
                        alSourcef(id, AL.AL_CONE_OUTER_GAIN, 0);
                        checkError(true);
                    } else {
                        //alSourcef(id, AL_CONE_INNER_ANGLE, 360);
                        alSourcef(id, AL.AL_CONE_INNER_ANGLE, 360);
                        checkError(true);
                        //alSourcef(id, AL_CONE_OUTER_ANGLE, 360);
                        alSourcef(id, AL.AL_CONE_OUTER_ANGLE, 360);
                        checkError(true);
                        //alSourcef(id, AL_CONE_OUTER_GAIN, 1f);
                        alSourcef(id, AL.AL_CONE_OUTER_GAIN, 1f);
                        checkError(true);
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
                            //alSourcei(id, EFX10.AL_DIRECT_FILTER, f.getId());
                            alSourcei(id, AL.AL_DIRECT_FILTER, f.getId());
                        }
                    } else {
                        //alSourcei(id, EFX10.AL_DIRECT_FILTER, EFX10.AL_FILTER_NULL);
                        alSourcei(id, AL.AL_DIRECT_FILTER, AL.AL_FILTER_NULL);
                    }
                    break;
                case Looping:
                    if (src.isLooping()) {
                        if (!(src.getAudioData() instanceof AudioStream)) {
                            //alSourcei(id, AL_LOOPING, AL_TRUE);
                            alSourcei(id, AL.AL_LOOPING, AL.AL_TRUE);
                            checkError(true);
                        }
                    } else {
                        //alSourcei(id, AL_LOOPING, AL_FALSE);
                        alSourcei(id, AL.AL_LOOPING, AL.AL_FALSE);
                        checkError(true);
                    }
                    break;
                case Volume:
                    //alSourcef(id, AL_GAIN, src.getVolume());
                    alSourcef(id, AL.AL_GAIN, src.getVolume());
                    checkError(true);
                    break;
                case Pitch:
                    //alSourcef(id, AL_PITCH, src.getPitch());
                    alSourcef(id, AL.AL_PITCH, src.getPitch());
                    checkError(true);
                    break;
            }
        }
    }

    private void setSourceParams(int id, AudioSource src, boolean forceNonLoop) {
        if (src.isPositional()) {
            Vector3f pos = src.getPosition();
            Vector3f vel = src.getVelocity();
            //alSource3f(id, AL_POSITION, pos.x, pos.y, pos.z);
            alSource3f(id, AL.AL_POSITION, pos.x, pos.y, pos.z);
            checkError(true);
            //alSource3f(id, AL_VELOCITY, vel.x, vel.y, vel.z);
            alSource3f(id, AL.AL_VELOCITY, vel.x, vel.y, vel.z);
            checkError(true);
            //alSourcef(id, AL_MAX_DISTANCE, src.getMaxDistance());
            alSourcef(id, AL.AL_MAX_DISTANCE, src.getMaxDistance());
            checkError(true);
            //alSourcef(id, AL_REFERENCE_DISTANCE, src.getRefDistance());
            alSourcef(id, AL.AL_REFERENCE_DISTANCE, src.getRefDistance());
            checkError(true);
            //alSourcei(id, AL_SOURCE_RELATIVE, AL_FALSE);
            alSourcei(id, AL.AL_SOURCE_RELATIVE, AL.AL_FALSE);
            checkError(true);

            if (src.isReverbEnabled() && supportEfx) {
                //int filter = EFX10.AL_FILTER_NULL;
                int filter = AL.AL_FILTER_NULL;
                if (src.getReverbFilter() != null) {
                    Filter f = src.getReverbFilter();
                    if (f.isUpdateNeeded()) {
                        updateFilter(f);
                    }
                    filter = f.getId();
                }
                //AL11.alSource3i(id, EFX10.AL_AUXILIARY_SEND_FILTER, reverbFxSlot, 0, filter);
                alSource3i(id, AL.AL_AUXILIARY_SEND_FILTER, reverbFxSlot, 0, filter);
            }
        } else {
            // play in headspace
            //alSourcei(id, AL_SOURCE_RELATIVE, AL_TRUE);
            alSourcei(id, AL.AL_SOURCE_RELATIVE, AL.AL_TRUE);
            checkError(true);
            //alSource3f(id, AL_POSITION, 0, 0, 0);
            alSource3f(id, AL.AL_POSITION, 0, 0, 0);
            checkError(true);
            //alSource3f(id, AL_VELOCITY, 0, 0, 0);
            alSource3f(id, AL.AL_VELOCITY, 0, 0, 0);
            checkError(true);
        }

        if (src.getDryFilter() != null && supportEfx) {
            Filter f = src.getDryFilter();
            if (f.isUpdateNeeded()) {
                updateFilter(f);

                // NOTE: must re-attach filter for changes to apply.
                //alSourcei(id, EFX10.AL_DIRECT_FILTER, f.getId());
                alSourcei(id, AL.AL_DIRECT_FILTER, f.getId());
            }
        }

        if (forceNonLoop) {
            //alSourcei(id, AL_LOOPING, AL_FALSE);
            alSourcei(id, AL.AL_LOOPING, AL.AL_FALSE);
            checkError(true);
        } else {
            //alSourcei(id, AL_LOOPING, src.isLooping() ? AL_TRUE : AL_FALSE);
            alSourcei(id, AL.AL_LOOPING, src.isLooping() ? AL.AL_TRUE : AL.AL_FALSE);
            checkError(true);
        }
        //alSourcef(id, AL_GAIN, src.getVolume());
        alSourcef(id, AL.AL_GAIN, src.getVolume());
        checkError(true);
        //alSourcef(id, AL_PITCH, src.getPitch());
        alSourcef(id, AL.AL_PITCH, src.getPitch());
        checkError(true);
        //alSourcef(id, AL11.AL_SEC_OFFSET, src.getTimeOffset());
        alSourcef(id, AL.AL_SEC_OFFSET, src.getTimeOffset());
        checkError(true);

        if (src.isDirectional()) {
            Vector3f dir = src.getDirection();
            //alSource3f(id, AL_DIRECTION, dir.x, dir.y, dir.z);
            alSource3f(id, AL.AL_DIRECTION, dir.x, dir.y, dir.z);
            checkError(true);
            //alSourcef(id, AL_CONE_INNER_ANGLE, src.getInnerAngle());
            alSourcef(id, AL.AL_CONE_INNER_ANGLE, src.getInnerAngle());
            checkError(true);
            //alSourcef(id, AL_CONE_OUTER_ANGLE, src.getOuterAngle());
            alSourcef(id, AL.AL_CONE_OUTER_ANGLE, src.getOuterAngle());
            checkError(true);
            //alSourcef(id, AL_CONE_OUTER_GAIN, 0);
            alSourcef(id, AL.AL_CONE_OUTER_GAIN, 0);
            checkError(true);
        } else {
            //alSourcef(id, AL_CONE_INNER_ANGLE, 360);
            alSourcef(id, AL.AL_CONE_INNER_ANGLE, 360);
            checkError(true);
            //alSourcef(id, AL_CONE_OUTER_ANGLE, 360);
            alSourcef(id, AL.AL_CONE_OUTER_ANGLE, 360);
            checkError(true);
            //alSourcef(id, AL_CONE_OUTER_GAIN, 1f);
            alSourcef(id, AL.AL_CONE_OUTER_GAIN, 1f);
            checkError(true);
        }
    }

    public void updateListenerParam(Listener listener, ListenerParam param) {
        checkDead();
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled) {
                return;
            }

            switch (param) {
                case Position:
                    Vector3f pos = listener.getLocation();
                    //alListener3f(AL_POSITION, pos.x, pos.y, pos.z);
                    alListener3f(AL.AL_POSITION, pos.x, pos.y, pos.z);
                    checkError(true);
                    break;
                case Rotation:
                    Vector3f dir = listener.getDirection();
                    Vector3f up = listener.getUp();
                    fb.rewind();
                    fb.put(dir.x).put(dir.y).put(dir.z);
                    fb.put(up.x).put(up.y).put(up.z);
                    fb.flip();
                    //alListener(AL_ORIENTATION, fb);
                    alListener(AL.AL_ORIENTATION, fb);
                    checkError(true);
                    break;
                case Velocity:
                    Vector3f vel = listener.getVelocity();
                    //alListener3f(AL_VELOCITY, vel.x, vel.y, vel.z);
                    alListener3f(AL.AL_VELOCITY, vel.x, vel.y, vel.z);
                    checkError(true);
                    break;
                case Volume:
                    //alListenerf(AL_GAIN, listener.getVolume());
                    alListenerf(AL.AL_GAIN, listener.getVolume());
                    checkError(true);
                    break;
            }
        }
    }

    private void setListenerParams(Listener listener) {
        Vector3f pos = listener.getLocation();
        Vector3f vel = listener.getVelocity();
        Vector3f dir = listener.getDirection();
        Vector3f up = listener.getUp();

        //alListener3f(AL_POSITION, pos.x, pos.y, pos.z);
        alListener3f(AL.AL_POSITION, pos.x, pos.y, pos.z);
        checkError(true);
        //alListener3f(AL_VELOCITY, vel.x, vel.y, vel.z);
        alListener3f(AL.AL_VELOCITY, vel.x, vel.y, vel.z);
        checkError(true);
        fb.rewind();
        fb.put(dir.x).put(dir.y).put(dir.z);
        fb.put(up.x).put(up.y).put(up.z);
        fb.flip();
        //alListener(AL_ORIENTATION, fb);
        alListener(AL.AL_ORIENTATION, fb);
        checkError(true);
        //alListenerf(AL_GAIN, listener.getVolume());
        alListenerf(AL.AL_GAIN, listener.getVolume());
        checkError(true);
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
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled || !supportEfx) {
                return;
            }

            //EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_DENSITY, env.getDensity());
            alEffectf(reverbFx, AL.AL_REVERB_DENSITY, env.getDensity());
            //EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_DIFFUSION, env.getDiffusion());
            alEffectf(reverbFx, AL.AL_REVERB_DIFFUSION, env.getDiffusion());
            //EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_GAIN, env.getGain());
            alEffectf(reverbFx, AL.AL_REVERB_GAIN, env.getGain());
            //EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_GAINHF, env.getGainHf());
            alEffectf(reverbFx, AL.AL_REVERB_GAINHF, env.getGainHf());
            //EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_DECAY_TIME, env.getDecayTime());
            alEffectf(reverbFx, AL.AL_REVERB_DECAY_TIME, env.getDecayTime());
            //EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_DECAY_HFRATIO, env.getDecayHFRatio());
            alEffectf(reverbFx, AL.AL_REVERB_DECAY_HFRATIO, env.getDecayHFRatio());
            //EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_REFLECTIONS_GAIN, env.getReflectGain());
            alEffectf(reverbFx, AL.AL_REVERB_REFLECTIONS_GAIN, env.getReflectGain());
            //EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_REFLECTIONS_DELAY, env.getReflectDelay());
            alEffectf(reverbFx, AL.AL_REVERB_REFLECTIONS_DELAY, env.getReflectDelay());
            //EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_LATE_REVERB_GAIN, env.getLateReverbGain());
            alEffectf(reverbFx, AL.AL_REVERB_LATE_REVERB_GAIN, env.getLateReverbGain());
            //EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_LATE_REVERB_DELAY, env.getLateReverbDelay());
            alEffectf(reverbFx, AL.AL_REVERB_LATE_REVERB_DELAY, env.getLateReverbDelay());
            //EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_AIR_ABSORPTION_GAINHF, env.getAirAbsorbGainHf());
            alEffectf(reverbFx, AL.AL_REVERB_AIR_ABSORPTION_GAINHF, env.getAirAbsorbGainHf());
            //EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_ROOM_ROLLOFF_FACTOR, env.getRoomRolloffFactor());
            alEffectf(reverbFx, AL.AL_REVERB_ROOM_ROLLOFF_FACTOR, env.getRoomRolloffFactor());

            // attach effect to slot
            //EFX10.alAuxiliaryEffectSloti(reverbFxSlot, EFX10.AL_EFFECTSLOT_EFFECT, reverbFx);
            alAuxiliaryEffectSloti(reverbFxSlot, AL.AL_EFFECTSLOT_EFFECT, reverbFx);
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

        //alBufferData(id, convertFormat(stream), nativeBuf, stream.getSampleRate());
        alBufferData(id, convertFormat(stream), nativeBuf, size, stream.getSampleRate());
        checkError(true);

        return true;
    }

    private boolean fillStreamingSource(int sourceId, AudioStream stream) {
        if (!stream.isOpen()) {
            return false;
        }

        boolean active = true;
        //int processed = alGetSourcei(sourceId, AL_BUFFERS_PROCESSED);
        int processed = alGetSourcei(sourceId, AL.AL_BUFFERS_PROCESSED);
        checkError(true);

        //while((processed--) != 0){
        if (processed > 0) {
            int buffer;

            ib.position(0).limit(1);
            //alSourceUnqueueBuffers(sourceId, ib);
            alSourceUnqueueBuffers(sourceId, 1, ib);
            checkError(true);
            buffer = ib.get(0);

            active = fillBuffer(stream, buffer);

            ib.position(0).limit(1);
            ib.put(0, buffer);
            //alSourceQueueBuffers(sourceId, ib);
            alSourceQueueBuffers(sourceId, 1, ib);
            checkError(true);
        }

        if (!active && stream.isOpen()) {
            stream.close();
        }

        return active;
    }

    private boolean attachStreamToSource(int sourceId, AudioStream stream) {
        boolean active = true;
        int activeBufferCount = 0;
        for (int id : stream.getIds()) {
            active = fillBuffer(stream, id);
            ib.position(0).limit(1);
            ib.put(id).flip();
            //alSourceQueueBuffers(sourceId, ib);
            // OpenAL Soft does not like 0 size buffer data in alSourceQueueBuffers
            //  Produces error code 40964 (0xA004) = AL_INVALID_OPERATION and
            //  does not return (crashes) so that the error code can be checked.
            // active is FALSE when the data size is 0
            if (active) {
                alSourceQueueBuffers(sourceId, 1, ib);
                checkError(true);
                activeBufferCount++;
            }
        }
        // adjust the steam id array if the audio data is smaller than STREAMING_BUFFER_COUNT
        // this is to avoid an error with OpenAL Soft when alSourceUnenqueueBuffers
        //   is called with more buffers than were originally used with alSourceQueueBuffers
        if (activeBufferCount < STREAMING_BUFFER_COUNT) {
            int[] newIds = new int[activeBufferCount];
            for (int i=0; i<STREAMING_BUFFER_COUNT; i++) {
                if (i < activeBufferCount) {
                    newIds[i] = stream.getIds()[i];
                } else {
                    ib.clear();
                    ib.put(stream.getIds()[i]).limit(1).flip();
                    alDeleteBuffers(1, ib);
                    checkError(true);
                }

            }
            stream.setIds(newIds);
        }

        return active;
    }

    private boolean attachBufferToSource(int sourceId, AudioBuffer buffer) {
        //alSourcei(sourceId, AL_BUFFER, buffer.getId());
        alSourcei(sourceId, AL.AL_BUFFER, buffer.getId());
        checkError(true);
        return true;
    }

    private boolean attachAudioToSource(int sourceId, AudioData data) {
        if (data instanceof AudioBuffer) {
            return attachBufferToSource(sourceId, (AudioBuffer) data);
        } else if (data instanceof AudioStream) {
            return attachStreamToSource(sourceId, (AudioStream) data);
        }
        throw new UnsupportedOperationException();
    }

    private void clearChannel(int index) {
        // make room at this channel
        if (chanSrcs[index] != null) {
            AudioSource src = chanSrcs[index];

            int sourceId = channels[index];
            alSourceStop(sourceId);

            if (src.getAudioData() instanceof AudioStream) {
                AudioStream str = (AudioStream) src.getAudioData();
                for (int i=0; i<str.getIds().length; i++) {
                }
                //ib.position(0).limit(STREAMING_BUFFER_COUNT);
                ib.position(0).limit(str.getIds().length);
                ib.put(str.getIds()).flip();
                int processed = alGetSourcei(sourceId, AL.AL_BUFFERS_PROCESSED);
                //alSourceUnqueueBuffers(sourceId, ib);
                alSourceUnqueueBuffers(sourceId, processed, ib);
                checkError(true);
            } else if (src.getAudioData() instanceof AudioBuffer) {
                //alSourcei(sourceId, AL_BUFFER, 0);
                alSourcei(sourceId, AL.AL_BUFFER, 0);
                checkError(true);
            }

            if (src.getDryFilter() != null && supportEfx) {
                // detach filter
                //alSourcei(sourceId, EFX10.AL_DIRECT_FILTER, EFX10.AL_FILTER_NULL);
                alSourcei(sourceId, AL.AL_DIRECT_FILTER, AL.AL_FILTER_NULL);
            }
            if (src.isPositional()) {
                AudioSource pas = (AudioSource) src;
                if (pas.isReverbEnabled() && supportEfx) {
                    //AL11.alSource3i(sourceId, EFX10.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX10.AL_FILTER_NULL);
                    alSource3i(sourceId, AL.AL_AUXILIARY_SEND_FILTER, 0, 0, AL.AL_FILTER_NULL);
                }
            }

            chanSrcs[index] = null;
        }
    }

    public void update(float tpf) {
        // does nothing
    }

    public void updateInThread(float tpf) {
        if (audioDisabled) {
            return;
        }

        for (int i = 0; i < channels.length; i++) {
            AudioSource src = chanSrcs[i];
            if (src == null) {
                continue;
            }

            int sourceId = channels[i];

            // is the source bound to this channel
            // if false, it's an instanced playback
            boolean boundSource = i == src.getChannel();

            // source's data is streaming
            boolean streaming = src.getAudioData() instanceof AudioStream;

            // only buffered sources can be bound
            assert (boundSource && streaming) || (!streaming);

            //int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
            int state = alGetSourcei(sourceId, AL.AL_SOURCE_STATE);
            checkError(true);
            boolean wantPlaying = src.getStatus() == Status.Playing;
            //boolean stopped = state == AL_STOPPED;
            boolean stopped = state == AL.AL_STOPPED;

            if (streaming && wantPlaying) {
                AudioStream stream = (AudioStream) src.getAudioData();
                if (stream.isOpen()) {
                    fillStreamingSource(sourceId, stream);
                    if (stopped) {
                        alSourcePlay(sourceId);
                        checkError(true);
                    }
                } else {
                    if (stopped) {
                        // became inactive
                        src.setStatus(Status.Stopped);
                        src.setChannel(-1);
                        clearChannel(i);
                        freeChannel(i);

                        // And free the audio since it cannot be
                        // played again anyway.
                        deleteAudioData(stream);
                    }
                }
            } else if (!streaming) {
                //boolean paused = state == AL_PAUSED;
                boolean paused = state == AL.AL_PAUSED;

                // make sure OAL pause state & source state coincide
                assert (src.getStatus() == Status.Paused && paused) || (!paused);

                if (stopped) {
                    if (boundSource) {
                        src.setStatus(Status.Stopped);
                        src.setChannel(-1);
                    }
                    clearChannel(i);
                    freeChannel(i);
                }
            }
        }

        // Delete any unused objects.
        objManager.deleteUnused(this);
    }

    public void setListener(Listener listener) {
        checkDead();
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
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

    public void playSourceInstance(AudioSource src) {
        checkDead();
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled) {
                return;
            }

            if (src.getAudioData() instanceof AudioStream) {
                throw new UnsupportedOperationException(
                        "Cannot play instances "
                        + "of audio streams. Use playSource() instead.");
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
            attachAudioToSource(sourceId, src.getAudioData());
            chanSrcs[index] = src;

            // play the channel
            alSourcePlay(sourceId);
            checkError(true);
        }
    }

    public void playSource(AudioSource src) {
        checkDead();
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled) {
                return;
            }

            //assert src.getStatus() == Status.Stopped || src.getChannel() == -1;

            if (src.getStatus() == Status.Playing) {
                return;
            } else if (src.getStatus() == Status.Stopped) {

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
                attachAudioToSource(channels[index], data);
            }

            alSourcePlay(channels[src.getChannel()]);
            checkError(true);
            src.setStatus(Status.Playing);
        }
    }

    public void pauseSource(AudioSource src) {
        checkDead();
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled) {
                return;
            }

            if (src.getStatus() == Status.Playing) {
                assert src.getChannel() != -1;

                alSourcePause(channels[src.getChannel()]);
                checkError(true);
                src.setStatus(Status.Paused);
            }
        }
    }

    public void stopSource(AudioSource src) {
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
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
                    AudioStream stream = (AudioStream) src.getAudioData();
                    if (stream.isOpen()) {
                        stream.close();
                    }

                    // And free the audio since it cannot be
                    // played again anyway.
                    deleteAudioData(src.getAudioData());
                }
            }
        }
    }

    private int convertFormat(AudioData ad) {
        switch (ad.getBitsPerSample()) {
            case 8:
                if (ad.getChannels() == 1) {
                    //return AL_FORMAT_MONO8;
                    return AL.AL_FORMAT_MONO8;
                } else if (ad.getChannels() == 2) {
                    //return AL_FORMAT_STEREO8;
                    return AL.AL_FORMAT_STEREO8;
                }

                break;
            case 16:
                if (ad.getChannels() == 1) {
                    //return AL_FORMAT_MONO16;
                    return AL.AL_FORMAT_MONO16;
                } else {
                    //return AL_FORMAT_STEREO16;
                    return AL.AL_FORMAT_STEREO16;
                }
        }
        throw new UnsupportedOperationException("Unsupported channels/bits combination: "
                + "bits=" + ad.getBitsPerSample() + ", channels=" + ad.getChannels());
    }

    private void updateAudioBuffer(AudioBuffer ab) {
        int id = ab.getId();
        if (ab.getId() == -1) {
            ib.position(0).limit(1);
            alGenBuffers(1, ib);
            checkError(true);
            id = ib.get(0);
            ab.setId(id);

            objManager.registerForCleanup(ab);
        }

        ab.getData().clear();
        //alBufferData(id, convertFormat(ab), ab.getData(), ab.getSampleRate());
        alBufferData(id, convertFormat(ab), ab.getData(), ab.getData().limit(), ab.getSampleRate());
        checkError(true);
        ab.clearUpdateNeeded();
    }

    private void updateAudioStream(AudioStream as) {
        if (as.getIds() != null) {
            deleteAudioData(as);
        }

        int[] ids = new int[STREAMING_BUFFER_COUNT];
        ib.position(0).limit(STREAMING_BUFFER_COUNT);
        //alGenBuffers(ib);
        alGenBuffers(STREAMING_BUFFER_COUNT, ib);
        checkError(true);
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
            //EFX10.alDeleteFilters(id);
            ib.put(0, id);
            ib.position(0).limit(1);
            alDeleteFilters(1, ib);
        }
    }

    public void deleteAudioData(AudioData ad) {
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled) {
                return;
            }

            if (ad instanceof AudioBuffer) {
                AudioBuffer ab = (AudioBuffer) ad;
                int id = ab.getId();
                if (id != -1) {
                    ib.put(0, id);
                    ib.position(0).limit(1);
                    //alDeleteBuffers(ib);
                    alDeleteBuffers(1, ib);
                    checkError(true);
                    ab.resetObject();
                }
            } else if (ad instanceof AudioStream) {
                AudioStream as = (AudioStream) ad;
                int[] ids = as.getIds();
                if (ids != null) {
                    ib.clear();
                    ib.put(ids).flip();
                    //alDeleteBuffers(ib);
                    alDeleteBuffers(ids.length, ib);
                    checkError(true);
                    as.resetObject();
                }
            }
        }
    }

    private int checkError(boolean stopOnError) {
        int errorCode = alGetError();
        String errorText = AL.GetALErrorMsg(errorCode);

        if (errorCode != AL.AL_NO_ERROR && stopOnError) {
            throw new IllegalStateException("AL Error Detected.  Error Code: " + errorCode + ": " + errorText);
        }

        return errorCode;
    }

    /** Native methods, implemented in jni folder */
    public static native boolean alIsCreated();
    public static native boolean alCreate();
    public static native boolean alDestroy();
    public static native String alcGetString(int parameter);
    public static native String alGetString(int parameter);
    public static native int alGenSources();
    public static native int alGetError();
    public static native void alDeleteSources(int numSources, IntBuffer sources);
    public static native void alGenBuffers(int numBuffers, IntBuffer buffers);
    public static native void alDeleteBuffers(int numBuffers, IntBuffer buffers);
    public static native void alSourceStop(int source);
    public static native void alSourcei(int source, int param, int value);
    public static native void alBufferData(int buffer, int format, ByteBuffer data, int size, int frequency);
    public static native void alSourcePlay(int source);
    public static native void alSourcePause(int source);
    public static native void alSourcef(int source, int param, float value);
    public static native void alSource3f(int source, int param, float value1, float value2, float value3);
    public static native int alGetSourcei(int source, int param);
    public static native void alSourceUnqueueBuffers(int source, int numBuffers, IntBuffer buffers);
    public static native void alSourceQueueBuffers(int source, int numBuffers, IntBuffer buffers);
    public static native void alListener(int param, FloatBuffer data);
    public static native void alListenerf(int param, float value);
    public static native void alListener3f(int param, float value1, float value2, float value3);
    public static native boolean alcIsExtensionPresent(String extension);
    public static native void alcGetInteger(int param, IntBuffer buffer, int size);
    public static native void alGenAuxiliaryEffectSlots(int numSlots, IntBuffer buffers);
    public static native void alGenEffects(int numEffects, IntBuffer buffers);
    public static native void alEffecti(int effect, int param, int value);
    public static native void alAuxiliaryEffectSloti(int effectSlot, int param, int value);
    public static native void alDeleteEffects(int numEffects, IntBuffer buffers);
    public static native void alDeleteAuxiliaryEffectSlots(int numEffectSlots, IntBuffer buffers);
    public static native void alGenFilters(int numFilters, IntBuffer buffers);
    public static native void alFilteri(int filter, int param, int value);
    public static native void alFilterf(int filter, int param, float value);
    public static native void alSource3i(int source, int param, int value1, int value2, int value3);
    public static native void alDeleteFilters(int numFilters, IntBuffer buffers);
    public static native void alEffectf(int effect, int param, float value);

    /** Load jni .so on initialization */
    static {
         System.loadLibrary("openalsoftjme");
    }

}
