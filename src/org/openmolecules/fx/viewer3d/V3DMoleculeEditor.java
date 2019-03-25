package org.openmolecules.fx.viewer3d;

import java.util.ArrayList;
import java.util.Arrays;

import org.openmolecules.fx.viewer3d.editor.actions.AbstractV3DEditorAction;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.AtomAssembler;
import com.actelion.research.chem.conf.BondLengthSet;
import com.actelion.research.chem.conf.Conformer;

import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.shape.Sphere;

public class V3DMoleculeEditor implements V3DMoleculeMouseListener{
	
	private AbstractV3DEditorAction mAction;
	private int mPartnerAtom;
	
	public V3DMoleculeEditor() {
		mPartnerAtom = -1;
	}
	
	public void setAction(AbstractV3DEditorAction action) {
		mAction = action;
	}
	
	public AbstractV3DEditorAction getAction() {
		return mAction;
	}
	
	@Override
	public void mouseClicked(V3DMolecule v3dMol, MouseEvent me) {
		if(mAction!=null) {
		PickResult result = me.getPickResult();
		Node node = result.getIntersectedNode();
		System.out.println(node);
		NodeDetail detail = (NodeDetail)node.getUserData();
		switch(mAction.getMode()) {
			case(AbstractV3DEditorAction.ATOM_ACTION):
				if(detail != null && detail.isAtom()) {
					mAction.onMouseUp(v3dMol, detail.getAtom());	
					return;
				}
				else return;


			
			case(AbstractV3DEditorAction.BOND_ACTION):
				if(detail != null && detail.isBond()) {
					mAction.onMouseUp(v3dMol, detail.getBond());	
					return;
				}
				else return;
			
			case(AbstractV3DEditorAction.DRAWING_ACTION):
				if(detail.isAtom()) {
					if(mPartnerAtom==-1)mPartnerAtom = detail.getAtom();
					else {
						mAction.onMouseUp(v3dMol, detail.getAtom(),mPartnerAtom);
						mPartnerAtom = -1;
					}
				}	
		}
		
	}
		
	
	
	}
	
}