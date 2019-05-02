package org.openmolecules.fx.viewer3d;

import org.openmolecules.fx.viewer3d.editor.actions.V3DEditorAction;

import com.actelion.research.chem.StereoMolecule;
import javafx.scene.Node;


public class V3DMoleculeEditor {
	
	private V3DEditorAction mAction;

	
	public void setAction(V3DEditorAction action) {
		mAction = action;
	}
	
	public V3DEditorAction getAction() {
		return mAction;
	}
	
	public V3DMolecule sceneClicked(V3DScene scene3D) {
		return mAction.onMouseUp(scene3D);
		

		
		
	}
	
	
	public void moleculeClicked(V3DMolecule v3dMol, Node node) {
		if(mAction!=null && v3dMol.getMeasurementMode()==V3DMolecule.MEASUREMENT.NONE) {
		NodeDetail detail = (NodeDetail)node.getUserData();
				if(detail != null)
					mAction.onMouseUp(v3dMol,detail);
		}
	}
}
