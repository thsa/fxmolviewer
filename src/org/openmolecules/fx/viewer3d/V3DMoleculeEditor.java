package org.openmolecules.fx.viewer3d;

import org.openmolecules.fx.viewer3d.editor.actions.V3DEditorAction;
import javafx.scene.Node;

public class V3DMoleculeEditor implements V3DMoleculeMouseListener{
	
	private V3DEditorAction mAction;
	private int mPartnerAtom;
	
	public V3DMoleculeEditor() {
		mPartnerAtom = -1;
	}
	
	public void setAction(V3DEditorAction action) {
		mAction = action;
	}
	
	public V3DEditorAction getAction() {
		return mAction;
	}
	
	@Override
	public void mouseClicked(V3DMolecule v3dMol, Node node) {
		if(mAction!=null && v3dMol.getMeasurementMode()==V3DMolecule.MEASUREMENT.NONE) {
		NodeDetail detail = (NodeDetail)node.getUserData();
				if(detail != null)
					mAction.onMouseUp(v3dMol,detail);
		}
	}
}
