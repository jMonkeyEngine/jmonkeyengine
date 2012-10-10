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
 * Portions Copyrighted 2007 Sun Microsystems, Inc.
 */
package org.netbeans.upgrade.systemoptions;

import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Tomas Stupka
 */
class HashSetProcessor extends PropertyProcessor {

    static final String CVS_PERSISTENT_HASHSET = "org.netbeans.modules.versioning.system.cvss.settings.CvsModuleConfig.PersistentHashSet";  // NOI18N
    static final String SVN_PERSISTENT_HASHSET = "org.netbeans.modules.subversion.settings.SvnModuleConfig.PersistentHashSet";              // NOI18N
    
    HashSetProcessor(String className) {
        super(className);
    }
    
    void processPropertyImpl(String propertyName, Object value) {
        if ("commitExclusions".equals(propertyName)) { // NOI18N
            List l = ((SerParser.ObjectWrapper) value).data;
            int c = 0;
            for (Iterator it = l.iterator(); it.hasNext();) {
                Object elem = it.next();
                if(elem instanceof String) {
                    addProperty(propertyName + "." + c, (String) elem);
                    c = c + 1;
                }
            }
        }  else {
            throw new IllegalStateException();
        }
    }    
}
