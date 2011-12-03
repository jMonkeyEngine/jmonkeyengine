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

package com.jme3.terrain.geomipmap;

import com.jme3.scene.VertexBuffer.Type;
import java.nio.IntBuffer;

/**
 * Stores a terrain patch's details so the LOD background thread can update
 * the actual terrain patch back on the ogl thread.
 *
 * @author Brent Owens
 *
 */
public class UpdatedTerrainPatch {

	private TerrainPatch updatedPatch;
	private int newLod;
	private int previousLod;
	private int rightLod,topLod,leftLod,bottomLod;
	private IntBuffer newIndexBuffer;
	private boolean reIndexNeeded = false;
	private boolean fixEdges = false;

	public UpdatedTerrainPatch(TerrainPatch updatedPatch, int newLod) {
		this.updatedPatch = updatedPatch;
		this.newLod = newLod;
	}

	public UpdatedTerrainPatch(TerrainPatch updatedPatch, int newLod, int prevLOD, boolean reIndexNeeded) {
		this.updatedPatch = updatedPatch;
		this.newLod = newLod;
		this.previousLod = prevLOD;
		this.reIndexNeeded = reIndexNeeded;
		if (this.newLod <= 0)
                    throw new IllegalArgumentException();
	}

	public String getName() {
		return updatedPatch.getName();
	}

	protected boolean lodChanged() {
		if (reIndexNeeded && previousLod != newLod)
			return true;
		else
			return false;
	}

	protected TerrainPatch getUpdatedPatch() {
		return updatedPatch;
	}

	protected void setUpdatedPatch(TerrainPatch updatedPatch) {
		this.updatedPatch = updatedPatch;
	}

	protected int getNewLod() {
		return newLod;
	}

	public void setNewLod(int newLod) {
		this.newLod = newLod;
                if (this.newLod < 0)
                    throw new IllegalArgumentException();
	}

	protected IntBuffer getNewIndexBuffer() {
		return newIndexBuffer;
	}

	protected void setNewIndexBuffer(IntBuffer newIndexBuffer) {
		this.newIndexBuffer = newIndexBuffer;
	}


	protected int getRightLod() {
		return rightLod;
	}


	protected void setRightLod(int rightLod) {
		this.rightLod = rightLod;
	}


	protected int getTopLod() {
		return topLod;
	}


	protected void setTopLod(int topLod) {
		this.topLod = topLod;
	}


	protected int getLeftLod() {
		return leftLod;
	}


	protected void setLeftLod(int leftLod) {
		this.leftLod = leftLod;
	}


	protected int getBottomLod() {
		return bottomLod;
	}


	protected void setBottomLod(int bottomLod) {
		this.bottomLod = bottomLod;
	}

	public boolean isReIndexNeeded() {
		return reIndexNeeded;
	}

	public void setReIndexNeeded(boolean reIndexNeeded) {
		this.reIndexNeeded = reIndexNeeded;
	}

	public boolean isFixEdges() {
		return fixEdges;
	}

	public void setFixEdges(boolean fixEdges) {
		this.fixEdges = fixEdges;
	}

	public int getPreviousLod() {
		return previousLod;
	}

	public void setPreviousLod(int previousLod) {
		this.previousLod = previousLod;
	}

	public void updateAll() {
		updatedPatch.setLod(newLod);
		updatedPatch.setLodRight(rightLod);
		updatedPatch.setLodTop(topLod);
		updatedPatch.setLodLeft(leftLod);
		updatedPatch.setLodBottom(bottomLod);
		if (newIndexBuffer != null && (reIndexNeeded || fixEdges)) {
			updatedPatch.setPreviousLod(previousLod);
			updatedPatch.getMesh().clearBuffer(Type.Index);
			updatedPatch.getMesh().setBuffer(Type.Index, 3, newIndexBuffer);
		}
	}

}
