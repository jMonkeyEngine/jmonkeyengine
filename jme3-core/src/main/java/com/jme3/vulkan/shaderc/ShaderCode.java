package com.jme3.vulkan.shaderc;

import com.jme3.util.struct.Struct;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderCode {

    private static final Pattern variable = Pattern.compile("(?sm)(layout\\s*\\(.*\\)\\s+)?(uniform|buffer|in|out)\\s+([a-zA-Z0-9_]+\\s+)?[a-zA-Z0-9_]+\\s*(\\{.*}\\s*[a-zA-Z0-9_]+\\s*)?;", Pattern.MULTILINE);
    private static final Pattern layout = Pattern.compile("(?sm)layout\\s*\\(.*\\)", Pattern.MULTILINE);

    private final Map<String, Variable> variables = new HashMap<>();

    public ShaderCode(String code) {
        for (Matcher m = variable.matcher(code); m.find();) {
            Variable v = new Variable(m.group());
            variables.putIfAbsent(v.getHandle(), v);
            code = code.substring(0, m.start()) + "$var[" + v.getHandle() + ']' + code.substring(m.end());
        }
    }

    private static class Variable {

        private final String type;
        private final String dataType;
        private final String handle;
        private final String body;
        private final Map<String, String> layout = new HashMap<>();

        public Variable(String text) {
            text = text.substring(0, text.length() - 1).trim();
            Matcher layoutMatch = ShaderCode.layout.matcher(text);
            if (layoutMatch.find()) {
                text = text.substring(layoutMatch.end() + 1).trim();
                String layoutContent = layoutMatch.group();
                String[] args = layoutContent.substring(layoutContent.indexOf('(') + 1, layoutContent.length() - 1).split(",");
                for (String a : args) {
                    String[] parts = a.split("=", 2);
                    layout.put(parts[0].trim(), parts.length >= 2 ? parts[1].trim() : null);
                }
                text = text.substring(layoutMatch.end()).trim();
            }
            String[] args;
            int bodyStart = text.indexOf('{');
            if (bodyStart >= 0) {
                int bodyEnd = text.lastIndexOf('}');
                handle = text.substring(bodyEnd + 1).trim();
                body = text.substring(bodyStart + 1, bodyEnd);
                args = text.substring(0, bodyStart).trim().split("\\s+");
                dataType = args[1];
            } else {
                args = text.split("\\s+");
                dataType = args[1];
                handle = args[2];
                body = null;
            }
            type = args[0];
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder();
            if (!layout.isEmpty()) {
                out.append("layout(");
                boolean firstArg = true;
                for (Map.Entry<String, String> l : layout.entrySet()) {
                    if (!firstArg) {
                        out.append(',');
                    }
                    out.append(l.getKey());
                    if (l.getValue() != null) {
                        out.append('=').append(l.getValue());
                    }
                    firstArg = false;
                }
                out.append(')');
            }
            out.append(type).append(' ').append(dataType).append(' ');
            if (body != null) {
                out.append('{').append(body).append('}');
            }
            out.append(handle).append(';');
            return out.toString();
        }

        public void setLayoutArgument(String name) {
            layout.put(name, null);
        }

        public void setLayoutArgument(String name, String value) {
            layout.put(name, value);
        }

        public String getType() {
            return type;
        }

        public String getDataType() {
            return dataType;
        }

        public String getHandle() {
            return handle;
        }

        public Map<String, String> getLayout() {
            return layout;
        }

    }

}
