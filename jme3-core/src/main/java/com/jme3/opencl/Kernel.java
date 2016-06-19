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

import com.jme3.math.*;
import com.jme3.util.TempVars;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Wrapper for an OpenCL kernel, a piece of executable code on the GPU.
 * <p>
 * Terminology:<br>
 * A Kernel is executed in parallel. In total number of parallel threads, 
 * called work items, are specified by the <i>global work size</i> (of type
 * {@link WorkSize}. These threads are organized in a 1D, 2D or 3D grid
 * (of coarse, this is only a logical view). Inside each kernel,
 * the id of each thread (i.e. the index inside this grid) can be requested
 * by {@code get_global_id(dimension)} with {@code dimension=0,1,2}.
 * <br>
 * Not all threads can always be executed in parallel because there simply might
 * not be enough processor cores.
 * Therefore, the concept of a <i>work group</i> is introduced. The work group
 * specifies the actual number of threads that are executed in parallel.
 * The maximal size of it can be queried by {@link Device#getMaxiumWorkItemsPerGroup() }.
 * Again, the threads inside the work group can be organized in a 1D, 2D or 3D
 * grid, but this is also just a logical view (specifying how the threads are
 * indexed). 
 * The work group is imporatant for another concept: <i> shared memory</i>
 * Unlike the normal global or constant memory (passing a {@link Buffer} object
 * as argument), shared memory can't be set from outside. Shared memory is
 * allocated by the kernel and is only valid within the kernel. It is used
 * to quickly share data between threads within a work group.
 * The size of the shared memory is specified by setting an instance of
 * {@link LocalMem} or {@link LocalMemPerElement} as argument.<br>
 * Due to heavy register usage or other reasons, a kernel might not be able
 * to utilize a whole work group. Therefore, the actual number of threads
 * that can be executed in a work group can be queried by 
 * {@link #getMaxWorkGroupSize(com.jme3.opencl.Device) }, which might differ from the 
 * value returned from the Device.
 * 
 * <p>
 * There are two ways to launch a kernel:<br>
 * First, arguments and the work group sizes can be set in advance 
 * ({@code setArg(index, ...)}, {@code setGlobalWorkSize(...)} and {@code setWorkGroupSize(...)}.
 * Then a kernel is launched by {@link #Run(com.jme3.opencl.CommandQueue) }.<br>
 * Second, two convenient functions are provided that set the arguments
 * and work sizes in one call:
 * {@link #Run1(com.jme3.opencl.CommandQueue, com.jme3.opencl.Kernel.WorkSize, java.lang.Object...) }
 * and {@link #Run2(com.jme3.opencl.CommandQueue, com.jme3.opencl.Kernel.WorkSize, com.jme3.opencl.Kernel.WorkSize, java.lang.Object...) }.
 * 
 * @author shaman
 * @see Program#createKernel(java.lang.String) 
 */
public abstract class Kernel extends AbstractOpenCLObject {
    /**
     * The current global work size
     */
    protected final WorkSize globalWorkSize;
    /**
     * The current local work size
     */
    protected final WorkSize workGroupSize;

    protected Kernel(ObjectReleaser releaser) {
        super(releaser);
        this.globalWorkSize = new WorkSize(0);
        this.workGroupSize = new WorkSize(0);
    }

	@Override
	public Kernel register() {
		super.register();
		return this;
	}
	
    /**
     * @return the name of the kernel as defined in the program source code
     */
    public abstract String getName();

    /**
     * @return the number of arguments
     */
    public abstract int getArgCount();

    /**
     * @return the current global work size
     */
    public WorkSize getGlobalWorkSize() {
        return globalWorkSize;
    }

    /**
     * Sets the global work size.
     * @param ws the work size to set
     */
    public void setGlobalWorkSize(WorkSize ws) {
        globalWorkSize.set(ws);
    }

    /**
     * Sets the global work size to a 1D grid
     * @param size the size in 1D
     */
    public void setGlobalWorkSize(int size) {
        globalWorkSize.set(1, size);
    }

    /**
     * Sets the global work size to be a 2D grid
     * @param width the width
     * @param height the height
     */
    public void setGlobalWorkSize(int width, int height) {
        globalWorkSize.set(2, width, height);
    }

    /**
     * Sets the global work size to be a 3D grid
     * @param width the width
     * @param height the height
     * @param depth the depth
     */
    public void setGlobalWorkSize(int width, int height, int depth) {
        globalWorkSize.set(3, width, height, depth);
    }

    /**
     * @return the current work group size
     */
    public WorkSize getWorkGroupSize() {
        return workGroupSize;
    }

    /**
     * Sets the work group size
     * @param ws the work group size to set
     */
    public void setWorkGroupSize(WorkSize ws) {
        workGroupSize.set(ws);
    }

    /**
     * Sets the work group size to be a 1D grid
     * @param size the size to set
     */
    public void setWorkGroupSize(int size) {
        workGroupSize.set(1, size);
    }

    /**
     * Sets the work group size to be a 2D grid
     * @param width the width
     * @param height the height
     */
    public void setWorkGroupSize(int width, int height) {
        workGroupSize.set(2, width, height);
    }

    /**
     * Sets the work group size to be a 3D grid
     * @param width the width
     * @param height the height
     * @param depth the depth
     */
    public void setWorkGroupSdize(int width, int height, int depth) {
        workGroupSize.set(3, width, height, depth);
    }
    
    /**
     * Tells the driver to figure out the work group size on their own.
     * Use this if you do not rely on specific work group layouts, i.e.
     * because shared memory is not used.
     * {@link #Run1(com.jme3.opencl.CommandQueue, com.jme3.opencl.Kernel.WorkSize, java.lang.Object...) }
     * implicetly calls this mehtod.
     */
    public void setWorkGroupSizeToNull() {
        workGroupSize.set(1, 0, 0, 0);
    }
    
    /**
     * Returns the maximal work group size when this kernel is executed on
     * the specified device
     * @param device the device
     * @return the maximal work group size
     */
    public abstract long getMaxWorkGroupSize(Device device);

    public abstract void setArg(int index, LocalMemPerElement t);

    public abstract void setArg(int index, LocalMem t);

    public abstract void setArg(int index, Buffer t);
    
    public abstract void setArg(int index, Image i);

    public abstract void setArg(int index, byte b);

    public abstract void setArg(int index, short s);

    public abstract void setArg(int index, int i);

    public abstract void setArg(int index, long l);

    public abstract void setArg(int index, float f);

    public abstract void setArg(int index, double d);

    public abstract void setArg(int index, Vector2f v);
  
    public abstract void setArg(int index, Vector4f v);

    public abstract void setArg(int index, Quaternion q);
    
    public abstract void setArg(int index, Matrix4f mat);
    
    public void setArg(int index, Matrix3f mat) {
        TempVars vars = TempVars.get();
        try {
            Matrix4f m = vars.tempMat4;
            m.zero();
            for (int i=0; i<3; ++i) {
                for (int j=0; j<3; ++j) {
                    m.set(i, j, mat.get(i, j));
                }
            }
            setArg(index, m);
        } finally {
            vars.release();
        }
    }
    
    /**
     * Raw version to set an argument.
     * {@code size} bytes of the provided byte buffer are copied to the kernel
     * argument. The size in bytes must match exactly the argument size
     * as defined in the kernel code.
     * Use this method to send custom structures to the kernel
     * @param index the index of the argument
     * @param buffer the raw buffer
     * @param size the size in bytes
     */
    public abstract void setArg(int index, ByteBuffer buffer, long size);

    /**
     * Sets the kernel argument at the specified index.<br>
     * The argument must be a known type:
     * {@code LocalMemPerElement, LocalMem, Image, Buffer, byte, short, int,
     * long, float, double, Vector2f, Vector4f, Quaternion, Matrix3f, Matrix4f}.
     * <br>
     * Note: Matrix3f and Matrix4f will be mapped to a {@code float16} (row major).
     * @param index the index of the argument, from 0 to {@link #getArgCount()}-1
     * @param arg the argument
     * @throws IllegalArgumentException if the argument type is not one of the listed ones
     */
    public void setArg(int index, Object arg) {
        if (arg instanceof Byte) {
            setArg(index, (byte) arg);
        } else if (arg instanceof Short) {
            setArg(index, (short) arg);
        } else if (arg instanceof Integer) {
            setArg(index, (int) arg);
        } else if (arg instanceof Long) {
            setArg(index, (long) arg);
        } else if (arg instanceof Float) {
            setArg(index, (float) arg);
        } else if (arg instanceof Double) {
            setArg(index, (double) arg);
        } else if (arg instanceof Vector2f) {
            setArg(index, (Vector2f) arg);
        } else if (arg instanceof Vector4f) {
            setArg(index, (Vector4f) arg);
        } else if (arg instanceof Quaternion) {
            setArg(index, (Quaternion) arg);
        } else if (arg instanceof Matrix3f) {
            setArg(index, (Matrix3f) arg);
        } else if (arg instanceof Matrix4f) {
            setArg(index, (Matrix4f) arg);
        } else if (arg instanceof LocalMemPerElement) {
            setArg(index, (LocalMemPerElement) arg);
        } else if (arg instanceof LocalMem) {
            setArg(index, (LocalMem) arg);
        } else if (arg instanceof Buffer) {
            setArg(index, (Buffer) arg);
        } else if (arg instanceof Image) {
            setArg(index, (Image) arg);
        } else {
            throw new IllegalArgumentException("unknown kernel argument type: " + arg);
        }
    }

    private void setArgs(Object... args) {
        for (int i = 0; i < args.length; ++i) {
            setArg(i, args[i]);
        }
    }

    /**
     * Launches the kernel with the current global work size, work group size
     * and arguments.
     * If the returned event object is not needed and would otherwise be
     * released immediately, {@link #RunNoEvent(com.jme3.opencl.CommandQueue) }
     * might bring a better performance.
     * @param queue the command queue
     * @return an event object indicating when the kernel is finished
     * @see #setGlobalWorkSize(com.jme3.opencl.Kernel.WorkSize) 
     * @see #setWorkGroupSize(com.jme3.opencl.Kernel.WorkSize) 
     * @see #setArg(int, java.lang.Object) 
     */
    public abstract Event Run(CommandQueue queue);
    
    /**
     * Launches the kernel with the current global work size, work group size
     * and arguments without returning an event object.
     * The generated event is directly released. Therefore, the performance
     * is better, but there is no way to detect when the kernel execution
     * has finished. For this purpose, use {@link #Run(com.jme3.opencl.CommandQueue) }.
     * @param queue the command queue
     * @see #setGlobalWorkSize(com.jme3.opencl.Kernel.WorkSize) 
     * @see #setWorkGroupSize(com.jme3.opencl.Kernel.WorkSize) 
     * @see #setArg(int, java.lang.Object) 
     */
    public void RunNoEvent(CommandQueue queue) {
        //Default implementation, overwrite to not allocate the event object
        Run(queue).release();
    }

    /**
     * Sets the work sizes and arguments in one call and launches the kernel.
     * The global work size is set to the specified size. The work group
     * size is automatically determined by the driver.
     * Each object in the argument array is sent to the kernel by
     * {@link #setArg(int, java.lang.Object) }.
     * @param queue the command queue
     * @param globalWorkSize the global work size
     * @param args the kernel arguments
     * @return an event object indicating when the kernel is finished
     * @see #Run2(com.jme3.opencl.CommandQueue, com.jme3.opencl.Kernel.WorkSize, com.jme3.opencl.Kernel.WorkSize, java.lang.Object...) 
     */
    public Event Run1(CommandQueue queue, WorkSize globalWorkSize, Object... args) {
        setGlobalWorkSize(globalWorkSize);
        setWorkGroupSizeToNull();
        setArgs(args);
        return Run(queue);
    }
    
    /**
     * Sets the work sizes and arguments in one call and launches the kernel.
     * The global work size is set to the specified size. The work group
     * size is automatically determined by the driver.
     * Each object in the argument array is sent to the kernel by
     * {@link #setArg(int, java.lang.Object) }.
     * The generated event is directly released. Therefore, the performance
     * is better, but there is no way to detect when the kernel execution
     * has finished. For this purpose, use 
     * {@link #Run1(com.jme3.opencl.CommandQueue, com.jme3.opencl.Kernel.WorkSize, java.lang.Object...) }.
     * @param queue the command queue
     * @param globalWorkSize the global work size
     * @param args the kernel arguments
     * @see #Run2(com.jme3.opencl.CommandQueue, com.jme3.opencl.Kernel.WorkSize, com.jme3.opencl.Kernel.WorkSize, java.lang.Object...) 
     */
    public void Run1NoEvent(CommandQueue queue, WorkSize globalWorkSize, Object... args) {
        setGlobalWorkSize(globalWorkSize);
        setWorkGroupSizeToNull();
        setArgs(args);
        RunNoEvent(queue);
    }

    /**
     * Sets the work sizes and arguments in one call and launches the kernel.
     * @param queue the command queue
     * @param globalWorkSize the global work size
     * @param workGroupSize the work group size
     * @param args the kernel arguments
     * @return an event object indicating when the kernel is finished
     */
    public Event Run2(CommandQueue queue, WorkSize globalWorkSize,
            WorkSize workGroupSize, Object... args) {
        setGlobalWorkSize(globalWorkSize);
        setWorkGroupSize(workGroupSize);
        setArgs(args);
        return Run(queue);
    }

    /**
     * Sets the work sizes and arguments in one call and launches the kernel.
     * The generated event is directly released. Therefore, the performance
     * is better, but there is no way to detect when the kernel execution
     * has finished. For this purpose, use 
     * {@link #Run2(com.jme3.opencl.CommandQueue, com.jme3.opencl.Kernel.WorkSize, com.jme3.opencl.Kernel.WorkSize, java.lang.Object...) }.
     * @param queue the command queue
     * @param globalWorkSize the global work size
     * @param workGroupSize the work group size
     * @param args the kernel arguments
     */
    public void Run2NoEvent(CommandQueue queue, WorkSize globalWorkSize,
            WorkSize workGroupSize, Object... args) {
        setGlobalWorkSize(globalWorkSize);
        setWorkGroupSize(workGroupSize);
        setArgs(args);
        RunNoEvent(queue);
    }

	@Override
	public String toString() {
		return "Kernel (" + getName() + ")";
	}
	
    /**
     * A placeholder for kernel arguments representing local kernel memory.
     * This defines the size of available shared memory of a {@code __shared} kernel
     * argument
     */
    public static final class LocalMem {

        private int size;

        /**
         * Creates a new LocalMem instance
         * @param size the size of the available shared memory in bytes
         */
        public LocalMem(int size) {
            super();
            this.size = size;
        }

        public int getSize() {
            return size;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + this.size;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LocalMem other = (LocalMem) obj;
            if (this.size != other.size) {
                return false;
            }
            return true;
        }

		@Override
		public String toString() {
			return "LocalMem (" + size + "B)";
		}
		
    }

    /**
     * A placeholder for a kernel argument representing local kernel memory per thread.
     * This effectively computes {@code SharedMemoryPerElement * WorkGroupSize}
     * and uses this value as the size of shared memory available in the kernel.
     * Therefore, an instance of this class must be set as an argument AFTER
     * the work group size has been specified. This is
     * ensured by {@link #Run2(com.jme3.opencl.CommandQueue, com.jme3.opencl.Kernel.WorkSize, com.jme3.opencl.Kernel.WorkSize, java.lang.Object...) }.
     * This argument can't be used when no work group size was defined explicetly
     * (e.g. by {@link #setWorkGroupSizeToNull()} or {@link #Run1(com.jme3.opencl.CommandQueue, com.jme3.opencl.Kernel.WorkSize, java.lang.Object...) }.
     */
    public static final class LocalMemPerElement {

        private int size;

        /**
         * Creates a new LocalMemPerElement instance
         * @param size the number of bytes available for each thread within
         * a work group
         */
        public LocalMemPerElement(int size) {
            super();
            this.size = size;
        }

        public int getSize() {
            return size;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + this.size;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LocalMemPerElement other = (LocalMemPerElement) obj;
            if (this.size != other.size) {
                return false;
            }
            return true;
        }

		@Override
		public String toString() {
			return "LocalMemPerElement (" + size + "B)";
		}
		
    }

    /**
     * The work size (global and local) for executing a kernel
     * @author shaman
     */
    public static final class WorkSize {

        private int dimension;
        private long[] sizes;

        /**
         * Creates a new work size object
         * @param dimension the dimension (1,2,3)
         * @param sizes the sizes in each dimension, the length must match the specified dimension
         */
        public WorkSize(int dimension, long... sizes) {
            super();
            set(dimension, sizes);
        }

        /**
         * Creates a work size of dimension 1 and extend 1,1,1 (only one thread).
         */
        public WorkSize() {
            this(1, 1, 1, 1);
        }

        /**
         * Creates a 1D work size of the specified extend
         * @param size the size
         */
        public WorkSize(long size) {
            this(1, size, 1, 1);
        }

        /**
         * Creates a 2D work size of the specified extend
         * @param width the width
         * @param height the height
         */
        public WorkSize(long width, long height) {
            this(2, width, height, 1);
        }

        /**
         * Creates a 3D work size of the specified extend.
         * @param width the width
         * @param height the height
         * @param depth the depth
         */
        public WorkSize(long width, long height, long depth) {
            this(3, width, height, depth);
        }

        public int getDimension() {
            return dimension;
        }

        public long[] getSizes() {
            return sizes;
        }

        public void set(int dimension, long... sizes) {
            if (sizes == null || sizes.length != 3) {
                throw new IllegalArgumentException("sizes must be an array of length 3");
            }
            if (dimension <= 0 || dimension > 3) {
                throw new IllegalArgumentException("dimension must be between 1 and 3");
            }
            this.dimension = dimension;
            this.sizes = sizes;
        }

        public void set(WorkSize ws) {
            this.dimension = ws.dimension;
            this.sizes = ws.sizes;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 47 * hash + this.dimension;
            hash = 47 * hash + Arrays.hashCode(this.sizes);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final WorkSize other = (WorkSize) obj;
            if (this.dimension != other.dimension) {
                return false;
            }
            if (!Arrays.equals(this.sizes, other.sizes)) {
                return false;
            }
            return true;
        }

		@Override
		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append("WorkSize[");
			for (int i=0; i<dimension; ++i) {
				if (i>0) {
					str.append(", ");
				}
				str.append(sizes[i]);
			}
			str.append(']');
			return str.toString();
		}
		
    }
    
}
