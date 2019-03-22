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

package org.openmolecules.fx.viewer3d.panel;

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.coords.CoordinateInventor;
import org.openmolecules.fx.viewer3d.V3DMolecule;

/**
 * Created by thomas on 09/10/16.
 */
public class MoleculeModel {
	private V3DMolecule mMol3D;
	private StereoMolecule mMol2D;
	private int mID;
	private static int sStructureCount;

	public MoleculeModel(V3DMolecule mol3D) {
		mMol3D = mol3D;
		mMol2D = null;
		mID = ++sStructureCount;
	}

	private StereoMolecule createMolecule2D(V3DMolecule mol3D) {
		StereoMolecule mol = mol3D.getMolecule().getCompactCopy();
		mol.ensureHelperArrays(Molecule.cHelperParities);	// create parities from 3D-coords
		new CoordinateInventor(CoordinateInventor.MODE_REMOVE_HYDROGEN).invent(mol);
		mol.setStereoBondsFromParity();

		return mol;
	}

	public String getMoleculeName() {
		if (mMol3D.getMolecule().getName() != null)
			return mMol3D.getMolecule().getName();

		return "Structure "+mID;
	}

	public StereoMolecule getMolecule2D() {
		if (mMol2D == null && mMol3D != null)
			mMol2D = createMolecule2D(mMol3D);

		return mMol2D;
	}

	public V3DMolecule getMolecule3D() {
		return mMol3D;
	}
}
