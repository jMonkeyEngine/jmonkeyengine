package com.jme.ant;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.LogLevel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author normenhansen
 */
public class UpdateNetBeansDependencies extends Task {

    File projectFile;
    File platformFolder;
    private HashMap<String, String> versionMap = new HashMap<String, String>();

    @Override
    public void execute() throws BuildException {
        if (projectFile == null || platformFolder == null) {
            throw new BuildException("Please set projectfile and version");
        }
        clearVesionMap();
        gatherVersionMap(platformFolder);
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
                    Element runDep = findChildElement(elem, "run-dependency");
                    if (runDep != null) {
                        Element specVersion = findChildElement(runDep, "specification-version");
                        if (specVersion != null) {
                            String name = base.getTextContent().trim();
                            String version = specVersion.getTextContent().trim();
                            String newVersion = versionMap.get(name);
                            if (newVersion != null && !newVersion.equals(version)) {
                                specVersion.setTextContent(newVersion);
                                change = true;
                                log("Updating dependency in for " + name + " to " + newVersion);
                            } else {
                                log("Unknown " + name + ", cannot update dependency.", LogLevel.WARN.getLevel());
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

    public void setProjectFile(File projectFile) {
        this.projectFile = projectFile;
    }

    public void setPlatformFolder(File platformFolder) {
        this.platformFolder = platformFolder;
    }

    private void clearVesionMap() {
        versionMap.clear();
    }

    private void gatherVersionMap(File baseFolder) {
        File[] packages = baseFolder.listFiles();
        for (File pkg : packages) {
            if (pkg.isDirectory()) {
                for (File utr : pkg.listFiles()) {
                    if (utr.isDirectory() && utr.getName().equals("update_tracking")) {
                        File[] xmls = utr.listFiles();
                        for (File file : xmls) {
                            if (file.getName().toLowerCase().endsWith(".xml")) {
                                parseModules(file);
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseModules(File file) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
            if (doc == null) {
                return;
            }
            Element moduleElement = doc.getDocumentElement();
            if (moduleElement == null || !moduleElement.getTagName().equalsIgnoreCase("module")) {
                return;
            }
            Element versionElement = findChildElement(moduleElement, "module_version");
            if (versionElement == null) {
                return;
            }
            String name = moduleElement.getAttribute("codename");
            int idx = name.indexOf("/");
            if (idx != -1) {
                name = name.substring(0, idx);
            }
            String version = versionElement.getAttribute("specification_version");
            versionMap.put(name, version);
        } catch (SAXException ex) {
            Logger.getLogger(UpdateNetBeansDependencies.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UpdateNetBeansDependencies.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(UpdateNetBeansDependencies.class.getName()).log(Level.SEVERE, null, ex);
        }
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
