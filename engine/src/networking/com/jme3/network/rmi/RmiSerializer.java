/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.network.rmi;

import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.SerializerRegistration;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link RmiSerializer} is responsible for serializing RMI messages
 * like define object, call, and return.
 *
 * @author Kirill Vainer
 */
public class RmiSerializer extends Serializer {

    private static final Logger logger = Logger.getLogger(RmiSerializer.class.getName());

    // not good for multithread applications
    private char[] chrBuf = new char[256];

    private void writeString(ByteBuffer buffer, String string) throws IOException{
        int length = string.length();
        if (length > 255){
            logger.log(Level.WARNING, "The string length exceeds the limit! {0} > 255", length);
            buffer.put( (byte) 0 );
            return;
        }

        buffer.put( (byte) length );
        for (int i = 0; i < length; i++){
            buffer.put( (byte) string.charAt(i) );
        }
    }

    private String readString(ByteBuffer buffer){
        int length = buffer.get() & 0xff;
        for (int i = 0; i < length; i++){
            chrBuf[i] = (char) (buffer.get() & 0xff);
        }
        return String.valueOf(chrBuf, 0, length);
    }

    private void writeType(ByteBuffer buffer, Class<?> clazz) throws IOException{
        if (clazz == void.class){
            buffer.putShort((short)0);
        } else {
            SerializerRegistration reg = Serializer.getSerializerRegistration(clazz);
            if (reg == null){
                logger.log(Level.WARNING, "Unknown class: {0}", clazz);
                throw new IOException(); // prevents message from being serialized
            }
            buffer.putShort(reg.getId());
        }
    }

    private Class<?> readType(ByteBuffer buffer) throws IOException{
        SerializerRegistration reg = Serializer.readClass(buffer);
        if (reg == null){
            // either "void" or unknown val
            short id = buffer.getShort(buffer.position()-2);
            if (id == 0){
                return void.class;
            } else{
                logger.log(Level.WARNING, "Undefined class ID: {0}", id);
                throw new IOException(); // prevents message from being serialized
            }
        }
        return reg.getType();
    }

    private void writeMethod(ByteBuffer buffer, Method method) throws IOException{
        String     name = method.getName();
        Class<?>[] paramTypes = method.getParameterTypes();
        Class<?>   returnType = method.getReturnType();

        writeString(buffer, name);
        writeType(buffer, returnType);
        buffer.put((byte)paramTypes.length);
        for (Class<?> paramType : paramTypes)
            writeType(buffer, paramType);
    }

    private MethodDef readMethod(ByteBuffer buffer) throws IOException{
        String name = readString(buffer);
        Class<?> retType = readType(buffer);
        
        int numParams = buffer.get() & 0xff;
        Class<?>[] paramTypes = new Class<?>[numParams];
        for (int i = 0; i < numParams; i++){
            paramTypes[i] = readType(buffer);
        }

        MethodDef def = new MethodDef();
        def.name = name;
        def.paramTypes = paramTypes;
        def.retType = retType;
        return def;
    }

    private void writeObjectDef(ByteBuffer buffer, ObjectDef def) throws IOException{
        buffer.putShort((short)def.objectId);
        writeString(buffer, def.objectName);
        Method[] methods = def.methods;
        buffer.put( (byte) methods.length );
        for (Method method : methods){
            writeMethod(buffer, method);
        }
    }

    private ObjectDef readObjectDef(ByteBuffer buffer) throws IOException{
        ObjectDef def = new ObjectDef();

        def.objectId = buffer.getShort();
        def.objectName = readString(buffer);

        int numMethods = buffer.get() & 0xff;
        MethodDef[] methodDefs = new MethodDef[numMethods];
        for (int i = 0; i < numMethods; i++){
            methodDefs[i] = readMethod(buffer);
        }
        def.methodDefs = methodDefs;
        return def;
    }

    private void writeObjectDefs(ByteBuffer buffer, RemoteObjectDefMessage defMsg) throws IOException{
        ObjectDef[] defs = defMsg.objects;
        buffer.put( (byte) defs.length );
        for (ObjectDef def : defs)
            writeObjectDef(buffer, def);
    }

    private RemoteObjectDefMessage readObjectDefs(ByteBuffer buffer) throws IOException{
        RemoteObjectDefMessage defMsg = new RemoteObjectDefMessage();
        int numObjs = buffer.get() & 0xff;
        ObjectDef[] defs = new ObjectDef[numObjs];
        for (int i = 0; i < numObjs; i++){
            defs[i] = readObjectDef(buffer);
        }
        defMsg.objects = defs;
        return defMsg;
    }

    private void writeMethodCall(ByteBuffer buffer, RemoteMethodCallMessage call) throws IOException{
        buffer.putShort((short)call.objectId);
        buffer.putShort(call.methodId);
        buffer.putShort(call.invocationId);
        if (call.args == null){
            buffer.put((byte)0);
        }else{
            buffer.put((byte)call.args.length);

            // Right now it writes 0 for every null argument
            // and 1 for every non-null argument followed by the serialized
            // argument. For the future, using a bit set should be considered.
            for (Object obj : call.args){
                if (obj != null){
                    buffer.put((byte)0x01);
                    Serializer.writeClassAndObject(buffer, obj);
                }else{
                    buffer.put((byte)0x00);
                }
            }
        }
    }

    private RemoteMethodCallMessage readMethodCall(ByteBuffer buffer) throws IOException{
        RemoteMethodCallMessage call = new RemoteMethodCallMessage();
        call.objectId = buffer.getShort();
        call.methodId = buffer.getShort();
        call.invocationId = buffer.getShort();
        int numArgs = buffer.get() & 0xff;
        if (numArgs > 0){
            Object[] args = new Object[numArgs];
            for (int i = 0; i < numArgs; i++){
                if (buffer.get() == (byte)0x01){
                    args[i] = Serializer.readClassAndObject(buffer);
                }
            }
            call.args = args;
        }
        return call;
    }

    private void writeMethodReturn(ByteBuffer buffer, RemoteMethodReturnMessage ret) throws IOException{
        buffer.putShort(ret.invocationID);
        if (ret.retVal != null){
            buffer.put((byte)0x01);
            Serializer.writeClassAndObject(buffer, ret.retVal);
        }else{
            buffer.put((byte)0x00);
        }
    }

    private RemoteMethodReturnMessage readMethodReturn(ByteBuffer buffer) throws IOException{
        RemoteMethodReturnMessage ret = new RemoteMethodReturnMessage();
        ret.invocationID = buffer.getShort();
        if (buffer.get() == (byte)0x01){
            ret.retVal = Serializer.readClassAndObject(buffer);
        }
        return ret;
    }
            
    @Override
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        if (c == RemoteObjectDefMessage.class){
            return (T) readObjectDefs(data);
        }else if (c == RemoteMethodCallMessage.class){
            return (T) readMethodCall(data);
        }else if (c == RemoteMethodReturnMessage.class){
            return (T) readMethodReturn(data);
        }
        return null;
    }

    @Override
    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
//        int p = buffer.position();
        if (object instanceof RemoteObjectDefMessage){
            RemoteObjectDefMessage def = (RemoteObjectDefMessage) object;
            writeObjectDefs(buffer, def);
        }else if (object instanceof RemoteMethodCallMessage){
            RemoteMethodCallMessage call = (RemoteMethodCallMessage) object;
            writeMethodCall(buffer, call);
        }else if (object instanceof RemoteMethodReturnMessage){
            RemoteMethodReturnMessage ret = (RemoteMethodReturnMessage) object;
            writeMethodReturn(buffer, ret);
        }
//        p = buffer.position() - p;
//        System.out.println(object+": uses " + p + " bytes");
    }

}
