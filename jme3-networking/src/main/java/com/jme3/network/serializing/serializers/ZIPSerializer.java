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
package com.jme3.network.serializing.serializers;

import com.jme3.network.Message;
import com.jme3.network.message.ZIPCompressedMessage;
import com.jme3.network.serializing.Serializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Serializes ZIP messages.
 *
 * @author Lars Wesselius
 */
public class ZIPSerializer extends Serializer {

    @SuppressWarnings("unchecked")
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        try
        {
            ZIPCompressedMessage result = new ZIPCompressedMessage();

            byte[] byteArray = new byte[data.remaining()];

            data.get(byteArray);

            ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(byteArray));
            in.getNextEntry();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte[] tmp = new byte[9012];
            int read;

            while (in.available() > 0 && ((read = in.read(tmp)) > 0)) {
                out.write(tmp, 0, read);
            }

            in.closeEntry();
            out.flush();
            in.close();

            result.setMessage((Message)Serializer.readClassAndObject(ByteBuffer.wrap(out.toByteArray())));
            return (T)result;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.toString());
        }
    }

    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
        if (!(object instanceof ZIPCompressedMessage)) return;

        ZIPCompressedMessage zipMessage = (ZIPCompressedMessage)object;
        Message message = zipMessage.getMessage();
        ByteBuffer tempBuffer = ByteBuffer.allocate(512000);
        Serializer.writeClassAndObject(tempBuffer, message);

        ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
        ZipOutputStream zipOutput = new ZipOutputStream(byteArrayOutput);
        zipOutput.setLevel(zipMessage.getLevel());

        ZipEntry zipEntry = new ZipEntry("zip");

        zipOutput.putNextEntry(zipEntry);
        tempBuffer.flip();
        zipOutput.write(tempBuffer.array(), 0, tempBuffer.limit());
        zipOutput.flush();
        zipOutput.closeEntry();
        zipOutput.close();

        buffer.put(byteArrayOutput.toByteArray());
    }
}
