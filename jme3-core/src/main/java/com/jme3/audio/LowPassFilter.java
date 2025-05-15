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
package com.jme3.audio;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.util.NativeObject;

import java.io.IOException;

/**
 * A filter that attenuates frequencies above a specified threshold, allowing lower
 * frequencies to pass through with less attenuation. Commonly used to simulate effects
 * such as muffling or underwater acoustics.
 */
public class LowPassFilter extends Filter {

    /**
     * The overall volume scaling of the filtered sound
     */
    protected float volume = 1.0f;
    /**
     * The volume scaling of the high frequencies allowed to pass through. Valid values range
     * from 0.0 to 1.0, where 0.0 completely eliminates high frequencies and 1.0 lets them pass
     * through unchanged.
     */
    protected float highFreqVolume = 1.0f;

    /**
     * Constructs a low-pass filter with default settings.
     * Required for jME deserialization.
     */
    public LowPassFilter() {
        super();
    }

    /**
     * Constructs a low-pass filter.
     *
     * @param volume         the overall volume scaling of the filtered sound (0.0 - 1.0).
     * @param highFreqVolume the volume scaling of high frequencies (0.0 - 1.0).
     * @throws IllegalArgumentException if {@code volume} or {@code highFreqVolume} is out of range.
     */
    public LowPassFilter(float volume, float highFreqVolume) {
        super();
        setVolume(volume);
        setHighFreqVolume(highFreqVolume);
    }

    /**
     * For internal cloning
     * @param id the native object ID
     */
    protected LowPassFilter(int id) {
        super(id);
    }

    /**
     * Retrieves the current volume scaling of high frequencies.
     *
     * @return the high-frequency volume scaling.
     */
    public float getHighFreqVolume() {
        return highFreqVolume;
    }

    /**
     * Sets the high-frequency volume.
     *
     * @param highFreqVolume the new high-frequency volume scaling (0.0 - 1.0).
     * @throws IllegalArgumentException if {@code highFreqVolume} is out of range.
     */
    public void setHighFreqVolume(float highFreqVolume) {
        if (highFreqVolume < 0 || highFreqVolume > 1)
            throw new IllegalArgumentException("High freq volume must be between 0 and 1");

        this.highFreqVolume = highFreqVolume;
        this.updateNeeded = true;
    }

    /**
     * Retrieves the current overall volume scaling of the filtered sound.
     *
     * @return the overall volume scaling.
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Sets the overall volume.
     *
     * @param volume the new overall volume scaling (0.0 - 1.0).
     * @throws IllegalArgumentException if {@code volume} is out of range.
     */
    public void setVolume(float volume) {
        if (volume < 0 || volume > 1)
            throw new IllegalArgumentException("Volume must be between 0 and 1");

        this.volume = volume;
        this.updateNeeded = true;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(volume, "volume", 1f);
        oc.write(highFreqVolume, "hf_volume", 1f);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        volume = ic.readFloat("volume", 1f);
        highFreqVolume = ic.readFloat("hf_volume", 1f);
    }

    /**
     * Creates a native object clone of this filter for internal usage.
     *
     * @return a new {@code LowPassFilter} instance with the same native ID.
     */
    @Override
    public NativeObject createDestructableClone() {
        return new LowPassFilter(id);
    }

    /**
     * Retrieves a unique identifier for this filter. Used internally for native object management.
     *
     * @return a unique long identifier.
     */
    @Override
    public long getUniqueId() {
        return ((long) OBJTYPE_FILTER << 32) | (0xffffffffL & (long) id);
    }
}
