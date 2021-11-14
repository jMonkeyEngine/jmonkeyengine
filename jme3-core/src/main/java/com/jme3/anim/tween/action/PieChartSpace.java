/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.anim.tween.action;

import com.jme3.math.FastMath;

/**
 * A mathematical space used for blending between 2 successive {@link com.jme3.anim.tween.action.BlendableAction}s
 * based on a circle sector area {@link PieChartSpace#sectorArea} calculated from {@link PieChartSpace#radius} and {@link PieChartSpace#angle},
 * the sector area is scaled by the scaleFactor (userCircleArea / unitCircleArea) at the last step before applying values,
 * and the step value represents a fraction from 0 to 1 (when the area of the circle sector reaches the areaOfUnitCircle), it means by how much the
 * sectorArea approximates the unit circle area.
 *
 * @author pavl_g.
 */
public class PieChartSpace implements BlendSpace{

    protected BlendAction action;
    //pie-chart radius -- max is 1f
    protected float radius;
    //sector angle -- max is 360f in degrees
    protected float angle;
    //pie-chart area
    protected float area;
    //pie-chart sector area
    protected float sectorArea;
    protected int firstActionIndex;
    protected boolean sectorAreaManualAdjustment;

    /**
     * Instantiates a default pie chart space implementation.
     * Default radius = 1f.
     * Default angle = 45 degrees.
     * Default area = 0.125 of unit circle area.
     */
    public PieChartSpace(){
        this(1f, 45f);
    }

    /**
     * Instantiates a pie chart space with a radius and a sector angle,
     * radius is clamped in the range [0, 1] and angle is clamped
     * in the range [0, 360].
     * @param radius circle radius.
     * @param angle sector angle in degrees.
     */
    public PieChartSpace(final float radius, final float angle){
        //implicit suppression to extrapolation
        //clamp values in the range :  r = [0, 1] and angle = [0, 360].
        this.radius = radius % 1.1f;
        this.angle = angle % 360.1f;
    }

    @Override
    public void setBlendAction(BlendAction action) {
        this.action = action;
        //calculate the area of the pieChart
        area = FastMath.PI * FastMath.pow(radius, 2f);
        //calculate the sector area
        sectorArea = (angle / 360f) * area;
    }

    @Override
    public float getWeight() {
        //keep the values updated with the loop (coherent update) as long as the manual adjustment is false.
        if(!sectorAreaManualAdjustment) {
            setBlendAction(action);
        }
        //calculate the unit circle area.
        final float areaOfUnitCircle = FastMath.PI;
        //the scaleFactor is the factor of ratio between the user's area and the unit circle area.
        final float scaleFactor = area / areaOfUnitCircle;
        //scaling the pieChart sector area with respect to the unitCircleArea.
        final float scaledSector = sectorArea * scaleFactor;
        //converting the step value to percentage from 0% (no stepping) to 100% (unitCircleArea).
        final float step = scaledSector / areaOfUnitCircle;
        //assign 2 successive actions to blend between them using the step value.
        //successive actions can be altered using setFirstActionIndex(int firstActionIndex).
        if(firstActionIndex < action.getActions().length - 1) {
            action.setFirstActiveIndex(firstActionIndex++);
            action.setSecondActiveIndex(firstActionIndex);
        }else{
            firstActionIndex = 0;
        }
        return step;
    }

    /**
     * Manually alters the value of the sector area.
     * Notes :
     * - Altering the value of the sector area manually would
     * ignore both {@link PieChartSpace#radius} and {@link PieChartSpace#angle}.
     *
     * - Adjust {@link PieChartSpace#setSectorAreaManualAdjustment(boolean)} to false
     * to neutralize the manual effect and return back to using both (the radius and the angle).
     *
     * @param sectorArea a sector area to use.
     */
    @Override
    public void setValue(float sectorArea) {
        this.sectorArea = sectorArea;
        //activates sector area adjustment ignoring the angle and radius
        this.sectorAreaManualAdjustment = true;
    }

    /**
     * Enables/Disables the manual area adjustment flag.
     *
     * @param sectorAreaManualAdjustment true to enable manual adjustment of the sector area ignoring both the radius and the angle,
     *                                   false to use the radius and the angle to calculate the sector area and ignore {@link PieChartSpace#setValue(float)}.
     */
    public void setSectorAreaManualAdjustment(boolean sectorAreaManualAdjustment) {
        this.sectorAreaManualAdjustment = sectorAreaManualAdjustment;
    }

    /**
     * Tests whether the manual adjustment is activated.
     * @return true if manual adjustment is enabled, false otherwise.
     */
    public boolean isSectorAreaManualAdjustment() {
        return sectorAreaManualAdjustment;
    }

    /**
     * Alters the angle value of the pie-chart sector.
     * Values are internally clamped in the range of [0, 360].
     * @param angle the angle in degrees.
     */
    public void setAngle(float angle) {
        this.angle = angle % 360.1f;
    }

    /**
     * Gets the angle value of the pie-chart sector in degrees.
     * @return the angle in degrees.
     */
    public float getAngle() {
        return angle;
    }

    /**
     * Alters the radius of the pie-chart.
     * Values are internally clamped in the range of [0, 1].
     * @param radius the circle radius.
     */
    public void setRadius(float radius) {
        this.radius = radius % 1.1f;
    }

    /**
     * Gets the radius of the pie-chart.
     * @return the radius of the circle.
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Explicitly alters the index of the first action.
     * Usually values represented here depends on the number of {@link com.jme3.anim.tween.action.BlendableAction}s used within
     * {@link com.jme3.anim.tween.action.BlendAction} arguments.
     * Indices of other actions are auto obtained by the {@link PieChartSpace#getWeight()}.
     * @param firstActionIndex the index of the first action.
     */
    public void setFirstActionIndex(int firstActionIndex) {
        this.firstActionIndex = firstActionIndex;
    }

    /**
     * Gets the index of the first action.
     * @return the index of first action in integers.
     */
    public int getFirstActionIndex() {
        return firstActionIndex;
    }
}
