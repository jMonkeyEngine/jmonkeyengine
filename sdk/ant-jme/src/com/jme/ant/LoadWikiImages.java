/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *
 * @author normenhansen
 */
public class LoadWikiImages extends Task {

    File file;
    String targetFolder;
    private String host;

    @Override
    public void execute() throws BuildException {
        validate();
        BufferedReader rdr;
        try {
            rdr = new BufferedReader(new FileReader(file));
            String line = rdr.readLine();
            while (line != null) {
                line = line.trim();
                int idx = line.indexOf("<img src=");
                while (idx >= 0) {
                    int endIdx = line.indexOf("\"", idx + 10);
                    if (endIdx >= 0) {
                        String link = line.substring(idx + 10, endIdx);
                        int wikidx = link.indexOf("/wiki/lib/exe/fetch.php/");
                        //int extidx = link.indexOf("/wiki/lib/exe/fetch.php?");
                        int extidx = -1;
                        if (wikidx >= 0) {
                            String name = link.replaceAll("/wiki/lib/exe/fetch\\.php/", "");
                            int markIdx = name.indexOf("?");
                            if (markIdx >= 0) {
                                name = name.substring(0, markIdx);
                            }
                            name = name.replaceAll(":", "/");
                            URL url = new URL(host + link);
                            InputStream in = null;
                            FileOutputStream out = null;
                            try {

                                in = url.openStream();
                                File file = new File(getLocation().getFileName().replaceAll("build.xml", "") + File.separator + targetFolder + File.separator + name.replaceAll("/", File.separator));
                                log("Getting image: " + host + link);
                                log("To: " + file);
                                File parent = file.getParentFile();
                                parent.mkdirs();
                                out = new FileOutputStream(file);
                                int byte_ = in.read();
                                while (byte_ != -1) {
                                    out.write(byte_);
                                    byte_ = in.read();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                if (in != null) {
                                    in.close();
                                }
                                if (out != null) {
                                    out.close();
                                }
                            }
                        } else if (extidx >= 0) {
                            String name = link.replaceAll("/wiki/lib/exe/fetch\\.php\\?([^>]*);media=([^>]*)\"", "");
                            int markIdx = name.indexOf("?");
                            if (markIdx >= 0) {
                                name = name.substring(0, markIdx);
                            }
                            //make external folder and clean filename
                            name = "external/" + name.replaceAll("[_[^\\w\\däüöÄÜÖ\\/\\+\\-\\. ]]", "_");
                            URL url = new URL(host + link);
                            InputStream in = url.openStream();
                            File file = new File(getLocation().getFileName().replaceAll("build.xml", "") + File.separator + targetFolder + File.separator + name.replaceAll("/", File.separator));
                            log("Getting external image: " + host + link);
                            log("To: " + file);
                            File parent = file.getParentFile();
                            parent.mkdirs();
                            FileOutputStream out = new FileOutputStream(file);
                            int byte_ = in.read();
                            while (byte_ != -1) {
                                out.write(byte_);
                                byte_ = in.read();
                            }
                            in.close();
                            out.close();
                        }
                    }
                    idx = line.indexOf("<img src=", idx + 1);
                }
                line = rdr.readLine();
            }
            rdr.close();
        } catch (Exception e) {
            throw new BuildException(e, getLocation());
        }
    }

    protected void validate() {
        if (file == null) {
            throw new BuildException("You must specify a file to read.");
        }
        if (!file.canRead()) {
            throw new BuildException("Can not read file " + file.getAbsolutePath());
        }
        if (targetFolder == null) {
            throw new BuildException("You must specify a destination folder.");
        }
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setTarget(String targetFolder) {
        this.targetFolder = targetFolder;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
