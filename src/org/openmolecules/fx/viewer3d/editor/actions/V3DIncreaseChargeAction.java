package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeModifier;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;

public class V3DIncreaseChargeAction implements V3DEditorAction { 
	
	

	@Override
	public boolean onMouseDown() {
		
		// TODO Auto-generated method stub
		return false;
	}

	
	@Override
	public void onMouseUp(V3DMolecule v3dMol, NodeDetail detail) {
		if(detail!=null && detail.isAtom())
			V3DMoleculeModifier.increaseCharge(v3dMol, detail.getAtom());
		
	}
	
	@Override
	public V3DMolecule onMouseUp(V3DScene scene3D) {
		return null;
	}

	
}
