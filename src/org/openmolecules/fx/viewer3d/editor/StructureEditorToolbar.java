package org.openmolecules.fx.viewer3d.editor;

import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.V3DSceneEditor;
import org.openmolecules.fx.viewer3d.editor.actions.V3DDrawBondAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DDeleteAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DAddFragmentAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DChangeAtomAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DDecreaseChargeAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DEditorAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DIncreaseChargeAction;

import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;



public class StructureEditorToolbar extends GridPane {
	private V3DScene mScene3D;

	
	public StructureEditorToolbar(final V3DScene scene3D) {
		super();
		mScene3D = scene3D;
		Button ch4 = new Button();
		V3DEditorAction actionC = new V3DChangeAtomAction(6);
		ch4.setText("C");
		ch4.setOnAction(event -> {
			mScene3D.getEditor().setAction(actionC);
		});
		add(ch4,1,0);
	
		Button oh2 = new Button();
		V3DEditorAction actionO = new V3DChangeAtomAction(8);
		oh2.setText("O");
		oh2.setOnAction(event -> {
			mScene3D.getEditor().setAction(actionO);
		});
		add(oh2,4,0);
		
		Button h = new Button();
		V3DEditorAction actionH = new V3DChangeAtomAction(1);
		h.setText("H");
		h.setOnAction(event -> {
			mScene3D.getEditor().setAction(actionH);
		});
		add(h,2,0);
		
		Button n = new Button();
		V3DEditorAction actionN = new V3DChangeAtomAction(7);
		n.setText("N");
		n.setOnAction(event -> {
			mScene3D.getEditor().setAction(actionN);
		});
		add(n,3,0);
		
		Button s = new Button();
		V3DEditorAction actionS = new V3DChangeAtomAction(16);
		s.setText("S");
		s.setOnAction(event -> {
			mScene3D.getEditor().setAction(actionS);
		});
		add(s,5,0);
		
		Button p = new Button();
		V3DEditorAction actionP = new V3DChangeAtomAction(15);
		p.setText("P");
		p.setOnAction(event -> {
			mScene3D.getEditor().setAction(actionP);
		});
		add(p,6,0);
		
		
		Button drawBond = new Button();
		V3DEditorAction actionDrawBond = new V3DDrawBondAction();
		drawBond.setText("/");
		drawBond.setOnAction(event -> {
			mScene3D.getEditor().setAction(actionDrawBond);
		});
		add(drawBond,7,0);
		
		Button deleteBond = new Button();
		V3DEditorAction deleteBondAction = new V3DDeleteAction();
		deleteBond.setText("x");
		deleteBond.setOnAction(event -> {
			mScene3D.getEditor().setAction(deleteBondAction);
		});
		add(deleteBond,8,0);
		

		
		Button increaseCharge = new Button();
		V3DEditorAction increaseChargeAction = new V3DIncreaseChargeAction();
		increaseCharge.setText("+");
		increaseCharge.setOnAction(event -> {
			mScene3D.getEditor().setAction(increaseChargeAction);
		});
		add(increaseCharge,9,0);
		
		Button decreaseCharge = new Button();
		V3DEditorAction decreaseChargeAction = new V3DDecreaseChargeAction();
		decreaseCharge.setText("-");
		decreaseCharge.setOnAction(event -> {
			mScene3D.getEditor().setAction(decreaseChargeAction);
		});
		add(decreaseCharge,10,0);
		
		Button addPhenyl = new Button();
		V3DEditorAction addPhenylAction = new V3DAddFragmentAction(V3DAddFragmentAction.PHENYL_FUSION,true);
		addPhenyl.setText("\\_/");
		addPhenyl.setOnAction(event -> {
			mScene3D.getEditor().setAction(addPhenylAction);
		});
		add(addPhenyl,11,0);
		}
		
		
		
	
	
		
	
	
		
	
	
}
		


