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

public class InteractionHandler implements ListChangeListener<V3DRotatableGroup> {

	private final V3DScene mScene3D;
	InteractionCalculator mCalculator;
	private List<InteractingPair> mInteractingPairs;
	private Map<V3DMolecule, InteractionSites> mInteractionSites;
	private BooleanProperty mVisibleProperty;

	public InteractionHandler(V3DScene scene, InteractionCalculator calculator) {
		mScene3D = scene;
		mScene3D.getWorld().addListener(this);
		mCalculator = calculator;
		init();
		evaluateInteractions();
	}

	private void init() {
		mInteractionSites  = new HashMap<>();
		mInteractingPairs = new ArrayList<>();
		mVisibleProperty = new SimpleBooleanProperty(true);
		mVisibleProperty.addListener((v,ov,nv) -> {
			for(InteractingPair interactingPair : mInteractingPairs) {
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
			mInteractionSites.put(v3dmol, new InteractionSites(v3dmol, mCalculator));
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
					InteractingPair interactingPair = new InteractingPair(
							mInteractionSites.get(fxmol1), mInteractionSites.get(fxmol2), mCalculator);
					if (interactingPair.hasInteractions())
						mInteractingPairs.add(interactingPair);
				}
			}
		}
	}

	private boolean areTwoMolsInteracting(V3DMolecule fxmol1, V3DMolecule fxmol2) {
		boolean compatibleByGroup = false;
		V3DMolecule.MoleculeRole role1 = fxmol1.getRole();
		V3DMolecule.MoleculeRole role2 = fxmol2.getRole();

		if (mScene3D.getParent(fxmol1) == mScene3D.getWorld()
		 && mScene3D.getParent(fxmol2) == mScene3D.getWorld()) // both mols are directly attached to world group --> interacting
			compatibleByGroup = true;

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
		for(InteractingPair pair: mInteractingPairs)
			pair.cleanup();
	}
}
