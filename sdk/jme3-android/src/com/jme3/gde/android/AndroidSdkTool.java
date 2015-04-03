/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.android;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Message;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author normenhansen
 */
public class AndroidSdkTool {

    /**
     * Starts the Android target configuration utility.
     */
    public static void startAndroidTool() {
        startAndroidTool(false);
    }

    public static void startAndroidTool(boolean modal) {
        final String path = getAndroidToolPath();
        if (path == null) {
            return;
        }
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                String[] command = new String[]{path};
                ProcessBuilder builder = new ProcessBuilder(command);
                try {
                    Process proc = builder.start();
                    OutputReader outReader = new OutputReader(proc.getInputStream());
                    OutputReader errReader = new OutputReader(proc.getErrorStream());
                    outReader.start();
                    errReader.start();
                    proc.waitFor();
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
        if (modal) {
            thread.run();
        } else {
            thread.start();
        }
    }

    /**
     * Returns a FileObject for the android SDK folder, null if none is specified
     * @return
     */
    public static FileObject getSdkFolder() {
        String path = getSdkPath();
        if (path == null) {
            return null;
        }
        FileObject fileObj = FileUtil.toFileObject(new File(path));
        if (fileObj == null) {
            return null;
        }
        return fileObj;
    }

    /**
     * Returns a String with the path to the SDK or null if none is specified.
     * @return
     */
    public static String getSdkPath() {
        String path = NbPreferences.forModule(AndroidSdkTool.class).get("sdk_path", null);
        if (path == null) {
            FileChooserBuilder builder = new FileChooserBuilder(AndroidSdkTool.class);
            builder.setTitle("Please select Android SDK Folder");
            builder.setDirectoriesOnly(true);
            File file = builder.showOpenDialog();
            if (file != null) {
                FileObject folder = FileUtil.toFileObject(file);
                if (folder.getFileObject("tools") == null) {
                    Message msg = new NotifyDescriptor.Message(
                            "Not a valid SDK folder!",
                            NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notifyLater(msg);

                } else {
                    String name = file.getPath();
                    NbPreferences.forModule(AndroidSdkTool.class).put("sdk_path", name);
                    return name;
                }
            }
        } else {
            return path;
        }
        return null;
    }

    /**
     * Returns a string with the path to the android tool, specific for platform (.exe for windows)
     * @return
     */
    public static String getAndroidToolPath() {
        FileObject executable = null;
        FileObject folder = getSdkFolder();
        if (folder == null) {
            return null;
        }
        if (Utilities.isWindows()) {
            executable = folder.getFileObject("tools/android.bat");
        } else {
            executable = folder.getFileObject("tools/android");
        }
        if (executable != null) {
            return FileUtil.toFile(executable).getPath();
        } else {
            return null;
        }
    }

    /**
     * Gets a list of android targets registered in the SDK
     * @return
     */
    public static List<AndroidTarget> getTargetList() {
        ArrayList<AndroidTarget> list = new ArrayList<AndroidTarget>();
        final String path = getAndroidToolPath();
        if (path == null) {
            return list;
        }
        String[] command = new String[]{path, "list", "targets"};
        ProcessBuilder builder = new ProcessBuilder(command);
        try {
            Process proc = builder.start();
            ListReader outReader = new ListReader(proc.getInputStream(), list);
            OutputReader errReader = new OutputReader(proc.getErrorStream());
            outReader.start();
            errReader.start();
            proc.waitFor();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return list;
    }

    //TODO: check mainJmeClass
    public static void checkProject(Project project, String target, String name, String activity, String packag, String mainJmeClass) {
        final String path = getAndroidToolPath();
        if (path == null) {
            return;
        }
        FileObject folder = project.getProjectDirectory().getFileObject("mobile");
        if (folder == null) {
            try {
                folder = project.getProjectDirectory().createFolder("mobile");
                createProject(project, target, name, activity, packag, mainJmeClass);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                return;
            }
        } else {
            updateProject(project, target, name);
        }
    }

    public static void createProject(Project project, String target, String name, String activity, String packag, String mainJmeClass) {
        final String path = getAndroidToolPath();
        if (path == null) {
            return;
        }
        FileObject folder = project.getProjectDirectory().getFileObject("mobile");
        if (folder == null) {
            try {
                folder = project.getProjectDirectory().createFolder("mobile");
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                return;
            }
        }
        String[] command = new String[]{path, "create", "project",
            "--target", target,
            "--name", name,
            "--path", FileUtil.toFile(folder).getPath(),
            "--activity", activity,
            "--package", packag};
        ProcessBuilder builder = new ProcessBuilder(command);
        FileLock lock = null;
        try {
            Process proc = builder.start();
            OutputReader outReader = new OutputReader(proc.getInputStream());
            OutputReader errReader = new OutputReader(proc.getErrorStream());
            outReader.start();
            errReader.start();
            proc.waitFor();
            folder.refresh();
            String mainActName = "mobile/src/" + packag.replaceAll("\\.", "/") + "/MainActivity.java";
            FileObject mainAct = project.getProjectDirectory().getFileObject(mainActName);
            if (mainAct != null) {
                lock = mainAct.lock();
                OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(mainAct.getOutputStream(lock)));
                out.write(mainActivityString(mainJmeClass, packag));
                out.close();
                lock.releaseLock();
            } else {
                Logger.getLogger(AndroidSdkTool.class.getName()).log(Level.WARNING, "Cannot find {0}", mainActName);
            }
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            if (lock != null) {
                lock.releaseLock();
            }
            Exceptions.printStackTrace(ex);
        }
        updateAndroidManifest(project);
        updateAndroidApplicationName(project, name);
        updateAndroidLayout(project, packag);
    }

    public static void updateProject(Project project, String target, String name) {
        final String path = getAndroidToolPath();
        if (path == null) {
            return;
        }
        FileObject folder = project.getProjectDirectory().getFileObject("mobile");
        if (folder == null) {
            return;
        }
        String[] command = new String[]{path, "update", "project",
            "--target", target,
            "--name", name,
            "--path", FileUtil.toFile(folder).getPath()};
        ProcessBuilder builder = new ProcessBuilder(command);
        try {
            Process proc = builder.start();
            OutputReader outReader = new OutputReader(proc.getInputStream());
            OutputReader errReader = new OutputReader(proc.getErrorStream());
            outReader.start();
            errReader.start();
            proc.waitFor();
            folder.refresh();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        updateAndroidApplicationName(project, name);
    }

    private static void updateAndroidManifest(Project project) {
        FileObject manifest = project.getProjectDirectory().getFileObject("mobile/AndroidManifest.xml");
        if (manifest == null) {
            Logger.getLogger(AndroidSdkTool.class.getName()).log(Level.WARNING, "Could not find AndroidManifest.xml");
            return;
        }
        InputStream in = null;
        FileLock lock = null;
        OutputStream out = null;
        try {
            in = manifest.getInputStream();
            Document configuration = XMLUtil.parse(new InputSource(in), false, false, null, null);
            in.close();
            in = null;
            boolean changed = false;
            Element sdkApplication = XmlHelper.findChildElement(configuration.getDocumentElement(), "application");
            if (sdkApplication != null) {
                Element sdkActivity = XmlHelper.findChildElement(sdkApplication, "activity");
                if (sdkActivity != null) {
                    if (!sdkActivity.hasAttribute("android:launchMode")) {
                        sdkActivity.setAttribute("android:launchMode", "singleTask");
                        changed = true;
                    }
                }
                if (sdkActivity != null) {
                    if (sdkActivity.hasAttribute("android:screenOrientation")) {
                        String attrScreenOrientation = sdkActivity.getAttribute("android:screenOrientation");
                    } else {
                        Logger.getLogger(AndroidSdkTool.class.getName()).log(Level.INFO, "creating attrScreenOrientation");
                        sdkActivity.setAttribute("android:screenOrientation", "landscape");
                        changed = true;
                    }
                }
            }

            Element sdkElement = XmlHelper.findChildElement(configuration.getDocumentElement(), "uses-sdk");
            if (sdkElement == null) {
                sdkElement = configuration.createElement("uses-sdk");
                configuration.getDocumentElement().appendChild(sdkElement);
                changed = true;
            }
            if (!"8".equals(sdkElement.getAttribute("android:minSdkVersion"))) {
                sdkElement.setAttribute("android:minSdkVersion", "9");
                changed = true;
            }
            Element screensElement = XmlHelper.findChildElement(configuration.getDocumentElement(), "supports-screens");
            if (screensElement == null) {
                screensElement = configuration.createElement("supports-screens");
                screensElement.setAttribute("android:anyDensity", "true");
//                screensElement.setAttribute("android:xlargeScreens", "true");
                screensElement.setAttribute("android:largeScreens", "true");
                screensElement.setAttribute("android:smallScreens", "true");
                screensElement.setAttribute("android:normalScreens", "true");
                configuration.getDocumentElement().appendChild(screensElement);
                changed = true;
            }
            if (changed) {
                lock = manifest.lock();
                out = manifest.getOutputStream(lock);
                XMLUtil.write(configuration, out, "UTF-8");
                out.close();
                out = null;
                lock.releaseLock();
                lock = null;
            }
        } catch (SAXException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex1) {
                Exceptions.printStackTrace(ex1);
            }
        }
    }

    private static void updateAndroidApplicationName(Project project, String name) {
        FileObject manifest = project.getProjectDirectory().getFileObject("mobile/res/values/strings.xml");
        if (manifest == null) {
            return;
        }
        InputStream in = null;
        FileLock lock = null;
        OutputStream out = null;
        try {
            in = manifest.getInputStream();
            Document configuration = XMLUtil.parse(new InputSource(in), false, false, null, null);
            in.close();
            in = null;
            Element sdkElement = XmlHelper.findChildElementWithAttribute(configuration.getDocumentElement(), "string", "name", "app_name");
            if (sdkElement == null) {
                sdkElement = configuration.createElement("string");
                sdkElement.setAttribute("name", "app_name");
                configuration.getDocumentElement().appendChild(sdkElement);
            }
            if (!sdkElement.getTextContent().trim().equals(name)) {
                sdkElement.setTextContent(name);
                lock = manifest.lock();
                out = manifest.getOutputStream(lock);
                XMLUtil.write(configuration, out, "UTF-8");
                out.close();
                out = null;
                lock.releaseLock();
                lock = null;
            }
        } catch (SAXException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex1) {
                Exceptions.printStackTrace(ex1);
            }
        }
    }

    private static void updateAndroidLayout(Project project, String packag) {
        FileObject layout = project.getProjectDirectory().getFileObject("mobile/res/layout/main.xml");
        if (layout == null) {
            Logger.getLogger(AndroidSdkTool.class.getName()).log(Level.WARNING, "Cannot find layout");
            return;
        }
        InputStream in = null;
        FileLock lock = null;
        OutputStream out = null;
        try {
            in = layout.getInputStream();
            Document configuration = XMLUtil.parse(new InputSource(in), false, false, null, null);
            in.close();
            in = null;

            Element textViewElement = XmlHelper.findChildElement(configuration.getDocumentElement(), "TextView");

            Element fragmentElement = configuration.createElement("fragment");
            fragmentElement.setAttribute("android:name", packag+".MainActivity$JmeFragment");
            fragmentElement.setAttribute("android:id", "@+id/jmeFragment");
            fragmentElement.setAttribute("android:layout_width", "match_parent");
            fragmentElement.setAttribute("android:layout_height", "match_parent");

            if (textViewElement == null) {
                configuration.getDocumentElement().appendChild(fragmentElement);
            } else {
                configuration.getDocumentElement().replaceChild(fragmentElement, textViewElement);
            }

            lock = layout.lock();
            out = layout.getOutputStream(lock);
            XMLUtil.write(configuration, out, "UTF-8");
            out.close();
            out = null;
            lock.releaseLock();
            lock = null;

        } catch (SAXException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex1) {
                Exceptions.printStackTrace(ex1);
            }
        }
    }

    private static String mainActivityString(String mainClass, String packag) {
        String str =
                "package " + packag + ";\n"
                + " \n"
                + "import com.jme3.app.DefaultAndroidProfiler;\n"
                + "import android.app.Activity;\n"
                + "import android.app.FragmentManager;\n"
                + "import android.os.Bundle;\n"
                + "import android.view.Window;\n"
                + "import android.view.WindowManager;\n"
                + "import com.jme3.app.AndroidHarnessFragment;\n"
                + "import java.util.logging.Level;\n"
                + "import java.util.logging.LogManager;\n"
                + " \n"
                + "public class MainActivity extends Activity {\n"
                + "    /*\n"
                + "     * Note that you can ignore the errors displayed in this file,\n"
                + "     * the android project will build regardless.\n"
                + "     * Install the 'Android' plugin under Tools->Plugins->Available Plugins\n"
                + "     * to get error checks and code completion for the Android project files.\n"
                + "     */\n"
                + " \n"
                + "    public MainActivity(){\n"
                + "        // Set the default logging level (default=Level.INFO, Level.ALL=All Debug Info)\n"
                + "        LogManager.getLogManager().getLogger(\"\").setLevel(Level.INFO);\n"
                + "    }\n"
                + " \n"
                + "    @Override\n"
                + "    protected void onCreate(Bundle savedInstanceState) {\n"
                + "        super.onCreate(savedInstanceState);\n"
                + "        // Set window fullscreen and remove title bar\n"
                + "        requestWindowFeature(Window.FEATURE_NO_TITLE);\n"
                + "        getWindow().setFlags(\n"
                + "                WindowManager.LayoutParams.FLAG_FULLSCREEN,\n"
                + "                WindowManager.LayoutParams.FLAG_FULLSCREEN);\n"
                + "        setContentView(R.layout.main);\n"
                + " \n"
                + "        // find the fragment\n"
                + "        FragmentManager fm = getFragmentManager();\n"
                + "        AndroidHarnessFragment jmeFragment =\n"
                + "                (AndroidHarnessFragment) fm.findFragmentById(R.id.jmeFragment);\n"
                + " \n"
                + "        // uncomment the next line to add the default android profiler to the project\n"
                + "        //jmeFragment.getJmeApplication().setAppProfiler(new DefaultAndroidProfiler());\n"
                + "    }\n"
                + " \n"
                + " \n"
                + "    public static class JmeFragment extends AndroidHarnessFragment {\n"
                + "        public JmeFragment() {\n"
                + "            // Set main project class (fully qualified path)\n"
                + "            appClass = \"" + mainClass + "\";\n"
                + " \n"
                + "            // Set the desired EGL configuration\n"
                + "            eglBitsPerPixel = 24;\n"
                + "            eglAlphaBits = 0;\n"
                + "            eglDepthBits = 16;\n"
                + "            eglSamples = 0;\n"
                + "            eglStencilBits = 0;\n"
                + " \n"
                + "            // Set the maximum framerate\n"
                + "            // (default = -1 for unlimited)\n"
                + "            frameRate = -1;\n"
                + " \n"
                + "            // Set the maximum resolution dimension\n"
                + "            // (the smaller side, height or width, is set automatically\n"
                + "            // to maintain the original device screen aspect ratio)\n"
                + "            // (default = -1 to match device screen resolution)\n"
                + "            maxResolutionDimension = -1;\n"
                + " \n"
                + "            // Set input configuration settings\n"
                + "            joystickEventsEnabled = false;\n"
                + "            keyEventsEnabled = true;\n"
                + "            mouseEventsEnabled = true;\n"
                + " \n"
                + "            // Set application exit settings\n"
                + "            finishOnAppStop = true;\n"
                + "            handleExitHook = true;\n"
                + "            exitDialogTitle = \"Do you want to exit?\";\n"
                + "            exitDialogMessage = \"Use your home key to bring this app into the background or exit to terminate it.\";\n"
                + " \n"
                + "            // Set splash screen resource id, if used\n"
                + "            // (default = 0, no splash screen)\n"
                + "            // For example, if the image file name is \"splash\"...\n"
                + "            //     splashPicID = R.drawable.splash;\n"
                + "            splashPicID = 0;\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        return str;
    }

    public static class AndroidTarget {

        private int id;
        private String name;
        private String title;
        private String platform;
        private int apiLevel;
        private int revision;
        private String skins;

        public AndroidTarget() {
        }

        public AndroidTarget(String name) {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public int getApiLevel() {
            return apiLevel;
        }

        public void setApiLevel(int apiLevel) {
            this.apiLevel = apiLevel;
        }

        public int getRevision() {
            return revision;
        }

        public void setRevision(int revision) {
            this.revision = revision;
        }

        public String getSkins() {
            return skins;
        }

        public void setSkins(String skins) {
            this.skins = skins;
        }

        @Override
        public String toString() {
            return getTitle();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof String && getName() != null) {
                return getName().equals(obj);
            } else {
                return super.equals(obj);
            }
        }
    }
}
