package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeModifier;

public class V3DDeleteBondAction extends AbstractV3DEditorAction {
	
	
	public V3DDeleteBondAction() {
		super(BOND_ACTION);
	}

	@Override
	public boolean onMouseDown() {
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onMouseUp(V3DMolecule v3dMol, int bond) {
		V3DMoleculeModifier.deleteBond(v3dMol, bond);
		//v3dMol.removeBond(bond);
		
	}
	
	@Override
	public void onMouseUp(V3DMolecule v3dMol, int bond, int i) {
		
		return;
		
	}

	
}
	
	


