package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.NodeDetail;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeModifier;

public class V3DDrawBondAction implements V3DEditorAction { 
	
	private int mPartnerAtom=-1;
	
	public V3DDrawBondAction() {

	}

	@Override
	public boolean onMouseDown() {
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onMouseUp(V3DMolecule v3dMol, NodeDetail detail) {
		if(detail.isAtom()) {
			int atom = detail.getAtom();
			if(mPartnerAtom==-1) {
				mPartnerAtom = atom;
				return;
			}
			else {
				V3DMoleculeModifier.drawBond(v3dMol, atom, mPartnerAtom);
				mPartnerAtom=-1;
			}
		}
		else if(detail.isBond()) {
			mPartnerAtom=-1;
			V3DMoleculeModifier.toggleBondOrder(v3dMol, detail.getBond());
			
			
			
		}

	}
	


	
}
