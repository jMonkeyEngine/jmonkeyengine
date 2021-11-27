package com.jme3.asset;

import java.util.Scanner;

/**
 * An example implementation of {@link AssetLoader} to load text
 * files as strings.
 */
public class TextLoader implements AssetLoader {
    @Override
    public Object load(AssetInfo assetInfo) {
        Scanner scan = new Scanner(assetInfo.openStream());
        StringBuilder sb = new StringBuilder();
        try {
            while (scan.hasNextLine()) {
                sb.append(scan.nextLine()).append('\n');
            }
        } finally {
            scan.close();
        }
        return sb.toString();
    }
}
