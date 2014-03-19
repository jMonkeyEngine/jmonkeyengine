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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class ContentProcessor  {
    private static Map<String, ContentProcessor> clsname2Delegate = new HashMap<String, ContentProcessor>();
    protected String systemOptionInstanceName;
    
    static {
        registerContentProcessor(new JUnitContentProcessor("org.netbeans.modules.junit.JUnitSettings"));//NOI18N
    }
    
    private static void registerContentProcessor(ContentProcessor instance) {
        if (clsname2Delegate.put(instance.systemOptionInstanceName, instance) != null) {
            throw new IllegalArgumentException();
        }
    }
        
            
    protected ContentProcessor(String systemOptionInstanceName) {
        this.systemOptionInstanceName = systemOptionInstanceName;
    }
            
    protected Result parseContent(final Iterator<Object> it, boolean types) {
        Map<String, String> m;
        Result result = null;
        try {
            Map<String, Object> props = parseProperties(it);
            assert props != null;
            //debugInfo("before: ", m);                        
            m = processProperties(props, types);
            //assert debugInfo("after: ", m);
            result = new DefaultResult(systemOptionInstanceName, m);
        } catch (IllegalStateException isx) {
            Logger.getLogger(ContentProcessor.class.getName()).log(Level.WARNING, systemOptionInstanceName + " not parsed", isx);
        }
        return result;        
    }
    
    static Result parseContent(String systemOptionInstanceName, boolean types, final Iterator<Object> it) {
        ContentProcessor cp = clsname2Delegate.get(systemOptionInstanceName);
        if (cp == null) {
            cp = new ContentProcessor(systemOptionInstanceName);
        }
        return cp.parseContent(it, types);
    }
    
    private final Map<String, String> processProperties(final Map<String, Object> properties, boolean types) {
        Map<String, String> allProps = new HashMap<String, String>();
        for (Iterator<Map.Entry<String, Object>> it = properties.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = it.next();
            String name = entry.getKey();
            Object value = entry.getValue();
            allProps.putAll(PropertyProcessor.processProperty(name, value, types));
        }
        return allProps;
    }
    
    private final  Map<String, Object> parseProperties(final Iterator<Object> it) { // sequences String, Object, SerParser.ObjectWrapper
        Map<String, Object> properties = new HashMap<String, Object>();
        for (; it.hasNext();) {
            Object name = it.next();
            if ("null".equals(name) || name == null) {
                //finito
                return properties;
            } else if (!(name instanceof String)) {
                throw new IllegalStateException(name.getClass().getName());
            } else {
                if (!it.hasNext()) {
                    throw new IllegalStateException(name.toString());
                }
                Object value = it.next();
                properties.put((String)name, value);
                Object propertyRead = it.next();
                if (!(propertyRead instanceof SerParser.ObjectWrapper )) {
                    throw new IllegalStateException(propertyRead.getClass().getName());
                } else {
                    SerParser.ObjectWrapper ow = (SerParser.ObjectWrapper)propertyRead;
                    if (!ow.classdesc.name.endsWith("java.lang.Boolean;")) {//NOI18N
                        throw new IllegalStateException(ow.classdesc.name);
                    }
                }
            }
        }
        throw new IllegalStateException("Unexpected end");//NOI18N
    }        
}
