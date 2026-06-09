/*
 * Copyright (c) 2026 jMonkeyEngine
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
package com.jme3.shader.bufferobject.layout;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import java.nio.ByteBuffer;

/**
 * Serializer that respects the Std430 layout.
 */
public class Std430Layout extends Std140Layout {

    public Std430Layout() {
        registerSerializer(new ObjectSerializer<Integer[]>(Integer[].class) {
            @Override
            public int length(BufferLayout serializer, Integer[] obj) {
                return 4 * obj.length;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Integer[] obj) {
                return 4;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Integer[] obj) {
                for (int i : obj) {
                    bbf.putInt(i);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Float[]>(Float[].class) {
            @Override
            public int length(BufferLayout serializer, Float[] obj) {
                return 4 * obj.length;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Float[] obj) {
                return 4;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Float[] obj) {
                for (float i : obj) {
                    bbf.putFloat(i);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Boolean[]>(Boolean[].class) {
            @Override
            public int length(BufferLayout serializer, Boolean[] obj) {
                return 4 * obj.length;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Boolean[] obj) {
                return 4;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Boolean[] obj) {
                for (boolean i : obj) {
                    bbf.putInt(i ? 1 : 0);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Vector2f[]>(Vector2f[].class) {
            @Override
            public int length(BufferLayout serializer, Vector2f[] obj) {
                return 8 * obj.length;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector2f[] obj) {
                return 8;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Vector2f[] obj) {
                for (Vector2f i : obj) {
                    bbf.putFloat(i.x);
                    bbf.putFloat(i.y);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Vector3f[]>(Vector3f[].class) {
            @Override
            public int length(BufferLayout serializer, Vector3f[] obj) {
                return 16 * obj.length;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector3f[] obj) {
                return 16;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Vector3f[] obj) {
                for (Vector3f i : obj) {
                    bbf.putFloat(i.x);
                    bbf.putFloat(i.y);
                    bbf.putFloat(i.z);
                    bbf.putInt(0);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Vector4f[]>(Vector4f[].class) {
            @Override
            public int length(BufferLayout serializer, Vector4f[] obj) {
                return 16 * obj.length;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector4f[] obj) {
                return 16;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Vector4f[] obj) {
                for (Vector4f i : obj) {
                    bbf.putFloat(i.x);
                    bbf.putFloat(i.y);
                    bbf.putFloat(i.z);
                    bbf.putFloat(i.w);
                }
            }
        });

        registerSerializer(new ObjectSerializer<ColorRGBA[]>(ColorRGBA[].class) {
            @Override
            public int length(BufferLayout serializer, ColorRGBA[] obj) {
                return 16 * obj.length;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, ColorRGBA[] obj) {
                return 16;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, ColorRGBA[] obj) {
                for (ColorRGBA i : obj) {
                    bbf.putFloat(i.r);
                    bbf.putFloat(i.g);
                    bbf.putFloat(i.b);
                    bbf.putFloat(i.a);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Quaternion[]>(Quaternion[].class) {
            @Override
            public int length(BufferLayout serializer, Quaternion[] obj) {
                return 16 * obj.length;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Quaternion[] obj) {
                return 16;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Quaternion[] obj) {
                for (Quaternion i : obj) {
                    bbf.putFloat(i.getX());
                    bbf.putFloat(i.getY());
                    bbf.putFloat(i.getZ());
                    bbf.putFloat(i.getW());
                }
            }
        });
    }

    @Override
    public int getStructureAlignment(int maxMemberAlignment) {
        return maxMemberAlignment;
    }

    @Override
    public String getId() {
        return "std430";
    }
}
