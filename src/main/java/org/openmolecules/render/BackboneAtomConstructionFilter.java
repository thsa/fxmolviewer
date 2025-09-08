package org.openmolecules.render;

import com.actelion.research.chem.StereoMolecule;

public class BackboneAtomConstructionFilter extends AtomConstructionFilter {
	/**
	 * Creates a construction filter for a protein with known ligand that marks all
	 * protein backbone atoms and all side chains of which atom least one atom is in
	 * a given distance to ligand atom.
	 * @param protein
	 * @param isBackboneAtom
	 */
	public BackboneAtomConstructionFilter(StereoMolecule protein, boolean[] isBackboneAtom) {
		super(protein, addMetalAtoms(protein, isBackboneAtom.clone()));
	}

	private static boolean[] addMetalAtoms(StereoMolecule protein, boolean[] isBackboneAtom) {
		for (int ap=0; ap<protein.getAllAtoms(); ap++)
			if (ap<isBackboneAtom.length && protein.isMetalAtom(ap))
				isBackboneAtom[ap] = true;
		return isBackboneAtom;
	}
}
