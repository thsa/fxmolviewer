package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeModifier;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;

public class V3DDeleteAction implements V3DEditorAction {
	
	

	@Override
	public boolean onMouseDown() {
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onMouseUp(V3DMolecule v3dMol, NodeDetail detail) {
		if(detail.isBond()) 
			V3DMoleculeModifier.deleteBond(v3dMol, detail.getBond());
		else if(detail.isAtom()) {
			V3DMoleculeModifier.deleteAtom(v3dMol, detail.getAtom());
			return;
		}
		
	}
	
	@Override
	public V3DMolecule onMouseUp(V3DScene scene3d) {
		return null;
	}


	
}
	
	


