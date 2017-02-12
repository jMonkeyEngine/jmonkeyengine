package com.jme3.app;

import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.*;
import com.jme3.input.controls.*;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.profile.AppStep;
import com.jme3.scene.*;
import com.jme3.scene.shape.Quad;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Created by Nehon on 25/01/2017.
 */
public class DetailedProfilerState extends BaseAppState {

    private static final int PANEL_WIDTH = 400;
    private static final int PADDING = 10;
    private static final int LINE_HEIGHT = 12;
    private static final int HEADER_HEIGHT = 100;
    private static final float REFRESH_TIME = 1.0f;
    private static final String TOGGLE_KEY = "Toggle_Detailed_Profiler";
    private static final String CLICK_KEY = "Click_Detailed_Profiler";
    private static final String INSIGNIFICANT = "Hide insignificant stat";
    private DetailedProfiler prof = new DetailedProfiler();

    private float time = 0;
    private BitmapFont font;
    private BitmapFont bigFont;
    private Node ui = new Node("Stats ui");
    private Map<String, StatLineView> lines = new HashMap<>();
    private double totalTimeCpu;
    private double totalTimeGpu;
    private int maxLevel = 0;

    private BitmapText frameTimeValue;
    private BitmapText frameCpuTimeValue;
    private BitmapText frameGpuTimeValue;
    private BitmapText hideInsignificantField;

    private BitmapText selectedField;
    private double selectedValueCpu = 0;
    private double selectedValueGpu = 0;
    private boolean hideInsignificant = false;

    private StatLineView rootLine;
    private int height = 0;
    private DecimalFormat df = new DecimalFormat("##0.00", new DecimalFormatSymbols(Locale.US));

    private ColorRGBA dimmedWhite = ColorRGBA.White.mult(0.7f);
    private ColorRGBA dimmedGreen = ColorRGBA.Green.mult(0.7f);
    private ColorRGBA dimmedOrange = ColorRGBA.Orange.mult(0.7f);
    private ColorRGBA dimmedRed = ColorRGBA.Red.mult(0.7f);

    public DetailedProfilerState() {

    }

    @Override
    protected void initialize(Application app) {
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0, 0, 0, 0.5f));
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        Geometry darkenStats = new Geometry("StatsDarken", new Quad(PANEL_WIDTH, app.getCamera().getHeight()));
        darkenStats.setMaterial(mat);
        darkenStats.setLocalTranslation(0, -app.getCamera().getHeight(), -1);

        ui.attachChild(darkenStats);
        ui.setLocalTranslation(app.getCamera().getWidth() - PANEL_WIDTH, app.getCamera().getHeight(), 0);
        font = app.getAssetManager().loadFont("Interface/Fonts/Console.fnt");
        bigFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        prof.setRenderer(app.getRenderer());
        rootLine = new StatLineView("Frame");
        rootLine.attachTo(ui);

        BitmapText frameLabel = new BitmapText(bigFont);
        frameLabel.setText("Total Frame Time: ");
        ui.attachChild(frameLabel);
        frameLabel.setLocalTranslation(new Vector3f(PANEL_WIDTH / 2 - bigFont.getLineWidth(frameLabel.getText()), -PADDING, 0));

        BitmapText cpuLabel = new BitmapText(bigFont);
        cpuLabel.setText("CPU");
        ui.attachChild(cpuLabel);
        cpuLabel.setLocalTranslation(PANEL_WIDTH / 4 - bigFont.getLineWidth(cpuLabel.getText()) / 2, -PADDING - 30, 0);

        BitmapText gpuLabel = new BitmapText(bigFont);
        gpuLabel.setText("GPU");
        ui.attachChild(gpuLabel);
        gpuLabel.setLocalTranslation(3 * PANEL_WIDTH / 4 - bigFont.getLineWidth(gpuLabel.getText()) / 2, -PADDING - 30, 0);

        frameTimeValue = new BitmapText(bigFont);
        frameCpuTimeValue = new BitmapText(bigFont);
        frameGpuTimeValue = new BitmapText(bigFont);

        selectedField = new BitmapText(font);
        selectedField.setText("Selected: ");
        selectedField.setLocalTranslation(PANEL_WIDTH / 2, -PADDING - 75, 0);
        selectedField.setColor(ColorRGBA.Yellow);


        ui.attachChild(frameTimeValue);
        ui.attachChild(frameCpuTimeValue);
        ui.attachChild(frameGpuTimeValue);
        ui.attachChild(selectedField);

        hideInsignificantField = new BitmapText(font);
        hideInsignificantField.setText("O " + INSIGNIFICANT);
        hideInsignificantField.setLocalTranslation(PADDING, -PADDING - 75, 0);
        ui.attachChild(hideInsignificantField);

        final InputManager inputManager = app.getInputManager();
        if (inputManager != null) {
            inputManager.addMapping(TOGGLE_KEY, new KeyTrigger(KeyInput.KEY_F6));
            inputManager.addMapping(CLICK_KEY, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
            inputManager.addListener(new ActionListener() {
                @Override
                public void onAction(String name, boolean isPressed, float tpf) {
                    if (name.equals(TOGGLE_KEY) && isPressed) {
                        setEnabled(!isEnabled());
                    }
                    if (isEnabled() && name.equals(CLICK_KEY) && isPressed) {
                        handleClick(inputManager.getCursorPosition());
                    }
                }
            }, TOGGLE_KEY, CLICK_KEY);
        }
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    public void update(float tpf) {
        time += tpf;
    }

    private void displayData(Map<String, DetailedProfiler.StatLine> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        for (StatLineView statLine : lines.values()) {
            statLine.reset();
            statLine.removeFromParent();
        }
        rootLine.reset();
        maxLevel = 0;
        for (String path : data.keySet()) {
            if (path.equals("EndFrame")) {
                continue;
            }
            maxLevel = Math.max(maxLevel, path.split("/").length);
            StatLineView line = getStatLineView(path);
            DetailedProfiler.StatLine statLine = data.get(path);
            line.updateValues(statLine.getAverageCpu(), statLine.getAverageGpu());
            String parent = getParent(path);
            while (parent != null) {
                StatLineView parentView = getStatLineView(parent);
                parentView.updateValues(statLine.getAverageCpu(), statLine.getAverageGpu());
                parentView.children.add(line);
                line.attachTo(ui);
                line = parentView;
                parent = getParent(parent);
            }
            rootLine.children.add(line);
            line.attachTo(ui);
            rootLine.updateValues(statLine.getAverageCpu(), statLine.getAverageGpu());
        }

        totalTimeCpu = rootLine.cpuValue;
        totalTimeGpu = rootLine.gpuValue + data.get("EndFrame").getAverageGpu();

        layout();

    }

    private void layout() {
        height = 0;
        selectedValueCpu = 0;
        selectedValueGpu = 0;
        rootLine.layout(0);

        frameTimeValue.setText(df.format(getMsFromNs(prof.getAverageFrameTime())) + "ms");
        frameTimeValue.setLocalTranslation(PANEL_WIDTH / 2, -PADDING, 0);
        setColor(frameTimeValue, prof.getAverageFrameTime(), totalTimeCpu, false, false);

        frameCpuTimeValue.setText(df.format(getMsFromNs(totalTimeCpu)) + "ms");
        frameCpuTimeValue.setLocalTranslation(new Vector3f(PANEL_WIDTH / 4 - bigFont.getLineWidth(frameCpuTimeValue.getText()) / 2, -PADDING - 50, 0));
        setColor(frameCpuTimeValue, totalTimeCpu, totalTimeCpu, false, false);

        frameGpuTimeValue.setText(df.format(getMsFromNs(totalTimeGpu)) + "ms");
        frameGpuTimeValue.setLocalTranslation(new Vector3f(3 * PANEL_WIDTH / 4 - bigFont.getLineWidth(frameGpuTimeValue.getText()) / 2, -PADDING - 50, 0));
        setColor(frameGpuTimeValue, totalTimeGpu, totalTimeGpu, false, false);

        selectedField.setText("Selected: " + df.format(getMsFromNs(selectedValueCpu)) + "ms / " + df.format(getMsFromNs(selectedValueGpu)) + "ms");

        selectedField.setLocalTranslation(3 * PANEL_WIDTH / 4 - font.getLineWidth(selectedField.getText()) / 2, -PADDING - 75, 0);
    }

    private StatLineView getStatLineView(String path) {
        StatLineView line = lines.get(path);

        if (line == null) {
            line = new StatLineView(getLeaf(path));
            lines.put(path, line);
            line.attachTo(ui);
        }
        return line;
    }

    private String getLeaf(String path) {
        int idx = path.lastIndexOf("/");
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private String getParent(String path) {
        int idx = path.lastIndexOf("/");
        return idx >= 0 ? path.substring(0, idx) : null;
    }


    @Override
    public void postRender() {
        if (time > REFRESH_TIME) {
            prof.appStep(AppStep.EndFrame);
            Map<String, DetailedProfiler.StatLine> data = prof.getStats();
            displayData(data);
            time = 0;
        }
    }

    public Node getUiNode() {
        return ui;
    }

    private double getMsFromNs(double time) {
        return time / 1000000.0;
    }

    @Override
    protected void onEnable() {
        getApplication().setAppProfiler(prof);
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(ui);
    }

    @Override
    protected void onDisable() {
        getApplication().setAppProfiler(null);
        ui.removeFromParent();
    }

    public boolean setColor(BitmapText t, double value, double totalTime, boolean isParent, boolean expended) {

        boolean dimmed = isParent && expended;
        boolean insignificant = false;

        if (value > 1000000000.0 / 30.0) {
            t.setColor(dimmed ? dimmedRed : ColorRGBA.Red);
        } else if (value > 1000000000.0 / 60.0) {
            t.setColor(dimmed ? dimmedOrange : ColorRGBA.Orange);
        } else if (value > totalTime / 3) {
            t.setColor(dimmed ? dimmedGreen : ColorRGBA.Green);
        } else if (value < 30000) {
            t.setColor(ColorRGBA.DarkGray);
            insignificant = true;
        } else {
            t.setColor(dimmed ? dimmedWhite : ColorRGBA.White);
        }
        return insignificant;
    }

    private void handleClick(Vector2f pos) {

        Vector3f lp = hideInsignificantField.getWorldTranslation();
        float width = font.getLineWidth(hideInsignificantField.getText());
        if (pos.x > lp.x && pos.x < (lp.x + width)
                && pos.y < lp.y && pos.y > lp.y - LINE_HEIGHT) {
            hideInsignificant = !hideInsignificant;
            hideInsignificantField.setText((hideInsignificant ? "X " : "O ") + INSIGNIFICANT);
            if (!hideInsignificant) {
                rootLine.setExpended(true);
            }
        }

        rootLine.onClick(pos);
        for (StatLineView statLineView : lines.values()) {
            statLineView.onClick(pos);
        }
        layout();
    }

    private class StatLineView {
        BitmapText label;
        BitmapText cpuText;
        BitmapText gpuText;
        BitmapText checkBox;
        double cpuValue;
        double gpuValue;
        private boolean expended = true;
        private boolean visible = true;
        private boolean selected = false;
        String text;

        Set<StatLineView> children = new LinkedHashSet<>();

        public StatLineView(String label) {
            this.text = label;
            this.label = new BitmapText(font);
            this.checkBox = new BitmapText(font);
            this.checkBox.setText("O");
            this.label.setText("- " + label);
            this.cpuText = new BitmapText(font);
            this.gpuText = new BitmapText(font);
        }

        public void onClick(Vector2f pos) {

            if (!visible) {
                return;
            }

            Vector3f lp = label.getWorldTranslation();
            Vector3f cp = checkBox.getWorldTranslation();
            if (pos.x > cp.x
                    && pos.y < lp.y && pos.y > lp.y - LINE_HEIGHT) {

                float width = font.getLineWidth(checkBox.getText());
                if (pos.x >= cp.x && pos.x <= (cp.x + width)) {
                    selected = !selected;
                    if (selected) {
                        checkBox.setText("X");
                    } else {
                        checkBox.setText("O");
                    }
                } else {
                    setExpended(!expended);
                }
            }
        }

        public void setExpended(boolean expended) {
            this.expended = expended;
            if (expended) {
                label.setText("- " + text);
            } else {
                label.setText("+ " + text);
            }
            for (StatLineView child : children) {
                child.setVisible(expended);
            }
        }

        public void layout(int indent) {

            boolean insignificant;
            cpuText.setText(df.format(getMsFromNs(cpuValue)) + "ms /");
            insignificant = setColor(cpuText, cpuValue, totalTimeCpu, !children.isEmpty(), expended);
            gpuText.setText(" " + df.format(getMsFromNs(gpuValue)) + "ms");
            insignificant &= setColor(gpuText, gpuValue, totalTimeGpu, !children.isEmpty(), expended);

            if (insignificant && hideInsignificant) {
                setVisible(false);
            }

            if (!visible) {
                return;
            }

            if (selected) {
                label.setColor(ColorRGBA.Yellow);
                selectedValueCpu += cpuValue;
                selectedValueGpu += gpuValue;
            } else {
                label.setColor(ColorRGBA.White);
            }

            int y = -(height * LINE_HEIGHT + HEADER_HEIGHT);

            label.setLocalTranslation(PADDING + indent * PADDING, y, 0);
            float gpuPos = PANEL_WIDTH - font.getLineWidth(gpuText.getText()) - PADDING * (maxLevel - indent + 1);
            cpuText.setLocalTranslation(gpuPos - font.getLineWidth(cpuText.getText()), y, 0);
            gpuText.setLocalTranslation(gpuPos, y, 0);

            checkBox.setLocalTranslation(3, y, 0);
            height++;
            for (StatLineView child : children) {
                child.layout(indent + 1);
            }
        }

        public void updateValues(double cpu, double gpu) {
            cpuValue += cpu;
            gpuValue += gpu;
        }

        public void attachTo(Node node) {
            node.attachChild(label);
            node.attachChild(cpuText);
            node.attachChild(gpuText);
            node.attachChild(checkBox);
        }

        public void removeFromParent() {
            label.removeFromParent();
            cpuText.removeFromParent();
            gpuText.removeFromParent();
            checkBox.removeFromParent();
        }

        public void reset() {
            children.clear();
            cpuValue = 0;
            gpuValue = 0;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
            label.setCullHint(visible ? Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
            cpuText.setCullHint(visible ? Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
            gpuText.setCullHint(visible ? Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
            checkBox.setCullHint(visible ? Spatial.CullHint.Dynamic : Spatial.CullHint.Always);


            for (StatLineView child : children) {
                child.setVisible(visible && expended);
            }

        }


        @Override
        public String toString() {
            return label.getText() + " - " + df.format(getMsFromNs(cpuValue)) + "ms / " + df.format(getMsFromNs(gpuValue)) + "ms";
        }


    }
}

