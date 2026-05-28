package org.openmolecules.fx.viewer3d.interactions;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import org.openmolecules.fx.viewer3d.MolCoordinatesChangeListener;
import org.openmolecules.fx.viewer3d.MolStructureChangeListener;
import org.openmolecules.fx.viewer3d.V3DMolecule;

import java.util.ArrayList;
import java.util.List;

public class V3DInteractionSite implements MolCoordinatesChangeListener, MolStructureChangeListener, Observable {

	private final V3DInteractionCalculator mCalculator;
	private List<V3DInteractionPoint> mInteractionPoints;
	private final V3DMolecule mFXMol;
	private final List<InvalidationListener> mInvalidationListeners;

	public V3DInteractionSite(V3DMolecule fxmol, V3DInteractionCalculator calculator) {
		mFXMol = fxmol;
		mCalculator = calculator;
		mInteractionPoints = mCalculator.determineInteractionPoints(fxmol);
		fxmol.addMoleculeCoordinatesChangeListener(this);
		fxmol.addMoleculeStructureChangeListener(this);
		mInvalidationListeners = new ArrayList<>();
	}

	@Override
	public void coordinatesChanged() {
		for(V3DInteractionPoint pp: mInteractionPoints)
			pp.updateCoordinates();
		mInvalidationListeners.forEach(i -> i.invalidated(this));
	}

	public V3DMolecule getFXMol() {
		return mFXMol;
	}

	public V3DInteractionCalculator getCalculator() {
		return mCalculator;
	}

	@Override
	public void structureChanged() {
		mInteractionPoints = mCalculator.determineInteractionPoints(mFXMol);
		mInvalidationListeners.forEach(i -> i.invalidated(this));
	}

	public List<V3DInteractionPoint> getSites() {
		return mInteractionPoints;
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
