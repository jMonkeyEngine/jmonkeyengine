package com.jme3.gde.textureeditor;

import dds.jogl.DDSImage;
import dds.model.SingleTextureMap;
import dds.model.TextureMap;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import net.nikr.dds.DDSImageReaderSpi;
import org.openide.filesystems.FileObject;
import tgaimageplugin.TGAImageReader;
import tgaimageplugin.TGAImageReaderSpi;
import tgaimageplugin.TGAImageWriter;
import tgaimageplugin.TGAImageWriterSpi;

public class IOModule {

    public static IOModule create() {
        return new IOModule();
    }

    private IOModule() {
    }

    public void store(BufferedImage editedImage, String type, File file) throws IOException {
        if (type.equals("tga")) {
            TGAImageWriterSpi spi = new TGAImageWriterSpi();
            TGAImageWriter wri = new TGAImageWriter(spi);
            wri.setOutput(new FileImageOutputStream(file));
            wri.write(editedImage);
		} else if (type.equals("dds")) {
			//DDSUtil.write(file, editedImage, DDSImage.D3DFMT_A8R8G8B8, false);
			writeDDS(editedImage, file);
        } else {
            ImageIO.write(editedImage, type, file);
        }
    }
	
	private void writeDDS(BufferedImage img, File file) throws IOException {
		Image image = Toolkit.getDefaultToolkit().createImage(
					new FilteredImageSource(img.getSource(), new RedBlueSwapFilter()));
		img = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_4BYTE_ABGR);
		Graphics g = img.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		//create dds data
		TextureMap tex;
		tex = new SingleTextureMap(img);
		//create dds image
		ByteBuffer[] data;
		data = tex.getUncompressedBuffer(); //no compression
		int format = DDSImage.D3DFMT_A8R8G8B8;
		DDSImage dds = DDSImage.createFromData(format, img.getWidth(), img.getHeight(), data);
		//save asset
		dds.write(file);
	}
	
	public static class RedBlueSwapFilter extends RGBImageFilter {

		public RedBlueSwapFilter() {
		// The filter's operation does not depend on the
			// pixel's location, so IndexColorModels can be
			// filtered directly.
			canFilterIndexColorModel = true;
		}

		@Override
		public int filterRGB(int x, int y, int rgb) {
			return ((rgb & 0xff00ff00)
					| ((rgb & 0xff0000) >> 16)
					| ((rgb & 0xff) << 16));
		}
	}


    public BufferedImage load(FileObject file) throws IOException, URISyntaxException {
        if (file.getExt().equalsIgnoreCase("tga")) {
            ImageInputStream in = new FileImageInputStream(new File(file.getURL().toURI()));
            TGAImageReaderSpi spi = new TGAImageReaderSpi();
            TGAImageReader rea = new TGAImageReader(spi);
            rea.setInput(in);
            return rea.read(0);
		} else if (file.getExt().equalsIgnoreCase("dds")) {
			//return DDSUtil.read(FileUtil.toFile(file)); 
			//Use DDSImageReader because of a bug in Dahie-DDS
			ImageInputStream in = new FileImageInputStream(new File(file.getURL().toURI()));
            DDSImageReaderSpi spi = new DDSImageReaderSpi();
            ImageReader rea = spi.createReaderInstance();
            rea.setInput(in);
            return rea.read(0);
        } else {
            BufferedImage image = ImageIO.read(file.getInputStream());
            return image;
        }
    }
}
