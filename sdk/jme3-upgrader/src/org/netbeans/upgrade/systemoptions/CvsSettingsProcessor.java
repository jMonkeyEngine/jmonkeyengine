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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

import java.util.*;

/**
 * Imports CVS root settings: external SSH command
 *
 * @author Maros Sandor
 */
public class CvsSettingsProcessor extends PropertyProcessor {

    private final String FIELD_SEPARATOR = "<~>";
    
    public CvsSettingsProcessor() {
        super("org.netbeans.modules.versioning.system.cvss.settings.CvsRootSettings.PersistentMap");
    }

    void processPropertyImpl(String propertyName, Object value) {
        if ("rootsMap".equals(propertyName)) { // NOI18N
            List mapData = ((SerParser.ObjectWrapper) value).data;
            int n = 0;
            int idx = 3;
            if (mapData.size() > 3) {
                for (;;) {
                    if (idx + 2 > mapData.size()) break;
                    String root = (String) mapData.get(idx);
                    List rootData = ((SerParser.ObjectWrapper) mapData.get(idx + 1)).data;
                    try {
                        List extSettingsData = ((SerParser.ObjectWrapper) ((SerParser.NameValue) rootData.get(0)).value).data;
                        Boolean extRememberPassword = (Boolean) ((SerParser.NameValue) extSettingsData.get(0)).value;
                        Boolean extUseInternalSSH = (Boolean) ((SerParser.NameValue) extSettingsData.get(1)).value;
                        String extCommand = (String) ((SerParser.NameValue) extSettingsData.get(2)).value;
                        String extPassword = (String) ((SerParser.NameValue) extSettingsData.get(3)).value;
                        String setting = root + FIELD_SEPARATOR + extUseInternalSSH + FIELD_SEPARATOR + extRememberPassword + FIELD_SEPARATOR + extCommand;
                        if (extPassword != null && !extPassword.equals("null")) setting += FIELD_SEPARATOR + extPassword; 
                        addProperty("cvsRootSettings" + "." + n, setting);
                        n++;
                    } catch (Exception e) {
                        // the setting is not there => nothing to import
                    }
                    idx += 2;
                }
            }
        }  else {
            throw new IllegalStateException();
        }
    }
}
