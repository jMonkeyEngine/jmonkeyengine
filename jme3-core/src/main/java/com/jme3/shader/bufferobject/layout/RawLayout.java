/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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

import java.nio.ByteBuffer;
import java.util.List;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.shader.bufferobject.BufferRegion;
import com.jme3.util.struct.Struct;

/**
 * Simple serializer
 * 
 * @author Riccardo Balbo
 */
public class RawLayout extends BufferLayout {

    public RawLayout() {
        // Init default serializers
        registerSerializer(new ObjectSerializer<byte[]>(byte[].class) {
            @Override
            public int length(BufferLayout serializer, byte[] obj) {
                return obj.length;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, byte[] obj) {
                return 1;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, byte[] obj) {
                bbf.put(obj);
            }
        });

        registerSerializer(new ObjectSerializer<Integer>(Integer.class) {
            @Override
            public int length(BufferLayout serializer, Integer obj) {
                return 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Integer obj) {
                return 1;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Integer obj) {
                bbf.putInt(obj);
            }
        });

        registerSerializer(new ObjectSerializer<Boolean>(Boolean.class) {
            @Override
            public int length(BufferLayout serializer, Boolean obj) {
                return 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Boolean obj) {
                return 1;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Boolean obj) {
                bbf.putInt(obj ? 1 : 0);
            }
        });

        registerSerializer(new ObjectSerializer<Float>(Float.class) {
            @Override
            public int length(BufferLayout serializer, Float obj) {
                return 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Float obj) {
                return 1;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Float obj) {
                bbf.putFloat(obj);
            }
        });

        registerSerializer(new ObjectSerializer<Vector2f>(Vector2f.class) {
            @Override
            public int length(BufferLayout serializer, Vector2f obj) {
                return 4 * 2;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector2f obj) {
                return 1;

            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Vector2f obj) {
                bbf.putFloat(obj.x);
                bbf.putFloat(obj.y);
            }
        });

        registerSerializer(new ObjectSerializer<ColorRGBA>(ColorRGBA.class) {
            @Override
            public int length(BufferLayout serializer, ColorRGBA obj) {
                return 4 * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, ColorRGBA obj) {
                return 1;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, ColorRGBA obj) {
                bbf.putFloat(obj.r);
                bbf.putFloat(obj.g);
                bbf.putFloat(obj.b);
                bbf.putFloat(obj.a);
            }
        });

        registerSerializer(new ObjectSerializer<Quaternion>(Quaternion.class) {
            @Override
            public int length(BufferLayout serializer, Quaternion obj) {
                return 4 * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Quaternion obj) {
                return 1;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Quaternion obj) {
                bbf.putFloat(obj.getX());
                bbf.putFloat(obj.getY());
                bbf.putFloat(obj.getZ());
                bbf.putFloat(obj.getW());
            }
        });

        registerSerializer(new ObjectSerializer<Vector4f>(Vector4f.class) {
            @Override
            public int length(BufferLayout serializer, Vector4f obj) {
                return 4 * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector4f obj) {
                return 1;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Vector4f obj) {
                bbf.putFloat(obj.x);
                bbf.putFloat(obj.y);
                bbf.putFloat(obj.z);
                bbf.putFloat(obj.w);

            }
        });

        registerSerializer(new ObjectSerializer<Vector3f>(Vector3f.class) {
            @Override
            public int length(BufferLayout serializer, Vector3f obj) {
                return 4 * 3;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector3f obj) {
                return 1;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Vector3f obj) {
                bbf.putFloat(obj.x);
                bbf.putFloat(obj.y);
                bbf.putFloat(obj.z);
            }
        });

        registerSerializer(new ObjectSerializer<Integer[]>(Integer[].class) {
            @Override
            public int length(BufferLayout serializer, Integer[] obj) {
                return 4 * obj.length;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Integer[] obj) {
                return 1;
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
                return 1;
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
                return 1;
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
                return 4 * obj.length * 2;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector2f[] obj) {
                return 1;
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
                return 4 * obj.length * 3;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector3f[] obj) {
                return 1;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Vector3f[] obj) {
                for (Vector3f i : obj) {
                    bbf.putFloat(i.x);
                    bbf.putFloat(i.y);
                    bbf.putFloat(i.z);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Vector4f[]>(Vector4f[].class) {
            @Override
            public int length(BufferLayout serializer, Vector4f[] obj) {
                return 4 * obj.length * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector4f[] obj) {
                return 1;
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
                return 4 * obj.length * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, ColorRGBA[] obj) {
                return 1;
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
                return 4 * obj.length * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Quaternion[] obj) {
                return 1;
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

        registerSerializer(new ObjectSerializer<Matrix3f>(Matrix3f.class) {
            @Override
            public int length(BufferLayout serializer, Matrix3f obj) {
                return 3 * 4 * 3;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Matrix3f obj) {
                return 1;
            }

            final Vector3f tmp = new Vector3f();

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Matrix3f obj) {
                obj.getColumn(0, tmp);
                bbf.putFloat(tmp.x);
                bbf.putFloat(tmp.y);
                bbf.putFloat(tmp.z);

                obj.getColumn(1, tmp);
                bbf.putFloat(tmp.x);
                bbf.putFloat(tmp.y);
                bbf.putFloat(tmp.z);

                obj.getColumn(2, tmp);
                bbf.putFloat(tmp.x);
                bbf.putFloat(tmp.y);
                bbf.putFloat(tmp.z);
            }
        });

        registerSerializer(new ObjectSerializer<Matrix4f>(Matrix4f.class) {
            @Override
            public int length(BufferLayout serializer, Matrix4f obj) {
                return 4 * 4 * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Matrix4f obj) {
                return 1;
            }

            final float[] tmpF = new float[4];

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Matrix4f obj) {
                obj.getColumn(0, tmpF);
                bbf.putFloat(tmpF[0]);
                bbf.putFloat(tmpF[1]);
                bbf.putFloat(tmpF[2]);
                bbf.putFloat(tmpF[3]);

                obj.getColumn(1, tmpF);
                bbf.putFloat(tmpF[0]);
                bbf.putFloat(tmpF[1]);
                bbf.putFloat(tmpF[2]);
                bbf.putFloat(tmpF[3]);

                obj.getColumn(2, tmpF);
                bbf.putFloat(tmpF[0]);
                bbf.putFloat(tmpF[1]);
                bbf.putFloat(tmpF[2]);
                bbf.putFloat(tmpF[3]);

                obj.getColumn(3, tmpF);
                bbf.putFloat(tmpF[0]);
                bbf.putFloat(tmpF[1]);
                bbf.putFloat(tmpF[2]);
                bbf.putFloat(tmpF[3]);

            }
        });

        registerSerializer(new ObjectSerializer<Matrix3f[]>(Matrix3f[].class) {
            @Override
            public int length(BufferLayout serializer, Matrix3f[] obj) {
                return 3 * 4 * 3 * obj.length;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Matrix3f[] obj) {
                return 1;
            }

            final Vector3f tmp = new Vector3f();

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Matrix3f[] objs) {
                for (Matrix3f obj : objs) {
                    obj.getColumn(0, tmp);
                    bbf.putFloat(tmp.x);
                    bbf.putFloat(tmp.y);
                    bbf.putFloat(tmp.z);

                    obj.getColumn(1, tmp);
                    bbf.putFloat(tmp.x);
                    bbf.putFloat(tmp.y);
                    bbf.putFloat(tmp.z);

                    obj.getColumn(2, tmp);
                    bbf.putFloat(tmp.x);
                    bbf.putFloat(tmp.y);
                    bbf.putFloat(tmp.z);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Matrix4f[]>(Matrix4f[].class) {
            @Override
            public int length(BufferLayout serializer, Matrix4f[] obj) {
                return 4 * 4 * 4 * obj.length;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Matrix4f[] obj) {
                return 1;
            }

            final float[] tmpF = new float[4];

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Matrix4f[] objs) {
                for (Matrix4f obj : objs) {
                    obj.getColumn(0, tmpF);
                    bbf.putFloat(tmpF[0]);
                    bbf.putFloat(tmpF[1]);
                    bbf.putFloat(tmpF[2]);
                    bbf.putFloat(tmpF[3]);

                    obj.getColumn(1, tmpF);
                    bbf.putFloat(tmpF[0]);
                    bbf.putFloat(tmpF[1]);
                    bbf.putFloat(tmpF[2]);
                    bbf.putFloat(tmpF[3]);

                    obj.getColumn(2, tmpF);
                    bbf.putFloat(tmpF[0]);
                    bbf.putFloat(tmpF[1]);
                    bbf.putFloat(tmpF[2]);
                    bbf.putFloat(tmpF[3]);

                    obj.getColumn(3, tmpF);
                    bbf.putFloat(tmpF[0]);
                    bbf.putFloat(tmpF[1]);
                    bbf.putFloat(tmpF[2]);
                    bbf.putFloat(tmpF[3]);
                }

            }
        });

    }

    @Override
    public String getId() {
        return "raw";
    }

    @Override
    public List<BufferRegion> generateFieldRegions(Struct struct) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

}
