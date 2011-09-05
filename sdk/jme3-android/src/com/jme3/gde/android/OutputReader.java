/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.android;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public class OutputReader implements Runnable {

    private Thread thread;
    private BufferedReader in;
    private ProgressHandle progress;

    public OutputReader(InputStream in) {
        this.in = new BufferedReader(new InputStreamReader(in));
    }

    public OutputReader(BufferedReader in) {
        this.in = in;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().length() > 0) {
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
