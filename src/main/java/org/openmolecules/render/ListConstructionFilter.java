package org.openmolecules.render;

import java.util.ArrayList;

public class ListConstructionFilter implements ConstructionFilter {
	private final ArrayList<Integer> mAtomList, mBondList;
	private int mAtomIndex,mBondIndex;

	public ListConstructionFilter(ArrayList<Integer> atomList, ArrayList<Integer> bondList) {
		mAtomList = atomList;
		mBondList = bondList;
		mAtomIndex = 0;
		mBondIndex = 0;
	}

	@Override
	public int getNextAtom() {
		if (mAtomIndex == mAtomList.size())
			return -1;

		return mAtomList.get(mAtomIndex++);
	}

	@Override
	public int getNextBond() {
		if (mBondIndex == mBondList.size())
			return -1;

		return mBondList.get(mBondIndex++);
	}
}
