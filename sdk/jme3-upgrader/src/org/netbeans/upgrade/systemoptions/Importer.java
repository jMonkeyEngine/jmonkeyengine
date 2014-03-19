/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.upgrade.systemoptions;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.*;

/**
 *
 * @author Radek Matous
 */
public class Importer {
    private static final String DEFINITION_OF_FILES =  "systemoptionsimport";//NOI18N
            
    public static void doImport() throws IOException  {
        Set<FileObject> files = getImportFiles(loadImportFilesDefinition());
        for (Iterator<DefaultResult> it = parse(files).iterator(); it.hasNext();) {
            saveResult(it.next());
        }
        for (Iterator it = files.iterator(); it.hasNext();) {
            FileObject fo = (FileObject) it.next();
            FileLock fLock = fo.lock();
            try {
                fo.rename(fLock, fo.getName(), "imported");//NOI18N
            } finally {
                fLock.releaseLock();
            }
        }
    }
    
    private static void saveResult(final DefaultResult result) throws IOException {
        String absolutePath = "/"+result.getModuleName();
        PropertiesStorage ps = PropertiesStorage.instance(absolutePath);
        Properties props = ps.load();
        String[] propertyNames = result.getPropertyNames();
        for (int i = 0; i < propertyNames.length; i++) {
            String val = result.getProperty(propertyNames[i]);
            if (val != null) {
                props.put(propertyNames[i], val);
            }
        }
        if (props.size() > 0) {
            ps.save(props);
        }
    }
    
    private static Set<DefaultResult> parse(final Set<FileObject> files) {
        Set<DefaultResult> retval = new HashSet<DefaultResult>();
        for (FileObject f: files) {
            try {
                retval.add(SystemOptionsParser.parse(f, false));
            } catch (Exception ex) {
                boolean assertOn = false;
                assert assertOn = true;
                if (assertOn) {
                    Logger.getLogger("org.netbeans.upgrade.systemoptions.parse").log(Level.INFO, "importing: " + f.getPath(), ex); // NOI18N
                }
                continue;
            }
        }
        return retval;
    }
    

    static Properties loadImportFilesDefinition() throws IOException {
        Properties props = new Properties();
        InputStream is = Importer.class.getResourceAsStream(DEFINITION_OF_FILES);
        try {
            props.load(is);
        } finally {
            is.close();
        }
        return props;
    }

    private static Set<FileObject> getImportFiles(final Properties props) {
        Set<FileObject> fileobjects = new HashSet<FileObject>();        
        for (Iterator it = props.keySet().iterator(); it.hasNext();) {
            String path = (String) it.next();
            FileObject f = FileUtil.getConfigFile(path);
            if (f != null) {
                fileobjects.add(f);
            }
        }
        return fileobjects;
    }
    
    /** Creates a new instance of SettingsReadSupport */
    private Importer() {}    
}
