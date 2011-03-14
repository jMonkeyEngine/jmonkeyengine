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
        } else {
            ImageIO.write(editedImage, type, file);
        }
    }

    public BufferedImage load(FileObject file) throws IOException, URISyntaxException {
        if(file.getExt().equalsIgnoreCase("tga")) {
            ImageInputStream in = new FileImageInputStream(new File(file.getURL().toURI()));
            TGAImageReaderSpi spi = new TGAImageReaderSpi();
            TGAImageReader rea = new TGAImageReader(spi);
            rea.setInput(in);
            return rea.read(0);
        } else {
            BufferedImage image = ImageIO.read(file.getInputStream());
            return image;
        }
    }
}
