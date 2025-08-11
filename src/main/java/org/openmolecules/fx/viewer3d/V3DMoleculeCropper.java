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
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import org.openmolecules.fx.surface.SurfaceCutter;
import org.openmolecules.fx.viewer3d.nodes.Ribbons;

public class V3DMoleculeCropper extends SurfaceCutter {
	private static final double CROP_ZONE_WIDTH = 1.6;

	private final double mSquareInsideDistance,mOutsideDistance,mSquareOutsideDistance;
	private final Point3D[] mRefPoints;
	private final Bounds mRefBounds;
	private final V3DMolecule mFXMol;

	public V3DMoleculeCropper(V3DMolecule fxmol, double distance, Point3D[] refPoints, Bounds refBounds) {
		mFXMol = fxmol;
		double insideDistance = distance - CROP_ZONE_WIDTH / 2;
		mSquareInsideDistance = insideDistance * insideDistance;
		mOutsideDistance = distance + CROP_ZONE_WIDTH / 2;
		mSquareOutsideDistance = mOutsideDistance * mOutsideDistance;
		mRefPoints = refPoints;
		mRefBounds = refBounds;
	}

	public void crop() {
		StereoMolecule mol = mFXMol.getMolecule();
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		boolean[] deleteAtom = new boolean[mol.getAllAtoms()];
		int count = 0;
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			Coordinates c = mol.getCoordinates(atom);
			int beyond = isBeyondDistance(mFXMol.localToScene(c.x, c.y, c.z));
			if (beyond == 1) {
				deleteAtom[atom] = true;
				count++;
			}
			else if (beyond == 0) {
				mol.setAtomMarker(atom, true);  // fixed atom for minimizer
			}
		}
		// also remove atoms close to removal zone that loose all their neighbours and would be single unconnected remaining atoms
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			if (!deleteAtom[atom]) {
				int deletedNeighbourCount = 0;
				for (int i=0; i<mol.getAllConnAtomsPlusMetalBonds(atom); i++)
					if (deleteAtom[mol.getConnAtom(atom, i)])
						deletedNeighbourCount++;
				if (deletedNeighbourCount != 0 && deletedNeighbourCount == mol.getAllConnAtomsPlusMetalBonds(atom)) {
					deleteAtom[atom] = true;
					count++;
				}
			}
		}

		if (count != 0) {
			int ribbonMode = mFXMol.getRibbonMode();
			if (ribbonMode != Ribbons.MODE_NONE)
				mFXMol.setRibbonMode(Ribbons.MODE_NONE);

			mFXMol.deleteAtoms(deleteAtom);

			if (ribbonMode != Ribbons.MODE_NONE)
				mFXMol.setRibbonMode(ribbonMode);
		}
	}

	@Override
	protected boolean isVertexToBeRemoved(float x, float y, float z) {
		return isBeyondDistance(mFXMol.localToScene(x, y, z)) == 1;
	}

	@Override
	protected void addCutPosition(float xi, float yi, float zi, float xo, float yo, float zo, float[] location) {
		location[0] += (xi+xo)/2;
		location[1] += (yi+yo)/2;
		location[2] += (zi+zo)/2;
	}

	/**
	 * @param p
	 * @return -1,0,1 for (inside, within 0.1 nm of cropping distance, outside)
	 */
	private int isBeyondDistance(Point3D p) {
		if (p.getX() < mRefBounds.getMinX() - mOutsideDistance
		 || p.getX() > mRefBounds.getMaxX() + mOutsideDistance
		 || p.getY() < mRefBounds.getMinY() - mOutsideDistance
		 || p.getY() > mRefBounds.getMaxY() + mOutsideDistance
		 || p.getZ() < mRefBounds.getMinZ() - mOutsideDistance
		 || p.getZ() > mRefBounds.getMaxZ() + mOutsideDistance)
			return 1;

		int result = 1;
		for (Point3D refP:mRefPoints) {
			double dx = Math.abs(p.getX() - refP.getX());
			if (dx < mOutsideDistance) {
				double dy = Math.abs(p.getY() - refP.getY());
				if (dy < mOutsideDistance) {
					double dz = Math.abs(p.getZ() - refP.getZ());
					if (dz < mOutsideDistance) {
						double squareDistance = dx*dx+dy*dy+dz*dz;
						if (squareDistance < mSquareInsideDistance)
							return -1;
						if (squareDistance < mSquareOutsideDistance)
							result = 0;
					}
				}
			}
		}

		return result;
	}
}
