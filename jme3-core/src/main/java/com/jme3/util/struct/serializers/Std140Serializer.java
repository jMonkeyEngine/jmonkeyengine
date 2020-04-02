package com.jme3.util.struct.serializers;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

/**
 * Std140Serializer
 * @author Riccardo Balbo
 */
public class Std140Serializer {

  
 
    
    public static abstract class ObjectSerializer<T> {
        final Class<T> type;
        public ObjectSerializer(Class<T> cls) {
            type = cls;
        }
        public final Class<T> getType() {
            return type;
        }
        public abstract int length(Std140Serializer serializer, T obj);     
        public  int basicAlignment(Std140Serializer serializer){
            return 4*4;
        }
    
        public abstract void write(Std140Serializer serializer, ByteBuffer bbf, T obj);
    }

    private ObjectSerializer<?> getSerializer(Class cls) {
        for (int i = registeredTypes.size() - 1; i >= 0; i--) {
            ObjectSerializer<?> sr = registeredTypes.get(i);
            if (sr.getType().isAssignableFrom(cls)) {
                return sr;
            }
        }
        return null;
    }

    public int estimateSize(Object o) {
        ObjectSerializer s = getSerializer(o.getClass());
        return s.length(this, o);
    }

    public int getBasicAlignment(Object o) {
        ObjectSerializer s = getSerializer(o.getClass());
        return s.basicAlignment(this);
    }


    public int align(int pos,int basicAlignment){
        return FastMath.toMultipleOf(pos, basicAlignment);
    }


    public void serialize(ByteBuffer out, Object o) {
        ObjectSerializer s = getSerializer(o.getClass());
        s.write(this, out, o);        
    }

    private final ArrayList<ObjectSerializer> registeredTypes = new ArrayList<ObjectSerializer>();


    public void registerSerializer(ObjectSerializer type) {
        registeredTypes.add(type);
    }

    {

        registerSerializer(new ObjectSerializer<Integer>(Integer.class) {
            @Override
            public int length(Std140Serializer serializer, Integer obj) {
                return 4;
            }

            @Override
            public int basicAlignment(Std140Serializer serializer) {
                return 4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Integer obj) {
                bbf.putInt(obj);
            }
        });




        registerSerializer(new ObjectSerializer<Integer[]>(Integer[].class) {
            @Override
            public int length(Std140Serializer serializer, Integer[] obj) {
                return 4*obj.length*4;
            }


            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Integer[] obj) {
                for(int i:obj){
                    bbf.putInt(i);
                    bbf.putInt(0);
                    bbf.putInt(0);
                    bbf.putInt(0);
                }
            }
        });

        

        registerSerializer(new ObjectSerializer<Float>(Float.class) {
            @Override
            public int length(Std140Serializer serializer, Float obj) {
                return 4;
            }

            @Override
            public int basicAlignment(Std140Serializer serializer) {
                return 4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Float obj) {
                bbf.putFloat(obj);
            }
        });

        registerSerializer(new ObjectSerializer<Float[]>(Float[].class) {
            @Override
            public int length(Std140Serializer serializer, Float[] obj) {
                return 4*obj.length*4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Float[] obj) {
                for(float i:obj){
                    bbf.putFloat(i);
                    bbf.putInt(0);
                    bbf.putInt(0);
                    bbf.putInt(0);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Boolean>(Boolean.class) {
            @Override
            public int length(Std140Serializer serializer, Boolean obj) {
                return 4;
            }

            @Override
            public int basicAlignment(Std140Serializer serializer) {
                return 4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Boolean obj) {
                bbf.putInt(obj ? 1 :  0);
            }
        });

        registerSerializer(new ObjectSerializer<Boolean[]>(Boolean[].class) {
            @Override
            public int length(Std140Serializer serializer, Boolean[] obj) {
                return 4*obj.length*4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Boolean[] obj) {
                for(boolean i:obj){
                    bbf.putInt(i?1:0);
                    bbf.putInt(0);
                    bbf.putInt(0);
                    bbf.putInt(0);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Vector2f>(Vector2f.class) {
            @Override
            public int length(Std140Serializer serializer, Vector2f obj) {
                return 4 * 2;
            }

            @Override
            public int basicAlignment(Std140Serializer serializer) {
                return 4*2;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Vector2f obj) {
                bbf.putFloat(obj.x);
                bbf.putFloat(obj.y);
            }
        });

        registerSerializer(new ObjectSerializer<Vector2f[]>(Vector2f[].class) {
            @Override
            public int length(Std140Serializer serializer, Vector2f[] obj) {
                return 4*obj.length*4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Vector2f[] obj) {
                for(Vector2f i:obj){
                    bbf.putFloat(i.x);
                    bbf.putFloat(i.y);
                    bbf.putInt(0);
                    bbf.putInt(0);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Vector3f>(Vector3f.class) {
            @Override
            public int length(Std140Serializer serializer, Vector3f obj) {
                return 4 * 4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Vector3f obj) {
                bbf.putFloat(obj.x);
                bbf.putFloat(obj.y);
                bbf.putFloat(obj.z);
                bbf.putFloat(0);

            }
        });

        registerSerializer(new ObjectSerializer<Vector3f[]>(Vector3f[].class) {
            @Override
            public int length(Std140Serializer serializer, Vector3f[] obj) {
                return 4*obj.length*4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Vector3f[] obj) {
                for(Vector3f i:obj){
                    bbf.putFloat(i.x);
                    bbf.putFloat(i.y);
                    bbf.putFloat(i.z);
                    bbf.putInt(0);
                }
            }
        });

        registerSerializer(new ObjectSerializer<Vector4f>(Vector4f.class) {
            @Override
            public int length(Std140Serializer serializer, Vector4f obj) {
                return 4 * 4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Vector4f obj) {
                bbf.putFloat(obj.x);
                bbf.putFloat(obj.y);
                bbf.putFloat(obj.z);
                bbf.putFloat(obj.w);

            }
        });

        registerSerializer(new ObjectSerializer<Vector4f[]>(Vector4f[].class) {
            @Override
            public int length(Std140Serializer serializer, Vector4f[] obj) {
                return 4*obj.length*4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Vector4f[] obj) {
                for(Vector4f i:obj){
                    bbf.putFloat(i.x);
                    bbf.putFloat(i.y);
                    bbf.putFloat(i.z);
                    bbf.putFloat(i.w);
                }
            }
        });


        registerSerializer(new ObjectSerializer<ColorRGBA>(ColorRGBA.class) {
            @Override
            public int length(Std140Serializer serializer, ColorRGBA obj) {
                return 4 * 4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, ColorRGBA obj) {
                bbf.putFloat(obj.r);
                bbf.putFloat(obj.g);
                bbf.putFloat(obj.b);
                bbf.putFloat(obj.a);
            }
        });

        registerSerializer(new ObjectSerializer<ColorRGBA[]>(ColorRGBA[].class) {
            @Override
            public int length(Std140Serializer serializer, ColorRGBA[] obj) {
                return 4*obj.length*4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, ColorRGBA[] obj) {
                for(ColorRGBA i:obj){
                    bbf.putFloat(i.r);
                    bbf.putFloat(i.g);
                    bbf.putFloat(i.b);
                    bbf.putFloat(i.a);
                }
            }
        });


        registerSerializer(new ObjectSerializer<Quaternion>(Quaternion.class) {
            @Override
            public int length(Std140Serializer serializer, Quaternion obj) {
                return 4 * 4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Quaternion obj) {
                bbf.putFloat(obj.getX());
                bbf.putFloat(obj.getY());
                bbf.putFloat(obj.getZ());
                bbf.putFloat(obj.getW());
            }
        });

        registerSerializer(new ObjectSerializer<Quaternion[]>(Quaternion[].class) {
            @Override
            public int length(Std140Serializer serializer, Quaternion[] obj) {
                return 4*obj.length*4;
            }

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Quaternion[] obj) {
                for(Quaternion i:obj){
                    bbf.putFloat(i.getX());
                    bbf.putFloat(i.getY());
                    bbf.putFloat(i.getZ());
                    bbf.putFloat(i.getW());
                }
            }
        });

        registerSerializer(new ObjectSerializer<Matrix3f>(Matrix3f.class) {
            @Override
            public int length(Std140Serializer serializer, Matrix3f obj) {
                return 3 * 4 * 4;
            }

            final Vector3f tmp = new Vector3f();

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Matrix3f obj) {
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


        // TODO: not sure if this is correct
        registerSerializer(new ObjectSerializer<Matrix3f[]>(Matrix3f[].class) {
            @Override
            public int length(Std140Serializer serializer, Matrix3f[] obj) {
                return 3 * 4 * 4 * obj.length;
            }

            final Vector3f tmp = new Vector3f();

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Matrix3f[] objs) {
                for(Matrix3f obj:objs){
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

        registerSerializer(new ObjectSerializer<Matrix4f>(Matrix4f.class) {
            @Override
            public int length(Std140Serializer serializer, Matrix4f obj) {
                return 4 * 4 * 4;
            }

            final float[] tmpF = new float[4];

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Matrix4f obj) {
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


        // TODO: not sure if this is correct
        registerSerializer(new ObjectSerializer<Matrix4f[]>(Matrix4f[].class) {
            @Override
            public int length(Std140Serializer serializer, Matrix4f[] obj) {
                return 4 * 4 * 4*obj.length;
            }

            final float[] tmpF = new float[4];

            @Override
            public void write(Std140Serializer serializer, ByteBuffer bbf, Matrix4f[] objs) {
                for(Matrix4f obj:objs){
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
}
