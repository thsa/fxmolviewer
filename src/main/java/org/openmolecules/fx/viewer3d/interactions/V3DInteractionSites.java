package org.openmolecules.fx.viewer3d.interactions;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import org.openmolecules.fx.viewer3d.MolCoordinatesChangeListener;
import org.openmolecules.fx.viewer3d.MolStructureChangeListener;
import org.openmolecules.fx.viewer3d.V3DMolecule;

import java.util.ArrayList;
import java.util.List;

public class V3DInteractionSites implements MolCoordinatesChangeListener, MolStructureChangeListener, Observable {

	private final V3DInteractionCalculator mCalculator;
	private List<V3DInteractionPoint> mInteractionSites;
	private final V3DMolecule mFXMol;
	private final List<InvalidationListener> mInvalidationListeners;

	public V3DInteractionSites(V3DMolecule fxmol, V3DInteractionCalculator calculator) {
		mFXMol = fxmol;
		mCalculator = calculator;
		mInteractionSites = mCalculator.determineInteractionPoints(fxmol);
		fxmol.addMoleculeCoordinatesChangeListener(this);
		fxmol.addMoleculeStructureChangeListener(this);
		mInvalidationListeners = new ArrayList<>();
	}

	@Override
	public void coordinatesChanged() {
		for(V3DInteractionPoint pp: mInteractionSites)
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

	public List<V3DInteractionPoint> getSites() {
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
