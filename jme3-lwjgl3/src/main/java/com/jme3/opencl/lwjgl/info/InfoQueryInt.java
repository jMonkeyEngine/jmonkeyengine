/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
package com.jme3.opencl.lwjgl.info;

import com.jme3.lwjgl3.utils.APIBuffer;
import static com.jme3.lwjgl3.utils.APIUtil.apiBuffer;
import static com.jme3.opencl.lwjgl.info.CLUtil.checkCLError;
import org.lwjgl.PointerBuffer;

import static org.lwjgl.system.Checks.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.Pointer.*;

/**
 * Base class for OpenCL object information queries.
 * <p>
 * All methods require the object being queried (a pointer value), a second
 * integer argument and the integer parameter name.
 *
 * @see Info
 */
abstract class InfoQueryInt {

    protected abstract int get(long pointer, int arg, int parameterName, long parameterValueSize, long parameterValue, long parameterValueSizeRet);

    InfoQueryInt() {
    }

    /**
     * Returns the integer value for the specified {@code parameterName}, converted
     * to a boolean.
     *
     * @param object the object to query
     * @param arg an integer argument
     * @param parameterName the parameter to query
     *
     * @return the parameter's boolean value
     */
    boolean getBoolean(long object, int arg, int parameterName) {
        return getInt(object, arg, parameterName) != 0;
    }

    /**
     * Returns the integer value for the specified {@code parameterName}.
     * <p>
     * For integer parameters that may be 32 or 64 bits (e.g. {@code size_t}),
     * {@link #getPointer} should be used instead.
     *
     * @param object the object to query
     * @param arg an integer argument
     * @param parameterName the parameter to query
     *
     * @return the parameter's int value
     */
    int getInt(long object, int arg, int parameterName) {
        APIBuffer __buffer = apiBuffer();
        int errcode = get(object, arg, parameterName, 4L, __buffer.address(), NULL);
        if (DEBUG) {
            checkCLError(errcode);
        }
        return __buffer.intValue(0);
    }

    /**
     * Returns the long value for the specified {@code parameterName}.
     * <p>
     * For integer parameters that may be 32 or 64 bits (e.g. {@code size_t}),
     * {@link #getPointer} should be used instead.
     *
     * @param object the object to query
     * @param arg an integer argument
     * @param parameterName the parameter to query
     *
     * @return the parameter's long value
     */
    long getLong(long object, int arg, int parameterName) {
        APIBuffer __buffer = apiBuffer();
        int errcode = get(object, arg, parameterName, 8L, __buffer.address(), NULL);
        if (DEBUG) {
            checkCLError(errcode);
        }
        return __buffer.longValue(0);
    }

    /**
     * Returns the pointer value for the specified {@code parameterName}.
     * <p>
     * This method should also be used for integer parameters that may be 32 or
     * 64 bits (e.g. {@code size_t}).
     *
     * @param object the object to query
     * @param arg an integer argument
     * @param parameterName the parameter to query
     *
     * @return the parameter's pointer value
     */
    long getPointer(long object, int arg, int parameterName) {
        APIBuffer __buffer = apiBuffer();
        int errcode = get(object, arg, parameterName, POINTER_SIZE, __buffer.address(), NULL);
        if (DEBUG) {
            checkCLError(errcode);
        }
        return __buffer.pointerValue(0);
    }

    /**
     * Writes the pointer list for the specified {@code parameterName} into
     * {@code target}.
     * <p>
     * This method should also be used for integer parameters that may be 32 or
     * 64 bits (e.g. {@code size_t}).
     *
     * @param object the object to query
     * @param arg an integer argument
     * @param parameterName the parameter to query
     * @param target the buffer in which to put the returned pointer list
     *
     * @return how many pointers were actually returned
     */
    int getPointers(long object, int arg, int parameterName, PointerBuffer target) {
        APIBuffer __buffer = apiBuffer();
        int errcode = get(object, arg, parameterName, target.remaining() * POINTER_SIZE, memAddress(target), __buffer.address());
        if (DEBUG) {
            checkCLError(errcode);
        }
        return (int) (__buffer.pointerValue(0) >> POINTER_SHIFT);
    }

    /**
     * Returns the string value for the specified {@code parameterName}. The raw
     * bytes returned are assumed to be ASCII encoded.
     *
     * @param object the object to query
     * @param arg an integer argument
     * @param parameterName the parameter to query
     *
     * @return the parameter's string value
     */
    String getStringASCII(long object, int arg, int parameterName) {
        APIBuffer __buffer = apiBuffer();
        int bytes = getString(object, arg, parameterName, __buffer);
        return __buffer.stringValueASCII(0, bytes);
    }

    /**
     * Returns the string value for the specified {@code parameterName}. The raw
     * bytes returned are assumed to be ASCII encoded and have length equal to {@code
     * parameterValueSize}.
     *
     * @param object the object to query
     * @param arg an integer argument
     * @param parameterName the parameter to query
     * @param parameterValueSize the explicit string length
     *
     * @return the parameter's string value
     */
    String getStringASCII(long object, int arg, int parameterName, int parameterValueSize) {
        APIBuffer __buffer = apiBuffer();
        int errcode = get(object, arg, parameterName, parameterValueSize, __buffer.address(), NULL);
        if (DEBUG) {
            checkCLError(errcode);
        }
        return __buffer.stringValueASCII(0, parameterValueSize);
    }

    /**
     * Returns the string value for the specified {@code parameterName}. The raw
     * bytes returned are assumed to be UTF-8 encoded.
     *
     * @param object the object to query
     * @param arg an integer argument
     * @param parameterName the parameter to query
     *
     * @return the parameter's string value
     */
    String getStringUTF8(long object, int arg, int parameterName) {
        APIBuffer __buffer = apiBuffer();
        int bytes = getString(object, arg, parameterName, __buffer);
        return __buffer.stringValueUTF8(0, bytes);
    }

    /**
     * Returns the string value for the specified {@code parameterName}. The raw
     * bytes returned are assumed to be UTF-8 encoded and have length equal to {@code
     * parameterValueSize}.
     *
     * @param object the object to query
     * @param arg an integer argument
     * @param parameterName the parameter to query
     * @param parameterValueSize the explicit string length
     *
     * @return the parameter's string value
     */
    String getStringUTF8(long object, int arg, int parameterName, int parameterValueSize) {
        APIBuffer __buffer = apiBuffer();
        int errcode = get(object, arg, parameterName, parameterValueSize, __buffer.address(), NULL);
        if (DEBUG) {
            checkCLError(errcode);
        }
        return __buffer.stringValueUTF8(0, parameterValueSize);
    }

    private int getString(long object, int arg, int parameterName, APIBuffer buffer) {
        // Get string length
        int errcode = get(object, arg, parameterName, 0, NULL, buffer.address());
        if (DEBUG) {
            checkCLError(errcode);
        }

        int bytes = (int) buffer.pointerValue(0);
        buffer.bufferParam(bytes + POINTER_SIZE);

        // Get string
        errcode = get(object, arg, parameterName, bytes, buffer.address(), NULL);
        if (DEBUG) {
            checkCLError(errcode);
        }

        return bytes - 1; // all OpenCL char[] parameters are null-terminated
    }

}
