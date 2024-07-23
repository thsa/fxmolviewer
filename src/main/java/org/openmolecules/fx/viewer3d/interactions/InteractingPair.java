package org.openmolecules.fx.viewer3d.interactions;

import java.util.ArrayList;
import java.util.TreeMap;

public class InteractingPair {

	private final InteractionSites mISites1;
	private final InteractionSites mISites2;
	private final InteractionCalculator mCalculator;
	private final TreeMap<Integer,ArrayList<Interaction>> mInteractionMap;

	public InteractingPair(InteractionSites iSites1,
	                       InteractionSites iSites2,
	                       InteractionCalculator calculator) {
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
		for(ArrayList<Interaction> interactions : mInteractionMap.values())
			for (Interaction interaction : interactions)
				interaction.cleanup();

		mInteractionMap.clear();
	}

	public void recalc() {
		cleanup();
		createInteractions();
	}

	private void createInteractions() {
		mCalculator.determineInteractions(mISites1, mISites2, mInteractionMap);
		for(ArrayList<Interaction> interactions : mInteractionMap.values())
			for (Interaction interaction : interactions)
				interaction.create();
	}

	public void setVisibility(boolean visible) {
		for(ArrayList<Interaction> interactions : mInteractionMap.values())
			for (Interaction interaction : interactions)
				interaction.setVisibility(visible);
	}

	private void molVisibilityChanged(boolean visible) {
		for(ArrayList<Interaction> interactions : mInteractionMap.values())
			for (Interaction interaction : interactions)
				interaction.setVisibility(visible);
	}
}