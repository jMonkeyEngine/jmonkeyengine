package jme3tools.shadercheck;

import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderSource;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CgcValidator implements Validator {

    private static final Logger logger = Logger.getLogger(CgcValidator.class.getName());
    private static String version;
    
    private static String checkCgCompilerVersion(){
        try {
            ProcessBuilder pb = new ProcessBuilder("cgc", "--version");
            Process p = pb.start();
            
            Scanner scan = new Scanner(p.getErrorStream());
            String ln = scan.nextLine();
            scan.close();
            
            p.waitFor();
            
            String versionNumber = ln.split("\\s")[2];
            return versionNumber.substring(0, versionNumber.length()-1);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "IOEx", ex);
        } catch (InterruptedException ex){
        }
        return null;
    }
    
    public String getName() {
        return "NVIDIA Cg Toolkit";
    }

    public boolean isInstalled() {
        return getInstalledVersion() != null;
    }

    public String getInstalledVersion() {
        if (version == null){
            version = checkCgCompilerVersion();
        }
        return version;
    }

    private static void executeCg(String sourceCode, String language, String defines, String profile, StringBuilder output){
        try {
            ProcessBuilder pb = new ProcessBuilder("cgc", "-oglsl", 
                                                          "-nocode", 
                                                          "-strict", 
                                                          "-glslWerror", 
                                                          "-profile", profile, 
                                                          //"-po", "NumMathInstructionSlots=64",// math instruction slots
                                                          //"-po", "MaxTexIndirections=4",     // texture indirections
                                                          "-po", "NumTemps=32",              // temporary variables
                                                          //"-po", "NumInstructionSlots=1",    // total instruction slots
                                                          //"-po", "NumTexInstructionSlots=32",// texture instruction slots 
                                                          "-po", "MaxLocalParams=32");         // local parameters
                                                          
            
            Process p = pb.start();
            
            String glslVer = language.substring(4);
            
            OutputStreamWriter writer = new OutputStreamWriter(p.getOutputStream());
            writer.append("#version ").append(glslVer).append('\n');
            writer.append("#extension all : warn").append('\n');
            writer.append(defines).append('\n');
            writer.write(sourceCode);
            writer.close();
            
            Scanner scan = new Scanner(p.getErrorStream());
            String ln = scan.nextLine();
            if (ln.contains("0 errors")){
                output.append(" - Success!").append('\n');
            }else{
                output.append(" - Failure!").append('\n');
                output.append(ln).append('\n');
                while (scan.hasNextLine()){
                    output.append(scan.nextLine()).append('\n');
                }
            }
            scan.close();

            p.waitFor();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "IOEx", ex);
        } catch (InterruptedException ex){
        }
    }
    
    public void validate(Shader shader, StringBuilder results) {
        for (ShaderSource source : shader.getSources()){
            results.append("Checking: ").append(source.getName());
            switch (source.getType()){
                case Fragment:
                    executeCg(source.getSource(), source.getLanguage(), source.getDefines(), "arbfp1", results);
                    break;
                case Vertex:
                    executeCg(source.getSource(), source.getLanguage(), source.getDefines(), "arbvp1", results);
                    break;
            }
        }
    }
    
}
