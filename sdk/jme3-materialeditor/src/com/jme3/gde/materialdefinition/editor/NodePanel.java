/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.editor;

import com.jme3.gde.materialdefinition.fileStructure.ShaderNodeBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.DefinitionBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.InputMappingBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.OutputMappingBlock;
import com.jme3.gde.materialdefinition.icons.Icons;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderNodeDefinition;
import com.jme3.shader.ShaderNodeVariable;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.openide.util.WeakListeners;

/**
 *
 * @author Nehon
 */
public class NodePanel extends DraggablePanel implements Selectable, PropertyChangeListener, InOut, KeyListener {

    List<JLabel> inputLabels = new ArrayList<JLabel>();
    List<JLabel> outputLabels = new ArrayList<JLabel>();
    List<Dot> inputDots = new ArrayList<Dot>();
    List<Dot> outputDots = new ArrayList<Dot>();
    private NodeType type = NodeType.Vertex;
    private JPanel content;
    private JLabel header;
    private Color color;
    private String name;
    private String techName;
    private NodeToolBar toolBar;
    protected List<String> filePaths = new ArrayList<String>();
    protected Shader.ShaderType shaderType;

    public enum NodeType {

        Vertex(new Color(220, 220, 70)),//yellow
        Fragment(new Color(114, 200, 255)),//bleue
        Attribute(Color.WHITE),
        MatParam(new Color(70, 220, 70)),//green
        WorldParam(new Color(220, 70, 70)); //red
        private Color color;

        private NodeType() {
        }

        private NodeType(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }

    /**
     * Creates new form NodePanel
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public NodePanel(ShaderNodeBlock node, ShaderNodeDefinition def) {
        super();
        shaderType = def.getType();
        if (def.getType() == Shader.ShaderType.Vertex) {
            type = NodePanel.NodeType.Vertex;
        } else {
            type = NodePanel.NodeType.Fragment;
        }
        init(def.getInputs(), def.getOutputs());

        node.addPropertyChangeListener(WeakListeners.propertyChange(this, node));
        this.addPropertyChangeListener(WeakListeners.propertyChange(node, this));
        refresh(node);
        addKeyListener(this);
        this.filePaths.addAll(def.getShadersPath());
        String defPath = ((DefinitionBlock) node.getContents().get(0)).getPath();
        this.filePaths.add(defPath);
        toolBar = new NodeToolBar(this);        
    }

    /**
     * Creates new form NodePanel
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public NodePanel(ShaderNodeVariable singleOut, NodePanel.NodeType type) {
        super();
        List<ShaderNodeVariable> outputs = new ArrayList<ShaderNodeVariable>();
        outputs.add(singleOut);
        this.type = type;
        init(new ArrayList<ShaderNodeVariable>(), outputs);
        addKeyListener(this);
        toolBar = new NodeToolBar(this);
    }

    public final void refresh(ShaderNodeBlock node) {
        name = node.getName();
        header.setText(node.getName());
        header.setToolTipText(node.getName());

    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("name")) {
            refresh((ShaderNodeBlock) evt.getSource());
        }
    }

    private void init(List<ShaderNodeVariable> inputs, List<ShaderNodeVariable> outputs) {

        setBounds(0, 0, 120, 30 + inputs.size() * 20 + outputs.size() * 20);

        for (ShaderNodeVariable input : inputs) {

            JLabel label = createLabel(input.getType(), input.getName(), Dot.ParamType.Input);
            Dot dot = createDot(input.getType(), Dot.ParamType.Input, input.getName());
            inputLabels.add(label);
            inputDots.add(dot);
        }
        int index = 0;
        for (ShaderNodeVariable output : outputs) {
            String outName = output.getName();
            JLabel label = createLabel(output.getType(), outName, Dot.ParamType.Output);
            Dot dot = createDot(output.getType(), Dot.ParamType.Output, outName);
            dot.setIndex(index++);
            outputLabels.add(label);
            outputDots.add(dot);
        }

        initComponents();
        updateType();
        setOpaque(false);

    }

    public void setTitle(String s) {
        header.setText(s);
        header.setToolTipText(s);
    }

    public Dot getInputConnectPoint(String varName) {
        return getConnectPoint(inputLabels, varName, inputDots);
    }

    public Dot getOutputConnectPoint(String varName) {
        return getConnectPoint(outputLabels, varName, outputDots);
    }

    private Dot getConnectPoint(List<JLabel> list, String varName, List<Dot> listDot) {
        if (varName.startsWith("m_") || varName.startsWith("g_")) {
            varName = varName.substring(2);
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getText().equals(varName)) {
                return listDot.get(i);
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        Color boderColor = Color.BLACK;
        if (getDiagram().getSelectedItems().contains(this)) {
            boderColor = Color.WHITE;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, // Anti-alias!
                RenderingHints.VALUE_ANTIALIAS_ON);
        // Color[] colors = {new Color(0, 0, 0, 0.7f), new Color(0, 0, 0, 0.15f)};
        if (getDiagram().getSelectedItems().contains(this)) {
            Color[] colors = new Color[]{new Color(0.6f, 0.6f, 1.0f, 0.8f), new Color(0.6f, 0.6f, 1.0f, 0.5f)};
            float[] factors = {0f, 1f};
            g.setPaint(new RadialGradientPaint(getWidth() / 2, getHeight() / 2, getWidth() / 2, factors, colors));
            g.fillRoundRect(8, 3, getWidth() - 10, getHeight() - 6, 15, 15);
        }else{
            if(toolBar.isVisible()){
                toolBar.setVisible(false);
            }
        }

        g.setColor(new Color(170, 170, 170, 120));
        g.fillRoundRect(5, 1, getWidth() - 9, getHeight() - 6, 15, 15);
        g.setColor(boderColor);

        g.drawRoundRect(4, 0, getWidth() - 9, getHeight() - 6, 15, 15);
        g.setColor(new Color(170, 170, 170, 120));
        g.fillRect(4, 1, 10, 10);
        g.setColor(boderColor);
        g.drawLine(4, 0, 14, 0);
        g.drawLine(4, 0, 4, 10);
        g.setColor(Color.BLACK);
        g.drawLine(5, 15, getWidth() - 6, 15);
        g.setColor(new Color(190, 190, 190));
        g.drawLine(5, 16, getWidth() - 6, 16);

        Color c1 = new Color(color.getRed(), color.getGreen(), color.getBlue(), 150);
        Color c2 = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
        g.setPaint(new GradientPaint(0, 15, c1, getWidth(), 15, c2));
        g.fillRect(5, 1, getWidth() - 10, 14);

    }

    public String getKey() {
        switch (type) {
            case Attribute:
                return "Attr." + outputLabels.get(0).getText();
            case WorldParam:
                return "WorldParam." + outputLabels.get(0).getText();
            case MatParam:
                return "MatParam." + outputLabels.get(0).getText();
            default:
                return techName + "/" + name;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);        
        diagram.select(this, e.isShiftDown() || e.isControlDown());
        showToolBar();
    }
    
    private void showToolBar(){
        toolBar.display();
    }

    public NodeType getType() {
        return type;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        diagram.fixSize();
        if (svdx != getLocation().x) {
            firePropertyChange(ShaderNodeBlock.POSITION, svdx, getLocation().x);
            getDiagram().getEditorParent().savePositionToMetaData(getKey(), getLocation().x, getLocation().y);
        }
    }

    public final void updateType() {

        switch (type) {
            case Vertex:
                header.setIcon(Icons.vert);
                break;
            case Fragment:
                header.setIcon(Icons.frag);
                break;
            case Attribute:
                header.setIcon(Icons.attrib);
                header.setText("Attribute");
                header.setToolTipText("Attribute");
                name = "Attr";
                break;
            case WorldParam:
                header.setIcon(Icons.world);
                header.setText("WorldParam");
                header.setToolTipText("WorldParam");
                name = "WorldParam";
                break;
            case MatParam:
                header.setIcon(Icons.mat);
                header.setText("MatParam");
                header.setToolTipText("MatParam");
                name = "MatParam";
                break;
        }
        color = type.getColor();
    }

    public void edit() {
        if (type == NodeType.Fragment || type == NodeType.Vertex) {
            diagram.showEdit(NodePanel.this);
        }
    }
    
    public void cleanup(){
        toolBar.getParent().remove(toolBar);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        ImageIcon icon = Icons.vert;
        if (type == NodeType.Fragment) {
            icon = Icons.frag;
        }
        header = new JLabel(icon);
        header.setForeground(Color.BLACK);
        header.addMouseListener(labelMouseMotionListener);
        header.addMouseMotionListener(labelMouseMotionListener);
        header.setHorizontalAlignment(SwingConstants.LEFT);
        header.setFont(new Font("Tahoma", Font.BOLD, 11));

        content = new JPanel();
        content.setOpaque(false);
        GroupLayout contentLayout = new GroupLayout(content);
        content.setLayout(contentLayout);

        int txtLength = 100;

        GroupLayout.ParallelGroup grpHoriz = contentLayout.createParallelGroup(GroupLayout.Alignment.LEADING);

        for (int i = 0; i < outputDots.size(); i++) {
            grpHoriz.addGroup(GroupLayout.Alignment.TRAILING, contentLayout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(outputLabels.get(i), GroupLayout.PREFERRED_SIZE, txtLength, GroupLayout.PREFERRED_SIZE)
                    .addGap(2, 2, 2)
                    .addComponent(outputDots.get(i), GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE));
        }
        for (int i = 0; i < inputDots.size(); i++) {
            grpHoriz.addGroup(GroupLayout.Alignment.LEADING, contentLayout.createSequentialGroup()
                    .addComponent(inputDots.get(i), GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
                    .addGap(2, 2, 2)
                    .addComponent(inputLabels.get(i), GroupLayout.PREFERRED_SIZE, txtLength, GroupLayout.PREFERRED_SIZE));
        }

        contentLayout.setHorizontalGroup(grpHoriz);

        GroupLayout.ParallelGroup grpVert = contentLayout.createParallelGroup(GroupLayout.Alignment.LEADING);

        GroupLayout.SequentialGroup grp = contentLayout.createSequentialGroup();
        for (int i = 0; i < inputDots.size(); i++) {
            grp.addGroup(contentLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(inputDots.get(i), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(inputLabels.get(i))).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
        }
        for (int i = 0; i < outputDots.size(); i++) {
            grp.addGroup(contentLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(outputDots.get(i), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(outputLabels.get(i))).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
        }

        grpVert.addGroup(GroupLayout.Alignment.TRAILING, grp);

        contentLayout.setVerticalGroup(grpVert);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(header, 100, 100, 100))
                .addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGap(6, 6, 6))
                .addComponent(content, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(header, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(content, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10));
    }

    public JLabel createLabel(String glslType, String txt, Dot.ParamType type) {
        JLabel label = new JLabel(txt);
        label.setForeground(Color.BLACK);
        label.setToolTipText(glslType + " " + txt);
        label.setOpaque(false);
        //label.setPreferredSize(new Dimension(50, 15));        
        label.setHorizontalAlignment(type == Dot.ParamType.Output ? SwingConstants.RIGHT : SwingConstants.LEFT);
        label.setFont(new Font("Tahoma", 0, 10));
        label.addMouseListener(labelMouseMotionListener);
        label.addMouseMotionListener(labelMouseMotionListener);
        // label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        return label;
    }

    public Dot createDot(String type, Dot.ParamType paramType, String paramName) {
        Dot dot1 = new Dot();
        dot1.setShaderTypr(shaderType);
        dot1.setNode(this);
        dot1.setText(paramName);
        dot1.setParamType(paramType);
        dot1.setType(type);
        return dot1;
    }

    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            delete();
        }
    }

    public void delete() {
        Diagram diag = getDiagram();
        diag.removeSelected();
    }

    public void keyReleased(KeyEvent e) {
    }
    // used to pass press and drag events to the NodePanel when they occur on the label
    private LabelMouseMotionListener labelMouseMotionListener = new LabelMouseMotionListener();

    private class LabelMouseMotionListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            MouseEvent me = SwingUtilities.convertMouseEvent(e.getComponent(), e, NodePanel.this);
            NodePanel.this.dispatchEvent(me);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            MouseEvent me = SwingUtilities.convertMouseEvent(e.getComponent(), e, NodePanel.this);
            NodePanel.this.dispatchEvent(me);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            MouseEvent me = SwingUtilities.convertMouseEvent(e.getComponent(), e, NodePanel.this);
            NodePanel.this.dispatchEvent(me);
        }
    }

    public void setTechName(String techName) {
        this.techName = techName;
    }

    public void addInputMapping(InputMappingBlock block) {
        firePropertyChange(ShaderNodeBlock.INPUT, null, block);
    }

    public void removeInputMapping(InputMappingBlock block) {
        firePropertyChange(ShaderNodeBlock.INPUT, block, null);
    }

    public void addOutputMapping(OutputMappingBlock block) {
        firePropertyChange(ShaderNodeBlock.OUTPUT, null, block);
    }

    public void removeOutputMapping(OutputMappingBlock block) {
        firePropertyChange(ShaderNodeBlock.OUTPUT, block, null);
    }
}
