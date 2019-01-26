/*
 * FXMolViewer, showing and manipulating molecules and protein structures in 3D.
 * Copyright (C) 2019 Thomas Sander

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * @author Thomas Sander
 */

package org.openmolecules.fx.viewer3d;

public class GeometryHelper {
	public static float getAngle(float x1, float y1, float z1, float x2, float y2, float z2) {
		float p = x1*x2+y1*y2+z1*z2;
		float l1 = (float)Math.sqrt(x1*x1+y1*y1+z1*z1);
		float l2 = (float)Math.sqrt(x2*x2+y2*y2+z2*z2);
		return (float)Math.acos(p/(l1*l2));
		}
	}
