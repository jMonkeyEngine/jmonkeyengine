package com.jme3.network.serializing.serializers;

import com.jme3.math.Vector3f;
import com.jme3.network.serializing.Serializer;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Kirill Vainer
 */
@SuppressWarnings("unchecked")
public class Vector3Serializer extends Serializer {

    public Vector3f readObject(ByteBuffer data, Class c) throws IOException {
        Vector3f vec3 = new Vector3f();
        vec3.x = data.getFloat();
        vec3.y = data.getFloat();
        vec3.z = data.getFloat();
        return vec3;
    }

    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
        Vector3f vec3 = (Vector3f) object;
        buffer.putFloat(vec3.x);
        buffer.putFloat(vec3.y);
        buffer.putFloat(vec3.z);
    }
}
