/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.system;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pulls in version info from the version.properties file.
 * 
 * @author Kirill Vainer
 */
public class JmeVersion {
    
    private static final Logger logger = Logger.getLogger(JmeVersion.class.getName());
    private static final Properties props = new Properties();
    
    static {
        try {
            props.load(JmeVersion.class.getResourceAsStream("version.properties"));
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Unable to read version info!", ex);
        }
    }
    
    public static final String BUILD_DATE       = props.getProperty("build.date", "1900-01-01");
    public static final String BRANCH_NAME      = props.getProperty("git.branch", "unknown");
    public static final String GIT_HASH         = props.getProperty("git.hash", "");
    public static final String GIT_SHORT_HASH   = props.getProperty("git.hash.short", "");
    public static final String GIT_TAG          = props.getProperty("git.tag", "");
    public static final String VERSION_NUMBER   = props.getProperty("version.number", "");
    public static final String VERSION_TAG      = props.getProperty("version.tag", "");
    public static final String VERSION_FULL     = props.getProperty("version.full", "");
    public static final String FULL_NAME        = props.getProperty("name.full", "jMonkeyEngine (unknown version)");
}
