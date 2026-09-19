/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.input.virtual;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.font.BitmapText;
import com.jme3.input.virtual.VirtualJoystickTheme.TextureKey;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Virtual joystick controls layout.
 */
public abstract class VirtualJoystickLayout implements Savable {

    private final Map<String, Element> buttons = new LinkedHashMap<>();
    private final Map<String, Element> axisElements = new LinkedHashMap<>();
    private volatile float scale = 1.15f;
    private transient volatile boolean updateNeeded = true;
    private transient volatile Element[] buttonSnapshot = new Element[0];
    private transient volatile Element[] axisSnapshot = new Element[0];
    protected VirtualJoystickLayout() {
    }

    protected final synchronized void addButtonElement(String logicalId, String label, float x, float y, float size,
            TextureKey textureKey) {
        buttons.put(logicalId, new Element(logicalId, label, x, y, size, textureKey));
        layoutChanged();
    }

    protected final synchronized void addButtonElement(String logicalId, String label, float x, float y, float shortOffsetX,
            float shortOffsetY, float size, TextureKey textureKey) {
        buttons.put(logicalId, new Element(logicalId, label, x, y, size, textureKey)
                .setShortOffset(shortOffsetX, shortOffsetY));
        layoutChanged();
    }

    protected final synchronized void addButtonElement(String logicalId, String label, float x, float y, float shortOffsetX,
            float shortOffsetY, float size, TextureKey textureKey, TextureKey iconTextureKey) {
        buttons.put(logicalId, new Element(logicalId, label, x, y, size, textureKey)
                .setShortOffset(shortOffsetX, shortOffsetY)
                .setIconTextureKey(iconTextureKey));
        layoutChanged();
    }

    protected final synchronized void addButtonElement(String logicalId, String label, float x, float y, float size, float aspect,
            TextureKey textureKey) {
        buttons.put(logicalId, new Element(logicalId, label, x, y, size, textureKey).setAspect(aspect));
        layoutChanged();
    }

    protected final synchronized void addButtonElement(String logicalId, String label, float x, float y, float size, float aspect,
            TextureKey textureKey, TextureKey iconTextureKey) {
        buttons.put(logicalId, new Element(logicalId, label, x, y, size, textureKey)
                .setAspect(aspect)
                .setIconTextureKey(iconTextureKey));
        layoutChanged();
    }

    protected final synchronized void addAxisElement(String xAxisLogicalId, String yAxisLogicalId, String label, float x, float y,
            float size, TextureKey textureKey, TextureKey nubTextureKey) {
        axisElements.put(xAxisLogicalId, new Element(xAxisLogicalId, label, x, y, size, textureKey)
                .setYAxisLogicalId(yAxisLogicalId)
                .setNubTextureKey(nubTextureKey));
        layoutChanged();
    }

    protected final synchronized void setButtonPosition(String logicalId, float x, float y) {
        element(buttons, logicalId).setPosition(x, y);
        markUpdateNeeded();
    }

    public synchronized Vector2f getButtonPosition(String logicalId) {
        Element element = element(buttons, logicalId);
        return new Vector2f(element.positionX, element.positionY);
    }

    protected final synchronized void setButtonVisible(String logicalId, boolean visible) {
        Element element = element(buttons, logicalId);
        if (element.visible == visible) {
            return;
        }
        element.visible = visible;
        markUpdateNeeded();
    }

    public synchronized boolean isButtonVisible(String logicalId) {
        return element(buttons, logicalId).visible;
    }

    protected final synchronized void setAxisPosition(String logicalId, float x, float y) {
        axisElement(logicalId).setPosition(x, y);
        markUpdateNeeded();
    }

    public synchronized Vector2f getAxisPosition(String logicalId) {
        Element element = axisElement(logicalId);
        return new Vector2f(element.positionX, element.positionY);
    }

    protected final synchronized void setAxisVisible(String logicalId, boolean visible) {
        Element element = axisElement(logicalId);
        if (element.visible == visible) {
            return;
        }
        element.visible = visible;
        markUpdateNeeded();
    }

    public synchronized boolean isAxisVisible(String logicalId) {
        return axisElement(logicalId).visible;
    }

    protected final synchronized void setButtonSize(String logicalId, float size) {
        Element element = element(buttons, logicalId);
        element.size = FastMath.clamp(size, 0.01f, 1f);
        markUpdateNeeded();
    }

    protected final synchronized void setButtonTextureKey(String logicalId, TextureKey textureKey) {
        Element element = element(buttons, logicalId);
        element.textureKey = textureKey;
        markUpdateNeeded();
    }

    protected final synchronized void setButtonIconTextureKey(String logicalId, TextureKey iconTextureKey) {
        Element element = element(buttons, logicalId);
        element.iconTextureKey = iconTextureKey;
        markUpdateNeeded();
    }

    protected final synchronized void setAxisSize(String logicalId, float size) {
        Element element = axisElement(logicalId);
        element.size = FastMath.clamp(size, 0.01f, 1f);
        markUpdateNeeded();
    }

    protected final synchronized void setAxisTextureKey(String logicalId, TextureKey textureKey) {
        Element element = axisElement(logicalId);
        element.textureKey = textureKey;
        markUpdateNeeded();
    }

    protected final synchronized void setAxisNubTextureKey(String logicalId, TextureKey nubTextureKey) {
        Element element = axisElement(logicalId);
        element.nubTextureKey = nubTextureKey;
        markUpdateNeeded();
    }

    protected final synchronized void setScale(float scale) {
        this.scale = FastMath.clamp(scale, 0.25f, 3f);
        markUpdateNeeded();
    }

    public float getScale() {
        return scale;
    }

    boolean isUpdateNeeded() {
        return updateNeeded;
    }

    void markUpdateNeeded() {
        updateNeeded = true;
    }

    void clearUpdateNeeded() {
        updateNeeded = false;
    }

    Element[] getButtons() {
        return buttonSnapshot;
    }

    Element[] getAxisElements() {
        return axisSnapshot;
    }

    public void update(VirtualJoystick joystick) {
    }

    public synchronized Element getButtonElement(String logicalId) {
        return buttons.get(logicalId);
    }

    public synchronized Element getAxisElement(String logicalId) {
        return findAxisElement(logicalId);
    }

    private Element axisElement(String logicalId) {
        Element element = findAxisElement(logicalId);
        if (element == null) {
            throw new IllegalArgumentException("Unknown virtual joystick axis element: " + logicalId);
        }
        return element;
    }

    private Element findAxisElement(String logicalId) {
        Element element = axisElements.get(logicalId);
        if (element != null) {
            return element;
        }
        for (Element axisElement : axisElements.values()) {
            if (logicalId.equals(axisElement.yAxisLogicalId)) {
                return axisElement;
            }
        }
        return null;
    }

    private Element element(Map<String, Element> elements, String logicalId) {
        Element element = elements.get(logicalId);
        if (element == null) {
            throw new IllegalArgumentException("Unknown virtual joystick element: " + logicalId);
        }
        return element;
    }

    @Override
    public synchronized void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(scale, "scale", 1.15f);
        capsule.write(buttons.values().toArray(new Element[0]), "buttons", null);
        capsule.write(axisElements.values().toArray(new Element[0]), "axes", null);
    }

    @Override
    public synchronized void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        scale = capsule.readFloat("scale", 1.15f);
        buttons.clear();
        axisElements.clear();
        Savable[] readButtons = capsule.readSavableArray("buttons", null);
        if (readButtons != null) {
            for (Savable savable : readButtons) {
                Element element = (Element) savable;
                buttons.put(element.id, element);
            }
        }
        Savable[] readAxes = capsule.readSavableArray("axes", null);
        if (readAxes != null) {
            for (Savable savable : readAxes) {
                Element element = (Element) savable;
                axisElements.put(element.id, element);
            }
        }
        rebuildSnapshots();
        markUpdateNeeded();
    }

    private void layoutChanged() {
        rebuildSnapshots();
        markUpdateNeeded();
    }

    private void rebuildSnapshots() {
        buttonSnapshot = buttons.values().toArray(new Element[0]);
        axisSnapshot = axisElements.values().toArray(new Element[0]);
    }

    public static class Element implements Savable {
        private static final ColorRGBA DEFAULT_COLOR = new ColorRGBA(1f, 1f, 1f, 0.72f);

        volatile String id;
        volatile String label;
        volatile String yAxisLogicalId;
        volatile float positionX;
        volatile float positionY;
        volatile TextureKey textureKey;
        volatile TextureKey nubTextureKey;
        volatile TextureKey iconTextureKey;
        volatile float size;
        volatile float aspect = 1f;
        volatile float shortOffsetX;
        volatile float shortOffsetY;
        volatile boolean visible = true;
        private transient volatile Bounds bounds = Bounds.EMPTY;
        transient volatile float nubX;
        transient volatile float nubY;
        transient Node node;
        transient Picture base;
        transient Picture nub;
        transient Picture icon;
        transient BitmapText text;
        private transient boolean baseGeometrySynced;
        private transient boolean baseColorSynced;
        private transient boolean nubGeometrySynced;
        private transient boolean iconGeometrySynced;
        private transient boolean iconColorSynced;
        private transient boolean textGeometrySynced;
        private transient boolean textColorSynced;
        private transient boolean nodePositionSynced;
        private transient boolean lastPressed;
        private transient float lastBaseWidth;
        private transient float lastBaseHeight;
        private transient float lastBaseX;
        private transient float lastBaseY;
        private transient float lastNubSize;
        private transient float lastNubX;
        private transient float lastNubY;
        private transient float lastIconSize;
        private transient float lastTextSize;
        private transient float lastNodeX;
        private transient float lastNodeY;

        public Element() {
        }

        public Element(String id, String label, float x, float y, float size, TextureKey textureKey) {
            this.id = id;
            this.label = label == null ? "" : label;
            this.size = size;
            this.textureKey = textureKey;
            setPosition(x, y);
        }

        synchronized Element setPosition(float x, float y) {
            positionX = FastMath.clamp(x, 0f, 1f);
            positionY = FastMath.clamp(y, 0f, 1f);
            shortOffsetX = 0f;
            shortOffsetY = 0f;
            return this;
        }

        synchronized Element setShortOffset(float x, float y) {
            shortOffsetX = x;
            shortOffsetY = y;
            return this;
        }

        synchronized Element setAspect(float aspect) {
            this.aspect = aspect;
            return this;
        }

        synchronized Element setYAxisLogicalId(String yAxisLogicalId) {
            this.yAxisLogicalId = yAxisLogicalId;
            return this;
        }

        synchronized Element setNubTextureKey(TextureKey nubTextureKey) {
            this.nubTextureKey = nubTextureKey;
            return this;
        }

        synchronized Element setIconTextureKey(TextureKey iconTextureKey) {
            this.iconTextureKey = iconTextureKey;
            return this;
        }

        boolean contains(float x, float y) {
            Bounds current = bounds;
            return Math.abs(x - current.x) <= current.width * 0.5f
                    && Math.abs(y - current.y) <= current.height * 0.5f;
        }

        void copyBoundsTo(BoundsSnapshot target) {
            Bounds current = bounds;
            target.x = current.x;
            target.y = current.y;
            target.size = current.size;
            target.width = current.width;
            target.height = current.height;
        }

        void sync(int width, int height, float scale, boolean pressed) {
            float shortSide = Math.min(width, height);
            float scaledShortSide = shortSide * scale;
            float pixelSize = Math.max(scaledShortSide * size, 1f);
            float pixelWidth = pixelSize * aspect;
            float pixelHeight = pixelSize;
            float pixelX = positionX * width + shortOffsetX * scaledShortSide;
            float pixelY = positionY * height + shortOffsetY * scaledShortSide;
            if (pixelWidth < width) {
                pixelX = FastMath.clamp(pixelX, pixelWidth * 0.5f, width - pixelWidth * 0.5f);
            } else {
                pixelX = width * 0.5f;
            }
            if (pixelHeight < height) {
                pixelY = FastMath.clamp(pixelY, pixelHeight * 0.5f, height - pixelHeight * 0.5f);
            } else {
                pixelY = height * 0.5f;
            }
            float baseX = -pixelWidth * 0.5f;
            float baseY = -pixelHeight * 0.5f;
            if (!baseGeometrySynced || lastBaseWidth != pixelWidth) {
                base.setWidth(pixelWidth);
                lastBaseWidth = pixelWidth;
            }
            if (!baseGeometrySynced || lastBaseHeight != pixelHeight) {
                base.setHeight(pixelHeight);
                lastBaseHeight = pixelHeight;
            }
            if (!baseGeometrySynced || lastBaseX != baseX || lastBaseY != baseY) {
                base.setPosition(baseX, baseY);
                lastBaseX = baseX;
                lastBaseY = baseY;
            }
            baseGeometrySynced = true;
            if (!baseColorSynced || lastPressed != pressed) {
                setColor(base, pressed ? ColorRGBA.White : DEFAULT_COLOR);
                lastPressed = pressed;
                baseColorSynced = true;
            }

            if (nub != null) {
                float nubSize = pixelHeight * 0.42f;
                float nubPixelX = (nubX * pixelHeight * 0.32f) - nubSize * 0.5f;
                float nubPixelY = (nubY * pixelHeight * 0.32f) - nubSize * 0.5f;
                if (!nubGeometrySynced || lastNubSize != nubSize) {
                    nub.setWidth(nubSize);
                    nub.setHeight(nubSize);
                    lastNubSize = nubSize;
                }
                if (!nubGeometrySynced || lastNubX != nubPixelX || lastNubY != nubPixelY) {
                    nub.setPosition(nubPixelX, nubPixelY);
                    lastNubX = nubPixelX;
                    lastNubY = nubPixelY;
                }
                nubGeometrySynced = true;
            }

            if (icon != null) {
                float iconSize = pixelHeight * (aspect > 1f ? 0.38f : 0.46f);
                if (!iconGeometrySynced || lastIconSize != iconSize) {
                    icon.setWidth(iconSize);
                    icon.setHeight(iconSize);
                    icon.setPosition(-iconSize * 0.5f, -iconSize * 0.5f);
                    lastIconSize = iconSize;
                    iconGeometrySynced = true;
                }
                if (!iconColorSynced) {
                    setColor(icon, ColorRGBA.White);
                    iconColorSynced = true;
                }
            }

            if (text != null) {
                float textSize = pixelHeight * (label.length() > 2 ? 0.2f : 0.32f);
                if (!textGeometrySynced || lastTextSize != textSize) {
                    text.setSize(textSize);
                    lastTextSize = textSize;
                    text.setLocalTranslation(-text.getLineWidth() * 0.5f,
                            text.getLineHeight() * 0.48f, 1f);
                    textGeometrySynced = true;
                }
                if (!textColorSynced) {
                    text.setColor(ColorRGBA.White);
                    textColorSynced = true;
                }
            }

            if (!nodePositionSynced || lastNodeX != pixelX || lastNodeY != pixelY) {
                node.setLocalTranslation(pixelX, pixelY, 0f);
                lastNodeX = pixelX;
                lastNodeY = pixelY;
                nodePositionSynced = true;
            }
            Bounds current = bounds;
            if (current.x != pixelX || current.y != pixelY || current.size != pixelSize
                    || current.width != pixelWidth || current.height != pixelHeight) {
                publishBounds(pixelX, pixelY, pixelSize, pixelWidth, pixelHeight);
            }
        }

        synchronized void clearVisuals() {
            if (node != null) {
                node.removeFromParent();
            }
            node = null;
            base = null;
            nub = null;
            icon = null;
            text = null;
            publishBounds(0f, 0f, 0f, 0f, 0f);
            clearSyncState();
        }

        private void setColor(Picture picture, ColorRGBA color) {
            if (picture.getMaterial() != null) {
                picture.getMaterial().setColor("Color", color);
            }
        }

        private void clearSyncState() {
            baseGeometrySynced = false;
            baseColorSynced = false;
            nubGeometrySynced = false;
            iconGeometrySynced = false;
            iconColorSynced = false;
            textGeometrySynced = false;
            textColorSynced = false;
            nodePositionSynced = false;
        }

        private void publishBounds(float x, float y, float size, float width, float height) {
            bounds = x == 0f && y == 0f && size == 0f && width == 0f && height == 0f
                    ? Bounds.EMPTY
                    : new Bounds(x, y, size, width, height);
        }

        private static final class Bounds {
            static final Bounds EMPTY = new Bounds(0f, 0f, 0f, 0f, 0f);

            final float x;
            final float y;
            final float size;
            final float width;
            final float height;

            Bounds(float x, float y, float size, float width, float height) {
                this.x = x;
                this.y = y;
                this.size = size;
                this.width = width;
                this.height = height;
            }
        }

        static final class BoundsSnapshot {
            float x;
            float y;
            float size;
            float width;
            float height;
        }

        @Override
        public synchronized void write(JmeExporter ex) throws IOException {
            OutputCapsule capsule = ex.getCapsule(this);
            capsule.write(id, "id", null);
            capsule.write(label, "label", "");
            capsule.write(yAxisLogicalId, "yAxisLogicalId", null);
            capsule.write(new Vector2f(positionX, positionY), "position", null);
            capsule.write(textureKey, "textureKey", null);
            capsule.write(nubTextureKey, "nubTextureKey", null);
            capsule.write(iconTextureKey, "iconTextureKey", null);
            capsule.write(size, "size", 0f);
            capsule.write(aspect, "aspect", 1f);
            capsule.write(shortOffsetX, "shortOffsetX", 0f);
            capsule.write(shortOffsetY, "shortOffsetY", 0f);
            capsule.write(visible, "visible", true);
        }

        @Override
        public synchronized void read(JmeImporter im) throws IOException {
            InputCapsule capsule = im.getCapsule(this);
            id = capsule.readString("id", null);
            label = capsule.readString("label", "");
            yAxisLogicalId = capsule.readString("yAxisLogicalId", null);
            Vector2f position = (Vector2f) capsule.readSavable("position", new Vector2f());
            positionX = position.x;
            positionY = position.y;
            textureKey = capsule.readEnum("textureKey", TextureKey.class, null);
            nubTextureKey = capsule.readEnum("nubTextureKey", TextureKey.class, null);
            iconTextureKey = capsule.readEnum("iconTextureKey", TextureKey.class, null);
            size = capsule.readFloat("size", 0f);
            aspect = capsule.readFloat("aspect", 1f);
            shortOffsetX = capsule.readFloat("shortOffsetX", 0f);
            shortOffsetY = capsule.readFloat("shortOffsetY", 0f);
            visible = capsule.readBoolean("visible", true);
        }
    }
}
