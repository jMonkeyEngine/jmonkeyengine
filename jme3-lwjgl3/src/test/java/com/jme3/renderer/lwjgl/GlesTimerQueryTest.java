/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.renderer.lwjgl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import com.jme3.system.NativeLibraryLoader;
import java.lang.reflect.Constructor;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengles.EXTDisjointTimerQuery;
import org.lwjgl.opengles.GLES;
import org.lwjgl.opengles.GLES20;
import org.lwjgl.opengles.GLES30;
import org.lwjgl.opengles.GLESCapabilities;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.MemoryUtil;
import org.mockito.MockedStatic;

class GlesTimerQueryTest {
    private static String previousEglLibrary;
    private static String previousGlesLibrary;

    private static final Set<String> GLES_20_EXTENSIONS = Set.of(
            "GLES20", "GL_EXT_disjoint_timer_query");
    private static final Set<String> REQUIRED_QUERY_FUNCTIONS = Set.of(
            "glGenQueriesEXT",
            "glBeginQueryEXT",
            "glEndQueryEXT",
            "glGetQueryObjectuivEXT",
            "glGetQueryObjectui64vEXT",
            "glGetIntegerv");

    @BeforeAll
    static void initializeLwjglLibraryConfiguration() throws ClassNotFoundException {
        // LwjglWindow registers the ANGLE native libraries. Do this before loading them;
        // initializing GLES first can cache the platform's default library name.
        Class.forName("com.jme3.system.lwjgl.LwjglWindow", true,
                GlesTimerQueryTest.class.getClassLoader());
        previousEglLibrary = Configuration.EGL_LIBRARY_NAME.get();
        previousGlesLibrary = Configuration.OPENGLES_LIBRARY_NAME.get();
        String angleEgl = NativeLibraryLoader.loadNativeLibrary("angleEGL", true);
        String angleGles = NativeLibraryLoader.loadNativeLibrary("angleGLESv2", true);
        Configuration.EGL_LIBRARY_NAME.set(angleEgl);
        Configuration.OPENGLES_LIBRARY_NAME.set(angleGles);
    }

    @AfterAll
    static void restoreLwjglLibraryConfiguration() {
        Configuration.EGL_LIBRARY_NAME.set(previousEglLibrary);
        Configuration.OPENGLES_LIBRARY_NAME.set(previousGlesLibrary);
    }

    @Test
    void requiresTheExtensionAndEveryFunctionNeededByTimerQueries() throws Exception {
        try (CapabilitiesFixture fixture = new CapabilitiesFixture(
                Set.of("GLES20", "GLES30"), Set.of());
                MockedStatic<GLES> gles = mockCapabilities(fixture.capabilities)) {
            assertFalse(new LwjglGLES().supportsGpuTimerQuery());
        }

        for (String missingFunction : REQUIRED_QUERY_FUNCTIONS) {
            try (CapabilitiesFixture fixture = new CapabilitiesFixture(
                    GLES_20_EXTENSIONS, Set.of(missingFunction));
                    MockedStatic<GLES> gles = mockCapabilities(fixture.capabilities)) {
                assertFalse(new LwjglGLES().supportsGpuTimerQuery(),
                        "timer support must be false without " + missingFunction);
            }
        }
    }

    @Test
    void usesTheExtensionQueryFamilyWhenCoreEntryPointsAreMissing() throws Exception {
        try (CapabilitiesFixture fixture = new CapabilitiesFixture(
                GLES_20_EXTENSIONS, coreQueryFunctions());
                MockedStatic<GLES> gles = mockCapabilities(fixture.capabilities);
                MockedStatic<GLES30> core = mockStatic(GLES30.class);
                MockedStatic<EXTDisjointTimerQuery> extension = mockStatic(EXTDisjointTimerQuery.class)) {
            LwjglGLES gl = new LwjglGLES();
            assertTrue(gl.supportsGpuTimerQuery());

            extension.when(() -> EXTDisjointTimerQuery.glGenQueriesEXT(any(IntBuffer.class)))
                    .thenAnswer(invocation -> {
                        IntBuffer ids = invocation.getArgument(0);
                        assertEquals(1, ids.position());
                        assertEquals(1, ids.remaining());
                        ids.put(ids.position(), 37);
                        return null;
                    });
            IntBuffer ids = IntBuffer.allocate(4);
            ids.position(1);
            gl.glGenQueries(1, ids);
            assertEquals(37, ids.get(1));
            assertEquals(1, ids.position());
            assertEquals(4, ids.limit());

            gl.glBeginQuery(EXTDisjointTimerQuery.GL_TIME_ELAPSED_EXT, 12);
            gl.glEndQuery(EXTDisjointTimerQuery.GL_TIME_ELAPSED_EXT);
            extension.when(() -> EXTDisjointTimerQuery.glGetQueryObjectuivEXT(
                    eq(12), eq(EXTDisjointTimerQuery.GL_QUERY_RESULT_AVAILABLE_EXT),
                    any(IntBuffer.class))).thenAnswer(invocation -> {
                        IntBuffer result = invocation.getArgument(2);
                        result.put(0, 1);
                        return null;
                    });
            assertEquals(1, gl.glGetQueryObjectiv(
                    12, EXTDisjointTimerQuery.GL_QUERY_RESULT_AVAILABLE_EXT));

            long rawResultBits = 0xFEDCBA9876543210L;
            extension.when(() -> EXTDisjointTimerQuery.glGetQueryObjectui64EXT(
                    12, EXTDisjointTimerQuery.GL_QUERY_RESULT_EXT))
                    .thenReturn(rawResultBits);
            assertEquals(rawResultBits, gl.glGetQueryObjectui64(
                    12, EXTDisjointTimerQuery.GL_QUERY_RESULT_EXT));

            extension.verify(() -> EXTDisjointTimerQuery.glBeginQueryEXT(
                    EXTDisjointTimerQuery.GL_TIME_ELAPSED_EXT, 12));
            extension.verify(() -> EXTDisjointTimerQuery.glEndQueryEXT(
                    EXTDisjointTimerQuery.GL_TIME_ELAPSED_EXT));
            core.verifyNoInteractions();
        }
    }

    @Test
    void restoresTheCallerBufferLimitAfterUnsupportedAndFailedGeneration() throws Exception {
        try (CapabilitiesFixture fixture = new CapabilitiesFixture(
                GLES_20_EXTENSIONS, Set.of("glGenQueries", "glGenQueriesEXT"));
                MockedStatic<GLES> gles = mockCapabilities(fixture.capabilities);
                MockedStatic<GLES30> core = mockStatic(GLES30.class);
                MockedStatic<EXTDisjointTimerQuery> extension = mockStatic(EXTDisjointTimerQuery.class)) {
            IntBuffer ids = IntBuffer.allocate(5);
            ids.position(1);
            assertThrows(UnsupportedOperationException.class, () -> new LwjglGLES().glGenQueries(2, ids));
            assertEquals(1, ids.position());
            assertEquals(5, ids.limit());
        }

        try (CapabilitiesFixture fixture = new CapabilitiesFixture(
                GLES_20_EXTENSIONS, coreQueryFunctions());
                MockedStatic<GLES> gles = mockCapabilities(fixture.capabilities);
                MockedStatic<EXTDisjointTimerQuery> extension = mockStatic(EXTDisjointTimerQuery.class)) {
            extension.when(() -> EXTDisjointTimerQuery.glGenQueriesEXT(any(IntBuffer.class)))
                    .thenThrow(new IllegalStateException("driver failure"));
            IntBuffer ids = IntBuffer.allocate(5);
            ids.position(1);
            assertThrows(IllegalStateException.class, () -> new LwjglGLES().glGenQueries(2, ids));
            assertEquals(1, ids.position());
            assertEquals(5, ids.limit());
        }
    }

    @Test
    void forwardsTheDisjointStateEnumThroughTheIntegerQuery() throws Exception {
        try (CapabilitiesFixture fixture = new CapabilitiesFixture(
                GLES_20_EXTENSIONS, coreQueryFunctions());
                MockedStatic<GLES> gles = mockCapabilities(fixture.capabilities);
                MockedStatic<GLES20> gles20 = mockStatic(GLES20.class)) {
            gles20.when(() -> GLES20.glGetInteger(
                    EXTDisjointTimerQuery.GL_GPU_DISJOINT_EXT)).thenReturn(1);
            assertTrue(new LwjglGLES().isGpuTimingDisjoint());
            gles20.verify(() -> GLES20.glGetInteger(
                    EXTDisjointTimerQuery.GL_GPU_DISJOINT_EXT));
        }
    }

    private static MockedStatic<GLES> mockCapabilities(GLESCapabilities capabilities) {
        MockedStatic<GLES> mock = mockStatic(GLES.class);
        mock.when(GLES::getCapabilities).thenReturn(capabilities);
        return mock;
    }

    private static Set<String> coreQueryFunctions() {
        return Set.of("glGenQueries", "glBeginQuery", "glEndQuery", "glGetQueryObjectuiv");
    }

    private static final class CapabilitiesFixture implements AutoCloseable {
        private static final long FAKE_FUNCTION_ADDRESS = 0x1000L;

        private final List<PointerBuffer> allocations = new ArrayList<>();
        private final GLESCapabilities capabilities;

        private CapabilitiesFixture(Set<String> extensions, Set<String> missingFunctions)
                throws Exception {
            FunctionProvider provider = functionName -> {
                String name = MemoryUtil.memASCII(MemoryUtil.memAddress(functionName));
                return missingFunctions.contains(name) ? MemoryUtil.NULL : FAKE_FUNCTION_ADDRESS;
            };
            IntFunction<PointerBuffer> allocator = size -> {
                PointerBuffer pointers = MemoryUtil.memCallocPointer(size);
                allocations.add(pointers);
                return pointers;
            };
            Constructor<GLESCapabilities> constructor = GLESCapabilities.class.getDeclaredConstructor(
                    FunctionProvider.class, Set.class, IntFunction.class);
            constructor.setAccessible(true);
            capabilities = constructor.newInstance(provider, extensions, allocator);
        }

        @Override
        public void close() {
            for (PointerBuffer allocation : allocations) {
                MemoryUtil.memFree(allocation);
            }
        }

    }
}
