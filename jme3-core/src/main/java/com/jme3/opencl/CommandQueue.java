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

/**
 * Wrapper for an OpenCL command queue.
 * The command queue serializes every GPU function call: By passing the same
 * queue to OpenCL function (buffer, image operations, kernel calls), it is 
 * ensured that they are executed in the order in which they are passed.
 * <br>
 * Each command queue is associtated with exactly one device: that device
 * is specified on creation ({@link Context#createQueue(com.jme3.opencl.Device) })
 * and all commands are sent to this device.
 * @author shaman
 */
public abstract class CommandQueue extends AbstractOpenCLObject {
	
	protected Device device;

    protected CommandQueue(ObjectReleaser releaser, Device device) {
        super(releaser);
		this.device = device;
    }

	@Override
	public CommandQueue register() {
		super.register();
		return this;
	}

	/**
	 * Returns the device associated with this command queue.
	 * It can be used to query properties of the device that is used to execute
	 * the commands issued to this command queue.
	 * @return the associated device
	 */
	public Device getDevice() {
		return device;
	}
    
    /**
     * Issues all previously queued OpenCL commands in command_queue to the
     * device associated with command queue. Flush only guarantees that all
     * queued commands to command_queue will eventually be submitted to the
     * appropriate device. There is no guarantee that they will be complete
     * after flush returns.
     */
    public abstract void flush();

    /**
     * Blocks until all previously queued OpenCL commands in command queue are
     * issued to the associated device and have completed. Finish does not
     * return until all previously queued commands in command queue have been
     * processed and completed. Finish is also a synchronization point.
     */
    public abstract void finish();

}
