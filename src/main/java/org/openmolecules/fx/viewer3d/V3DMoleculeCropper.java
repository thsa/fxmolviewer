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
import javafx.scene.Node;
import org.openmolecules.fx.surface.SurfaceCutter;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;
import org.openmolecules.fx.viewer3d.nodes.Ribbons;

import java.util.ArrayList;

public class V3DMoleculeCropper extends SurfaceCutter {
	private static final double CROP_ZONE_WIDTH = 1.6;

	private final double mSquareInsideDistance,mOutsideDistance,mSquareOutsideDistance;
	private final Point3D[] mRefPoints;
	private final Bounds mRefBounds;
	private final V3DMolecule mFXMol;
	private final StereoMolecule mMol;

	public V3DMoleculeCropper(V3DMolecule fxmol, double distance, Point3D[] refPoints, Bounds refBounds) {
		mFXMol = fxmol;
		mMol = fxmol.getMolecule();
		double insideDistance = distance - CROP_ZONE_WIDTH / 2;
		mSquareInsideDistance = insideDistance * insideDistance;
		mOutsideDistance = distance + CROP_ZONE_WIDTH / 2;
		mSquareOutsideDistance = mOutsideDistance * mOutsideDistance;
		mRefPoints = refPoints;
		mRefBounds = refBounds;
	}

	public V3DMoleculeCropper(StereoMolecule mol, double distance, Point3D[] refPoints, Bounds refBounds) {
		mFXMol = null;
		mMol = mol;
		double insideDistance = distance - CROP_ZONE_WIDTH / 2;
		mSquareInsideDistance = insideDistance * insideDistance;
		mOutsideDistance = distance + CROP_ZONE_WIDTH / 2;
		mSquareOutsideDistance = mOutsideDistance * mOutsideDistance;
		mRefPoints = refPoints;
		mRefBounds = refBounds;
	}

	/**
	 * Crops the V3DMolecule or just the StereoMolecule depending on the Constructor used,
	 * which means removes all atoms that are within the distance given to the constructor.
	 * If all atoms would be cropped, then no atoms are deleted and 2 is returned, because
	 * in this case the entire molecule should be removed outside of this method.
	 * If an V3DMolecule was given to the constructor and if it shows a carton or ribbon,
	 * then this is removed and freshly constructerd from the remaining atoms.
	 * @return 0: no atoms removed; 1: some atoms removed; 2: atoms would be removed, nothing cropped
	 */
	public int crop() {
		mMol.ensureHelperArrays(Molecule.cHelperNeighbours);
		boolean[] deleteAtom = new boolean[mMol.getAllAtoms()];
		int count = 0;
		for (int atom=0; atom<mMol.getAllAtoms(); atom++) {
			Coordinates c = mMol.getAtomCoordinates(atom);
			double x = c.x;
			double y = c.y;
			double z = c.z;
			if (mFXMol != null) {
				Point3D p = mFXMol.localToScene(x, y, z);
				x = p.getX();
				y = p.getY();
				z = p.getZ();
			}
			int beyond = isBeyondDistance(x, y, z);
			if (beyond == 1) {
				deleteAtom[atom] = true;
				count++;
			}
			else if (beyond == 0) {
				mMol.setAtomMarker(atom, true);  // fixed atom for minimizer
			}
		}
		// also remove atoms close to removal zone that loose all their neighbours and would be single unconnected remaining atoms
		for (int atom=0; atom<mMol.getAllAtoms(); atom++) {
			if (!deleteAtom[atom]) {
				int deletedNeighbourCount = 0;
				for (int i=0; i<mMol.getAllConnAtomsPlusMetalBonds(atom); i++)
					if (deleteAtom[mMol.getConnAtom(atom, i)])
						deletedNeighbourCount++;
				if (deletedNeighbourCount != 0 && deletedNeighbourCount == mMol.getAllConnAtomsPlusMetalBonds(atom)) {
					deleteAtom[atom] = true;
					count++;
				}
			}
		}

		if (count == mMol.getAllAtoms())
			return 2;

		if (count != 0) {
			if (mFXMol == null) {
				deleteAtoms(deleteAtom);
			}
			else {
				int ribbonMode = mFXMol.getRibbonMode();
				if (ribbonMode != Ribbons.MODE_NONE)
					mFXMol.setRibbonMode(Ribbons.MODE_NONE);

				deleteAtoms(deleteAtom);

				if (ribbonMode != Ribbons.MODE_NONE)
					mFXMol.setRibbonMode(ribbonMode);
			}
			return 1;
		}

		return 0;
	}

	// Delete all flagged atoms except those that have a direct connection
	// to an atom not to be deleted. In this case change the atom's atomic no
	// to 0 unless it is a hydrogen, which is not touched.
	// Note: This method sets the atom marker for all non-hydrogen atoms that
	// were not deleted and set to atomicNo=0 (exit vectors).
	private void deleteAtoms(boolean[] isToBeDeleted) {
		mMol.ensureHelperArrays(Molecule.cHelperNeighbours);

		// delete also all hydrogens that are connected to an atom destined to be deleted
		for (int atom=mMol.getAtoms(); atom<mMol.getAllAtoms(); atom++)
			if (isToBeDeleted[mMol.getConnAtom(atom, 0)])
				isToBeDeleted[atom] = true;

		// Set atomicNo to 0 for first layer of deleted non-H atoms.
		// They stay (deletion flag is purged), but bonds between them go.
		int[] borderAtom = new int[mMol.getAllAtoms()];
		int borderAtomCount = 0;
		for (int bond=0; bond<mMol.getAllBonds(); bond++) {
			int atom1 = mMol.getBondAtom(0, bond);
			int atom2 = mMol.getBondAtom(1, bond);
			if (isToBeDeleted[atom1] ^ isToBeDeleted[atom2])
				borderAtom[borderAtomCount++] = isToBeDeleted[atom1] ? atom1 : atom2;

			// Make sure that bond between two atoms for deletion is deleted
			if (isToBeDeleted[atom1] && isToBeDeleted[atom2])
				mMol.markBondForDeletion(bond);
		}
		for (int i=0; i<borderAtomCount; i++) {
			int atom = borderAtom[i];
			if (mMol.getAtomicNo(atom) != 1) {
				mMol.setAtomicNo(atom, 0);
				mMol.setAtomMarker(atom, true); // fixed atom for minimizer
			}
			isToBeDeleted[atom] = false;
		}

		int[] oldAtomToNew = new int[mMol.getAllAtoms()];
		int index = 0;
		for (int i=0; i<oldAtomToNew.length; i++) {
			if (isToBeDeleted[i])
				oldAtomToNew[i] = -1;
			else
				oldAtomToNew[i] = index++;
		}

		int[] oldBondToNew = new int[mMol.getAllBonds()];
		index = 0;
		for (int i=0; i<oldBondToNew.length; i++) {
			if (isToBeDeleted[mMol.getBondAtom(0, i)]
					|| isToBeDeleted[mMol.getBondAtom(1, i)])
				oldBondToNew[i] = -1;
			else
				oldBondToNew[i] = index++;
		}

		mMol.deleteAtoms(isToBeDeleted);

		if (mFXMol != null) {
			ArrayList<Node> nodesToBeDeleted = new ArrayList<>();
			for (Node node : mFXMol.getChildren()) {
				NodeDetail detail = (NodeDetail)node.getUserData();
				if (detail != null) {
					if (detail.isAtom()) {
						if (oldAtomToNew[detail.getAtom()] == -1)
							nodesToBeDeleted.add(node);
						else
							detail.setIndex(oldAtomToNew[detail.getAtom()]);
					} else if (detail.isBond()) {
						if (oldBondToNew[detail.getBond()] == -1)
							nodesToBeDeleted.add(node);
						else
							detail.setIndex(oldBondToNew[detail.getBond()]);
					}
				}
			}
			mFXMol.getChildren().removeAll(nodesToBeDeleted);
			mFXMol.setInitialCoordinates();
		}
	}

	@Override
	protected boolean isVertexToBeRemoved(float x, float y, float z) {
		Point3D p = mFXMol.localToScene(x, y, z);
		return isBeyondDistance(p.getX(), p.getY(), p.getZ()) == 1;
	}

	@Override
	protected void addCutPosition(float xi, float yi, float zi, float xo, float yo, float zo, float[] location) {
		location[0] += (xi+xo)/2;
		location[1] += (yi+yo)/2;
		location[2] += (zi+zo)/2;
	}

	/**
	 * @return -1,0,1 for (inside, within 0.1 nm of cropping distance, outside)
	 */
	private int isBeyondDistance(double x, double y, double z) {
		if (x < mRefBounds.getMinX() - mOutsideDistance
		 || x > mRefBounds.getMaxX() + mOutsideDistance
		 || y < mRefBounds.getMinY() - mOutsideDistance
		 || y > mRefBounds.getMaxY() + mOutsideDistance
		 || z < mRefBounds.getMinZ() - mOutsideDistance
		 || z > mRefBounds.getMaxZ() + mOutsideDistance)
			return 1;

		int result = 1;
		for (Point3D refP:mRefPoints) {
			double dx = Math.abs(x - refP.getX());
			if (dx < mOutsideDistance) {
				double dy = Math.abs(y - refP.getY());
				if (dy < mOutsideDistance) {
					double dz = Math.abs(z - refP.getZ());
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
