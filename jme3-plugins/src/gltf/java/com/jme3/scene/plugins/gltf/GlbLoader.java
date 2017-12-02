package com.jme3.scene.plugins.gltf;

import com.jme3.asset.AssetInfo;
import com.jme3.util.LittleEndien;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by Nehon on 12/09/2017.
 */
public class GlbLoader extends GltfLoader {

    private static final int GLTF_MAGIC = 0x46546C67;
    private static final int JSON_TYPE = 0x4E4F534A;
    private static final int BIN_TYPE = 0x004E4942;
    private ArrayList<byte[]> data = new ArrayList<>();

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        LittleEndien stream = new LittleEndien(new DataInputStream(assetInfo.openStream()));
        int magic = stream.readInt();
        int version = stream.readInt();
        int length = stream.readInt();
        System.err.println(magic == GLTF_MAGIC ? "gltf" : "no no no");
        System.err.println(version);
        System.err.println(length);

        byte[] json = null;

        //length is the total size, we have to remove the header size (3 integers = 12 bytes).
        length -= 12;

        while (length > 0) {
            int chunkLength = stream.readInt();
            int chunkType = stream.readInt();
            if (chunkType == JSON_TYPE) {
                json = new byte[chunkLength];
                stream.read(json);
                System.err.println(new String(json));
            } else {
                byte[] bin = new byte[chunkLength];
                stream.read(bin);
                data.add(bin);
            }
            //8 is the byte size of the 2 ints chunkLength and chunkType.
            length -= chunkLength + 8;
        }

        return loadFromStream(assetInfo, new ByteArrayInputStream(json));
    }

    @Override
    protected byte[] getBytes(int bufferIndex, String uri, Integer bufferLength) throws IOException {
        return data.get(bufferIndex);
    }

}
