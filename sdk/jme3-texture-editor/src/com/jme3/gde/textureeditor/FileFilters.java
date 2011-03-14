package com.jme3.gde.textureeditor;

public interface FileFilters {

    ExtensionFileFilter PNG = new ExtensionFileFilter(".png", "PNG Image");
    ExtensionFileFilter JPG = new ExtensionFileFilter(".jpg", "JPG Image");
    ExtensionFileFilter BMP = new ExtensionFileFilter(".bmp", "BMP Image");
    ExtensionFileFilter TGA = new ExtensionFileFilter(".tga", "TGA Image");
}
