package org.openmolecules.fx.viewer3d.interactions;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;

import java.util.ArrayList;
import java.util.TreeMap;

public class V3DInteractingPair {

	private final V3DInteractionSites mISites1;
	private final V3DInteractionSites mISites2;
	private final V3DInteractionCalculator mCalculator;
	private final TreeMap<Integer,ArrayList<V3DInteraction>> mInteractionMap;
	private final InvalidationListener mInvalidationListener;
	private final ChangeListener<Boolean> mVisibilityChangeListener;

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

		mInvalidationListener  = observable -> recalc();
		iSites1.addListener(mInvalidationListener);
		iSites2.addListener(mInvalidationListener);

		mVisibilityChangeListener = (observable, oldValue, newValue) -> molVisibilityChanged(newValue);
		iSites1.getFXMol().visibleProperty().addListener(mVisibilityChangeListener);
		iSites2.getFXMol().visibleProperty().addListener(mVisibilityChangeListener);

		mInteractionMap = new TreeMap<>();
		createInteractions();
	}

	public void cleanup() {
		mISites1.removeListener(mInvalidationListener);
		mISites2.removeListener(mInvalidationListener);
		mISites1.getFXMol().visibleProperty().removeListener(mVisibilityChangeListener);
		mISites2.getFXMol().visibleProperty().removeListener(mVisibilityChangeListener);
	}

	public boolean hasInteractions() {
		return !mInteractionMap.isEmpty();
	}

	public void removeInteractions() {
		for(ArrayList<V3DInteraction> interactions : mInteractionMap.values())
			for (V3DInteraction interaction : interactions)
				interaction.remove();

		mInteractionMap.clear();
	}

	public void recalc() {
		removeInteractions();
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