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
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
 * Serializer that respects the <a href="https://www.khronos.org/registry/OpenGL/specs/gl/glspec45.core.pdf#page=159">
 * Std140 layout</a> (www.opengl.org/registry/specs/ARB/uniform_buffer_object.txt).
 *
 * @author Riccardo Balbo
 */
public class Std140Layout extends BufferLayout {

    public Std140Layout() {
        // Init default serializers
        // 1. If the member is a scalar consuming N basic machine units, the
        // base alignment is N .
        registerSerializer(new ObjectSerializer<Integer>(Integer.class) {
            @Override
            public int length(BufferLayout serializer, Integer obj) {
                return 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Integer obj) {
                return 4;
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
                return 4;
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
                return 4;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Float obj) {
                bbf.putFloat(obj);
            }
        });

        // 2. If the member is a two- or four-component vector with components
        // consuming N basic machine units, the base alignment is 2N or 4N ,
        // respectively
        registerSerializer(new ObjectSerializer<Vector2f>(Vector2f.class) {
            @Override
            public int length(BufferLayout serializer, Vector2f obj) {
                return 4 * 2;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector2f obj) {
                return 4 * 2;
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
                return 4 * 4;
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
                return 4 * 4;
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
                return 4 * 4;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Vector4f obj) {
                bbf.putFloat(obj.x);
                bbf.putFloat(obj.y);
                bbf.putFloat(obj.z);
                bbf.putFloat(obj.w);

            }
        });

        // 3. If the member is a three-component vector with components
        // consuming N
        // basic machine units, the base alignment is 4N
        registerSerializer(new ObjectSerializer<Vector3f>(Vector3f.class) {
            @Override
            public int length(BufferLayout serializer, Vector3f obj) {
                return 4 * 3;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector3f obj) {
                return 4 * 4;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Vector3f obj) {
                bbf.putFloat(obj.x);
                bbf.putFloat(obj.y);
                bbf.putFloat(obj.z);
                // bbf.putFloat(0);

            }
        });

        // 4. If the member is an array of scalars or vectors, the base
        // alignment and array
        // stride are set to match the base alignment of a single array element,
        // according
        // to rules (1), (2), and (3), and rounded up to the base alignment of a
        // vec4. The
        // array may have padding at the end; the base offset of the member
        // following
        // the array is rounded up to the next multiple of the base alignment.

        registerSerializer(new ObjectSerializer<Integer[]>(Integer[].class) {
            @Override
            public int length(BufferLayout serializer, Integer[] obj) {
                return 4 * obj.length * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Integer[] obj) {
                return 4 * 4;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Integer[] obj) {
                for (int i : obj) {
                    bbf.putInt(i);
                    bbf.putInt(0);
                    bbf.putInt(0);
                    bbf.putInt(0);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Float[]>(Float[].class) {
            @Override
            public int length(BufferLayout serializer, Float[] obj) {
                return 4 * obj.length * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Float[] obj) {
                return 4 * 4;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Float[] obj) {
                for (float i : obj) {
                    bbf.putFloat(i);
                    bbf.putInt(0);
                    bbf.putInt(0);
                    bbf.putInt(0);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Boolean[]>(Boolean[].class) {
            @Override
            public int length(BufferLayout serializer, Boolean[] obj) {
                return 4 * obj.length * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Boolean[] obj) {
                return 4 * 4;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Boolean[] obj) {
                for (boolean i : obj) {
                    bbf.putInt(i ? 1 : 0);
                    bbf.putInt(0);
                    bbf.putInt(0);
                    bbf.putInt(0);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Vector2f[]>(Vector2f[].class) {
            @Override
            public int length(BufferLayout serializer, Vector2f[] obj) {
                return 4 * obj.length * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector2f[] obj) {
                return 4 * 4;
            }

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Vector2f[] obj) {
                for (Vector2f i : obj) {
                    bbf.putFloat(i.x);
                    bbf.putFloat(i.y);
                    bbf.putInt(0);
                    bbf.putInt(0);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Vector3f[]>(Vector3f[].class) {
            @Override
            public int length(BufferLayout serializer, Vector3f[] obj) {
                return 4 * obj.length * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector3f[] obj) {
                return 4 * 4;
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
                return 4 * obj.length * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Vector4f[] obj) {
                return 4 * 4;
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
                return 4 * 4;
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
                return 4 * 4;
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

        // 5. If the member is a column-major matrix with C columns and R rows,
        // the
        // matrix is stored identically to an array of C column vectors with R
        // compo-
        // nents each, according to rule (4).

        registerSerializer(new ObjectSerializer<Matrix3f>(Matrix3f.class) {
            @Override
            public int length(BufferLayout serializer, Matrix3f obj) {
                return 3 * 4 * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Matrix3f obj) {
                return 4 * 4;
            }

            final Vector3f tmp = new Vector3f();

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Matrix3f obj) {
                obj.getColumn(0, tmp);
                bbf.putFloat(tmp.x);
                bbf.putFloat(tmp.y);
                bbf.putFloat(tmp.z);
                bbf.putFloat(0);

                obj.getColumn(1, tmp);
                bbf.putFloat(tmp.x);
                bbf.putFloat(tmp.y);
                bbf.putFloat(tmp.z);
                bbf.putFloat(0);

                obj.getColumn(2, tmp);
                bbf.putFloat(tmp.x);
                bbf.putFloat(tmp.y);
                bbf.putFloat(tmp.z);
                bbf.putFloat(0);
            }
        });

        registerSerializer(new ObjectSerializer<Matrix4f>(Matrix4f.class) {
            @Override
            public int length(BufferLayout serializer, Matrix4f obj) {
                return 4 * 4 * 4;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Matrix4f obj) {
                return 4 * 4;
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

        // 6. If the member is an array of S column-major matrices with C
        // columns and
        // R rows, the matrix is stored identically to a row of S × C column
        // vectors
        // with R components each, according to rule (4).

        registerSerializer(new ObjectSerializer<Matrix3f[]>(Matrix3f[].class) {
            @Override
            public int length(BufferLayout serializer, Matrix3f[] obj) {
                return 3 * 4 * 4 * obj.length;
            }

            @Override
            public int basicAlignment(BufferLayout serializer, Matrix3f[] obj) {
                return 4 * 4;
            }

            final Vector3f tmp = new Vector3f();

            @Override
            public void write(BufferLayout serializer, ByteBuffer bbf, Matrix3f[] objs) {
                for (Matrix3f obj : objs) {
                    obj.getColumn(0, tmp);
                    bbf.putFloat(tmp.x);
                    bbf.putFloat(tmp.y);
                    bbf.putFloat(tmp.z);
                    bbf.putFloat(0);

                    obj.getColumn(1, tmp);
                    bbf.putFloat(tmp.x);
                    bbf.putFloat(tmp.y);
                    bbf.putFloat(tmp.z);
                    bbf.putFloat(0);

                    obj.getColumn(2, tmp);
                    bbf.putFloat(tmp.x);
                    bbf.putFloat(tmp.y);
                    bbf.putFloat(tmp.z);
                    bbf.putFloat(0);
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
                return 4 * 4;
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

        // 7. If the member is a row-major matrix with C columns and R rows, the
        // matrix
        // is stored identically to an array of R row vectors with C components
        // each,
        // according to rule (4).

        // Nothing: jme matrices are column-major

        // 8. If the member is an array of S row-major matrices with C columns
        // and R
        // rows, the matrix is stored identically to a row of S × R row vectors
        // with C
        // components each, according to rule (4)

        // Nothing: jme matrices are column-major

        // 9. If the member is a structure, the base alignment of the structure
        // is N , where
        // N is the largest base alignment value of any of its members, and
        // rounded
        // up to the base alignment of a vec4. The individual members of this
        // sub-
        // structure are then assigned offsets by applying this set of rules
        // recursively,
        // where the base offset of the first member of the sub-structure is
        // equal to the
        // aligned offset of the structure. The structure may have padding at
        // the end;
        // the base offset of the member following the sub-structure is rounded
        // up to
        // the next multiple of the base alignment of the structure.

        // IMPLEMENTED AT A HIGHER LEVEL

        // 10. If the member is an array of S structures, the S elements of the
        // array are laid
        // out in order, according to rule (9)

        // IMPLEMENTED AT A HIGHER LEVEL

    }

    @Override
    public String getId() {
        return "std140";
    }

    @Override
    public List<BufferRegion> generateFieldRegions(Struct struct) {
        int pos = -1;
        List<BufferRegion> regions = new ArrayList<>();
        List<StructField> fields = struct.getFields();
        for (ListIterator<StructField> it = fields.listIterator(); it.hasNext();) {
            StructField<?> f = it.next();
            Object v = f.getFieldValue();

            int basicAlignment = getBasicAlignment(v);
            int length = estimateSize(v);

            int start = align(pos + 1, basicAlignment);
            int end = start + length - 1;

            if (!it.hasNext() || f.getOwner() != fields.get(it.nextIndex()).getOwner()) {
                end = align(end, 16) - 1;
            }

            BufferRegion r = new BufferRegion(start, end);
            regions.add(r);
            pos = end;
        }
        return regions;
    }

    @Override
    public BufferSlice getNextFieldRegion(int position, StructField field, StructField next) {
        Object v = field.getFieldValue();
        int basicAlignment = getBasicAlignment(v);
        int length = estimateSize(v);
        int start = align(position, basicAlignment);
        int end = start + length - 1;
        if (next == null || field.getOwner() != next.getOwner()) {
            end = align(end, 16) - 1;
        }
        return new BufferSlice(start, end);
    }

}
