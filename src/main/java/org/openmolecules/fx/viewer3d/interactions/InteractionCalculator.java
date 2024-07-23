package org.openmolecules.fx.viewer3d.interactions;

import org.openmolecules.fx.viewer3d.V3DMolecule;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public interface InteractionCalculator {
	public List<InteractionPoint> determineInteractionPoints(V3DMolecule fxmol);
	public void determineInteractions(InteractionSites is1, InteractionSites is2, TreeMap<Integer, ArrayList<Interaction>> interactionMap);
	}
