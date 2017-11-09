package com.jme3.export.binary;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The default loader to load binaries files.
 *
 * @author JavaSaBr
 */
public class BinaryLoader implements AssetLoader {

    /**
     * The importers queue.
     */
    private final Deque<BinaryImporter> importers;

    public BinaryLoader() {
        importers = new ArrayDeque<>();
    }

    @Override
    public Object load(final AssetInfo assetInfo) throws IOException {

        BinaryImporter importer = importers.pollLast();

        if (importer == null) {
            importer = new BinaryImporter();
        }

        try {
            return importer.load(assetInfo);
        } finally {
            importers.addLast(importer);
        }
    }
}
