/*
 Blender Options:
 -b or --background <file>	Load <file> in background (often used for UI-less rendering)
 -a or --render-anim 	Render frames from start to end (inclusive)
 -S or --scene <name>	Set the active scene <name> for rendering
 -f or --render-frame <frame>	Render frame <frame> and save it.	+<frame> start frame relative, -<frame> end frame relative.
 -s or --frame-start <frame>	Set start to frame <frame> (use before the -a argument)
 -e or --frame-end <frame>	Set end to frame <frame> (use before the -a argument)
 -j or --frame-jump <frames>	Set number of frames to step forward after each rendered frame
 -o or --render-output <path>	Set the render path and file name.	Use // at the start of the path to
 render relative to the blend file.	The # characters are replaced by the frame number, and used to define zero padding.
 ani_##_test.png becomes ani_01_test.png
 test-######.png becomes test-000001.png
 When the filename does not contain #, The suffix #### is added to the filename	The frame number will be added at the end of the filename.
 eg: blender -b foobar.blend -o //render_ -F PNG -x 1 -a
 //render_ becomes //render_####, writing frames as //render_0001.png//
 -E or --engine <engine>	Specify the render engine	use -E help to list available engines

 Format Options:
 -F or --render-format <format>	Set the render format, Valid options are...
 TGA IRIS JPEG MOVIE IRIZ RAWTGA
 AVIRAW AVIJPEG PNG BMP FRAMESERVER	(formats that can be compiled into blender, not available on all systems)
 HDR TIFF EXR MULTILAYER MPEG AVICODEC QUICKTIME CINEON DPX DDS
 -x or --use-extension <bool>	Set option to add the file extension to the end of the file
 -t or --threads <threads>	Use amount of <threads> for rendering in background	[1-64], 0 for systems processor count.

 Animation Playback Options:
 -a <options> <file(s)>	Playback <file(s)>, only operates this way when not running in background.
 -p <sx> <sy>	Open with lower left corner at <sx>, <sy>
 -m		Read from disk (Don't buffer)
 -f <fps> <fps-base>		Specify FPS to start with
 -j <frame>	Set frame step to <frame>

 Window Options:
 -w or --window-border 	Force opening with borders (default)
 -W or --window-borderless 	Force opening without borders
 -p or --window-geometry <sx> <sy> <w> <h>	Open with lower left corner at <sx>, <sy> and width and height as <w>, <h>
 -con or --start-console 	Start with the console window open (ignored if -b is set)

 Game Engine Specific Options:
 -g Game Engine specific options
 -g fixedtime		Run on 50 hertz without dropping frames
 -g vertexarrays		Use Vertex Arrays for rendering (usually faster)
 -g nomipmap		No Texture Mipmapping
 -g linearmipmap		Linear Texture Mipmapping instead of Nearest (default)

 Misc Options:
 -d or --debug 	Turn debugging on
 * Prints every operator call and their arguments
 * Disables mouse grab (to interact with a debugger in some cases)
 * Keeps python sys.stdin rather than setting it to None
 --debug-fpe 	Enable floating point exceptions
 --debug-ffmpeg 	Enable debug messages from FFmpeg library
 --debug-libmv 	Enable debug messages from libmv library

 --factory-startup 	Skip reading the "startup.blend" in the users home directory

 --env-system-datafiles 	Set the BLENDER_SYSTEM_DATAFILES environment variable
 --env-system-scripts 	Set the BLENDER_SYSTEM_SCRIPTS environment variable
 --env-system-python 	Set the BLENDER_SYSTEM_PYTHON environment variable

 -nojoystick 	Disable joystick support
 -noglsl 	Disable GLSL shading
 -noaudio 	Force sound system to None
 -setaudio 	Force sound system to a specific device	NULL SDL OPENAL JACK

 -h or --help 	Print this help text and exit

 -y or --enable-autoexec 	Enable automatic python script execution, (default)
 -Y or --disable-autoexec 	Disable automatic python script execution (pydrivers & startup scripts)

 -P or --python <filename>	Run the given Python script (filename or Blender Text)
 --python-console 	Run blender with an interactive console
 --addons 	Comma separated list of addons (no spaces)
 -v or --version 	Print Blender version and exit
 -- 	Ends option processing, following arguments passed unchanged. Access via python's sys.argv
 Other Options:
 /? 	Print this help text and exit (windows only)
 --debug-python 	Enable debug messages for python
 --debug-events 	Enable debug messages for the event system
 --debug-wm 	Enable debug messages for the window manager
 --debug-all 	Enable all debug messages (excludes libmv)
 --debug-value <value>	Set debug value of <value> on startup

 --debug-jobs 	Enable time profiling for background jobs.
 --verbose <verbose>	Set logging verbosity level.
 -R 	Register .blend extension, then exit (Windows only)
 -r 	Silently register .blend extension, then exit (Windows only)
 Argument Parsing:	arguments must be separated by white space. eg
 "blender -ba test.blend"
 ...will ignore the 'a'
 "blender -b test.blend -f8"
 ...will ignore 8 because there is no space between the -f and the frame value
 Argument Order:
 Arguments are executed in the order they are given. eg
 "blender --background test.blend --render-frame 1 --render-output /tmp"
 ...will not render to /tmp because '--render-frame 1' renders before the output path is set
 "blender --background --render-output /tmp test.blend --render-frame 1"
 ...will not render to /tmp because loading the blend file overwrites the render output that was set
 "blender --background test.blend --render-output /tmp --render-frame 1" works as expected.

 Environment Variables:
 $BLENDER_USER_CONFIG      Directory for user configuration files.
 $BLENDER_USER_SCRIPTS     Directory for user scripts.
 $BLENDER_SYSTEM_SCRIPTS   Directory for system wide scripts.
 $Directory for user data files (icons, translations, ..).
 $BLENDER_SYSTEM_DATAFILES Directory for system wide data files.
 $BLENDER_SYSTEM_PYTHON    Directory for system python libraries.
 $TMP or $TMPDIR           Store temporary files here.
 $SDL_AUDIODRIVER          LibSDL audio driver - alsa, esd, dma.
 $PYTHONHOME               Path to the python directory, eg. /usr/lib/python.

 blender/config
 blender/scripts
 blender/userscripts
 */
package com.jme3.gde.blender;

import com.jme3.gde.blender.scripts.Scripts;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

/**
 *
 * @author normenhansen
 */
public class BlenderTool {

    public static final String TEMP_SUFFIX = "blend";
    private static final String mainFolderName = "blender";
    private static final String configFolderName = mainFolderName + "/config";
    private static final String scriptsFolderName = mainFolderName + "/scripts";
    private static final String jmeScriptsFolderName = mainFolderName + "/jmescripts";
    private static final String userScriptsFolderName = mainFolderName + "/userscripts";
    private static final String tempFolderName = mainFolderName + "/temp";
    private static final Logger logger = Logger.getLogger(BlenderTool.class.getName());
    private static final AtomicBoolean blenderOpened = new AtomicBoolean(false);

    private static String getBlenderExeName() {
        if (Utilities.isWindows()) {
            return "blender.exe";
        } else {
            return "blender";
        }
    }

    private static String getBlenderOsPath() {
        if (Utilities.isMac()) {
            return "../blender/blender.app/Contents/MacOS";
        } else {
            return "../blender";
        }
    }

    private static boolean checkBlenderFolders() {
        String jmpDir = System.getProperty("netbeans.user");
        FileObject fileObject = FileUtil.toFileObject(new File(jmpDir));
        if (fileObject != null) {
            FileObject configFileObject = fileObject.getFileObject(configFolderName);
            //TODO: using installed blender scripts folder, make more flexible by moving
            //to updateable folder
            FileObject scriptsFileObject = fileObject.getFileObject(scriptsFolderName);
            FileObject jmeScriptsFileObject = fileObject.getFileObject(jmeScriptsFolderName);
            FileObject userScriptsFileObject = fileObject.getFileObject(userScriptsFolderName);
            if (configFileObject == null) {
                try {
                    configFileObject = FileUtil.createFolder(fileObject, configFolderName);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                    return false;
                }
            }
            if (scriptsFileObject == null) {
                try {
                    scriptsFileObject = FileUtil.createFolder(fileObject, scriptsFolderName);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                    return false;
                }
            }
            if (jmeScriptsFileObject == null) {
                try {
                    jmeScriptsFileObject = FileUtil.createFolder(fileObject, jmeScriptsFolderName);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                    return false;
                }
            }
            if (userScriptsFileObject == null) {
                try {
                    userScriptsFileObject = FileUtil.createFolder(fileObject, userScriptsFolderName);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                    return false;
                }
            }
            Scripts.copyToFolder(jmeScriptsFileObject);
        } else {
            logger.log(Level.SEVERE, "No global settings folder found!");
            return false;
        }
        return true;
    }

    private static String getConfigEnv() {
        String ret = System.getProperty("netbeans.user") + "/" + configFolderName;
        ret = ret.replace("/", File.separator);
        return ret;
    }

    private static String getScriptsEnv() {
        //TODO: using installed blender scripts folder
        String ret = getBlenderSettingsFolder().getAbsolutePath();
//        String ret = System.getProperty("netbeans.user") + "/" + scriptsFolderName;
        ret = ret.replace("/", File.separator);
        return ret;
    }

    private static String getUserScriptsEnv() {
        String ret = System.getProperty("netbeans.user") + "/" + userScriptsFolderName;
        ret = ret.replace("/", File.separator);
        return ret;
    }

    private static String getScriptPath(String scriptName, String prefix) {
        String ret = System.getProperty("netbeans.user") + "/" + jmeScriptsFolderName + "/" + prefix + "_" + scriptName + ".py";
        ret = ret.replace("/", File.separator);
        return ret;
    }

    private static File getBlenderExecutable() {
        File blender = InstalledFileLocator.getDefault().locate(getBlenderOsPath() + "/" + getBlenderExeName(), null, false);
        if (blender == null) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Error finding Blender executable"));
            logger.log(Level.SEVERE, "Error finding Blender executable");
        }
        return blender;
    }

    private static File getBlenderSettingsFolder() {
        File blender = InstalledFileLocator.getDefault().locate(getBlenderOsPath() + "/2.69", null, false);
        if (blender == null) {
            blender = InstalledFileLocator.getDefault().locate(getBlenderOsPath() + "/2.67", null, false);
        }
        if (blender == null) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Error finding Blender settings"));
            logger.log(Level.SEVERE, "Error finding Blender settings");
        }
        return blender;
    }

    private static File getBlenderRootFolder() {
        File blender = InstalledFileLocator.getDefault().locate(getBlenderOsPath(), null, false);
        if (blender == null) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Error finding Blender root folder"));
            logger.log(Level.SEVERE, "Error finding Blender root folder");
        }
        return blender;
    }

    public static boolean runConversionScript(String type, FileObject input) {
        if (!checkBlenderFolders()) {
            logger.log(Level.SEVERE, "Could not create blender settings folders!");
            return false;
        }
        final File exe = getBlenderExecutable();
        if (exe == null) {
            logger.log(Level.SEVERE, "Could not find blender executable!");
            return false;
        }
        logger.log(Level.INFO, "Running blender as converter for file {0}", input.getPath());
        String scriptPath = getScriptPath(type, "import");
        String inputPath = input.getPath().replace("/", File.separator);
        String inputFolder = input.getParent().getPath().replace("/", File.separator) + File.separator;
        String outputPath = inputFolder + input.getName() + "." + TEMP_SUFFIX;
        try {
            String command = exe.getAbsolutePath();
            ProcessBuilder buildr = new ProcessBuilder(command, "-b",
                    "--factory-startup",
                    "-P", scriptPath,
                    "--",
                    "-i", inputPath,
                    "-o", outputPath);
            buildr.directory(getBlenderRootFolder());
            buildr.environment().put("BLENDER_USER_CONFIG", getConfigEnv());
            buildr.environment().put("BLENDER_SYSTEM_SCRIPTS", getScriptsEnv());
            buildr.environment().put("BLENDER_USER_SCRIPTS", getUserScriptsEnv());
            Process proc = buildr.start();
            OutputReader outReader = new OutputReader(proc.getInputStream());
            OutputReader errReader = new OutputReader(proc.getErrorStream());
            outReader.start();
            errReader.start();
            try {
                proc.waitFor();
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
            if (proc.exitValue() != 0) {
                logger.log(Level.SEVERE, "Error running blender!");
                return false;
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return true;
    }

    public static boolean runToolScript(String toolName, FileObject input) {
        if (!checkBlenderFolders()) {
            logger.log(Level.SEVERE, "Could not create blender settings folders!");
            return false;
        }
        final File exe = getBlenderExecutable();
        if (exe == null) {
            logger.log(Level.SEVERE, "Could not find blender executable!");
            return false;
        }
        logger.log(Level.INFO, "Running blender as {0} tool for file {1}", new Object[]{toolName, input.getPath()});
        String scriptPath = getScriptPath(toolName, "tool");
        String inputPath = input.getPath().replace("/", File.separator);
        try {
            String command = exe.getAbsolutePath();
            ProcessBuilder buildr = new ProcessBuilder(command, "-b",
                    "--factory-startup",
                    "-P", scriptPath,
                    "--",
                    "-i", inputPath);
            buildr.directory(getBlenderRootFolder());
            buildr.environment().put("BLENDER_USER_CONFIG", getConfigEnv());
            buildr.environment().put("BLENDER_SYSTEM_SCRIPTS", getScriptsEnv());
            buildr.environment().put("BLENDER_USER_SCRIPTS", getUserScriptsEnv());
            Process proc = buildr.start();
            OutputReader outReader = new OutputReader(proc.getInputStream());
            OutputReader errReader = new OutputReader(proc.getErrorStream());
            outReader.start();
            errReader.start();
            try {
                proc.waitFor();
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
            if (proc.exitValue() != 0) {
                logger.log(Level.SEVERE, "Error running blender!");
                return false;
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return true;
    }

    private static boolean runBlender(final String file, boolean async) {
        if (!checkBlenderFolders()) {
            logger.log(Level.SEVERE, "Could not create blender settings folders!");
            return false;
        }
        logger.log(Level.INFO, "Running blender with options {0}", file);
        if (blenderOpened.getAndSet(true)) {
            logger.log(Level.WARNING, "Blender seems to be running already.");
            return false;
        }
        final AtomicBoolean successful = new AtomicBoolean(true);
        final File exe = getBlenderExecutable();
        if (exe == null) {
            logger.log(Level.SEVERE, "Could not find blender executable!");
            blenderOpened.set(false);
            return false;
        }
        final Frame mainWin = WindowManager.getDefault().getMainWindow();
        assert (mainWin != null);
        mainWin.setExtendedState(Frame.ICONIFIED);
        Runnable r = new Runnable() {
            public void run() {
                try {
                    String command = exe.getAbsolutePath();
                    ProcessBuilder buildr = new ProcessBuilder(command, file);
                    buildr.directory(getBlenderRootFolder());
                    buildr.environment().put("BLENDER_USER_CONFIG", getConfigEnv());
                    buildr.environment().put("BLENDER_SYSTEM_SCRIPTS", getScriptsEnv());
                    buildr.environment().put("BLENDER_USER_SCRIPTS", getUserScriptsEnv());
                    Process proc = buildr.start();
                    OutputReader outReader = new OutputReader(proc.getInputStream());
                    OutputReader errReader = new OutputReader(proc.getErrorStream());
                    outReader.start();
                    errReader.start();
                    try {
                        proc.waitFor();
                    } catch (InterruptedException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    if (proc.exitValue() == 0) {
                        successful.set(true);
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                    successful.set(false);
                } finally {
                    blenderOpened.set(false);
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            mainWin.setExtendedState(Frame.NORMAL);
                            mainWin.requestFocus();
                        }
                    });
                }
            }
        };
        if (async) {
            new Thread(r).start();
        } else {
            r.run();
        }
        return successful.get();
    }

    public static boolean openInBlender(FileObject file) {
        String path = file.getPath().replace("/", File.separator);
        return runBlender(path, true);
    }

    public static void runBlender() {
        if (!runBlender(null, true)) {
            logger.log(Level.INFO, "Could not run blender, already running? Trying to focus window.");
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Blender is already running!"));
        }
    }
}
