package org.openmolecules.fx.viewer3d.editor.actions;

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

public class V3DChangeAtomAction extends AbstractV3DEditorAction { 
	
	private int mAtomicNo;
	
	public V3DChangeAtomAction(int atomicNo) {
		super(ATOM_ACTION);
		mAtomicNo = atomicNo;
	}

	@Override
	public boolean onMouseDown() {
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onMouseUp(V3DMolecule v3dMol, int atom) {
		V3DMoleculeModifier.changeAtomElement(v3dMol, atom, mAtomicNo);
		
	}
	
	@Override
	public void onMouseUp(V3DMolecule v3dMol, int atom1, int atom2) {
		return;
		
	}

	
}
