package org.openmolecules.fx.viewer3d.interactions;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DRotatableGroup;
import org.openmolecules.fx.viewer3d.V3DScene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class V3DInteractionHandler implements ListChangeListener<V3DRotatableGroup> {

	private final V3DScene mScene3D;
	V3DInteractionCalculator mCalculator;
	private List<V3DInteractingPair> mInteractingPairs;
	private Map<V3DMolecule, V3DInteractionSites> mInteractionSites;
	private BooleanProperty mVisibleProperty;

	public V3DInteractionHandler(V3DScene scene, V3DInteractionCalculator calculator) {
		mScene3D = scene;
		mScene3D.getWorld().addListener(this);
		mCalculator = calculator;
		init();
		evaluateInteractions();
	}

	private void init() {
		mInteractionSites  = new HashMap<>();
		mInteractingPairs = new ArrayList<>();
		if (mVisibleProperty == null) {
			mVisibleProperty = new SimpleBooleanProperty(true);
			mVisibleProperty.addListener((v,ov,nv) -> {
				for(V3DInteractingPair interactingPair : mInteractingPairs) {
					interactingPair.setVisibility(mVisibleProperty.get());
				}
			});
		}
	}

	public void evaluateInteractions() {
		List<V3DMolecule> fxmols = mScene3D.getMolsInScene();
		for(V3DMolecule v3dmol : fxmols) {
			v3dmol.addImplicitHydrogens();
			v3dmol.RoleProperty().addListener((v,ov,nv) -> update());
			v3dmol.IDProperty().addListener((v,ov,nv) -> update());
			mInteractionSites.put(v3dmol, new V3DInteractionSites(v3dmol, mCalculator));
		}

		calculateInteractionsBetweenMols(fxmols);
	}

	private void update() {
		cleanup();
		init();
		evaluateInteractions();
	}

	private void calculateInteractionsBetweenMols(List<V3DMolecule> fxmols) {
		for (int i=1; i<fxmols.size(); i++) {
			V3DMolecule fxmol1 = fxmols.get(i);
			for (int j=0; j<i; j++) {
				V3DMolecule fxmol2 = fxmols.get(j);
				if (areTwoMolsInteracting(fxmol1, fxmol2)) {
					V3DInteractingPair interactingPair = new V3DInteractingPair(
							mInteractionSites.get(fxmol1), mInteractionSites.get(fxmol2), mCalculator);
					if (interactingPair.hasInteractions()) {
						interactingPair.setVisibility(mVisibleProperty.get());
						mInteractingPairs.add(interactingPair);
					}
				}
			}
		}
	}

	private boolean areTwoMolsInteracting(V3DMolecule fxmol1, V3DMolecule fxmol2) {
		V3DMolecule.MoleculeRole role1 = fxmol1.getRole();
		V3DMolecule.MoleculeRole role2 = fxmol2.getRole();

		// both mols are directly attached to world group --> interacting
		boolean compatibleByGroup = mScene3D.getParent(fxmol1) == mScene3D.getWorld()
								 && mScene3D.getParent(fxmol2) == mScene3D.getWorld();

		V3DRotatableGroup subgroup1 = null;
		V3DRotatableGroup subgroup2 = null;
		//neither fxmol1 nor fxmol2 are attached directly to the world group, but belong to subgroups
		for(V3DRotatableGroup subgroup : mScene3D.getWorld().getGroups()) {
			if(subgroup.getAllAttachedRotatableGroups().contains(fxmol1))
				subgroup1 = subgroup;
			if(subgroup.getAllAttachedRotatableGroups().contains(fxmol2))
				subgroup2 = subgroup;
		}
		if(subgroup2==subgroup1)
			compatibleByGroup = true;

		if (!compatibleByGroup)
			return false;

		if(role1 == V3DMolecule.MoleculeRole.SOLVENT && role2 == V3DMolecule.MoleculeRole.SOLVENT)
			return true;
		if(role1 ==role2)
			return false;
		if(role1 == V3DMolecule.MoleculeRole.MACROMOLECULE || role2 == V3DMolecule.MoleculeRole.MACROMOLECULE)
			return true; //protein interacts with all other mols in the group
		if(role1 == V3DMolecule.MoleculeRole.COFACTOR || role2 == V3DMolecule.MoleculeRole.COFACTOR)
			return true; //same for cofactor
		if ((role1 == V3DMolecule.MoleculeRole.LIGAND && role2 == V3DMolecule.MoleculeRole.SOLVENT)
		 || (role2 == V3DMolecule.MoleculeRole.LIGAND && role1 == V3DMolecule.MoleculeRole.SOLVENT))
			return fxmol1.getID() == fxmol2.getID();

		return false;
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
