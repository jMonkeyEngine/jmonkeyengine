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

    /**
     * Initializes the OpenAL and ALC context.
     */
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

        // Check for specific ALC extensions
        supportPauseDevice = alc.alcIsExtensionPresent("ALC_SOFT_pause_device");
        if (!supportPauseDevice) {
            logger.log(Level.WARNING, "Pausing audio device not supported (ALC_SOFT_pause_device).");
        }
        supportDisconnect = alc.alcIsExtensionPresent("ALC_EXT_disconnect");
        if (!supportDisconnect) {
            logger.log(Level.INFO, "Device disconnect detection not supported (ALC_EXT_disconnect).");
        }

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
            int sourceId = al.alGenSources();
            if (al.alGetError() != 0) {
                break;
            } else {
                channelList.add(sourceId);
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
            int majorVersion = ib.get(0);

            ib.clear().limit(1);
            alc.alcGetInteger(EFX.ALC_EFX_MINOR_VERSION, ib, 1);
            int minorVersion = ib.get(0);
            logger.log(Level.INFO, "Audio effect extension version: {0}.{1}", new Object[]{majorVersion, minorVersion});

            ib.clear().limit(1);
            alc.alcGetInteger(EFX.ALC_MAX_AUXILIARY_SENDS, ib, 1);
            int maxAuxSends = ib.get(0);
            logger.log(Level.INFO, "Audio max auxiliary sends: {0}", maxAuxSends);

            // 1. Create reverb effect slot
            ib.clear().limit(1);
            efx.alGenAuxiliaryEffectSlots(1, ib);
            reverbFxSlot = ib.get(0);

            // 2. Create reverb effect
            ib.clear().limit(1);
            efx.alGenEffects(1, ib);
            reverbFx = ib.get(0);

            // 3. Configure effect type
            efx.alEffecti(reverbFx, EFX.AL_EFFECT_TYPE, EFX.AL_EFFECT_REVERB);
            checkAlError("setting reverb effect type");

            // 4. attach reverb effect to effect slot
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
            return; // Nothing to destroy if context wasn't created
        }

        // Stops channels and detaches buffers/filters
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
                ib.clear().limit(1);
                ib.put(0, reverbFx);
                efx.alDeleteEffects(1, ib);
                checkAlError("deleting reverbFx effect " + reverbFx);
                reverbFx = -1;
            }

            if (reverbFxSlot != -1) {
                ib.clear().limit(1);
                ib.put(0, reverbFxSlot);
                efx.alDeleteAuxiliaryEffectSlots(1, ib);
                checkAlError("deleting effect reverbFxSlot " + reverbFxSlot);
                reverbFxSlot = -1;
            }
        }

        channels = null; // Force re-enumeration
        channelSources = null;
        freeChannels.clear();
        nextChannelIndex = 0;

        alc.destroyALC();
        logger.info("OpenAL context destroyed.");
    }

    /**
     * Initializes the OpenAL context, enumerates channels, checks capabilities,
     * and starts the audio decoder thread.
     */
    @Override
    public void initialize() {
        if (decoderThread.isAlive()) {
            throw new IllegalStateException("Initialize already called");
        }

        // Initialize OpenAL context.
        initOpenAL();

        if (audioDisabled) {
            logger.warning("Audio Disabled. Cannot start decoder thread.");
            return;
        }

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

        // Destroy OpenAL context
        destroyOpenAL();
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

            if (checkAlError("updating filter " + id)) {
                deleteFilter(f); // Try to clean up
            } else {
                f.clearUpdateNeeded(); // Mark as updated in AL
            }
        }
        // ** Add other filter types (HighPass, BandPass) here if implemented **
        else {
            throw new UnsupportedOperationException("Unsupported filter type: " + f.getClass().getName());
        }
    }

    /**
     * Gets the current playback time (in seconds) for a source.
     * For streams, includes the time represented by already processed buffers.
     * @param src The audio source.
     * @return The playback time in seconds, or 0 if not playing or invalid.
     */
    @Override
    public float getSourcePlaybackTime(AudioSource src) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return 0;
            }

            if (src.getChannel() < 0) {
                return 0; // Not playing or invalid state
            }

            int sourceId = channels[src.getChannel()];
            AudioData data = src.getAudioData();
            if (data == null) {
                return 0; // No audio data
            }
            int playbackOffsetBytes = 0;

            // For streams, add the bytes from buffers that have already been fully processed and unqueued.
            if (data instanceof AudioStream) {
                AudioStream stream = (AudioStream) data;
                // This value is updated by the decoder thread when buffers are unqueued.
                playbackOffsetBytes = stream.getUnqueuedBufferBytes();
            }

            // Add byte offset from source (for both streams and buffers)
            int byteOffset = al.alGetSourcei(sourceId, AL_BYTE_OFFSET);
            if (checkAlError("getting source byte offset for " + sourceId)) {
                return 0; // Error getting offset
            }
            playbackOffsetBytes += byteOffset;

            // Compute time value from bytes
            // E.g. for 44100 source with 2 channels and 16 bits per sample:
            //    (44100 * 2 * 16 / 8) = 176400
            int bytesPerSecond = (data.getSampleRate()
                    * data.getChannels()
                    * data.getBitsPerSample() / 8);

            if (bytesPerSecond <= 0) {
                logger.warning("Invalid bytesPerSecond calculated for source. Cannot get playback time.");
                return 0; // Avoid division by zero
            }

            return (float) playbackOffsetBytes / bytesPerSecond;
        }
    }

    /**
     * Updates a specific parameter for an audio source on its assigned channel.
     * @param src The audio source.
     * @param param The parameter to update.
     */
    @Override
    public void updateSourceParam(AudioSource src, AudioParam param) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            int channel = src.getChannel();
            // Parameter updates only make sense if the source is associated with a channel
            // and hasn't been stopped (which would set channel to -1).
            if (channel < 0) {
                // This can happen due to race conditions if a source stops playing
                // right as a parameter update is requested from another thread.
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Ignoring parameter update for source {0} as it's not validly associated with channel {1}.",
                            new Object[]{src, channel});
                }
                return;
            }

            int sourceId = channels[channel];

            switch (param) {
                case Position:
                    if (src.isPositional()) {
                        Vector3f pos = src.getPosition();
                        al.alSource3f(sourceId, AL_POSITION, pos.x, pos.y, pos.z);
                    }
                    break;

                case Velocity:
                    if (src.isPositional()) {
                        Vector3f vel = src.getVelocity();
                        al.alSource3f(sourceId, AL_VELOCITY, vel.x, vel.y, vel.z);
                    }
                    break;

                case MaxDistance:
                    if (src.isPositional()) {
                        al.alSourcef(sourceId, AL_MAX_DISTANCE, src.getMaxDistance());
                    }
                    break;

                case RefDistance:
                    if (src.isPositional()) {
                        al.alSourcef(sourceId, AL_REFERENCE_DISTANCE, src.getRefDistance());
                    }
                    break;

                case IsPositional:
                    applySourcePositionalState(sourceId, src);
                    break;

                case Direction:
                    if (src.isDirectional()) {
                        Vector3f dir = src.getDirection();
                        al.alSource3f(sourceId, AL_DIRECTION, dir.x, dir.y, dir.z);
                    }
                    break;

                case InnerAngle:
                    if (src.isDirectional()) {
                        al.alSourcef(sourceId, AL_CONE_INNER_ANGLE, src.getInnerAngle());
                    }
                    break;

                case OuterAngle:
                    if (src.isDirectional()) {
                        al.alSourcef(sourceId, AL_CONE_OUTER_ANGLE, src.getOuterAngle());
                    }
                    break;

                case IsDirectional:
                    applySourceDirectionalState(sourceId, src);
                    break;

                case DryFilter:
                    applySourceDryFilter(sourceId, src);
                    break;

                case ReverbEnabled:
                    if (!supportEfx || !src.isPositional()) {
                        return;
                    }
                    if (!src.isReverbEnabled()) {
                        al.alSource3i(sourceId, EFX.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX.AL_FILTER_NULL);
                    } else {
                        applySourceReverbFilter(sourceId, src);
                    }
                    break;

                case ReverbFilter:
                    if (src.isPositional()) {
                        applySourceReverbFilter(sourceId, src);
                    }
                    break;

                case Looping:
                    applySourceLooping(sourceId, src, false);
                    break;

                case Volume:
                    al.alSourcef(sourceId, AL_GAIN, src.getVolume());
                    break;

                case Pitch:
                    al.alSourcef(sourceId, AL_PITCH, src.getPitch());
                    break;

                default:
                    logger.log(Level.WARNING, "Unhandled source parameter update: {0}", param);
                    break;
            }
        }
    }

    /**
     * Applies all parameters from the AudioSource to the specified OpenAL source ID.
     * Used when initially playing a source or instance.
     *
     * @param sourceId     The OpenAL source ID.
     * @param src          The jME AudioSource.
     * @param forceNonLoop If true, looping will be disabled regardless of source setting (used for instances).
     */
    private void setSourceParams(int sourceId, AudioSource src, boolean forceNonLoop) {

        al.alSourcef(sourceId, AL_GAIN, src.getVolume());
        al.alSourcef(sourceId, AL_PITCH, src.getPitch());
        al.alSourcef(sourceId, AL_SEC_OFFSET, src.getTimeOffset());

        applySourceLooping(sourceId, src, forceNonLoop);
        applySourcePositionalState(sourceId, src);
        applySourceDirectionalState(sourceId, src);
        applySourceDryFilter(sourceId, src);
    }

    // --- Source Parameter Helper Methods ---

    private void applySourceDryFilter(int sourceId, AudioSource src) {
        if (supportEfx) {
            int filterId = EFX.AL_FILTER_NULL;
            if (src.getDryFilter() != null) {
                Filter f = src.getDryFilter();
                if (f.isUpdateNeeded()) {
                    updateFilter(f);
                }
                filterId = f.getId();
            }
            // NOTE: must re-attach filter for changes to apply.
            al.alSourcei(sourceId, EFX.AL_DIRECT_FILTER, filterId);
            checkAlError("setting source direct filter for " + sourceId);
        }
    }

    private void applySourceReverbFilter(int sourceId, AudioSource src) {
        if (supportEfx) {
            int filterId = EFX.AL_FILTER_NULL;
            if (src.isReverbEnabled() && src.getReverbFilter() != null) {
                Filter f = src.getReverbFilter();
                if (f.isUpdateNeeded()) {
                    updateFilter(f);
                }
                filterId = f.getId();
            }
            al.alSource3i(sourceId, EFX.AL_AUXILIARY_SEND_FILTER, reverbFxSlot, 0, filterId);
            checkAlError("setting source reverb send for " + sourceId);
        }
    }

    private void applySourceLooping(int sourceId, AudioSource src, boolean forceNonLoop) {
        boolean looping = !forceNonLoop && src.isLooping();
        // Streams handle looping internally by rewinding, not via AL_LOOPING.
        if (src.getAudioData() instanceof AudioStream) {
            looping = false;
        }
        al.alSourcei(sourceId, AL_LOOPING, looping ? AL_TRUE : AL_FALSE);
        checkAlError("setting source looping for " + sourceId);
    }

    /** Sets AL_SOURCE_RELATIVE and applies position/velocity/distance accordingly */
    private void applySourcePositionalState(int sourceId, AudioSource src) {
        if (src.isPositional()) {
            // Play in world space: absolute position/velocity
            Vector3f pos = src.getPosition();
            Vector3f vel = src.getVelocity();
            al.alSource3f(sourceId, AL_POSITION, pos.x, pos.y, pos.z);
            al.alSource3f(sourceId, AL_VELOCITY, vel.x, vel.y, vel.z);
            al.alSourcef(sourceId, AL_REFERENCE_DISTANCE, src.getRefDistance());
            al.alSourcef(sourceId, AL_MAX_DISTANCE, src.getMaxDistance());
            al.alSourcei(sourceId, AL_SOURCE_RELATIVE, AL_FALSE);

            if (supportEfx) {
                if (!src.isReverbEnabled()) {
                    al.alSource3i(sourceId, EFX.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX.AL_FILTER_NULL);
                } else {
                    applySourceReverbFilter(sourceId, src);
                }
            }
        } else {
            // Play in headspace: relative to listener, fixed position/velocity
            al.alSource3f(sourceId, AL_POSITION, 0, 0, 0);
            al.alSource3f(sourceId, AL_VELOCITY, 0, 0, 0);
            al.alSourcei(sourceId, AL_SOURCE_RELATIVE, AL_TRUE);

            // Disable reverb send for non-positional sounds
            if (supportEfx) {
                al.alSource3i(sourceId, EFX.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX.AL_FILTER_NULL);
            }
        }
        checkAlError("setting source positional state for " + sourceId);
    }

    /** Sets cone angles/gain based on whether the source is directional */
    private void applySourceDirectionalState(int sourceId, AudioSource src) {
        if (src.isDirectional()) {
            Vector3f dir = src.getDirection();
            al.alSource3f(sourceId, AL_DIRECTION, dir.x, dir.y, dir.z);
            al.alSourcef(sourceId, AL_CONE_INNER_ANGLE, src.getInnerAngle());
            al.alSourcef(sourceId, AL_CONE_OUTER_ANGLE, src.getOuterAngle());
            al.alSourcef(sourceId, AL_CONE_OUTER_GAIN, 0);
        } else {
            // Omnidirectional: 360 degree cone, full gain
            al.alSourcef(sourceId, AL_CONE_INNER_ANGLE, 360f);
            al.alSourcef(sourceId, AL_CONE_OUTER_ANGLE, 360f);
            al.alSourcef(sourceId, AL_CONE_OUTER_GAIN, 1f);
        }
        checkAlError("setting source directional state for " + sourceId);
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

    /**
     * Fills a single OpenAL buffer with data from the audio stream.
     * Uses the shared nativeBuf and arrayBuf.
     *
     * @param stream   The AudioStream to read from.
     * @param bufferId The OpenAL buffer ID to fill.
     * @return True if the buffer was filled with data, false if stream EOF was reached before filling.
     */
    private boolean fillBuffer(AudioStream stream, int bufferId) {
        int totalBytesRead = 0;
        int bytesRead;

        while (totalBytesRead < arrayBuf.length) {
            bytesRead = stream.readSamples(arrayBuf, totalBytesRead, arrayBuf.length - totalBytesRead);

            if (bytesRead > 0) {
                totalBytesRead += bytesRead;
            } else {
                break;
            }
        }

        if (totalBytesRead == 0) {
            return false;
        }

        // Copy data from arrayBuf to nativeBuf
        nativeBuf.clear();
        nativeBuf.put(arrayBuf, 0, totalBytesRead);
        nativeBuf.flip();

        // Upload data to the OpenAL buffer
        int format = getOpenALFormat(stream);
        int sampleRate = stream.getSampleRate();
        al.alBufferData(bufferId, format, nativeBuf, totalBytesRead, sampleRate);

        if (checkAlError("filling buffer " + bufferId + " for stream")) {
            return false;
        }

        return true;
    }

    /**
     * Unqueues processed buffers from a streaming source and refills/requeues them.
     * Updates the stream's internal count of processed bytes.
     *
     * @param sourceId The OpenAL source ID.
     * @param stream   The AudioStream.
     * @param looping  Whether the stream should loop internally.
     * @return True if at least one buffer was successfully refilled and requeued.
     */
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

            // Try to refill the buffer
            boolean filled = fillBuffer(stream, buffer);
            if (!filled && !stream.isEOF()) {
                throw new AssertionError();
            }

            if (!filled && looping) {
                stream.setTime(0);
                filled = fillBuffer(stream, buffer); // Try filling again
                if (!filled) {
                    throw new IllegalStateException("Looping streaming source "
                            + "was rewound but could not be filled");
                }
            }

            if (filled) {
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

        // Update the stream's internal counter for processed bytes
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
            // Try to refill the buffer
            boolean filled = fillBuffer(stream, id);
            if (!filled && !stream.isEOF()) {
                throw new AssertionError();
            }

            if (!filled && looping) {
                stream.setTime(0);
                filled = fillBuffer(stream, id);
                if (!filled) {
                    throw new IllegalStateException("Looping streaming source "
                            + "was rewound but could not be filled");
                }
            }

            if (filled) {
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

    /**
     * Stops the AL source on the channel, detaches buffers and filters,
     * and clears the jME source association. Does NOT free the channel index itself.
     *
     * @param index The channel index to clear.
     */
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

    private AudioSource.Status convertStatus(int openALState) {
        switch (openALState) {
            case AL_INITIAL:
            case AL_STOPPED:
                return Status.Stopped;
            case AL_PAUSED:
                return Status.Paused;
            case AL_PLAYING:
                return Status.Playing;
            default:
                throw new UnsupportedOperationException("Unrecognized OpenAL state: " + openALState);
        }
    }

    @Override
    public void update(float tpf) {
        synchronized (threadLock) {
            updateInRenderThread(tpf);
        }
    }

    /**
     * Checks the device connection status and attempts to restart the renderer if disconnected.
     * Called periodically from the decoder thread.
     */
    private void checkDevice() {
        if (isDeviceDisconnected()) {
            logger.log(Level.WARNING, "Audio device disconnected! Attempting to restart audio renderer...");
            restartAudioRenderer();
        }
    }

    /**
     * Checks if the audio device has been disconnected.
     * Requires ALC_EXT_disconnect extension.
     * @return True if disconnected, false otherwise or if not supported.
     */
    private boolean isDeviceDisconnected() {
        if (audioDisabled || !supportDisconnect) {
            return false;
        }

        ib.clear().limit(1);
        alc.alcGetInteger(ALC.ALC_CONNECTED, ib, 1);
        // Returns 1 if connected, 0 if disconnected.
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

    /**
     * Internal update logic called from the render thread within the lock.
     * Checks source statuses and reclaims finished channels.
     *
     * @param tpf Time per frame (currently unused).
     */
    public void updateInRenderThread(float tpf) {
        if (audioDisabled) {
            return;
        }

        for (int i = 0; i < channels.length; i++) {
            AudioSource src = channelSources[i];

            if (src == null) {
                continue; // No source on this channel
            }

            int sourceId = channels[i];
            boolean boundSource = i == src.getChannel();
            boolean reclaimChannel = false;

            // Get OpenAL status for the source
            int openALState = al.alGetSourcei(sourceId, AL_SOURCE_STATE);
            Status openALStatus = convertStatus(openALState);

            // --- Handle Instanced Playback (Not bound to a specific channel) ---
            if (!boundSource) {
                if (openALStatus == Status.Stopped) {
                    // Instanced audio (non-looping buffer) finished playing. Reclaim channel.
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Reclaiming channel {0} from finished instance.", i);
                    }
                    clearChannel(i); // Stop source, detach buffer/filter
                    freeChannel(i);  // Add channel back to the free pool
                } else if (openALStatus == Status.Paused) {
                    throw new AssertionError("Instanced audio source on channel " + i + " cannot be paused.");
                }

                // If Playing, do nothing, let it finish.
                continue;
            }

            // --- Handle Bound Playback (Normal play/pause/stop) ---
            Status jmeStatus = src.getStatus();

            // Check if we need to sync JME status with OpenAL status.
            if (openALStatus != jmeStatus) {
                if (openALStatus == Status.Stopped && jmeStatus == Status.Playing) {

                    // Source stopped playing unexpectedly (finished or starved)
                    if (src.getAudioData() instanceof AudioStream) {
                        AudioStream stream = (AudioStream) src.getAudioData();

                        if (stream.isEOF() && !src.isLooping()) {
                            // Stream reached EOF and is not looping.
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, "Stream source on channel {0} finished.", i);
                            }
                            reclaimChannel = true;
                        } else {
                            // Stream still has data or is looping, but stopped.
                            // This indicates buffer starvation. The decoder thread will handle restarting it.
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, "Stream source on channel {0} likely starved.", i);
                            }
                            // Don't reclaim channel here, let decoder thread refill and restart.
                        }
                    } else {
                        // Buffer finished playing.
                        if (src.isLooping()) {
                            // This is unexpected for looping buffers unless the device was disconnected/reset.
                            logger.log(Level.WARNING, "Looping buffer source on channel {0} stopped unexpectedly.", i);
                        }  else {
                            // Non-looping buffer finished normally.
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, "Buffer source on channel {0} finished.", i);
                            }
                        }

                        reclaimChannel = true;
                    }

                    if (reclaimChannel) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "Reclaiming channel {0} from finished source.", i);
                        }
                        src.setStatus(Status.Stopped);
                        src.setChannel(-1);
                        clearChannel(i); // Stop AL source, detach buffers/filters
                        freeChannel(i);  // Add channel back to the free pool
                    }
                } else {
                    // jME3 state does not match OpenAL state.
                    // This is only relevant for bound sources.
                    throw new AssertionError("Unexpected sound status. "
                            + "OpenAL: " + openALStatus + ", JME: " + jmeStatus);
                }
            } else {
                // Stopped channel was not cleared correctly.
                if (openALStatus == Status.Stopped) {
                    throw new AssertionError("Channel " + i + " was not reclaimed");
                }
            }
        }
    }

    /**
     * Internal update logic called from the decoder thread within the lock.
     * Fills streaming buffers and restarts starved sources. Deletes unused objects.
     *
     * @param tpf Time per frame (currently unused).
     */
    public void updateInDecoderThread(float tpf) {
        if (audioDisabled) {
            return;
        }

        for (int i = 0; i < channels.length; i++) {
            AudioSource src = channelSources[i];

            // Only process streaming sources associated with this channel
            if (src == null || !(src.getAudioData() instanceof AudioStream)) {
                continue;
            }

            int sourceId = channels[i];
            AudioStream stream = (AudioStream) src.getAudioData();

            // Get current AL state, primarily to check if we need to restart playback
            int openALState = al.alGetSourcei(sourceId, AL_SOURCE_STATE);
            Status openALStatus = convertStatus(openALState);
            Status jmeStatus = src.getStatus();

            // Keep filling data (even if we are stopped / paused)
            boolean buffersWereFilled = fillStreamingSource(sourceId, stream, src.isLooping());

            // Check if the source stopped due to buffer starvation while it was supposed to be playing
            if (buffersWereFilled
                    && openALStatus == Status.Stopped
                    && jmeStatus == Status.Playing) {
                // The source got stopped due to buffer starvation.
                // Start it again.
                logger.log(Level.WARNING, "Buffer starvation detected for stream on channel {0}. Restarting playback.", i);
                al.alSourcePlay(sourceId);
                checkAlError("restarting starved source " + sourceId);
            }
        }

        // Delete any unused objects (buffers, filters) that are no longer referenced.
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

    /**
     * Pauses all audio playback by pausing the OpenAL device context.
     * Requires ALC_SOFT_pause_device extension.
     * @throws UnsupportedOperationException if the extension is not supported.
     */
    @Override
    public void pauseAll() {
        if (!supportPauseDevice) {
            throw new UnsupportedOperationException(
                    "Pausing the audio device is not supported by the current OpenAL driver" +
                            " (requires ALC_SOFT_pause_device).");
        }

        alc.alcDevicePauseSOFT();
        logger.info("Audio device paused.");
    }

    /**
     * Resumes all audio playback by resuming the OpenAL device context.
     * Requires ALC_SOFT_pause_device extension.
     * @throws UnsupportedOperationException if the extension is not supported.
     */
    @Override
    public void resumeAll() {
        if (!supportPauseDevice) {
            throw new UnsupportedOperationException(
                    "Resuming the audio device is not supported by the current OpenAL driver" +
                            " (requires ALC_SOFT_pause_device).");
        }

        alc.alcDeviceResumeSOFT();
        logger.info("Audio device resumed.");
    }

    /**
     * Plays an audio source as a one-shot instance (non-looping buffer).
     * A free channel is allocated temporarily.
     * @param src The audio source to play.
     */
    @Override
    public void playSourceInstance(AudioSource src) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            AudioData audioData = src.getAudioData();
            if (audioData == null) {
                logger.warning("playSourceInstance called with null AudioData.");
                return;
            }
            if (audioData instanceof AudioStream) {
                throw new UnsupportedOperationException(
                        "Cannot play instances of audio streams. Use play() instead.");
            }

            if (audioData.isUpdateNeeded()) {
                updateAudioData(audioData);
            }

            // Allocate a temporary channel
            int index = newChannel();
            if (index == -1) {
                logger.log(Level.WARNING, "No channel available to play instance of {0}", src);
                return;
            }

            // Ensure channel is clean before use
            int sourceId = channels[index];
            clearChannel(index);

            // Set parameters for this specific instance (force non-looping)
            setSourceParams(sourceId, src, true);
            attachAudioToSource(sourceId, audioData, false);
            channelSources[index] = src;

            // play the channel
            al.alSourcePlay(sourceId);
            checkAlError("playing source instance " + sourceId);
        }
    }

    /**
     * Plays an audio source, allocating a persistent channel for it.
     * Handles both buffers and streams. Can be paused and stopped.
     * @param src The audio source to play.
     */
    @Override
    public void playSource(AudioSource src) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            if (src.getStatus() == Status.Playing) {
                // Already playing, do nothing.
                return;
            }

            if (src.getStatus() == Status.Stopped) {

                AudioData audioData = src.getAudioData();
                if (audioData == null) {
                    logger.log(Level.WARNING, "playSource called on source with null AudioData: {0}", src);
                    return;
                }

                // Allocate a temporary channel
                int index = newChannel();
                if (index == -1) {
                    logger.log(Level.WARNING, "No channel available to play instance of {0}", src);
                    return;
                }

                // Ensure channel is clean before use
                int sourceId = channels[index];
                clearChannel(index);
                src.setChannel(index);

                if (audioData.isUpdateNeeded()) {
                    updateAudioData(audioData);
                }

                // Set all source parameters and attach the audio data
                channelSources[index] = src;
                setSourceParams(sourceId, src, false);
                attachAudioToSource(sourceId, audioData, src.isLooping());
            }

            // play the channel
            int sourceId = channels[src.getChannel()];
            al.alSourcePlay(sourceId);
            if (!checkAlError("playing source " + sourceId)) {
                src.setStatus(Status.Playing); // Update JME status on success
            }
        }
    }

    /**
     * Pauses a playing audio source.
     * @param src The audio source to pause.
     */
    @Override
    public void pauseSource(AudioSource src) {
        checkDead();
        synchronized (threadLock) {
            if (audioDisabled) {
                return;
            }

            if (src.getStatus() == Status.Playing) {
                assert src.getChannel() != -1;

                int sourceId = channels[src.getChannel()];
                al.alSourcePause(sourceId);
                if (!checkAlError("pausing source " + sourceId)) {
                    src.setStatus(Status.Paused); // Update JME status on success
                }
            }
        }
    }

    /**
     * Stops a playing or paused audio source, releasing its channel.
     * For streams, resets or closes the stream.
     * @param src The audio source to stop.
     */
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
                    // If the stream is seekable, rewind it to the beginning.
                    // Otherwise (non-seekable), close it, as it might be invalid now.
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
     * @return The OpenAL format enum.
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

        // Format not supported
        throw new UnsupportedOperationException("Unsupported audio format: "
                + channels + " channels, " + bitsPerSample + " bits per sample.");
    }

    /**
     * Uploads buffer data to OpenAL. Generates buffer ID if needed.
     * @param ab The AudioBuffer.
     */
    private void updateAudioBuffer(AudioBuffer ab) {
        int id = ab.getId();
        if (ab.getId() == -1) {
            ib.clear().limit(1);
            al.alGenBuffers(1, ib);
            checkAlError("generating bufferId");
            id = ib.get(0);
            ab.setId(id);

            // Register for automatic cleanup if unused
            objManager.registerObject(ab);
        }

        ByteBuffer data = ab.getData();

        data.clear(); // Ensure buffer is ready for reading
        int format = getOpenALFormat(ab);
        int sampleRate = ab.getSampleRate();

        al.alBufferData(id, format, data, data.capacity(), sampleRate);
        if (!checkAlError("uploading buffer data for ID " + id)) {
            ab.clearUpdateNeeded();
        }
    }

    /**
     * Prepares OpenAL buffers for an AudioStream. Generates buffer IDs.
     * Does not fill buffers with data yet.
     * @param as The AudioStream.
     */
    private void updateAudioStream(AudioStream as) {
        // Delete old buffers if they exist (e.g., re-initializing stream)
        if (as.getIds() != null) {
            deleteAudioData(as);
        }

        int[] ids = new int[STREAMING_BUFFER_COUNT];
        ib.clear().limit(STREAMING_BUFFER_COUNT);

        al.alGenBuffers(STREAMING_BUFFER_COUNT, ib);
        checkAlError("generating stream buffers ids");

        ib.rewind();
        ib.get(ids);

        // Streams are managed directly, not via NativeObjectManager,
        // because their lifecycle is tied to active playback.
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
            ib.clear().limit(1);
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
                    ib.clear().limit(1);
                    al.alDeleteBuffers(1, ib);
                    checkAlError("deleting buffer " + id);
                    ab.resetObject(); // Mark as deleted on JME side
                }
            } else if (audioData instanceof AudioStream) {
                AudioStream as = (AudioStream) audioData;
                int[] ids = as.getIds();
                if (ids != null) {
                    ib.clear();
                    ib.put(ids).flip();
                    al.alDeleteBuffers(ids.length, ib);
                    checkAlError("deleting " + ids.length + " buffers");
                    as.resetObject(); // Mark as deleted on JME side
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
