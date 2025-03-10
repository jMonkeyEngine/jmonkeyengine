package com.jme3.texture;

import static org.junit.Assert.*;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.texture.Texture.WrapAxis;
import com.jme3.texture.Texture.WrapMode;
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
    List<Image> images = new ArrayList<>();
    images.add(createImage());
    images.add(createImage());
    TextureArray tex3 = new TextureArray(images);
    tex3.setWrap(WrapMode.Repeat);
    TextureArray tex4 = BinaryExporter.saveAndLoad(assetManager, tex3);

    assertEquals(tex3.getWrap(WrapAxis.S), tex4.getWrap(WrapAxis.S));
    assertEquals(tex3.getWrap(WrapAxis.T), tex4.getWrap(WrapAxis.T));
  }

  private Image createImage() {
    int width = 8;
    int height = 8;
    int numBytes = 4 * width * height;
    ByteBuffer data = BufferUtils.createByteBuffer(numBytes);
    return new Image(Image.Format.RGBA8, width, height, data, ColorSpace.Linear);
  }

}
