package org.openmolecules.fx.viewer3d.interactions.drugscore;

import com.actelion.research.chem.StereoMolecule;

public interface InteractionAtomClassifier {
	int[] classifyAtoms(StereoMolecule mol);
	String getAtomTypeName(int type);
}
