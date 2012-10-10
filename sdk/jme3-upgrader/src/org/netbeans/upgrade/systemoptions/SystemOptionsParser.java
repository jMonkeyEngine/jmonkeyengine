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
import java.util.Iterator;
import java.util.Set;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Radek Matous
 */
public class SystemOptionsParser  {
    static final String EXPECTED_INSTANCE = "org.openide.options.SystemOption";//NOI18N
    
    private String systemOptionInstanceName;
    private boolean types;
    
    private SystemOptionsParser(final String systemOptionInstanceName, final boolean types) {
        this.systemOptionInstanceName = systemOptionInstanceName;
        this.types = types;
    }
    
    public static DefaultResult parse(FileObject settingsFo, boolean types) throws IOException, ClassNotFoundException {
        SettingsRecognizer instance = getRecognizer(settingsFo);
        
        SystemOptionsParser rImpl = null;
        InputStream is = instance.getSerializedInstance();
        try {
            SerParser sp = new SerParser(is);
            SerParser.Stream s = sp.parse();
            rImpl = new SystemOptionsParser(instance.instanceName(), types);
            DefaultResult ret = (DefaultResult)rImpl.processContent(s.contents.iterator(), false);
            ret.setModuleName(instance.getCodeNameBase().replace('.','/'));
            return ret;
        } finally {
            is.close();
        }
    }
    
    private Result processContent(final Iterator<Object> it, final boolean reachedWriteReplace) {
        for (; it.hasNext();) {
            Object elem = it.next();
            if (!reachedWriteReplace && elem instanceof SerParser.ObjectWrapper) {
                SerParser.ObjectWrapper ow = (SerParser.ObjectWrapper)elem;
                String name = ow.classdesc.name;
                if (name.endsWith("org.openide.util.SharedClassObject$WriteReplace;")) {//NOI18N
                    return processContent(ow.data.iterator(), true);
                }
            } else if (reachedWriteReplace && elem instanceof SerParser.NameValue ) {
                SerParser.NameValue nv = (SerParser.NameValue)elem;
                if (systemOptionInstanceName.equals(nv.value)) {
                        Result result = ContentProcessor.parseContent(systemOptionInstanceName, types, it);
                    return result;
                }
            }
        }
        return null;
    }            
            
    private static SettingsRecognizer getRecognizer(final FileObject settingsFo) throws IOException {
        SettingsRecognizer recognizer = new SettingsRecognizer(false, settingsFo);
        recognizer.parse();
        
        Set instances = recognizer.getInstanceOf();
        String iName = recognizer.instanceName();
        if (!instances.contains(EXPECTED_INSTANCE)) {
            throw new IOException(iName);
        }
        return recognizer;
    }
}




