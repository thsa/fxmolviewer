package org.openmolecules.fx.viewer3d.interactions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.V3DSceneListener;


public class V3DInteractionHandler implements V3DSceneListener{
	
	private V3DScene mScene3D;
	private List<V3DInteractingPair> mInteractingPairs;
	private Map<V3DMolecule,V3DInteractionSites> mInteractionSites;
	
	public V3DInteractionHandler(V3DScene scene) {
		mScene3D = scene;
		scene.addSceneListener(this);
		mInteractionSites  = new HashMap<V3DMolecule,V3DInteractionSites>();
		mInteractingPairs = new ArrayList<V3DInteractingPair>();
	}
	
	public void displayInteractions() {
		List<V3DMolecule> fxmols = mScene3D.getMolsInScene();
		for(V3DMolecule mol3d : fxmols) {
			mol3d.addImplicitHydrogens();
			mol3d.RoleProperty().addListener((v,ov,nv) -> update());
			mol3d.GroupProperty().addListener((v,ov,nv) -> update());
			mol3d.IDProperty().addListener((v,ov,nv) -> update());
			mInteractionSites.put(mol3d, new V3DInteractionSites(mol3d));
		}

		calculateInteractionsBetweenMols(fxmols);	
	}
	
	private void update() {
		cleanup();
		calculateInteractionsBetweenMols(mScene3D.getMolsInScene());
	}
	
	
	private void calculateInteractionsBetweenMols(List<V3DMolecule> fxmols) {
		for(int i=0;i<fxmols.size();i++) { 
			V3DMolecule fxmol1 = fxmols.get(i);
			if(fxmol1.getUnconnectedFragmentNo()>1) {
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
		if(fxmol1.getGroup()!=fxmol2.getGroup())
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

	@Override
	public void removeMolecule(V3DMolecule fxmol) {
		mInteractionSites.remove(fxmol);
		List<V3DInteractingPair> toDelete = new ArrayList<V3DInteractingPair>();
		for(V3DInteractingPair pair: mInteractingPairs) {
			if(pair.containsMolecule3D(fxmol)) {
				pair.cleanup();
				toDelete.add(pair);
			}
		}
		
		
	}

	@Override
	public void addMolecule(V3DMolecule fxmol) {
		fxmol.addImplicitHydrogens();
		fxmol.RoleProperty().addListener((v,ov,nv) -> update());
		fxmol.GroupProperty().addListener((v,ov,nv) -> update());
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

	@Override
	public void initialize(boolean isSmallMoleculeMode) {
		// TODO Auto-generated method stub
		
	}
	
	private void cleanup() {
		for(V3DInteractingPair pair: mInteractingPairs) 
			pair.cleanup();
		mInteractingPairs.clear();
		}
	}
	


