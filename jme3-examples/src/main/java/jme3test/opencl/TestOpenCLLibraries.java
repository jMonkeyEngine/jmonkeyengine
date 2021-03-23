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

package jme3test.opencl;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.opencl.*;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import java.nio.*;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test class for the build in libraries
 * @author shaman
 */
public class TestOpenCLLibraries extends SimpleApplication {
    private static final Logger LOG = Logger.getLogger(TestOpenCLLibraries.class.getName());

    public static void main(String[] args){
        TestOpenCLLibraries app = new TestOpenCLLibraries();
        AppSettings settings = new AppSettings(true);
        settings.setOpenCLSupport(true);
        settings.setVSync(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL2);
        app.setSettings(settings);
        app.start(); // start the game
    }

    @Override
    public void simpleInitApp() {
        BitmapFont fnt = assetManager.loadFont("Interface/Fonts/Default.fnt");
        Context clContext = context.getOpenCLContext();
        if (clContext == null) {
            BitmapText txt = new BitmapText(fnt);
            txt.setText("No OpenCL Context created!\nSee output log for details.");
            txt.setLocalTranslation(5, settings.getHeight() - 5, 0);
            guiNode.attachChild(txt);
            return;
        }
        CommandQueue clQueue = clContext.createQueue(clContext.getDevices().get(0));
        
        StringBuilder str = new StringBuilder();
        str.append("OpenCL Context created:\n  Platform: ")
                .append(clContext.getDevices().get(0).getPlatform().getName())
                .append("\n  Devices: ").append(clContext.getDevices());
        str.append("\nTests:");
        str.append("\n  Random numbers: ").append(testRandom(clContext, clQueue));
        str.append("\n  Matrix3f: ").append(testMatrix3f(clContext, clQueue));
        str.append("\n  Matrix4f: ").append(testMatrix4f(clContext, clQueue));
        
        clQueue.release();
        
        BitmapText txt1 = new BitmapText(fnt);
        txt1.setText(str.toString());
        txt1.setLocalTranslation(5, settings.getHeight() - 5, 0);
        guiNode.attachChild(txt1);
        
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);
    }
    
    private static void assertEquals(byte expected, byte actual, String message) {
        if (expected != actual) {
            System.err.println(message+": expected="+expected+", actual="+actual);
            throw new AssertionError();
        }
    }
    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            System.err.println(message+": expected="+expected+", actual="+actual);
            throw new AssertionError();
        }
    }
    private static void assertEquals(double expected, double actual, String message) {
        if (Math.abs(expected - actual) >= 0.00001) {
            System.err.println(message+": expected="+expected+", actual="+actual);
            throw new AssertionError();
        }
    }
    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            System.err.println(message+": expected="+expected+", actual="+actual);
            throw new AssertionError();
        }
    }

    private boolean testRandom(Context clContext, CommandQueue clQueue) {
        try {
            //test for doubles
            boolean supportsDoubles = clContext.getDevices().get(0).hasDouble();
            
            //create code
            String code = ""
                    + "#import \"Common/OpenCL/Random.clh\"\n"
                    + "__kernel void TestBool(__global ulong* seeds, __global uchar* results) {\n"
                    + "  results[get_global_id(0)] = randBool(seeds + get_global_id(0)) ? 1 : 0;\n"
                    + "}\n"
                    + "__kernel void TestInt(__global ulong* seeds, __global int* results) {\n"
                    + "  results[get_global_id(0)] = randInt(seeds + get_global_id(0));\n"
                    + "}\n"
                    + "__kernel void TestIntN(__global ulong* seeds, int n, __global int* results) {\n"
                    + "  results[get_global_id(0)] = randIntN(n, seeds + get_global_id(0));\n"
                    + "}\n"
                    + "__kernel void TestLong(__global ulong* seeds, __global long* results) {\n"
                    + "  results[get_global_id(0)] = randLong(seeds + get_global_id(0));\n"
                    + "}\n"
                    + "__kernel void TestFloat(__global ulong* seeds, __global float* results) {\n"
                    + "  results[get_global_id(0)] = randFloat(seeds + get_global_id(0));\n"
                    + "}\n"
                    + "#ifdef RANDOM_DOUBLES\n"
                    + "__kernel void TestDouble(__global ulong* seeds, __global double* results) {\n"
                    + "  results[get_global_id(0)] = randDouble(seeds + get_global_id(0));\n"
                    + "}\n"
                    + "#endif\n";
            if (supportsDoubles) {
                code = "#define RANDOM_DOUBLES\n" + code;
            }
            Program program = clContext.createProgramFromSourceCodeWithDependencies(code, assetManager);
            program.build();
            
            int count = 256;
            Kernel.WorkSize ws = new Kernel.WorkSize(count);
            
            //create seeds
            Random initRandom = new Random();
            long[] seeds = new long[count];
            Random[] randoms = new Random[count];
            for (int i=0; i<count; ++i) {
                seeds[i] = initRandom.nextLong();
                randoms[i] = new Random(seeds[i]);
                seeds[i] = (seeds[i] ^ 0x5DEECE66DL) & ((1L << 48) - 1); //needed because the Random constructor scrambles the initial seed
            }
            com.jme3.opencl.Buffer seedsBuffer = clContext.createBuffer(8 * count);
            ByteBuffer tmpByteBuffer = BufferUtils.createByteBuffer(8 * count);
            tmpByteBuffer.asLongBuffer().put(seeds);
            seedsBuffer.write(clQueue, tmpByteBuffer);
            
            //test it
            ByteBuffer resultByteBuffer = BufferUtils.createByteBuffer(8 * count);
            IntBuffer resultIntBuffer = resultByteBuffer.asIntBuffer();
            LongBuffer resultLongBuffer = resultByteBuffer.asLongBuffer();
            FloatBuffer resultFloatBuffer = resultByteBuffer.asFloatBuffer();
            DoubleBuffer resultDoubleBuffer = resultByteBuffer.asDoubleBuffer();
            com.jme3.opencl.Buffer resultBuffer = clContext.createBuffer(8 * count);
            //boolean
            Kernel testBoolKernel = program.createKernel("TestBool");
            testBoolKernel.Run1NoEvent(clQueue, ws, seedsBuffer, resultBuffer);
            resultByteBuffer.rewind();
            resultBuffer.read(clQueue, resultByteBuffer);
            for (int i=0; i<count; ++i) {
                assertEquals(randoms[i].nextBoolean() ? 1 : 0, resultByteBuffer.get(i), "randBool at i="+i);
            }
            testBoolKernel.release();
            //int
            Kernel testIntKernel = program.createKernel("TestInt");
            testIntKernel.Run1NoEvent(clQueue, ws, seedsBuffer, resultBuffer);
            resultByteBuffer.rewind();
            resultBuffer.read(clQueue, resultByteBuffer);
            for (int i=0; i<count; ++i) {
                assertEquals(randoms[i].nextInt(), resultIntBuffer.get(i), "randInt at i="+i);
            }
            testIntKernel.release();
            //int n
            Kernel testIntNKernel = program.createKernel("TestIntN");
            testIntNKernel.Run1NoEvent(clQueue, ws, seedsBuffer, 186, resultBuffer);
            resultByteBuffer.rewind();
            resultBuffer.read(clQueue, resultByteBuffer);
            for (int i=0; i<count; ++i) {
                assertEquals(randoms[i].nextInt(186), resultIntBuffer.get(i), "randInt at i="+i+" with n="+186);
            }
            testIntNKernel.Run1NoEvent(clQueue, ws, seedsBuffer, 97357, resultBuffer);
            resultByteBuffer.rewind();
            resultBuffer.read(clQueue, resultByteBuffer);
            for (int i=0; i<count; ++i) {
                assertEquals(randoms[i].nextInt(97357), resultIntBuffer.get(i), "randInt at i="+i+" with n="+97357);
            }
            testIntNKernel.release();
            //long
            Kernel testLongKernel = program.createKernel("TestLong");
            testLongKernel.Run1NoEvent(clQueue, ws, seedsBuffer, resultBuffer);
            resultByteBuffer.rewind();
            resultBuffer.read(clQueue, resultByteBuffer);
            for (int i=0; i<count; ++i) {
                assertEquals(randoms[i].nextLong(), resultLongBuffer.get(i), "randLong at i="+i);
            }
            testLongKernel.release();
            //float
            Kernel testFloatKernel = program.createKernel("TestFloat");
            testFloatKernel.Run1NoEvent(clQueue, ws, seedsBuffer, resultBuffer);
            resultByteBuffer.rewind();
            resultBuffer.read(clQueue, resultByteBuffer);
            for (int i=0; i<count; ++i) {
                assertEquals(randoms[i].nextFloat(), resultFloatBuffer.get(i), "randFloat at i="+i);
            }
            testFloatKernel.release();
            //double
            if (supportsDoubles) {
                Kernel testDoubleKernel = program.createKernel("TestDouble");
                testDoubleKernel.Run1NoEvent(clQueue, ws, seedsBuffer, resultBuffer);
                resultByteBuffer.rewind();
                resultBuffer.read(clQueue, resultByteBuffer);
                for (int i=0; i<count; ++i) {
                    assertEquals(randoms[i].nextDouble(), resultDoubleBuffer.get(i), "randLong at i="+i);
                }
                testDoubleKernel.release();
            }
            
            seedsBuffer.release();
            resultBuffer.release();
            program.release();

        } catch (AssertionError ex) {
            LOG.log(Level.SEVERE, "random test failed with an assertion error");
            return false;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "random test failed with:", ex);
            return false;
        }
        return true;
    }
    
    private boolean testMatrix3f(Context clContext, CommandQueue clQueue) {
        try {
            
            String code = ""
                    + "#import \"Common/OpenCL/Matrix3f.clh\"\n"
                    + "\n"
                    + "__kernel void TestMatrix3f_1(__global char* result)\n"
                    + "{\n"
                    + "  mat3 id = mat3Identity();\n"
                    + "  mat3 m1 = mat3FromRows( (float3)(23,-3,10.4f), (float3)(5,-8,2.22f), (float3)(-1,0,34) );\n"
                    + "  mat3 m1Inv = mat3Invert(m1);\n"
                    + "  mat3 m1Res = mat3Mult(m1, m1Inv);\n"
                    + "  result[0] = mat3Equals(id, m1Res, 0.0001f) ? 1 : 0;\n"
                    + "}\n"
                    + "\n"
                    + "__kernel void TestMatrix3f_2(mat3 m1, float a, mat3 m2, mat3 mRes, __global char* result)\n"
                    + "{\n"
                    + "  mat3 m = mat3Transpose(m1);\n"
                    + "  m = mat3Add(mat3Scale(m, a), m2);\n"
                    + "  result[0] = mat3Equals(mRes, m, 0.01f) ? 1 : 0;\n"
                    + "}\n";
            Program program = clContext.createProgramFromSourceCodeWithDependencies(code, assetManager);
            program.build();
            com.jme3.opencl.Buffer buffer = clContext.createBuffer(1);
            
            Kernel testMatrix3fKernel1 = program.createKernel("TestMatrix3f_1");
            testMatrix3fKernel1.Run1NoEvent(clQueue, new Kernel.WorkSize(1), buffer);
            ByteBuffer bb = buffer.map(clQueue, MappingAccess.MAP_READ_ONLY);
            if (bb.get() == 0) {
                LOG.severe("Matrix inversion failed");
                return false;
            }
            buffer.unmap(clQueue, bb);
            testMatrix3fKernel1.release();
            
            Kernel testMatrix3fKernel2 = program.createKernel("TestMatrix3f_2");
            Matrix3f m1 = new Matrix3f(13.24f, -0.234f, 42, 83.23f, -34.2f, 3.2f, 0.25f, -42, 7.64f);
            Matrix3f m2 = new Matrix3f(-5.2f, 0.757f, 2.01f, 12.0f, -6, 2, 0.01f, 9, 2.255f);
            Matrix3f mRes = new Matrix3f(-31.68f, -165.703f, 1.51f, 12.468f, 62.4f, 86, -83.99f, 2.6f, -13.025f);
            testMatrix3fKernel2.Run1NoEvent(clQueue, new Kernel.WorkSize(1), m1, -2.0f, m2, mRes, buffer);
            bb = buffer.map(clQueue, MappingAccess.MAP_READ_ONLY);
            if (bb.get() == 0) {
                LOG.severe("Matrix add, multiply, transpose failed");
                return false;
            }
            buffer.unmap(clQueue, bb);
            testMatrix3fKernel2.release();
            
            buffer.release();
            
        } catch (AssertionError ex) {
            LOG.log(Level.SEVERE, "matrix3f test failed with an assertion error");
            return false;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "matrix3f test failed with:", ex);
            return false;
        }
        return true;
    }
    
    private boolean testMatrix4f(Context clContext, CommandQueue clQueue) {
        try {
            
            String code = ""
                    + "#import \"Common/OpenCL/Matrix4f.clh\"\n"
                    + "\n"
                    + "__kernel void TestMatrix4f_1(mat4 m1, __global char* result)\n"
                    + "{\n"
                    + "  mat4 id = mat4Identity();\n"
                    + "  mat4 m1Inv = mat4Invert(m1);\n"
                    + "  mat4 m1Res = mat4Mult(m1, m1Inv);\n"
                    + "  result[0] = mat4Equals(id, m1Res, 0.0001f) ? 1 : 0;\n"
                    + "}\n"
                    + "\n"
                    + "__kernel void TestMatrix4f_2(mat4 m1, float d, mat4 m2, mat4 m3, __global char* result)\n"
                    + "{\n"
                    + "  float d2 = mat4Determinant(m1);\n"
                    + "  result[0] = fabs(d - d2) < 0.0001f ? 1 : 0;\n"
                    + "  mat4 res = mat4Transpose(m1);\n"
                    + "  result[1] = mat4Equals(res, m2, 0.0001f) ? 1 : 0;\n"
                    + "  res = mat4Adjoint(m1);\n"
                    + "  result[2] = mat4Equals(res, m3, 0.0001f) ? 1 : 0;\n"
                    + "}\n";
            Program program = clContext.createProgramFromSourceCodeWithDependencies(code, assetManager);
            program.build();
            com.jme3.opencl.Buffer buffer = clContext.createBuffer(3);
            
            Random rand = new Random(1561);
            
            Kernel testMatrix4fKernel1 = program.createKernel("TestMatrix4f_1");
            Matrix4f m1 = new Matrix4f();
            do {
                for (int i=0; i<4; ++i) {
                    for (int j=0; j<4; ++j) {
                        m1.set(i, j, rand.nextFloat()*20 - 10);
                    }
                }
            } while (FastMath.abs(m1.determinant()) < 0.00001f);
            testMatrix4fKernel1.Run1NoEvent(clQueue, new Kernel.WorkSize(1), m1, buffer);
            ByteBuffer bb = buffer.map(clQueue, MappingAccess.MAP_READ_ONLY);
            if (bb.get() == 0) {
                LOG.severe("Matrix inversion failed");
                return false;
            }
            buffer.unmap(clQueue, bb);
            testMatrix4fKernel1.release();
            
            Kernel testMatrix4fKernel2 = program.createKernel("TestMatrix4f_2");
            for (int i=0; i<4; ++i) {
                for (int j=0; j<4; ++j) {
                    m1.set(i, j, rand.nextFloat()*20 - 10);
                }
            }
            testMatrix4fKernel2.Run1NoEvent(clQueue, new Kernel.WorkSize(1), m1, m1.determinant(), m1.transpose(), m1.adjoint(), buffer);
            bb = buffer.map(clQueue, MappingAccess.MAP_READ_ONLY);
            if (bb.get() == 0) {
                LOG.severe("Matrix determinant computation failed");
                return false;
            }
            if (bb.get() == 0) {
                LOG.severe("Matrix transposing failed");
                return false;
            }
            if (bb.get() == 0) {
                LOG.severe("Matrix adjoint computation failed");
                return false;
            }
            buffer.unmap(clQueue, bb);
            testMatrix4fKernel2.release();
            
            buffer.release();
            
        } catch (AssertionError ex) {
            LOG.log(Level.SEVERE, "matrix4f test failed with an assertion error");
            return false;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "matrix4f test failed with:", ex);
            return false;
        }
        return true;
    }
}
