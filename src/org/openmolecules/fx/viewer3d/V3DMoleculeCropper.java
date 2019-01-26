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

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.conf.Conformer;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import org.openmolecules.fx.surface.SurfaceCutter;

public class V3DMoleculeCropper extends SurfaceCutter {

	private double mDistance,mSquareDistance;
	private Point3D[] mRefPoints;
	private Bounds mRefBounds;
	private V3DMolecule mFXMol;

	public V3DMoleculeCropper(V3DMolecule fxmol, double distance, Point3D[] refPoints, Bounds refBounds) {
		mFXMol = fxmol;
		mDistance = distance;
		mSquareDistance = distance * distance;
		mRefPoints = refPoints;
		mRefBounds = refBounds;
	}

	public void crop() {
		mFXMol.removeMeasurements();   // TODO be more specific
		Conformer conformer = mFXMol.getConformer();
		boolean[] deleteAtom = new boolean[conformer.getSize()];
		int count = 0;
		for (int atom=0; atom<conformer.getSize(); atom++) {
			Coordinates c = conformer.getCoordinates(atom);
			if (isBeyondDistance(mFXMol.localToScene(c.x, c.y, c.z))) {
				deleteAtom[atom] = true;
				count++;
			}
		}

		if (count != 0)
			mFXMol.deleteAtoms(deleteAtom);
	}

	@Override
	protected boolean isVertexToBeRemoved(float x, float y, float z) {
		return isBeyondDistance(mFXMol.localToScene(x, y, z));
	}

	@Override
	protected void addCutPosition(float xi, float yi, float zi, float xo, float yo, float zo, float[] location) {
		location[0] += (xi+xo)/2;
		location[1] += (yi+yo)/2;
		location[2] += (zi+zo)/2;
	}

	private boolean isBeyondDistance(Point3D p) {
		if (p.getX() < mRefBounds.getMinX() - mDistance
		 || p.getX() > mRefBounds.getMaxX() + mDistance
		 || p.getY() < mRefBounds.getMinY() - mDistance
		 || p.getY() > mRefBounds.getMaxY() + mDistance
		 || p.getZ() < mRefBounds.getMinZ() - mDistance
		 || p.getZ() > mRefBounds.getMaxZ() + mDistance)
			return true;

		for (Point3D refP:mRefPoints) {
			double dx = Math.abs(p.getX() - refP.getX());
			if (dx < mDistance) {
				double dy = Math.abs(p.getY() - refP.getY());
				if (dy <mDistance) {
					double dz = Math.abs(p.getZ() - refP.getZ());
					if (dz <mDistance) {
						if (dx*dx+dy*dy+dz*dz < mSquareDistance) {
							return false;
						}
					}
				}
			}
		}

		return true;
	}
}
