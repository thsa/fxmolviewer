package org.openmolecules.render;

import com.actelion.research.chem.StereoMolecule;

public class MaskConstructionFilter implements ConstructionFilter {
	private StereoMolecule mMol;
	private int mAtom, mBond;
	private final boolean[] mAtomMask;

	public MaskConstructionFilter(StereoMolecule mol, boolean[] atomMask) {
		mMol = mol;
		mAtomMask = atomMask;
		mAtom = 0;
		mBond = 0;
	}

	@Override
	public int getNextAtom() {
		while (mAtom< mMol.getAllAtoms() && !mAtomMask[mAtom])
			mAtom++;

		if (mAtom == mMol.getAllAtoms())
			return -1;

		return mAtom++;
	}

	@Override
	public int getNextBond() {
		while (mBond < mMol.getAllBonds() && !mAtomMask[mMol.getBondAtom(0, mBond)] && !mAtomMask[mMol.getBondAtom(1, mBond)])
			mBond++;

		if (mBond == mMol.getAllBonds())
			return -1;

		return mBond++;
	}
}
