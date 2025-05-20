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
 * Represents an OpenAL EFX High-Pass Filter.
 */
public class HighPassFilter extends Filter {

    // Default values based on OpenAL EFX specification defaults
    protected float volume = 1.0f;
    protected float lowFreqVolume = 1.0f;

    /**
     * Constructs a high-pass filter with default settings.
     * Required for jME deserialization
     */
    public HighPassFilter(){}

    protected HighPassFilter(int id) {
        super(id);
    }

    public HighPassFilter(float volume, float lowFreqVolume) {
        super();
        setVolume(volume);
        setLowFreqVolume(lowFreqVolume);
    }

    public float getVolume() {
        return volume;
    }

    /**
     * Sets the gain of the High-Pass filter.
     * The change is immediately applied to the native OpenAL filter.
     *
     * @param volume The gain value (0.0 to 1.0).
     */
    public void setVolume(float volume) {
        if (volume < 0 || volume > 1)
            throw new IllegalArgumentException("Volume must be between 0 and 1");

        this.volume = volume;
        this.updateNeeded = true;
    }

    public float getLowFreqVolume() {
        return lowFreqVolume;
    }

    /**
     * Sets the gain at low frequencies for the High-Pass filter.
     * The change is immediately applied to the native OpenAL filter.
     *
     * @param lowFreqVolume The low-frequency gain value (0.0 to 1.0).
     */
    public void setLowFreqVolume(float lowFreqVolume) {
        if (lowFreqVolume < 0 || lowFreqVolume > 1)
            throw new IllegalArgumentException("Low freq volume must be between 0 and 1");

        this.lowFreqVolume = lowFreqVolume;
        this.updateNeeded = true;
    }

    @Override
    public NativeObject createDestructableClone() {
        return new HighPassFilter(this.id);
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

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(this.volume, "volume", 10.f);
        oc.write(this.lowFreqVolume, "lf_volume", 1.0f);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        this.volume = ic.readFloat("volume", 1.0f);
        this.lowFreqVolume = ic.readFloat("lf_volume", 1.0f);
    }
}
