package com.jme3.scene.plugins.fbx;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.ModelKey;

public class SceneWithAnimationLoader implements AssetLoader {

    private Pattern splitStrings = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AssetKey<?> key = assetInfo.getKey();
        if(!(key instanceof ModelKey))
            throw new AssetLoadException("Invalid asset key");
        InputStream stream = assetInfo.openStream();
        Scanner scanner = new Scanner(stream);
        AnimationList animList = new AnimationList();
        String modelName = null;
        try {
            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if(line.startsWith("#"))
                    continue;
                if(modelName == null) {
                    modelName = line;
                    continue;
                }
                String[] split = split(line);
                if(split.length < 3)
                    throw new IOException("Unparseable string \"" + line + "\"");
                int start;
                int end;
                try {
                    start = Integer.parseInt(split[0]);
                    end = Integer.parseInt(split[1]);
                } catch(NumberFormatException e) {
                    throw new IOException("Unparseable string \"" + line + "\"", e);
                }
                animList.add(split[2], split.length > 3 ? split[3] : null, start, end);
            }
        } finally {
            scanner.close();
            stream.close();
        }
        return assetInfo.getManager().loadAsset(new SceneKey(key.getFolder() + modelName, animList));
    }

    private String[] split(String src) {
        List<String> list = new ArrayList<>();
        Matcher m = splitStrings.matcher(src);
        while(m.find())
            list.add(m.group(1).replace("\"", ""));
        return list.toArray(new String[list.size()]);
    }

}
