package jme3tools.shadercheck;

import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shader validator implementation for AMD's GPUShaderAnalyser.
 * 
 * @author Kirill Vainer
 */
public class GpuAnalyzerValidator implements Validator {

    private static final Logger logger = Logger.getLogger(CgcValidator.class.getName());
    private static String version;
    
    private static String checkGpuAnalyzerVersion(){
        try {
            ProcessBuilder pb = new ProcessBuilder("GPUShaderAnalyzer", "-ListModules");
            Process p = pb.start();
            
            Scanner scan = new Scanner(p.getInputStream());
            String ln = scan.nextLine();
            scan.close();
            
            p.destroy();
            
            return ln;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "IOEx", ex);
        }
        return null;
    }
    
    public String getName() {
        return "AMD GPU Shader Analyzer";
    }

    public boolean isInstalled() {
        return getInstalledVersion() != null;
    }

    public String getInstalledVersion() {
        if (version == null){
            version = checkGpuAnalyzerVersion();
        }
        return version;
    }
    private static void executeAnalyzer(String sourceCode, String language, String defines, String asic, StringBuilder results){
        try {
            // Export sourcecode to temporary file
            File tempFile = File.createTempFile("test_shader", ".glsl");
            FileWriter writer = new FileWriter(tempFile);
            
            String glslVer = language.substring(4);
            writer.append("#version ").append(glslVer).append('\n');
            writer.append("#extension all : warn").append('\n');
            writer.append(defines).append('\n');
            writer.write(sourceCode);
            writer.close();

            ProcessBuilder pb = new ProcessBuilder("GPUShaderAnalyzer", 
                                                   tempFile.getAbsolutePath(),
                                                   "-I",
                                                   "-ASIC", asic);
                                                   
            Process p = pb.start();
            
            Scanner scan = new Scanner(p.getInputStream());
            
            if (!scan.hasNextLine()){
                String x = scan.next();
                System.out.println(x);
            }
            
            String ln = scan.nextLine();
            
            if (ln.startsWith(";")){
                results.append(" - Success!").append('\n');
            }else{
                results.append(" - Failure!").append('\n');
                results.append(ln).append('\n');
                while (scan.hasNextLine()){
                    results.append(scan.nextLine()).append('\n');
                }
            }

            scan.close();
            p.getOutputStream().close();
            p.getErrorStream().close();
            
            p.waitFor();
            p.destroy();
            
            tempFile.delete();
        } catch (InterruptedException ex) {
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "IOEx", ex);
        }
    }
    
    public void validate(Shader shader, StringBuilder results) {
        for (ShaderSource source : shader.getSources()){
            results.append("Checking: ").append(source.getName());
            switch (source.getType()){
                case Fragment:
                    executeAnalyzer(source.getSource(), source.getLanguage(), source.getDefines(), "HD5770", results);
                    break;
                case Vertex:
                    executeAnalyzer(source.getSource(), source.getLanguage(), source.getDefines(), "HD5770", results);
                    break;
            }
        }
    }
    
}
