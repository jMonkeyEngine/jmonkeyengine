package com.jme3.scene.plugins.blender.file;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure.DataType;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a single field in the structure. It can be either a primitive type or a table or a reference to
 * another structure.
 * @author Marcin Roguski
 */
/*package*/
class Field implements Cloneable {

    private static final int NAME_LENGTH = 24;
    private static final int TYPE_LENGTH = 16;
    /** The blender context. */
    public BlenderContext blenderContext;
    /** The type of the field. */
    public String type;
    /** The name of the field. */
    public String name;
    /** The value of the field. Filled during data reading. */
    public Object value;
    /** This variable indicates the level of the pointer. */
    public int pointerLevel;
    /**
     * This variable determines the sizes of the array. If the value is null the n the field is not an array.
     */
    public int[] tableSizes;
    /** This variable indicates if the field is a function pointer. */
    public boolean function;

    /**
     * Constructor. Saves the field data and parses its name.
     * @param name
     *        the name of the field
     * @param type
     *        the type of the field
     * @param blenderContext
     *        the blender context
     * @throws BlenderFileException
     *         this exception is thrown if the names contain errors
     */
    public Field(String name, String type, BlenderContext blenderContext) throws BlenderFileException {
        this.type = type;
        this.blenderContext = blenderContext;
        this.parseField(new StringBuilder(name));
    }

    /**
     * Copy constructor. Used in clone method. Copying is not full. The value in the new object is not set so that we
     * have a clead empty copy of the filed to fill with data.
     * @param field
     *        the object that we copy
     */
    private Field(Field field) {
        type = field.type;
        name = field.name;
        blenderContext = field.blenderContext;
        pointerLevel = field.pointerLevel;
        if (field.tableSizes != null) {
            tableSizes = field.tableSizes.clone();
        }
        function = field.function;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new Field(this);
    }

    /**
     * This method fills the field wth data read from the input stream.
     * @param blenderInputStream
     *        the stream we read data from
     * @throws BlenderFileException
     *         an exception is thrown when the blend file is somehow invalid or corrupted
     */
    public void fill(BlenderInputStream blenderInputStream) throws BlenderFileException {
        int dataToRead = 1;
        if (tableSizes != null && tableSizes.length > 0) {
            for (int size : tableSizes) {
                if (size <= 0) {
                    throw new BlenderFileException("The field " + name + " has invalid table size: " + size);
                }
                dataToRead *= size;
            }
        }
        DataType dataType = pointerLevel == 0 ? DataType.getDataType(type, blenderContext) : DataType.POINTER;
        switch (dataType) {
            case POINTER:
                if (dataToRead == 1) {
                    Pointer pointer = new Pointer(pointerLevel, function, blenderContext);
                    pointer.fill(blenderInputStream);
                    value = pointer;
                } else {
                    Pointer[] data = new Pointer[dataToRead];
                    for (int i = 0; i < dataToRead; ++i) {
                        Pointer pointer = new Pointer(pointerLevel, function, blenderContext);
                        pointer.fill(blenderInputStream);
                        data[i] = pointer;
                    }
                    value = new DynamicArray<Pointer>(tableSizes, data);
                }
                break;
            case CHARACTER:
                //character is also stored as a number, because sometimes the new blender version uses
                //other number type instead of character as a field type
                //and characters are very often used as byte number stores instead of real chars
                if (dataToRead == 1) {
                    value = Byte.valueOf((byte) blenderInputStream.readByte());
                } else {
                    Character[] data = new Character[dataToRead];
                    for (int i = 0; i < dataToRead; ++i) {
                        data[i] = Character.valueOf((char) blenderInputStream.readByte());
                    }
                    value = new DynamicArray<Character>(tableSizes, data);
                }
                break;
            case SHORT:
                if (dataToRead == 1) {
                    value = Integer.valueOf(blenderInputStream.readShort());
                } else {
                    Number[] data = new Number[dataToRead];
                    for (int i = 0; i < dataToRead; ++i) {
                        data[i] = Integer.valueOf(blenderInputStream.readShort());
                    }
                    value = new DynamicArray<Number>(tableSizes, data);
                }
                break;
            case INTEGER:
                if (dataToRead == 1) {
                    value = Integer.valueOf(blenderInputStream.readInt());
                } else {
                    Number[] data = new Number[dataToRead];
                    for (int i = 0; i < dataToRead; ++i) {
                        data[i] = Integer.valueOf(blenderInputStream.readInt());
                    }
                    value = new DynamicArray<Number>(tableSizes, data);
                }
                break;
            case LONG:
                if (dataToRead == 1) {
                    value = Long.valueOf(blenderInputStream.readLong());
                } else {
                    Number[] data = new Number[dataToRead];
                    for (int i = 0; i < dataToRead; ++i) {
                        data[i] = Long.valueOf(blenderInputStream.readLong());
                    }
                    value = new DynamicArray<Number>(tableSizes, data);
                }
                break;
            case FLOAT:
                if (dataToRead == 1) {
                    value = Float.valueOf(blenderInputStream.readFloat());
                } else {
                    Number[] data = new Number[dataToRead];
                    for (int i = 0; i < dataToRead; ++i) {
                        data[i] = Float.valueOf(blenderInputStream.readFloat());
                    }
                    value = new DynamicArray<Number>(tableSizes, data);
                }
                break;
            case DOUBLE:
                if (dataToRead == 1) {
                    value = Double.valueOf(blenderInputStream.readDouble());
                } else {
                    Number[] data = new Number[dataToRead];
                    for (int i = 0; i < dataToRead; ++i) {
                        data[i] = Double.valueOf(blenderInputStream.readDouble());
                    }
                    value = new DynamicArray<Number>(tableSizes, data);
                }
                break;
            case VOID:
                break;
            case STRUCTURE:
                if (dataToRead == 1) {
                    Structure structure = blenderContext.getDnaBlockData().getStructure(type);
                    structure.fill(blenderInputStream);
                    value = structure;
                } else {
                    Structure[] data = new Structure[dataToRead];
                    for (int i = 0; i < dataToRead; ++i) {
                        Structure structure = blenderContext.getDnaBlockData().getStructure(type);
                        structure.fill(blenderInputStream);
                        data[i] = structure;
                    }
                    value = new DynamicArray<Structure>(tableSizes, data);
                }
                break;
            default:
                throw new IllegalStateException("Unimplemented filling of type: " + type);
        }
    }

    /**
     * This method parses the field name to determine how the field should be used.
     * @param nameBuilder
     *        the name of the field (given as StringBuilder)
     * @throws BlenderFileException
     *         this exception is thrown if the names contain errors
     */
    private void parseField(StringBuilder nameBuilder) throws BlenderFileException {
        this.removeWhitespaces(nameBuilder);
        //veryfying if the name is a pointer
        int pointerIndex = nameBuilder.indexOf("*");
        while (pointerIndex >= 0) {
            ++pointerLevel;
            nameBuilder.deleteCharAt(pointerIndex);
            pointerIndex = nameBuilder.indexOf("*");
        }
        //veryfying if the name is a function pointer
        if (nameBuilder.indexOf("(") >= 0) {
            function = true;
            this.removeCharacter(nameBuilder, '(');
            this.removeCharacter(nameBuilder, ')');
        } else {
            //veryfying if the name is a table
            int tableStartIndex = 0;
            List<Integer> lengths = new ArrayList<Integer>(3);//3 dimensions will be enough in most cases
            do {
                tableStartIndex = nameBuilder.indexOf("[");
                if (tableStartIndex > 0) {
                    int tableStopIndex = nameBuilder.indexOf("]");
                    if (tableStopIndex < 0) {
                        throw new BlenderFileException("Invalid structure name: " + name);
                    }
                    try {
                        lengths.add(Integer.valueOf(nameBuilder.substring(tableStartIndex + 1, tableStopIndex)));
                    } catch (NumberFormatException e) {
                        throw new BlenderFileException("Invalid structure name caused by invalid table length: " + name, e);
                    }
                    nameBuilder.delete(tableStartIndex, tableStopIndex + 1);
                }
            } while (tableStartIndex > 0);
            if (!lengths.isEmpty()) {
                tableSizes = new int[lengths.size()];
                for (int i = 0; i < tableSizes.length; ++i) {
                    tableSizes[i] = lengths.get(i).intValue();
                }
            }
        }
        name = nameBuilder.toString();
    }

    /**
     * This method removes the required character from the text.
     * @param text
     *        the text we remove characters from
     * @param toRemove
     *        the character to be removed
     */
    private void removeCharacter(StringBuilder text, char toRemove) {
        for (int i = 0; i < text.length(); ++i) {
            if (text.charAt(i) == toRemove) {
                text.deleteCharAt(i);
                --i;
            }
        }
    }

    /**
     * This method removes all whitespaces from the text.
     * @param text
     *        the text we remove whitespaces from
     */
    private void removeWhitespaces(StringBuilder text) {
        for (int i = 0; i < text.length(); ++i) {
            if (Character.isWhitespace(text.charAt(i))) {
                text.deleteCharAt(i);
                --i;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (function) {
            result.append('(');
        }
        for (int i = 0; i < pointerLevel; ++i) {
            result.append('*');
        }
        result.append(name);
        if (tableSizes != null) {
            for (int i = 0; i < tableSizes.length; ++i) {
                result.append('[').append(tableSizes[i]).append(']');
            }
        }
        if (function) {
            result.append(")()");
        }
        //insert appropriate amount of spaces to format the output corrently
        int nameLength = result.length();
        result.append(' ');//at least one space is a must
        for (int i = 1; i < NAME_LENGTH - nameLength; ++i) {//we start from i=1 because one space is already added
            result.append(' ');
        }
        result.append(type);
        nameLength = result.length();
        for (int i = 0; i < NAME_LENGTH + TYPE_LENGTH - nameLength; ++i) {
            result.append(' ');
        }
        if (value instanceof Character) {
            result.append(" = ").append((int) ((Character) value).charValue());
        } else {
            result.append(" = ").append(value != null ? value.toString() : "null");
        }
        return result.toString();
    }
}