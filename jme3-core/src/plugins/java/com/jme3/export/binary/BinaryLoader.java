package com.jme3.export.binary;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The default loader to load binaries files.
 *
 * @author JavaSaBr
 */
public class BinaryLoader implements JmeImporter {

    /**
     * The thread local importers.
     */
    private final ThreadLocal<Deque<BinaryImporter>> threadLocalImporters;

    /**
     * The current importer.
     */
    private final ThreadLocal<BinaryImporter> currentImporter;

    public BinaryLoader() {
        currentImporter = new ThreadLocal<>();
        threadLocalImporters = new ThreadLocal<Deque<BinaryImporter>>() {

            @Override
            protected Deque<BinaryImporter> initialValue() {
                return new ArrayDeque<>();
            }
        };
    }

    @Override
    public InputCapsule getCapsule(final Savable id) {
        final BinaryImporter importer = currentImporter.get();
        return importer.getCapsule(id);
    }

    @Override
    public AssetManager getAssetManager() {
        final BinaryImporter importer = currentImporter.get();
        return importer.getAssetManager();
    }

    @Override
    public int getFormatVersion() {
        final BinaryImporter importer = currentImporter.get();
        return importer.getFormatVersion();
    }

    @Override
    public Object load(final AssetInfo assetInfo) throws IOException {

        final Deque<BinaryImporter> importers = threadLocalImporters.get();
        BinaryImporter importer = importers.pollLast();

        if (importer == null) {
            importer = new BinaryImporter();
        }

        final BinaryImporter prev = currentImporter.get();
        currentImporter.set(importer);
        try {
            return importer.load(assetInfo);
        } finally {
            importers.addLast(importer);
            currentImporter.set(prev);
        }
    }
}
