package org.openmolecules.fx.viewer3d.interactions;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import org.openmolecules.fx.viewer3d.V3DRotatableGroup;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;
import org.openmolecules.fx.viewer3d.V3DScene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class V3DInteractionHandler implements ListChangeListener<V3DRotatableGroup> {
	
	private V3DScene mScene3D;
	private List<V3DInteractingPair> mInteractingPairs;
	private Map<V3DMolecule,V3DInteractionSites> mInteractionSites;
	private BooleanProperty mVisibleProperty;
	
	public V3DInteractionHandler(V3DScene scene) {
		mScene3D = scene;
		mScene3D.getWorld().addListener(this);
		init();
		evaluateInteractions();
	}
	
	private void init() {
		mInteractionSites  = new HashMap<V3DMolecule,V3DInteractionSites>();
		mInteractingPairs = new ArrayList<V3DInteractingPair>();
		mVisibleProperty = new SimpleBooleanProperty(true);
		mVisibleProperty.addListener((v,ov,nv) -> {
			for(V3DInteractingPair interactingPair : mInteractingPairs) {
				interactingPair.setVisibility(mVisibleProperty.get());
			}
		});
	}
	
	public void evaluateInteractions() {
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
		init();
		evaluateInteractions();
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
		boolean compatibleByGroup = false;
		MoleculeRole role1 = fxmol1.getRole();
		MoleculeRole role2 = fxmol2.getRole();
		
		if(mScene3D.getParent(fxmol1)==mScene3D.getWorld()) {
			if(mScene3D.getParent(fxmol2)==mScene3D.getWorld()) { // both mols are directly attached to world group --> interacting
				compatibleByGroup = true;
			}
			else 
				compatibleByGroup = false; // one is in the world, the other belongs to a subgroup
		}
		V3DRotatableGroup subgroup1 = null;
		V3DRotatableGroup subgroup2 = null;
		//neither fxmol1 nor fxmol2 are attached directly to the world group, but belong to subgroups
		for(V3DRotatableGroup subgroup : mScene3D.getWorld().getMolGroups()) {
			if(subgroup.getAllAttachedRotatableGroups().contains(fxmol1))
				subgroup1 = subgroup;
			if(subgroup.getAllAttachedRotatableGroups().contains(fxmol2))
				subgroup2 = subgroup;
			
		}
		if(subgroup2==subgroup1)
			compatibleByGroup = true;
		boolean interacting = false;
		if(compatibleByGroup) {
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
		}
		return interacting;
	}

	public void removeGroup(V3DRotatableGroup group) {
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
	public void onChanged(Change<? extends V3DRotatableGroup> c) {
		update();
	}


	public boolean isVisible() {
		return mVisibleProperty.get();
	}

	public void setVisibible(boolean b) {
		mVisibleProperty.set(b);
	}

	private void cleanup() {
		for(V3DInteractingPair pair: mInteractingPairs) 
			pair.cleanup();
		}


	}


	


