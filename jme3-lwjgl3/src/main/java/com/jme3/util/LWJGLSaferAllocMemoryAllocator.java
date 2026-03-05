package com.jme3.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.logging.Logger;

import org.lwjgl.system.MemoryUtil;

public final class LWJGLSaferAllocMemoryAllocator implements MemoryUtil.MemoryAllocator {
    private static final Logger logger = Logger.getLogger(LWJGLSaferAllocMemoryAllocator.class.getName());

    public static final String SAFER_BUFFER_ALLOCATOR_CLASS = "com.jme3.util.SaferBufferAllocator";

    private static final Bindings BINDINGS = Bindings.create();

    private final long fnMalloc;
    private final long fnCalloc;
    private final long fnRealloc;
    private final long fnFree;
    private final long fnAlignedAlloc;
    private final long fnAlignedFree;

    public static boolean isAvailable() {
        return BINDINGS != null;
    }

    public LWJGLSaferAllocMemoryAllocator() {
        if (BINDINGS == null) {
            throw new IllegalStateException(SAFER_BUFFER_ALLOCATOR_CLASS + " is not available on classpath.");
        }
        logger.info(getClass().getSimpleName() + " enabled!");
        this.fnMalloc = BINDINGS.getMallocFunctionPointer();
        this.fnCalloc = BINDINGS.getCallocFunctionPointer();
        this.fnRealloc = BINDINGS.getReallocFunctionPointer();
        this.fnFree = BINDINGS.getFreeFunctionPointer();
        this.fnAlignedAlloc = BINDINGS.getAlignedAllocFunctionPointer();
        this.fnAlignedFree = BINDINGS.getAlignedFreeFunctionPointer();
    }

    @Override
    public long getMalloc() {
        return fnMalloc;
    }

    @Override
    public long getCalloc() {
        return fnCalloc;
    }

    @Override
    public long getRealloc() {
        return fnRealloc;
    }

    @Override
    public long getFree() {
        return fnFree;
    }

    @Override
    public long getAlignedAlloc() {
        return fnAlignedAlloc;
    }

    @Override
    public long getAlignedFree() {
        return fnAlignedFree;
    }

    @Override
    public long malloc(long size) {
        return BINDINGS.malloc(size);
    }

    @Override
    public long calloc(long num, long size) {
        return BINDINGS.calloc(num, size);
    }

    @Override
    public long realloc(long ptr, long size) {
        return BINDINGS.realloc(ptr, size);
    }

    @Override
    public void free(long ptr) {
        BINDINGS.free(ptr);
    }

    @Override
    public long aligned_alloc(long alignment, long size) {
        return BINDINGS.alignedAlloc(alignment, size);
    }

    @Override
    public void aligned_free(long ptr) {
        BINDINGS.alignedFree(ptr);
    }

    private static final class Bindings {
        private final MethodHandle mallocFnPtr;
        private final MethodHandle callocFnPtr;
        private final MethodHandle reallocFnPtr;
        private final MethodHandle freeFnPtr;
        private final MethodHandle alignedAllocFnPtr;
        private final MethodHandle alignedFreeFnPtr;
        private final MethodHandle malloc;
        private final MethodHandle calloc;
        private final MethodHandle realloc;
        private final MethodHandle free;
        private final MethodHandle alignedAlloc;
        private final MethodHandle alignedFree;

        private Bindings(
                MethodHandle mallocFnPtr,
                MethodHandle callocFnPtr,
                MethodHandle reallocFnPtr,
                MethodHandle freeFnPtr,
                MethodHandle alignedAllocFnPtr,
                MethodHandle alignedFreeFnPtr,
                MethodHandle malloc,
                MethodHandle calloc,
                MethodHandle realloc,
                MethodHandle free,
                MethodHandle alignedAlloc,
                MethodHandle alignedFree) {
            this.mallocFnPtr = mallocFnPtr;
            this.callocFnPtr = callocFnPtr;
            this.reallocFnPtr = reallocFnPtr;
            this.freeFnPtr = freeFnPtr;
            this.alignedAllocFnPtr = alignedAllocFnPtr;
            this.alignedFreeFnPtr = alignedFreeFnPtr;
            this.malloc = malloc;
            this.calloc = calloc;
            this.realloc = realloc;
            this.free = free;
            this.alignedAlloc = alignedAlloc;
            this.alignedFree = alignedFree;
        }

        static Bindings create() {
            try {
                Class<?> clazz = Class.forName(SAFER_BUFFER_ALLOCATOR_CLASS, false,
                        LWJGLSaferAllocMemoryAllocator.class.getClassLoader());
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                return new Bindings(
                        lookup.findStatic(clazz, "getMallocFunctionPointer", MethodType.methodType(long.class)),
                        lookup.findStatic(clazz, "getCallocFunctionPointer", MethodType.methodType(long.class)),
                        lookup.findStatic(clazz, "getReallocFunctionPointer", MethodType.methodType(long.class)),
                        lookup.findStatic(clazz, "getFreeFunctionPointer", MethodType.methodType(long.class)),
                        lookup.findStatic(clazz, "getAlignedAllocFunctionPointer", MethodType.methodType(long.class)),
                        lookup.findStatic(clazz, "getAlignedFreeFunctionPointer", MethodType.methodType(long.class)),
                        lookup.findStatic(clazz, "malloc", MethodType.methodType(long.class, long.class)),
                        lookup.findStatic(clazz, "calloc", MethodType.methodType(long.class, long.class, long.class)),
                        lookup.findStatic(clazz, "realloc", MethodType.methodType(long.class, long.class, long.class)),
                        lookup.findStatic(clazz, "free", MethodType.methodType(void.class, long.class)),
                        lookup.findStatic(clazz, "alignedAlloc",
                                MethodType.methodType(long.class, long.class, long.class)),
                        lookup.findStatic(clazz, "alignedFree", MethodType.methodType(void.class, long.class)));
            } catch (Throwable ignored) {
                return null;
            }
        }

        long getMallocFunctionPointer() {
            try {
                return (long) mallocFnPtr.invokeExact();
            } catch (Throwable t) {
                throw unchecked(t);
            }
        }

        long getCallocFunctionPointer() {
            try {
                return (long) callocFnPtr.invokeExact();
            } catch (Throwable t) {
                throw unchecked(t);
            }
        }

        long getReallocFunctionPointer() {
            try {
                return (long) reallocFnPtr.invokeExact();
            } catch (Throwable t) {
                throw unchecked(t);
            }
        }

        long getFreeFunctionPointer() {
            try {
                return (long) freeFnPtr.invokeExact();
            } catch (Throwable t) {
                throw unchecked(t);
            }
        }

        long getAlignedAllocFunctionPointer() {
            try {
                return (long) alignedAllocFnPtr.invokeExact();
            } catch (Throwable t) {
                throw unchecked(t);
            }
        }

        long getAlignedFreeFunctionPointer() {
            try {
                return (long) alignedFreeFnPtr.invokeExact();
            } catch (Throwable t) {
                throw unchecked(t);
            }
        }

        long malloc(long size) {
            try {
                return (long) malloc.invokeExact(size);
            } catch (Throwable t) {
                throw unchecked(t);
            }
        }

        long calloc(long num, long size) {
            try {
                return (long) calloc.invokeExact(num, size);
            } catch (Throwable t) {
                throw unchecked(t);
            }
        }

        long realloc(long ptr, long size) {
            try {
                return (long) realloc.invokeExact(ptr, size);
            } catch (Throwable t) {
                throw unchecked(t);
            }
        }

        void free(long ptr) {
            try {
                free.invokeExact(ptr);
            } catch (Throwable t) {
                throw unchecked(t);
            }
        }

        long alignedAlloc(long alignment, long size) {
            try {
                return (long) alignedAlloc.invokeExact(alignment, size);
            } catch (Throwable t) {
                throw unchecked(t);
            }
        }

        void alignedFree(long ptr) {
            try {
                alignedFree.invokeExact(ptr);
            } catch (Throwable t) {
                throw unchecked(t);
            }
        }

        private RuntimeException unchecked(Throwable t) {
            if (t instanceof RuntimeException) {
                return (RuntimeException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            return new RuntimeException(t);
        }
    }
}
