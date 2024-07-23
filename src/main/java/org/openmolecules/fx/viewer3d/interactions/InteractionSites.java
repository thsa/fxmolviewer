package org.openmolecules.fx.viewer3d.interactions;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import org.openmolecules.fx.viewer3d.MolCoordinatesChangeListener;
import org.openmolecules.fx.viewer3d.MolStructureChangeListener;
import org.openmolecules.fx.viewer3d.V3DMolecule;

import java.util.ArrayList;
import java.util.List;

public class InteractionSites implements MolCoordinatesChangeListener, MolStructureChangeListener, Observable {

	private final InteractionCalculator mCalculator;
	private List<InteractionPoint> mInteractionSites;
	private final V3DMolecule mFXMol;
	private final List<InvalidationListener> mInvalidationListeners;

	public InteractionSites(V3DMolecule fxmol, InteractionCalculator calculator) {
		mFXMol = fxmol;
		mCalculator = calculator;
		mInteractionSites = mCalculator.determineInteractionPoints(fxmol);
		fxmol.addMoleculeCoordinatesChangeListener(this);
		fxmol.addMoleculeStructureChangeListener(this);
		mInvalidationListeners = new ArrayList<>();
	}

	@Override
	public void coordinatesChanged() {
		for(InteractionPoint pp: mInteractionSites)
			pp.updateCoordinates();
		mInvalidationListeners.forEach(i -> i.invalidated(this));
	}

	public V3DMolecule getFXMol() {
		return mFXMol;
	}

	@Override
	public void structureChanged() {
		mInteractionSites = mCalculator.determineInteractionPoints(mFXMol);
		mInvalidationListeners.forEach(i -> i.invalidated(this));
	}

	public List<InteractionPoint> getSites() {
		return mInteractionSites;
	}

	@Override
	public void addListener(InvalidationListener listener) {
		mInvalidationListeners.add(listener);
	}

	@Override
	public void removeListener(InvalidationListener listener) {
		mInvalidationListeners.remove(listener);
	}
}
