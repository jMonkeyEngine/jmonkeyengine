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

package org.netbeans.upgrade;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.openide.util.Union2;



/** A test that is initialized based on includes and excludes.
 *
 * @author Jaroslav Tulach
 */
final class IncludeExclude extends AbstractSet {
    /** List<Boolean and Pattern>
     */
    private List<Union2<Boolean, Pattern>> patterns = new ArrayList<Union2<Boolean, Pattern>> ();

    private IncludeExclude () {
    }

    /** Reads the include/exclude set from a given reader.
     * @param r reader
     * @return set that accepts names based on include exclude from the file
     */
    public static IncludeExclude create (Reader r) throws IOException {
        IncludeExclude set = new IncludeExclude ();
        
        BufferedReader buf = new BufferedReader (r);
        for (;;) {
            String line = buf.readLine ();
            if (line == null) break;
            
            line = line.trim ();
            if (line.length () == 0 || line.startsWith ("#")) {
                continue;
            }
            
            Boolean plus;
            if (line.startsWith ("include ")) {
                line = line.substring (8);
                plus = Boolean.TRUE;
            } else {
                if (line.startsWith ("exclude ")) {
                    line = line.substring (8);
                    plus = Boolean.FALSE;
                } else {
                    throw new java.io.IOException ("Wrong line: " + line);
                }
            }
            
            Pattern p = Pattern.compile (line);
            
            set.patterns.add (Union2.<Boolean,Pattern>createFirst(plus));
            set.patterns.add (Union2.<Boolean,Pattern>createSecond(p));
        }
        
        return set; 
    }
    
    
    public Iterator iterator () {
        return null;
    }
    
    public int size () {
        return 0;
    }
    
    @Override
    public boolean contains (Object o) {
        String s = (String)o;
        
        boolean yes = false;
        
        Iterator<Union2<Boolean,Pattern>> it = patterns.iterator ();
        while (it.hasNext ()) {
            Boolean include = it.next ().first();
            Pattern p = it.next ().second();
            
            Matcher m = p.matcher (s);
            if (m.matches ()) {
                yes = include.booleanValue ();
                if (!yes) {
                    // exclude matches => immediately return
                    return false;
                }
            }
        }
        
        return yes;
    }
    
}
