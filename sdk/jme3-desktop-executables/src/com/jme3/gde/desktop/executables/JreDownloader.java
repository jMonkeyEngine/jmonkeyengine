/*
 *  Copyright (c) 2009-2015 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.desktop.executables;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.RequestProcessor;

/**
 * Downloads Oracle JRE runtimes for a specific platform (windows-i586,
 * windows-x64, linux-i586, linux-x64, maxosx-x64), uses the platform being run
 * on to determine version to download.
 *
 * @author normenhansen
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class JreDownloader {

    private static final Logger logger = Logger.getLogger(JreDownloader.class.getName());
    private static final RequestProcessor processor = new RequestProcessor("JRE Downloader", 1);

    /**
     * Download a specific platforms JRE to the location specified, a tar.gz
     * file will be downloaded so the location parameter should end with tar.gz
     *
     * @param platform The platform to download for (windows-i586, windows-x64,
     * linux-i586, linux-x64, maxosx-x64)
     * @param location The absolute file path to download to.
     */
    public static void downloadJre(String platform, String location) {
        String property = System.getProperty("java.runtime.version");
        Matcher m = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)_(\\d+)\\-b(\\d+)").matcher(property);
        if (m.matches()) {
//            "http://download.oracle.com/otn-pub/java/jdk/${jv.minor}u${jv.update}-b${jv.build}/jre-${jv.minor}u${jv.update}-${platform.durl}.tar.gz";
            String urlString = "http://download.oracle.com/otn-pub/java/jdk/" + m.group(2) + "u" + m.group(4) + "-b" + m.group(5) + "/jre-" + m.group(2) + "u" + m.group(4) + "-" + platform + ".tar.gz";
            attemptDownload(urlString, new File(location));
        }
    }

    private static void attemptDownload(String newUrl, File dest) {
        logger.log(Level.INFO, "Attempt to download JRE from {0}", newUrl);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(newUrl).openConnection();
            connection.setRequestProperty("Cookie", "gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie");
            connection.setAllowUserInteraction(false);
            connection.setInstanceFollowRedirects(true);
            connection.connect();
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                downloadFile(connection, dest);
            } else if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
                handleRedirect(connection, dest);
            } else {
                logger.log(Level.WARNING, "Download of JRE from {0} failed", newUrl);
            }
        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, "{0}", ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "{0}", ex);
        }
    }

    private static void handleRedirect(HttpURLConnection conn, File dest) {
        String newUrl = conn.getHeaderField("Location");
        logger.log(Level.INFO, "JRE download redirected to {0}", newUrl);
        conn.disconnect();
        attemptDownload(newUrl, dest);
    }

    private static void downloadFile(final HttpURLConnection connection, final File dest) {
        logger.log(Level.INFO, "Downloading JRE from {0}", connection.getURL());
        Callable task = new Callable() {

            public Object call() throws Exception {
                long length = connection.getContentLengthLong();
                ProgressHandle progress = ProgressHandleFactory.createHandle("Downloading JRE to " + dest.getName());
                progress.start((int) length);
                BufferedInputStream in = null;
                BufferedOutputStream out = null;
                try {
                    in = new BufferedInputStream(connection.getInputStream());
                    out = new BufferedOutputStream(new FileOutputStream(dest));
                    int input = in.read();
                    int i = 0;
                    while (input != -1) {
                        out.write(input);
                        input = in.read();
                        progress.progress(i);
                        i++;
                    }
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "{0}", ex);
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "{0}", ex);
                    }
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "{0}", ex);
                    }
                    connection.disconnect();
                    progress.finish();
                }
                return null;
            }
        };
        processor.submit(task);
    }
}
