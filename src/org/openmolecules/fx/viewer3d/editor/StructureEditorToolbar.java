package org.openmolecules.fx.viewer3d.editor;

import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.V3DSceneEditor;
import org.openmolecules.fx.viewer3d.editor.actions.AbstractV3DEditorAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DAddBondAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DDeleteBondAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DChangeAtomAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DEditorAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DIncreaseBondOrderAction;

import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;



public class StructureEditorToolbar extends GridPane {
	private V3DScene mScene3D;

	
	public StructureEditorToolbar(final V3DScene scene3D) {
		super();
		mScene3D = scene3D;
		Button ch4 = new Button();
		AbstractV3DEditorAction actionC = new V3DChangeAtomAction(6);
		ch4.setText("C");
		ch4.setOnAction(event -> {
			mScene3D.getEditor().setAction(actionC);
		});
		add(ch4,1,0);
	
		Button oh2 = new Button();
		AbstractV3DEditorAction actionO = new V3DChangeAtomAction(8);
		oh2.setText("O");
		oh2.setOnAction(event -> {
			mScene3D.getEditor().setAction(actionO);
		});
		add(oh2,4,0);
		
		Button h = new Button();
		AbstractV3DEditorAction actionH = new V3DChangeAtomAction(1);
		h.setText("H");
		h.setOnAction(event -> {
			mScene3D.getEditor().setAction(actionH);
		});
		add(h,2,0);
		
		Button n = new Button();
		AbstractV3DEditorAction actionN = new V3DChangeAtomAction(7);
		n.setText("N");
		n.setOnAction(event -> {
			mScene3D.getEditor().setAction(actionN);
		});
		add(n,3,0);
		
		
		Button drawBond = new Button();
		AbstractV3DEditorAction actionDrawBond = new V3DAddBondAction();
		drawBond.setText("/");
		drawBond.setOnAction(event -> {
			mScene3D.getEditor().setAction(actionDrawBond);
		});
		add(drawBond,5,0);
		
		Button deleteBond = new Button();
		AbstractV3DEditorAction deleteBondAction = new V3DDeleteBondAction();
		deleteBond.setText("x");
		deleteBond.setOnAction(event -> {
			mScene3D.getEditor().setAction(deleteBondAction);
		});
		add(deleteBond,6,0);
		
		Button increaseBond = new Button();
		AbstractV3DEditorAction increaseBondOrderAction = new V3DIncreaseBondOrderAction();
		increaseBond.setText("//");
		increaseBond.setOnAction(event -> {
			mScene3D.getEditor().setAction(increaseBondOrderAction);
		});
		add(increaseBond,7,0);
		}
		
	
	
		
	
	
}
		


