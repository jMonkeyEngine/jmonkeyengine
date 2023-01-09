/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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

package jme3test;

import com.jme3.app.LegacyApplication;
import com.jme3.app.SimpleApplication;
import com.jme3.system.JmeContext;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 * Class with a main method that displays a dialog to choose any jME demo to be
 * started.
 */
public class TestChooser extends JFrame {
    private static final Logger logger = Logger.getLogger(TestChooser.class
            .getName());

    private static final long serialVersionUID = 1L;

    /**
     * Only accessed from EDT
     */
    private List<Class<?>> selectedClass = null;
    private boolean showSetting = true;

    private ExecutorService executorService;

    /**
     * Constructs a new TestChooser that is initially invisible.
     */
    public TestChooser() throws HeadlessException {
        super("TestChooser");
        /* This listener ends application when window is closed (x button on top right corner of test chooser).
         * @see issue#85 https://github.com/jMonkeyEngine/jmonkeyengine/issues/85
         */
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    @Override
    public void dispose() {
        if (executorService != null) {
            executorService.shutdown();
        }

        super.dispose();
    }

    /**
     * @param classes
     *            vector that receives the found classes
     * @return classes vector, list of all the classes in a given package (must
     *         be found in classpath).
     */
    private void find(String packageName, boolean recursive,
            Set<Class<?>> classes) {

        // Translate the package name into an absolute path
        String name = packageName;
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        name = name.replace('.', '/');

        // Get a File object for the package
        packageName = packageName + ".";
        URI uri;
        FileSystem fileSystem = null;
        try {
            uri = this.getClass().getResource(name).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to load demo classes.", e);
        }

        // Special case if we are running from inside a JAR
        if ("jar".equalsIgnoreCase(uri.getScheme())) {
            try {
                fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
            } catch (IOException e) {
                throw new RuntimeException("Failed to load demo classes from JAR.", e);
            }
        }

        try {
            Path directory = Paths.get(uri);
            logger.log(Level.FINE, "Searching for Demo classes in \"{0}\".", directory.getFileName().toString());
            addAllFilesInDirectory(directory, classes, packageName, recursive);
        } catch (Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(),
                    "find(pckgname, recursive, classes)", "Exception", e);
        } finally {
            if (fileSystem != null) {
                try {
                    fileSystem.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to close JAR.", e);
                }
            }
        }
    }

    /**
     * Load a class specified by a file- or entry-name
     *
     * @param name
     *            name of a file or entry
     * @return class file that was denoted by the name, null if no class or does
     *         not contain a main method
     */
    private Class load(String name) {
        String classname = name.substring(0, name.length()
                - ".class".length());

        if (classname.startsWith("/")) {
            classname = classname.substring(1);
        }
        classname = classname.replace('/', '.');

        try {
            final Class<?> cls = Class.forName(classname);
            cls.getMethod("main", new Class[]{String[].class});
            if (!getClass().equals(cls)) {
                return cls;
            }
        } catch (NoClassDefFoundError // class has unresolved dependencies
                | ClassNotFoundException // class not in classpath
                | NoSuchMethodException // class does not have a main method
                | UnsupportedClassVersionError e) { // unsupported version
            return null;
        }
        return null;
    }

    /**
     * Used to descent in directories, loads classes via {@link #load}
     *
     * @param directory
     *            where to search for class files
     * @param allClasses
     *            add loaded classes to this collection
     * @param packageName
     *            current package name for the given directory
     * @param recursive
     *            true to descend into subdirectories
     */
    private void addAllFilesInDirectory(final Path directory,
            final Set<Class<?>> allClasses, final String packageName, final boolean recursive) {
        // Get the list of the files contained in the package
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, getFileFilter())) {
            for (Path file : stream) {

                // we are only interested in .class files
                if (Files.isDirectory(file)) {
                    if (recursive) {
                        String dirName = String.valueOf(file.getFileName());
                        if (dirName.endsWith("/")) {
                            // Seems java 8 adds "/" at the end of directory name when
                            // reading from jar filesystem. We need to remove it. - Ali-RS 2023-1-5
                            dirName = dirName.substring(0, dirName.length() - 1);
                        }
                        addAllFilesInDirectory(file, allClasses, packageName + dirName + ".", true);
                    }
                } else {
                    Class<?> result = load(packageName + file.getFileName());
                    if (result != null && !allClasses.contains(result)) {
                        allClasses.add(result);
                    }
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not search the folder!", ex);
        }
    }

    /**
     * @return FileFilter for searching class files (no inner classes, only
     *         those with "Test" in the name)
     */
    private static DirectoryStream.Filter<Path> getFileFilter() {
        return new DirectoryStream.Filter<Path>() {

            @Override
            public boolean accept(Path entry) throws IOException {
                String fileName = entry.getFileName().toString();
                return (fileName.endsWith(".class")
                        && (fileName.contains("Test"))
                        && !fileName.contains("$"))
                        || (!fileName.startsWith(".") && Files.isDirectory(entry));
            }
        };
    }

    private void startApp(final List<Class<?>> appClass) {
        if (appClass == null || appClass.isEmpty()) {
            JOptionPane.showMessageDialog(rootPane,
                                          "Please select a test from the list",
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        executorService.submit(getAppRunner(appClass));
    }

    private Runnable getAppRunner(final List<Class<?>> appClass) {
        return new Runnable() {
            @Override
            public void run() {
                for (Class<?> clazz : appClass) {
                    try {
                        if (LegacyApplication.class.isAssignableFrom(clazz)) {
                            Object app = clazz.getDeclaredConstructor().newInstance();
                            if (app instanceof SimpleApplication) {
                                final Method settingMethod = clazz.getMethod("setShowSettings", boolean.class);
                                settingMethod.invoke(app, showSetting);
                            }
                            final Method mainMethod = clazz.getMethod("start");
                            mainMethod.invoke(app);
                            Field contextField = LegacyApplication.class.getDeclaredField("context");
                            contextField.setAccessible(true);
                            JmeContext context = null;
                            while (context == null) {
                                context = (JmeContext) contextField.get(app);
                                Thread.sleep(100);
                            }
                            while (!context.isCreated()) {
                                Thread.sleep(100);
                            }
                            while (context.isCreated()) {
                                Thread.sleep(100);
                            }
                        } else {
                            final Method mainMethod = clazz.getMethod("main", (new String[0]).getClass());
                            mainMethod.invoke(clazz, new Object[]{new String[0]});
                        }
                        // wait for destroy
                        System.gc();
                    } catch (IllegalAccessException ex) {
                        logger.log(Level.SEVERE, "Cannot access constructor: " + clazz.getName(), ex);
                    } catch (IllegalArgumentException ex) {
                        logger.log(Level.SEVERE, "main() had illegal argument: " + clazz.getName(), ex);
                    } catch (InvocationTargetException ex) {
                        logger.log(Level.SEVERE, "main() method had exception: " + clazz.getName(), ex);
                    } catch (InstantiationException ex) {
                        logger.log(Level.SEVERE, "Failed to create app: " + clazz.getName(), ex);
                    } catch (NoSuchMethodException ex) {
                        logger.log(Level.SEVERE, "Test class doesn't have main method: " + clazz.getName(), ex);
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Cannot start test: " + clazz.getName(), ex);
                        ex.printStackTrace();
                    }
                }
            }
        };
    }

    /**
     * Code to create components and action listeners.
     *
     * @param classes
     *            what Classes to show in the list box
     */
    private void setup(Collection<Class<?>> classes) {
        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        final FilteredJList list = new FilteredJList();
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        DefaultListModel<Class<?>> model = new DefaultListModel<>();
        model.ensureCapacity(classes.size());
        for (Class<?> c : classes) {
            model.addElement(c);
        }
        list.setModel(model);

        mainPanel.add(createSearchPanel(list), BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(list), BorderLayout.CENTER);

        list.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        selectedClass = list.getSelectedValuesList();
                    }
                });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && selectedClass != null) {
                    startApp(selectedClass);
                }
            }
        });
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    startApp(selectedClass);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                }
            }
        });

        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        mainPanel.add(buttonPanel, BorderLayout.PAGE_END);

        final JButton okButton = new JButton("Ok");
        okButton.setMnemonic('O');
        buttonPanel.add(okButton);
        getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startApp(selectedClass);
            }
        });

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic('C');
        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        pack();
        center();
    }

    private class FilteredJList extends JList<Class<?>> {
        private static final long serialVersionUID = 1L;

        private String filter;
        private ListModel originalModel;

        @Override
        @SuppressWarnings("unchecked")
        public void setModel(ListModel m) {
            originalModel = m;
            super.setModel(m);
        }

        @SuppressWarnings("unchecked")
        private void update() {
            if (filter == null || filter.length() == 0) {
                super.setModel(originalModel);
            }

            DefaultListModel v = new DefaultListModel();
            for (int i = 0; i < originalModel.getSize(); i++) {
                Object o = originalModel.getElementAt(i);
                String s = String.valueOf(o).toLowerCase();
                if (s.contains(filter)) {
                    v.addElement(o);
                }
            }
            super.setModel(v);
            if (v.getSize() == 1) {
                setSelectedIndex(0);
            }
            revalidate();
        }

        public String getFilter() {
            return filter;
        }

        public void setFilter(String filter) {
            this.filter = filter.toLowerCase();
            update();
        }
    }

    /**
     * center the frame.
     */
    private void center() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = this.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        this.setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);
    }

    /**
     * Start the chooser.
     *
     * @param args
     *            command line parameters
     */
    public static void main(final String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        new TestChooser().start(args);
    }

    protected void start(String[] args) {
        executorService = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "AppStarter");
            }
        });
        final Set<Class<?>> classes = new LinkedHashSet<>();
        logger.fine("Composing Test list...");
        addDisplayedClasses(classes);
        setup(classes);
        setVisible(true);
    }

    protected void addDisplayedClasses(Set<Class<?>> classes) {
        find("jme3test", true, classes);
    }

    private JPanel createSearchPanel(final FilteredJList classes) {
        JPanel search = new JPanel();
        search.setLayout(new BorderLayout());
        search.add(new JLabel("Choose a Demo to start:      Find: "),
                BorderLayout.WEST);
        final javax.swing.JTextField jtf = new javax.swing.JTextField();
        jtf.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                classes.setFilter(jtf.getText());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                classes.setFilter(jtf.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                classes.setFilter(jtf.getText());
            }
        });
        jtf.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedClass = classes.getSelectedValuesList();
                startApp(selectedClass);
            }
        });
        final JCheckBox showSettingCheck = new JCheckBox("Show Setting");
        showSettingCheck.setSelected(true);
        showSettingCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSetting = showSettingCheck.isSelected();
            }
        });
        jtf.setPreferredSize(new Dimension(100, 25));
        search.add(jtf, BorderLayout.CENTER);
        search.add(showSettingCheck, BorderLayout.EAST);
        return search;
    }
}
