package jme3tools.autogen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import org.lwjgl.opengl.ARBDepthBufferFloat;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBES3Compatibility;
import org.lwjgl.opengl.ARBHalfFloatPixel;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ARBMultisample;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.ARBTextureMultisample;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.EXTFramebufferBlit;
import org.lwjgl.opengl.EXTFramebufferMultisample;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTFramebufferSRGB;
import org.lwjgl.opengl.EXTPackedDepthStencil;
import org.lwjgl.opengl.EXTPackedFloat;
import org.lwjgl.opengl.EXTTextureArray;
import org.lwjgl.opengl.EXTTextureCompressionLATC;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.EXTTextureSRGB;
import org.lwjgl.opengl.EXTTextureSharedExponent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class GLAutoGen {

    private static class ConstantInfo {

        Class<?> declaringClazz;
        String constantName;
        Class<?> constantType;
        Object constantValue;

        public ConstantInfo(Class<?> declaringClazz, String constantName,
                Class<?> constantType, Object constantValue) {
            this.declaringClazz = declaringClazz;
            this.constantName = constantName;
            this.constantType = constantType;
            this.constantValue = constantValue;
        }

        @Override
        public String toString() {
            return "ConstantInfo{"
                    + "declaringClazz=" + declaringClazz
                    + ", constantName=" + constantName
                    + ", constantType=" + constantType
                    + ", constantValue=" + constantValue
                    + '}';
        }
    }

    private static class MethodInfo {

        Class<?> declaringClazz;
        String methodName;
        Class<?> returnType;
        Class<?>[] paramTypes;

        public MethodInfo(Class<?> declaringClazz, String methodName, Class<?> returnType, Class<?>[] paramTypes) {
            this.declaringClazz = declaringClazz;
            this.methodName = methodName;
            this.returnType = returnType;
            this.paramTypes = paramTypes;
        }

        @Override
        public String toString() {
            return "MethodInfo{" + "declaringClazz=" + declaringClazz + ", methodName=" + methodName + ", returnType=" + returnType + ", paramTypes=" + paramTypes + '}';
        }
    }

    private static final HashMap<String, ConstantInfo> constantMap
            = new HashMap<String, ConstantInfo>();

    private static final HashMap<String, List<MethodInfo>> methodMap
            = new HashMap<String, List<MethodInfo>>();

    private static final TreeSet<String> usedConstants = new TreeSet<String>();
    private static final TreeSet<String> usedMethods = new TreeSet<String>();

    private static void scanConstantsFromType(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if ((field.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) != 0) {
                String name = field.getName();
                Class<?> type = field.getType();
                Object value = null;

                if (constantMap.containsKey(name)) {
                    // throw new UnsupportedOperationException(name + " constant redeclared");
                    continue;
                }

                if (type == int.class) {
                    try {
                        value = field.getInt(null);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if (type == long.class) {
                    try {
                        value = field.getLong(null);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                if (value == null) {
                    throw new UnsupportedOperationException("Unsupported type: " + type);
                }
                constantMap.put(name, new ConstantInfo(clazz, name, type, value));
            }
        }
    }

    private static void scanMethodsFromType(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if ((method.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) != 0) {
                String name = method.getName();
                Class<?> type = method.getReturnType();
                Class<?>[] paramTypes = method.getParameterTypes();

                List<MethodInfo> overloads = methodMap.get(name);

                if (overloads == null) {
                    overloads = new ArrayList<MethodInfo>();
                    methodMap.put(name, overloads);
                }

                MethodInfo info = new MethodInfo(clazz, name, type, paramTypes);
                overloads.add(info);
            }
        }
    }

    private static void scanGLType(Class<?> clazz) {
        scanConstantsFromType(clazz);
        scanMethodsFromType(clazz);
    }

    private static String scanConstants(String line, 
            Collection<String> consts) {
        String modifiedLine = line;
        int next_gl = modifiedLine.indexOf("GL_");
        while (next_gl > 0) {
            char chrBefore = modifiedLine.charAt(next_gl - 1);
            if (!Character.isWhitespace(chrBefore)
                    && chrBefore != '.'
                    && chrBefore != '!'
                    && chrBefore != '('
                    && chrBefore != ',') {
            //    System.out.println(modifiedLine + "\t\t\t\tPreceding character \"" + chrBefore + "\" not acceptable.");
            } else {
                boolean isCap = false;
                for (int scan_idx = next_gl + 3; scan_idx < modifiedLine.length(); scan_idx++) {
                    char chrCall = modifiedLine.charAt(scan_idx);
                    if (Character.isLowerCase(chrCall)) {
                        isCap = true;
                    } else if (!Character.isLetterOrDigit(chrCall) && chrCall != '_') {
                        if (!isCap) {
                            String constant = modifiedLine.substring(next_gl, scan_idx);
                        
                            consts.add(constant);

                            ConstantInfo info = (ConstantInfo) constantMap.get(constant);
                            String className = "GL";

                            if (!info.declaringClazz.getSimpleName().startsWith("GL")) {
                                className = "EX";
                            }

                            // Also perform in-line injection.
                            modifiedLine = modifiedLine.substring(0, next_gl)
                                    + className + "."
                                    + modifiedLine.substring(next_gl);
                        }
                        break;
                    }
                }
            }
            next_gl = modifiedLine.indexOf("GL_", next_gl + 5);
        }
        return modifiedLine;
    }

    private static String scanMethods(String line, Collection<String> methods) {
        String modifiedLine = line;
        int next_gl = line.indexOf("gl");
        while (next_gl > 0) {
            char chrBefore = line.charAt(next_gl - 1);
            if (!Character.isWhitespace(chrBefore)
                    && chrBefore != '.'
                    && chrBefore != '!'
                    && chrBefore != '(') {
                // System.out.println(line + "\t\t\t\tPreceding character not acceptable.");
            } else {
                for (int scan_idx = next_gl + 2; scan_idx < line.length(); scan_idx++) {
                    char chrCall = line.charAt(scan_idx);
                    if (chrCall == '(') {
                        String methodName = line.substring(next_gl, scan_idx);
                        methods.add(methodName);
                        
                        MethodInfo info = (MethodInfo) methodMap.get(methodName).get(0);
                        String memberName = "gl";
                        
                        if (!info.declaringClazz.getSimpleName().startsWith("GL")) {
                            memberName = "glext";
                        }
                        
                        // Also perform in-line injection.
                        modifiedLine = modifiedLine.substring(0, next_gl) +
                                       memberName + "." +
                                       modifiedLine.substring(next_gl);
                        
                        break;
                    } else if (!Character.isLetterOrDigit(chrCall)) {
                        // System.out.println(line.substring(next_gl) + "\t\t\t\tFound non-letter inside call");
                        break;
                    }
                }
            }
            next_gl = line.indexOf("gl", next_gl + 2);
        }
        return modifiedLine;
    }

    private static String scanFile(String path) {
        StringBuilder sb = new StringBuilder();
        FileReader reader = null;
        List<String> methods = new ArrayList<String>();
        List<String> consts = new ArrayList<String>();
        try {
            reader = new FileReader(path);
            BufferedReader br = new BufferedReader(reader);
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                line = scanMethods(line, methods);
                line = scanConstants(line, consts);
                sb.append(line).append("\n");
            }
            
            usedMethods.addAll(methods);
            usedConstants.addAll(consts);
            return sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                }
            }
        }
    }
    
    private static String exportGL(boolean forExt) {
        StringBuilder sb = new StringBuilder();
        sb.append("package jme3tools.autogen;\n");
        sb.append("\n");
        sb.append("import java.nio.ByteBuffer;\n");
        sb.append("import java.nio.DoubleBuffer;\n");
        sb.append("import java.nio.FloatBuffer;\n");
        sb.append("import java.nio.IntBuffer;\n");
        sb.append("import java.nio.ShortBuffer;\n");
        sb.append("\n");
        sb.append("/**\n");
        sb.append(" * Auto-generated interface\n");
        sb.append(" */\n");
        sb.append("public interface ").append(forExt ? "GLExt" : "GL").append(" {\n");
        sb.append("\n");
        sb.append("// -- begin constants\n");
        for (String constant : usedConstants) {
            ConstantInfo info = constantMap.get(constant);
            
            if (info == null) {
                throw new IllegalStateException("Cannot find required constant: " + constant);
            }
            
            boolean isExtConst = !info.declaringClazz.getSimpleName().startsWith("GL");
            if (forExt != isExtConst) {
                continue;
            }

            String typeStr = info.constantType.toString();
            String valueStr = null;

            if (info.constantType == int.class) {
                valueStr = "0x" + Integer.toHexString((Integer) info.constantValue).toUpperCase();
            } else if (info.constantType == long.class) {
                valueStr = "0x" + Long.toHexString((Long) info.constantValue).toUpperCase();
            }

            sb.append("\tpublic static final ").append(typeStr)
              .append(" ").append(info.constantName)
              .append(" = ").append(valueStr).append(";\n");
        }
        sb.append("// -- end constants\n");
        sb.append("\n");
        sb.append("// -- begin methods\n");

        for (String method : usedMethods) {
            List<MethodInfo> infos = methodMap.get(method);
            if (infos == null) {
                throw new IllegalStateException("Cannot find required method: " + method);
            }

            for (MethodInfo info : infos) {
                boolean isExtConst = !info.declaringClazz.getSimpleName().startsWith("GL");
                if (forExt != isExtConst) {
                    continue;
                }
                
                String retTypeStr = info.returnType.getSimpleName();
                sb.append("\tpublic ").append(retTypeStr).append(" ").append(method).append("(");
                for (int i = 0; i < info.paramTypes.length; i++) {
                    sb.append(info.paramTypes[i].getSimpleName()).append(" param").append(i + 1);
                    if (i != info.paramTypes.length - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(");\n");
            }
        }

        sb.append("// -- end methods\n");
        sb.append("\n");
        sb.append("}\n");
        return sb.toString();
    }
    
    private static void writeFile(String path, String content) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(path);
            writer.write(content);
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                }
            }
        }
    }
    
    public static void main(String[] args) throws IOException {
        String rendererPath = "../jme3-lwjgl/src/main/java/com/jme3/renderer/lwjgl/LwjglRenderer.java";
        String textureUtilPath = "../jme3-lwjgl/src/main/java/com/jme3/renderer/lwjgl/TextureUtil.java";
        File rendererSrc = new File(rendererPath).getAbsoluteFile();
        File textureUtilSrc = new File(textureUtilPath).getAbsoluteFile();
       
        scanGLType(GL11.class);
        scanGLType(GL14.class);
        scanGLType(GL12.class);
        scanGLType(GL13.class);
        scanGLType(GL15.class);
        scanGLType(GL20.class);
        scanGLType(GL30.class);
        
        // Renderer extensions
        scanGLType(ARBDrawInstanced.class);
        scanGLType(ARBInstancedArrays.class);
        scanGLType(ARBMultisample.class);
        scanGLType(ARBTextureMultisample.class);
        scanGLType(ARBVertexArrayObject.class);
        scanGLType(EXTFramebufferBlit.class);
        scanGLType(EXTFramebufferMultisample.class);
        scanGLType(EXTFramebufferObject.class);
        scanGLType(EXTFramebufferSRGB.class);
        scanGLType(EXTTextureArray.class);
        scanGLType(EXTTextureFilterAnisotropic.class);
        
        // Texture extensions
        scanGLType(ARBDepthBufferFloat.class);
        scanGLType(ARBES3Compatibility.class);
        scanGLType(ARBHalfFloatPixel.class);
        scanGLType(ARBTextureFloat.class);
        scanGLType(ARBTextureMultisample.class);
        scanGLType(EXTPackedDepthStencil.class);
        scanGLType(EXTPackedFloat.class);
        scanGLType(EXTTextureArray.class);
        scanGLType(EXTTextureCompressionLATC.class);
        scanGLType(EXTTextureCompressionS3TC.class);
        scanGLType(EXTTextureSRGB.class);
        scanGLType(EXTTextureSharedExponent.class);
        
        String processedRenderer = scanFile(rendererSrc.toString());
        String processedTextureUtil = scanFile(textureUtilSrc.toString());
        String gl = exportGL(false);
        String glext = exportGL(true);
        
        //writeFile("src/main/java/jme3tools/autogen/GLRenderer.java", processedRenderer);
        //writeFile("src/main/java/jme3tools/autogen/TextureUtil.java", processedTextureUtil);
        //writeFile("src/main/java/jme3tools/autogen/GL.java", gl);
        //writeFile("src/main/java/jme3tools/autogen/GLExt.java", glext);
    }
}
