/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.gde.textureeditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 * Utility class that allows to execute nvDXT.exe. If this executable is not found,
 * a dialog is shown where the user can select it.
 * @author Sebastian Weiss
 */
final class NvDXTExecutor {
    private static final Logger LOG = Logger.getLogger(NvDXTExecutor.class.getName());
    private static final String NB_PREF_KEY = "DDSToolsPath";
    /**
     * a list of paths where nvDXT.exe can be stored.
     */
    private static final String[] PATHS = {
        "C:\\Program Files (x86)\\NVIDIA Corporation\\DDS Utilities",
        "C:\\Program Files\\NVIDIA Corporation\\DDS Utilities"
    };    
    /**
     * the actual path to nvDXT.exe.
     */
    private static String PATH = null;
    
    private static final String COMPRESS = "nvdxt";
    private static final String DECOMPRESS = "readdxt";
    
    private static boolean checkPath(String path) {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            boolean compress = false;
            boolean decompress = false;
            for (File f : dir.listFiles()) {
                if (!f.isFile()) {
                    continue;
                }
                if (f.getName().startsWith(COMPRESS)) {
                    compress = true;
                }
                if (f.getName().startsWith(DECOMPRESS)) {
                    decompress = true;
                }
            }
            return compress && decompress;
        }
        return false;
    }
    
    /**
     * Searches nvDXT.exe and sets the PATH variable
     * @return true, if the executable was found
     */
    private static boolean findPath() {
        //first, check if not already set
        if (PATH != null && checkPath(PATH)) {
            return true;
        }
        //check NbPreferences
        PATH = NbPreferences.forModule(NvDXTExecutor.class).get(NB_PREF_KEY, null);
        if (PATH != null && checkPath(PATH)) {
            return true;
        }
        //check probable paths
        for (String p : PATHS) {
            if (checkPath(p)) {
                //use this
                PATH = p;
                NbPreferences.forModule(NvDXTExecutor.class).put(NB_PREF_KEY, p);
                return true;
            }
        }
        //ask user
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("nvdxt.exe", "exe"));
        chooser.setDialogTitle("search NVIDIA dds texture tools (nvdxt.exe)");
        int ret = chooser.showSaveDialog(WindowManager.getDefault().getMainWindow());
        if (ret == JFileChooser.APPROVE_OPTION) {
            String p = chooser.getSelectedFile().getParentFile().getAbsolutePath();
            if (!checkPath(p)) {
                JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), 
                        "folder does not contain nvdxt and readdxt", "invalid folder", 
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            //use this
            PATH = p;
            NbPreferences.forModule(NvDXTExecutor.class).put(NB_PREF_KEY, PATH);
            return true;
        }
        return false;
    }
    
    /**
     * Executes the nvidia dds texture tool that writes a dds texture, 
     * using the specified arguments (nvdxt.exe).
     * @param args the arguments to use (must not contain the executable file name)
     * @return {@code} true on success.
     */
    public static boolean executeCompress(String... args) {
        return execute(COMPRESS, args);
    }
    /**
     * Executes the nvidia dds texture tool that reads a dds texture, 
     * using the specified arguments (readdxt.exe).
     * @param args the arguments to use (must not contain the executable file name)
     * @return {@code} true on success.
     */
    public static boolean executeDeompress(String... args) {
        return execute(DECOMPRESS, args);
    }
    
    private static synchronized boolean execute(String name, String... args) {
        if (!findPath()) {
            return false; //path not found
        }
        //execute
        String[] args2 = new String[args.length + 1];
        args2[0] = name;
        System.arraycopy(args, 0, args2, 1, args.length);
        ProcessBuilder builder = new ProcessBuilder(args2);
        builder.directory(new File(PATH));
        LOG.log(Level.INFO, "execute process {0}", Arrays.toString(args2));
        try {
            Process p = builder.start();
            StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), true);
            StreamGobbler outputGobbler = new StreamGobbler(p.getErrorStream(), false);
            errorGobbler.setDaemon(true);
            outputGobbler.setDaemon(true);
            errorGobbler.start();
            outputGobbler.start();
            int result = p.waitFor();
            if (result == 0) {
                return true; //success
            } else {
                LOG.log(Level.WARNING, "process terminated with error code {0}", result);
                return false;
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return false;
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
            return false;
        }
        
    }
    private static class StreamGobbler extends Thread {
		private final InputStream is;
		private final boolean error;

		// reads everything from is until empty. 
		private StreamGobbler(InputStream is, boolean error) {
			this.is = is;
			this.error = error;
		}

		@Override
		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line=null;
				while ( (line = br.readLine()) != null) {
					if (error) {
						System.err.println(line);
					} else {
						System.out.println(line);
					}
				}    
			} catch (IOException ioe) {
				LOG.log(Level.WARNING, "error in reading from process' output stream", ioe);
			}
		}
	}
}
