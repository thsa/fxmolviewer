package org.openmolecules.fx.viewer3d.interactions;

import java.util.ArrayList;
import java.util.List;

import org.openmolecules.fx.viewer3d.MolCoordinatesChangeListener;
import org.openmolecules.fx.viewer3d.MolStructureChangeListener;
import org.openmolecules.fx.viewer3d.V3DMolecule;

import com.actelion.research.chem.phesa.pharmacophore.IPharmacophorePoint;
import com.actelion.research.chem.phesa.pharmacophore.PharmacophoreCalculator;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;




/**
 * @author JW
 * Class that stores the interaction sites of a V3DMolecule and automatically updates 
 * their internal coordinates when the molecule coordinates are changed
 */



public class V3DInteractionSites implements MolCoordinatesChangeListener, MolStructureChangeListener, Observable{
	
	private List<IPharmacophorePoint> interactionSites = new ArrayList<IPharmacophorePoint>();
	private V3DMolecule fxmol;
	private List<InvalidationListener> invalidationListeners;
	
	public V3DInteractionSites(V3DMolecule fxmol) {
		interactionSites = PharmacophoreCalculator.getPharmacophorePoints(fxmol.getMolecule());
		this.fxmol = fxmol;
		fxmol.addMoleculeCoordinatesChangeListener(this);
		fxmol.addMoleculeStructureChangeListener(this);
		invalidationListeners = new ArrayList<InvalidationListener>();
	}

	@Override
	public void coordinatesChanged() {
		for(IPharmacophorePoint pp: interactionSites)
			pp.updateCoordinates(fxmol.getMolecule());
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
