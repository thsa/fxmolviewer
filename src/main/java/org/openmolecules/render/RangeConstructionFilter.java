package org.openmolecules.render;

public class RangeConstructionFilter implements ConstructionFilter {
	private int mAtom,mBond;
	private final int mToAtom,mToBond;

	public RangeConstructionFilter(int fromAtom, int toAtom, int fromBond, int toBond) {
		mAtom = fromAtom;
		mBond = fromBond;
		mToAtom = toAtom;
		mToBond = toBond;
	}

	@Override
	public int getNextAtom() {
		if (mAtom == mToAtom)
			return -1;

		return mAtom++;
	}

	@Override
	public int getNextBond() {
		if (mBond == mToBond)
			return -1;

		return mBond++;
	}
}
