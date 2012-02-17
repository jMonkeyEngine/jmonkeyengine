/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jme3test.games;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingVolume;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Dome;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kyle "bonechilla" Williams
 */
public class CubeField extends SimpleApplication implements AnalogListener {

    public static void main(String[] args) {
        CubeField app = new CubeField();
        app.start();
    }

    private BitmapFont defaultFont;

    private boolean START;
    private int difficulty, Score, colorInt, highCap, lowCap,diffHelp;
    private Node player;
    private Geometry fcube;
    private ArrayList<Geometry> cubeField;
    private ArrayList<ColorRGBA> obstacleColors;
    private float speed, coreTime,coreTime2;
    private float camAngle = 0;
    private BitmapText fpsScoreText, pressStart;

    private boolean solidBox = true;
    private Material playerMaterial;
    private Material floorMaterial;

    private float fpsRate = 1000f / 1f;

    /**
     * Initializes game 
     */
    @Override
    public void simpleInitApp() {
        Logger.getLogger("com.jme3").setLevel(Level.WARNING);

        flyCam.setEnabled(false);
        setDisplayStatView(false);

        Keys();

        defaultFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        pressStart = new BitmapText(defaultFont, false);
        fpsScoreText = new BitmapText(defaultFont, false);

        loadText(fpsScoreText, "Current Score: 0", defaultFont, 0, 2, 0);
        loadText(pressStart, "PRESS ENTER", defaultFont, 0, 5, 0);
        
        player = createPlayer();
        rootNode.attachChild(player);
        cubeField = new ArrayList<Geometry>();
        obstacleColors = new ArrayList<ColorRGBA>();

        gameReset();
    }
    /**
     * Used to reset cubeField 
     */
    private void gameReset(){
        Score = 0;
        lowCap = 10;
        colorInt = 0;
        highCap = 40;
        difficulty = highCap;

        for (Geometry cube : cubeField){
            cube.removeFromParent();
        }
        cubeField.clear();

        if (fcube != null){
            fcube.removeFromParent();
        }
        fcube = createFirstCube();

        obstacleColors.clear();
        obstacleColors.add(ColorRGBA.Orange);
        obstacleColors.add(ColorRGBA.Red);
        obstacleColors.add(ColorRGBA.Yellow);
        renderer.setBackgroundColor(ColorRGBA.White);
        speed = lowCap / 400f;
        coreTime = 20.0f;
        coreTime2 = 10.0f;
        diffHelp=lowCap;
        player.setLocalTranslation(0,0,0);
    }

    @Override
    public void simpleUpdate(float tpf) {
        camTakeOver(tpf);
        if (START){
            gameLogic(tpf);
        }
        colorLogic();
    }
    /**
     * Forcefully takes over Camera adding functionality and placing it behind the character
     * @param tpf Tickes Per Frame
     */
    private void camTakeOver(float tpf) {
        cam.setLocation(player.getLocalTranslation().add(-8, 2, 0));
        cam.lookAt(player.getLocalTranslation(), Vector3f.UNIT_Y);
        
        Quaternion rot = new Quaternion();
        rot.fromAngleNormalAxis(camAngle, Vector3f.UNIT_Z);
        cam.setRotation(cam.getRotation().mult(rot));
        camAngle *= FastMath.pow(.99f, fpsRate * tpf);
    }

    @Override
    public void requestClose(boolean esc) {
        if (!esc){
            System.out.println("The game was quit.");
        }else{
            System.out.println("Player has Collided. Final Score is " + Score);
        }
        context.destroy(false);
    }
    /**
     * Randomly Places a cube on the map between 30 and 90 paces away from player
     */
    private void randomizeCube() {
        Geometry cube = fcube.clone();
        int playerX = (int) player.getLocalTranslation().getX();
        int playerZ = (int) player.getLocalTranslation().getZ();
//        float x = FastMath.nextRandomInt(playerX + difficulty + 10, playerX + difficulty + 150);
        float x = FastMath.nextRandomInt(playerX + difficulty + 30, playerX + difficulty + 90);
        float z = FastMath.nextRandomInt(playerZ - difficulty - 50, playerZ + difficulty + 50);
        cube.getLocalTranslation().set(x, 0, z);

//        playerX+difficulty+30,playerX+difficulty+90

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        if (!solidBox){
            mat.getAdditionalRenderState().setWireframe(true);
        }
        mat.setColor("Color", obstacleColors.get(FastMath.nextRandomInt(0, obstacleColors.size() - 1)));
        cube.setMaterial(mat);

        rootNode.attachChild(cube);
        cubeField.add(cube);
    }

    private Geometry createFirstCube() {
        Vector3f loc = player.getLocalTranslation();
        loc.addLocal(4, 0, 0);
        Box b = new Box(loc, 1, 1, 1);

        Geometry geom = new Geometry("Box", b);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);

        return geom;
    }

    private Node createPlayer() {
        Dome b = new Dome(Vector3f.ZERO, 10, 100, 1);
        Geometry playerMesh = new Geometry("Box", b);

        playerMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        playerMaterial.setColor("Color", ColorRGBA.Red);
        playerMesh.setMaterial(playerMaterial);
        playerMesh.setName("player");

        Box floor = new Box(Vector3f.ZERO.add(playerMesh.getLocalTranslation().getX(),
                playerMesh.getLocalTranslation().getY() - 1, 0), 100, 0, 100);
        Geometry floorMesh = new Geometry("Box", floor);

        floorMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        floorMaterial.setColor("Color", ColorRGBA.LightGray);
        floorMesh.setMaterial(floorMaterial);
        floorMesh.setName("floor");

        Node playerNode = new Node();
        playerNode.attachChild(playerMesh);
        playerNode.attachChild(floorMesh);

        return playerNode;
    }

    /**
     * If Game is Lost display Score and Reset the Game
     */
    private void gameLost(){
        START = false;
        loadText(pressStart, "You lost! Press enter to try again.", defaultFont, 0, 5, 0);
        gameReset();
    }
    
    /**
     * Core Game Logic
     */
    private void gameLogic(float tpf){
    	//Subtract difficulty level in accordance to speed every 10 seconds
    	if(timer.getTimeInSeconds()>=coreTime2){
			coreTime2=timer.getTimeInSeconds()+10;
			if(difficulty<=lowCap){
				difficulty=lowCap;
			}
			else if(difficulty>lowCap){
				difficulty-=5;
				diffHelp+=1;
			}
		}
    	
        if(speed<.1f){
            speed+=.000001f*tpf*fpsRate;
        }

        player.move(speed * tpf * fpsRate, 0, 0);
        if (cubeField.size() > difficulty){
            cubeField.remove(0);
        }else if (cubeField.size() != difficulty){
            randomizeCube();
        }

        if (cubeField.isEmpty()){
            requestClose(false);
        }else{
            for (int i = 0; i < cubeField.size(); i++){
            	
            	//better way to check collision
                Geometry playerModel = (Geometry) player.getChild(0);
                Geometry cubeModel = cubeField.get(i);
                cubeModel.updateGeometricState();

                BoundingVolume pVol = playerModel.getWorldBound();
                BoundingVolume vVol = cubeModel.getWorldBound();

                if (pVol.intersects(vVol)){
                    gameLost();
                    return;
                }
                //Remove cube if 10 world units behind player
                if (cubeField.get(i).getLocalTranslation().getX() + 10 < player.getLocalTranslation().getX()){
                    cubeField.get(i).removeFromParent();
                    cubeField.remove(cubeField.get(i));
                }

            }
        }

        Score += fpsRate * tpf;
        fpsScoreText.setText("Current Score: "+Score);
    }
    /**
     * Sets up the keyboard bindings
     */
    private void Keys() {
        inputManager.addMapping("START", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("Left",  new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addListener(this, "START", "Left", "Right");
    }

    public void onAnalog(String binding, float value, float tpf) {
        if (binding.equals("START") && !START){
            START = true;
            guiNode.detachChild(pressStart);
            System.out.println("START");
        }else if (START == true && binding.equals("Left")){
            player.move(0, 0, -(speed / 2f) * value * fpsRate);
            camAngle -= value*tpf;
        }else if (START == true && binding.equals("Right")){
            player.move(0, 0, (speed / 2f) * value * fpsRate);
            camAngle += value*tpf;
        }
    }

    /**
     * Determines the colors of the player, floor, obstacle and background
     */
    private void colorLogic() {
    	if (timer.getTimeInSeconds() >= coreTime){
            
        	colorInt++;
            coreTime = timer.getTimeInSeconds() + 20;
        

	        switch (colorInt){
	            case 1:
	                obstacleColors.clear();
	                solidBox = false;
	                obstacleColors.add(ColorRGBA.Green);
	                renderer.setBackgroundColor(ColorRGBA.Black);
	                playerMaterial.setColor("Color", ColorRGBA.White);
			floorMaterial.setColor("Color", ColorRGBA.Black);
	                break;
	            case 2:
	                obstacleColors.set(0, ColorRGBA.Black);
	                solidBox = true;
	                renderer.setBackgroundColor(ColorRGBA.White);
	                playerMaterial.setColor("Color", ColorRGBA.Gray);
                        floorMaterial.setColor("Color", ColorRGBA.LightGray);
	                break;
	            case 3:
	                obstacleColors.set(0, ColorRGBA.Pink);
	                break;
	            case 4:
	                obstacleColors.set(0, ColorRGBA.Cyan);
	                obstacleColors.add(ColorRGBA.Magenta);
	                renderer.setBackgroundColor(ColorRGBA.Gray);
                        floorMaterial.setColor("Color", ColorRGBA.Gray);
	                playerMaterial.setColor("Color", ColorRGBA.White);
	                break;
	            case 5:
	                obstacleColors.remove(0);
	                renderer.setBackgroundColor(ColorRGBA.Pink);
	                solidBox = false;
	                playerMaterial.setColor("Color", ColorRGBA.White);
	                break;
	            case 6:
	                obstacleColors.set(0, ColorRGBA.White);
	                solidBox = true;
	                renderer.setBackgroundColor(ColorRGBA.Black);
	                playerMaterial.setColor("Color", ColorRGBA.Gray);
                        floorMaterial.setColor("Color", ColorRGBA.LightGray);
	                break;
	            case 7:
	                obstacleColors.set(0, ColorRGBA.Green);
	                renderer.setBackgroundColor(ColorRGBA.Gray);
	                playerMaterial.setColor("Color", ColorRGBA.Black);
                        floorMaterial.setColor("Color", ColorRGBA.Orange);
	                break;
	            case 8:
	                obstacleColors.set(0, ColorRGBA.Red);
                        floorMaterial.setColor("Color", ColorRGBA.Pink);
	                break;
	            case 9:
	                obstacleColors.set(0, ColorRGBA.Orange);
	                obstacleColors.add(ColorRGBA.Red);
	                obstacleColors.add(ColorRGBA.Yellow);
	                renderer.setBackgroundColor(ColorRGBA.White);
	                playerMaterial.setColor("Color", ColorRGBA.Red);
	                floorMaterial.setColor("Color", ColorRGBA.Gray);
	                colorInt=0;
	                break;
	            default:
	                break;
	        }
        }
    }
    /**
     * Sets up a BitmapText to be displayed
     * @param txt the Bitmap Text
     * @param text the 
     * @param font the font of the text
     * @param x    
     * @param y
     * @param z
     */
    private void loadText(BitmapText txt, String text, BitmapFont font, float x, float y, float z) {
        txt.setSize(font.getCharSet().getRenderedSize());
        txt.setLocalTranslation(txt.getLineWidth() * x, txt.getLineHeight() * y, z);
        txt.setText(text);
        guiNode.attachChild(txt);
    }
} 