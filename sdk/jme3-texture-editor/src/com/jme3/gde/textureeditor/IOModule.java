package com.jme3.gde.textureeditor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
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
			writeDDS(editedImage, file);
        } else {
            ImageIO.write(editedImage, type, file);
        }
    }
	
	private void writeDDS(BufferedImage img, File file) throws IOException {
        File tga = null;
        TGAImageWriter wri = null;
        try {
            //copy to tmp tga file
            tga = File.createTempFile("tmp", ".tga").getCanonicalFile();
            TGAImageWriterSpi spi = new TGAImageWriterSpi();
            wri = new TGAImageWriter(spi);
            wri.setOutput(new FileImageOutputStream(tga));
            wri.write(img);
            //convert to uncompressed u8888 dds texture
            if (!NvDXTExecutor.executeCompress("-file", tga.getPath(), "-u8888", "-outfile", file.getCanonicalPath())) {
                throw new IOException("unable to write dds texture");
            }
        } finally {
            if (wri!=null) {
                wri.abort();
                wri.dispose();
            }
            if (tga!=null) {
                tga.delete();
            }
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
            return loadDDS(file);
        } else {
            BufferedImage image = ImageIO.read(file.getInputStream());
            return image;
        }
    }

    private synchronized BufferedImage loadDDS(final FileObject file) throws IOException {
        File dds = null;
        File tga;
        File parent = null;
        String name = null;
        TGAImageReader rea = null;
        try {
            //copy to tmp dir
            dds = File.createTempFile("tmp", ".dds").getCanonicalFile();
            String path = dds.getAbsolutePath();
            name = dds.getName();
            name = name.substring(0, name.length() - ".dds".length());
            parent = dds.getParentFile();
            FileObject folder = FileUtil.toFileObject(parent);
            dds.delete();
            file.copy(folder, name, "dds");
            //convert to tga
            if (!NvDXTExecutor.executeDeompress(path)) {
                throw new IOException("unable to decompress dds texture");
            }
            //read tga file
            tga = new File(parent, name + "00.tga");
            ImageInputStream in = new FileImageInputStream(tga);
            TGAImageReaderSpi spi = new TGAImageReaderSpi();
            rea = new TGAImageReader(spi);
            rea.setInput(in);
            BufferedImage img = rea.read(0);
            return img;
        } finally {
            if (dds!=null) {
                dds.delete();
            }
            if (rea!=null) {
                rea.abort();
                rea.dispose();
            }
            if (parent!=null) {
                for (File f : parent.listFiles()) {
                    if (f.getName().startsWith(name) && f.getName().endsWith(".tga")) {
                        f.delete();
                    }
                }
            }
        }
    }

}
