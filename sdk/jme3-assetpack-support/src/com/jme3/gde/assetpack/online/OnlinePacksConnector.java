/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.online;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author normenhansen
 */
public class OnlinePacksConnector {

    public static void upload(String exsistingFileName, String user, String pass) {
        upload("http://jmonkeyengine.org/assetpacks/upload.php", exsistingFileName, user, pass);
    }

    public static void upload(String urlString, String exsistingFileName, String user, String pass) {
        try {
            File file = new File(exsistingFileName);
            int size = (int) FileUtil.toFileObject(file).getSize();
            Logger.getLogger(OnlinePacksConnector.class.getName()).log(Level.FINE, "Upload file size: {0}", size);

            URL url = new URL(urlString);
            String boundary = MultiPartFormOutputStream.createBoundary();
            URLConnection urlConn = MultiPartFormOutputStream.createConnection(url);
            urlConn.setRequestProperty("Accept", "*/*");
            urlConn.setRequestProperty("Content-Type", MultiPartFormOutputStream.getContentType(boundary));
            urlConn.setRequestProperty("Connection", "Keep-Alive");
            urlConn.setRequestProperty("Cache-Control", "no-cache");
            MultiPartFormOutputStream out = new MultiPartFormOutputStream(urlConn.getOutputStream(), boundary);
            // write a text field element
            out.writeField("user", user);
            out.writeField("pass", pass);
            // upload a file
            out.writeFile("file", "application/zip", file);
            // can also write bytes directly
            //out.writeFile("myFile", "text/plain", "C:\\test.txt",
            //	"This is some file text.".getBytes("ASCII"));
            out.close();
            // read response from server
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String line = "";
            while ((line = in.readLine()) != null) {
                if (line.startsWith("Error:")) {
                    line = line.substring(6, line.length()).trim();
                    Confirmation msg = new NotifyDescriptor.Confirmation(
                            "Error uploading to jmonkeyengine.org!\n" + line,
                            NotifyDescriptor.OK_CANCEL_OPTION,
                            NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notifyLater(msg);
                }else{
                    Confirmation msg = new NotifyDescriptor.Confirmation(
                            "Successfully uploaded to jmonkeyengine.org!\n" + line,
                            NotifyDescriptor.OK_CANCEL_OPTION,
                            NotifyDescriptor.INFORMATION_MESSAGE);
                    DialogDisplayer.getDefault().notifyLater(msg);
                }
                System.out.println(line);
            }
            in.close();
        } catch (Exception ex) {
            Logger.getLogger(OnlinePacksConnector.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
        }
    }
}
