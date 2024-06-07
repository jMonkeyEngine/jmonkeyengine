package org.jmonkeyengine.screenshottests.testframework;

public class TestResolution{
    int width;
    int height;

    public TestResolution(int width, int height){
        this.width = width;
        this.height = height;
    }

    public int getWidth(){
        return width;
    }

    public int getHeight(){
        return height;
    }
}
