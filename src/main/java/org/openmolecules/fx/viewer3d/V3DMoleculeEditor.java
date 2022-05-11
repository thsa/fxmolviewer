package org.openmolecules.fx.viewer3d;

import org.openmolecules.fx.viewer3d.editor.actions.V3DEditorAction;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;

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
	
	
	public boolean moleculeClicked(V3DMolecule v3dMol, Node node) {
		if(mAction!=null) {
		NodeDetail detail = (NodeDetail)node.getUserData();
				if(detail != null)
					 return mAction.onMouseUp(v3dMol,detail);
		}
		return false;
	}
	
	public boolean scrolledOnMolecule(V3DMolecule v3dMol, Node node, double delta) {
		if(mAction!=null) {
		NodeDetail detail = (NodeDetail)node.getUserData();
				if(detail != null)
					return mAction.onMouseScrolled(v3dMol, detail, delta);
		}
		return false;
	}
	
}
