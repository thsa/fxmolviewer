package org.openmolecules.fx.viewer3d.interactions.jw;

import com.actelion.research.chem.phesa.pharmacophore.PharmacophoreCalculator;
import com.actelion.research.chem.phesa.pharmacophore.pp.IPharmacophorePoint;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import org.openmolecules.fx.viewer3d.MolCoordinatesChangeListener;
import org.openmolecules.fx.viewer3d.MolStructureChangeListener;
import org.openmolecules.fx.viewer3d.V3DMolecule;

import java.util.ArrayList;
import java.util.List;




/**
 * @author JW
 * Class that stores the interaction sites of a V3DMolecule and automatically updates 
 * their internal coordinates when the molecule coordinates are changed
 */



public class JWInteractionSites implements MolCoordinatesChangeListener, MolStructureChangeListener, Observable{
	
	private List<IPharmacophorePoint> interactionSites = new ArrayList<IPharmacophorePoint>();
	private final V3DMolecule fxmol;
	private final List<InvalidationListener> invalidationListeners;
	
	public JWInteractionSites(V3DMolecule fxmol) {
		interactionSites = PharmacophoreCalculator.getPharmacophorePoints(fxmol.getMolecule());
		this.fxmol = fxmol;
		fxmol.addMoleculeCoordinatesChangeListener(this);
		fxmol.addMoleculeStructureChangeListener(this);
		invalidationListeners = new ArrayList<InvalidationListener>();
	}

	@Override
	public void coordinatesChanged() {
		for(IPharmacophorePoint pp: interactionSites)
			pp.updateCoordinates(fxmol.getMolecule().getAtomCoordinates());
		invalidationListeners.forEach(i -> i.invalidated(this));
	}
	
	@Override
	public void structureChanged() {
		interactionSites = PharmacophoreCalculator.getPharmacophorePoints(fxmol.getMolecule());
		invalidationListeners.forEach(i -> i.invalidated(this));
	}
	
	public List<IPharmacophorePoint> getSites() {
		return interactionSites;
	}



	@Override
	public void addListener(InvalidationListener listener) {
		invalidationListeners.add(listener);
	}

	@Override
	public void removeListener(InvalidationListener listener) {
		invalidationListeners.remove(listener);
		
	}
}
