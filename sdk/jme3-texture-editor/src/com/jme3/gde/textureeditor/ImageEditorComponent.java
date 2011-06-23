package com.jme3.gde.textureeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;
import com.jme3.gde.textureeditor.filters.BrightFilter;
import com.jme3.gde.textureeditor.filters.BumpMapFilter;
import com.jme3.gde.textureeditor.filters.GrayscaleFilter;
import com.jme3.gde.textureeditor.filters.MirrorFilter;
import com.jme3.gde.textureeditor.filters.ResizeFilter;
import com.jme3.gde.textureeditor.filters.RotateLeftFilter;
import com.jme3.gde.textureeditor.filters.SphereMappedFilter;
import com.jme3.gde.textureeditor.tools.ColorPicker;
import com.jme3.gde.textureeditor.tools.CropTool;
import java.io.IOException;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;
import org.openide.NotifyDescriptor.Message;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public class ImageEditorComponent implements EditorToolTarget {

    private static ImageIcon Icon(String name) {
        return new ImageIcon(ImageEditorComponent.class.getResource("/com/jme3/gde/textureeditor/resources/" + name));
    }

    public static ImageEditorComponent create() {
        return new ImageEditorComponent();
    }
    private final JPanel COMPONENT = new JPanel(new BorderLayout());
    private FileObject editedFile;
    private BufferedImage editedImage;
    private float scaleX = 1;
    private float scaleY = 1;
    private JPanel imageScreen = new JPanel() {

        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);
            doPaint((Graphics2D) graphics);
        }
    };
    private final ColorController COLOR_CONTROLLER = ColorController.create();
    private final JScrollPane scroller;
    private final JPanel imageContainer;
    private JPanel topContainer = new JPanel(new BorderLayout());
    private TopComponent owner;
    private EditorTool currentTool;
    private SaveNode saveNode = new SaveNode();
    private boolean newFile = false;

    private void doPaint(Graphics2D g) {
        if (editedImage != null) {
            g.drawImage(editedImage, 0, 0, imageScreen.getWidth(), imageScreen.getHeight(), null);
        }
        if (currentTool != null) {
            currentTool.drawTrack(g, imageScreen.getWidth(), imageScreen.getHeight(), scaleX, scaleY);
        }
    }

    public ImageEditorComponent() {
        JToolBar bottomBar = new JToolBar();
        bottomBar.setFloatable(false);
        bottomBar.add(COLOR_CONTROLLER.getComponent());
        createToolBar();
        createMenuBar();
        imageContainer = new JPanel(new GridBagLayout());
        GridBagConstraints lim = new GridBagConstraints();
        lim.gridx = lim.gridy = 0;
        imageContainer.add(imageScreen, lim);
        scroller = new JScrollPane(imageContainer);
        COMPONENT.add(scroller);
        COMPONENT.add(topContainer, BorderLayout.NORTH);
        COMPONENT.add(bottomBar, BorderLayout.SOUTH);
    }

    public void setCurrentTool(EditorTool t) {
        if (currentTool != null) {
            currentTool.uninstall(this);
        }
        currentTool = t;
        if (currentTool != null) {
            currentTool.install(this);
        }
    }

    public void setScaleFactor(float value) {
        value = Math.max(value, 0.01f);
        scaleX = value;
        scaleY = value;
        resizeDisplay();
    }

    public float getScaleFactor() {
        return scaleX;
    }

    public JComponent getComponent() {
        return COMPONENT;
    }

    private void enableSaving() {
        saveNode.fire(true);
    }

    private void disableSaving() {
        saveNode.fire(false);
    }

    public void setEditedImage(TopComponent component, BufferedImage image, FileObject file) {
        this.owner = component;
        component.setActivatedNodes(new Node[]{saveNode});
        if (file == null) {
            newFile = true;
            enableSaving();
        } else {
            newFile = false;
            disableSaving();
        }
        editedFile = file;
        editedImage = image;
        resizeDisplay();
    }

    private void resizeDisplay() {
        Dimension s = new Dimension(
                (int) (editedImage.getWidth() * scaleX),
                (int) (editedImage.getHeight() * scaleY));
        imageScreen.setPreferredSize(s);
        imageScreen.setMinimumSize(s);
        imageScreen.setMaximumSize(s);
        COMPONENT.revalidate();
        COMPONENT.repaint();
        scroller.setViewportView(imageContainer);
    }

    @SuppressWarnings("unchecked")
    private void createToolBar() {
        final JButton zoomIn = new JButton(Icon("zoom-in-2.png"));
        final JButton zoomOut = new JButton(Icon("zoom-out-2.png"));
        final JButton resize = new JButton(Icon("transform-scale-2.png"));
        final JButton rotateLeft = new JButton(Icon("object-rotate-left-2.png"));
        final JButton mirrorX = new JButton(Icon("mirror_x.png"));
        final JButton mirrorY = new JButton(Icon("mirror_y.png"));

        JToolBar toolbar1 = new JToolBar();
        toolbar1.add(zoomIn);
        toolbar1.add(zoomOut);
        toolbar1.addSeparator();
        toolbar1.add(resize);
        toolbar1.add(rotateLeft);
        toolbar1.add(mirrorX);
        toolbar1.add(mirrorY);
        toolbar1.setFloatable(false);
        topContainer.add(toolbar1, BorderLayout.CENTER);

        final ButtonGroup toolsGroup = new ButtonGroup();
        final JToggleButton colorPicker = new JToggleButton(Icon("color-picker.png"));
        final JToggleButton imageCrop = new JToggleButton(Icon("transform-crop.png"));
        toolsGroup.add(colorPicker);
        toolsGroup.add(imageCrop);
        JToolBar toolbar2 = new JToolBar();
        toolbar2.setOrientation(JToolBar.VERTICAL);
        toolbar2.setFloatable(false);
        toolbar2.add(colorPicker);
        toolbar2.add(imageCrop);
        COMPONENT.add(toolbar2, BorderLayout.WEST);

        ActionListener al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Object source = e.getSource();
                if (source == zoomIn) {
                    setScaleFactor(getScaleFactor() + 0.1f);
                } else if (source == zoomOut) {
                    setScaleFactor(getScaleFactor() - 0.1f);
                } else if (source == resize) {
                    querySizeAndResize();
                } else if (source == rotateLeft) {
                    editedImage = RotateLeftFilter.create().filter(editedImage);
                    resizeDisplay();
                    enableSaving();
                } else if (source == mirrorX) {
                    editedImage = MirrorFilter.create().filter(editedImage, MirrorFilter.X);
                    resizeDisplay();
                    enableSaving();
                } else if (source == mirrorY) {
                    editedImage = MirrorFilter.create().filter(editedImage, MirrorFilter.Y);
                    resizeDisplay();
                    enableSaving();
                } else if (source == colorPicker) {
                    setCurrentTool(ColorPicker.create());
                } else if (source == imageCrop) {
                    setCurrentTool(CropTool.create());
                }
            }
        };
        for (AbstractButton b : Arrays.asList(zoomIn, zoomOut, resize, /*save, saveAs,*/
                rotateLeft, mirrorX, mirrorY, colorPicker, imageCrop)) {
            b.addActionListener(al);
        }
    }

    private void confirmAndSave() {
        Confirmation msg = new NotifyDescriptor.Confirmation("Confirm overwriting?",
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.QUESTION_MESSAGE);

        Object result = DialogDisplayer.getDefault().notify(msg);

        if (NotifyDescriptor.YES_OPTION.equals(result)) {
            String name = editedFile.getExt();
            try {
                IOModule.create().store(editedImage, name, new File(editedFile.getURL().toURI()));
                disableSaving();
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void requestFileAndSave() {
        FileChooserBuilder builder = new FileChooserBuilder(ImageEditorComponent.class);
        builder.addFileFilter(FileFilters.JPG);
        builder.addFileFilter(FileFilters.TGA);
        builder.addFileFilter(FileFilters.PNG);

        JFileChooser fc = builder.createFileChooser();
        fc.setFileSelectionMode(JFileChooser.SAVE_DIALOG);
        fc.setAcceptAllFileFilterUsed(false);

        int a = fc.showOpenDialog(COMPONENT);
        if (a == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String name = file.getName().toLowerCase();
            String type;
            if (name.endsWith(".png")) {
                type = "png";
            } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                type = "jpg";
            } else if (name.endsWith(".bmp")) {
                type = "bmp";
            } else if (name.endsWith(".tga")) {
                type = "tga";
            } else {
                ExtensionFileFilter filter = (ExtensionFileFilter) fc.getFileFilter();
                file = new File(file.getParentFile(), file.getName() + filter.getExtension());
                type = filter.getExtension().substring(1);
            }
            if (file.exists()) {
                a = JOptionPane.showConfirmDialog(COMPONENT, "Overwrite existing file?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (a != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            try {
                IOModule.create().store(editedImage, type, file);

                Message msg = new NotifyDescriptor.Message("Image saved.");
                DialogDisplayer.getDefault().notify(msg);

                editedFile = FileUtil.toFileObject(file);
                newFile = false;
                owner.setName("PixelHead - " + editedFile.getName());
                disableSaving();
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void querySizeAndResize() {
        final SpinnerNumberModel w = new SpinnerNumberModel(editedImage.getWidth(), 1, 10000, 1);
        final SpinnerNumberModel h = new SpinnerNumberModel(editedImage.getHeight(), 1, 10000, 1);
        final JSpinner ws = new JSpinner(w);
        final JSpinner hs = new JSpinner(h);
        final JPanel lab = new JPanel(new GridLayout(2, 1, 8, 8));
        final JPanel spi = new JPanel(new GridLayout(2, 1, 8, 8));
        lab.add(new JLabel("New Width"));
        lab.add(new JLabel("New Height"));
        spi.add(ws);
        spi.add(hs);
        final JPanel box = new JPanel(new BorderLayout(8, 8));
        box.add(lab, BorderLayout.LINE_START);
        box.add(spi, BorderLayout.CENTER);
        final Object[] options = {"Ok", "Cancel"};

        int a = JOptionPane.showOptionDialog(
                COMPONENT,
                box,
                "Resize Image",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[1]);
        if (a == 0) {
            int newWidth = w.getNumber().intValue();
            int newHeight = h.getNumber().intValue();
            if (newWidth != editedImage.getWidth() || newHeight != editedImage.getHeight()) {
                spawnEditor(ResizeFilter.create().filter(editedImage, newWidth, newHeight));
            }
        }
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu filters = new JMenu("Filters");
        menuBar.add(filters);
        topContainer.add(menuBar, BorderLayout.NORTH);

        final JMenu bumpMenu = new JMenu("Bump Map");
        final JMenuItem bumpSoft = bumpMenu.add("Soft");
        final JMenuItem bumpMedium = bumpMenu.add("Medium");
        final JMenuItem bumpStrong = bumpMenu.add("Strong");
        filters.add(bumpMenu);

        final JMenuItem gray = filters.add("Grayscale");
        final JMenuItem bright = filters.add("Brightness");
        final JMenuItem spheremap = filters.add("SphereMapped");

        ActionListener al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Object source = e.getSource();
                if (source == bumpSoft) {
                    spawnEditor(BumpMapFilter.create().filter(editedImage, 0.01f));
                } else if (source == bumpMedium) {
                    spawnEditor(BumpMapFilter.create().filter(editedImage, 0.025f));
                } else if (source == bumpStrong) {
                    spawnEditor(BumpMapFilter.create().filter(editedImage, 0.5f));
                } else if (source == gray) {
                    spawnEditor(GrayscaleFilter.create().filter(editedImage));
                } else if (source == bright) {
                    spawnEditor(BrightFilter.create().filter(editedImage, ImageEditorComponent.this));
                } else if (source == spheremap) {
                    spawnEditor(SphereMappedFilter.create().filter(editedImage));
                }
            }
        };

        for (AbstractButton b : Arrays.asList(bumpSoft, bumpMedium, bumpStrong, gray, bright, spheremap)) {
            b.addActionListener(al);
        }
    }

    public void spawnEditor(BufferedImage image) {
        if (image != null) {
            ImageEditorTopComponent component = new ImageEditorTopComponent();
            component.setEditedImage(image);
            component.open();
            component.requestActive();
        }
    }

    public JComponent getImageCanvas() {
        return this.imageScreen;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public BufferedImage getCurrentImage() {
        return editedImage;
    }

    public void setForeground(Color picked) {
        COLOR_CONTROLLER.setForeground(picked);
    }

    public void setBackground(Color picked) {
        COLOR_CONTROLLER.setBackground(picked);
    }

    public BufferedImage createIcon(int w, int h) {
        BufferedImage icon = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        icon.getGraphics().drawImage(editedImage, 0, 0, w, h, null);
        return icon;
    }

    public class SaveNode extends AbstractNode {

        SaveCookie impl;

        public SaveNode() {
            super(Children.LEAF);
            impl = new SaveCookieImpl();
        }

        public SaveNode(SaveCookie impl) {
            super(Children.LEAF);
            this.impl = impl;
        }

        public void fire(boolean modified) {
            if (modified) {
                getCookieSet().assign(SaveCookie.class, impl);
            } else {
                getCookieSet().assign(SaveCookie.class);
            }
        }

        private class SaveCookieImpl implements SaveCookie {

            public void save() throws IOException {

                if (newFile) {
                    requestFileAndSave();
                } else {
                    confirmAndSave();
                }
            }
        }
    }
}
