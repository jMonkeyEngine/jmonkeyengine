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

import java.util.HashMap;
import java.util.Map;


abstract class PropertyProcessor  {
    private String className;
    private static Map<String, String> results;
    private static Map<String, PropertyProcessor> clsname2Delegate = new HashMap<String, PropertyProcessor>();
    
    static {
        //To extend behaviour of this class then regisetr your own implementation
        registerPropertyProcessor(new TaskTagsProcessor());
        registerPropertyProcessor(new HostPropertyProcessor());
        registerPropertyProcessor(new FileProcessor());//AntSettings
        registerPropertyProcessor(new NbClassPathProcessor());//AntSettings
        registerPropertyProcessor(new HashMapProcessor());//AntSettings
        registerPropertyProcessor(new IntrospectedInfoProcessor());//AntSettings                
        registerPropertyProcessor(new ListProcessor());//ProjectUISettings             
        registerPropertyProcessor(new URLProcessor());//ProjectUISettings             
        registerPropertyProcessor(new ColorProcessor(ColorProcessor.JAVA_AWT_COLOR));//FormLoaderSettings
        registerPropertyProcessor(new ColorProcessor(ColorProcessor.NETBEANS_COLOREDITOR_SUPERCOLOR));//FormLoaderSettings
        registerPropertyProcessor(new StringPropertyProcessor());//ProxySettings
        registerPropertyProcessor(new HashSetProcessor(HashSetProcessor.CVS_PERSISTENT_HASHSET));//CvsSettings
        registerPropertyProcessor(new HashSetProcessor(HashSetProcessor.SVN_PERSISTENT_HASHSET));//SvnSettings
        registerPropertyProcessor(new CvsSettingsProcessor());
        registerPropertyProcessor(new DocumentationSettingsProcessor());
    }           


    private static void registerPropertyProcessor(PropertyProcessor instance) {
        if (clsname2Delegate.put(instance.className, instance) != null) {
            throw new IllegalArgumentException();
        }
    }
    
    private static PropertyProcessor DEFAULT = new PropertyProcessor(false) {
        void processPropertyImpl(final String propertyName, final Object value) {
            String stringvalue = null;
            stringvalue = Utils.valueFromObjectWrapper(value);
            addProperty(propertyName, stringvalue);
        }
    };
    
    private static PropertyProcessor TYPES = new PropertyProcessor(true) {
        void processPropertyImpl(final String propertyName, final Object value) {
            addProperty(propertyName, Utils.getClassNameFromObject(value));
        }        
    };
    
    private boolean types;
    
    
    private PropertyProcessor(boolean types) {
        this.types = types;
    }
    
    protected PropertyProcessor(String className) {
        this(false);        
        this.className = className;
    }
    
    static Map<String, String> processProperty(String propertyName, Object value, boolean types) {
        results = new HashMap<String, String>();
        PropertyProcessor p = (types) ? TYPES : findDelegate(value);
        if (p == null) {
            p = DEFAULT;
        }
        assert p != null;
        p.processPropertyImpl(propertyName, value);
        return results;
    }
    
    abstract void processPropertyImpl(String propertyName, Object value);
    
    protected final void addProperty(String propertyName, String value) {
        if (results.put(propertyName, value) != null) {
            throw new IllegalArgumentException(propertyName);
        }
    }
    
    private static PropertyProcessor findDelegate(final Object value) {
        String clsName = Utils.getClassNameFromObject(value);
        return (PropertyProcessor)clsname2Delegate.get(clsName);
    }       
}
