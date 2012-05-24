package com.jme.ant;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author normenhansen
 */
public class UpdateSdkDependencies extends Task {

    File projectFile;
    String version;
    String basePackage = "com.jme3.gde";

    @Override
    public void execute() throws BuildException {
        if (projectFile == null || version == null) {
            throw new BuildException("Please set projectfile and version");
        }
        try {
            boolean change = false;
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(projectFile);
            Element project = doc.getDocumentElement();
            if (project == null) {
                return;
            }
            Element configuration = findChildElement(project, "configuration");
            if (configuration == null) {
                return;
            }
            Element data = findChildElement(configuration, "data");
            if (data == null) {
                return;
            }
            Element deps = findChildElement(data, "module-dependencies");
            if (deps == null) {
                return;
            }
            NodeList list = deps.getElementsByTagName("dependency");
            for (int i = 0; i < list.getLength(); i++) {
                Element elem = (Element) list.item(i);
                Element base = findChildElement(elem, "code-name-base");
                if (base != null) {
                    if (base.getTextContent().trim().startsWith(basePackage)) {
                        Element runDep = findChildElement(elem, "run-dependency");
                        if (runDep != null) {
                            Element specVersion = findChildElement(runDep, "specification-version");
                            if (specVersion != null && !version.equals(specVersion.getTextContent().trim())) {
                                specVersion.setTextContent(version);
                                log("Updating plugin dependency in " + projectFile);
                                change = true;
                            }
                        }
                    }
                }
            }
            if (change) {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(projectFile);
                transformer.transform(source, result);
                OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(projectFile, true)));
                out.write("\n");
                out.close();
            }
        } catch (Exception ex) {
            throw new BuildException("Error changing file: " + ex);
        }
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public void setProjectFile(File projectFile) {
        this.projectFile = projectFile;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    private Element findChildElement(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        org.w3c.dom.Node ret = parent.getFirstChild();
        while (ret != null && (!(ret instanceof Element) || !ret.getNodeName().equals(name))) {
            ret = ret.getNextSibling();
        }
        return (Element) ret;
    }

}
