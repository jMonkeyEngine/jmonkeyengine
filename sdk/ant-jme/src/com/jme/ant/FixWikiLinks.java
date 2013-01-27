/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringWriter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *
 * @author normenhansen
 */
public class FixWikiLinks extends Task {

    File file;
    String helpPath;

    @Override
    public void execute() throws BuildException {
        validate();
        BufferedReader rdr;
        try {
            rdr = new BufferedReader(new FileReader(file));
            StringWriter out = new StringWriter();
            String line = rdr.readLine();
            while (line != null) {
                //internal links
                line = line.replaceAll("wiki/doku\\.php/([^>]*):([^>]*):([^>]*):([^>]*)\\?do=export_xhtmlbody", helpPath + "$1/$2/$3/$4.html");
                line = line.replaceAll("wiki/doku\\.php/([^>]*):([^>]*):([^>]*)\\?do=export_xhtmlbody", helpPath + "$1/$2/$3.html");
                line = line.replaceAll("wiki/doku\\.php/([^>]*):([^>]*)\\?do=export_xhtmlbody", helpPath + "$1/$2.html");
                line = line.replaceAll("wiki/doku\\.php/([^>]*)\\?do=export_xhtmlbody", helpPath + "$1.html");
                //images
                line = line.replaceAll("/wiki/lib/exe/fetch\\.php/([^>]*):([^>]*):([^>]*):([^>]*)\"", "nbdocs:/" + helpPath + "$1/$2/$3/$4\"");
                line = line.replaceAll("/wiki/lib/exe/fetch\\.php/([^>]*):([^>]*):([^>]*)\"", "nbdocs:/" + helpPath + "$1/$2/$3\"");
                line = line.replaceAll("/wiki/lib/exe/fetch\\.php/([^>]*):([^>]*)\"", "nbdocs:/" + helpPath + "$1/$2\"");
                line = line.replaceAll("/wiki/lib/exe/fetch\\.php/([^>]*)\"", "nbdocs:/" + helpPath + "$1\"");
//                line = line.replaceAll("/wiki/lib/exe/fetch\\.php?([^>]*)\"", "nbdocs:/" + helpPath + "external/$1\"").replaceAll("[_[^\\w\\däüö:ÄÜÖ\\/\\+\\-\\. ]]", "_");

                line = line.replaceAll("<a href=([^>]*)><img src=\"([^\"]*)\"([^>]*)></a>", "<img src=\"$2\">");
                line = line.replaceAll("<img src=\"([^>]*)\\?([^>]*)\">", "<img src=\"$1\">");
                //                                      vvvv------v
                //line=line.replaceAll("<span([^>]*)>(.*(?<!/span>))</span>","$2");
                //remove class, name and id from tags
                line = line.replaceAll(" class=\"([^>]*)\">", ">");
                line = line.replaceAll(" name=\"([^>]*)\">", ">");
                line = line.replaceAll(" id=\"([^>]*)\">", ">");
                //remove obnoxious spans using negative look-behind..
                line = line.replaceAll("<span>([^>]*)</span>", "$1");
                //remove links to http://www.google.com/search added by wiki
                line = line.replaceAll("<a href=\"http://www\\.google\\.com/search([^\"]*)\"(.*)>(.*)</a>", "$3");
                //make external links netbeans help external links
//                line = line.replaceAll("<a href=\"http://([^\"]*)\"(.*)>(.*)</a>", "<object classid=\"java:org.netbeans.modules.javahelp.BrowserDisplayer\"><param name=\"content\" value=\"http://$1\"><param name=\"text\" value=\"<html><u>$3</u></html>\"><param name=\"textColor\" value=\"blue\"></object>");
//                line = line.replaceAll("<a href=\"https://([^\"]*)\"(.*)>(.*)</a>", "<object classid=\"java:org.netbeans.modules.javahelp.BrowserDisplayer\"><param name=\"content\" value=\"https://$1\"><param name=\"text\" value=\"<html><u>$3</u></html>\"><param name=\"textColor\" value=\"blue\"></object>");
                line = line.replaceAll("<a href=\"http://([^\"]*)\">([^<]*)</a>", "<object classid=\"java:org.netbeans.modules.javahelp.BrowserDisplayer\"><param name=\"content\" value=\"http://$1\"><param name=\"text\" value=\"<html><u>$2</u></html>\"><param name=\"textColor\" value=\"blue\"></object>");
                line = line.replaceAll("<a href=\"https://([^\"]*)\">([^<]*)</a>", "<object classid=\"java:org.netbeans.modules.javahelp.BrowserDisplayer\"><param name=\"content\" value=\"https://$1\"><param name=\"text\" value=\"<html><u>$2</u></html>\"><param name=\"textColor\" value=\"blue\"></object>");
                //other stuff
//                line = line.replaceAll("<note [^>]*>([^>]*)</note>", "<p>$2</p>");

                out.write(line + "\n");
                line = rdr.readLine();
            }
            rdr.close();
            FileWriter outWriter = new FileWriter(file);
            out.flush();
            outWriter.write(out.toString());
            out.close();
            outWriter.close();
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
        if (helpPath == null) {
            throw new BuildException("You must specify a help path.");
        }
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setHelpPath(String helpPath) {
        this.helpPath = helpPath;
    }
}
