package org.openmolecules.fx.viewer3d.interactions;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;

import java.util.ArrayList;
import java.util.TreeMap;

public class V3DInteractingPair {

	private final V3DInteractionSite mISite1;
	private final V3DInteractionSite mISite2;
	private final V3DInteractionCalculator mCalculator;
	private final TreeMap<Integer,ArrayList<V3DInteraction>> mInteractionMap;
	private final InvalidationListener mInvalidationListener;
	private final ChangeListener<Boolean> mVisibilityChangeListener;

	public V3DInteractingPair(V3DInteractionSite iSite1,
							  V3DInteractionSite iSite2,
							  V3DInteractionCalculator calculator) {
		mISite1 = iSite1;
		mISite2 = iSite2;
		mCalculator = calculator;
		//fxmol1.addMoleculeCoordinatesChangeListener(this);
		//fxmol2.addMoleculeCoordinatesChangeListener(this);
		//fxmol1.addMoleculeCoordinatesChangeListener(this);
		//fxmol2.addMoleculeCoordinatesChangeListener(this);

		mInvalidationListener  = observable -> recalc();
		mISite1.addListener(mInvalidationListener);
		mISite2.addListener(mInvalidationListener);

		mVisibilityChangeListener = (observable, oldValue, newValue) -> molVisibilityChanged(newValue);
		mISite1.getFXMol().visibleProperty().addListener(mVisibilityChangeListener);
		mISite2.getFXMol().visibleProperty().addListener(mVisibilityChangeListener);

		mInteractionMap = new TreeMap<>();
		createInteractions();
	}

	public V3DInteractionSite getInteractionSite(int i) {
		return i == 0 ? mISite1 : mISite2;
	}

	public void cleanup() {
		mISite1.removeListener(mInvalidationListener);
		mISite2.removeListener(mInvalidationListener);
		mISite1.getFXMol().visibleProperty().removeListener(mVisibilityChangeListener);
		mISite2.getFXMol().visibleProperty().removeListener(mVisibilityChangeListener);
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

	public TreeMap<Integer,ArrayList<V3DInteraction>> getInteractionMap() {
		return mInteractionMap;
	}

	private void createInteractions() {
		mCalculator.determineInteractions(mISite1, mISite2, mInteractionMap);
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