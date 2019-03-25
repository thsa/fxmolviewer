package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeModifier;

public class V3DIncreaseBondOrderAction extends AbstractV3DEditorAction {
	
	
	public  V3DIncreaseBondOrderAction() {
		super(BOND_ACTION);
	}

	@Override
	public boolean onMouseDown() {
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onMouseUp(V3DMolecule v3dMol, int bond) {
		V3DMoleculeModifier.changeBondOrder(v3dMol, bond);
		//v3dMol.increaseBondOrder(bond);
		
	}
	
	@Override
	public void onMouseUp(V3DMolecule v3dMol, int index1, int index2) {
		
		return;
		
	}

	
}