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

package org.openmolecules.fx.surface;

import org.openmolecules.fx.viewer3d.V3DMolecule;

/**
 * Created by thomas on 23.04.16.
 */
public class ClipSurfaceCutter extends SurfaceCutter {
	private float mNearClip, mFarClip;
	private V3DMolecule mFXMol;

	public ClipSurfaceCutter(float nearSlab, float farSlab, V3DMolecule fxmol) {
		mNearClip = nearSlab;
		mFarClip = farSlab;
		mFXMol = fxmol;
	}

	@Override
	protected void addCutPosition(float xi, float yi, float zi, float xo, float yo, float zo, float[] location) {
		float szi = (float)mFXMol.localToScene(xi, yi, zi).getZ();
		float szo = (float)mFXMol.localToScene(xo, yo, zo).getZ();
		float f = (szo < mNearClip) ? (mNearClip - szo) / (szi - szo) : (szo - mFarClip) / (szo - szi);
		location[0] += xo + f * (xi - xo);
		location[1] += yo + f * (yi - yo);
		location[2] += zo + f * (zi - zo);
	}

	@Override
	protected boolean isVertexToBeRemoved(float x, float y, float z) {
		double sz = mFXMol.localToScene(x, y, z).getZ();
		return (sz < mNearClip) || (sz > mFarClip);
	}
}
