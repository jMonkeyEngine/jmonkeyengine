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

package com.jme3.network.serializing.serializers;

import com.jme3.network.Message;
import com.jme3.network.message.GZIPCompressedMessage;
import com.jme3.network.serializing.Serializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Serializes GZIP messages.
 *
 * @author Lars Wesselius
 */
public class GZIPSerializer extends Serializer {

    @SuppressWarnings("unchecked")
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        try
        {
            GZIPCompressedMessage result = new GZIPCompressedMessage();

            byte[] byteArray = new byte[data.remaining()];

            data.get(byteArray);

            GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(byteArray));
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte[] tmp = new byte[9012];
            int read;

            while (in.available() > 0 && ((read = in.read(tmp)) > 0)) {
                out.write(tmp, 0, read);
            }

            result.setMessage((Message)Serializer.readClassAndObject(ByteBuffer.wrap(out.toByteArray())));
            return (T)result;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.toString());
        }
    }

    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
        if (!(object instanceof GZIPCompressedMessage)) return;
        Message message = ((GZIPCompressedMessage)object).getMessage();

        ByteBuffer tempBuffer = ByteBuffer.allocate(512000);
        Serializer.writeClassAndObject(tempBuffer, message);

        ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutput = new GZIPOutputStream(byteArrayOutput);

        gzipOutput.write(tempBuffer.array());
        gzipOutput.flush();
        gzipOutput.finish();
        gzipOutput.close();

        buffer.put(byteArrayOutput.toByteArray());
    }
}
