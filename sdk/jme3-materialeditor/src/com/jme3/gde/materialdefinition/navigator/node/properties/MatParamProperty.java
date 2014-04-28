/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.navigator.node.properties;

import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.properties.ColorRGBAPropertyEditor;
import com.jme3.gde.core.properties.Matrix3fPropertyEditor;
import com.jme3.gde.core.properties.QuaternionPropertyEditor;
import com.jme3.gde.core.properties.TexturePropertyEditor;
import com.jme3.gde.core.properties.Vector2fPropertyEditor;
import com.jme3.gde.core.properties.Vector3fPropertyEditor;
import com.jme3.gde.materialdefinition.MatDefDataObject;
import com.jme3.gde.materialdefinition.fileStructure.leaves.MatParamBlock;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.nodes.Node;
import org.openide.util.Lookup;

/**
 *
 * @author Nehon
 */
public class MatParamProperty<T> extends Node.Property<T> {

    private Lookup lookup;
    private String type;
    private Class<T> valueType;   

    public MatParamProperty(String name, String type, Class<T> valueType, Lookup lookup) {
        super(valueType);
        this.valueType = valueType;
        setName(name);
        this.type = type;
        this.lookup = lookup;
        setDisplayName(name);
    }

    @Override
    public boolean canRead() {
        return true;
    }
    
    

    @Override
    public T getValue() throws IllegalAccessException, InvocationTargetException {
        MatParam param = lookup.lookup(Material.class).getParam(getName());
        if (param != null) {
            return (T) param.getValue();
        }
        return null;
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public void setValue(T val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object v = val;
        if (valueType == Vector3f.class) {
            float[] f = getFloatArrayValue((String) val, 3);
            v = new Vector3f(f[0], f[1], f[2]);
        } else if (valueType == Quaternion.class) {
            float[] f = getFloatArrayValue((String) val, 4);
            v = new Quaternion(f[0], f[1], f[2], f[3]);
        } else if (valueType == Vector4f.class) {
            float[] f = getFloatArrayValue((String) val, 4);
            v = new Vector4f(f[0], f[1], f[2], f[3]);
        } else if (valueType == Vector2f.class) {
            float[] f = getFloatArrayValue((String) val, 2);
            v = new Vector2f(f[0], f[1]);
        }
        VarType vType = MatParamProperty.getVarType(type);
        
        Material m = lookup.lookup(Material.class);
        m.setParam(getName(), vType, v);
        MatDefDataObject obj = lookup.lookup(MatDefDataObject.class);
        obj.getLookupContents().remove(m);
        obj.getLookupContents().add(m);
        
    }
    
    

    public float[] getFloatArrayValue(String value, int capacity) {
        float[] ret = new float[capacity];
        String[] vals = extractValues(value);
        try {
            for (int i = 0; i < vals.length; i++) {
                ret[i] = Float.parseFloat(vals[i]);
            }
        } catch (NumberFormatException e) {
            Logger.getLogger(VectorTextField.class.getName()).log(Level.WARNING, "Invalid format");
        }
        return ret;
    }

    private String[] extractValues(String value) {
        String text = value.replaceAll("[\\[\\]]", "");
        String[] values = text.split(",");
        return values;
    }

    public String getStringFromFloatArray(float[] vals) {
        String t = "[";
        for (float f : vals) {
            t += f + ",";
        }
        t = t.substring(0, t.length() - 1) + "]";
        return t;
    }

    @Override
    public PropertyEditor getPropertyEditor() {
        if (valueType == Vector3f.class) {
            return new Vector3fPropertyEditor();
        } else if (valueType == Quaternion.class) {
            return new QuaternionPropertyEditor();
        } else if (valueType == Matrix3f.class) {
            return new Matrix3fPropertyEditor();
        } else if (valueType == ColorRGBA.class) {
            return new ColorRGBAPropertyEditor();
        } else if (valueType == Vector2f.class) {
            return new Vector2fPropertyEditor();
        } else if (valueType == Texture.class) {
            return new TexturePropertyEditor(lookup.lookup(ProjectAssetManager.class));
        }


        return super.getPropertyEditor();
    }

    public static MatParamProperty<?> makeProperty(MatParamBlock param, Lookup lookup) {

        VarType vType = MatParamProperty.getVarType(param.getType());
        switch (vType) {
            case Boolean:
                return new MatParamProperty<Boolean>(param.getName(), param.getType(), Boolean.class, lookup);
            case Float:
                return new MatParamProperty<Float>(param.getName(), param.getType(), Float.class, lookup);
            case FloatArray:
                return new MatParamProperty<Object>(param.getName(), param.getType(), Object.class, lookup);
            case Int:
                return new MatParamProperty<Integer>(param.getName(), param.getType(), Integer.class, lookup);
            case Matrix3:
                return new MatParamProperty<Matrix3f>(param.getName(), param.getType(), Matrix3f.class, lookup);
            case Matrix3Array:
                return new MatParamProperty<Object>(param.getName(), param.getType(), Object.class, lookup);
            case Matrix4:
                return new MatParamProperty<Matrix4f>(param.getName(), param.getType(), Matrix4f.class, lookup);
            case Matrix4Array:
                return new MatParamProperty<Object>(param.getName(), param.getType(), Object.class, lookup);
            case Texture2D:
            case Texture3D:
            case TextureArray:
            case TextureBuffer:
            case TextureCubeMap:
                return new MatParamProperty<Texture>(param.getName(), param.getType(), Texture.class, lookup);
            case Vector2:
                return new MatParamProperty<Vector2f>(param.getName(), param.getType(), Vector2f.class, lookup);
            case Vector2Array:
                return new MatParamProperty<Object>(param.getName(), param.getType(), Object.class, lookup);
            case Vector3:
                return new MatParamProperty<Vector3f>(param.getName(), param.getType(), Vector3f.class, lookup);
            case Vector3Array:
                return new MatParamProperty<Object>(param.getName(), param.getType(), Object.class, lookup);
            case Vector4:
                if (param.getType().equals("Color")) {
                    return new MatParamProperty<ColorRGBA>(param.getName(), param.getType(), ColorRGBA.class, lookup);
                }
                return new MatParamProperty<Vector4f>(param.getName(), param.getType(), Vector4f.class, lookup);
            case Vector4Array:
                return new MatParamProperty<Object>(param.getName(), param.getType(), Object.class, lookup);
            default:
                return null;
        }
    }

    private static VarType getVarType(String type) {
        VarType vType = null;
        if (type.equals("Color")) {
            vType = VarType.Vector4;
        } else {
            vType = VarType.valueOf(type);
        }
        return vType;
    }

    
}
