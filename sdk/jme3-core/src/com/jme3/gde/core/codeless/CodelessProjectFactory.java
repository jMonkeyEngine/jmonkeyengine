
package com.jme3.gde.core.codeless;

import java.io.IOException;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;

public class CodelessProjectFactory implements ProjectFactory {

    public static final String CONFIG_NAME="assets.jmp";

    //Specifies when a project is a project, i.e. properties file exists
    @Override
    public boolean isProject(FileObject projectDirectory) {
        if(projectDirectory.getFileObject(CONFIG_NAME)!=null){
            return true;
        }
        return false;
    }

    //Specifies when the project will be opened, i.e.,
    //if the project exists:
    @Override
    public Project loadProject(FileObject dir, ProjectState state) throws IOException {
        return isProject(dir) ? new CodelessProject(dir, state) : null;
    }

    @Override
    public void saveProject(final Project project) throws IOException, ClassCastException {
        FileObject projectRoot = project.getProjectDirectory();
        if (projectRoot.getFileObject(CONFIG_NAME) == null) {
            throw new IOException("Project Settings " + projectRoot.getPath() +
                    " deleted," +
                    " cannot save project");
        }
    }
    
}
