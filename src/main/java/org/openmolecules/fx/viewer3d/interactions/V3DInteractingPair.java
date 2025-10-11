package org.openmolecules.fx.viewer3d.interactions;

import java.util.ArrayList;
import java.util.TreeMap;

public class V3DInteractingPair {

	private final V3DInteractionSites mISites1;
	private final V3DInteractionSites mISites2;
	private final V3DInteractionCalculator mCalculator;
	private final TreeMap<Integer,ArrayList<V3DInteraction>> mInteractionMap;

	public V3DInteractingPair(V3DInteractionSites iSites1,
							  V3DInteractionSites iSites2,
							  V3DInteractionCalculator calculator) {
		mISites1 = iSites1;
		mISites2 = iSites2;
		mCalculator = calculator;
		//fxmol1.addMoleculeCoordinatesChangeListener(this);
		//fxmol2.addMoleculeCoordinatesChangeListener(this);
		//fxmol1.addMoleculeCoordinatesChangeListener(this);
		//fxmol2.addMoleculeCoordinatesChangeListener(this);
		iSites1.addListener((o) -> recalc());
		iSites2.addListener((o) -> recalc());
		iSites1.getFXMol().visibleProperty().addListener((v,ov,nv) -> molVisibilityChanged(nv));
		iSites1.getFXMol().visibleProperty().addListener((v,ov,nv) -> molVisibilityChanged(nv));

		mInteractionMap = new TreeMap<>();
		createInteractions();
	}

	public boolean hasInteractions() {
		return !mInteractionMap.isEmpty();
	}

	public void cleanup() {
		for(ArrayList<V3DInteraction> interactions : mInteractionMap.values())
			for (V3DInteraction interaction : interactions)
				interaction.cleanup();

		mInteractionMap.clear();
	}

	public void recalc() {
		cleanup();
		createInteractions();
	}

	private void createInteractions() {
		mCalculator.determineInteractions(mISites1, mISites2, mInteractionMap);
		for(ArrayList<V3DInteraction> interactions : mInteractionMap.values())
			for (V3DInteraction interaction : interactions)
				interaction.create();
	}

	public void setVisibility(boolean visible) {
		for(ArrayList<V3DInteraction> interactions : mInteractionMap.values())
			for (V3DInteraction interaction : interactions)
				interaction.setVisibility(visible);
	}

	private void molVisibilityChanged(boolean visible) {
		for(ArrayList<V3DInteraction> interactions : mInteractionMap.values())
			for (V3DInteraction interaction : interactions)
				interaction.setVisibility(visible);
	}
}