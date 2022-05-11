package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;

public class V3DRotateBondAction implements V3DEditorAction {
	
	private static final double DIHEDRAL_FACTOR = 0.0015;

	@Override
	public boolean onMouseUp(V3DMolecule v3dMol, NodeDetail detail) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onMouseScrolled(V3DMolecule v3dMol, NodeDetail detail, double delta) {

		//NodeDetail detail = (NodeDetail) mHighlightedMol.getHighlightedShape().getUserData();
		if(detail!= null && detail.isBond()) {
			int bond = detail.getBond();
			if(v3dMol.getBondRotationHelper().isRotatableBond(bond)) {
				double scale = delta/Math.PI*DIHEDRAL_FACTOR;
				v3dMol.getBondRotationHelper().rotateSmallerSide(bond, scale);
				return true;

			}
		}
	
		return false;
	}

	@Override
	public V3DMolecule onMouseUp(V3DScene scene3d) {
		// TODO Auto-generated method stub
		return null;
	}

}
