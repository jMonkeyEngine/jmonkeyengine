/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.j2seproject;

/**
 *
 * @author normenhansen
 */
import java.util.ResourceBundle;
import javax.swing.JComponent;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.netbeans.spi.project.ui.support.ProjectCustomizer.Category;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

public class AssetsPropertiesPanelProvider implements ProjectCustomizer.CompositeCategoryProvider {

    @ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-java-j2seproject", category="BuildCategory", position = 90)
    public static AssetsPropertiesPanelProvider createAssetsPanel() {
        return new AssetsPropertiesPanelProvider();
    }
    private Project project;

    private AssetsPropertiesPanelProvider() {
    }

    @Override
    public Category createCategory(Lookup lkp) {
        ResourceBundle bundle = NbBundle.getBundle(AssetsPropertiesPanelProvider.class);
        ProjectCustomizer.Category toReturn = null;
        project = lkp.lookup(Project.class);
        if (project == null) {
            return toReturn;
        }
        toReturn = ProjectCustomizer.Category.create(
                "assets",
                bundle.getString("LBL_Config_assets"),
                null);
        return toReturn;
    }

    @Override
    public JComponent createComponent(Category category, Lookup lkp) {
        String nm = category.getName();
        AssetsPropertiesPanel panel = new AssetsPropertiesPanel(project);
        category.setStoreListener(panel);
        return panel;
    }
}
