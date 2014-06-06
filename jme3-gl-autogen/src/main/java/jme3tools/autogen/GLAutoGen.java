package jme3tools.autogen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ARBMultisample;
import org.lwjgl.opengl.ARBTextureMultisample;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTFramebufferBlit;
import org.lwjgl.opengl.EXTFramebufferMultisample;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTTextureArray;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.NVHalfFloat;

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

    private static final HashSet<String> capsSet
            = new HashSet<String>();
    
    private static final TreeSet<String> usedConstants = new TreeSet<String>();
    private static final TreeSet<String> usedMethods = new TreeSet<String>();
    private static final TreeSet<String> usedCaps = new TreeSet<String>();

    private static void scanCapsFromType(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if ((field.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) != 0) {
                String name = field.getName();
                Class<?> type = field.getType();
                if (type == boolean.class) {
                    capsSet.add(name);
                }
            }
        }
    }
    
    private static void scanConstantsFromType(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if ((field.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) != 0) {
                String name = field.getName();
                Class<?> type = field.getType();
                Object value = null;

                if (constantMap.containsKey(name)) {
                    throw new UnsupportedOperationException(name + " constant redeclared");
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
            Collection<String> consts, 
            Collection<String> caps) {
        String modifiedLine = line;
        int next_gl = line.indexOf("GL_");
        while (next_gl > 0) {
            char chrBefore = line.charAt(next_gl - 1);
            if (!Character.isWhitespace(chrBefore)
                    && chrBefore != '.'
                    && chrBefore != '!'
                    && chrBefore != '(') {
                // System.out.println(line + "\t\t\t\tPreceding character \"" + chrBefore + "\" not acceptable.");
            } else {
                boolean isCap = false;
                for (int scan_idx = next_gl + 3; scan_idx < line.length(); scan_idx++) {
                    char chrCall = line.charAt(scan_idx);
                    if (Character.isLowerCase(chrCall)) {
                        // GL constants cannot have lowercase letters.
                        // This is most likely capability type.
                        isCap = true;
                    } else if (!Character.isLetterOrDigit(chrCall) && chrCall != '_') {
                        if (isCap) {
                            caps.add(line.substring(next_gl, scan_idx));
                        } else {

                            // Also perform in-line injection.
                            modifiedLine = modifiedLine.substring(0, next_gl)
                                    + "GL."
                                    + modifiedLine.substring(next_gl);

                            consts.add(line.substring(next_gl, scan_idx));
                        }
                        break;
                    }
                }
            }
            next_gl = line.indexOf("GL_", next_gl + 3);
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
                        
                        // Also perform in-line injection.
                        modifiedLine = modifiedLine.substring(0, next_gl) +
                                       "gl." +
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

    private static void scanFile(String path) {
        FileReader reader = null;
        List<String> methods = new ArrayList<String>();
        List<String> consts = new ArrayList<String>();
        List<String> caps = new ArrayList<String>();
        try {
            reader = new FileReader(path);
            BufferedReader br = new BufferedReader(reader);
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                line = scanMethods(line, methods);
                line = scanConstants(line, consts, caps);
                System.out.println(line);
            }
            
            usedMethods.addAll(methods);
            usedConstants.addAll(consts);
            usedCaps.addAll(caps);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    private static void exportInterface() {
        System.out.println("package autogen;");
        System.out.println();
        System.out.println("public final class GLCaps {");
        System.out.println();
        System.out.println("\tprivate GLCaps { }");
        System.out.println();
        
        for (String cap : capsSet) {
            if (usedCaps.contains(cap)) {
                System.out.println("\tpublic boolean " + cap + ";");
            }
        }
        
        System.out.println();
        System.out.println("}");
        System.out.println();
        System.out.println("/**");
        System.out.println(" * Auto-generated interface");
        System.out.println(" */");
        System.out.println("public interface GL {");
        System.out.println();
        System.out.println("// -- begin constants");
        for (String constant : usedConstants) {
            ConstantInfo info = constantMap.get(constant);
            if (info == null) {
                throw new IllegalStateException("Cannot find required constant: " + constant);
            }

            String typeStr = info.constantType.toString();
            String valueStr = null;

            if (info.constantType == int.class) {
                valueStr = "0x" + Integer.toHexString((Integer) info.constantValue).toUpperCase();
            } else if (info.constantType == long.class) {
                valueStr = "0x" + Long.toHexString((Long) info.constantValue).toUpperCase();
            }

            System.out.println("\tpublic static final " + typeStr + " " + info.constantName + " = " + valueStr + ";");
        }
        System.out.println("// -- end constants");
        System.out.println();
        System.out.println("// -- begin methods");

        for (String method : usedMethods) {
            List<MethodInfo> infos = methodMap.get(method);
            if (infos == null) {
                throw new IllegalStateException("Cannot find required method: " + method);
            }

            for (MethodInfo info : infos) {
                String retTypeStr = info.returnType.getSimpleName();
                System.out.print("\tpublic " + retTypeStr + " " + method + "(");
                for (int i = 0; i < info.paramTypes.length; i++) {
                    System.out.print(info.paramTypes[i].getSimpleName() + " param" + (i + 1));
                    if (i != info.paramTypes.length - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.println(");");
            }
        }

        System.out.println("// -- end methods");
        System.out.println("// -- begin custom methods");
        System.out.println("\tpublic GLCaps getGLCaps();");
        System.out.println("// -- end custom methods");
        System.out.println();
        System.out.println("}");
    }

    public static void main(String[] args) throws IOException {
        String path = "../jme3-lwjgl/src/main/java/com/jme3/renderer/lwjgl/LwjglRenderer.java";
        File lwjglRendererSrc = new File(path).getAbsoluteFile();
        
        scanGLType(GL11.class);
        scanGLType(GL14.class);
        scanGLType(GL12.class);
        scanGLType(GL13.class);
        scanGLType(GL15.class);
        scanGLType(GL20.class);
        scanGLType(GL21.class);
        scanGLType(GL30.class);
        scanGLType(NVHalfFloat.class);
        scanGLType(ARBGeometryShader4.class);
        scanGLType(EXTFramebufferObject.class);
        scanGLType(EXTFramebufferBlit.class);
        scanGLType(EXTFramebufferMultisample.class);
        scanGLType(ARBTextureMultisample.class);
        scanGLType(ARBMultisample.class);
        scanGLType(EXTTextureArray.class);
        scanGLType(EXTTextureFilterAnisotropic.class);
        scanGLType(ARBDrawInstanced.class);
        scanGLType(ARBInstancedArrays.class);
        scanCapsFromType(ContextCapabilities.class);
        
        scanFile(lwjglRendererSrc.toString());

        exportInterface();
    }
}
