package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.NodeDetail;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeBuilder;
import org.openmolecules.fx.viewer3d.V3DMoleculeModifier;
import org.openmolecules.fx.viewer3d.V3DScene;

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
	public boolean onMouseDown() {
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onMouseUp(V3DMolecule v3dMol, NodeDetail detail) {
		if(detail.isAtom())
			V3DMoleculeModifier.changeAtomElement(v3dMol, detail.getAtom(),mAtomicNo);
		
	}
	
	@Override
	public V3DMolecule onMouseUp(V3DScene scene3D) {
		V3DMolecule v3dMol = new V3DMolecule(new StereoMolecule());
		v3dMol.setId("Molecule");
		scene3D.addMolecule(v3dMol);
//		v3dMol.activateEvents();
		V3DMoleculeModifier.placeAtom(v3dMol, mAtomicNo);
		return v3dMol;
	}
	


	
}
