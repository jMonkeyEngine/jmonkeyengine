/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.opencl;

import java.util.Collection;

/**
 *
 * @author Sebastian Weiss
 */
public final class Device {
	private final long device;

	public Device(long device) {
		this.device = device;
	}
	
	public static enum DeviceType {
		DEFAULT,
		CPU,
		GPU,
		ACCELEARTOR,
		ALL
	}
	public DeviceType getDeviceType() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int getVendorId() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public boolean isAvailable() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int getPCIeBus() {
		throw new UnsupportedOperationException("not supported yet");
	}
	
	public boolean hasCompiler() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public boolean hasDouble() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public boolean hasHalfFloat() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public boolean hasErrorCorrectingMemory() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public boolean hasUnifiedMemory() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public boolean hasImageSupport() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public boolean hasExtension(String extension) {
		throw new UnsupportedOperationException("not supported yet");
	}
	public Collection<? extends String> getExtensions() {
		throw new UnsupportedOperationException("not supported yet");
	}
	
	public int getComputeUnits() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int getClockFrequency() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int getAddressBits() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public boolean isLittleEndian() {
		throw new UnsupportedOperationException("not supported yet");
	}
	
	public int getMaximumWorkItemDimensions() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int[] getMaximumWorkItemSizes() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int getMaxiumWorkItemsPerGroup() {
		throw new UnsupportedOperationException("not supported yet");
	}
	
	public int getMaximumSamplers() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int getMaximumReadImages() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int getMaximumWriteImages() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int[] getMaximumImage2DSize() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int[] getMaximumImage3DSize() {
		throw new UnsupportedOperationException("not supported yet");
	}
	
	//TODO: cache, prefered sizes properties
	
	public String getProfile() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public String getVersion() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int getVersionMajor() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int getVersionMinor() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public String getCompilerVersion() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int getCompilerVersionMajor() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int getCompilerVersionMinor() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public String getDriverVersion() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int getDriverVersionMajor() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public int getDriverVersionMinor() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public String getName() {
		throw new UnsupportedOperationException("not supported yet");
	}
	public String getVendor() {
		throw new UnsupportedOperationException("not supported yet");
	}

	@Override
	public String toString() {
		throw new UnsupportedOperationException("not supported yet");
	}
}
