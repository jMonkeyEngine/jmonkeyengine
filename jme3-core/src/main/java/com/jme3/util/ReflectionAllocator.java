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
package com.jme3.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class contains the reflection based way to remove DirectByteBuffers in
 * java, allocation is done via ByteBuffer.allocateDirect
 */
public final class ReflectionAllocator implements BufferAllocator {
    private static Method cleanerMethod = null;
    private static Method cleanMethod = null;
    private static Method viewedBufferMethod = null;
    private static Method freeMethod = null;

    static {
        // Oracle JRE / OpenJDK
        cleanerMethod = loadMethod("sun.nio.ch.DirectBuffer", "cleaner");
        cleanMethod = loadMethod("sun.misc.Cleaner", "clean");
        viewedBufferMethod = loadMethod("sun.nio.ch.DirectBuffer", "viewedBuffer");
        if (viewedBufferMethod == null) {
            // They changed the name in Java 7
            viewedBufferMethod = loadMethod("sun.nio.ch.DirectBuffer", "attachment");
        }

        // Apache Harmony (allocated directly, to not trigger allocator used
        // logic in BufferUtils)
        ByteBuffer bb = ByteBuffer.allocateDirect(1);
        Class<?> clazz = bb.getClass();
        try {
            freeMethod = clazz.getMethod("free");
        } catch (NoSuchMethodException ex) {
        } catch (SecurityException ex) {
        }
    }

    private static Method loadMethod(String className, String methodName) {
        try {
            Method method = Class.forName(className).getMethod(methodName);
            method.setAccessible(true);// according to the Java documentation, by default, a reflected object is not accessible
            return method;
        } catch (NoSuchMethodException ex) {
            return null; // the method was not found
        } catch (SecurityException ex) {
            return null; // setAccessible not allowed by security policy
        } catch (ClassNotFoundException ex) {
            return null; // the direct buffer implementation was not found
        } catch (Throwable t) {
        	if (t.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
        		return null;// the class is in an unexported module
        	} else {
        		throw t;
        	}
        }
    }

    @Override
    /**
     * This function explicitly calls the Cleaner method of a direct buffer.
     * 
     * @param toBeDestroyed
     *            The direct buffer that will be "cleaned". Utilizes reflection.
     * 
     */
    public void destroyDirectBuffer(Buffer toBeDestroyed) {
        try {
            if (freeMethod != null) {
                freeMethod.invoke(toBeDestroyed);
            } else {
            	//TODO load the methods only once, store them into a cache (only for Java >= 9)
            	Method localCleanerMethod;
            	if (cleanerMethod == null) {
            		localCleanerMethod = loadMethod(toBeDestroyed.getClass().getName(), "cleaner");
            	} else {
            		localCleanerMethod = cleanerMethod;
            	}
				if (localCleanerMethod == null) {
					Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE,
							"Buffer cannot be destroyed: {0}", toBeDestroyed);
				} else {
					Object cleaner = localCleanerMethod.invoke(toBeDestroyed);
					if (cleaner != null) {
						Method localCleanMethod;
						if (cleanMethod == null) {
							if (cleaner instanceof Runnable) {
								// jdk.internal.ref.Cleaner implements Runnable in Java 9
								localCleanMethod = loadMethod(Runnable.class.getName(), "run");
							} else {
								// sun.misc.Cleaner does not implement Runnable in Java < 9
								localCleanMethod = loadMethod(cleaner.getClass().getName(), "clean");
							}
						} else {
							localCleanMethod = cleanMethod;
						}
						if (localCleanMethod == null) {
							Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE,
									"Buffer cannot be destroyed: {0}", toBeDestroyed);
						} else {
							localCleanMethod.invoke(cleaner);
						}
					} else {
						Method localViewedBufferMethod;
						if (viewedBufferMethod == null) {
							localViewedBufferMethod = loadMethod(toBeDestroyed.getClass().getName(), "viewedBuffer");
						} else {
							localViewedBufferMethod = viewedBufferMethod;
						}
						if (localViewedBufferMethod == null) {
							Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE,
									"Buffer cannot be destroyed: {0}", toBeDestroyed);
						} else {
							// Try the alternate approach of getting the viewed
							// buffer
							// first
							Object viewedBuffer = localViewedBufferMethod.invoke(toBeDestroyed);
							if (viewedBuffer != null) {
								destroyDirectBuffer((Buffer) viewedBuffer);
							} else {
								Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE,
										"Buffer cannot be destroyed: {0}", toBeDestroyed);
							}
						}
					}
				}
            }
        } catch (IllegalAccessException ex) {
            Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE, "{0}", ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE, "{0}", ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE, "{0}", ex);
        } catch (SecurityException ex) {
            Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE, "{0}", ex);
        }
    }

    @Override
    public ByteBuffer allocate(int size) {
        return ByteBuffer.allocateDirect(size);
    }

}
