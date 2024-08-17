package org.openmolecules.fx.viewer3d.editor.actions;

import com.actelion.research.chem.StereoMolecule;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeModifier;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;

public class V3DAddAtomAction implements V3DEditorAction { 
	
	private int mAtomicNo;
	
	public V3DAddAtomAction(int atomicNo) {
		mAtomicNo = atomicNo;
	}



	@Override
	public boolean onMouseUp(V3DMolecule v3dMol, NodeDetail detail) {
		if(detail.isAtom()) {
			V3DMoleculeModifier.changeAtomElement(v3dMol, detail.getAtom(),mAtomicNo);
			v3dMol.setInitialCoordinates();
		}
		return true;
	}
	
	@Override
	public V3DMolecule onMouseUp(V3DScene scene3D) {
		StereoMolecule mol = new StereoMolecule();
		mol.setName("Molecule");
		V3DMolecule v3dMol = new V3DMolecule(mol, V3DMolecule.getNextID(), V3DMolecule.MoleculeRole.LIGAND, scene3D.mayOverrideHydrogenColor());
//		v3dMol.activateEvents();
		V3DMoleculeModifier.placeAtom(v3dMol, mAtomicNo);
		scene3D.addMolecule(v3dMol, true);
		return v3dMol;
	}



	@Override
	public boolean onMouseScrolled(V3DMolecule v3dMol, NodeDetail detail, double delta) {
		// TODO Auto-generated method stub
		return false;
	}
	


	
}
