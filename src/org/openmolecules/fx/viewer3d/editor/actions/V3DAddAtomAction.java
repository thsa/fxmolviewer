package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeBuilder;
import org.openmolecules.fx.viewer3d.V3DMoleculeModifier;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.AtomAssembler;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.share.gui.editor.geom.IDrawContext;
import com.actelion.research.share.gui.editor.io.IKeyEvent;
import com.actelion.research.share.gui.editor.io.IMouseEvent;

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
		V3DMolecule v3dMol = new V3DMolecule(new StereoMolecule(), V3DMolecule.getNextID(),scene3D.getMaxGroupID(), V3DMolecule.MoleculeRole.LIGAND, scene3D.mayOverrideHydrogenColor());
		v3dMol.setId("Molecule");
//		v3dMol.activateEvents();
		V3DMoleculeModifier.placeAtom(v3dMol, mAtomicNo);
		scene3D.addMolecule(v3dMol);
		return v3dMol;
	}



	@Override
	public boolean onMouseScrolled(V3DMolecule v3dMol, NodeDetail detail, double delta) {
		// TODO Auto-generated method stub
		return false;
	}
	


	
}
