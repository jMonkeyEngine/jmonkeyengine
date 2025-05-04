/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
import static com.jme3.audio.openal.AL.*;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObjectManager;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ALAudioRenderer is the backend implementation for OpenAL audio rendering.
 */
public class ALAudioRenderer implements AudioRenderer, Runnable {

    private static final Logger logger = Logger.getLogger(ALAudioRenderer.class.getName());

    private static final String THREAD_NAME = "jME3 Audio Decoder";

    private final NativeObjectManager objManager = new NativeObjectManager();
    // When multiplied by STREAMING_BUFFER_COUNT, will equal 44100 * 2 * 2
    // which is exactly 1 second of audio.
    private static final int BUFFER_SIZE = 35280;
    private static final int STREAMING_BUFFER_COUNT = 5;
    private static final int MAX_NUM_CHANNELS = 64;

    // Buffers for OpenAL calls
    private IntBuffer ib = BufferUtils.createIntBuffer(1); // Reused for single int operations
    private final FloatBuffer fb = BufferUtils.createVector3Buffer(2); // For listener orientation
    private final ByteBuffer nativeBuf = BufferUtils.createByteBuffer(BUFFER_SIZE); // For streaming data
    private final byte[] arrayBuf = new byte[BUFFER_SIZE]; // Intermediate array buffer for streaming

    // Channel management
    private int[] channels; // OpenAL source IDs
    private AudioSource[] channelSources; // jME source associated with each channel
    private int nextChannelIndex = 0; // Next available channel index
    private final ArrayList<Integer> freeChannels = new ArrayList<>(); // Pool of freed channels

    // Listener and environment
    private Listener listener;
    private Environment environment;
    private int reverbFx = -1; // EFX reverb effect ID
    private int reverbFxSlot = -1; // EFX effect slot ID

    // State and capabilities
    private boolean audioDisabled = false;
    private boolean supportEfx = false;
    private boolean supportPauseDevice = false;
    private boolean supportDisconnect = false;
    private int auxSends = 0;

    // Update thread
    private static final float UPDATE_RATE = 0.05f; // Update streaming sources every 50ms
    private final Thread decoderThread = new Thread(this, THREAD_NAME);
    private final Object threadLock = new Object(); // Lock for thread safety

    // OpenAL API interfaces
    private final AL al;
    private final ALC alc;
    private final EFX efx;

    /**
     * Creates a new ALAudioRenderer instance.
     *
     * @param al  The OpenAL interface.
     * @param alc The OpenAL Context interface.
     * @param efx The OpenAL Effects Extension interface.
     */
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
            logger.log(Level.SEVERE, "Failed to load audio library (OpenAL). Audio will be disabled.", ex);
            audioDisabled = true;
            return;
        }

        enumerateAvailableChannels();

        printAudioRendererInfo();

        // Pause device is a feature used specifically on Android
        // where the application could be closed but still running,
        // thus the audio context remains open but no audio should be playing.
        supportPauseDevice = alc.alcIsExtensionPresent("ALC_SOFT_pause_device");
        if (!supportPauseDevice) {
            logger.log(Level.WARNING, "Pausing audio device not supported.");
        }

        // Disconnected audio devices (such as USB sound cards, headphones...)
        // never reconnect, the whole context must be re-created
        supportDisconnect = alc.alcIsExtensionPresent("ALC_EXT_disconnect");

        initEfx();
    }

    private void printAudioRendererInfo() {
        final String deviceName = alc.alcGetString(ALC.ALC_DEVICE_SPECIFIER);

        logger.log(Level.INFO, "Audio Renderer Information\n"
                        + " * Device: {0}\n"
                        + " * Vendor: {1}\n"
                        + " * Renderer: {2}\n"
                        + " * Version: {3}\n"
                        + " * Supported channels: {4}\n"
                        + " * ALC extensions: {5}\n"
                        + " * AL extensions: {6}",
                new Object[] {
                        deviceName,
                        al.alGetString(AL_VENDOR),
                        al.alGetString(AL_RENDERER),
                        al.alGetString(AL_VERSION),
                        channels.length,
                        alc.alcGetString(ALC.ALC_EXTENSIONS),
                        al.alGetString(AL_EXTENSIONS)
                });
    }

    /**
     * Generates OpenAL sources to determine the maximum number supported.
     */
    private void enumerateAvailableChannels() {
        // Find maximum # of sources supported by this implementation
        ArrayList<Integer> channelList = new ArrayList<>();
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
        channelSources = new AudioSource[channels.length];
    }

    /**
     * Initializes the EFX extension if supported.
     */
    private void initEfx() {
        supportEfx = alc.alcIsExtensionPresent("ALC_EXT_EFX");
        if (supportEfx) {
            ib.clear().limit(1);
            alc.alcGetInteger(EFX.ALC_EFX_MAJOR_VERSION, ib, 1);
            int major = ib.get(0);

            ib.clear().limit(1);
            alc.alcGetInteger(EFX.ALC_EFX_MINOR_VERSION, ib, 1);
            int minor = ib.get(0);
            logger.log(Level.INFO, "Audio effect extension version: {0}.{1}", new Object[]{major, minor});

            ib.clear().limit(1);
            alc.alcGetInteger(EFX.ALC_MAX_AUXILIARY_SENDS, ib, 1);
            auxSends = ib.get(0);
            logger.log(Level.INFO, "Audio max auxiliary sends: {0}", auxSends);

            // Create reverb effect slot
            ib.clear().limit(1);
            efx.alGenAuxiliaryEffectSlots(1, ib);
            reverbFxSlot = ib.get(0);

            // Create reverb effect
            ib.clear().limit(1);
            efx.alGenEffects(1, ib);
            reverbFx = ib.get(0);

            // Configure effect type
            efx.alEffecti(reverbFx, EFX.AL_EFFECT_TYPE, EFX.AL_EFFECT_REVERB);
            checkAlError("setting reverb effect type");

            // attach reverb effect to effect slot
            efx.alAuxiliaryEffectSloti(reverbFxSlot, EFX.AL_EFFECTSLOT_EFFECT, reverbFx);
            checkAlError("attaching reverb effect to slot");

        } else {
            logger.log(Level.WARNING, "OpenAL EFX not available! Audio effects won't work.");
        }
    }

    /**
     * Destroys the OpenAL context, deleting sources, buffers, filters, and effects.
     */
    private void destroyOpenAL() {
        if (audioDisabled) {
            alc.destroyALC();
            return;
        }

        // stop any playing channels
        for (int i = 0; i < channelSources.length; i++) {
            if (channelSources[i] != null) {
                clearChannel(i);
            }
        }

        // delete channel-based sources
        ib.clear();
        ib.put(channels);
        ib.flip();
        al.alDeleteSources(channels.length, ib);
        checkAlError("deleting sources");

        // Delete audio buffers and filters managed by NativeObjectManager
        objManager.deleteAllObjects(this);

        // Delete EFX objects if they were created
        if (supportEfx) {
            if (reverbFx != -1) {
                ib.position(0).limit(1);
                ib.put(0, reverbFx);
                efx.alDeleteEffects(1, ib);
                checkAlError("deleting reverbFx effect " + reverbFx);
                reverbFx = -1;
            }

            if (reverbFxSlot != -1) {
                ib.position(0).limit(1);
                ib.put(0, reverbFxSlot);
                efx.alDeleteAuxiliaryEffectSlots(1, ib);
                checkAlError("deleting effect reverbFxSlot " + reverbFxSlot);
                reverbFxSlot = -1;
            }
        }

        alc.destroyALC();
    }

    @Override
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

    /**
     * Checks if the audio thread has terminated unexpectedly.
     * @throws IllegalStateException if the decoding thread is terminated.
     */
    private void checkDead() {
        if (decoderThread.getState() == Thread.State.TERMINATED) {
            throw new IllegalStateException("Decoding thread is terminated");
        }
    }

    /**
     * Main loop for the audio decoder thread. Updates streaming sources.
     */
    @Override
    public void run() {
        long updateRateNanos = (long) (UPDATE_RATE * 1_000_000_000);
        mainloop:
        while (true) {
            long startTime = System.nanoTime();

            if (Thread.interrupted()) {
                logger.info("Audio decoder thread interrupted, exiting.");
                break;
            }

            synchronized (threadLock) {
                checkDevice();
                updateInDecoderThread(UPDATE_RATE);
            }

            long endTime = System.nanoTime();
            long diffTime = endTime - startTime;

            // Sleep to maintain the desired update rate
            if (diffTime < updateRateNanos) {
                long desiredEndTime = startTime + updateRateNanos;
                while (System.nanoTime() < desiredEndTime) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        logger.info("Audio decoder thread interrupted during sleep, exiting.");
                        break mainloop;
                    }
                }
            }
        }
        logger.info("Audio decoder thread finished.");
    }

    /**
     * Shuts down the audio decoder thread and destroys the OpenAL context.
     */
    @Override
    public void cleanup() {
        // kill audio thread
        if (decoderThread.isAlive()) {
            decoderThread.interrupt();
            try {
                decoderThread.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt(); // Re-interrupt thread
                logger.log(Level.WARNING, "Interrupted while waiting for audio thread to finish.", ex);
            }
        }

        // Destroy OpenAL context (only if initialized and not disabled)
        if (!audioDisabled && alc.isCreated()) {
            destroyOpenAL();
        }
    }

    /**
     * Updates an OpenAL filter object based on the jME Filter properties.
     * Generates the AL filter ID if necessary.
     * @param f The Filter object.
     */
    private void updateFilter(Filter f) {
        int id = f.getId();
        if (id == -1) {
            // Generate OpenAL filter ID
            ib.clear().limit(1);
            efx.alGenFilters(1, ib);
            id = ib.get(0);
            f.setId(id);

            objManager.registerObject(f);
        }

        if (f instanceof LowPassFilter) {
            LowPassFilter lowPass = (LowPassFilter) f;
            efx.alFilteri(id, EFX.AL_FILTER_TYPE, EFX.AL_FILTER_LOWPASS);
            efx.alFilterf(id, EFX.AL_LOWPASS_GAIN, lowPass.getVolume());
            efx.alFilterf(id, EFX.AL_LOWPASS_GAINHF, lowPass.getHighFreqVolume());
        }
        // ** Add other filter types (HighPass, BandPass) here if implemented **
        else {
            logger.log(Level.WARNING, "Unsupported filter type: {0}", f.getClass().getName());
        }

        if (checkAlError("updating filter " + id)) {
            deleteFilter(f); // Try to clean up
            f.resetObject();
        } else {
            f.clearUpdateNeeded(); // Mark as updated in AL
        }
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

            int sourceId = channels[src.getChannel()];
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
                playbackOffsetBytes = stream.getUnqueuedBufferBytes();
            }

            // Add byte offset from source (for both streams and buffers)
            playbackOffsetBytes += al.alGetSourcei(sourceId, AL_BYTE_OFFSET);

            // Compute time value from bytes
            // E.g. for 44100 source with 2 channels and 16 bits per sample:
            //    (44100 * 2 * 16 / 8) = 176400
            int bytesPerSecond = (data.getSampleRate()
                    * data.getChannels()
                    * data.getBitsPerSample() / 8);

            return (float) playbackOffsetBytes / bytesPerSecond;
        }
    }

    @Override
    public void updateSourceParam(AudioSource src, AudioParam param) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            // There is a race condition in AudioSource that can
            // cause this to be called for a node that has been
            // detached from its channel.  For example, setVolume()
            // called from the render thread may see that the AudioSource
            // still has a channel value but the audio thread may
            // clear that channel before setVolume() gets to call
            // updateSourceParam() (because the audio stopped playing
            // on its own right as the volume was set).  In this case,
            // it should be safe to just ignore the update.
            if (src.getChannel() < 0) {
                return;
            }

            assert src.getChannel() >= 0;

            int sourceId = channels[src.getChannel()];
            int filterId = EFX.AL_FILTER_NULL;

            switch (param) {
                case Position:
                    if (!src.isPositional()) {
                        return;
                    }
                    Vector3f pos = src.getPosition();
                    al.alSource3f(sourceId, AL_POSITION, pos.x, pos.y, pos.z);
                    break;

                case Velocity:
                    if (!src.isPositional()) {
                        return;
                    }
                    Vector3f vel = src.getVelocity();
                    al.alSource3f(sourceId, AL_VELOCITY, vel.x, vel.y, vel.z);
                    break;

                case MaxDistance:
                    if (!src.isPositional()) {
                        return;
                    }
                    al.alSourcef(sourceId, AL_MAX_DISTANCE, src.getMaxDistance());
                    break;

                case RefDistance:
                    if (!src.isPositional()) {
                        return;
                    }
                    al.alSourcef(sourceId, AL_REFERENCE_DISTANCE, src.getRefDistance());
                    break;

                case ReverbFilter:
                    if (!supportEfx || !src.isPositional() || !src.isReverbEnabled()) {
                        return;
                    }
                    Filter reverbFilter = src.getReverbFilter();
                    if (reverbFilter != null) {
                        if (reverbFilter.isUpdateNeeded()) {
                            updateFilter(reverbFilter);
                        }
                        filterId = reverbFilter.getId();
                    }
                    al.alSource3i(sourceId, EFX.AL_AUXILIARY_SEND_FILTER, reverbFxSlot, 0, filterId);
                    break;

                case ReverbEnabled:
                    if (!supportEfx || !src.isPositional()) {
                        return;
                    }
                    if (src.isReverbEnabled()) {
                        updateSourceParam(src, AudioParam.ReverbFilter);
                    } else {
                        al.alSource3i(sourceId, EFX.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX.AL_FILTER_NULL);
                    }
                    break;

                case IsPositional:
                    if (!src.isPositional()) {
                        // Play in headspace
                        al.alSourcei(sourceId, AL_SOURCE_RELATIVE, AL_TRUE);
                        al.alSource3f(sourceId, AL_POSITION, 0, 0, 0);
                        al.alSource3f(sourceId, AL_VELOCITY, 0, 0, 0);

                        // Disable reverb
                        al.alSource3i(sourceId, EFX.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX.AL_FILTER_NULL);
                    } else {
                        al.alSourcei(sourceId, AL_SOURCE_RELATIVE, AL_FALSE);
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
                    al.alSource3f(sourceId, AL_DIRECTION, dir.x, dir.y, dir.z);
                    break;

                case InnerAngle:
                    if (!src.isDirectional()) {
                        return;
                    }
                    al.alSourcef(sourceId, AL_CONE_INNER_ANGLE, src.getInnerAngle());
                    break;

                case OuterAngle:
                    if (!src.isDirectional()) {
                        return;
                    }
                    al.alSourcef(sourceId, AL_CONE_OUTER_ANGLE, src.getOuterAngle());
                    break;

                case IsDirectional:
                    if (src.isDirectional()) {
                        updateSourceParam(src, AudioParam.Direction);
                        updateSourceParam(src, AudioParam.InnerAngle);
                        updateSourceParam(src, AudioParam.OuterAngle);
                        al.alSourcef(sourceId, AL_CONE_OUTER_GAIN, 0);
                    } else {
                        al.alSourcef(sourceId, AL_CONE_INNER_ANGLE, 360);
                        al.alSourcef(sourceId, AL_CONE_OUTER_ANGLE, 360);
                        al.alSourcef(sourceId, AL_CONE_OUTER_GAIN, 1f);
                    }
                    break;

                case DryFilter:
                    if (!supportEfx) {
                        return;
                    }
                    Filter dryFilter = src.getDryFilter();
                    if (dryFilter != null) {
                        if (dryFilter.isUpdateNeeded()) {
                            updateFilter(dryFilter);
                        }
                        filterId = dryFilter.getId();
                    }
                    // NOTE: must re-attach filter for changes to apply.
                    al.alSourcei(sourceId, EFX.AL_DIRECT_FILTER, filterId);
                    break;

                case Looping:
                    if (src.getAudioData() instanceof AudioStream) {
                        al.alSourcei(sourceId, AL_LOOPING, AL_FALSE);
                    } else {
                        // AudioData instanceof AudioBuffer
                        al.alSourcei(sourceId, AL_LOOPING, src.isLooping() ? AL_TRUE : AL_FALSE);
                    }
                    break;

                case Volume:
                    al.alSourcef(sourceId, AL_GAIN, src.getVolume());
                    break;

                case Pitch:
                    al.alSourcef(sourceId, AL_PITCH, src.getPitch());
                    break;
            }
        }
    }

    private void setSourceParams(int sourceId, AudioSource src, boolean forceNonLoop) {
        if (src.isPositional()) {
            Vector3f pos = src.getPosition();
            Vector3f vel = src.getVelocity();
            al.alSource3f(sourceId, AL_POSITION, pos.x, pos.y, pos.z);
            al.alSource3f(sourceId, AL_VELOCITY, vel.x, vel.y, vel.z);
            al.alSourcef(sourceId, AL_MAX_DISTANCE, src.getMaxDistance());
            al.alSourcef(sourceId, AL_REFERENCE_DISTANCE, src.getRefDistance());
            al.alSourcei(sourceId, AL_SOURCE_RELATIVE, AL_FALSE);

            if (src.isReverbEnabled() && supportEfx) {
                int filterId = EFX.AL_FILTER_NULL;
                if (src.getReverbFilter() != null) {
                    Filter f = src.getReverbFilter();
                    if (f.isUpdateNeeded()) {
                        updateFilter(f);
                    }
                    filterId = f.getId();
                }
                al.alSource3i(sourceId, EFX.AL_AUXILIARY_SEND_FILTER, reverbFxSlot, 0, filterId);
            }
        } else {
            // play in headspace
            al.alSourcei(sourceId, AL_SOURCE_RELATIVE, AL_TRUE);
            al.alSource3f(sourceId, AL_POSITION, 0, 0, 0);
            al.alSource3f(sourceId, AL_VELOCITY, 0, 0, 0);
        }

        if (src.getDryFilter() != null && supportEfx) {
            Filter f = src.getDryFilter();
            if (f.isUpdateNeeded()) {
                updateFilter(f);
                // NOTE: must re-attach filter for changes to apply.
                al.alSourcei(sourceId, EFX.AL_DIRECT_FILTER, f.getId());
            }
        }

        if (forceNonLoop || src.getAudioData() instanceof AudioStream) {
            al.alSourcei(sourceId, AL_LOOPING, AL_FALSE);
        } else {
            al.alSourcei(sourceId, AL_LOOPING, src.isLooping() ? AL_TRUE : AL_FALSE);
        }
        al.alSourcef(sourceId, AL_GAIN, src.getVolume());
        al.alSourcef(sourceId, AL_PITCH, src.getPitch());
        al.alSourcef(sourceId, AL_SEC_OFFSET, src.getTimeOffset());

        if (src.isDirectional()) {
            Vector3f dir = src.getDirection();
            al.alSource3f(sourceId, AL_DIRECTION, dir.x, dir.y, dir.z);
            al.alSourcef(sourceId, AL_CONE_INNER_ANGLE, src.getInnerAngle());
            al.alSourcef(sourceId, AL_CONE_OUTER_ANGLE, src.getOuterAngle());
            al.alSourcef(sourceId, AL_CONE_OUTER_GAIN, 0);
        } else {
            al.alSourcef(sourceId, AL_CONE_INNER_ANGLE, 360);
            al.alSourcef(sourceId, AL_CONE_OUTER_ANGLE, 360);
            al.alSourcef(sourceId, AL_CONE_OUTER_GAIN, 1f);
        }
    }

    /**
     * Updates a specific parameter for the listener.
     *
     * @param listener The listener object.
     * @param param    The parameter to update.
     */
    @Override
    public void updateListenerParam(Listener listener, ListenerParam param) {
        checkDead();
        // Check if this listener is the active one
        if (this.listener != listener) {
            logger.warning("updateListenerParam called on inactive listener.");
            return;
        }

        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            switch (param) {
                case Position:
                    applyListenerPosition(listener);
                    break;
                case Rotation:
                    applyListenerRotation(listener);
                    break;
                case Velocity:
                    applyListenerVelocity(listener);
                    break;
                case Volume:
                    applyListenerVolume(listener);
                    break;
                default:
                    logger.log(Level.WARNING, "Unhandled listener parameter: {0}", param);
                    break;
            }
        }
    }

    /**
     * Applies all parameters from the listener object to OpenAL.
     * @param listener The listener object.
     */
    private void setListenerParams(Listener listener) {
        applyListenerPosition(listener);
        applyListenerRotation(listener);
        applyListenerVelocity(listener);
        applyListenerVolume(listener);
    }

    // --- Listener Parameter Helper Methods ---

    private void applyListenerPosition(Listener listener) {
        Vector3f pos = listener.getLocation();
        al.alListener3f(AL_POSITION, pos.x, pos.y, pos.z);
        checkAlError("setting listener position");
    }

    private void applyListenerRotation(Listener listener) {
        Vector3f dir = listener.getDirection();
        Vector3f up = listener.getUp();
        // Use the shared FloatBuffer fb
        fb.rewind();
        fb.put(dir.x).put(dir.y).put(dir.z);
        fb.put(up.x).put(up.y).put(up.z);
        fb.flip();
        al.alListener(AL_ORIENTATION, fb);
        checkAlError("setting listener orientation");
    }

    private void applyListenerVelocity(Listener listener) {
        Vector3f vel = listener.getVelocity();
        al.alListener3f(AL_VELOCITY, vel.x, vel.y, vel.z);
        checkAlError("setting listener velocity");
    }

    private void applyListenerVolume(Listener listener) {
        al.alListenerf(AL_GAIN, listener.getVolume());
        checkAlError("setting listener volume");
    }

    private int newChannel() {
        if (!freeChannels.isEmpty()) {
            return freeChannels.remove(0);
        } else if (nextChannelIndex < channels.length) {
            return nextChannelIndex++;
        } else {
            return -1;
        }
    }

    private void freeChannel(int index) {
        if (index == nextChannelIndex - 1) {
            nextChannelIndex--;
        } else {
            freeChannels.add(index);
        }
    }

    /**
     * Configures the global reverb effect based on the Environment settings.
     * @param env The Environment object.
     */
    @Override
    public void setEnvironment(Environment env) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled || !supportEfx) {
                return;
            }

            // Apply reverb properties from the Environment object
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

            if (checkAlError("setting reverb effect parameters")) {
                return;
            }

            // (Re)attach the configured reverb effect to the slot
            efx.alAuxiliaryEffectSloti(reverbFxSlot, EFX.AL_EFFECTSLOT_EFFECT, reverbFx);
            checkAlError("attaching reverb effect to slot");

            this.environment = env;
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

        al.alBufferData(id, getOpenALFormat(stream), nativeBuf, size, stream.getSampleRate());

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
                    throw new IllegalStateException("Looping streaming source "
                            + "was rewound but could not be filled");
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

        // Reset the stream. Typically, happens if it finished playing on its own and got reclaimed.
        // Note that AudioNode.stop() already resets the stream since it might not be at the EOF when stopped.
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
                    throw new IllegalStateException("Looping streaming source "
                            + "was rewound but could not be filled");
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

    private void attachBufferToSource(int sourceId, AudioBuffer buffer) {
        al.alSourcei(sourceId, AL_BUFFER, buffer.getId());
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
        if (channelSources[index] != null) {
            AudioSource src = channelSources[index];

            int sourceId = channels[index];
            al.alSourceStop(sourceId);
            checkAlError("stopping source " + sourceId + " on clearChannel");

            // For streaming sources, this will clear all queued buffers.
            al.alSourcei(sourceId, AL_BUFFER, 0);
            checkAlError("detaching buffer from source " + sourceId);

            if (supportEfx) {
                if (src.getDryFilter() != null) {
                    // detach direct filter
                    al.alSourcei(sourceId, EFX.AL_DIRECT_FILTER, EFX.AL_FILTER_NULL);
                    checkAlError("detaching direct filter from source " + sourceId);
                }

                if (src.isPositional() && src.isReverbEnabled()) {
                    // Detach auxiliary send filter (reverb)
                    al.alSource3i(sourceId, EFX.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX.AL_FILTER_NULL);
                    checkAlError("detaching aux filter from source " + sourceId);
                }
            }

            channelSources[index] = null;
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

    @Override
    public void update(float tpf) {
        synchronized (threadLock) {
            updateInRenderThread(tpf);
        }
    }

    private void checkDevice() {
        // If the device is disconnected, pick a new one
        if (isDisconnected()) {
            logger.log(Level.INFO, "Current audio device disconnected.");
            restartAudioRenderer();
        }
    }

    private boolean isDisconnected() {
        if (!supportDisconnect) {
            return false;
        }

        alc.alcGetInteger(ALC.ALC_CONNECTED, ib, 1);
        return ib.get(0) == 0;
    }

    private void restartAudioRenderer() {
        // Preserve internal state variables
        Listener currentListener = this.listener;
        Environment currentEnvironment = this.environment;

        // Destroy existing OpenAL resources
        destroyOpenAL();

        // Re-initialize OpenAL
        // Creates new context, enumerates channels, checks caps, inits EFX
        initOpenAL();

        // Restore Listener and Environment (if possible and successful init)
        if (!audioDisabled) {
            if (currentListener != null) {
                setListener(currentListener); // Re-apply listener params
            }
            if (currentEnvironment != null) {
                setEnvironment(currentEnvironment); // Re-apply environment
            }
            // TODO: What about existing AudioSource objects?
            // Their state (Playing/Paused/Stopped) is lost.
            // Their AudioData (buffers/streams) needs re-uploading/re-preparing.
            // This requires iterating through all known AudioNodes, which the renderer doesn't track.
            // The application layer would need to handle re-playing sounds after a device reset.
            logger.warning("Audio renderer restarted. Application may need to re-play active sounds.");

        } else {
            logger.severe("Audio remained disabled after attempting restart.");
        }
    }

    public void updateInRenderThread(float tpf) {
        if (audioDisabled) {
            return;
        }

        for (int i = 0; i < channels.length; i++) {
            AudioSource src = channelSources[i];

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
                            // Buffer starvation occurred.
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
                            + "OAL: " + oalStatus + ", JME: " + jmeStatus);
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
            AudioSource src = channelSources[i];

            if (src == null || !(src.getAudioData() instanceof AudioStream)) {
                continue;
            }

            int sourceId = channels[i];
            AudioStream stream = (AudioStream) src.getAudioData();

            Status oalStatus = convertStatus(al.alGetSourcei(sourceId, AL_SOURCE_STATE));
            Status jmeStatus = src.getStatus();

            // Keep filling data (even if we are stopped / paused)
            boolean buffersWereFilled = fillStreamingSource(sourceId, stream, src.isLooping());

            if (buffersWereFilled
                    && oalStatus == Status.Stopped
                    && jmeStatus == Status.Playing) {
                // The source got stopped due to buffer starvation.
                // Start it again.
                logger.log(Level.WARNING, "Buffer starvation occurred while playing stream");
                al.alSourcePlay(sourceId);
            }
        }

        // Delete any unused objects.
        objManager.deleteUnused(this);
    }

    @Override
    public void setListener(Listener listener) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            if (this.listener != null) {
                // previous listener no longer associated with current renderer
                this.listener.setRenderer(null);
            }

            this.listener = listener;

            if (this.listener != null) {
                this.listener.setRenderer(this);
                setListenerParams(listener);
            } else {
                logger.info("Listener set to null.");
            }
        }
    }

    @Override
    public void pauseAll() {
        if (!supportPauseDevice) {
            throw new UnsupportedOperationException("Pause device is NOT supported!");
        }

        alc.alcDevicePauseSOFT();
    }

    @Override
    public void resumeAll() {
        if (!supportPauseDevice) {
            throw new UnsupportedOperationException("Pause device is NOT supported!");
        }

        alc.alcDeviceResumeSOFT();
    }

    @Override
    public void playSourceInstance(AudioSource src) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            AudioData audioData = src.getAudioData();
            if (audioData instanceof AudioStream) {
                throw new UnsupportedOperationException(
                        "Cannot play instances of audio streams. Use play() instead.");
            }

            if (audioData.isUpdateNeeded()) {
                updateAudioData(audioData);
            }

            // create a new index for an audio-channel
            int index = newChannel();
            if (index == -1) {
                return;
            }

            int sourceId = channels[index];
            clearChannel(index);

            // set parameters, like position and max distance
            setSourceParams(sourceId, src, true); // forceNonLoop
            attachAudioToSource(sourceId, audioData, false); // no looping
            channelSources[index] = src;

            // play the channel
            al.alSourcePlay(sourceId);
        }
    }

    @Override
    public void playSource(AudioSource src) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            if (src.getStatus() == Status.Playing) {
                return;
            } else if (src.getStatus() == Status.Stopped) {
                // Assertion removed because it seems it's not possible to have
                // something different from -1 when first playing an AudioNode.
                // assert src.getChannel() != -1;

                // allocate channel to this source
                int index = newChannel();
                if (index == -1) {
                    logger.log(Level.WARNING, "No channel available to play {0}", src);
                    return;
                }
                clearChannel(index);
                src.setChannel(index);

                AudioData audioData = src.getAudioData();
                if (audioData.isUpdateNeeded()) {
                    updateAudioData(audioData);
                }

                channelSources[index] = src;
                setSourceParams(channels[index], src, false);
                attachAudioToSource(channels[index], audioData, src.isLooping());
            }

            al.alSourcePlay(channels[src.getChannel()]);
            src.setStatus(Status.Playing);
        }
    }

    @Override
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

    @Override
    public void stopSource(AudioSource src) {
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            if (src.getStatus() != Status.Stopped) {
                int channel = src.getChannel();
                assert channel != -1; // if it's not stopped, must have id

                src.setStatus(Status.Stopped);
                src.setChannel(-1);
                clearChannel(channel);
                freeChannel(channel);

                if (src.getAudioData() instanceof AudioStream) {
                    // If the stream is seekable, then rewind it.
                    // Otherwise, close it, as it is no longer valid.
                    AudioStream stream = (AudioStream) src.getAudioData();
                    if (stream.isSeekable()) {
                        stream.setTime(0);
                    } else {
                        stream.close();
                    }
                }
            }
        }
    }

    /**
     * Gets the corresponding OpenAL format enum for the audio data properties.
     * @param audioData The AudioData.
     * @return The OpenAL format enum (e.g., AL_FORMAT_STEREO16).
     * @throws UnsupportedOperationException if the format is not supported.
     */
    private int getOpenALFormat(AudioData audioData) {

        int channels = audioData.getChannels();
        int bitsPerSample = audioData.getBitsPerSample();

        if (channels == 1) {
            if (bitsPerSample == 8) {
                return AL_FORMAT_MONO8;
            } else if (bitsPerSample == 16) {
                return AL_FORMAT_MONO16;
            }
        } else if (channels == 2) {
            if (bitsPerSample == 8) {
                return AL_FORMAT_STEREO8;
            } else if (bitsPerSample == 16) {
                return AL_FORMAT_STEREO16;
            }
        }
        // Add support for AL_EXT_MCFORMATS if needed later

        // Format not supported
        throw new UnsupportedOperationException("Unsupported audio format: "
                + channels + " channels, " + bitsPerSample + " bits per sample.");
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
        al.alBufferData(id, getOpenALFormat(ab), ab.getData(), ab.getData().capacity(), ab.getSampleRate());
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

    private void updateAudioData(AudioData audioData) {
        if (audioData instanceof AudioBuffer) {
            updateAudioBuffer((AudioBuffer) audioData);
        } else if (audioData instanceof AudioStream) {
            updateAudioStream((AudioStream) audioData);
        }
    }

    /**
     * Deletes the OpenAL filter object associated with the Filter.
     * @param filter The Filter object.
     */
    @Override
    public void deleteFilter(Filter filter) {
        int id = filter.getId();
        if (id != -1) {
            ib.position(0).limit(1);
            ib.put(id).flip();
            efx.alDeleteFilters(1, ib);
            checkAlError("deleting filter " + id);
            filter.resetObject();
        }
    }

    /**
     * Deletes the OpenAL objects associated with the AudioData.
     * @param audioData The AudioData to delete.
     */
    @Override
    public void deleteAudioData(AudioData audioData) {
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            if (audioData instanceof AudioBuffer) {
                AudioBuffer ab = (AudioBuffer) audioData;
                int id = ab.getId();
                if (id != -1) {
                    ib.put(0, id);
                    ib.position(0).limit(1);
                    al.alDeleteBuffers(1, ib);
                    checkAlError("deleting buffer " + id);
                    ab.resetObject();
                }
            } else if (audioData instanceof AudioStream) {
                AudioStream as = (AudioStream) audioData;
                int[] ids = as.getIds();
                if (ids != null) {
                    ib.clear();
                    ib.put(ids).flip();
                    al.alDeleteBuffers(ids.length, ib);
                    checkAlError("deleting " + ids.length + " buffers");
                    as.resetObject();
                }
            }
        }
    }

    /**
     * Checks for OpenAL errors and logs a warning if an error occurred.
     * @param location A string describing where the check is occurring (for logging).
     * @return True if an error occurred, false otherwise.
     */
    private boolean checkAlError(String location) {
        int error = al.alGetError();
        if (error != AL_NO_ERROR) {
            String errorString;
            switch (error) {
                case AL_INVALID_NAME:
                    errorString = "AL_INVALID_NAME";
                    break;
                case AL_INVALID_ENUM:
                    errorString = "AL_INVALID_ENUM";
                    break;
                case AL_INVALID_VALUE:
                    errorString = "AL_INVALID_VALUE";
                    break;
                case AL_INVALID_OPERATION:
                    errorString = "AL_INVALID_OPERATION";
                    break;
                case AL_OUT_OF_MEMORY:
                    errorString = "AL_OUT_OF_MEMORY";
                    break;
                default:
                    errorString = "Unknown AL error code: " + error;
                    break;
            }
            logger.log(Level.WARNING, "OpenAL Error ({0}) at {1}", new Object[]{errorString, location});
            return true;
        }
        return false;
    }
}
