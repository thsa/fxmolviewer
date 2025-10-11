package org.openmolecules.fx.viewer3d.interactions.drugscore;

import com.actelion.research.chem.*;

import java.util.ArrayList;

public abstract class FragmentBasedAtomClassifier implements InteractionAtomClassifier {
	private final SSSearcher[] mFragmentSearcher;

	public FragmentBasedAtomClassifier() {
		mFragmentSearcher = new SSSearcher[getFragmentCount()];
		for (int i=0; i<getFragmentCount(); i++) {
			StereoMolecule fragment = getFragment(i);
			if (fragment != null) {
				mFragmentSearcher[i] = new SSSearcher();
				mFragmentSearcher[i].setFragment(fragment);
			}
		}
	}

	public abstract int getFragmentCount();
	public abstract String getFragmentName(int i);
	public abstract StereoMolecule getFragment(int i);

	@Override
	public int[] classifyAtoms(StereoMolecule mol) {
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		int[] atomType = new int[mol.getAtoms()];
		int assignCount = 0;
		for (int i=0; assignCount<mol.getAtoms() && i<mFragmentSearcher.length; i++) {
			if (mFragmentSearcher[i] != null) {
				mFragmentSearcher[i].setMolecule(mol);

				if (mFragmentSearcher[i].findFragmentInMolecule(SSSearcher.cCountModeRigorous, SSSearcher.cDefaultMatchMode) != 0) {
					ArrayList<int[]> matchList = mFragmentSearcher[i].getMatchList();
					for (int[] match : matchList) {
						if (atomType[match[0]] == 0) {
							atomType[match[0]] = i+1;
							assignCount++;
						}
					}
				}
			}
		}

		return atomType;
	}

	public String getInteractingGroupName(int i) {
		return i == 0 ? "?" : getFragmentName(i-1);
	}
}
