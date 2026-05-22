package jme3test.ios;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListenerAdapter;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.system.JmeSystem;
import java.util.ArrayList;
import java.util.List;

public final class IosTestChooser extends SimpleApplication implements ActionListener {
    private static final String SELECT_MAPPING = "IosTestChooserSelect";
    private static final float MIN_TEXT_SIZE = 10f;
    private static final long TAP_DEBOUNCE_NANOS = 180_000_000L;
    private static final ColorRGBA BACKGROUND_COLOR = new ColorRGBA(0.045f, 0.060f, 0.050f, 1f);
    private static final ColorRGBA EXAMPLE_BUTTON_COLOR = new ColorRGBA(0.135f, 0.305f, 0.145f, 1f);
    private static final ColorRGBA ACTION_BUTTON_COLOR = new ColorRGBA(0.760f, 0.360f, 0.060f, 1f);
    private static final ColorRGBA DISABLED_BUTTON_COLOR = new ColorRGBA(0.090f, 0.105f, 0.095f, 1f);
    private static final ColorRGBA SEARCH_BUTTON_COLOR = new ColorRGBA(0.070f, 0.085f, 0.075f, 1f);
    private static final ColorRGBA PRIMARY_TEXT_COLOR = new ColorRGBA(0.930f, 0.960f, 0.900f, 1f);
    private static final ColorRGBA MUTED_TEXT_COLOR = new ColorRGBA(0.500f, 0.560f, 0.480f, 1f);
    private static final ColorRGBA SEARCH_TEXT_COLOR = new ColorRGBA(0.765f, 0.910f, 0.555f, 1f);

    private final List<Button> buttons = new ArrayList<>();
    private int firstVisibleTest;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private long lastTapNanos;
    private String searchQuery = "";
    private Material buttonMaterial;
    private Material scrollMaterial;
    private Material disabledMaterial;
    private Material searchMaterial;
    private RawInputListenerAdapter keyboardListener;
    private boolean searchInputActive;

    @Override
    public void simpleInitApp() {
        setDisplayStatView(false);
        setDisplayFps(false);
        if (flyCam != null) {
            flyCam.setEnabled(false);
        }
        viewPort.setBackgroundColor(BACKGROUND_COLOR);
        buttonMaterial = material(EXAMPLE_BUTTON_COLOR);
        scrollMaterial = material(ACTION_BUTTON_COLOR);
        disabledMaterial = material(DISABLED_BUTTON_COLOR);
        searchMaterial = material(SEARCH_BUTTON_COLOR);
        inputManager.addMapping(SELECT_MAPPING, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, SELECT_MAPPING);
        keyboardListener = new RawInputListenerAdapter() {
            @Override
            public void onKeyEvent(KeyInputEvent evt) {
                handleSearchKey(evt);
            }

            @Override
            public void onTouchEvent(TouchEvent evt) {
                handleTouch(evt);
            }
        };
        inputManager.addRawInputListener(keyboardListener);
        rebuild();
    }

    @Override
    public void simpleUpdate(float tpf) {
        int width = cam.getWidth();
        int height = cam.getHeight();
        if (width != lastWidth || height != lastHeight) {
            rebuild();
        }
    }

    @Override
    public void onAction(String name, boolean pressed, float tpf) {
        if (!SELECT_MAPPING.equals(name) || !pressed) {
            return;
        }
        long now = System.nanoTime();
        if (!shouldAcceptTap(now)) {
            return;
        }
        Vector2f cursor = inputManager.getCursorPosition();
        activateAt(cursor.x, cursor.y);
    }

    private void handleTouch(TouchEvent evt) {
        if (evt.getType() == TouchEvent.Type.UP) {
            long now = System.nanoTime();
            if (shouldAcceptTap(now)) {
                activateAt(evt.getX(), evt.getY());
            }
        }
    }

    private void activateAt(float x, float y) {
        for (Button button : buttons) {
            if (x >= button.minX && x <= button.maxX && y >= button.minY && y <= button.maxY) {
                button.activate();
                return;
            }
        }
    }

    boolean shouldAcceptTap(long nowNanos) {
        if (nowNanos - lastTapNanos < TAP_DEBOUNCE_NANOS) {
            return false;
        }
        lastTapNanos = nowNanos;
        return true;
    }

    private void rebuild() {
        lastWidth = cam.getWidth();
        lastHeight = cam.getHeight();
        guiNode.detachAllChildren();
        buttons.clear();

        Layout layout = layout();
        float contentWidth = Math.max(180f, lastWidth - layout.margin * 2f);
        float searchY = lastHeight - layout.topInset - layout.searchHeight;
        addSearchControls(layout, contentWidth, searchY);

        float listTop = searchY - layout.gap;
        float listBottom = layout.bottomInset;
        float listHeight = Math.max(layout.minRowHeight * 2f + layout.gap, listTop - listBottom);
        float scrollWidth = Math.min(layout.scrollColumnWidth, contentWidth * 0.28f);
        float rowWidth = Math.max(160f, contentWidth - scrollWidth - layout.gap);
        float rowX = layout.margin;
        float scrollX = rowX + rowWidth + layout.gap;
        int visibleRows = Math.max(2, Math.min(8, (int) ((listHeight + layout.gap) / (layout.minRowHeight + layout.gap))));
        float rowHeight = Math.min(layout.maxRowHeight, (listHeight - layout.gap * (visibleRows - 1)) / visibleRows);
        List<String> testClasses = filteredTestClasses();
        int maxFirst = Math.max(0, testClasses.size() - visibleRows);
        firstVisibleTest = Math.min(firstVisibleTest, maxFirst);

        boolean canScrollUp = firstVisibleTest > 0;
        float scrollButtonHeight = (listHeight - layout.gap) * 0.5f;
        addButton("Up", scrollX, listBottom + scrollButtonHeight + layout.gap, scrollWidth, scrollButtonHeight,
                canScrollUp ? scrollMaterial : disabledMaterial,
                canScrollUp ? PRIMARY_TEXT_COLOR : MUTED_TEXT_COLOR,
                layout.scrollTextSize,
                () -> {
                    firstVisibleTest = Math.max(0, firstVisibleTest - visibleRows);
                    rebuild();
                },
                canScrollUp);

        float y = listTop - rowHeight;
        for (int i = 0; i < visibleRows && firstVisibleTest + i < testClasses.size(); i++) {
            int testIndex = firstVisibleTest + i;
            String className = testClasses.get(testIndex);
            addButton(labelFor(className), rowX, y, rowWidth, rowHeight, buttonMaterial, PRIMARY_TEXT_COLOR, layout.rowTextSize,
                    () -> IosTestChooserLauncher.selectForNextLaunch(className), true);
            y -= rowHeight + layout.gap;
        }

        boolean canScrollDown = firstVisibleTest < maxFirst;
        addButton("Down", scrollX, listBottom, scrollWidth, scrollButtonHeight,
                canScrollDown ? scrollMaterial : disabledMaterial,
                canScrollDown ? PRIMARY_TEXT_COLOR : MUTED_TEXT_COLOR,
                layout.scrollTextSize,
                () -> {
                    firstVisibleTest = Math.min(maxFirst, firstVisibleTest + visibleRows);
                    rebuild();
                },
                canScrollDown);
    }

    void prepareForHandoff() {
        inputManager.removeListener(this);
        if (keyboardListener != null) {
            inputManager.removeRawInputListener(keyboardListener);
            keyboardListener = null;
        }
        hideSearchKeyboard();
        buttons.clear();
        guiNode.detachAllChildren();
        rootNode.detachAllChildren();
        viewPort.clearScenes();
        guiViewPort.clearScenes();
    }

    private void addSearchControls(Layout layout, float contentWidth, float searchY) {
        float clearWidth = Math.min(96f, Math.max(64f, contentWidth * 0.12f));
        float deleteWidth = Math.min(78f, Math.max(54f, contentWidth * 0.095f));
        float searchWidth = Math.max(140f, contentWidth - clearWidth - deleteWidth - layout.gap * 2f);
        String label = searchQuery.isEmpty() ? "Search" : "Search: " + searchQuery;
        addButton(label, layout.margin, searchY, searchWidth, layout.searchHeight, searchMaterial, SEARCH_TEXT_COLOR,
                layout.searchTextSize, this::showSearchKeyboard, true);

        boolean hasQuery = !searchQuery.isEmpty();
        addButton("Del", layout.margin + searchWidth + layout.gap, searchY, deleteWidth, layout.searchHeight,
                hasQuery ? scrollMaterial : disabledMaterial,
                hasQuery ? PRIMARY_TEXT_COLOR : MUTED_TEXT_COLOR,
                layout.keyTextSize,
                () -> {
                    if (!searchQuery.isEmpty()) {
                        searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                        firstVisibleTest = 0;
                        rebuild();
                    }
                },
                hasQuery);
        addButton("Clear", layout.margin + searchWidth + deleteWidth + layout.gap * 2f, searchY, clearWidth,
                layout.searchHeight,
                hasQuery ? scrollMaterial : disabledMaterial,
                hasQuery ? PRIMARY_TEXT_COLOR : MUTED_TEXT_COLOR,
                layout.keyTextSize,
                () -> {
                    searchQuery = "";
                    firstVisibleTest = 0;
                    rebuild();
                },
                hasQuery);
    }

    private void handleSearchKey(KeyInputEvent evt) {
        if (!evt.isPressed()) {
            return;
        }
        if (isReturn(evt)) {
            if (searchInputActive) {
                hideSearchKeyboard();
            }
            return;
        }
        if (!searchInputActive) {
            return;
        }
        char typed = evt.getKeyChar();
        if (evt.getKeyCode() == KeyInput.KEY_BACK) {
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                firstVisibleTest = 0;
                rebuildIfReady();
            }
            return;
        }
        if (!isSearchCharacter(typed)) {
            return;
        }
        searchQuery += typed;
        firstVisibleTest = 0;
        rebuildIfReady();
    }

    private boolean isReturn(KeyInputEvent evt) {
        return evt.getKeyCode() == KeyInput.KEY_RETURN
                || evt.getKeyCode() == KeyInput.KEY_NUMPADENTER
                || evt.getKeyChar() == '\r'
                || evt.getKeyChar() == '\n';
    }

    private boolean isSearchCharacter(char typed) {
        return Character.isLetterOrDigit(typed)
                || typed == '.'
                || typed == '_'
                || typed == '$';
    }

    private void showSearchKeyboard() {
        searchInputActive = true;
        JmeSystem.showSoftKeyboard(true);
    }

    private void hideSearchKeyboard() {
        searchInputActive = false;
        JmeSystem.showSoftKeyboard(false);
    }

    private void rebuildIfReady() {
        if (cam != null && guiNode != null) {
            rebuild();
        }
    }

    private List<String> filteredTestClasses() {
        List<String> source = IosTestChooserLauncher.testClasses();
        if (searchQuery.isEmpty()) {
            return source;
        }
        String query = searchQuery.toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String className : source) {
            if (className.toLowerCase().contains(query) || labelFor(className).toLowerCase().contains(query)) {
                filtered.add(className);
            }
        }
        return filtered;
    }

    private void addButton(String label, float x, float y, float width, float height, Material material,
            ColorRGBA textColor, float textSize, Runnable action, boolean enabled) {
        Geometry background = new Geometry(label + "Button", new Quad(width, height));
        background.setMaterial(material);
        background.setQueueBucket(RenderQueue.Bucket.Gui);
        background.setLocalTranslation(x, y, 0f);
        guiNode.attachChild(background);

        float textPadding = Math.max(8f, Math.min(18f, width * 0.05f));
        BitmapText rowText = fittedText(label, textSize, textColor, width - textPadding * 2f);
        rowText.setLocalTranslation(x + textPadding, y + height * 0.5f + rowText.getLineHeight() * 0.35f, 1f);
        guiNode.attachChild(rowText);
        if (enabled) {
            buttons.add(new Button(x, x + width, y, y + height, action));
        }
    }

    private BitmapText fittedText(String value, float size, ColorRGBA color, float maxWidth) {
        BitmapText text = text(value, size, color);
        while (text.getLineWidth() > maxWidth && text.getSize() > MIN_TEXT_SIZE) {
            text.setSize(text.getSize() - 1f);
        }
        if (text.getLineWidth() <= maxWidth) {
            return text;
        }
        String trimmed = value;
        while (trimmed.length() > 4 && text.getLineWidth() > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 4) + "...";
            text.setText(trimmed);
        }
        return text;
    }

    private BitmapText text(String value, float size, ColorRGBA color) {
        BitmapText text = new BitmapText(guiFont);
        text.setText(value);
        text.setSize(size);
        text.setColor(color);
        return text;
    }

    private Layout layout() {
        float shortSide = Math.max(1f, Math.min(lastWidth, lastHeight));
        float margin = clamp(shortSide * 0.035f, 12f, 28f);
        float gap = clamp(shortSide * 0.025f, 7f, 13f);
        float topInset = clamp(shortSide * 0.035f, 10f, 30f);
        float bottomInset = clamp(shortSide * 0.04f, 12f, 34f);
        float searchHeight = clamp(shortSide * 0.125f, 42f, 68f);
        float minRowHeight = clamp(shortSide * 0.12f, 42f, 72f);
        float maxRowHeight = clamp(shortSide * 0.18f, 58f, 104f);
        float scrollColumnWidth = clamp(lastWidth * 0.12f, 70f, 116f);
        float baseText = clamp(shortSide * 0.048f, 16f, 28f);
        return new Layout(margin, gap, topInset, bottomInset, searchHeight,
                minRowHeight, maxRowHeight, scrollColumnWidth, baseText * 0.95f,
                baseText * 0.86f, baseText, baseText);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private String labelFor(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }

    private Material material(ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        return material;
    }

    private static final class Layout {
        private final float margin;
        private final float gap;
        private final float topInset;
        private final float bottomInset;
        private final float searchHeight;
        private final float minRowHeight;
        private final float maxRowHeight;
        private final float scrollColumnWidth;
        private final float rowTextSize;
        private final float keyTextSize;
        private final float searchTextSize;
        private final float scrollTextSize;

        private Layout(float margin, float gap, float topInset, float bottomInset, float searchHeight,
                float minRowHeight, float maxRowHeight, float scrollColumnWidth,
                float rowTextSize, float keyTextSize, float searchTextSize, float scrollTextSize) {
            this.margin = margin;
            this.gap = gap;
            this.topInset = topInset;
            this.bottomInset = bottomInset;
            this.searchHeight = searchHeight;
            this.minRowHeight = minRowHeight;
            this.maxRowHeight = maxRowHeight;
            this.scrollColumnWidth = scrollColumnWidth;
            this.rowTextSize = rowTextSize;
            this.keyTextSize = keyTextSize;
            this.searchTextSize = searchTextSize;
            this.scrollTextSize = scrollTextSize;
        }
    }

    private static final class Button {
        private final float minX;
        private final float maxX;
        private final float minY;
        private final float maxY;
        private final Runnable action;

        private Button(float minX, float maxX, float minY, float maxY, Runnable action) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.action = action;
        }

        private void activate() {
            action.run();
        }
    }
}
