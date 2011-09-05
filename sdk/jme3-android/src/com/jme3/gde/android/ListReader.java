/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.android;

import com.jme3.gde.android.AndroidSdkTool.AndroidTarget;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public class ListReader implements Runnable {

    private Thread thread;
    private BufferedReader in;
    private ProgressHandle progress;
    private ArrayList<AndroidTarget> list;

    public ListReader(InputStream in, ArrayList<AndroidTarget> list) {
        this.in = new BufferedReader(new InputStreamReader(in));
        this.list = list;
    }

    public ListReader(BufferedReader in, ArrayList<AndroidTarget> list) {
        this.in = in;
        this.list = list;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        try {
            String line;
            AndroidTarget target = null;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    try {

                        if (line.startsWith("id:")) {
                            target = new AndroidTarget();
                            int idstart = line.indexOf(":") + 1;
                            int idend = line.indexOf("or");
                            int start = line.indexOf("\"") + 1;
                            int end = line.lastIndexOf("\"");
                            target.setId(Integer.parseInt(line.substring(idstart, idend).trim()));
                            target.setName(line.substring(start, end));
                            list.add(target);
                        } else if (line.startsWith("Name:") && target != null) {
                            target.setTitle(line.split(":")[1].trim());
                        } else if (line.startsWith("Type:") && target != null) {
                            target.setPlatform(line.split(":")[1].trim());
                        } else if (line.startsWith("API level:") && target != null) {
                            target.setApiLevel(Integer.parseInt(line.split(":")[1].trim()));
                        } else if (line.startsWith("Revision:") && target != null) {
                            target.setRevision(Integer.parseInt(line.split(":")[1].trim()));
                        } else if (line.startsWith("Skins:") && target != null) {
                            target.setSkins(line.split(":")[1].trim());
                        }
                    } catch (Exception e) {
                        Exceptions.printStackTrace(e);
                    }
                    if (progress != null) {
                        progress.progress(line);
                    } else {
                        Logger.getLogger(this.getClass().getName()).log(Level.INFO, line);
                    }
                }
            }
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
    }

    /**
     * @param progress the progress to set
     */
    public void setProgress(ProgressHandle progress) {
        this.progress = progress;
    }
}
