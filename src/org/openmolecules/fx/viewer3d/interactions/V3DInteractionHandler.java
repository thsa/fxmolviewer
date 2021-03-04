package org.openmolecules.fx.viewer3d.interactions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmolecules.fx.viewer3d.V3DMolGroup;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;

import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.V3DSceneListener;


public class V3DInteractionHandler implements ListChangeListener<V3DMolGroup> {
	
	private V3DScene mScene3D;
	private List<V3DInteractingPair> mInteractingPairs;
	private Map<V3DMolecule,V3DInteractionSites> mInteractionSites;
	private BooleanProperty mVisibleProperty;
	
	public V3DInteractionHandler(V3DScene scene) {
		mScene3D = scene;
		mScene3D.getWorld().addListener(this);
		mInteractionSites  = new HashMap<V3DMolecule,V3DInteractionSites>();
		mInteractingPairs = new ArrayList<V3DInteractingPair>();
		mVisibleProperty = new SimpleBooleanProperty(true);
		mVisibleProperty.addListener((v,ov,nv) -> {
			for(V3DInteractingPair interactingPair : mInteractingPairs) {
				interactingPair.setVisibility(mVisibleProperty.get());
			}
		});
		
	}
	
	public void displayInteractions() {
		List<V3DMolecule> fxmols = mScene3D.getMolsInScene();
		for(V3DMolecule v3dmol : fxmols) {
				v3dmol.addImplicitHydrogens();
				v3dmol.RoleProperty().addListener((v,ov,nv) -> update());
				v3dmol.IDProperty().addListener((v,ov,nv) -> update());
				mInteractionSites.put(v3dmol, new V3DInteractionSites(v3dmol));
		}

		calculateInteractionsBetweenMols(fxmols);	
	}
	
	private void update() {
		cleanup();
		mScene3D.setInteractionHandler(null);
		//displayInteractions();
		//calculateInteractionsBetweenMols(mScene3D.getMolsInScene());
	}
	
	
	private void calculateInteractionsBetweenMols(List<V3DMolecule> fxmols) {
		for(int i=0;i<fxmols.size();i++) { 
			V3DMolecule fxmol1 = fxmols.get(i);
			if(fxmol1.getRole()!=MoleculeRole.MACROMOLECULE && fxmol1.getUnconnectedFragmentNo()>1) {
				V3DInteractingPair interactingPair = new V3DInteractingPair(fxmol1, fxmol1, mInteractionSites.get(fxmol1),
						mInteractionSites.get(fxmol1), mScene3D);
				interactingPair.analyze();
				mInteractingPairs.add(interactingPair);
			}
			for(int j=i+1;j<fxmols.size();j++) {
				V3DMolecule fxmol2 = fxmols.get(j);
				boolean interacting = areTwoMolsInteracting(fxmol1,fxmol2);
				if(interacting) {
					V3DInteractingPair interactingPair = new V3DInteractingPair(fxmol1, fxmol2, mInteractionSites.get(fxmol1),
						mInteractionSites.get(fxmol2), mScene3D);
					interactingPair.analyze();
					mInteractingPairs.add(interactingPair);
				}
			}
		}	
	}
	
	private boolean areTwoMolsInteracting(V3DMolecule fxmol1, V3DMolecule fxmol2) {
		boolean interacting = false;
		MoleculeRole role1 = fxmol1.getRole();
		MoleculeRole role2 = fxmol2.getRole();
		//only mols from same group interact;
		if(fxmol1.getParentSubGroup(mScene3D.getWorld())!=fxmol2.getParentSubGroup(mScene3D.getWorld()))
			return interacting;
		if(role1==MoleculeRole.SOLVENT && role2==MoleculeRole.SOLVENT)	
			interacting=true;
		else if(role1==role2)
			return interacting;
		if(role1==MoleculeRole.MACROMOLECULE || role2==MoleculeRole.MACROMOLECULE)
			interacting=true; //protein interacts with all other mols in the group
		else if(role1==MoleculeRole.COFACTOR || role2==MoleculeRole.COFACTOR)
			interacting=true; //same for cofactor
		else if((role1==MoleculeRole.LIGAND) && (role2==MoleculeRole.SOLVENT) ||
				(role2==MoleculeRole.LIGAND) && (role1==MoleculeRole.SOLVENT))
			interacting = fxmol1.getID()==fxmol2.getID() ? true : false;
		return interacting;
	}

	public void removeMolecule(V3DMolGroup group) {
		if(group instanceof V3DMolecule) {
			V3DMolecule fxmol = (V3DMolecule) group;
			mInteractionSites.remove(fxmol);
			List<V3DInteractingPair> toDelete = new ArrayList<V3DInteractingPair>();
			for(V3DInteractingPair pair: mInteractingPairs) {
				if(pair.containsMolecule3D(fxmol)) {
					pair.cleanup();
					toDelete.add(pair);
				}
			}
		}
		
		
	}
	@Override
	public void onChanged(Change<? extends V3DMolGroup> c) {
		update();
	}
	/*
	public void addMolecule(V3DMolGroup group) {
		if(group instanceof V3DMolecule) {
			V3DMolecule fxmol = (V3DMolecule) group;
			fxmol.addImplicitHydrogens();
			fxmol.RoleProperty().addListener((v,ov,nv) -> update());
			fxmol.IDProperty().addListener((v,ov,nv) -> update());
			mInteractionSites.put(fxmol, new V3DInteractionSites(fxmol));
			if(fxmol.getUnconnectedFragmentNo()>1) {
				V3DInteractingPair interactingPair = new V3DInteractingPair(fxmol, fxmol, mInteractionSites.get(fxmol),
						mInteractionSites.get(fxmol), mScene3D);
				interactingPair.analyze();
				mInteractingPairs.add(interactingPair);
			}
			List<V3DMolecule> fxmols = mScene3D.getMolsInScene();
			for(int i=0;i<fxmols.size();i++) {
				V3DMolecule fxmol2 = fxmols.get(i);
				if(fxmol==fxmol2)
					continue;
				boolean interacting = areTwoMolsInteracting(fxmol2,fxmol);
				if(interacting) {
					V3DInteractingPair interactingPair = new V3DInteractingPair(fxmol, fxmol2, mInteractionSites.get(fxmol),
						mInteractionSites.get(fxmol2), mScene3D);
					interactingPair.analyze();
					mInteractingPairs.add(interactingPair);
				}
			}
		}
		
	}
	*/
	public void toggleVisibility() {
		if(mVisibleProperty.get())
			mVisibleProperty.set(false);
		else 
			mVisibleProperty.set(true);
	}


	
	private void cleanup() {
		for(V3DInteractingPair pair: mInteractingPairs) 
			pair.cleanup();
		mInteractingPairs.clear();
		}


	}


	


