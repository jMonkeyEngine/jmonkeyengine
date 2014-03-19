package com.jme3.gde.textureeditor;

import java.awt.Graphics2D;

public interface EditorTool {

    public void install(EditorToolTarget t);

    public void uninstall(EditorToolTarget t);

    public void drawTrack(Graphics2D g, int width, int height, float scaleX, float scaleY);

}
