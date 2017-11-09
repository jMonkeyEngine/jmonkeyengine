/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
package com.jme3.opencl.lwjgl.info;

import static com.jme3.lwjgl3.utils.APIUtil.apiOptionalClass;
import com.jme3.opencl.OpenCLException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import org.lwjgl.opencl.CL10;

import static org.lwjgl.opencl.CL10.*;
import org.lwjgl.opencl.CL11;
import org.lwjgl.opencl.CL12;
import org.lwjgl.opencl.CL20;
import org.lwjgl.opencl.INTELAccelerator;
import org.lwjgl.opencl.KHRICD;
import static org.lwjgl.system.APIUtil.*;

public final class CLUtil {

    /**
     * Maps OpenCL error token values to their String representations.
     */
    private static final Map<Integer, String> CL_ERROR_TOKENS = apiClassTokens(
            new BiPredicate<Field, Integer>() {
        private final List<String> EXCLUDE = Arrays.asList("CL_DEVICE_TYPE_ALL", "CL_BUILD_NONE", "CL_BUILD_ERROR", "CL_BUILD_IN_PROGRESS");

        @Override
        public boolean test(Field field, Integer value) {
            return value < 0 && !EXCLUDE.contains(field.getName()); // OpenCL errors have negative values.
        }
    },
            null,
            CL10.class,
            apiOptionalClass("org.lwjgl.opencl.CL10GL"),
            CL11.class,
            CL12.class,
            CL20.class,
            apiOptionalClass("org.lwjgl.opencl.APPLEGLSharing"),
            INTELAccelerator.class,
            apiOptionalClass("org.lwjgl.opencl.KHRGLSharing"),
            apiOptionalClass("org.lwjgl.opencl.KHREGLEvent"),
            apiOptionalClass("org.lwjgl.opencl.KHREGLImage"),
            KHRICD.class
    /*, EXTDeviceFission.class*/
    );

    private CLUtil() {
    }

    /**
     * Checks the {@code errcode} present in the current position of the
     * specified {@code errcode_ret} buffer and throws an
     * {@link OpenCLException} if it's not equal to {@link CL10#CL_SUCCESS}.
     *
     * @param errcode_ret the {@code errcode} buffer
     *
     * @throws OpenCLException
     */
    public static void checkCLError(ByteBuffer errcode_ret) {
        checkCLError(errcode_ret.getInt(errcode_ret.position()));
    }

    /**
     * Checks the {@code errcode} present in the current position of the
     * specified {@code errcode_ret} buffer and throws an
     * {@link OpenCLException} if it's not equal to {@link CL10#CL_SUCCESS}.
     *
     * @param errcode_ret the {@code errcode} buffer
     *
     * @throws OpenCLException
     */
    public static void checkCLError(IntBuffer errcode_ret) {
        checkCLError(errcode_ret.get(errcode_ret.position()));
    }

    /**
     * Checks the specified {@code errcode} and throws an
     * {@link OpenCLException} if it's not equal to {@link CL10#CL_SUCCESS}.
     *
     * @param errcode the {@code errcode} to check
     *
     * @throws OpenCLException
     */
    public static void checkCLError(int errcode) {
        if (errcode != CL_SUCCESS) {
            throw new OpenCLException(getErrcodeName(errcode));
        }
    }

    /**
     * Returns the token name of the specified {@code errcode}.
     *
     * @param errcode the {@code errcode}
     *
     * @return the {@code errcode} token name
     */
    public static String getErrcodeName(int errcode) {
        String errname = CL_ERROR_TOKENS.get(errcode);
        if (errname == null) {
            errname = apiUnknownToken(errcode);
        }

        return errname;
    }

}
