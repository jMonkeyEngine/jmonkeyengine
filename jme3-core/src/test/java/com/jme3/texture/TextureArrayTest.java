/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.texture;

import static org.junit.Assert.*;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.texture.GlTexture.WrapAxis;
import com.jme3.texture.GlTexture.WrapMode;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class TextureArrayTest {

  private static final AssetManager assetManager = new DesktopAssetManager();

  @Test
  public void testExportWrapMode() {
    List<GlImage> images = new ArrayList<>();
    images.add(createImage());
    images.add(createImage());
    TextureArray tex3 = new TextureArray(images);
    tex3.setWrap(WrapMode.Repeat);
    TextureArray tex4 = BinaryExporter.saveAndLoad(assetManager, tex3);

    assertEquals(tex3.getWrap(WrapAxis.S), tex4.getWrap(WrapAxis.S));
    assertEquals(tex3.getWrap(WrapAxis.T), tex4.getWrap(WrapAxis.T));
  }

  private GlImage createImage() {
    int width = 8;
    int height = 8;
    int numBytes = 4 * width * height;
    ByteBuffer data = BufferUtils.createByteBuffer(numBytes);
    return new GlImage(GlImage.Format.RGBA8, width, height, data, ColorSpace.Linear);
  }

}
