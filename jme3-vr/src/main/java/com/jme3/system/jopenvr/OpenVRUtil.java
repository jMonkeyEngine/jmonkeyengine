package com.jme3.system.jopenvr;

import com.jme3.input.vr.OpenVRInput;
import com.jme3.system.jopenvr.JOpenVRLibrary.EColorSpace;
import com.jme3.system.jopenvr.JOpenVRLibrary.ETextureType;
import com.jme3.system.jopenvr.JOpenVRLibrary.ETrackedDeviceProperty;
import com.jme3.system.jopenvr.JOpenVRLibrary.ETrackedPropertyError;
import com.jme3.system.jopenvr.JOpenVRLibrary.EVRCompositorError;
import com.jme3.system.jopenvr.JOpenVRLibrary.EVRInitError;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * A utility class that provide helper methods for OpenVR system.
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 *
 */
public class OpenVRUtil {

	/**
	 * Get the value of the given string {@link JOpenVRLibrary.ETrackedDeviceProperty property} attached to the given device.
	 * @param system the underlying OpenVR system.
	 * @param deviceIndex the index of the device to query.
	 * @param property the property to query.
	 * @param bufferSize the size of the buffer to use for storing native string.
	 * @return the value of the given string property attached to the given device.
	 * @see OpenVRInput#getTrackedControllerCount()
	 * @see JOpenVRLibrary.ETrackedDeviceProperty
	 * @see #getTrackedDeviceStringProperty(VR_IVRSystem_FnTable, int, int)
	 */
	public static String getTrackedDeviceStringProperty(VR_IVRSystem_FnTable system, int deviceIndex, int property, int bufferSize){
		String str ="";
		
		int unBufferSize = 256;
		Pointer pchValue = new Memory(unBufferSize); 
		IntByReference pError = new IntByReference();
		
		system.GetStringTrackedDeviceProperty.apply(deviceIndex, property, pchValue, unBufferSize, pError);
		
		if (pError.getValue() == ETrackedPropertyError.ETrackedPropertyError_TrackedProp_Success){
			str = pchValue.getString(0);
		} else if (pError.getValue() == ETrackedPropertyError.ETrackedPropertyError_TrackedProp_BufferTooSmall){
			throw new IllegalArgumentException("Cannot access property \""+getETrackedDevicePropertyString(property)+"\" ("+property+") for device "+deviceIndex+": "+getETrackedPropertyErrorString(pError.getValue())+" ("+pError.getValue()+")");
		} else if (pError.getValue() == ETrackedPropertyError.ETrackedPropertyError_TrackedProp_CouldNotContactServer){
			throw new IllegalArgumentException("Cannot access property \""+getETrackedDevicePropertyString(property)+"\" ("+property+") for device "+deviceIndex+": "+getETrackedPropertyErrorString(pError.getValue())+" ("+pError.getValue()+")");
		} else if (pError.getValue() == ETrackedPropertyError.ETrackedPropertyError_TrackedProp_InvalidDevice){
			throw new IllegalArgumentException("Cannot access property \""+getETrackedDevicePropertyString(property)+"\" ("+property+") for device "+deviceIndex+": "+getETrackedPropertyErrorString(pError.getValue())+" ("+pError.getValue()+")");
		} else if (pError.getValue() == ETrackedPropertyError.ETrackedPropertyError_TrackedProp_InvalidOperation){
			throw new IllegalArgumentException("Cannot access property \""+getETrackedDevicePropertyString(property)+"\" ("+property+") for device "+deviceIndex+": "+getETrackedPropertyErrorString(pError.getValue())+" ("+pError.getValue()+")");
		} else if (pError.getValue() == ETrackedPropertyError.ETrackedPropertyError_TrackedProp_NotYetAvailable){
			throw new IllegalArgumentException("Cannot access property \""+getETrackedDevicePropertyString(property)+"\" ("+property+") for device "+deviceIndex+": "+getETrackedPropertyErrorString(pError.getValue())+" ("+pError.getValue()+")");
		} else if (pError.getValue() == ETrackedPropertyError.ETrackedPropertyError_TrackedProp_PermissionDenied){
			throw new IllegalArgumentException("Cannot access property \""+getETrackedDevicePropertyString(property)+"\" ("+property+") for device "+deviceIndex+": "+getETrackedPropertyErrorString(pError.getValue())+" ("+pError.getValue()+")");
		} else if (pError.getValue() == ETrackedPropertyError.ETrackedPropertyError_TrackedProp_StringExceedsMaximumLength){
			throw new IllegalArgumentException("Cannot access property \""+getETrackedDevicePropertyString(property)+"\" ("+property+") for device "+deviceIndex+": "+getETrackedPropertyErrorString(pError.getValue())+" ("+pError.getValue()+")");
		} else if (pError.getValue() == ETrackedPropertyError.ETrackedPropertyError_TrackedProp_UnknownProperty){
			throw new IllegalArgumentException("Cannot access property \""+getETrackedDevicePropertyString(property)+"\" ("+property+") for device "+deviceIndex+": "+getETrackedPropertyErrorString(pError.getValue())+" ("+pError.getValue()+")");
		} else if (pError.getValue() == ETrackedPropertyError.ETrackedPropertyError_TrackedProp_ValueNotProvidedByDevice){
			throw new IllegalArgumentException("Cannot access property \""+getETrackedDevicePropertyString(property)+"\" ("+property+") for device "+deviceIndex+": "+getETrackedPropertyErrorString(pError.getValue())+" ("+pError.getValue()+")");
		} else if (pError.getValue() == ETrackedPropertyError.ETrackedPropertyError_TrackedProp_WrongDataType){
			throw new IllegalArgumentException("Cannot access property \""+getETrackedDevicePropertyString(property)+"\" ("+property+") for device "+deviceIndex+": "+getETrackedPropertyErrorString(pError.getValue())+" ("+pError.getValue()+")");
		} else if (pError.getValue() == ETrackedPropertyError.ETrackedPropertyError_TrackedProp_WrongDeviceClass){
			throw new IllegalArgumentException("Cannot access property \""+getETrackedDevicePropertyString(property)+"\" ("+property+") for device "+deviceIndex+": "+getETrackedPropertyErrorString(pError.getValue())+" ("+pError.getValue()+")");
		} else {
			throw new IllegalArgumentException("Cannot access property \""+getETrackedDevicePropertyString(property)+"\" ("+property+") for device "+deviceIndex+": "+getETrackedPropertyErrorString(pError.getValue())+" ("+pError.getValue()+")");
		}
		
		return str;
	}
	
	/**
	 * Get the value of the given string {@link JOpenVRLibrary.ETrackedDeviceProperty property} attached to the given device.
	 * @param system the underlying OpenVR system.
	 * @param deviceIndex the index of the device to query.
	 * @param property the property to query.
	 * @return the value of the given string property attached to the given device.
	 * @see OpenVRInput#getTrackedControllerCount()
	 * @see JOpenVRLibrary.ETrackedDeviceProperty
	 * @see #getTrackedDeviceStringProperty(VR_IVRSystem_FnTable, int, int, int)
	 */
	public static String getTrackedDeviceStringProperty(VR_IVRSystem_FnTable system, int deviceIndex, int property){
      return getTrackedDeviceStringProperty(system, deviceIndex, property, 256);
	}
	
	/**
	 * Get the String description of the given {@link ETrackedPropertyError string tracked property error}.
	 * @param error the string tracked property error.
	 * @return the String description of the given string tracked property error.
	 */
	public static String getETrackedPropertyErrorString(int error){
		String str ="";
		
		switch(error){
		case ETrackedPropertyError.ETrackedPropertyError_TrackedProp_Success:
			str = "Success";
			break;
		case ETrackedPropertyError.ETrackedPropertyError_TrackedProp_WrongDataType:
			str = "Wrong data type";
			break;
		case ETrackedPropertyError.ETrackedPropertyError_TrackedProp_WrongDeviceClass:
			str = "Wrong device class";
			break;
		case ETrackedPropertyError.ETrackedPropertyError_TrackedProp_BufferTooSmall:
			str = "Buffer too small";
			break;
		case ETrackedPropertyError.ETrackedPropertyError_TrackedProp_UnknownProperty:
			str = "Unknown property";
			break;
		case ETrackedPropertyError.ETrackedPropertyError_TrackedProp_InvalidDevice:
			str = "Invalid device";
			break;
		case ETrackedPropertyError.ETrackedPropertyError_TrackedProp_CouldNotContactServer:
			str = "Could not contact server";
			break;
		case ETrackedPropertyError.ETrackedPropertyError_TrackedProp_ValueNotProvidedByDevice:
			str = "Value not provided by device";
			break;
		case ETrackedPropertyError.ETrackedPropertyError_TrackedProp_StringExceedsMaximumLength:
			str = "String exceed maximum length";
			break;
		case ETrackedPropertyError.ETrackedPropertyError_TrackedProp_NotYetAvailable:
			str = "Not yet available";
			break;
		case ETrackedPropertyError.ETrackedPropertyError_TrackedProp_PermissionDenied:
			str = "Permission denied";
			break;
		case ETrackedPropertyError.ETrackedPropertyError_TrackedProp_InvalidOperation:
			str = "Invalid operation";
			break;
		default:
			str = "Not handled error";
		}
		
		return str;
	}
	
	/**
	 * Get the description of the given {@link EColorSpace color space}.
	 * @param eColorSpace the color space.
	 * @return the description of the given color space.
	 */
	public static String getEColorSpaceString(int eColorSpace){
		String str = "";
		
		switch(eColorSpace){
		  case EColorSpace.EColorSpace_ColorSpace_Auto:
			  str = "Auto";
			  break;
		  case EColorSpace.EColorSpace_ColorSpace_Gamma:
			  str = "Gamma";
			  break;
		  case EColorSpace.EColorSpace_ColorSpace_Linear:
			  str = "Linear";
			  break;
		  default:
			  str = "Unknown ("+eColorSpace+")";
		}
		
		return str;
	}
	
	/**
	 * Get the description of the given {@link ETextureType texture type}.
	 * @param type the texture type
	 * @return the description of the given texture type.
	 */
	public static String getETextureTypeString(int type){
		
		String str = "";
		
		switch(type){
		  case ETextureType.ETextureType_TextureType_DirectX:
			  str = "DirectX";
			  break;
		  case ETextureType.ETextureType_TextureType_OpenGL:
			  str = "OpenGL";
			  break;
		  case ETextureType.ETextureType_TextureType_Vulkan:
			  str = "Vulkan";
			  break;
		  case ETextureType.ETextureType_TextureType_IOSurface:
			  str = "IOSurface";
			  break;
		  case ETextureType.ETextureType_TextureType_DirectX12:
			  str = "DirectX12";
			  break;
		  default: 
			  str = "Unknown ("+type+")";
		}
		
		return str;
	}
	
	/**
	 * Get the description of the given {@link EVRCompositorError EVR compositor error}.
	 * @param error the EVR compositor error.
	 * @return the description of the given EVR compositor error.
	 */
	public static String getEVRCompositorErrorString(int error){
		String str ="";
		
		switch(error){
		  case EVRCompositorError.EVRCompositorError_VRCompositorError_None: 
			  str = "None"; 
		  break;
		  case EVRCompositorError.EVRCompositorError_VRCompositorError_RequestFailed: 
			  str = "Request failed"; 
		  break;
		  case EVRCompositorError.EVRCompositorError_VRCompositorError_IncompatibleVersion: 
			  str = "Incompatible version"; 
		  break;
		  case EVRCompositorError.EVRCompositorError_VRCompositorError_DoNotHaveFocus: 
			  str = "Do not have focus"; 
		  break;
		  case EVRCompositorError.EVRCompositorError_VRCompositorError_InvalidTexture: 
             str = "Invalid texture"; 
		  break;
		  case EVRCompositorError.EVRCompositorError_VRCompositorError_IsNotSceneApplication: 
			  str = "Is not scene application"; 
		  break;
		  case EVRCompositorError.EVRCompositorError_VRCompositorError_TextureIsOnWrongDevice: 
			  str = "Texture is on wrong device"; 
		  break;
		  case EVRCompositorError.EVRCompositorError_VRCompositorError_TextureUsesUnsupportedFormat: 
			  str = "Texture uses unsupported format"; 
		  break;
		  case EVRCompositorError.EVRCompositorError_VRCompositorError_SharedTexturesNotSupported: 
			  str = "Shared textures not supported"; 
		  break;
		  case EVRCompositorError.EVRCompositorError_VRCompositorError_IndexOutOfRange: 
			  str = "Index out of range"; 
		  break;
		  case EVRCompositorError.EVRCompositorError_VRCompositorError_AlreadySubmitted: 
			  str = "Already submitted"; 
		  break;
		}
		return str;
	}
	
	/**
	 * Get the description of the given {@link EVRInitError EVR init error}.
	 * @param error the EVR init error.
	 * @return the description of the given EVR init error.
	 */
	public static String getEVRInitErrorString(int error){
		String str = "";
		
		switch(error){
		
		
		case EVRInitError.EVRInitError_VRInitError_None: 
			str="None"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Unknown: 
			str="Unknown"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_InstallationNotFound: 
			str="Installation not found"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_InstallationCorrupt: 
			str="Installation corrupt"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_VRClientDLLNotFound: 
			str="VR Client DLL not found"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_FileNotFound: 
			str="File not found"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_FactoryNotFound: 
			str="Factory not found"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_InterfaceNotFound: 
			str="Interface not found"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_InvalidInterface: 
			str="Invalid interface"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_UserConfigDirectoryInvalid: 
			str="User config directory invalid"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_HmdNotFound: 
			str="HMD not found"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_NotInitialized: 
			str="Not initialized"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_PathRegistryNotFound: 
			str="Path registry not found"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_NoConfigPath: 
			str="No config path"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_NoLogPath: 
			str="No log path"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_PathRegistryNotWritable: 
			str="Path registry not writable"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_AppInfoInitFailed: 
			str="AppInfo init failed"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_Retry: 
			str="Init retry";
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_InitCanceledByUser: 
			str="Init canceled by user"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_AnotherAppLaunching: 
			str="Another app launching"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_SettingsInitFailed: 
			str="Setting init failed"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_ShuttingDown: 
			str="Shutting down"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_TooManyObjects: 
			str="Too many objects"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_NoServerForBackgroundApp: 
			str="No server background app"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_NotSupportedWithCompositor: 
			str="Not supported with compositor"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_NotAvailableToUtilityApps: 
			str="Not available to utility apps"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_Internal: 
			str="Internal"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_HmdDriverIdIsNone: 
			str="Driver Id is None"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_HmdNotFoundPresenceFailed: 
			str="HMD not found presence failed"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_VRMonitorNotFound: 
			str="VR monitor not found"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_VRMonitorStartupFailed: 
			str="VR monitor startup failed"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_LowPowerWatchdogNotSupported: 
			str="Low power watchdog not supported"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_InvalidApplicationType: 
			str="Invalid application type"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_NotAvailableToWatchdogApps: 
			str="Not available to watchdog apps"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_WatchdogDisabledInSettings: 
			str="Watchdog disabled in settings"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_VRDashboardNotFound: 
			str="VR dashboard not found"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Init_VRDashboardStartupFailed: 
			str="VR dashboard setup failed"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Driver_Failed: 
			str="Driver failed"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Driver_Unknown: 
			str="Driver unknown"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Driver_HmdUnknown: 
			str="HMD unknown"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Driver_NotLoaded: 
			str="Driver not loaded"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Driver_RuntimeOutOfDate: 
			str="Driver runtime out of date"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Driver_HmdInUse: 
			str="HMD in use"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Driver_NotCalibrated: 
			str="Not calibrated"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Driver_CalibrationInvalid: 
			str="Calibration invalid"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Driver_HmdDisplayNotFound: 
			str="HMD display not found"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Driver_TrackedDeviceInterfaceUnknown: 
			str="Tracked device interface unknown"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Driver_HmdDriverIdOutOfBounds: 
			str="HMD driver Id out of bounds"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_Driver_HmdDisplayMirrored: 
			str="HMD display mirrored"; 
			break;
		case EVRInitError.EVRInitError_VRInitError_IPC_ServerInitFailed: 
			str=""; 
			break;
		case EVRInitError.EVRInitError_VRInitError_IPC_ConnectFailed: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_IPC_SharedStateInitFailed: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_IPC_CompositorInitFailed: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_IPC_MutexInitFailed: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_IPC_Failed: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_IPC_CompositorConnectFailed: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_IPC_CompositorInvalidConnectResponse: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_IPC_ConnectFailedAfterMultipleAttempts: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_Compositor_Failed: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_Compositor_D3D11HardwareRequired: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_Compositor_FirmwareRequiresUpdate: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_Compositor_OverlayInitFailed: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_Compositor_ScreenshotsInitFailed: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_UnableToConnectToOculusRuntime: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_HmdFound_CantOpenDevice: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_HmdFound_UnableToRequestConfigStart: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_HmdFound_NoStoredConfig: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_HmdFound_ConfigTooBig: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_HmdFound_ConfigTooSmall: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_HmdFound_UnableToInitZLib: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_HmdFound_CantReadFirmwareVersion: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_HmdFound_UnableToSendUserDataStart: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_HmdFound_UnableToGetUserDataStart: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_HmdFound_UnableToGetUserDataNext: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_HmdFound_UserDataAddressRange: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_HmdFound_UserDataError: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_VendorSpecific_HmdFound_ConfigFailedSanityCheck: str=""; break;
		case EVRInitError.EVRInitError_VRInitError_Steam_SteamInstallationNotFound: str=""; break;
		default:
	    }
		
		return str;
	}
	
	/**
	 * Get the description of the given tracked device property.
	 * @param property the tracked device property.
	 * @return the description of the given tracked device property.
	 */
	public static String getETrackedDevicePropertyString(int property){
		String str = "";

		switch(property){
		
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_Invalid:
			str = "Invalid";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_TrackingSystemName_String:
			str = "Tracking system name";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ModelNumber_String:
			str = "Model number";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_SerialNumber_String:
			str = "Serial number";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_RenderModelName_String:
			str = "Render model name";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_WillDriftInYaw_Bool:
			str = "Will drift in yaw";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ManufacturerName_String:
			str = "Manufacturer name";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_TrackingFirmwareVersion_String:
			str = "Tracking firmware version";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_HardwareRevision_String:
			str = "Hardware revision";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_AllWirelessDongleDescriptions_String:
			str = "All wireless dongle descriptions";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ConnectedWirelessDongle_String:
			str = "Connect wireless dongle";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DeviceIsWireless_Bool:
			str = "Device is wireless";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DeviceIsCharging_Bool:
			str = "Device is charging";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DeviceBatteryPercentage_Float:
			str = "Device battery percentage";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_StatusDisplayTransform_Matrix34:
			str = "Status display transform";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_Firmware_UpdateAvailable_Bool:
			str = "Update available";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_Firmware_ManualUpdate_Bool:
			str = "Firmware manual update";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_Firmware_ManualUpdateURL_String:
			str = "Firmware manual update URL";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_HardwareRevision_Uint64:
			str = "Hardware revision";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FirmwareVersion_Uint64:
			str = "Firmware version";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FPGAVersion_Uint64:
			str = "FPGA version";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_VRCVersion_Uint64:
			str = "VRC version";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_RadioVersion_Uint64:
			str = "Radio version";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DongleVersion_Uint64:
			str = "Dongle version";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_BlockServerShutdown_Bool:
			str = "Block server shutdown";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_CanUnifyCoordinateSystemWithHmd_Bool:
			str = "Can unify coordinate system with HMD";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ContainsProximitySensor_Bool:
			str = "Contains proximity sensor";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DeviceProvidesBatteryStatus_Bool:
			str = "Device provides battery status";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DeviceCanPowerOff_Bool:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_Firmware_ProgrammingTarget_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DeviceClass_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_HasCamera_Bool:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DriverVersion_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_Firmware_ForceUpdateRequired_Bool:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ViveSystemButtonFixRequired_Bool:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ParentDriver_Uint64:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ReportsTimeSinceVSync_Bool:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_SecondsFromVsyncToPhotons_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayFrequency_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_UserIpdMeters_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_CurrentUniverseId_Uint64:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_PreviousUniverseId_Uint64:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayFirmwareVersion_Uint64:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_IsOnDesktop_Bool:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayMCType_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayMCOffset_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayMCScale_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_EdidVendorID_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayMCImageLeft_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayMCImageRight_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayGCBlackClamp_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_EdidProductID_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_CameraToHeadTransform_Matrix34:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayGCType_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayGCOffset_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayGCScale_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayGCPrescale_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayGCImage_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_LensCenterLeftU_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_LensCenterLeftV_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_LensCenterRightU_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_LensCenterRightV_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_UserHeadToEyeDepthMeters_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_CameraFirmwareVersion_Uint64:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_CameraFirmwareDescription_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayFPGAVersion_Uint64:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayBootloaderVersion_Uint64:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayHardwareVersion_Uint64:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_AudioFirmwareVersion_Uint64:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_CameraCompatibilityMode_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ScreenshotHorizontalFieldOfViewDegrees_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ScreenshotVerticalFieldOfViewDegrees_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplaySuppressed_Bool:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayAllowNightMode_Bool:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayMCImageWidth_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayMCImageHeight_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayMCImageNumChannels_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayMCImageData_Binary:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_UsesDriverDirectMode_Bool:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_AttachedDeviceId_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_SupportedButtons_Uint64:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_Axis0Type_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_Axis1Type_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_Axis2Type_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_Axis3Type_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_Axis4Type_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ControllerRoleHint_Int32:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewLeftDegrees_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewRightDegrees_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewTopDegrees_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewBottomDegrees_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_TrackingRangeMinimumMeters_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_TrackingRangeMaximumMeters_Float:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ModeLabel_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_IconPathName_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_NamedIconPathDeviceOff_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_NamedIconPathDeviceSearching_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_NamedIconPathDeviceSearchingAlert_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_NamedIconPathDeviceReady_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_NamedIconPathDeviceReadyAlert_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_NamedIconPathDeviceNotReady_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_NamedIconPathDeviceStandby_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_NamedIconPathDeviceAlertLow_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayHiddenArea_Binary_Start:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayHiddenArea_Binary_End:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_UserConfigPath_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_InstallPath_String:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_VendorSpecific_Reserved_Start:
			str = "";
			break;
		case ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_VendorSpecific_Reserved_End:
			str = "";
			break;
		}
		
		
		return str;
	}
}
