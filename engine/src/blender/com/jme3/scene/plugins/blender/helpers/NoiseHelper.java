/*
 *
 * $Id: noise.c 14611 2008-04-29 08:24:33Z campbellbarton $
 *
 * ***** BEGIN GPL LICENSE BLOCK *****
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The Original Code is Copyright (C) 2001-2002 by NaN Holding BV.
 * All rights reserved.
 *
 * The Original Code is: all of this file.
 *
 * Contributor(s): none yet.
 *
 * ***** END GPL LICENSE BLOCK *****
 *
 */
package com.jme3.scene.plugins.blender.helpers;

/**
 * Methods of this class are copied from blender 2.49 source code and modified so that they can be used in java. They are mostly NOT
 * documented because they are not documented in blender's source code. If I find a proper description or discover what they actually do and
 * what parameters mean - I shall describe such methods :) If anyone have some hint what these methods are doing please rite the proper
 * javadoc documentation. These methods should be used to create generated textures.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class NoiseHelper extends com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper {

    /**
     * Constructor. Stores the blender version number and loads the constants needed for computations.
     * 
     * @param blenderVersion
     *            the number of blender version
     */
    public NoiseHelper(String blenderVersion) {
        super(blenderVersion);
    }
}
