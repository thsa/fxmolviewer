package org.openmolecules.render;

import com.actelion.research.chem.StereoMolecule;

public class AtomConstructionFilter implements ConstructionFilter {
	private StereoMolecule mMol;
	private int mAtom, mBond;
	private final boolean[] mAtomMask;

	public AtomConstructionFilter(StereoMolecule mol, boolean[] atomMask) {
		mMol = mol;
		mAtomMask = atomMask;
		mAtom = 0;
		mBond = 0;
	}

	@Override
	public int getNextAtom() {
		while (mAtom<mMol.getAllAtoms() && mAtom<mAtomMask.length && !mAtomMask[mAtom])
			mAtom++;

		if (mAtom == mMol.getAllAtoms() || mAtom==mAtomMask.length)
			return -1;

		return mAtom++;
	}

	@Override
	public int getNextBond() {
		while (mBond<mMol.getAllBonds()) {
			int atom1 = mMol.getBondAtom(0, mBond);
			int atom2 = mMol.getBondAtom(1, mBond);
			if (atom1 < mAtomMask.length && mAtomMask[atom1]
			 && atom2 < mAtomMask.length && mAtomMask[atom2])
				break;
			mBond++;
			}

		if (mBond == mMol.getAllBonds())
			return -1;

		return mBond++;
	}
}
