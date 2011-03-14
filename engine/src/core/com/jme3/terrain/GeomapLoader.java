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

package com.jme3.terrain;
//
//import java.awt.Graphics2D;
//import java.awt.image.BufferedImage;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URL;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.IntBuffer;
//import javax.imageio.ImageIO;
//
//public class GeomapLoader {
//
//    /**
//     * Loads a heightmap from the image
//     *
//     * The returned IntBuffer has heights ranging from 0 to 2^16
//     */
//    protected static IntBuffer loadHeightmapImage(BufferedImage bi, boolean invertY){
//        BufferedImage hm = null;
//
//        if (bi.getType()!=BufferedImage.TYPE_USHORT_GRAY || invertY){
//            hm = new BufferedImage(bi.getWidth(),bi.getHeight(),BufferedImage.TYPE_USHORT_GRAY);
//
//            Graphics2D g = hm.createGraphics();
//            if (invertY){
//               g.scale(1d,-1d);
//               g.translate(0, -bi.getHeight());
//            }
//            g.drawImage(bi,null,0,0);
//            g.dispose();
//        }else{
//            hm = bi;
//        }
//
//        IntBuffer ib = BufferUtils.createIntBuffer(bi.getWidth()*bi.getHeight());
//        short[] data = (short[]) hm.getData().getDataElements(0,0,bi.getWidth(),bi.getHeight(),null);
//        for (int i = 0; i < hm.getWidth()*hm.getHeight(); i++){
//            //System.out.println(data[i] & 0xFFFF);
////            ib.put( (((data[i] & 0xFF00) >> 8) | ((data[i] & 0x00FF) << 8)) );
//            ib.put( data[i] & 0xFFFF );
//        }
//
//        //for (int y = 0; y < hm.getHeight(); y++){
//        //    for (int x = 0; x < hm.getWidth(); x++){
//        //        ib.put(db.getElem
//        //    }
//        //}
//        ib.flip();
//
//        return ib;
//    }
//
//    protected static IntBuffer loadHeightmapRAW(ByteBuffer raw, int width, int height, ByteOrder order, int bitsize){
//        raw.order(order);
//
//        if (bitsize == 32){
//            // directly map to heightmap
//            raw = raw.duplicate();
//            raw.limit(width * height * 4);
//            return raw.asIntBuffer();
//        }
//
//        ByteBuffer target = ByteBuffer.allocateDirect(width * height * 4);
//        switch (bitsize){
//            case 8:
//                for (int i = 0, len = width*height; i < len; i++){
//                    target.putInt(raw.get() & 0xFF);
//                }
//                break;
//            case 16:
//                for (int i = 0, len = width*height; i < len; i++){
//                    target.putInt(raw.getShort() & 0xFFFF);
//                }
//                break;
//            case 24:
//                //for (int i = 0, len = width*height; i < len; i++){
//                //}
//                throw new UnsupportedOperationException("24 bitsize is not supported!");
//            case 32:
//                //for (int i = 0, len = width*height; i < len; i++){
//                //    target.putInt(raw.getInt() & 0xFFFFFFFF);
//                //}
//                target.put(raw);
//                break;
//        }
//        target.flip();
//
//        return target.asIntBuffer();
//    }
//
//    public static Geomap fromRaw(URL rawURL, int width, int height, int bitsize) throws IOException{
//        InputStream in = rawURL.openStream();
//
//        // read bytes from stream
//        byte[] data = new byte[width * height * bitsize / 8];
//        in.read(data);
//
//        // convert to bytebuffer
//        ByteBuffer bb = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
//        bb.put(data);
//        bb.flip();
//
//        IntBuffer ib = loadHeightmapRAW(bb, width, height, ByteOrder.BIG_ENDIAN, bitsize);
//        return new BufferGeomap(ib, null, width, height, (int)Math.pow(2, bitsize));
//    }
//
//    public static Geomap fromImage(URL imageURL) throws IOException {
//        BufferedImage image = ImageIO.read(imageURL);
//        IntBuffer ib = loadHeightmapImage(image, false);
//        // maximum value is of unsigned short, because loadHeightmapImage
//        // converts the image into a TYPE_USHORT_GRAY before reading in the
//        // values.
//        return new BufferGeomap(ib, null, image.getWidth(), image.getHeight(), 65536);
//    }
//
//}
