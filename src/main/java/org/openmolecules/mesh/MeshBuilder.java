/*
 * Copyright 2017 Thomas Sander, Therwilerstrasse 41, CH-4153 Reinach, Switzerland
 *
 * This file is part of openmolecules.org's 3D-Molecule-Viewer.
 *
 * 3D-Molecule-Viewer is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * 3D-Molecule-Viewer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with 3D-Molecule-Viewer.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package org.openmolecules.mesh;

public interface MeshBuilder {
	int addPoint(float x, float y, float z);
	void addTriangle(int i1, int i2, int i3);
	void getPoint(int index, float[] xyz);
	}
