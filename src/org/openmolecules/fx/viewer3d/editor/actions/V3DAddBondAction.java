package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeModifier;

public class V3DAddBondAction extends AbstractV3DEditorAction { 
	
	
	public V3DAddBondAction() {
		super(DRAWING_ACTION);
	}

	@Override
	public boolean onMouseDown() {
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onMouseUp(V3DMolecule v3dMol, int atom1,int atom2) {
		//v3dMol.constructBond(atom2);
		V3DMoleculeModifier.drawBond(v3dMol, atom1, atom2);
		
	}
	
	@Override
	public void onMouseUp(V3DMolecule v3dMol, int atom) {
		return;
		
	}

	
}
