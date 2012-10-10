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
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author rmatous
 */
public class JUnitContentProcessor extends ContentProcessor{
    protected JUnitContentProcessor(String systemOptionInstanceName) {
        super(systemOptionInstanceName);
    }
    
    protected Result parseContent(final Iterator<Object> it, boolean types) {
        Map<String, String> properties = new HashMap<String, String>();
        assert it.hasNext();
        Object o = it.next();
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        SerParser.ObjectWrapper ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Integer") : Utils.getClassNameFromObject(ow);//NOI18N
        properties.put("version", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        assert it.hasNext();
        o = it.next();           
        assert o.getClass().equals(String.class);        
        properties.put("fileSystem", ((types)?"java.lang.String": (String)o));//NOI18N
        o = it.next();           
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Boolean") : Utils.getClassNameFromObject(ow);//NOI18N
        properties.put("membersPublic", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        o = it.next();           
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Boolean") : Utils.getClassNameFromObject(ow);
        properties.put("membersProtected", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        o = it.next();           
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Boolean") : Utils.getClassNameFromObject(ow);//NOI18N
        properties.put("membersPackage", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        o = it.next();           
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Boolean") : Utils.getClassNameFromObject(ow);//NOI18N
        properties.put("bodyComments", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        o = it.next();           
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Boolean") : Utils.getClassNameFromObject(ow);//NOI18N
        properties.put("bodyContent", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        o = it.next();           
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Boolean") : Utils.getClassNameFromObject(ow);//NOI18N
        properties.put("javaDoc", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        o = it.next();           
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Boolean") : Utils.getClassNameFromObject(ow);//NOI18N
        properties.put("generateAbstractImpl", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        o = it.next();           
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Boolean") : Utils.getClassNameFromObject(ow);//NOI18N
        properties.put("generateExceptionClasses", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        o = it.next();           
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Boolean") : Utils.getClassNameFromObject(ow);//NOI18N
        properties.put("generateSuiteClasses", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        o = it.next();           
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Boolean") : Utils.getClassNameFromObject(ow);//NOI18N
        properties.put("includePackagePrivateClasses", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        o = it.next();           
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Boolean") : Utils.getClassNameFromObject(ow);//NOI18N
        properties.put("generateMainMethod", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        o = it.next();           
        assert o.getClass().equals(String.class);        
        properties.put("generateMainMethodBody", ((types)?"java.lang.String": (String)o));//NOI18N
        o = it.next();           
        assert o.getClass().equals(String.class);        
        properties.put("rootSuiteClassName", ((types)?"java.lang.String": (String)o));//NOI18N
        o = it.next();           
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Boolean") : Utils.getClassNameFromObject(ow);//NOI18N
        properties.put("generateSetUp", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        o = it.next();           
        assert o.getClass().equals(SerParser.ObjectWrapper.class);
        ow = (SerParser.ObjectWrapper)o;        
        assert Utils.getClassNameFromObject(ow).equals("java.lang.Boolean") : Utils.getClassNameFromObject(ow);//NOI18N
        properties.put("generateTearDown", ((types)?Utils.getClassNameFromObject(ow): Utils.valueFromObjectWrapper(ow)));//NOI18N
        
        
        return new DefaultResult(systemOptionInstanceName, properties);
    }        
}
