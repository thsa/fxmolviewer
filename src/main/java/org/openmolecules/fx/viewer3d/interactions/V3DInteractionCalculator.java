package org.openmolecules.fx.viewer3d.interactions;

import org.openmolecules.fx.viewer3d.V3DMolecule;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public interface V3DInteractionCalculator {
	List<V3DInteractionPoint> determineInteractionPoints(V3DMolecule fxmol);
	void determineInteractions(V3DInteractionSite is1, V3DInteractionSite is2, TreeMap<Integer, ArrayList<V3DInteraction>> interactionMap);
	String getInteractionTypeName();
	String getAtomTypeName(int type, boolean isProtein);
	String getInteractionInfo(V3DInteraction interaction, int remoteIndex, boolean isProtein);
	}
