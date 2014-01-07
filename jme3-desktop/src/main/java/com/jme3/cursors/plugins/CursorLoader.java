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
package com.jme3.cursors.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.util.BufferUtils;
import com.jme3.util.LittleEndien;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/**
 *
 * @author MadJack
 * @creation Jun 5, 2012 9:45:58 AM
 */
public class CursorLoader implements AssetLoader {

    private boolean isIco;
    private boolean isAni;

    /**
     * Loads and return a cursor file of one of the following format: .ani, .cur and .ico.
     * @param info The {@link AssetInfo} describing the cursor file.
     * @return A JmeCursor representation of the LWJGL's Cursor.
     * @throws IOException if the file is not found.
     */
    public JmeCursor load(AssetInfo info) throws IOException {

        isIco = false;
        isAni = false;

        isIco = ((AssetKey) info.getKey()).getExtension().equals("ico");
        if (!isIco) {
            isIco = ((AssetKey) info.getKey()).getExtension().equals("cur");
            if (!isIco) {
                isAni = ((AssetKey) info.getKey()).getExtension().equals("ani");
            }
        }
        if (!isAni && !isIco) {
            throw new IllegalArgumentException("Cursors supported are .ico, .cur or .ani");
        }

        InputStream in = null;
        try {
            in = info.openStream();
            return loadCursor(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private JmeCursor loadCursor(InputStream inStream) throws IOException {

        byte[] icoimages = new byte[0]; // new byte [0] facilitates read()

        if (isAni) {
            CursorImageData ciDat = new CursorImageData();
            int numIcons = 0;
            int jiffy = 0;
            // not using those but keeping references for now.
            int steps = 0;
            int width = 0;
            int height = 0;
            int flag = 0; // we don't use that.
            int[] rate = null;
            int[] animSeq = null;
            ArrayList<byte[]> icons;

            DataInput leIn = new LittleEndien(inStream);
            int riff = leIn.readInt();
            if (riff == 0x46464952) { // RIFF
                // read next int (file length), discarding it, we don't need that.
                leIn.readInt();

                int nextInt = 0;

                nextInt = getNext(leIn);
                if (nextInt == 0x4e4f4341) {
                    // We have ACON, we do nothing
//                    System.out.println("We have ACON. Next!");
                    nextInt = getNext(leIn);
                    while (nextInt >= 0) {
                        if (nextInt == 0x68696e61) {
//                            System.out.println("we have 'anih' header");
                            leIn.skipBytes(8); // internal struct length (always 36)
                            numIcons = leIn.readInt();
                            steps = leIn.readInt(); // number of blits for ani cycles
                            width = leIn.readInt();
                            height = leIn.readInt();
                            leIn.skipBytes(8);
                            jiffy = leIn.readInt();
                            flag = leIn.readInt();
                            nextInt = leIn.readInt();
                        } else if (nextInt == 0x65746172) { // found a 'rate' of animation
//                            System.out.println("we have 'rate'.");
                            // Fill rate here.
                            // Rate is synchronous with frames.
                            int length = leIn.readInt();
                            rate = new int[length / 4];
                            for (int i = 0; i < length / 4; i++) {
                                rate[i] = leIn.readInt();
                            }
                            nextInt = leIn.readInt();
                        } else if (nextInt == 0x20716573) { // found a 'seq ' of animation
//                            System.out.println("we have 'seq '.");
                            // Fill animation sequence here
                            int length = leIn.readInt();
                            animSeq = new int[length / 4];
                            for (int i = 0; i < length / 4; i++) {
                                animSeq[i] = leIn.readInt();
                            }
                            nextInt = leIn.readInt();
                        } else if (nextInt == 0x5453494c) { // Found a LIST
//                            System.out.println("we have 'LIST'.");
                            int length = leIn.readInt();
                            nextInt = leIn.readInt();
                            if (nextInt == 0x4f464e49) { // Got an INFO, skip its length
                                // this part consist  of Author, title, etc
                                leIn.skipBytes(length - 4);
//                                System.out.println(" Discarding INFO (skipped = " + skipped + ")");
                                nextInt = leIn.readInt();
                            } else if (nextInt == 0x6d617266) { // found a 'fram' for animation
//                                System.out.println("we have 'fram'.");
                                if (leIn.readInt() == 0x6e6f6369) { // we have 'icon'
                                    // We have an icon and from this point on
                                    // the rest is only icons.
                                    int icoLength = leIn.readInt();
                                    ciDat.numImages = numIcons;
                                    icons = new ArrayList<byte[]>(numIcons);
                                    for (int i = 0; i < numIcons; i++) {
                                        if (i > 0) {
                                            // skip 'icon' header and length as they are
                                            // known already and won't change.
                                            leIn.skipBytes(8);
                                        }
                                        byte[] data = new byte[icoLength];
                                        ((InputStream) leIn).read(data, 0, icoLength);
                                        // in case the header didn't have width or height
                                        // get it from first image.
                                        if (width == 0 || height == 0 && i == 1) {
                                            width = data[6];
                                            height = data[7];
                                        }
                                        icons.add(data);
                                    }
                                    // at this point we have the icons, rates (either
                                    // through jiffy or rate array, the sequence (if
                                    // applicable) and the ani header info.
                                    // Put things together.
                                    ciDat.assembleCursor(icons, rate, animSeq, jiffy, steps, width, height);
                                    ciDat.completeCursor();
                                    nextInt = leIn.readInt();
                                    // if for some reason there's JUNK (nextInt > -1)
                                    // bail out.
                                    nextInt = nextInt > -1 ? -1 : nextInt;
                                }
                            }
                        }
                    }
                }
                return setJmeCursor(ciDat);

            } else if (riff == 0x58464952) {
                throw new IllegalArgumentException("Big-Endian RIFX is not supported. Sorry.");
            } else {
                throw new IllegalArgumentException("Unknown format.");
            }
        } else if (isIco) {
            DataInputStream in = new DataInputStream(inStream);
            int bytesToRead;
            while ((bytesToRead = in.available()) != 0) {
                byte[] icoimage2 = new byte[icoimages.length + bytesToRead];
                System.arraycopy(icoimages, 0, icoimage2, 0, icoimages.length);
                in.read(icoimage2, icoimages.length, bytesToRead);
                icoimages = icoimage2;
            }
        }

        BufferedImage bi[] = parseICOImage(icoimages);
        CursorImageData cid = new CursorImageData(bi, 0, 0, 0, 0);
        cid.completeCursor();

        return setJmeCursor(cid);
    }

    private JmeCursor setJmeCursor(CursorImageData cid) {
        JmeCursor jmeCursor = new JmeCursor();

        // set cursor's params.
        jmeCursor.setWidth(cid.width);
        jmeCursor.setHeight(cid.height);
        jmeCursor.setxHotSpot(cid.xHotSpot);
        jmeCursor.setyHotSpot(cid.yHotSpot);
        jmeCursor.setNumImages(cid.numImages);
        jmeCursor.setImagesDelay(cid.imgDelay);
        jmeCursor.setImagesData(cid.data);
//        System.out.println("Width = " + cid.width);
//        System.out.println("Height = " + cid.height);
//        System.out.println("HSx = " + cid.xHotSpot);
//        System.out.println("HSy = " + cid.yHotSpot);
//        System.out.println("# img = " + cid.numImages);

        return jmeCursor;
    }

    private BufferedImage[] parseICOImage(byte[] icoimage) throws IOException {
        /*
         * Most of this is original code by Jeff Friesen at
         * http://www.informit.com/articles/article.aspx?p=1186882&seqNum=3
         */

        BufferedImage[] bi;
        // Check resource type field.
        int FDE_OFFSET = 6; // first directory entry offset
        int DE_LENGTH = 16; // directory entry length
        int BMIH_LENGTH = 40; // BITMAPINFOHEADER length

        if (icoimage[2] != 1 && icoimage[2] != 2 || icoimage[3] != 0) {
            throw new IllegalArgumentException("Bad data in ICO/CUR file. ImageType has to be either 1 or 2.");
        }

        int numImages = ubyte(icoimage[5]);
        numImages <<= 8;
        numImages |= icoimage[4];
        bi = new BufferedImage[numImages];
        int[] colorCount = new int[numImages];

        for (int i = 0; i < numImages; i++) {
            int width = ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH]);

            int height = ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 1]);

            colorCount[i] = ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 2]);

            int bytesInRes = ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 11]);
            bytesInRes <<= 8;
            bytesInRes |= ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 10]);
            bytesInRes <<= 8;
            bytesInRes |= ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 9]);
            bytesInRes <<= 8;
            bytesInRes |= ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 8]);

            int imageOffset = ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 15]);
            imageOffset <<= 8;
            imageOffset |= ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 14]);
            imageOffset <<= 8;
            imageOffset |= ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 13]);
            imageOffset <<= 8;
            imageOffset |= ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 12]);

            if (icoimage[imageOffset] == 40
                    && icoimage[imageOffset + 1] == 0
                    && icoimage[imageOffset + 2] == 0
                    && icoimage[imageOffset + 3] == 0) {
                // BITMAPINFOHEADER detected

                int _width = ubyte(icoimage[imageOffset + 7]);
                _width <<= 8;
                _width |= ubyte(icoimage[imageOffset + 6]);
                _width <<= 8;
                _width |= ubyte(icoimage[imageOffset + 5]);
                _width <<= 8;
                _width |= ubyte(icoimage[imageOffset + 4]);

                // If width is 0 (for 256 pixels or higher), _width contains
                // actual width.

                if (width == 0) {
                    width = _width;
                }

                int _height = ubyte(icoimage[imageOffset + 11]);
                _height <<= 8;
                _height |= ubyte(icoimage[imageOffset + 10]);
                _height <<= 8;
                _height |= ubyte(icoimage[imageOffset + 9]);
                _height <<= 8;
                _height |= ubyte(icoimage[imageOffset + 8]);

                // If height is 0 (for 256 pixels or higher), _height contains
                // actual height times 2.

                if (height == 0) {
                    height = _height >> 1; // Divide by 2.
                }
                int planes = ubyte(icoimage[imageOffset + 13]);
                planes <<= 8;
                planes |= ubyte(icoimage[imageOffset + 12]);

                int bitCount = ubyte(icoimage[imageOffset + 15]);
                bitCount <<= 8;
                bitCount |= ubyte(icoimage[imageOffset + 14]);

                // If colorCount [i] is 0, the number of colors is determined
                // from the planes and bitCount values. For example, the number
                // of colors is 256 when planes is 1 and bitCount is 8. Leave
                // colorCount [i] set to 0 when planes is 1 and bitCount is 32.

                if (colorCount[i] == 0) {
                    if (planes == 1) {
                        if (bitCount == 1) {
                            colorCount[i] = 2;
                        } else if (bitCount == 4) {
                            colorCount[i] = 16;
                        } else if (bitCount == 8) {
                            colorCount[i] = 256;
                        } else if (bitCount != 32) {
                            colorCount[i] = (int) Math.pow(2, bitCount);
                        }
                    } else {
                        colorCount[i] = (int) Math.pow(2, bitCount * planes);
                    }
                }

                bi[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

                // Parse image to image buffer.

                int colorTableOffset = imageOffset + BMIH_LENGTH;

                if (colorCount[i] == 2) {
                    int xorImageOffset = colorTableOffset + 2 * 4;

                    int scanlineBytes = calcScanlineBytes(width, 1);
                    int andImageOffset = xorImageOffset + scanlineBytes * height;

                    int[] masks = {128, 64, 32, 16, 8, 4, 2, 1};

                    for (int row = 0; row < height; row++) {
                        for (int col = 0; col < width; col++) {
                            int index;

                            if ((ubyte(icoimage[xorImageOffset + row
                                    * scanlineBytes + col / 8])
                                    & masks[col % 8]) != 0) {
                                index = 1;
                            } else {
                                index = 0;
                            }

                            int rgb = 0;
                            rgb |= (ubyte(icoimage[colorTableOffset + index * 4
                                    + 2]));
                            rgb <<= 8;
                            rgb |= (ubyte(icoimage[colorTableOffset + index * 4
                                    + 1]));
                            rgb <<= 8;
                            rgb |= (ubyte(icoimage[colorTableOffset + index
                                    * 4]));

                            if ((ubyte(icoimage[andImageOffset + row
                                    * scanlineBytes + col / 8])
                                    & masks[col % 8]) != 0) {
                                bi[i].setRGB(col, height - 1 - row, rgb);
                            } else {
                                bi[i].setRGB(col, height - 1 - row,
                                        0xff000000 | rgb);
                            }
                        }
                    }
                } else if (colorCount[i] == 16) {
                    int xorImageOffset = colorTableOffset + 16 * 4;

                    int scanlineBytes = calcScanlineBytes(width, 4);
                    int andImageOffset = xorImageOffset + scanlineBytes * height;

                    int[] masks = {128, 64, 32, 16, 8, 4, 2, 1};

                    for (int row = 0; row < height; row++) {
                        for (int col = 0; col < width; col++) {
                            int index;
                            if ((col & 1) == 0) // even
                            {
                                index = ubyte(icoimage[xorImageOffset + row
                                        * scanlineBytes + col / 2]);
                                index >>= 4;
                            } else {
                                index = ubyte(icoimage[xorImageOffset + row
                                        * scanlineBytes + col / 2])
                                        & 15;
                            }

                            int rgb = 0;
                            rgb |= (ubyte(icoimage[colorTableOffset + index * 4
                                    + 2]));
                            rgb <<= 8;
                            rgb |= (ubyte(icoimage[colorTableOffset + index * 4
                                    + 1]));
                            rgb <<= 8;
                            rgb |= (ubyte(icoimage[colorTableOffset + index
                                    * 4]));

                            if ((ubyte(icoimage[andImageOffset + row
                                    * calcScanlineBytes(width, 1)
                                    + col / 8]) & masks[col % 8])
                                    != 0) {
                                bi[i].setRGB(col, height - 1 - row, rgb);
                            } else {
                                bi[i].setRGB(col, height - 1 - row,
                                        0xff000000 | rgb);
                            }
                        }
                    }
                } else if (colorCount[i] == 256) {
                    int xorImageOffset = colorTableOffset + 256 * 4;

                    int scanlineBytes = calcScanlineBytes(width, 8);
                    int andImageOffset = xorImageOffset + scanlineBytes * height;

                    int[] masks = {128, 64, 32, 16, 8, 4, 2, 1};

                    for (int row = 0; row < height; row++) {
                        for (int col = 0; col < width; col++) {
                            int index;
                            index = ubyte(icoimage[xorImageOffset + row
                                    * scanlineBytes + col]);

                            int rgb = 0;
                            rgb |= (ubyte(icoimage[colorTableOffset + index * 4
                                    + 2]));
                            rgb <<= 8;
                            rgb |= (ubyte(icoimage[colorTableOffset + index * 4
                                    + 1]));
                            rgb <<= 8;
                            rgb |= (ubyte(icoimage[colorTableOffset + index * 4]));

                            if ((ubyte(icoimage[andImageOffset + row
                                    * calcScanlineBytes(width, 1)
                                    + col / 8]) & masks[col % 8])
                                    != 0) {
                                bi[i].setRGB(col, height - 1 - row, rgb);
                            } else {
                                bi[i].setRGB(col, height - 1 - row,
                                        0xff000000 | rgb);
                            }
                        }
                    }
                } else if (colorCount[i] == 0) {
                    int scanlineBytes = calcScanlineBytes(width, 32);

                    for (int row = 0; row < height; row++) {
                        for (int col = 0; col < width; col++) {
                            int rgb = ubyte(icoimage[colorTableOffset + row
                                    * scanlineBytes + col * 4 + 3]);
                            rgb <<= 8;
                            rgb |= ubyte(icoimage[colorTableOffset + row
                                    * scanlineBytes + col * 4 + 2]);
                            rgb <<= 8;
                            rgb |= ubyte(icoimage[colorTableOffset + row
                                    * scanlineBytes + col * 4 + 1]);
                            rgb <<= 8;
                            rgb |= ubyte(icoimage[colorTableOffset + row
                                    * scanlineBytes + col * 4]);

                            bi[i].setRGB(col, height - 1 - row, rgb);
                        }
                    }
                }
            } else if (ubyte(icoimage[imageOffset]) == 0x89
                    && icoimage[imageOffset + 1] == 0x50
                    && icoimage[imageOffset + 2] == 0x4e
                    && icoimage[imageOffset + 3] == 0x47
                    && icoimage[imageOffset + 4] == 0x0d
                    && icoimage[imageOffset + 5] == 0x0a
                    && icoimage[imageOffset + 6] == 0x1a
                    && icoimage[imageOffset + 7] == 0x0a) {
                // PNG detected

                ByteArrayInputStream bais;
                bais = new ByteArrayInputStream(icoimage, imageOffset,
                        bytesInRes);
                bi[i] = ImageIO.read(bais);
            } else {
                throw new IllegalArgumentException("Bad data in ICO/CUR file. BITMAPINFOHEADER or PNG "
                        + "expected");
            }
        }
        icoimage = null; // This array can now be garbage collected.

        return bi;
    }

    private int ubyte(byte b) {
        return (b < 0) ? 256 + b : b; // Convert byte to unsigned byte.
    }

    private int calcScanlineBytes(int width, int bitCount) {
        // Calculate minimum number of double-words required to store width
        // pixels where each pixel occupies bitCount bits. XOR and AND bitmaps
        // are stored such that each scanline is aligned on a double-word
        // boundary.

        return (((width * bitCount) + 31) / 32) * 4;
    }

    private int getNext(DataInput in) throws IOException {
        return in.readInt();
    }

    private class CursorImageData {

        int width;
        int height;
        int xHotSpot;
        int yHotSpot;
        int numImages;
        IntBuffer imgDelay;
        IntBuffer data;

        public CursorImageData() {
        }

        CursorImageData(BufferedImage[] bi, int delay, int hsX, int hsY, int curType) {
            // cursor type
            // 0 - Undefined (an array of images inside an ICO)
            // 1 - ICO
            // 2 - CUR
            IntBuffer singleCursor = null;
            ArrayList<IntBuffer> cursors = new ArrayList<IntBuffer>();
            int bwidth = 0;
            int bheight = 0;
            boolean multIcons = false;

            // make the cursor image
            for (int i = 0; i < bi.length; i++) {
                BufferedImage img = bi[i];
                bwidth = img.getWidth();
                bheight = img.getHeight();
                if (curType == 1) {
                    hsX = 0;
                    hsY = bheight - 1;
                } else if (curType == 2) {
                    if (hsY == 0) {
                        // make sure we flip if 0
                        hsY = bheight - 1;
                    }
                } else {
                    // We force to choose 32x32 icon from
                    // the array of icons in that ICO file.
                    if (bwidth != 32 && bheight != 32) {
                        multIcons = true;
                        continue;
                    } else {
                        if (img.getType() != 2) {
                            continue;
                        } else {
                            // force hotspot
                            hsY = bheight - 1;
                        }
                    }
                }

                // We flip our image because .ICO and .CUR will always be reversed.
                AffineTransform trans = AffineTransform.getScaleInstance(1, -1);
                trans.translate(0, -img.getHeight(null));
                AffineTransformOp op = new AffineTransformOp(trans, AffineTransformOp.TYPE_BILINEAR);
                img = op.filter(img, null);

                singleCursor = BufferUtils.createIntBuffer(img.getWidth() * img.getHeight());
                DataBufferInt dataIntBuf = (DataBufferInt) img.getData().getDataBuffer();
                singleCursor = IntBuffer.wrap(dataIntBuf.getData());
                cursors.add(singleCursor);
            }

            int count;
            if (multIcons) {
                bwidth = 32;
                bheight = 32;
                count = 1;
            } else {
                count = cursors.size();
            }
            // put the image in the IntBuffer
            data = BufferUtils.createIntBuffer(bwidth * bheight);
            imgDelay = BufferUtils.createIntBuffer(bi.length);
            for (int i = 0; i < count; i++) {
                data.put(cursors.get(i));
                if (delay > 0) {
                    imgDelay.put(delay);
                }
            }
            width = bwidth;
            height = bheight;
            xHotSpot = hsX;
            yHotSpot = hsY;
            numImages = count;
            data.rewind();
            if (imgDelay != null) {
                imgDelay.rewind();
            }
        }

        private void addFrame(byte[] imgData, int rate, int jiffy, int width, int height, int numSeq) throws IOException {
            BufferedImage bi[] = parseICOImage(imgData);
            int hotspotx = 0;
            int hotspoty = 0;
            int type = imgData[2] | imgData[3];
            if (type == 2) {
                // CUR type, hotspot might be stored.
                hotspotx = imgData[10] | imgData[11];
                hotspoty = imgData[12] | imgData[13];
            } else if (type == 1) {
                // ICO type, hotspot not stored. Put at 0, height - 1
                // because it's flipped.
                hotspotx = 0;
                hotspoty = height - 1;
            }
//            System.out.println("Image type = " + (type == 1 ? "CUR" : "ICO"));
            if (rate == 0) {
                rate = jiffy;
            }
            CursorImageData cid = new CursorImageData(bi, rate, hotspotx, hotspoty, type);
            if (width == 0) {
                this.width = cid.width;
            } else {
                this.width = width;
            }
            if (height == 0) {
                this.height = cid.height;
            } else {
                this.height = height;
            }
            if (data == null) {
                if (numSeq > numImages) {
                    data = BufferUtils.createIntBuffer(this.width * this.height * numSeq);
                } else {
                    data = BufferUtils.createIntBuffer(this.width * this.height * numImages);
                }
                data.put(cid.data);
            } else {
                data.put(cid.data);
            }
            if (imgDelay == null && (numImages > 1 || numSeq > 1)) {
                if (numSeq > numImages) {
                    imgDelay = BufferUtils.createIntBuffer(numSeq);
                } else {
                    imgDelay = BufferUtils.createIntBuffer(numImages);
                }
                imgDelay.put(cid.imgDelay);
            } else if (imgData != null) {
                imgDelay.put(cid.imgDelay);
            }
            xHotSpot = cid.xHotSpot;
            yHotSpot = cid.yHotSpot;
            cid = null;
        }

        void assembleCursor(ArrayList<byte[]> icons, int[] rate, int[] animSeq, int jiffy, int steps, int width, int height) throws IOException {
            // Jiffy multiplicator for LWJGL's delay, which is in milisecond.
            final int MULT = 17;
            numImages = icons.size();
            int frRate = 0;
            byte[] frame = new byte[0];
            // if we have an animation sequence we use that
            // since the sequence can be larger than the number
            // of images in the ani if it reuses one or more of those
            // images.
            if (animSeq != null && animSeq.length > 0) {
                for (int i = 0; i < animSeq.length; i++) {
                    if (rate != null) {
                        frRate = rate[i] * MULT;
                    } else {
                        frRate = jiffy * MULT;
                    }
                    // the frame # is the one in the animation sequence
                    frame = icons.get(animSeq[i]);
                    addFrame(frame, frRate, jiffy, width, height, animSeq.length);
//                    System.out.println("delay of " + frRate);
                }
            } else {
                for (int i = 0; i < icons.size(); i++) {
                    frame = icons.get(i);
                    if (rate == null) {
                        frRate = jiffy * MULT;
                    } else {
                        frRate = rate[i] * MULT;
                    }
                    addFrame(frame, frRate, jiffy, width, height, 0);
//                    System.out.println("delay of " + frRate);
                }
            }
        }

        /**
         * Called to rewind the buffers after filling them.
         */
        void completeCursor() {
            if (numImages == 1) {
                imgDelay = null;
            } else {
                imgDelay.rewind();
            }
            data.rewind();
        }
    }
}
