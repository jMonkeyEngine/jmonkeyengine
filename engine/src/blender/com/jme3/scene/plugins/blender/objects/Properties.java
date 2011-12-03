package com.jme3.scene.plugins.blender.objects;

import com.jme3.export.*;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.BlenderInputStream;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The blender object's custom properties.
 * This class is valid for all versions of blender.
 * @author Marcin Roguski (Kaelthas)
 */
public class Properties implements Cloneable, Savable {
	private static final Logger		LOGGER				= Logger.getLogger(Properties.class.getName());

	// property type
	public static final int			IDP_STRING			= 0;
	public static final int			IDP_INT				= 1;
	public static final int			IDP_FLOAT			= 2;
	public static final int			IDP_ARRAY			= 5;
	public static final int			IDP_GROUP			= 6;
	// public static final int IDP_ID = 7;//this is not implemented in blender (yet)
	public static final int			IDP_DOUBLE			= 8;
	// the following are valid for blender 2.5x+
	public static final int			IDP_IDPARRAY		= 9;
	public static final int			IDP_NUMTYPES		= 10;

	protected static final String	RNA_PROPERTY_NAME	= "_RNA_UI";
	/** Default name of the property (used if the name is not specified in blender file). */
	protected static final String	DEFAULT_NAME		= "Unnamed property";

	/** The name of the property. */
	private String					name;
	/** The type of the property. */
	private int						type;
	/** The subtype of the property. Defines the type of array's elements. */
	private int						subType;
	/** The value of the property. */
	private Object					value;
	/** The description of the property. */
	private String					description;

	/**
	 * This method loads the property from the belnder file.
	 * @param idPropertyStructure
	 *        the ID structure constining the property
	 * @param blenderContext
	 *        the blender context
	 * @throws BlenderFileException
	 *         an exception is thrown when the belnder file is somehow invalid
	 */
	public void load(Structure idPropertyStructure, BlenderContext blenderContext) throws BlenderFileException {
		name = idPropertyStructure.getFieldValue("name").toString();
		if (name == null || name.length() == 0) {
			name = DEFAULT_NAME;
		}
		subType = ((Number) idPropertyStructure.getFieldValue("subtype")).intValue();
		type = ((Number) idPropertyStructure.getFieldValue("type")).intValue();

		// reading the data
		Structure data = (Structure) idPropertyStructure.getFieldValue("data");
		int len = ((Number) idPropertyStructure.getFieldValue("len")).intValue();
		switch (type) {
			case IDP_STRING: {
				Pointer pointer = (Pointer) data.getFieldValue("pointer");
				BlenderInputStream bis = blenderContext.getInputStream();
				FileBlockHeader dataFileBlock = blenderContext.getFileBlock(pointer.getOldMemoryAddress());
				bis.setPosition(dataFileBlock.getBlockPosition());
				value = bis.readString();
				break;
			}
			case IDP_INT:
				int intValue = ((Number) data.getFieldValue("val")).intValue();
				value = Integer.valueOf(intValue);
				break;
			case IDP_FLOAT:
				int floatValue = ((Number) data.getFieldValue("val")).intValue();
				value = Float.valueOf(Float.intBitsToFloat(floatValue));
				break;
			case IDP_ARRAY: {
				Pointer pointer = (Pointer) data.getFieldValue("pointer");
				BlenderInputStream bis = blenderContext.getInputStream();
				FileBlockHeader dataFileBlock = blenderContext.getFileBlock(pointer.getOldMemoryAddress());
				bis.setPosition(dataFileBlock.getBlockPosition());
				int elementAmount = dataFileBlock.getSize();
				switch (subType) {
					case IDP_INT:
						elementAmount /= 4;
						int[] intList = new int[elementAmount];
						for (int i = 0; i < elementAmount; ++i) {
							intList[i] = bis.readInt();
						}
						value = intList;
						break;
					case IDP_FLOAT:
						elementAmount /= 4;
						float[] floatList = new float[elementAmount];
						for (int i = 0; i < elementAmount; ++i) {
							floatList[i] = bis.readFloat();
						}
						value = floatList;
						break;
					case IDP_DOUBLE:
						elementAmount /= 8;
						double[] doubleList = new double[elementAmount];
						for (int i = 0; i < elementAmount; ++i) {
							doubleList[i] = bis.readDouble();
						}
						value = doubleList;
						break;
					default:
						throw new IllegalStateException("Invalid array subtype: " + subType);
				}
			}
			case IDP_GROUP:
				Structure group = (Structure) data.getFieldValue("group");
				List<Structure> dataList = group.evaluateListBase(blenderContext);
				List<Properties> subProperties = new ArrayList<Properties>(len);
				for (Structure d : dataList) {
					Properties properties = new Properties();
					properties.load(d, blenderContext);
					subProperties.add(properties);
				}
				value = subProperties;
				break;
			case IDP_DOUBLE:
				int doublePart1 = ((Number) data.getFieldValue("val")).intValue();
				int doublePart2 = ((Number) data.getFieldValue("val2")).intValue();
				long doubleVal = (long) doublePart2 << 32 | doublePart1;
				value = Double.valueOf(Double.longBitsToDouble(doubleVal));
				break;
			case IDP_IDPARRAY: {
				Pointer pointer = (Pointer) data.getFieldValue("pointer");
				List<Structure> arrays = pointer.fetchData(blenderContext.getInputStream());
				List<Object> result = new ArrayList<Object>(arrays.size());
				Properties temp = new Properties();
				for (Structure array : arrays) {
					temp.load(array, blenderContext);
					result.add(temp.value);
				}
				this.value = result;
				break;
			}
			case IDP_NUMTYPES:
				throw new UnsupportedOperationException();
				// case IDP_ID://not yet implemented in blender
				// return null;
			default:
				throw new IllegalStateException("Unknown custom property type: " + type);
		}
		this.completeLoading();
	}

	/**
	 * This method returns the name of the property.
	 * @return the name of the property
	 */
	public String getName() {
		return name;
	}

	/**
	 * This method returns the description of the property.
	 * @return the description of the property
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * This method returns the type of the property.
	 * @return the type of the property
	 */
	public int getType() {
		return type;
	}

	/**
	 * This method returns the value of the property.
	 * The type of the value depends on the type of the property.
	 * @return the value of the property
	 */
	public Object getValue() {
		return value;
	}
	
	/**
	 * This method returns the same as getValue if the current property is of
	 * other type than IDP_GROUP and its name matches 'propertyName' param. If
	 * this property is a group property the method tries to find subproperty
	 * value of the given name. The first found value is returnes os <b>use this
	 * method wisely</b>. If no property of a given name is foung - <b>null</b>
	 * is returned.
	 * 
	 * @param propertyName
	 *            the name of the property
	 * @return found property value or <b>null</b>
	 */
	@SuppressWarnings("unchecked")
	public Object findValue(String propertyName) {
		if (name.equals(propertyName)) {
			return value;
		} else {
			if (type == IDP_GROUP) {
				List<Properties> props = (List<Properties>) value;
				for (Properties p : props) {
					Object v = p.findValue(propertyName);
					if (v != null) {
						return v;
					}
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.append(sb, new StringBuilder());
		return sb.toString();
	}

	/**
	 * This method appends the data of the property to the given string buffer.
	 * @param sb
	 *        string buffer
	 * @param indent
	 *        indent buffer
	 */
	@SuppressWarnings("unchecked")
	private void append(StringBuilder sb, StringBuilder indent) {
		sb.append(indent).append("name: ").append(name).append("\n\r");
		sb.append(indent).append("type: ").append(type).append("\n\r");
		sb.append(indent).append("subType: ").append(subType).append("\n\r");
		sb.append(indent).append("description: ").append(description).append("\n\r");
		indent.append('\t');
		sb.append(indent).append("value: ");
		if (value instanceof Properties) {
			((Properties) value).append(sb, indent);
		} else if (value instanceof List) {
			for (Object v : (List<Object>) value) {
				if (v instanceof Properties) {
					sb.append(indent).append("{\n\r");
					indent.append('\t');
					((Properties) v).append(sb, indent);
					indent.deleteCharAt(indent.length() - 1);
					sb.append(indent).append("}\n\r");
				} else {
					sb.append(v);
				}
			}
		} else {
			sb.append(value);
		}
		sb.append("\n\r");
		indent.deleteCharAt(indent.length() - 1);
	}

	/**
	 * This method should be called after the properties loading.
	 * It loads the properties from the _RNA_UI property and removes this property from the
	 * result list.
	 */
	@SuppressWarnings("unchecked")
	protected void completeLoading() {
		if (this.type == IDP_GROUP) {
			List<Properties> groupProperties = (List<Properties>) this.value;
			Properties rnaUI = null;
			for (Properties properties : groupProperties) {
				if (properties.name.equals(RNA_PROPERTY_NAME) && properties.type == IDP_GROUP) {
					rnaUI = properties;
					break;
				}
			}
			if (rnaUI != null) {
				// removing the RNA from the result list
				groupProperties.remove(rnaUI);

				// loading the descriptions
				Map<String, String> descriptions = new HashMap<String, String>(groupProperties.size());
				List<Properties> propertiesRNA = (List<Properties>) rnaUI.value;
				for (Properties properties : propertiesRNA) {
					String name = properties.name;
					String description = null;
					List<Properties> rnaData = (List<Properties>) properties.value;
					for (Properties rna : rnaData) {
						if ("description".equalsIgnoreCase(rna.name)) {
							description = (String) rna.value;
							break;
						}
					}
					descriptions.put(name, description);
				}

				// applying the descriptions
				for (Properties properties : groupProperties) {
					properties.description = descriptions.get(properties.name);
				}
			}
		}
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void write(JmeExporter ex) throws IOException {
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(name, "name", DEFAULT_NAME);
		oc.write(type, "type", 0);
		oc.write(subType, "subtype", 0);
		oc.write(description, "description", null);
		switch (type) {
			case IDP_STRING:
				oc.write((String) value, "value", null);
				break;
			case IDP_INT:
				oc.write((Integer) value, "value", 0);
				break;
			case IDP_FLOAT:
				oc.write((Float) value, "value", 0);
				break;
			case IDP_ARRAY:
				switch (subType) {
					case IDP_INT:
						oc.write((int[]) value, "value", null);
						break;
					case IDP_FLOAT:
						oc.write((float[]) value, "value", null);
						break;
					case IDP_DOUBLE:
						oc.write((double[]) value, "value", null);
						break;
					default:
						LOGGER.warning("Cannot save the property's value! Invalid array subtype! Property: name: " + name + "; subtype: " + subType);
				}
			case IDP_GROUP:
				oc.writeSavableArrayList((ArrayList<Properties>) value, "value", null);
				break;
			case IDP_DOUBLE:
				oc.write((Double) value, "value", 0);
				break;
			case IDP_IDPARRAY:
				oc.writeSavableArrayList((ArrayList) value, "value", null);
				break;
			case IDP_NUMTYPES:
				LOGGER.warning("Numtypes value not supported! Cannot write it!");
				break;
			// case IDP_ID://not yet implemented in blender
			// break;
			default:
				LOGGER.warning("Cannot save the property's value! Invalid type! Property: name: " + name + "; type: " + type);
		}
	}

	@Override
	public void read(JmeImporter im) throws IOException {
		InputCapsule ic = im.getCapsule(this);
		name = ic.readString("name", DEFAULT_NAME);
		type = ic.readInt("type", 0);
		subType = ic.readInt("subtype", 0);
		description = ic.readString("description", null);
		switch (type) {
			case IDP_STRING:
				value = ic.readString("value", null);
				break;
			case IDP_INT:
				value = Integer.valueOf(ic.readInt("value", 0));
				break;
			case IDP_FLOAT:
				value = Float.valueOf(ic.readFloat("value", 0.0f));
				break;
			case IDP_ARRAY:
				switch (subType) {
					case IDP_INT:
						value = ic.readIntArray("value", null);
						break;
					case IDP_FLOAT:
						value = ic.readFloatArray("value", null);
						break;
					case IDP_DOUBLE:
						value = ic.readDoubleArray("value", null);
						break;
					default:
						LOGGER.warning("Cannot read the property's value! Invalid array subtype! Property: name: " + name + "; subtype: " + subType);
				}
			case IDP_GROUP:
				value = ic.readSavable("value", null);
				break;
			case IDP_DOUBLE:
				value = Double.valueOf(ic.readDouble("value", 0.0));
				break;
			case IDP_IDPARRAY:
				value = ic.readSavableArrayList("value", null);
				break;
			case IDP_NUMTYPES:
				LOGGER.warning("Numtypes value not supported! Cannot read it!");
				break;
			// case IDP_ID://not yet implemented in blender
			// break;
			default:
				LOGGER.warning("Cannot read the property's value! Invalid type! Property: name: " + name + "; type: " + type);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (description == null ? 0 : description.hashCode());
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + subType;
		result = prime * result + type;
		result = prime * result + (value == null ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		Properties other = (Properties) obj;
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (subType != other.subType) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}
}
