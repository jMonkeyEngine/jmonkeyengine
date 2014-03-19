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

import java.util.Iterator;
import java.util.List;
import org.netbeans.upgrade.systemoptions.SerParser.ArrayWrapper;
import org.netbeans.upgrade.systemoptions.SerParser.NameValue;
import org.netbeans.upgrade.systemoptions.SerParser.ObjectWrapper;

/**
 *
 * @author rmatous
 */
final class Utils {
    
    /** Creates a new instance of Utils */
    private Utils() {}

    
    static String valueFromObjectWrapper(final Object value) {
        String stringvalue = null;
        if (value instanceof ObjectWrapper) {
            List l = ((SerParser.ObjectWrapper)value).data;
            if (l.size() == 1) {
                Object o = l.get(0);
                if (o instanceof NameValue) {
                    Object key = null;
                    stringvalue = ((NameValue) o).value.toString();
                }
            }
            if (stringvalue == null) {
                stringvalue = ((ObjectWrapper) value).classdesc.name;
            }
        }  else if (value instanceof String && !"null".equals(value)) {
            stringvalue = value.toString();
            
        } else if (value instanceof SerParser.ArrayWrapper && "[Ljava.lang.String;".equals(((SerParser.ArrayWrapper)value).classdesc.name)) {
            StringBuffer sb = new StringBuffer();
            List es = ((SerParser.ArrayWrapper)value).values;
            for (Iterator it = es.iterator(); it.hasNext();) {
                sb.append((String)it.next());
                if (it.hasNext()) {
                    sb.append(" , ");
                }                
            }
            stringvalue = sb.toString();            
        } else if (value instanceof SerParser.ArrayWrapper && "[[Ljava.lang.String;".equals(((SerParser.ArrayWrapper)value).classdesc.name)) {
            StringBuffer sb = new StringBuffer();
            List awl = ((SerParser.ArrayWrapper)value).values;
            for (Iterator it = awl.iterator(); it.hasNext();) {
                SerParser.ArrayWrapper aw = (SerParser.ArrayWrapper)it.next();
                sb.append(valueFromObjectWrapper(aw));
                if (it.hasNext()) {
                    sb.append(" | ");
                }
            }
            stringvalue = sb.toString();            
        } else {
            stringvalue = "unknown";//value.toString();
        }
        return stringvalue;
    }
    
    static String getClassNameFromObject(final Object value) {
        String clsName = null;
        if (value instanceof ObjectWrapper) {
            clsName = prettify(((ObjectWrapper) value).classdesc.name);
        }  else if (value instanceof ArrayWrapper) {
            clsName = prettify(((ArrayWrapper) value).classdesc.name);
        }  else {
            clsName = prettify(value.getClass().getName());
        }
        return clsName;
    }
    
    static String prettify(String type) {
        if (type.equals("B")) { // NOI18N
            return "byte"; // NOI18N
        } else if (type.equals("S")) { // NOI18N
            return "short"; // NOI18N
        } else if (type.equals("I")) { // NOI18N
            return "int"; // NOI18N
        } else if (type.equals("J")) { // NOI18N
            return "long"; // NOI18N
        } else if (type.equals("F")) { // NOI18N
            return "float"; // NOI18N
        } else if (type.equals("D")) { // NOI18N
            return "double"; // NOI18N
        } else if (type.equals("C")) { // NOI18N
            return "char"; // NOI18N
        } else if (type.equals("Z")) { // NOI18N
            return "boolean"; // NOI18N
        } else if (type.startsWith("L") && type.endsWith(";")) { // NOI18N
            String fqn = type.substring(1, type.length() - 1).replace('/', '.').replace('$', '.'); // NOI18N
            return fqn;
        }
        if (!type.startsWith("[")) {
            if (type.startsWith("L")) {
                return type.substring(1);
            }
            if (type.endsWith(";")) {
                return type.substring(0,type.length()-1);
            }
        }
        return type;
    }
}
