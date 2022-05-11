package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeModifier;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;

public class V3DDrawBondAction implements V3DEditorAction { 
	
	private int mPartnerAtom=-1;
	
	public V3DDrawBondAction() {

	}

	@Override
	public boolean onMouseUp(V3DMolecule v3dMol, NodeDetail detail) {
		if(detail.isAtom()) {
			int atom = detail.getAtom();
			if(mPartnerAtom==-1) {
				mPartnerAtom = atom;
				return true;
			}
			else {
				V3DMoleculeModifier.drawBond(v3dMol, atom, mPartnerAtom);
				v3dMol.setInitialCoordinates();
				mPartnerAtom=-1;
			}
		}
		else if(detail.isBond()) {
			mPartnerAtom=-1;
			V3DMoleculeModifier.toggleBondOrder(v3dMol, detail.getBond());
			v3dMol.setInitialCoordinates();			
		}
		return true;

	}
	
	@Override
	public V3DMolecule onMouseUp(V3DScene scene3D) {
		return null;
	}
	
	@Override
	public boolean onMouseScrolled(V3DMolecule v3dMol, NodeDetail detail, double delta) {
		// TODO Auto-generated method stub
		return false;
	}
	


	
}
